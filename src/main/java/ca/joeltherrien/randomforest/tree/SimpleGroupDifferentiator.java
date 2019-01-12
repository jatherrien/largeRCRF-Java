package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.Row;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SimpleGroupDifferentiator<Y> implements GroupDifferentiator<Y> {

    @Override
    public SplitAndScore<Y, ?> differentiate(Iterator<Split<Y, ?>> splitIterator) {
        Double bestScore = null;
        Split<Y, ?> bestSplit = null;

        while(splitIterator.hasNext()){
            final Split<Y, ?> candidateSplit = splitIterator.next();

            final List<Y> leftHand = candidateSplit.getLeftHand().stream().map(Row::getResponse).collect(Collectors.toList());
            final List<Y> rightHand = candidateSplit.getRightHand().stream().map(Row::getResponse).collect(Collectors.toList());

            if(leftHand.isEmpty() || rightHand.isEmpty()){
                continue;
            }

            final Double score = getScore(leftHand, rightHand);

            if(score != null && (bestScore == null || score > bestScore)){
                bestScore = score;
                bestSplit = candidateSplit;
            }
        }

        if(bestSplit == null){
            return null;
        }

        return new SplitAndScore<>(bestSplit, bestScore);
    }

    /**
     * Return a score; higher is better.
     *
     * @param leftHand
     * @param rightHand
     * @return
     */
    public abstract Double getScore(List<Y> leftHand, List<Y> rightHand);

}
