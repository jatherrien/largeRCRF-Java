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
