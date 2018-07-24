package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.VisibleForTesting;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.tree.Tree;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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

    public double[] calculateAll(final List<Row<CompetingRiskResponse>> rows, final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest){

        final Collection<Tree<CompetingRiskFunctions>> trees = forest.getTrees();

        // This predicts for rows based on their OOB trees.
        final List<CompetingRiskFunctions> riskFunctions = rows.stream()
                .map(row -> {
                    return trees.stream().filter(tree -> !tree.idInBootstrapSample(row.getId())).map(tree -> tree.evaluate(row)).collect(Collectors.toList());
                })
                .map(list -> combiner.combine(list))
                .collect(Collectors.toList());

        final double[] errorRates = new double[events.length];

        final List<CompetingRiskResponse> responses = rows.stream().map(row -> row.getResponse()).collect(Collectors.toList());

        // Let \tau be the max time.

        for(int e=0; e<events.length; e++){
            final int event = events[e];

            final double[] mortalityList = riskFunctions.stream()
                    .map(riskFunction -> riskFunction.getCumulativeIncidenceFunction(event))
                    .mapToDouble(cif -> functionToMortality(cif))
                    .toArray();

            final double concordance = calculate(responses, mortalityList, event);
            errorRates[e] = 1.0 - concordance;

        }

        return errorRates;

    }

    @VisibleForTesting
    public double calculate(final List<CompetingRiskResponse> responseList, final double[] mortalityArray, final int event){

        // Let \tau be the max time.

        int permissible = 0;
        int numerator = 0;

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
                    numerator += mortalityI > mortalityJ ? 1 : 0;

                }

            }

        }

        return (double) numerator / (double) permissible;


    }

    /*
    public double[] calculateAll(final List<Row<CompetingRiskResponse>> rows, final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest){
        rows.sort(Comparator.comparing(row -> row.getResponse().getU())); // optimization for later loop

        final Collection<Tree<CompetingRiskFunctions>> trees = forest.getTrees();

        final List<CompetingRiskFunctions> riskFunctions = rows.stream()
                .map(row -> {
                    return trees.stream().filter(tree -> !tree.idInBootstrapSample(row.getId())).map(tree -> tree.evaluate(row)).collect(Collectors.toList());
                })
                .map(list -> combiner.combine(list))
                .collect(Collectors.toList());

        final double[] errorRates = new double[events.length];


        for(int e=0; e<events.length; e++){
            final int event = events[e];

            final double[] mortalityList = riskFunctions.stream()
                    .map(riskFunction -> riskFunction.getCumulativeIncidenceFunction(event))
                    .mapToDouble(cif -> functionToMortality(cif))
                    .toArray();

            int permissible = 0;
            double c = 0.0;

            outer_mortality:
            for(int i = 0; i< mortalityList.length; i++){
                final Row<CompetingRiskResponse> leftRow = rows.get(i);
                final double mortalityLeft = mortalityList[i];
                final CompetingRiskResponse leftResponse = leftRow.getResponse();

                for(int j=i+1; j<mortalityList.length; j++){

                    final Row<CompetingRiskResponse> rightRow = rows.get(j);
                    final double mortalityRight = mortalityList[j];
                    final CompetingRiskResponse rightResponse = rightRow.getResponse();

                    if(leftResponse.getDelta() != event && rightResponse.getU() > leftResponse.getU()){
                        // because we've sorted the responses earlier we will never get a permissable result for greater j.
                        continue outer_mortality;
                    }

                    // check and see if pair is permissable
                    if(isPermissablePair(leftResponse, rightResponse, event)){
                        permissible++;

                        final double comparisonScore = compare(leftResponse, rightResponse, event);
                        if(comparisonScore < 0) { // left > right
                            // right has shorter time
                            if(mortalityRight > mortalityLeft){
                                c += 1.0;
                            }
                            else if(mortalityRight == mortalityLeft){
                                c += 0.5;
                            }
                        }
                        else if(comparisonScore > 0){ // left < right
                            // left has shorter term
                            if(mortalityRight < mortalityLeft){
                                c += 1.0;
                            }
                            else if(mortalityRight == mortalityLeft){
                                c += 0.5;
                            }
                        }
                        else{ // comparisonScore == 0
                            c += (mortalityLeft == mortalityRight) ? 1.0 : 0.5;
                        }

                    }
                    else{
                        continue;
                    }

                }

            }

            final double concordance = c / (double) permissible;
            errorRates[e] = 1.0 - concordance;


        }


        return errorRates;
    }
*/

    /*
    private boolean isPermissablePair(final CompetingRiskResponse left, final CompetingRiskResponse right){
        if(left.isCensored() && right.isCensored()){
            return false;
        }

        if(left.getU() < right.getU() && left.isCensored()){
            return false;
        }

        if(left.getU() > right.getU() && right.isCensored()){
            return false;
        }

        return true;

    }
    */

    /*
    private boolean isPermissablePair(final CompetingRiskResponse left, final CompetingRiskResponse right, int event){
        if(left.getDelta() != event && right.getDelta() != event){
            return false;
        }

        if(left.getU() < right.getU() && left.getDelta() != event){
            return false;
        }

        if(left.getU() > right.getU() && right.getDelta() != event){
            return false;
        }

        return true;

    }
    */


    private double functionToMortality(final MathFunction cif){
        double summation = 0.0;
        double previousTime = 0.0;

        for(final Point point : cif.getPoints()){
            summation += point.getY() * (point.getTime() - previousTime);
            previousTime = point.getTime();
        }

        return summation;

    }


    /**
     * Compare two CompetingRiskResponses to see which is larger than the other (if it can be determined).
     *
     * @param left
     * @param right
     * @param event Event of interest. All other events are treated as censoring.
     * @return -1 if left is strictly greater than right, -0.5 if left is greater than right, 0 if both are equal, 0.5 if right is greater than left, and 1 if right is strictly greater than left.
     *//*
    @VisibleForTesting
    public double compare(final CompetingRiskResponse left, final CompetingRiskResponse right, int event){


        if(left.getU() > right.getU() && right.getDelta()==event){
            // left is greater
            return -1;
        }
        else if(right.getU() > left.getU() && left.getDelta()==event){
            // right is greater
            return 1;
        }
        else if(left.getU() == right.getU() && left.getDelta()==event && right.getDelta()==event){
            // they are equal
            return 0;
        }
        else if(left.getU() == right.getU() && left.getDelta()!=event && right.getDelta()==event){
            // left is greater (note; could be unknown depending on definitions)
            //return -0.5;
            return 0;
        }
        else if(left.getU() == right.getU() && left.getDelta()==event && right.getDelta()!=event){
            // right is greater (note; could be unknown depending on definitions)
            //return 0.5;
            return 0;
        }
        else{
            throw new IllegalArgumentException("Invalid comparison of " + left + " and " + right + "; did you call isPermissablePair first?");
        }


    }*/

}
