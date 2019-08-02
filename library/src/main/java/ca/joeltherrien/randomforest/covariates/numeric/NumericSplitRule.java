/*
 * Copyright (c) 2019 Joel Therrien.
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.joeltherrien.randomforest.covariates.numeric;

import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.SplitRule;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class NumericSplitRule implements SplitRule<Double> {

    private static final long serialVersionUID = 1L;

    private final int parentCovariateIndex;
    private final double threshold;

    public NumericSplitRule(NumericCovariate parent, final double threshold){
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
