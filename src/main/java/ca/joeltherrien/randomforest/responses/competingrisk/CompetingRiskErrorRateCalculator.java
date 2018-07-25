package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.VisibleForTesting;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.tree.Tree;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Based on the naive version in Section 3.2 of "Concordance for Prognastic Models with Competing Risks" by Wolbers et al.
 *
 * Note that this is the same version implemented in randomForestSRC. The downsides of this approach is that we can expect the errors to be biased, possibly severely.
 * Therefore I suggest that this measure only be used in comparing models, but not as a final output.
 */
@RequiredArgsConstructor
public class CompetingRiskErrorRateCalculator {

    private final CompetingRiskFunctionCombiner combiner;
    private final int[] events;

    public CompetingRiskErrorRateCalculator(final int[] events, final double[] times){
        this.events = events;
        this.combiner = new CompetingRiskFunctionCombiner(events, times);
    }

    public double[] calculateConcordance(final List<Row<CompetingRiskResponse>> rows, final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest){
        final double tau = rows.stream().mapToDouble(row -> row.getResponse().getU()).max().orElse(0.0);

        return calculateConcordance(rows, forest, tau);
    }

    private double[] calculateConcordance(final List<Row<CompetingRiskResponse>> rows, final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest, final double tau){

        final Collection<Tree<CompetingRiskFunctions>> trees = forest.getTrees();

        // This predicts for rows based on their OOB trees.

        final List<CompetingRiskFunctions> riskFunctions = rows.stream()
                .map(row -> {
                    return trees.stream().filter(tree -> !tree.idInBootstrapSample(row.getId())).map(tree -> tree.evaluate(row)).collect(Collectors.toList());
                })
                .map(combiner::combine)
                .collect(Collectors.toList());


        //final List<CompetingRiskFunctions> riskFunctions = rows.stream().map(row -> forest.evaluate(row)).collect(Collectors.toList());

        final double[] errorRates = new double[events.length];

        final List<CompetingRiskResponse> responses = rows.stream().map(Row::getResponse).collect(Collectors.toList());

        // Let \tau be the max time.

        for(int e=0; e<events.length; e++){
            final int event = events[e];

            final double[] mortalityList = riskFunctions.stream()
                    .map(riskFunction -> riskFunction.getCumulativeIncidenceFunction(event))
                    .mapToDouble(cif -> functionToMortality(cif, tau))
                    .toArray();

            final double concordance = calculateConcordance(responses, mortalityList, event);
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

    private double functionToMortality(final MathFunction cif, final double tau){
        double summation = 0.0;
        Point previousPoint = null;

        for(final Point point : cif.getPoints()){
            if(previousPoint != null){
                summation += previousPoint.getY() * (point.getTime() - previousPoint.getTime());
            }
            previousPoint = point;

        }

        // this is to ensure that we integrate over the same range for every function and get comparable results.
        // Don't need to assert whether previousPoint is null or not; if it is null then the MathFunction was incorrectly made as there will always be at least one point for a response
        summation += previousPoint.getY() * (tau - previousPoint.getTime());

        return summation;

    }


}
