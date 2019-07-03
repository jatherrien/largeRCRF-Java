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

package ca.joeltherrien.randomforest.covariates.settings;

import ca.joeltherrien.randomforest.covariates.numeric.NumericCovariate;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor // required by Jackson
@Data
public final class NumericCovariateSettings extends CovariateSettings<Double> {

    public NumericCovariateSettings(String name){
        super(name);
    }

    @Override
    public NumericCovariate build(int index) {
        return new NumericCovariate(name, index);
    }
}
