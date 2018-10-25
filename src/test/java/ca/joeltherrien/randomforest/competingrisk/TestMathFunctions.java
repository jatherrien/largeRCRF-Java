package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.utils.LeftContinuousStepFunction;
import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMathFunctions {

    private RightContinuousStepFunction generateRightContinuousStepFunction(){
        final double[] time = new double[]{1.0, 2.0, 3.0};
        final double[] y = new double[]{-1.0, 1.0, 0.5};

        return new RightContinuousStepFunction(time, y, 0.1);
    }

    private LeftContinuousStepFunction generateLeftContinuousStepFunction(){
        final double[] time = new double[]{1.0, 2.0, 3.0};
        final double[] y = new double[]{-1.0, 1.0, 0.5};

        return new LeftContinuousStepFunction(time, y, 0.1);
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

    @Test
    public void testLeftContinuousStepFunction(){
        final LeftContinuousStepFunction function = generateLeftContinuousStepFunction();

        assertEquals(0.1, function.evaluate(0.5));
        assertEquals(0.1, function.evaluate(1.0));
        assertEquals(-1.0, function.evaluate(2.0));
        assertEquals(1.0, function.evaluate(3.0));


        assertEquals(0.1, function.evaluate(0.6));
        assertEquals(-1.0, function.evaluate(1.1));
        assertEquals(1.0, function.evaluate(2.1));
        assertEquals(0.5, function.evaluate(3.1));

    }

}
