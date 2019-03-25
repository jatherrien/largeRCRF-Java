package ca.joeltherrien.randomforest.covariates.numeric;

import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.SplitRule;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class NumericSplitRule implements SplitRule<Double> {

    private final int parentCovariateIndex;
    private final double threshold;

    NumericSplitRule(NumericCovariate parent, final double threshold){
        this.parentCovariateIndex = parent.getIndex();
        this.threshold = threshold;
    }

    @Override
    public final String toString() {
        return "NumericSplitRule on " + getParentCovariateIndex() + " at " + threshold;
    }

    @Override
    public int getParentCovariateIndex() {
        return parentCovariateIndex;
    }

    @Override
    public boolean isLeftHand(final Covariate.Value<Double> x) {
        if(x.isNA()) {
            throw new IllegalArgumentException("Trying to determine split on missing value");
        }

        final double xNum = x.getValue();

        return xNum <= threshold;
    }
}
