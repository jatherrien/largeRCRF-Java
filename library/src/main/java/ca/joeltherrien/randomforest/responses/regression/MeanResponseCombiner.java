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

package ca.joeltherrien.randomforest.responses.regression;

import ca.joeltherrien.randomforest.tree.ForestResponseCombiner;
import ca.joeltherrien.randomforest.tree.IntermediateCombinedResponse;

import java.util.List;

/**
 * Returns the Mean value of a group of Doubles.
 *
 */
public class MeanResponseCombiner implements ForestResponseCombiner<Double, Double> {
    private static final long serialVersionUID = 1L;

    @Override
    public Double combine(List<Double> responses) {
        final double size = responses.size();

        return responses.stream().mapToDouble(db -> db/size).sum();

    }

    @Override
    public IntermediateCombinedResponse<Double, Double> startIntermediateCombinedResponse(int countInputs) {
        return new MeanIntermediateCombinedResponse(countInputs);
    }

    public static class MeanIntermediateCombinedResponse implements IntermediateCombinedResponse<Double, Double>{

        private double expectedN;
        private int actualN;
        private double currentMean;

        public MeanIntermediateCombinedResponse(int n){
            this.expectedN = n;
            this.actualN = 0;
            this.currentMean = 0.0;
        }

        @Override
        public void processNewInput(Double input) {
            this.currentMean = this.currentMean + input / expectedN;
            this.actualN ++;
        }

        @Override
        public Double transformToOutput() {
            // rescale if necessary
            this.currentMean = this.currentMean * (this.expectedN / (double) actualN);
            this.expectedN = actualN;

            return currentMean;
        }


    }

}
