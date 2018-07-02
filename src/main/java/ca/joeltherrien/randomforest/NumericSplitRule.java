package ca.joeltherrien.randomforest;

import java.util.LinkedList;
import java.util.List;

import ca.joeltherrien.randomforest.exceptions.MissingValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NumericSplitRule extends SplitRule{

	public final String covariateName;	
	public final double threshold;

	@Override
	public final String toString() {
		return "NumericSplitRule on " + covariateName + " at " + threshold;
	}

    @Override
    public boolean isLeftHand(CovariateRow row) {
        final Value<?> x = row.getCovariate(covariateName);
        if(x == null) {
            throw new MissingValueException(row, this);
        }

        final double xNum = (Double) x.getValue();

        return xNum <= threshold;
    }


}
