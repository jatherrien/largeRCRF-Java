package ca.joeltherrien.randomforest.responses.competingrisk.combiner;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.tree.IntermediateCombinedResponse;
import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;
import ca.joeltherrien.randomforest.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class IntermediateCompetingRisksFunctionsTimesKnown implements IntermediateCombinedResponse<CompetingRiskFunctions, CompetingRiskFunctions> {

    private double expectedN;
    private final int[] events;
    private final double[] timesToUse;
    private int actualN;

    private final double[] survivalY;
    private final double[][] csCHFY;
    private final double[][] cifY;

    public IntermediateCompetingRisksFunctionsTimesKnown(int n, int[] events, double[] timesToUse){
        this.expectedN = n;
        this.events = events;
        this.timesToUse = timesToUse;
        this.actualN = 0;

        this.survivalY = new double[timesToUse.length];
        this.csCHFY = new double[events.length][timesToUse.length];
        this.cifY = new double[events.length][timesToUse.length];
    }

    @Override
    public void processNewInput(CompetingRiskFunctions input) {
        /*
            We're going to try to efficiently put our predictions together -
                Assumptions - for each event on a response, the hazard and CIF functions share the same x points

            Plan - go through the time on each response and make use of that so that when we search for a time index
            to evaluate the function at, we don't need to re-search the earlier times.

         */

        this.actualN++;

        final double[] survivalXPoints = input.getSurvivalCurve().getX();
        final double[][] eventSpecificXPoints = new double[events.length][];

        for(final int event : events){
            eventSpecificXPoints[event-1] = input.getCumulativeIncidenceFunction(event)
                    .getX();
        }

        int previousSurvivalIndex = 0;
        final int[] previousEventSpecificIndex = new int[events.length]; // relying on 0 being default value

        for(int i=0; i<timesToUse.length; i++){
            final double time = timesToUse[i];

            // Survival curve
            final int survivalTimeIndex = Utils.binarySearchLessThan(previousSurvivalIndex, survivalXPoints.length, survivalXPoints, time);
            survivalY[i] = survivalY[i] + input.getSurvivalCurve().evaluateByIndex(survivalTimeIndex) / expectedN;
            previousSurvivalIndex = Math.max(survivalTimeIndex, 0); // if our current time is less than the smallest time in xPoints then binarySearchLessThan returned a -1.
            // -1's not an issue for evaluateByIndex, but it is an issue for the next time binarySearchLessThan is called.

            // CHFs and CIFs
            for(final int event : events){
                final double[] xPoints = eventSpecificXPoints[event-1];
                final int eventTimeIndex = Utils.binarySearchLessThan(previousEventSpecificIndex[event-1], xPoints.length,
                        xPoints, time);
                csCHFY[event-1][i] = csCHFY[event-1][i] + input.getCauseSpecificHazardFunction(event)
                        .evaluateByIndex(eventTimeIndex) / expectedN;
                cifY[event-1][i] = cifY[event-1][i] + input.getCumulativeIncidenceFunction(event)
                        .evaluateByIndex(eventTimeIndex) / expectedN;

                previousEventSpecificIndex[event-1] = Math.max(eventTimeIndex, 0);
            }
        }
    }

    @Override
    public CompetingRiskFunctions transformToOutput() {
        rescaleOutput();

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

    private void rescaleOutput() {
        rescaleArray(actualN, this.survivalY);

        for(int event : events){
            rescaleArray(actualN, this.cifY[event - 1]);
            rescaleArray(actualN, this.csCHFY[event - 1]);
        }

        this.expectedN = actualN;

    }

    private void rescaleArray(double newN, double[] array){
        for(int i=0; i<array.length; i++){
            array[i] = array[i] * (this.expectedN / newN);
        }
    }
}
