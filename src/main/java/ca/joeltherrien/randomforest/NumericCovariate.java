package ca.joeltherrien.randomforest;

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

        final Random random = ThreadLocalRandom.current();

        // only work with non-NA values
        data = data.stream().filter(value -> !value.isNA()).collect(Collectors.toList());

        // for this implementation we need to shuffle the data
        final List<Value<Double>> shuffledData;
        if(number > data.size()){
            shuffledData = new ArrayList<>(data);
            Collections.shuffle(shuffledData, random);
        }
        else{ // only need the top number entries
            shuffledData = new ArrayList<>(number);
            final Set<Integer> indexesToUse = new HashSet<>();

            while(indexesToUse.size() < number){
                final int index = random.nextInt(data.size());

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
        public boolean isLeftHand(final Value<Double> x) {
            if(x.isNA()) {
                throw new IllegalArgumentException("Trying to determine split on missing value");
            }

            final double xNum = x.getValue();

            return xNum <= threshold;
        }
    }
}
