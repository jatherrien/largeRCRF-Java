package ca.joeltherrien.randomforest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public class BooleanCovariate implements Covariate<Boolean>{

    @Getter
    private final String name;

    private final BooleanSplitRule splitRule = new BooleanSplitRule(); // there's only one possible rule for BooleanCovariates.

    @Override
    public Collection<BooleanSplitRule> generateSplitRules(List<Value<Boolean>> data, int number) {
        return Collections.singleton(splitRule);
    }

    @Override
    public BooleanValue createValue(Boolean value) {
        return new BooleanValue(value);
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
