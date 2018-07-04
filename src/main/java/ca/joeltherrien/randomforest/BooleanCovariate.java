package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.exceptions.MissingValueException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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

        private final boolean value;

        private BooleanValue(final boolean value){
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
        public boolean isLeftHand(CovariateRow row) {
            final Value<?> x = row.getCovariateValue(getParent().getName());
            if(x == null) {
                throw new MissingValueException(row, this);
            }

            final boolean xBoolean = (Boolean) x.getValue();

            return !xBoolean;
        }
    }
}
