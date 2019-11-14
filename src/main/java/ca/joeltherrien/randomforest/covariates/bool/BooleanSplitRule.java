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

package ca.joeltherrien.randomforest.covariates.bool;

import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.SplitRule;

public class BooleanSplitRule implements SplitRule<Boolean> {

    private static final long serialVersionUID = 1L;

    private final int parentCovariateIndex;

    public BooleanSplitRule(BooleanCovariate parent){
        this.parentCovariateIndex = parent.getIndex();
    }

    @Override
    public final String toString() {
        return "BooleanSplitRule";
    }

    @Override
    public int getParentCovariateIndex() {
        return parentCovariateIndex;
    }

    @Override
    public boolean isLeftHand(final Covariate.Value<Boolean> value) {
        if(value.isNA()) {
            throw new IllegalArgumentException("Trying to determine split on missing value");
        }

        return !value.getValue();
    }
}