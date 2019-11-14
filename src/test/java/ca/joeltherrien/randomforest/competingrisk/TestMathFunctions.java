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

package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMathFunctions {

    private RightContinuousStepFunction generateRightContinuousStepFunction(){
        final double[] time = new double[]{1.0, 2.0, 3.0};
        final double[] y = new double[]{-1.0, 1.0, 0.5};

        return new RightContinuousStepFunction(time, y, 0.1);
    }

    @Test
    public void testRightContinuousStepFunction(){
        final RightContinuousStepFunction function = generateRightContinuousStepFunction();

        assertEquals(0.1, function.evaluate(0.5));
        assertEquals(-1.0, function.evaluate(1.0));
        assertEquals(1.0, function.evaluate(2.0));
        assertEquals(0.5, function.evaluate(3.0));


        assertEquals(0.1, function.evaluate(0.6));
        assertEquals(-1.0, function.evaluate(1.1));
        assertEquals(1.0, function.evaluate(2.1));
        assertEquals(0.5, function.evaluate(3.1));


    }


}
