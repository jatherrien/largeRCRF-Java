package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.exceptions.MissingValueException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class NumericCovariate implements Covariate<Double>{

    @Getter
    private final String name;

    @Override
    public Collection<NumericSplitRule> generateSplitRules(List<Value<Double>> data, int number) {

        // for this implementation we need to shuffle the data
        final List<Value<Double>> shuffledData;
        if(number > data.size()){
            shuffledData = new ArrayList<>(data);
            Collections.shuffle(shuffledData);
        }
        else{ // only need the top number entries
            shuffledData = new ArrayList<>(number);
            final Set<Integer> indexesToUse = new HashSet<>();

            while(indexesToUse.size() < number){
                final int index = ThreadLocalRandom.current().nextInt(data.size());

                if(indexesToUse.add(index)){
                    shuffledData.add(data.get(index));
                }
            }

        }

        return shuffledData.stream()
                .mapToDouble(v -> v.getValue())
                .mapToObj(threshold -> new NumericSplitRule(threshold))
                .collect(Collectors.toSet());
        // by returning a set we'll make everything far more efficient as a lot of rules can repeat due to bootstrapping


    }

    @Override
    public NumericValue createValue(Double value) {
        return new NumericValue(value);
    }

    public class NumericValue implements Covariate.Value<Double>{

        private final double value;

        private NumericValue(final double value){
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
    }

    public class NumericSplitRule implements Covariate.SplitRule<Double>{

        private final double threshold;

        private NumericSplitRule(final double threshold){
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
        public boolean isLeftHand(CovariateRow row) {
            final Covariate.Value<?> x = row.getCovariateValue(getParent().getName());
            if(x == null) {
                throw new MissingValueException(row, this);
            }

            final double xNum = (Double) x.getValue();

            return xNum <= threshold;
        }
    }
}
