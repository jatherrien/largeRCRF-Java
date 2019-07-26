package ca.joeltherrien.randomforest.tree.vimp;

import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegressionErrorCalculatorTest {

    private final RegressionErrorCalculator calculator = new RegressionErrorCalculator();

    @Test
    public void testRegressionErrorCalculator(){
        final List<Double> responses = Utils.easyList(1.0, 1.5, 0.0, 3.0);
        final List<Double> predictions = Utils.easyList(1.5, 1.7, 0.1, 2.9);

        // Differences are 0.5, 0.2, -0.1, 0.1
        // Squared: 0.25, 0.04, 0.01, 0.01

        assertEquals((0.25 + 0.04 + 0.01 + 0.01)/4.0, calculator.averageError(responses, predictions), 0.000000001);

    }

}
