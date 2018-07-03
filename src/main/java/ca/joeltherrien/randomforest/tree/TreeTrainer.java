package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.*;
import ca.joeltherrien.randomforest.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.regression.WeightedVarianceGroupDifferentiator;
import lombok.Builder;

import java.util.*;
import java.util.stream.Collectors;

@Builder
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

    private final Random random = new Random();


    public Node<Y> growTree(List<Row<Y>> data, List<String> covariatesToTry){
        return growNode(data, covariatesToTry, 0);
    }

    private Node<Y> growNode(List<Row<Y>> data, List<String> covariatesToTry, int depth){
        // TODO; what is minimum per tree?
        if(data.size() >= 2*nodeSize && depth < maxNodeDepth && !nodeIsPure(data, covariatesToTry)){
            final SplitRule bestSplitRule = findBestSplitRule(data, covariatesToTry);

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

    private SplitRule findBestSplitRule(List<Row<Y>> data, List<String> covariatesToTry){
        SplitRule bestSplitRule = null;
        Double bestSplitScore = 0.0; // may be null
        boolean first = true;

        for(final String covariate : covariatesToTry){

            final List<Row<Y>> shuffledData;
            if(numberOfSplits == 0 || numberOfSplits > data.size()){
                shuffledData = new ArrayList<>(data);
                Collections.shuffle(shuffledData);
            }
            else{ // only need the top numberOfSplits entries
                shuffledData = new ArrayList<>(numberOfSplits);
                final Set<Integer> indexesToUse = new HashSet<>();

                while(indexesToUse.size() < numberOfSplits){
                    final int index = random.nextInt(data.size());

                    if(indexesToUse.add(index)){
                        shuffledData.add(data.get(index));
                    }
                }

            }


            int tries = 0;

            while(tries < shuffledData.size()){
                final SplitRule possibleRule = shuffledData.get(tries).getCovariate(covariate).generateSplitRule(covariate);
                final Split<Y> possibleSplit = possibleRule.applyRule(data);

                final Double score = groupDifferentiator.differentiate(
                        possibleSplit.leftHand.stream().map(row -> row.getResponse()).collect(Collectors.toList()),
                        possibleSplit.rightHand.stream().map(row -> row.getResponse()).collect(Collectors.toList())
                );

                if( first || (score != null && (bestSplitScore == null || score > bestSplitScore))){
                    bestSplitRule = possibleRule;
                    bestSplitScore = score;
                    first = false;
                }

                tries++;
            }

        }

        return bestSplitRule;

    }

    private boolean nodeIsPure(List<Row<Y>> data, List<String> covariatesToTry){
        // TODO how is this done?

        final Y first = data.get(0).getResponse();
        return data.stream().allMatch(row -> row.getResponse().equals(first));
    }

}
