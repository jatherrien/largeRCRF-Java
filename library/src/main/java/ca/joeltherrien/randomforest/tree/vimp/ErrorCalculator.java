package ca.joeltherrien.randomforest.tree.vimp;

import java.util.List;

/**
 * Simple interface for VariableImportanceCalculator; takes in a List of observed responses and a List of predictions
 * and produces an average error measure.
 *
 * @param <Y> The class of the responses.
 * @param <P> The class of the predictions.
 */
public interface ErrorCalculator<Y, P>{

    /**
     * Compares the observed responses with the predictions to produce an average error measure.
     * Lower errors should indicate a better model fit.
     *
     * @param responses
     * @param predictions
     * @return
     */
    double averageError(List<Y> responses, List<P> predictions);

}
