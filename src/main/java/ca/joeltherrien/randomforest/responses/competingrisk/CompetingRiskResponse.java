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

package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.DataLoader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;

import java.io.Serializable;

@Data
public class CompetingRiskResponse implements Serializable {

    private final int delta;
    private final double u;

    public boolean isCensored(){
        return delta == 0;
    }


    @RequiredArgsConstructor
    public static class CompetingResponseLoader implements DataLoader.ResponseLoader<CompetingRiskResponse>{

        private final String deltaName;
        private final String uName;

        public CompetingResponseLoader(ObjectNode node){
            this.deltaName = node.get("delta").asText();
            this.uName = node.get("u").asText();
        }

        @Override
        public CompetingRiskResponse parse(CSVRecord record) {
            final int delta = Integer.parseInt(record.get(deltaName));
            final double u = Double.parseDouble(record.get(uName));

            return new CompetingRiskResponse(delta, u);
        }
    }

}
