package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.ResponseCombiner;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.Split;
import ca.joeltherrien.randomforest.SplitRule;
import lombok.Builder;

import java.util.Collections;
import java.util.List;
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
        double bestSplitScore = 0;
        boolean first = true;

        for(final String covariate : covariatesToTry){
            Collections.shuffle(data);

            int tries = 0;
            while(tries <= numberOfSplits || (numberOfSplits == 0 && tries < data.size())){
                final SplitRule possibleRule = data.get(tries).getCovariate(covariate).generateSplitRule(covariate);
                final Split<Y> possibleSplit = possibleRule.applyRule(data);

                final Double score = groupDifferentiator.differentiate(
                        possibleSplit.leftHand.stream().map(row -> row.getResponse()).collect(Collectors.toList()),
                        possibleSplit.rightHand.stream().map(row -> row.getResponse()).collect(Collectors.toList())
                );

                /*
                if( (groupDifferentiator.shouldMaximize() && score > bestSplitScore) || (!groupDifferentiator.shouldMaximize() && score < bestSplitScore) || first){
                    bestSplitRule = possibleRule;
                    bestSplitScore = score;
                    first = false;
                }
                */

                if( score != null && (score > bestSplitScore || first)){
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
