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
import ca.joeltherrien.randomforest.tree.ResponseCombiner;
import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class CompetingRiskFunctionCombiner implements ResponseCombiner<CompetingRiskFunctions, CompetingRiskFunctions> {

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

        final double n = responses.size();

        final double[] survivalY = new double[timesToUse.length];

        for(int i=0; i<timesToUse.length; i++){
            final double time = timesToUse[i];
            survivalY[i] = responses.stream()
                    .mapToDouble(functions -> functions.getSurvivalCurve().evaluate(time) / n)
                    .sum();
        }

        final RightContinuousStepFunction survivalFunction = new RightContinuousStepFunction(timesToUse, survivalY, 1.0);

        final List<RightContinuousStepFunction> causeSpecificCumulativeHazardFunctionList = new ArrayList<>(events.length);
        final List<RightContinuousStepFunction> cumulativeIncidenceFunctionList = new ArrayList<>(events.length);

        for(final int event : events){

            final double[] cumulativeHazardFunctionY = new double[timesToUse.length];
            final double[] cumulativeIncidenceFunctionY = new double[timesToUse.length];

            for(int i=0; i<timesToUse.length; i++){
                final double time = timesToUse[i];

                cumulativeHazardFunctionY[i] = responses.stream()
                        .mapToDouble(functions -> functions.getCauseSpecificHazardFunction(event).evaluate(time) / n)
                        .sum();

                cumulativeIncidenceFunctionY[i] = responses.stream()
                        .mapToDouble(functions -> functions.getCumulativeIncidenceFunction(event).evaluate(time) / n)
                        .sum();

            }

            causeSpecificCumulativeHazardFunctionList.add(event-1, new RightContinuousStepFunction(timesToUse, cumulativeHazardFunctionY, 0));
            cumulativeIncidenceFunctionList.add(event-1, new RightContinuousStepFunction(timesToUse, cumulativeIncidenceFunctionY, 0));

        }

        return CompetingRiskFunctions.builder()
                .causeSpecificHazards(causeSpecificCumulativeHazardFunctionList)
                .cumulativeIncidenceCurves(cumulativeIncidenceFunctionList)
                .survivalCurve(survivalFunction)
                .build();
    }
}
