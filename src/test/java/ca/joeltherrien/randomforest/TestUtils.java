/*
 * Copyright (c) 2019 Joel Therrien.
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.utils.StepFunction;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleSupplier;
import java.util.stream.DoubleStream;

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
        final Random random = new Random();
        for(int i=0; i<100; i++) { // want to test many times to ensure it doesn't work just due to randomness
            final List<Integer> testList1 = new ArrayList<>(testList);
            // Test when removing elements
            Utils.reduceListToSize(testList1, 7, random);
            assertEquals(7, testList1.size()); // verify proper size
            assertEquals(7, new HashSet<>(testList1).size()); // verify the items are unique


            final List<Integer> testList2 = new ArrayList<>(testList);
            // Test when adding elements
            Utils.reduceListToSize(testList2, 3, random);
            assertEquals(3, testList2.size()); // verify proper size
            assertEquals(3, new HashSet<>(testList2).size()); // verify the items are unique

            final List<Integer> testList3 = new ArrayList<>(testList);
            // verify no change
            Utils.reduceListToSize(testList3, 15, random);
            assertEquals(10, testList3.size()); // verify proper size
            assertEquals(10, new HashSet<>(testList3).size()); // verify the items are unique

        }
    }

    @Test
    public void testBinarySearchLessThan(){
        /*
        There was a bug where I didn't add startIndex to range/2 for middle; no other tests caught it!
         */
        final int n = 10000;

        double[] x = DoubleStream.generate(new DoubleSequenceGenerator()).limit(n).toArray();


        for(int i = 0; i < n; i=i+100){
            final int index = Utils.binarySearchLessThan(0, n, x, i);
            final int indexOff = Utils.binarySearchLessThan(0, n, x, ((double) i) + 1.5);

            assertEquals(i, index);
            assertEquals(i+1, indexOff);
        }

        final int indexTooFar = Utils.binarySearchLessThan(0, n, x, n + 100);
        assertEquals(n-1, indexTooFar);

        final int indexTooEarly = Utils.binarySearchLessThan(0, n, x, -100);
        assertEquals(-1, indexTooEarly);


    }

    private static class DoubleSequenceGenerator implements DoubleSupplier {
        private double previous = 0.0;

        @Override
        public double getAsDouble() {
            return previous++;
        }
    }

}
