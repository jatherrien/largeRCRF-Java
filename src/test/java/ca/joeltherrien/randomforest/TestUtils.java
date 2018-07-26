package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.utils.MathFunction;
import ca.joeltherrien.randomforest.utils.Point;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUtils {

    public static void closeEnough(double expected, double actual, double margin){
        assertTrue(Math.abs(expected - actual) < margin, "Expected " + expected + " but saw " + actual);
    }

    /**
     * We know the function is cumulative; make sure it is ordered correctly and that that function is monotone.
     *
     * @param function
     */
    public static void assertCumulativeFunction(MathFunction function){
        Point previousPoint = null;
        for(final Point point : function.getPoints()){

            if(previousPoint != null){
                assertTrue(previousPoint.getTime() < point.getTime(), "Points should be ordered and strictly different");
                assertTrue(previousPoint.getY() <= point.getY(), "Cumulative incidence functions are monotone");
            }


            previousPoint = point;
        }
    }

    public static void assertSurvivalCurve(MathFunction function){
        Point previousPoint = null;
        for(final Point point : function.getPoints()){

            if(previousPoint != null){
                assertTrue(previousPoint.getTime() < point.getTime(), "Points should be ordered and strictly different");
                assertTrue(previousPoint.getY() >= point.getY(), "Survival functions are monotone");
            }


            previousPoint = point;
        }
    }

    @Test
    public void testOneMinusECDF(){
        final double[] times = new double[]{1.0, 1.0, 2.0, 3.0, 3.0, 50.0};
        final MathFunction survivalCurve = Utils.estimateOneMinusECDF(times);

        final double margin = 0.000001;
        closeEnough(1.0, survivalCurve.evaluate(0.0).getY(), margin);

        closeEnough(1.0, survivalCurve.evaluatePrevious(1.0).getY(), margin);
        closeEnough(4.0/6.0, survivalCurve.evaluate(1.0).getY(), margin);

        closeEnough(4.0/6.0, survivalCurve.evaluatePrevious(2.0).getY(), margin);
        closeEnough(3.0/6.0, survivalCurve.evaluate(2.0).getY(), margin);

        closeEnough(3.0/6.0, survivalCurve.evaluatePrevious(3.0).getY(), margin);
        closeEnough(1.0/6.0, survivalCurve.evaluate(3.0).getY(), margin);

        closeEnough(1.0/6.0, survivalCurve.evaluatePrevious(50.0).getY(), margin);
        closeEnough(0.0, survivalCurve.evaluate(50.0).getY(), margin);

        assertSurvivalCurve(survivalCurve);

    }

}
