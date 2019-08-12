package ca.joeltherrien.randomforest.tree.vimp;

import ca.joeltherrien.randomforest.CovariateRow;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.tree.Tree;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;


public class VariableImportanceCalculator<Y, P> {

    private final ErrorCalculator<Y, P> errorCalculator;
    private final List<Tree<P>> trees;
    private final List<Row<Y>> observations;

    private final boolean isTrainingSet; // If true, then we use out-of-bag predictions
    private final double[] baselineErrors;

    public VariableImportanceCalculator(
            ErrorCalculator<Y, P> errorCalculator,
            List<Tree<P>> trees,
            List<Row<Y>> observations,
            boolean isTrainingSet
            ){
        this.errorCalculator = errorCalculator;
        this.trees = trees;
        this.observations = observations;
        this.isTrainingSet = isTrainingSet;


        try {

            this.baselineErrors = new double[trees.size()];
            for (int i = 0; i < baselineErrors.length; i++) {
                final Tree<P> tree = trees.get(i);
                final List<Row<Y>> oobSubset = getAppropriateSubset(observations, tree); // may not actually be OOB depending on isTrainingSet
                final List<Y> responses = oobSubset.stream().map(Row::getResponse).collect(Collectors.toList());

                this.baselineErrors[i] = errorCalculator.averageError(responses, makePredictions(oobSubset, tree));
            }

        } catch(Exception e){
            e.printStackTrace();
            throw e;
        }

    }

    /**
     * Returns an array of importance values for every Tree for the given Covariate.
     *
     * @param covariate The Covariate to scramble.
     * @param random
     * @return
     */
    public double[] calculateVariableImportanceRaw(Covariate covariate, Optional<Random> random){

        final double[] vimp = new double[trees.size()];
        for(int i = 0; i < vimp.length; i++){
            final Tree<P> tree = trees.get(i);
            final List<Row<Y>> oobSubset = getAppropriateSubset(observations, tree); // may not actually be OOB depending on isTrainingSet
            final List<Y> responses = oobSubset.stream().map(Row::getResponse).collect(Collectors.toList());
            final List<CovariateRow> scrambledValues = CovariateRow.scrambleCovariateValues(oobSubset, covariate, random);

            final double error = errorCalculator.averageError(responses, makePredictions(scrambledValues, tree));

            vimp[i] = error - this.baselineErrors[i];
        }

        return vimp;
    }

    public double calculateVariableImportanceZScore(Covariate covariate, Optional<Random> random){
        final double[] vimpArray = calculateVariableImportanceRaw(covariate, random);

        double mean = 0.0;
        double variance = 0.0;
        final double numTrees = vimpArray.length;

        for(double vimp : vimpArray){
            mean += vimp / numTrees;
        }
        for(double vimp : vimpArray){
            variance += (vimp - mean)*(vimp - mean) / (numTrees - 1.0);
        }

        final double standardError = Math.sqrt(variance / numTrees);

        return mean / standardError;
    }



    // Assume rowList has already been filtered for OOB
    private List<P> makePredictions(List<? extends CovariateRow> rowList, Tree<P> tree){
        return rowList.stream()
                .map(tree::evaluate)
                .collect(Collectors.toList());
    }

    private List<Row<Y>> getAppropriateSubset(List<Row<Y>> initialList, Tree<P> tree){
        if(!isTrainingSet){
            return initialList; // no need to make any subsets
        }

        return initialList.stream()
                .filter(row -> !tree.idInBootstrapSample(row.getId()))
                .collect(Collectors.toList());

    }


}
