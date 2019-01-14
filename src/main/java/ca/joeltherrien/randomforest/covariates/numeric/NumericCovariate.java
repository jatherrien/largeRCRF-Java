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
import ca.joeltherrien.randomforest.utils.IndexedIterator;
import ca.joeltherrien.randomforest.utils.UniqueSubsetValueIterator;
import ca.joeltherrien.randomforest.utils.UniqueValueIterator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@ToString
public final class NumericCovariate implements Covariate<Double> {

    @Getter
    private final String name;

    @Getter
    private final int index;

    private boolean hasNAs = false;

    @Override
    public <Y> NumericSplitRuleUpdater<Y> generateSplitRuleUpdater(List<Row<Y>> data, int number, Random random) {
        Stream<Row<Y>> stream = data.stream();

        if(hasNAs()){
            stream = stream.filter(row -> !row.getCovariateValue(this).isNA());
        }

        data = stream
                .sorted((r1, r2) -> {
                    Double d1 = r1.getCovariateValue(this).getValue();
                    Double d2 = r2.getCovariateValue(this).getValue();

                    return d1.compareTo(d2);
                })
                .collect(Collectors.toList());

        Iterator<Double> sortedDataIterator = data.stream()
                .map(row -> row.getCovariateValue(this).getValue())
                .iterator();


        final IndexedIterator<Double> dataIterator;
        if(number == 0){
            dataIterator = new UniqueValueIterator<>(sortedDataIterator);
        }
        else{
            final TreeSet<Integer> indexSet = new TreeSet<>();

            final int maxIndex = data.size();

            for(int i=0; i<number; i++){
                indexSet.add(random.nextInt(maxIndex));
            }

            dataIterator = new UniqueSubsetValueIterator<>(
                    new UniqueValueIterator<>(sortedDataIterator),
                    indexSet.toArray(new Integer[indexSet.size()])
            );

        }

        return new NumericSplitRuleUpdater<>(this, data, dataIterator);

    }

    @Override
    public NumericValue createValue(Double value) {
        return new NumericValue(value);
    }

    @Override
    public NumericValue createValue(String value) {
        if(value == null || value.equalsIgnoreCase("na")){
            this.hasNAs = true;
            return createValue((Double) null);
        }

        return createValue(Double.parseDouble(value));
    }


    @Override
    public boolean hasNAs() {
        return hasNAs;
    }

    @EqualsAndHashCode
    public class NumericValue implements Covariate.Value<Double>{

        private final Double value; // may be null

        private NumericValue(final Double value){
            this.value = value;
        }

        @Override
        public NumericCovariate getParent() {
            return NumericCovariate.this;
        }

        @Override
        public Double getValue() {
            return value;
        }

        @Override
        public boolean isNA() {
            return value == null;
        }
    }

    @EqualsAndHashCode
    public class NumericSplitRule implements Covariate.SplitRule<Double>{

        private final double threshold;

        NumericSplitRule(final double threshold){
            this.threshold = threshold;
        }

        @Override
        public final String toString() {
            return "NumericSplitRule on " + getParent().getName() + " at " + threshold;
        }

        @Override
        public NumericCovariate getParent() {
            return NumericCovariate.this;
        }

        @Override
        public boolean isLeftHand(final Value<Double> x) {
            if(x.isNA()) {
                throw new IllegalArgumentException("Trying to determine split on missing value");
            }

            final double xNum = x.getValue();

            return xNum <= threshold;
        }
    }
}
