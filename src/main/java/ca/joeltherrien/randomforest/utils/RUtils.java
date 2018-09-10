package ca.joeltherrien.randomforest.utils;

import java.util.List;

/**
 * These static methods are designed to make the R interface more performant; and to avoid using R loops.
 *
 */
public final class RUtils {

    public static double[] extractTimes(final MathFunction function){
        final List<Point> pointList = function.getPoints();
        final double[] times = new double[pointList.size()];

        for(int i=0; i<pointList.size(); i++){
            times[i] = pointList.get(i).getTime();
        }

        return times;
    }

    public static double[] extractY(final MathFunction function){
        final List<Point> pointList = function.getPoints();
        final double[] times = new double[pointList.size()];

        for(int i=0; i<pointList.size(); i++){
            times[i] = pointList.get(i).getY();
        }

        return times;
    }


}
