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

package ca.joeltherrien.randomforest.utils;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponseWithCensorTime;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;

/*
    Note - this interface is copied from the runnable largeRCRF-Java project
    and is used only for helping some tests load data.
 */
@FunctionalInterface
public interface ResponseLoader<Y> {
    Y parse(CSVRecord record);

    @RequiredArgsConstructor
    class DoubleLoader implements ResponseLoader<Double> {

        private final String yName;

        @Override
        public Double parse(CSVRecord record) {
            return Double.parseDouble(record.get(yName));
        }
    }

    @RequiredArgsConstructor
    class CompetingRisksResponseLoader implements ResponseLoader<CompetingRiskResponse> {

        private final String deltaName;
        private final String uName;

        @Override
        public CompetingRiskResponse parse(CSVRecord record) {
            final int delta = Integer.parseInt(record.get(deltaName));
            final double u = Double.parseDouble(record.get(uName));

            return new CompetingRiskResponse(delta, u);
        }
    }

    @RequiredArgsConstructor
    class CompetingRisksResponseWithCensorTimesLoader implements ResponseLoader<CompetingRiskResponseWithCensorTime> {

        private final String deltaName;
        private final String uName;
        private final String cName;

        @Override
        public CompetingRiskResponseWithCensorTime parse(CSVRecord record) {
            final int delta = Integer.parseInt(record.get(deltaName));
            final double u = Double.parseDouble(record.get(uName));
            final double c = Double.parseDouble(record.get(cName));

            return new CompetingRiskResponseWithCensorTime(delta, u, c);
        }
    }

}


