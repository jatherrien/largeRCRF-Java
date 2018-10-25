package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.utils.LeftContinuousStepFunction;
import ca.joeltherrien.randomforest.utils.StepFunction;
import ca.joeltherrien.randomforest.utils.VeryDiscontinuousStepFunction;

import java.util.*;
import java.util.stream.DoubleStream;

public class CompetingRiskUtils {

    public static double calculateConcordance(final List<CompetingRiskResponse> responseList, double[] mortalityArray, final int event){

        // Let \tau be the max time.

        int permissible = 0;
        double numerator = 0;

        for(int i = 0; i<mortalityArray.length; i++){
            final CompetingRiskResponse responseI = responseList.get(i);
            if(responseI.getDelta() != event){ // \tilde{N}_i^1(\tau) == 1 check
                continue; // skip if it's 0
            }

            final double mortalityI = mortalityArray[i];

            for(int j=0; j<mortalityArray.length; j++){
                final CompetingRiskResponse responseJ = responseList.get(j);
                // Check that Aij or Bij == 1
                if(responseI.getU() < responseJ.getU() || (responseI.getU() >= responseJ.getU() && !responseJ.isCensored() && responseJ.getDelta() != event)){
                    permissible++;

                    final double mortalityJ = mortalityArray[j];
                    if(mortalityI > mortalityJ){
                        numerator += 1.0;
                    }
                    else if(mortalityI == mortalityJ){
                        numerator += 0.5; // Edge case that can happen in trees with only a few BooleanCovariates, when you're looking at training error
                    }

                }

            }

        }

        return numerator / (double) permissible;

    }


    public static double calculateIPCWConcordance(final List<CompetingRiskResponse> responseList,
                                                  double[] mortalityArray, final int event,
                                                  final StepFunction censoringDistribution){

        // Let \tau be the max time.

        double denominator = 0.0;
        double numerator = 0.0;

        for(int i = 0; i<mortalityArray.length; i++){
            final CompetingRiskResponse responseI = responseList.get(i);
            if(responseI.getDelta() != event){ // \tilde{N}_i^1(\tau) == 1 check
                continue; // skip if it's 0
            }

            final double mortalityI = mortalityArray[i];
            final double Ti = responseI.getU();
            final double G_Ti_minus = censoringDistribution.evaluatePrevious(Ti);
            final double AijWeight = 1.0 / (censoringDistribution.evaluate(Ti) * G_Ti_minus);

            for(int j=0; j<mortalityArray.length; j++){
                final CompetingRiskResponse responseJ = responseList.get(j);

                final double AijWeightPlusBijWeight;

                if(responseI.getU() < responseJ.getU()){ // Aij == 1
                    AijWeightPlusBijWeight = AijWeight;
                }
                else if(responseI.getU() >= responseJ.getU() && !responseJ.isCensored() && responseJ.getDelta() != event){ // Bij == 1
                    AijWeightPlusBijWeight = 1.0 / (G_Ti_minus * censoringDistribution.evaluatePrevious(responseJ.getU()));
                }
                else{
                    continue;
                }

                denominator += AijWeightPlusBijWeight;

                final double mortalityJ = mortalityArray[j];
                if(mortalityI > mortalityJ){
                    numerator += AijWeightPlusBijWeight*1.0;
                }
                else if(mortalityI == mortalityJ){
                    numerator += AijWeightPlusBijWeight*0.5; // Edge case that can happen in trees with only a few BooleanCovariates, when you're looking at training error
                }

            }

        }

        return numerator / denominator;

    }

