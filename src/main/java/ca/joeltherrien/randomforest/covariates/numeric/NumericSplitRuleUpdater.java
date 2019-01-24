/*
 * Copyright (c) 2019 Joel Therrien.
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
                covariate.new NumericSplitRule(Double.NEGATIVE_INFINITY),
                leftHandList,
                rightHandList,
                Collections.emptyList());

    }

    @Override
    public Split<Y, Double> currentSplit() {
        return this.currentSplit;
    }

    @Override
    public boolean currentSplitValid() {
        return currentSplit.getLeftHand().size() > 0 && currentSplit.getRightHand().size() > 0;
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
                    Collections.unmodifiableList(orderedData.subList(0, newPosition)),
                    Collections.unmodifiableList(orderedData.subList(newPosition, orderedData.size())),
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
