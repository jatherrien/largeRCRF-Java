package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.utils.MathFunction;
import ca.joeltherrien.randomforest.utils.Point;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class TestMathFunction {

    private MathFunction generateMathFunction(){
        final double[] time = new double[]{1.0, 2.0, 3.0};
        final double[] y = new double[]{-1.0, 1.0, 0.5};

        final List<Point> pointList = new ArrayList<>();
        for(int i=0; i<time.length; i++){
            pointList.add(new Point(time[i], y[i]));
        }

        return new MathFunction(pointList, new Point(0.0, 0.1));

    }

    @Test
    public void test(){
        final MathFunction function = generateMathFunction();

        assertEquals(new Point(1.0, -1.0), function.evaluate(1.0));
        assertEquals(new Point(2.0, 1.0), function.evaluate(2.0));
        assertEquals(new Point(3.0, 0.5), function.evaluate(3.0));
        assertEquals(new Point(0.0, 0.1), function.evaluate(0.5));

        assertEquals(new Point(1.0, -1.0), function.evaluate(1.1));
        assertEquals(new Point(2.0, 1.0), function.evaluate(2.1));
        assertEquals(new Point(3.0, 0.5), function.evaluate(3.1));
        assertEquals(new Point(0.0, 0.1), function.evaluate(0.6));

    }


}
