package ca.joeltherrien.randomforest;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NumericValue implements Value<Double> {

    private final double value;

    @Override
    public Double getValue() {
        return value;
    }

    @Override
    public SplitRule generateSplitRule(final String covariateName) {
        return new NumericSplitRule(covariateName, value);
    }
}
