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

package ca.joeltherrien.randomforest.covariates.factor;

import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.SplitRule;
import lombok.EqualsAndHashCode;

import java.util.Set;

@EqualsAndHashCode
public final class FactorSplitRule implements SplitRule<String> {

    private static final long serialVersionUID = 1L;

    private final int parentCovariateIndex;
    private final Set<String> leftSideValues;

    public FactorSplitRule(final FactorCovariate parent, final Set<String> leftSideValues){
        this.parentCovariateIndex = parent.getIndex();
        this.leftSideValues = leftSideValues;
    }

    @Override
    public int getParentCovariateIndex() {
        return parentCovariateIndex;
    }

    @Override
    public boolean isLeftHand(final Covariate.Value<String> value) {
        if(value.isNA()){
            throw new IllegalArgumentException("Trying to determine split on missing value");
        }

        return leftSideValues.contains(value.getValue());
    }
}
