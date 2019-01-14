package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.utils.StepFunction;

import java.util.*;
import java.util.stream.Stream;

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


    public static CompetingRiskSetsImpl calculateSetsEfficiently(final List<CompetingRiskResponse> initialLeftHand,
                                                                 final List<CompetingRiskResponse> initialRightHand,
                                                                 int[] eventsOfFocus,
                                                                 boolean calculateRiskSets){

        final double[] distinctEventTimes = Stream.concat(
                initialLeftHand.stream(),
                initialRightHand.stream())
                //.filter(y -> !y.isCensored())
                .map(CompetingRiskResponse::getU)
                .mapToDouble(Double::doubleValue)
                .sorted()
                .distinct()
                .toArray();


        final int m = distinctEventTimes.length;
        final int[][] numberOfCurrentEventsTotal = new int[eventsOfFocus.length+1][m];

        // Left Hand First

        // need to first sort responses
        Collections.sort(initialLeftHand, (y1, y2) -> {
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

        final int nLeft = initialLeftHand.size();
        final int nRight = initialRightHand.size();

        final int[][] numberOfCurrentEventsLeft = new int[eventsOfFocus.length+1][m];
        final int[] riskSetArrayLeft = new int[m];
        final int[] riskSetArrayTotal = new int[m];



        for(int k=0; k<m; k++){
            riskSetArrayLeft[k] = nLeft;
            riskSetArrayTotal[k] = nLeft + nRight;
        }

        // Left Hand
        for(int i=0; i<nLeft; i++){
            final CompetingRiskResponse currentResponse = initialLeftHand.get(i);
            final boolean lastOfTime = (i+1)==nLeft || initialLeftHand.get(i+1).getU() > currentResponse.getU();

            final int k = Arrays.binarySearch(distinctEventTimes, currentResponse.getU());

            numberOfCurrentEventsLeft[currentResponse.getDelta()][k]++;
            numberOfCurrentEventsTotal[currentResponse.getDelta()][k]++;

            if(lastOfTime){
                int totalNumberOfCurrentEvents = 0;
                for(int e = 1; e < eventsOfFocus.length+1; e++){ // exclude censored events
                    totalNumberOfCurrentEvents += numberOfCurrentEventsLeft[e][k];
                }

                // Calculate risk set values
                // Note that we only decrease values in the *future*
                if(calculateRiskSets){
                    final int decreaseBy = totalNumberOfCurrentEvents + numberOfCurrentEventsLeft[0][k];
                    for(int j=k+1; j<m; j++){
                        riskSetArrayLeft[j] = riskSetArrayLeft[j] - decreaseBy;
                        riskSetArrayTotal[j] = riskSetArrayTotal[j] - decreaseBy;

                    }
                }


            }

        }


        // Right Hand Next. Note that we only need to keep track of the Left Hand and the Total

        // need to first sort responses
        Collections.sort(initialRightHand, (y1, y2) -> {
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

        // Right Hand
        int[] currentEventsRight = new int[eventsOfFocus.length+1];
        for(int i=0; i<nRight; i++){
            final CompetingRiskResponse currentResponse = initialRightHand.get(i);
            final boolean lastOfTime = (i+1)==nRight || initialRightHand.get(i+1).getU() > currentResponse.getU();

            final int k = Arrays.binarySearch(distinctEventTimes, currentResponse.getU());

            currentEventsRight[currentResponse.getDelta()]++;
            numberOfCurrentEventsTotal[currentResponse.getDelta()][k]++;

            if(lastOfTime){
                int totalNumberOfCurrentEvents = 0;
                for(int e = 1; e < eventsOfFocus.length+1; e++){ // exclude censored events
                    totalNumberOfCurrentEvents += currentEventsRight[e];
                }

                // Calculate risk set values
                // Note that we only decrease values in the *future*
                if(calculateRiskSets){
                    final int decreaseBy = totalNumberOfCurrentEvents + currentEventsRight[0];
                    for(int j=k+1; j<m; j++){
                        riskSetArrayTotal[j] = riskSetArrayTotal[j] - decreaseBy;
                    }
                }

                // Reset
                currentEventsRight = new int[eventsOfFocus.length+1];

            }

        }

        return new CompetingRiskSetsImpl(distinctEventTimes, riskSetArrayLeft, riskSetArrayTotal, numberOfCurrentEventsLeft, numberOfCurrentEventsTotal);

    }


    public static CompetingRiskGraySetsImpl calculateGraySetsEfficiently(final List<CompetingRiskResponseWithCensorTime> initialLeftHand,
                                                                         final List<CompetingRiskResponseWithCensorTime> initialRightHand,
                                                                         int[] eventsOfFocus){

        final List leftHandGenericsSuck = initialLeftHand;
        final List rightHandGenericsSuck = initialRightHand;

        final CompetingRiskSetsImpl normalSets = calculateSetsEfficiently(
                leftHandGenericsSuck,
                rightHandGenericsSuck,
                eventsOfFocus, false);

        final double[] times = normalSets.times;
        final int[][] numberOfEventsLeft = normalSets.numberOfEventsLeft;
        final int[][] numberOfEventsTotal = normalSets.numberOfEventsTotal;

        // FYI; initialLeftHand and initialRightHand have both now been sorted
        // Time to calculate the Gray modified risk sets
        final int[][] riskSetsLeft = new int[eventsOfFocus.length][times.length];
        final int[][] riskSetsTotal = new int[eventsOfFocus.length][times.length];

        // Left hand first
        for(final CompetingRiskResponseWithCensorTime response : initialLeftHand){
            final double time = response.getU();
            final int k = Arrays.binarySearch(times, time);
            final int delta_m_1 = response.getDelta() - 1;
            final double censorTime = response.getC();

            for(int j=0; j<eventsOfFocus.length; j++){
                final int[] riskSetLeftJ = riskSetsLeft[j];
                final int[] riskSetTotalJ = riskSetsTotal[j];

                // first iteration; perform normal increment as if Y is normal
                // corresponds to the first part, U_i >= t, in I(...)
                for(int i=0; i<=k; i++){
                    riskSetLeftJ[i]++;
                    riskSetTotalJ[i]++;
                }

                // second iteration; only if delta-1 != j
                // corresponds to the second part, U_i < t & delta_i != j & C_i > t
                if(delta_m_1 != j && !response.isCensored()){
                    int i = k+1;
                    while(i < times.length && times[i] < censorTime){
                        riskSetLeftJ[i]++;
                        riskSetTotalJ[i]++;
                        i++;
                    }
                }

            }

        }

        // Repeat for right hand
        for(final CompetingRiskResponseWithCensorTime response : initialRightHand){
            final double time = response.getU();
            final int k = Arrays.binarySearch(times, time);
            final int delta_m_1 = response.getDelta() - 1;
            final double censorTime = response.getC();

            for(int j=0; j<eventsOfFocus.length; j++){
                final int[] riskSetTotalJ = riskSetsTotal[j];

                // first iteration; perform normal increment as if Y is normal
                // corresponds to the first part, U_i >= t, in I(...)
                for(int i=0; i<=k; i++){
                    riskSetTotalJ[i]++;
                }

                // second iteration; only if delta-1 != j
                // corresponds to the second part, U_i < t & delta_i != j & C_i > t
                if(delta_m_1 != j && !response.isCensored()){
                    int i = k+1;
                    while(i < times.length && times[i] < censorTime){
                        riskSetTotalJ[i]++;
                        i++;
                    }
                }

            }

        }

        return new CompetingRiskGraySetsImpl(times, riskSetsLeft, riskSetsTotal, numberOfEventsLeft, numberOfEventsTotal);

    }


}
