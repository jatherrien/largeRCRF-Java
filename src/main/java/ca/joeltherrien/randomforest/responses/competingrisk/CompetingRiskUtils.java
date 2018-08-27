package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.utils.MathFunction;

import java.util.List;

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


    public static double calculateIPCWConcordance(final List<CompetingRiskResponse> responseList, double[] mortalityArray, final int event, final MathFunction censoringDistribution){

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
            final double G_Ti_minus = censoringDistribution.evaluatePrevious(Ti).getY();
            final double AijWeight = 1.0 / (censoringDistribution.evaluate(Ti).getY() * G_Ti_minus);

            for(int j=0; j<mortalityArray.length; j++){
                final CompetingRiskResponse responseJ = responseList.get(j);

                final double AijWeightPlusBijWeight;

                if(responseI.getU() < responseJ.getU()){ // Aij == 1
                    AijWeightPlusBijWeight = AijWeight;
                }
                else if(responseI.getU() >= responseJ.getU() && !responseJ.isCensored() && responseJ.getDelta() != event){ // Bij == 1
                    AijWeightPlusBijWeight = 1.0 / (G_Ti_minus * censoringDistribution.evaluatePrevious(responseJ.getU()).getY());
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


}
