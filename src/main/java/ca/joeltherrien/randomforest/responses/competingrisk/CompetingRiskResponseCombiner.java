package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.tree.ResponseCombiner;
import ca.joeltherrien.randomforest.utils.MathFunction;
import ca.joeltherrien.randomforest.utils.Point;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * This class takes all of the observations in a terminal node and combines them to produce estimates of the cause-specific hazard function
 * and the cumulative incidence curve.
 *
 * See https://kogalur.github.io/randomForestSRC/theory.html for details.
 *
 */
@RequiredArgsConstructor
public class CompetingRiskResponseCombiner implements ResponseCombiner<CompetingRiskResponse, CompetingRiskFunctions> {

    private final int[] events;
    private final double[] times; // We may restrict ourselves to specific times.

    @Override
    public CompetingRiskFunctions combine(List<CompetingRiskResponse> responses) {

        final Map<Integer, MathFunction> causeSpecificCumulativeHazardFunctionMap = new HashMap<>();
        final Map<Integer, MathFunction> cumulativeIncidenceFunctionMap = new HashMap<>();

        final double[] timesToUse;
        if(times != null){
            timesToUse = this.times;
        }
        else{
            timesToUse = responses.stream()
                    .filter(response -> !response.isCensored())
                    .mapToDouble(response -> response.getU())
                    .sorted().distinct()
                    .toArray();
        }

        final double[] individualsAtRiskArray = Arrays.stream(timesToUse).map(time -> riskSet(responses, time)).toArray();

        // First we need to develop the overall survival curve!
        final List<Point> survivalPoints = new ArrayList<>(timesToUse.length);
        double previousSurvivalValue = 1.0;
        for(int i=0; i<timesToUse.length; i++){
            final double time_k = timesToUse[i];
            final double individualsAtRisk = individualsAtRiskArray[i]; // Y(t_k)
            final double numberOfEventsAtTime = (double) responses.stream()
                    .filter(event -> !event.isCensored())
                    .filter(event -> event.getU() == time_k) // since delta != 0 we know censoring didn't occur prior to this
                    .count();

            final double newValue = previousSurvivalValue * (1.0 - numberOfEventsAtTime / individualsAtRisk);
            survivalPoints.add(new Point(time_k, newValue));
            previousSurvivalValue = newValue;

        }

        final MathFunction survivalCurve = new MathFunction(survivalPoints, new Point(0.0, 1.0));


        for(final int event : events){

            final List<Point> hazardFunctionPoints = new ArrayList<>(timesToUse.length);
            Point previousHazardFunctionPoint = new Point(0.0, 0.0);

            final List<Point> cifPoints = new ArrayList<>(timesToUse.length);
            Point previousCIFPoint = new Point(0.0, 0.0);

            for(int i=0; i<timesToUse.length; i++){
                final double time_k = timesToUse[i];
                final double individualsAtRisk = individualsAtRiskArray[i]; // Y(t_k)
                final double numberEventsAtTime = numberOfEventsAtTime(event, responses, time_k); // d_j(t_k)

                // Cause-specific cumulative hazard function
                final double hazardDeltaY = numberEventsAtTime / individualsAtRisk;
                final Point newHazardPoint = new Point(time_k, previousHazardFunctionPoint.getY() + hazardDeltaY);
                hazardFunctionPoints.add(newHazardPoint);
                previousHazardFunctionPoint = newHazardPoint;


                // Cumulative incidence function
                // TODO - confirm this behaviour
                //final double previousSurvivalEvaluation = i > 0 ? survivalCurve.evaluate(timesToUse[i-1]).getY() : survivalCurve.evaluate(0.0).getY();
                final double previousSurvivalEvaluation = i > 0 ? survivalCurve.evaluate(timesToUse[i-1]).getY() : 1.0;

                final double cifDeltaY = previousSurvivalEvaluation * (numberEventsAtTime / individualsAtRisk);
                final Point newCIFPoint = new Point(time_k, previousCIFPoint.getY() + cifDeltaY);
                cifPoints.add(newCIFPoint);
                previousCIFPoint = newCIFPoint;

            }

            final MathFunction causeSpecificCumulativeHazardFunction = new MathFunction(hazardFunctionPoints);
            causeSpecificCumulativeHazardFunctionMap.put(event, causeSpecificCumulativeHazardFunction);

            final MathFunction cifFunction = new MathFunction(cifPoints);
            cumulativeIncidenceFunctionMap.put(event, cifFunction);
        }


        return CompetingRiskFunctions.builder()
                .causeSpecificHazardFunctionMap(causeSpecificCumulativeHazardFunctionMap)
                .cumulativeIncidenceFunctionMap(cumulativeIncidenceFunctionMap)
                .survivalCurve(survivalCurve)
                .build();
    }


    private double riskSet(List<CompetingRiskResponse> eventList, double time) {
        return eventList.stream()
                .filter(event -> event.getU() >= time)
                .count();
    }

    private double numberOfEventsAtTime(int eventOfFocus, List<CompetingRiskResponse> eventList, double time){
        return (double) eventList.stream()
                .filter(event -> event.getDelta() == eventOfFocus)
                .filter(event -> event.getU() == time) // since delta != 0 we know censoring didn't occur prior to this
                .count();

    }

}
