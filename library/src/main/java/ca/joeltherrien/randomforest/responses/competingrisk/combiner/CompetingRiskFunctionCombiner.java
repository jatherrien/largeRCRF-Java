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
import ca.joeltherrien.randomforest.utils.Utils;
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
        final double[][] csCHFY = new double[events.length][timesToUse.length];
        final double[][] cifY = new double[events.length][timesToUse.length];

        /*
            We're going to try to efficiently put our predictions together -
                Assumptions - for each event on a response, the hazard and CIF functions share the same x points

            Plan - go through the time on each response and make use of that so that when we search for a time index
            to evaluate the function at, we don't need to re-search the earlier times.

         */


        for(final CompetingRiskFunctions currentFunctions : responses){
            final double[] survivalXPoints = currentFunctions.getSurvivalCurve().getX();
            final double[][] eventSpecificXPoints = new double[events.length][];

            for(final int event : events){
                eventSpecificXPoints[event-1] = currentFunctions.getCumulativeIncidenceFunction(event)
                        .getX();
            }

            int previousSurvivalIndex = 0;
            final int[] previousEventSpecificIndex = new int[events.length]; // relying on 0 being default value

            for(int i=0; i<timesToUse.length; i++){
                final double time = timesToUse[i];

                // Survival curve
                final int survivalTimeIndex = Utils.binarySearchLessThan(previousSurvivalIndex, survivalXPoints.length, survivalXPoints, time);
                survivalY[i] = survivalY[i] + currentFunctions.getSurvivalCurve().evaluateByIndex(survivalTimeIndex) / n;
                previousSurvivalIndex = Math.max(survivalTimeIndex, 0); // if our current time is less than the smallest time in xPoints then binarySearchLessThan returned a -1.
                // -1's not an issue for evaluateByIndex, but it is an issue for the next time binarySearchLessThan is called.

                // CHFs and CIFs
                for(final int event : events){
                    final double[] xPoints = eventSpecificXPoints[event-1];
                    final int eventTimeIndex = Utils.binarySearchLessThan(previousEventSpecificIndex[event-1], xPoints.length,
                            xPoints, time);
                    csCHFY[event-1][i] = csCHFY[event-1][i] + currentFunctions.getCauseSpecificHazardFunction(event)
                            .evaluateByIndex(eventTimeIndex) / n;
                    cifY[event-1][i] = cifY[event-1][i] + currentFunctions.getCumulativeIncidenceFunction(event)
                            .evaluateByIndex(eventTimeIndex) / n;

                    previousEventSpecificIndex[event-1] = Math.max(eventTimeIndex, 0);
                }
            }

        }

        final RightContinuousStepFunction survivalFunction = new RightContinuousStepFunction(timesToUse, survivalY, 1.0);
        final List<RightContinuousStepFunction> causeSpecificCumulativeHazardFunctionList = new ArrayList<>(events.length);
        final List<RightContinuousStepFunction> cumulativeIncidenceFunctionList = new ArrayList<>(events.length);

        for(final int event : events){
            causeSpecificCumulativeHazardFunctionList.add(event-1, new RightContinuousStepFunction(timesToUse, csCHFY[event-1], 0));
            cumulativeIncidenceFunctionList.add(event-1, new RightContinuousStepFunction(timesToUse, cifY[event-1], 0));
        }

        return CompetingRiskFunctions.builder()
                .causeSpecificHazards(causeSpecificCumulativeHazardFunctionList)
                .cumulativeIncidenceCurves(cumulativeIncidenceFunctionList)
                .survivalCurve(survivalFunction)
                .build();
    }
}
