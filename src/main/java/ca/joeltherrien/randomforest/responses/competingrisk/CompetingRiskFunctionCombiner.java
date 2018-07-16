package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.tree.ResponseCombiner;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CompetingRiskFunctionCombiner implements ResponseCombiner<CompetingRiskFunctions, CompetingRiskFunctions> {

    private final int[] events;
    private final double[] times; // We may restrict ourselves to specific times.

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
        final Map<Integer, MathFunction> causeSpecificCumulativeHazardFunctionMap = new HashMap<>();
        final Map<Integer, MathFunction> cumulativeIncidenceFunctionMap = new HashMap<>();

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

            causeSpecificCumulativeHazardFunctionMap.put(event, new MathFunction(cumulativeHazardFunctionPoints));
            cumulativeIncidenceFunctionMap.put(event, new MathFunction(cumulativeIncidenceFunctionPoints));

        }

        return CompetingRiskFunctions.builder()
                .causeSpecificHazardFunctionMap(causeSpecificCumulativeHazardFunctionMap)
                .cumulativeIncidenceFunctionMap(cumulativeIncidenceFunctionMap)
                .survivalCurve(survivalFunction)
                .build();
    }
}
