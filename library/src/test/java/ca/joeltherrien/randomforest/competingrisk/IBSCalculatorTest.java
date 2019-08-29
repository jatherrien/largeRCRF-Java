package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskUtils;
import ca.joeltherrien.randomforest.responses.competingrisk.IBSCalculator;
import ca.joeltherrien.randomforest.utils.Point;
import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IBSCalculatorTest {

    private final RightContinuousStepFunction cif;

    public IBSCalculatorTest(){
        this.cif = RightContinuousStepFunction.constructFromPoints(
                Utils.easyList(
                    new Point(1.0, 0.1),
                    new Point(2.0, 0.2),
                    new Point(3.0, 0.3),
                    new Point(4.0, 0.8)
                ), 0.0
        );
    }

    /*
    R code to get these results:

    predicted_cif <- stepfun(1:4, c(0, 0.1, 0.2, 0.3, 0.8))
    weights <- 1
    recorded_time <- 2.0
    recorded_status <- 1.0
    event_of_interest <- 2
    times <- 0:4

    errors <- weights * ( as.integer(recorded_time <= times & recorded_status == event_of_interest) - predicted_cif(times))^2
    sum(errors)


    and run again with event_of_interest <- 1


    Note that in the R code I only evaluate up to 4, while in the Java code I integrate up to 5
    This is because the evaluation at 4 is giving the area of the rectangle from 4 to 5.

     */

    @Test
    public void testResultsWithoutCensoringDistribution(){
        final IBSCalculator calculator = new IBSCalculator();

        final double errorDifferentEvent = calculator.calculateError(
                new CompetingRiskResponse(1, 2.0),
                this.cif,
                2,
                5.0);

        assertEquals(0.78, errorDifferentEvent, 0.000001);

        final double errorSameEvent = calculator.calculateError(
                new CompetingRiskResponse(1, 2.0),
                this.cif,
                1,
                5.0);

        assertEquals(1.18, errorSameEvent, 0.000001);

    }

    @Test
    public void testResultsWithCensoringDistribution(){
        final RightContinuousStepFunction censorSurvivalFunction = RightContinuousStepFunction.constructFromPoints(
                Utils.easyList(
                        new Point(0.0, 0.75),
                        new Point(1.0, 0.5),
                        new Point(3.0, 0.25),
                        new Point(5.0, 0)
                        ), 1.0
        );

        final IBSCalculator calculator = new IBSCalculator(censorSurvivalFunction);

        final double errorDifferentEvent = calculator.calculateError(
                new CompetingRiskResponse(1, 2.0),
                this.cif,
                2,
                5.0);

        assertEquals(1.56, errorDifferentEvent, 0.000001);

        final double errorSameEvent = calculator.calculateError(
                new CompetingRiskResponse(1, 2.0),
                this.cif,
                1,
                5.0);

        assertEquals(2.36, errorSameEvent, 0.000001);

    }

    @Test
    public void testStaticFunction(){
        final RightContinuousStepFunction censorSurvivalFunction = RightContinuousStepFunction.constructFromPoints(
                Utils.easyList(
                        new Point(0.0, 0.75),
                        new Point(1.0, 0.5),
                        new Point(3.0, 0.25),
                        new Point(5.0, 0)
                ), 1.0
        );

        final List<CompetingRiskResponse> responseList = Utils.easyList(
                new CompetingRiskResponse(1, 2.0),
                new CompetingRiskResponse(1, 2.0));

        // for predictions; we'll construct an improper CompetingRisksFunctions
        final RightContinuousStepFunction trivialFunction = RightContinuousStepFunction.constructFromPoints(
                Utils.easyList(new Point(1.0, 0.0)),
                1.0);

        final CompetingRiskFunctions prediction = CompetingRiskFunctions.builder()
                .survivalCurve(trivialFunction)
                .causeSpecificHazards(Utils.easyList(trivialFunction, trivialFunction))
                .cumulativeIncidenceCurves(Utils.easyList(this.cif, trivialFunction))
                .build();

        final List<CompetingRiskFunctions> predictionList = Utils.easyList(prediction, prediction);

        double[] errorParallel = CompetingRiskUtils.calculateIBSError(
                responseList,
                predictionList,
                Optional.of(censorSurvivalFunction),
                1,
                5.0,
                true);

        double[] errorSerial = CompetingRiskUtils.calculateIBSError(
                responseList,
                predictionList,
                Optional.of(censorSurvivalFunction),
                1,
                5.0,
                false);

        assertEquals(responseList.size(), errorParallel.length);
        assertEquals(responseList.size(), errorSerial.length);

        assertEquals(2.36, errorParallel[0], 0.000001);
        assertEquals(2.36, errorParallel[1], 0.000001);

        assertEquals(2.36, errorSerial[0], 0.000001);
        assertEquals(2.36, errorSerial[1], 0.000001);

    }



}
