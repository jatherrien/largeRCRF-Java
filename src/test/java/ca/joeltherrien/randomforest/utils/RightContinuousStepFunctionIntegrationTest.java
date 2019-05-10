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


}
