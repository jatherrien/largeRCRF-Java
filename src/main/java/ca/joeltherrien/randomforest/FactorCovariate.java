package ca.joeltherrien.randomforest;

import lombok.EqualsAndHashCode;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class FactorCovariate implements Covariate<String>{

    private final String name;
    private final Map<String, FactorValue> factorLevels;
    private final int numberOfPossiblePairings;


    public FactorCovariate(final String name, List<String> levels){
        this.name = name;
        this.factorLevels = new HashMap<>();

        for(final String level : levels){
            final FactorValue newValue = new FactorValue(level);

            factorLevels.put(level, newValue);
        }

        int numberOfPossiblePairingsTemp = 1;
        for(int i=0; i<levels.size()-1; i++){
            numberOfPossiblePairingsTemp *= 2;
        }
        this.numberOfPossiblePairings = numberOfPossiblePairingsTemp-1;

    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<FactorSplitRule> generateSplitRules(List<Value<String>> data, int number) {
        final Set<FactorSplitRule> splitRules = new HashSet<>();

        // This is to ensure we don't get stuck in an infinite loop for small factors
        number = Math.min(number, numberOfPossiblePairings);
        final Random random = ThreadLocalRandom.current();
        final List<FactorValue> levels = new ArrayList<>(factorLevels.values());



        while(splitRules.size() < number){
            Collections.shuffle(levels, random);
            final Set<FactorValue> leftSideValues = new HashSet<>();
            leftSideValues.add(levels.get(0));

            for(int i=1; i<levels.size()/2; i++){
                if(random.nextBoolean()){
                    leftSideValues.add(levels.get(i));
                }
            }

            splitRules.add(new FactorSplitRule(leftSideValues));
        }

        return splitRules;

    }

    @Override
    public FactorValue createValue(String value) {
        final FactorValue factorValue = factorLevels.get(value);

        if(factorValue == null){
            throw new IllegalArgumentException(value + " is not a level in FactorCovariate " + name);
        }

        return factorValue;
    }

    @EqualsAndHashCode
    public final class FactorValue implements Covariate.Value<String>{

        private final String value;

        private FactorValue(final String value){
            this.value = value;
        }

        @Override
        public FactorCovariate getParent() {
            return FactorCovariate.this;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    @EqualsAndHashCode
    public final class FactorSplitRule implements Covariate.SplitRule<String>{

        private final Set<FactorValue> leftSideValues;

        private FactorSplitRule(final Set<FactorValue> leftSideValues){
            this.leftSideValues = leftSideValues;
        }

        @Override
        public FactorCovariate getParent() {
            return FactorCovariate.this;
        }

        @Override
        public boolean isLeftHand(CovariateRow row) {
            final FactorValue value = (FactorValue) row.getCovariateValue(getName()).getValue();

            return leftSideValues.contains(value);


        }
    }
}
