package ca.joeltherrien.randomforest.tree.vimp;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.IBSCalculator;
import ca.joeltherrien.randomforest.utils.Point;
import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IBSErrorCalculatorWrapperTest {

    /*
        We already have tests for the IBSCalculator, so these tests are concerned with making sure we correctly average
        the errors together, not that we fully test the production of each error under different scenarios (like
        providing / not providing a censoring distribution).
     */

    private final double integrationUpperBound = 5.0;

    private final List<CompetingRiskResponse> responses;
    private final List<CompetingRiskFunctions> functions;


    private final double[][] errors;

    public IBSErrorCalculatorWrapperTest(){
        this.responses = Utils.easyList(
                new CompetingRiskResponse(0, 2.0),
                new CompetingRiskResponse(0, 3.0),
                new CompetingRiskResponse(1, 1.0),
                new CompetingRiskResponse(1, 1.5),
                new CompetingRiskResponse(2, 3.0),
                new CompetingRiskResponse(2, 4.0)
        );

        final RightContinuousStepFunction cif1 = RightContinuousStepFunction.constructFromPoints(Utils.easyList(
                new Point(1.0, 0.25),
                new Point(1.5, 0.45)
        ), 0.0);

        final RightContinuousStepFunction cif2 = RightContinuousStepFunction.constructFromPoints(Utils.easyList(
                new Point(3.0, 0.25),
                new Point(4.0, 0.45)
        ), 0.0);

        // This function is for the unused CHFs and survival curve
        // If we see infinities or NaNs popping up in our output we should look here.
        final RightContinuousStepFunction emptyFun = RightContinuousStepFunction.constructFromPoints(Utils.easyList(
                new Point(0.0, Double.NaN)
                ), Double.NEGATIVE_INFINITY
        );

        final CompetingRiskFunctions function = CompetingRiskFunctions.builder()
                .cumulativeIncidenceCurves(Utils.easyList(cif1, cif2))
                .causeSpecificHazards(Utils.easyList(emptyFun, emptyFun))
                .survivalCurve(emptyFun)
                .build();

        // Same prediction for every response.
        this.functions = Utils.easyList(function, function, function, function, function, function);

        final IBSCalculator calculator = new IBSCalculator();
        this.errors = new double[2][6];

        for(int event : new int[]{1, 2}){
            for(int i=0; i<6; i++){
                this.errors[event-1][i] = calculator.calculateError(
                        responses.get(i), function.getCumulativeIncidenceFunction(event),
                        event, integrationUpperBound
                );
            }
        }

    }

    @Test
    public void testOneEventOne(){
        final IBSErrorCalculatorWrapper wrapper = new IBSErrorCalculatorWrapper(new IBSCalculator(), new int[]{1},
                this.integrationUpperBound);

        final double error = wrapper.averageError(this.responses, this.functions);
        double expectedError = 0.0;
        for(int i=0; i<6; i++){
            expectedError += errors[0][i] / 6.0;
        }

        assertEquals(expectedError, error, 0.00000001);
    }

    @Test
    public void testOneEventTwo(){
        final IBSErrorCalculatorWrapper wrapper = new IBSErrorCalculatorWrapper(new IBSCalculator(), new int[]{2},
                this.integrationUpperBound);

        final double error = wrapper.averageError(this.responses, this.functions);
        double expectedError = 0.0;
        for(int i=0; i<6; i++){
            expectedError += errors[1][i] / 6.0;
        }

        assertEquals(expectedError, error, 0.00000001);
    }

    @Test
    public void testTwoEventsNoWeights(){
        final IBSErrorCalculatorWrapper wrapper = new IBSErrorCalculatorWrapper(new IBSCalculator(), new int[]{1, 2},
                this.integrationUpperBound);

        final double error = wrapper.averageError(this.responses, this.functions);
        double expectedError1 = 0.0;
        double expectedError2 = 0.0;

        for(int i=0; i<6; i++){
            expectedError1 += errors[0][i] / 6.0;
            expectedError2 += errors[1][i] / 6.0;
        }

        assertEquals(expectedError1 + expectedError2, error, 0.00000001);
    }

    @Test
    public void testTwoEventsWithWeights(){
        final IBSErrorCalculatorWrapper wrapper = new IBSErrorCalculatorWrapper(new IBSCalculator(), new int[]{1, 2},
                this.integrationUpperBound, new double[]{1.0, 2.0});

        final double error = wrapper.averageError(this.responses, this.functions);
        double expectedError1 = 0.0;
        double expectedError2 = 0.0;

        for(int i=0; i<6; i++){
            expectedError1 += errors[0][i] / 6.0;
            expectedError2 += errors[1][i] / 6.0;
        }

        assertEquals(1.0 * expectedError1 + 2.0 * expectedError2, error, 0.00000001);
    }

}
