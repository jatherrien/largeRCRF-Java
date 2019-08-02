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
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.tree.ResponseCombiner;
import ca.joeltherrien.randomforest.utils.Point;
import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class takes all of the observations in a terminal node and combines them to produce estimates of the cause-specific hazard function
 * and the cumulative incidence curve.
 *
 * See https://kogalur.github.io/randomForestSRC/theory.html for details.
 *
 */
public class CompetingRiskResponseCombiner implements ResponseCombiner<CompetingRiskResponse, CompetingRiskFunctions> {

    private static final long serialVersionUID = 1L;

    private final int[] events;

    public CompetingRiskResponseCombiner(final int[] events){
        this.events = events.clone();

        // Check to make sure that events go from 1 to the right order
        for(int i=0; i<events.length; i++){
            if(events[i] != (i+1)){
                throw new IllegalArgumentException("The events parameter must be in the form 1,2,3,...J with no gaps");
            }
        }
    }

    public int[] getEvents(){
        return events.clone();
    }

    @Override
    public CompetingRiskFunctions combine(List<CompetingRiskResponse> responses) {

        final List<RightContinuousStepFunction> causeSpecificCumulativeHazardFunctionList = new ArrayList<>(events.length);
        final List<RightContinuousStepFunction> cumulativeIncidenceFunctionList = new ArrayList<>(events.length);

        Collections.sort(responses, (y1, y2) -> {
            if(y1.getU() < y2.getU()){
                return -1;
            }
            else if(y1.getU() > y2.getU()){
                return 1;
            }
            else{
                return 0;
            }
        });

        final int n = responses.size();

        int[] numberOfCurrentEvents = new int[events.length+1];

        double previousSurvivalValue = 1.0;
        final List<Point> survivalPoints = new ArrayList<>(n); // better to be too large than too small

        // Also track riskSet variables and numberOfEvents, and timesToUse
        final List<Double> timesToUseList = new ArrayList<>(n);
        final List<Integer> riskSetList = new ArrayList<>(n);
        final List<int[]> numberOfEvents = new ArrayList<>(n);


        for(int i=0; i<n; i++){
            final CompetingRiskResponse currentResponse = responses.get(i);
            final boolean lastOfTime = (i+1)==n || responses.get(i+1).getU() > currentResponse.getU();

            numberOfCurrentEvents[currentResponse.getDelta()]++;

            if(lastOfTime){
                int totalNumberOfCurrentEvents = 0;
                for(int e = 1; e < numberOfCurrentEvents.length; e++){ // exclude censored events
                    totalNumberOfCurrentEvents += numberOfCurrentEvents[e];
                }

                if(totalNumberOfCurrentEvents > 0){
                    // Add point
                    final double currentTime = currentResponse.getU();
                    final int riskSet = n - (i+1) + totalNumberOfCurrentEvents + numberOfCurrentEvents[0];
                    final double newValue = previousSurvivalValue * (1.0 - (double) totalNumberOfCurrentEvents / (double) riskSet);
                    survivalPoints.add(new Point(currentTime, newValue));
                    previousSurvivalValue = newValue;

                    timesToUseList.add(currentTime);
                    riskSetList.add(riskSet);
                    numberOfEvents.add(numberOfCurrentEvents);

                }
                // reset counters
                numberOfCurrentEvents = new int[events.length+1];

            }

        }
        final RightContinuousStepFunction survivalCurve = RightContinuousStepFunction.constructFromPoints(survivalPoints, 1.0);


        for(final int event : events){

            final List<Point> hazardFunctionPoints = new ArrayList<>(timesToUseList.size());
            Point previousHazardFunctionPoint = new Point(0.0, 0.0);

            final List<Point> cifPoints = new ArrayList<>(timesToUseList.size());
            Point previousCIFPoint = new Point(0.0, 0.0);

            for(int i=0; i<timesToUseList.size(); i++){
                final double time_k = timesToUseList.get(i);
                final double individualsAtRisk = riskSetList.get(i); // Y(t_k)

                if(individualsAtRisk == 0){
                    // if we continue we'll get NaN
                    break;
                }

                final double numberEventsAtTime = numberOfEvents.get(i)[event]; // d_j(t_k)

                // Cause-specific cumulative hazard function
                final double hazardDeltaY = numberEventsAtTime / individualsAtRisk;
                final Point newHazardPoint = new Point(time_k, previousHazardFunctionPoint.getY() + hazardDeltaY);
                hazardFunctionPoints.add(newHazardPoint);
                previousHazardFunctionPoint = newHazardPoint;


                // Cumulative incidence function
                // TODO - confirm this behaviour
                //final double previousSurvivalEvaluation = i > 0 ? survivalCurve.evaluate(timesToUse[i-1]).getY() : survivalCurve.evaluate(0.0).getY();
                final double previousSurvivalEvaluation = i > 0 ? survivalCurve.evaluateByIndex(i-1) : 1.0;

                final double cifDeltaY = previousSurvivalEvaluation * (numberEventsAtTime / individualsAtRisk);
                final Point newCIFPoint = new Point(time_k, previousCIFPoint.getY() + cifDeltaY);
                cifPoints.add(newCIFPoint);
                previousCIFPoint = newCIFPoint;

            }

            final RightContinuousStepFunction causeSpecificCumulativeHazardFunction = RightContinuousStepFunction.constructFromPoints(hazardFunctionPoints, 0.0);
            causeSpecificCumulativeHazardFunctionList.add(event-1, causeSpecificCumulativeHazardFunction);

            final RightContinuousStepFunction cifFunction = RightContinuousStepFunction.constructFromPoints(cifPoints, 0.0);
            cumulativeIncidenceFunctionList.add(event-1, cifFunction);
        }


        return CompetingRiskFunctions.builder()
                .causeSpecificHazards(causeSpecificCumulativeHazardFunctionList)
                .cumulativeIncidenceCurves(cumulativeIncidenceFunctionList)
                .survivalCurve(survivalCurve)
                .build();
    }



}
