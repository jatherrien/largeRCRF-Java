package ca.joeltherrien.randomforest.responses.competingrisk.combiner;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.tree.ResponseCombiner;
import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;
import ca.joeltherrien.randomforest.utils.StepFunction;
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

        final StepFunction survivalFunction = new RightContinuousStepFunction(timesToUse, survivalY, 1.0);

        final List<StepFunction> causeSpecificCumulativeHazardFunctionList = new ArrayList<>(events.length);
        final List<StepFunction> cumulativeIncidenceFunctionList = new ArrayList<>(events.length);

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
