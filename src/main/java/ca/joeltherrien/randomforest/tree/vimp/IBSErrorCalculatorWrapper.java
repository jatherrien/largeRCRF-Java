package ca.joeltherrien.randomforest.tree.vimp;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.IBSCalculator;
import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;

import java.util.Arrays;
import java.util.List;

/**
 * Implements ErrorCalculator; essentially just wraps around IBSCalculator to fit into VariableImportanceCalculator.
 *
 */
public class IBSErrorCalculatorWrapper implements ErrorCalculator<CompetingRiskResponse, CompetingRiskFunctions> {

    private final IBSCalculator calculator;
    private final int[] events;
    private final double integrationUpperBound;
    private final double[] eventWeights;

    public IBSErrorCalculatorWrapper(IBSCalculator calculator, int[] events, double integrationUpperBound, double[] eventWeights) {
        this.calculator = calculator;
        this.events = events;
        this.integrationUpperBound = integrationUpperBound;
        this.eventWeights = eventWeights;
    }

    public IBSErrorCalculatorWrapper(IBSCalculator calculator, int[] events, double integrationUpperBound) {
        this.calculator = calculator;
        this.events = events;
        this.integrationUpperBound = integrationUpperBound;
        this.eventWeights = new double[events.length];

        Arrays.fill(this.eventWeights, 1.0); // default is to just sum all errors together

    }

    @Override
    public double averageError(List<CompetingRiskResponse> responses, List<CompetingRiskFunctions> predictions) {
        final double[] errors = new double[events.length];
        final double n = responses.size();

        for(int i=0; i < responses.size(); i++){
            final CompetingRiskResponse response = responses.get(i);
            final CompetingRiskFunctions prediction = predictions.get(i);

            for(int k=0; k < this.events.length; k++){
                final int event = this.events[k];
                final RightContinuousStepFunction cif = prediction.getCumulativeIncidenceFunction(event);
                errors[k] += calculator.calculateError(response, cif, event, integrationUpperBound) / n;
            }

        }

        double totalError = 0.0;
        for(int k=0; k < this.events.length; k++){
            totalError += this.eventWeights[k] * errors[k];
        }

        return totalError;
    }
}
