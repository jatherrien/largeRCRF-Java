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

import ca.joeltherrien.randomforest.covariates.bool.BooleanCovariate;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor // required by Jackson
@Data
public final class BooleanCovariateSettings extends CovariateSettings<Boolean> {

    public BooleanCovariateSettings(String name){
        super(name);
    }

    @Override
    public BooleanCovariate build(int index) {
        return new BooleanCovariate(name, index);
    }
}
