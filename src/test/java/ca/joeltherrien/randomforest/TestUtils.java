package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.utils.StepFunction;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public static void assertCumulativeFunction(StepFunction function){
        Double previousTime = null;
        Double previousY = null;

        final double[] times = function.getX();

        for(int i=0; i<times.length; i++){
            final double time = times[i];
            final double y = function.evaluateByIndex(i);

            if(previousTime != null){
                assertTrue(previousTime < time, "Points should be ordered and strictly different");
                assertTrue(previousY <= y, "Cumulative incidence functions are monotone");
            }

            previousTime = time;
            previousY = y;

        }
    }

    public static void assertSurvivalCurve(StepFunction function){
        Double previousTime = null;
        Double previousY = null;

        final double[] times = function.getX();

        for(int i=0; i<times.length; i++){
            final double time = times[i];
            final double y = function.evaluateByIndex(i);

            if(previousTime != null){
                assertTrue(previousTime < time, "Points should be ordered and strictly different");
                assertTrue(previousY >= y, "Survival functions are monotone");
            }

            previousTime = time;
            previousY = y;

        }
    }

    @Test
    public void testOneMinusECDF(){
        final double[] times = new double[]{1.0, 1.0, 2.0, 3.0, 3.0, 50.0};
        final StepFunction survivalCurve = Utils.estimateOneMinusECDF(times);

        final double margin = 0.000001;
        closeEnough(1.0, survivalCurve.evaluate(0.0), margin);

        closeEnough(1.0, survivalCurve.evaluatePrevious(1.0), margin);
        closeEnough(4.0/6.0, survivalCurve.evaluate(1.0), margin);

        closeEnough(4.0/6.0, survivalCurve.evaluatePrevious(2.0), margin);
        closeEnough(3.0/6.0, survivalCurve.evaluate(2.0), margin);

        closeEnough(3.0/6.0, survivalCurve.evaluatePrevious(3.0), margin);
        closeEnough(1.0/6.0, survivalCurve.evaluate(3.0), margin);

        closeEnough(1.0/6.0, survivalCurve.evaluatePrevious(50.0), margin);
        closeEnough(0.0, survivalCurve.evaluate(50.0), margin);

        assertSurvivalCurve(survivalCurve);

    }

    @Test
    public void reduceListToSize(){
        final List<Integer> testList = Utils.easyList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        for(int i=0; i<100; i++) { // want to test many times to ensure it doesn't work just due to randomness
            final List<Integer> testList1 = new ArrayList<>(testList);
            // Test when removing elements
            Utils.reduceListToSize(testList1, 7);
            assertEquals(7, testList1.size()); // verify proper size
            assertEquals(7, new HashSet<>(testList1).size()); // verify the items are unique


            final List<Integer> testList2 = new ArrayList<>(testList);
            // Test when adding elements
            Utils.reduceListToSize(testList2, 3);
            assertEquals(3, testList2.size()); // verify proper size
            assertEquals(3, new HashSet<>(testList2).size()); // verify the items are unique

            final List<Integer> testList3 = new ArrayList<>(testList);
            // verify no change
            Utils.reduceListToSize(testList3, 15);
            assertEquals(10, testList3.size()); // verify proper size
            assertEquals(10, new HashSet<>(testList3).size()); // verify the items are unique

        }
    }

}
