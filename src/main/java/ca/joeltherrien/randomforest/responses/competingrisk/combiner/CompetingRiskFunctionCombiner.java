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

package ca.joeltherrien.randomforest.responses.competingrisk.combiner;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.tree.ForestResponseCombiner;
import ca.joeltherrien.randomforest.tree.IntermediateCombinedResponse;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class CompetingRiskFunctionCombiner implements ForestResponseCombiner<CompetingRiskFunctions, CompetingRiskFunctions> {

    private static final long serialVersionUID = 1L;

    private final int[] events;
    private final double[] times; // We may restrict ourselves to specific times.

    public int[] getEvents(){
        return events.clone();
    }

    public double[] getTimes(){
        return times.clone();
    }

    @Override
    public CompetingRiskFunctions combine(List<CompetingRiskFunctions> responses) {

        final double[] timesToUse;
        if(times != null){
            timesToUse = times;
        }
        else{
            timesToUse = responses.stream()
                    .map(functions -> functions.getSurvivalCurve())
                    .flatMapToDouble(
                            function -> Arrays.stream(function.getX())
                    ).sorted().distinct().toArray();
        }

        final IntermediateCompetingRisksFunctionsTimesKnown intermediateResult = new IntermediateCompetingRisksFunctionsTimesKnown(responses.size(), this.events, timesToUse);

        for(CompetingRiskFunctions input : responses){
            intermediateResult.processNewInput(input);
        }

        return intermediateResult.transformToOutput();
    }

    @Override
    public IntermediateCombinedResponse<CompetingRiskFunctions, CompetingRiskFunctions> startIntermediateCombinedResponse(int countInputs) {
        if(this.times != null){
            return new IntermediateCompetingRisksFunctionsTimesKnown(countInputs, this.events, this.times);
        }

        // TODO - implement
        throw new RuntimeException("startIntermediateCombinedResponse when times is unknown is not yet implemented");
    }
}
