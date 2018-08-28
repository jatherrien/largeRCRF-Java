package ca.joeltherrien.randomforest.responses.competingrisk.combiner;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.tree.ResponseCombiner;
import ca.joeltherrien.randomforest.utils.MathFunction;
import ca.joeltherrien.randomforest.utils.Point;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                            function -> function.getPoints().stream()
                            .mapToDouble(point -> point.getTime())
                    ).sorted().distinct().toArray();
        }

        final double n = responses.size();

        final List<Point> survivalPoints = new ArrayList<>(timesToUse.length);
        for(final double time : timesToUse){

            final double survivalY = responses.stream()
                    .mapToDouble(functions -> functions.getSurvivalCurve().evaluate(time).getY() / n)
                    .sum();

            survivalPoints.add(new Point(time, survivalY));

        }

        final MathFunction survivalFunction = new MathFunction(survivalPoints, new Point(0.0, 1.0));

        final List<MathFunction> causeSpecificCumulativeHazardFunctionList = new ArrayList<>(events.length);
        final List<MathFunction> cumulativeIncidenceFunctionList = new ArrayList<>(events.length);

        for(final int event : events){

            final List<Point> cumulativeHazardFunctionPoints = new ArrayList<>(timesToUse.length);
            final List<Point> cumulativeIncidenceFunctionPoints = new ArrayList<>(timesToUse.length);

            for(final double time : timesToUse){

                final double hazardY = responses.stream()
                        .mapToDouble(functions -> functions.getCauseSpecificHazardFunction(event).evaluate(time).getY() / n)
                        .sum();

                final double incidenceY = responses.stream()
                        .mapToDouble(functions -> functions.getCumulativeIncidenceFunction(event).evaluate(time).getY() / n)
                        .sum();

                cumulativeHazardFunctionPoints.add(new Point(time, hazardY));
                cumulativeIncidenceFunctionPoints.add(new Point(time, incidenceY));

            }

            causeSpecificCumulativeHazardFunctionList.add(event-1, new MathFunction(cumulativeHazardFunctionPoints));
            cumulativeIncidenceFunctionList.add(event-1, new MathFunction(cumulativeIncidenceFunctionPoints));

        }

        return CompetingRiskFunctions.builder()
                .causeSpecificHazards(causeSpecificCumulativeHazardFunctionList)
                .cumulativeIncidenceCurves(cumulativeIncidenceFunctionList)
                .survivalCurve(survivalFunction)
                .build();
    }
}
