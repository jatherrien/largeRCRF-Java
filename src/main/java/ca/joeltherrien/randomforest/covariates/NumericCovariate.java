package ca.joeltherrien.randomforest.covariates;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@ToString
public final class NumericCovariate implements Covariate<Double>{

    @Getter
    private final String name;

    @Getter
    private final int index;

    @Override
    public Collection<NumericSplitRule> generateSplitRules(List<Value<Double>> data, int number) {

        final Random random = ThreadLocalRandom.current();

        // only work with non-NA values
        data = data.stream().filter(value -> !value.isNA()).collect(Collectors.toList());
        //data = data.stream().filter(value -> !value.isNA()).distinct().collect(Collectors.toList()); // TODO which to use?

        // for this implementation we need to shuffle the data
        final List<Value<Double>> shuffledData;
        if(number >= data.size()){
            shuffledData = data;
        }
        else{ // only need the top number entries
            shuffledData = new ArrayList<>(number);
            final Set<Integer> indexesToUse = new HashSet<>();
            //final List<Integer> indexesToUse = new ArrayList<>(); // TODO which to use?

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

    @Override
    public NumericValue createValue(String value) {
        if(value == null || value.equalsIgnoreCase("na")){
            return createValue((Double) null);
        }

        return createValue(Double.parseDouble(value));
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
