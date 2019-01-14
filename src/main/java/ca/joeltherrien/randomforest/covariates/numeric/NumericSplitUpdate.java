package ca.joeltherrien.randomforest.covariates.numeric;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import lombok.AllArgsConstructor;

import java.util.Collection;

@AllArgsConstructor
public class NumericSplitUpdate<Y> implements Covariate.SplitUpdate<Y, Double> {

    private final NumericCovariate.NumericSplitRule numericSplitRule;
    private final Collection<Row<Y>> rowsMoved;

    @Override
    public NumericCovariate.NumericSplitRule getSplitRule() {
        return numericSplitRule;
    }

    @Override
    public Collection<Row<Y>> rowsMovedToLeftHand() {
        return rowsMoved;
    }
}
