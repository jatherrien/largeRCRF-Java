package ca.joeltherrien.randomforest.covariates.numeric;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.tree.Split;
import ca.joeltherrien.randomforest.utils.IndexedIterator;

import java.util.Collections;
import java.util.List;

public class NumericSplitRuleUpdater<Y> implements Covariate.SplitRuleUpdater<Y, Double>  {

    private final NumericCovariate covariate;
    private final List<Row<Y>> orderedData;
    private final IndexedIterator<Double> dataIterator;

    private Split<Y, Double> currentSplit;

    public NumericSplitRuleUpdater(final NumericCovariate covariate, final List<Row<Y>> orderedData, final IndexedIterator<Double> iterator){
        this.covariate = covariate;
        this.orderedData = orderedData;
        this.dataIterator = iterator;

        final List<Row<Y>> leftHandList = Collections.emptyList();
        final List<Row<Y>> rightHandList = orderedData;

        this.currentSplit = new Split<>(
                covariate.new NumericSplitRule(Double.MIN_VALUE),
                leftHandList,
                rightHandList,
                Collections.emptyList());

    }

    @Override
    public Split<Y, Double> currentSplit() {
        return this.currentSplit;
    }

    @Override
    public NumericSplitUpdate<Y> nextUpdate() {
        if(hasNext()){
            final int currentPosition = dataIterator.getIndex();
            final Double splitValue = dataIterator.next();
            final int newPosition = dataIterator.getIndex();

            final List<Row<Y>> rowsMoved = orderedData.subList(currentPosition, newPosition);

            final NumericCovariate.NumericSplitRule splitRule = covariate.new NumericSplitRule(splitValue);

            // Update current split
            this.currentSplit = new Split<>(
                    splitRule,
                    orderedData.subList(0, newPosition),
                    orderedData.subList(newPosition, orderedData.size()),
                    Collections.emptyList());


            return new NumericSplitUpdate<>(splitRule, rowsMoved);
        }

        return null;
    }

    @Override
    public boolean hasNext() {
        return dataIterator.hasNext();
    }

    @Override
    public Split<Y, Double> next() {
        if(hasNext()){
            nextUpdate();
        }

        return this.currentSplit();
    }

}
