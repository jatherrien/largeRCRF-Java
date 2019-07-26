package ca.joeltherrien.randomforest.tree.vimp;

import ca.joeltherrien.randomforest.CovariateRow;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.tree.Forest;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;


public class VariableImportanceCalculator<Y, P> {

    private final ErrorCalculator<Y, P> errorCalculator;
    private final Forest<Y, P> forest;
    private final List<Row<Y>> observations;
    private final List<Y> observedResponses;

    private final boolean isTrainingSet; // If true, then we use out-of-bag predictions
    private final double baselineError;

    public VariableImportanceCalculator(
            ErrorCalculator<Y, P> errorCalculator,
            Forest<Y, P> forest,
            List<Row<Y>> observations,
            boolean isTrainingSet
            ){
        this.errorCalculator = errorCalculator;
        this.forest = forest;
        this.observations = observations;
        this.isTrainingSet = isTrainingSet;

        this.observedResponses = observations.stream()
                .map(row -> row.getResponse()).collect(Collectors.toList());

        final List<P> baselinePredictions = makePredictions(observations);
        this.baselineError = errorCalculator.averageError(observedResponses, baselinePredictions);

    }

    public double calculateVariableImportance(Covariate covariate, Optional<Random> random){
        final List<CovariateRow> scrambledValues = CovariateRow.scrambleCovariateValues(this.observations, covariate, random);
        final List<P> alternatePredictions = makePredictions(scrambledValues);
        final double newError = errorCalculator.averageError(this.observedResponses, alternatePredictions);

        return newError - this.baselineError;
    }

    public double[] calculateVariableImportance(List<Covariate> covariates, Optional<Random> random){
        return covariates.stream()
                .mapToDouble(covariate -> calculateVariableImportance(covariate, random))
                .toArray();
    }

    private List<P> makePredictions(List<? extends CovariateRow> rowList){
        if(isTrainingSet){
            return forest.evaluateOOB(rowList);
        } else{
            return forest.evaluate(rowList);
        }
    }

}
