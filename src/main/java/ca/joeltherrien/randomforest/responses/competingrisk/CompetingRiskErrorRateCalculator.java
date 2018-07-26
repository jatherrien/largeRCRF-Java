package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.VisibleForTesting;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.utils.MathFunction;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Based on the naive version in Section 3.2 of "Concordance for Prognastic Models with Competing Risks" by Wolbers et al.
 *
 * Note that this is the same version implemented in randomForestSRC. The downsides of this approach is that we can expect the errors to be biased, possibly severely.
 * Therefore I suggest that this measure only be used in comparing models, but not as a final output.
 */
public class CompetingRiskErrorRateCalculator {

    private final List<Row<CompetingRiskResponse>> dataset;
    private final List<CompetingRiskFunctions> riskFunctions;

    public CompetingRiskErrorRateCalculator(final List<Row<CompetingRiskResponse>> dataset, final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest){
        this.dataset = dataset;
        this.riskFunctions = dataset.stream()
                .map(forest::evaluateOOB)
                .collect(Collectors.toList());
    }

    /**
     * Idea for this error rate; go through every observation I have and calculate its mortality for the different events. If the event with the highest mortality is not the one that happened,
     * then we add one to the error scale.
     *
     * Ignore censored observations.
     *
     * Possible extensions might involve counting how many other events had higher mortality, instead of just a single PASS / FAIL.
     *
     * My observation is that this error rate isn't very useful...
     *
     * @return
     */
    public double calculateNaiveMortalityError(final int[] events){
        int failures = 0;
        int attempts = 0;

        response_loop:
        for(int i=0; i<dataset.size(); i++){
            final CompetingRiskResponse response = dataset.get(i).getResponse();

            if(response.isCensored()){
                continue;
            }
            attempts++;

            final CompetingRiskFunctions functions = riskFunctions.get(i);
            final int delta = response.getDelta();
            final double time = response.getU();
            final double shouldBeHighestMortality = functions.calculateEventSpecificMortality(delta, time);

            for(final int event : events){
                if(event != delta){
                    final double otherEventMortality = functions.calculateEventSpecificMortality(event, time);

                    if(shouldBeHighestMortality < otherEventMortality){
                        failures++;
                        continue response_loop;
                    }

                }
            }
        }

        return (double) failures / (double) attempts;


    }


    public double[] calculateConcordance(final int[] events){
        final double tau = dataset.stream().mapToDouble(row -> row.getResponse().getU()).max().orElse(0.0);

        return calculateConcordance(events, tau);
    }

    private double[] calculateConcordance(final int[] events, final double tau){

        final double[] errorRates = new double[events.length];

        final List<CompetingRiskResponse> responses = dataset.stream().map(Row::getResponse).collect(Collectors.toList());

        // Let \tau be the max time.

        for(int e=0; e<events.length; e++){
            final int event = events[e];

            final double[] mortalityList = riskFunctions.stream()
                    .mapToDouble(riskFunction -> riskFunction.calculateEventSpecificMortality(event, tau))
                    .toArray();

            final double concordance = calculateConcordance(responses, mortalityList, event);
            errorRates[e] = 1.0 - concordance;

        }

        return errorRates;

    }

    public double[] calculateIPCWConcordance(final int[] events, final MathFunction censoringDistribution){
        final double tau = dataset.stream().mapToDouble(row -> row.getResponse().getU()).max().orElse(0.0);

        return calculateIPCWConcordance(events, censoringDistribution, tau);
    }

    private double[] calculateIPCWConcordance(final int[] events, final MathFunction censoringDistribution, final double tau){

        final double[] errorRates = new double[events.length];

        final List<CompetingRiskResponse> responses = dataset.stream().map(Row::getResponse).collect(Collectors.toList());

        // Let \tau be the max time.

        for(int e=0; e<events.length; e++){
            final int event = events[e];

            final double[] mortalityList = riskFunctions.stream()
                    .mapToDouble(riskFunction -> riskFunction.calculateEventSpecificMortality(event, tau))
                    .toArray();

            final double concordance = calculateIPCWConcordance(responses, mortalityList, event, censoringDistribution);
            errorRates[e] = 1.0 - concordance;

        }

        return errorRates;

    }

    @VisibleForTesting
    public double calculateConcordance(final List<CompetingRiskResponse> responseList, double[] mortalityArray, final int event){

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


    @VisibleForTesting
    public double calculateIPCWConcordance(final List<CompetingRiskResponse> responseList, double[] mortalityArray, final int event, final MathFunction censoringDistribution){

        // Let \tau be the max time.

        double denominator = 0.0;
        double numerator = 0.0;

        for(int i = 0; i<mortalityArray.length; i++){
            final CompetingRiskResponse responseI = responseList.get(i);
            if(responseI.getDelta() != event){ // \tilde{N}_i^1(\tau) == 1 check
                continue; // skip if it's 0
            }

            final double mortalityI = mortalityArray[i];

            for(int j=0; j<mortalityArray.length; j++){
                final CompetingRiskResponse responseJ = responseList.get(j);

                final double AijWeightPlusBijWeight;

                if(responseI.getU() < responseJ.getU()){ // Aij == 1
                    final double Ti = responseI.getU();
                    AijWeightPlusBijWeight = 1.0 / (censoringDistribution.evaluate(Ti).getY() * censoringDistribution.evaluatePrevious(Ti).getY());
                }
                else if(responseI.getU() >= responseJ.getU() && !responseJ.isCensored() && responseJ.getDelta() != event){ // Bij == 1
                    AijWeightPlusBijWeight = 1.0 / (censoringDistribution.evaluatePrevious(responseI.getU()).getY() * censoringDistribution.evaluatePrevious(responseJ.getU()).getY());
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
