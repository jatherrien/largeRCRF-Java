package ca.joeltherrien.randomforest.tree.vimp;

import java.util.List;

public class RegressionErrorCalculator implements ErrorCalculator<Double, Double>{

    @Override
    public double averageError(List<Double> responses, List<Double> predictions) {
        double mean = 0.0;
        final double n = responses.size();

        for(int i=0; i<responses.size(); i++){
            final double response = responses.get(i);
            final double prediction = predictions.get(i);

            final double difference = response - prediction;

            mean += difference * difference / n;
        }

        return mean;
    }
}
