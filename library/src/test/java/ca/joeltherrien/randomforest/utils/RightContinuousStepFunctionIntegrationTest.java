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

package ca.joeltherrien.randomforest.utils;

import ca.joeltherrien.randomforest.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RightContinuousStepFunctionIntegrationTest {

    private RightContinuousStepFunction createTestFunction(){

        final double defaultY = 1;
        final double[] x = new double[]{1.0, 2.0, 3.0, 4.0, 5.0};
        final double[] y = new double[]{2.0, 3.0, -0.5, -1.0, 3.0};

        return new RightContinuousStepFunction(x, y, defaultY);
    }

    @Test
    public void testIntegration(){
        final RightContinuousStepFunction function = createTestFunction();

        // Test whether it can handle both from and to being before all the x-values
        TestUtils.closeEnough(0.9,
                function.integrate(0, 0.9)
                , 0.00000001);

        // Test whether it can handle both from and to being after all the x-values
        TestUtils.closeEnough(3.0,
                function.integrate(6, 7)
                , 0.00000001);

        TestUtils.closeEnough(6.0,
                function.integrate(0, 3)
                , 0.00000001);

        TestUtils.closeEnough(4.5,
                function.integrate(0, 2.5)
                , 0.00000001);

        TestUtils.closeEnough(7.5,
                function.integrate(0, 6)
                , 0.00000001);

        TestUtils.closeEnough(-0.5,
                function.integrate(3, 4)
                , 0.00000001);

        TestUtils.closeEnough(-0.5*0.8,
                function.integrate(3.1, 3.9)
                , 0.00000001);

        TestUtils.closeEnough(1.5,
                function.integrate(3, 6)
                , 0.00000001);

        TestUtils.closeEnough(3.0,
                function.integrate(2.5, 6)
                , 0.00000001);


    }

    @Test
    public void testInvertedFromTo(){
        final RightContinuousStepFunction function = createTestFunction();

        final double area1 = function.integrate(0, 3.0);
        final double area2 = function.integrate(3.0, 0.0);

        assertEquals(area1, -area2, 0.0000001);

    }

    @Test
    public void testIntegratingUpToNan(){
        // Idea here - you have a function that is valid up to point x where it becomes NaN
        // You should be able to integrate *up to* point x and not get an NaN

        final RightContinuousStepFunction function1 = new RightContinuousStepFunction(
                new double[]{1.0, 2.0, 3.0, 4.0},
                new double[]{1.0, 1.0, 1.0, Double.NaN},
                0.0);


        final double area1 = function1.integrate(0.0, 4.0);
        assertEquals(3.0, area1, 0.000000001);

        final double nanArea1 = function1.integrate(0.0, 4.0001);
        assertTrue(Double.isNaN(nanArea1));


        // This tests integrating over the defaultY up to the NaN point
        final RightContinuousStepFunction function2 = new RightContinuousStepFunction(
                new double[]{1.0, 2.0, 3.0, 4.0},
                new double[]{Double.NaN, 1.0, 1.0, Double.NaN},
                1.0);


        final double area2 = function2.integrate(0.0, 1.0);
        assertEquals(1.0, area2, 0.000000001);

        final double nanArea2 = function2.integrate(0.0, 4.0);
        assertTrue(Double.isNaN(nanArea2));


        // This tests integrating between two NaN points. Note that of course for RightContinuousValues carry the previous
        // value until the next x point, so this is just making sure the code works if the x-value we pass over is NaN
        final RightContinuousStepFunction function3 = new RightContinuousStepFunction(
                new double[]{1.0, 2.0, 3.0, 4.0},
                new double[]{Double.NaN, 1.0, 1.0, Double.NaN},
                0.0);


        final double area3 = function3.integrate(2.0, 4.0);
        assertEquals(2.0, area3, 0.000000001);

        final double nanArea3 = function3.integrate(0.0, 4.0001);
        assertTrue(Double.isNaN(nanArea3));

    }


    @Test
    public void testIntegratingEmptyFunction(){
        // A function might have no points, but we'll still need to integrate it.

        final RightContinuousStepFunction function = new RightContinuousStepFunction(
                new double[]{}, new double[]{}, 1.0
        );

        final double area = function.integrate(1.0 ,3.0);
        assertEquals(2.0, area, 0.000001);

    }


}
