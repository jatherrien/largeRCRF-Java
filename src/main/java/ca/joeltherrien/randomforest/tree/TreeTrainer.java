package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.*;
import ca.joeltherrien.randomforest.covariates.Covariate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.*;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TreeTrainer<Y> {

    private final ResponseCombiner<Y, ?> responseCombiner;
    private final GroupDifferentiator<Y> groupDifferentiator;

    /**
     * The number of splits to perform on each covariate. A value of 0 means all possible splits are tried.
     *
     */
    private final int numberOfSplits;
    private final int nodeSize;
    private final int maxNodeDepth;

    public TreeTrainer(final Settings settings){
        this.numberOfSplits = settings.getNumberOfSplits();
        this.nodeSize = settings.getNodeSize();
        this.maxNodeDepth = settings.getMaxNodeDepth();

        this.responseCombiner = ResponseCombiner.loadResponseCombinerByName(settings.getResponseCombiner());
        this.groupDifferentiator = settings.getGroupDifferentiator();
    }

    public Node<Y> growTree(List<Row<Y>> data, List<Covariate> covariatesToTry){
        return growNode(data, covariatesToTry, 0);
    }

    private Node<Y> growNode(List<Row<Y>> data, List<Covariate> covariatesToTry, int depth){
        // TODO; what is minimum per tree?
        if(data.size() >= 2*nodeSize && depth < maxNodeDepth && !nodeIsPure(data)){
            final Covariate.SplitRule bestSplitRule = findBestSplitRule(data, covariatesToTry);

            if(bestSplitRule == null){
                return new TerminalNode<>(
                        data.stream()
                                .map(row -> row.getResponse())
                                .collect(responseCombiner)

                );
            }

            final Split<Y> split = bestSplitRule.applyRule(data); // TODO optimize this as we're duplicating work done in findBestSplitRule

            final Node<Y> leftNode = growNode(split.leftHand, covariatesToTry, depth+1);
            final Node<Y> rightNode = growNode(split.rightHand, covariatesToTry, depth+1);

            return new SplitNode<>(leftNode, rightNode, bestSplitRule);

        }
        else{
            return new TerminalNode<>(
                    data.stream()
                        .map(row -> row.getResponse())
                        .collect(responseCombiner)

            );
        }


    }

    private Covariate.SplitRule findBestSplitRule(List<Row<Y>> data, List<Covariate> covariatesToTry){
        Covariate.SplitRule bestSplitRule = null;
        double bestSplitScore = 0.0;
        boolean first = true;

        for(final Covariate covariate : covariatesToTry){

            final int numberToTry = numberOfSplits==0 ? data.size() : numberOfSplits;

            final Collection<Covariate.SplitRule> splitRulesToTry = covariate
                    .generateSplitRules(
                            data
                                    .stream()
                                    .map(row -> row.getCovariateValue(covariate.getName()))
                                    .collect(Collectors.toList())
                            , numberToTry);

            for(final Covariate.SplitRule possibleRule : splitRulesToTry){
                final Split<Y> possibleSplit = possibleRule.applyRule(data);

                final Double score = groupDifferentiator.differentiate(
                        possibleSplit.leftHand.stream().map(row -> row.getResponse()).collect(Collectors.toList()),
                        possibleSplit.rightHand.stream().map(row -> row.getResponse()).collect(Collectors.toList())
                );

                if(score != null && (score > bestSplitScore || first)){
                    bestSplitRule = possibleRule;
                    bestSplitScore = score;
                    first = false;
                }
            }

        }

        return bestSplitRule;

    }

    private boolean nodeIsPure(List<Row<Y>> data){
        final Y first = data.get(0).getResponse();
        return data.stream().allMatch(row -> row.getResponse().equals(first));
    }

}
