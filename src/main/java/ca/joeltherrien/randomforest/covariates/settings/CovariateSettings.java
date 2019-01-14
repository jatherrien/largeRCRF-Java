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

import ca.joeltherrien.randomforest.covariates.Covariate;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Nuisance class to work with Jackson for persisting settings.
 *
 * @param <V>
 */
@NoArgsConstructor // required for Jackson
@Getter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BooleanCovariateSettings.class, name = "boolean"),
        @JsonSubTypes.Type(value = NumericCovariateSettings.class, name = "numeric"),
        @JsonSubTypes.Type(value = FactorCovariateSettings.class, name = "factor")
})
public abstract class CovariateSettings<V> {

    String name;

    CovariateSettings(String name){
        this.name = name;
    }

    public abstract Covariate<V> build(int index);
}