    public static CompetingRiskSetsImpl calculateSetsEfficiently(final List<CompetingRiskResponse> responses, int[] eventsOfFocus){
        final int n = responses.size();
        int[] numberOfCurrentEvents = new int[eventsOfFocus.length+1];

        final Map<Double, int[]> numberOfEvents = new HashMap<>();

        final List<Double> eventTimes = new ArrayList<>(n);
        final List<Double> eventAndCensorTimes = new ArrayList<>(n);
        final List<Integer> riskSetNumberList = new ArrayList<>(n);

        // need to first sort responses
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



        for(int i=0; i<n; i++){
            final CompetingRiskResponse currentResponse = responses.get(i);
            final boolean lastOfTime = (i+1)==n || responses.get(i+1).getU() > currentResponse.getU();

            numberOfCurrentEvents[currentResponse.getDelta()]++;

            if(lastOfTime){
                int totalNumberOfCurrentEvents = 0;
                for(int e = 1; e < numberOfCurrentEvents.length; e++){ // exclude censored events
                    totalNumberOfCurrentEvents += numberOfCurrentEvents[e];
                }

                final double currentTime = currentResponse.getU();

                if(totalNumberOfCurrentEvents > 0){ // add numberOfCurrentEvents
                    // Add point
                    eventTimes.add(currentTime);
                    numberOfEvents.put(currentTime, numberOfCurrentEvents);
                }

                // Always do risk set
                // remember that the LeftContinuousFunction takes into account that at this currentTime the risk value is the previous value
                final int riskSet = n - (i+1);
                riskSetNumberList.add(riskSet);
                eventAndCensorTimes.add(currentTime);

                // reset counters
                numberOfCurrentEvents = new int[eventsOfFocus.length+1];

            }

        }

        final double[] riskSetArray = new double[eventAndCensorTimes.size()];
        final double[] timesArray = new double[eventAndCensorTimes.size()];
        for(int i=0; i<riskSetArray.length; i++){
            timesArray[i] = eventAndCensorTimes.get(i);
            riskSetArray[i] = riskSetNumberList.get(i);
        }

        final LeftContinuousStepFunction riskSetFunction = new LeftContinuousStepFunction(timesArray, riskSetArray, n);

        return CompetingRiskSetsImpl.builder()
                .numberOfEvents(numberOfEvents)
                .riskSet(riskSetFunction)
                .eventTimes(eventTimes)
                .build();

    }

    public static CompetingRiskGraySetsImpl calculateGraySetsEfficiently(final List<CompetingRiskResponseWithCensorTime> responses, int[] eventsOfFocus){
        final List sillyList = responses; // annoying Java generic work-around
        final CompetingRiskSetsImpl originalSets = calculateSetsEfficiently(sillyList, eventsOfFocus);

        final double[] allTimes = DoubleStream.concat(
                responses.stream()
                        .mapToDouble(CompetingRiskResponseWithCensorTime::getC),
                responses.stream()
                        .mapToDouble(CompetingRiskResponseWithCensorTime::getU)
        ).sorted().distinct().toArray();



        final VeryDiscontinuousStepFunction[] riskSets = new VeryDiscontinuousStepFunction[eventsOfFocus.length];

        for(final int event : eventsOfFocus){
            final double[] yAt = new double[allTimes.length];
            final double[] yRight = new double[allTimes.length];

            for(final CompetingRiskResponseWithCensorTime response : responses){
                if(response.getDelta() == event){
                    // traditional case only; increment on time t when I(t <= Ui)
                    final double time = response.getU();
                    final int index = Arrays.binarySearch(allTimes, time);

                    if(index < 0){ // TODO remove once code is stable
                        throw new IllegalStateException("Index shouldn't be negative!");
                    }

                    // All yAts up to and including index are incremented;
                    // All yRights up to index are incremented
                    yAt[index]++;
                    for(int i=0; i<index; i++){
                        yAt[i]++;
                        yRight[i]++;
                    }
                }
                else{
                    // need to increment on time t on following conditions; I(t <= Ui | t < Ci)
                    // Fact: Ci >= Ui.

                    // increment yAt up to Ci. If Ui==Ci, increment yAt at Ci.
                    final double time = response.getC();
                    final int index = Arrays.binarySearch(allTimes, time);

                    if(index < 0){ // TODO remove once code is stable
                        throw new IllegalStateException("Index shouldn't be negative!");
                    }

                    for(int i=0; i<index; i++){
                        yAt[i]++;
                        yRight[i]++;
                    }
                    if(response.getU() == response.getC()){
                        yAt[index]++;
                    }

                }
            }

            riskSets[event-1] = new VeryDiscontinuousStepFunction(allTimes, yAt, yRight, responses.size());

        }

        return CompetingRiskGraySetsImpl.builder()
                .numberOfEvents(originalSets.getNumberOfEvents())
                .eventTimes(originalSets.getEventTimes())
                .riskSet(riskSets)
                .build();

    }


}
