package ca.joeltherrien.randomforest.covariates;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public final class BooleanCovariate implements Covariate<Boolean>{

    @Getter
    private final String name;

    @Getter
    private final int index;

    private final BooleanSplitRule splitRule = new BooleanSplitRule(); // there's only one possible rule for BooleanCovariates.

    @Override
    public Collection<BooleanSplitRule> generateSplitRules(List<Value<Boolean>> data, int number) {
        return Collections.singleton(splitRule);
    }

    @Override
    public BooleanValue createValue(Boolean value) {
        return new BooleanValue(value);
    }

    @Override
    public Value<Boolean> createValue(String value) {
        if(value == null || value.equalsIgnoreCase("na")){
            return createValue( (Boolean) null);
        }

        if(value.equalsIgnoreCase("true")){
            return createValue(true);
        }
        else if(value.equalsIgnoreCase("false")){
            return createValue(false);
        }
        else{
            throw new IllegalArgumentException("Require either true/false/na to create BooleanCovariate");
        }
    }

    @Override
    public String toString(){
        return "BooleanCovariate(name=" + name + ")";
    }

    public class BooleanValue implements Value<Boolean>{

        private final Boolean value;

        private BooleanValue(final Boolean value){
            this.value = value;
        }

        @Override
        public BooleanCovariate getParent() {
            return BooleanCovariate.this;
        }

        @Override
        public Boolean getValue() {
            return value;
        }

        @Override
        public boolean isNA() {
            return value == null;
        }
    }

    public class BooleanSplitRule implements SplitRule<Boolean>{

        @Override
        public final String toString() {
            return "BooleanSplitRule";
        }

        @Override
        public BooleanCovariate getParent() {
            return BooleanCovariate.this;
        }

        @Override
        public boolean isLeftHand(final Value<Boolean> value) {
            if(value.isNA()) {
                throw new IllegalArgumentException("Trying to determine split on missing value");
            }

            return !value.getValue();
        }
    }
}
