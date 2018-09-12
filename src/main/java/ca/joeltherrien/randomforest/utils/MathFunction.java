package ca.joeltherrien.randomforest.utils;

import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Represents a function represented by discrete points. We assume that the function is a stepwise continuous function,
 * constant at the value of the previous encountered point.
 *
 */
public class MathFunction implements Serializable {

    @Getter
    private final List<Point> points;

    /**
     * Represents the value that should be returned by evaluate if there are points prior to the time the function is being evaluated at.
     *
     * Map be null.
     */
    private final Point defaultValue;

    public MathFunction(final List<Point> points){
        this(points, new Point(0.0, 0.0));
    }

    public MathFunction(final List<Point> points, final Point defaultValue){
        this.points = Collections.unmodifiableList(points);
        this.defaultValue = defaultValue;
    }

    public Point evaluate(double time){
        int index = binarySearch(points, time);
        if(index < 0){
            return defaultValue;
        }
        else{
            return points.get(index);
        }
    }

    public Point evaluatePrevious(double time){

        int index = binarySearch(points, time) - 1;
        if(index < 0){
            return defaultValue;
        }
        else{
            return points.get(index);
        }


    }

    /**
     * Returns the index of the largest (in terms of time) Point that is <= the provided time value.
     *
     * @param points
     * @param time
     * @return The index of the largest Point who's time is <= the time parameter.
     */
    private static int binarySearch(List<Point> points, double time){
        final int pointSize = points.size();

        if(pointSize == 0 || points.get(pointSize-1).getTime() <= time){
            // we're already too far
            return pointSize - 1;
        }

        if(pointSize < 200){
            for(int i = 0; i < pointSize; i++){
                if(points.get(i).getTime() > time){
                    return i - 1;
                }
            }
        }

         // else


        final int middle = pointSize / 2;
        final double middleTime = points.get(middle).getTime();
        if(middleTime < time){
            // go right
            return binarySearch(points.subList(middle, pointSize), time) + middle;
        }
        else if(middleTime > time){
            // go left
            return binarySearch(points.subList(0, middle), time);
        }
        else{ // middleTime == time
            return middle;
        }
    }

    @Override
    public String toString(){
        final StringBuilder builder = new StringBuilder();
        builder.append("Default point: ");
        builder.append(defaultValue);
        builder.append("\n");

        for(final Point point : points){
            builder.append(point);
            builder.append("\n");
        }

        return builder.toString();
    }

}
