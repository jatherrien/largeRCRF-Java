package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.Settings;
import ca.joeltherrien.randomforest.covariates.Covariate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TreeTrainer<Y, O> {

    private final ResponseCombiner<Y, O> responseCombiner;
    private final GroupDifferentiator<Y> groupDifferentiator;

    /**
     * The number of splits to perform on each covariate. A value of 0 means all possible splits are tried.
     *
     */
    private final int numberOfSplits;
    private final int nodeSize;
    private final int maxNodeDepth;
    private final int mtry;

    private final List<Covariate> covariates;

    public TreeTrainer(final Settings settings, final List<Covariate> covariates){
        this.numberOfSplits = settings.getNumberOfSplits();
        this.nodeSize = settings.getNodeSize();
        this.maxNodeDepth = settings.getMaxNodeDepth();
        this.mtry = settings.getMtry();

        this.responseCombiner = settings.getResponseCombiner();
        this.groupDifferentiator = settings.getGroupDifferentiator();
        this.covariates = covariates;
    }

    public Tree<O> growTree(List<Row<Y>> data){

        final Node<O> rootNode = growNode(data, 0);
        return new Tree<>(rootNode, data.stream().mapToInt(Row::getId).toArray());

    }

    private Node<O> growNode(List<Row<Y>> data, int depth){
        // TODO; what is minimum per tree?
        if(data.size() >= 2*nodeSize && depth < maxNodeDepth && !nodeIsPure(data)){
            final List<Covariate> covariatesToTry = selectCovariates(this.mtry);
            final SplitRuleAndSplit bestSplitRuleAndSplit = findBestSplitRule(data, covariatesToTry);

            if(bestSplitRuleAndSplit.splitRule == null){

                return new TerminalNode<>(
                        responseCombiner.combine(
                                data.stream().map(row -> row.getResponse()).collect(Collectors.toList())
                        )
                );


            }

            final Split<Y> split = bestSplitRuleAndSplit.split;
            // Note that NAs have already been handled


            final Node<O> leftNode = growNode(split.leftHand, depth+1);
            final Node<O> rightNode = growNode(split.rightHand, depth+1);

            return new SplitNode<>(leftNode, rightNode, bestSplitRuleAndSplit.splitRule, bestSplitRuleAndSplit.probabilityLeftHand);

        }
        else{
            return new TerminalNode<>(
                    responseCombiner.combine(
                            data.stream().map(row -> row.getResponse()).collect(Collectors.toList())
                    )
            );
        }


    }

    private List<Covariate> selectCovariates(int mtry){
        if(mtry >= covariates.size()){
            return covariates;
        }

        final List<Covariate> splitCovariates = new ArrayList<>(covariates);
        Collections.shuffle(splitCovariates, ThreadLocalRandom.current());

        if (splitCovariates.size() > mtry) {
            splitCovariates.subList(mtry, splitCovariates.size()).clear();
        }

        return splitCovariates;
    }

    private SplitRuleAndSplit findBestSplitRule(List<Row<Y>> data, List<Covariate> covariatesToTry){
        SplitRuleAndSplit bestSplitRuleAndSplit = new SplitRuleAndSplit();
        double bestSplitScore = 0.0;
        boolean first = true;

        for(final Covariate covariate : covariatesToTry){

            final int numberToTry = numberOfSplits==0 ? data.size() : numberOfSplits;

            final Collection<Covariate.SplitRule> splitRulesToTry = covariate
                    .generateSplitRules(
                            data
                                    .stream()
                                    .map(row -> row.getCovariateValue(covariate))
                                    .collect(Collectors.toList())
                            , numberToTry);

            for(final Covariate.SplitRule possibleRule : splitRulesToTry){
                final Split<Y> possibleSplit = possibleRule.applyRule(data);

                // We have to handle any NAs
                if(possibleSplit.leftHand.size() == 0 && possibleSplit.rightHand.size() == 0 && possibleSplit.naHand.size() > 0){
                    throw new IllegalArgumentException("Can't apply " + this + " when there are rows with missing data and no non-missing value rows");
                }

                final double probabilityLeftHand = (double) possibleSplit.leftHand.size() / (double) (possibleSplit.leftHand.size() + possibleSplit.rightHand.size());

                final Random random = ThreadLocalRandom.current();
                for(final Row<Y> missingValueRow : possibleSplit.naHand){
                    final boolean randomDecision = random.nextDouble() <= probabilityLeftHand;
                    if(randomDecision){
                        possibleSplit.leftHand.add(missingValueRow);
                    }
                    else{
                        possibleSplit.rightHand.add(missingValueRow);
                    }
                }

                final Double score = groupDifferentiator.differentiate(
                        possibleSplit.leftHand.stream().map(row -> row.getResponse()).collect(Collectors.toList()),
                        possibleSplit.rightHand.stream().map(row -> row.getResponse()).collect(Collectors.toList())
                );


                if(score != null && !Double.isNaN(score) && (score > bestSplitScore || first)){
                    bestSplitRuleAndSplit.splitRule = possibleRule;
                    bestSplitRuleAndSplit.split = possibleSplit;
                    bestSplitRuleAndSplit.probabilityLeftHand = probabilityLeftHand;

                    bestSplitScore = score;
                    first = false;
                }
            }

        }

        return bestSplitRuleAndSplit;

    }

    private boolean nodeIsPure(List<Row<Y>> data){
        final Y first = data.get(0).getResponse();
        return data.stream().allMatch(row -> row.getResponse().equals(first));
    }

    private class SplitRuleAndSplit{
        private Covariate.SplitRule splitRule = null;
        private Split<Y> split = null;
        private double probabilityLeftHand;
    }

}
