package ca.joeltherrien.randomforest.utils;

import java.util.List;
import java.util.ListIterator;

/**
 * Represents a function represented by discrete points. We assume that the function is a stepwise right-continuous
 * function, constant at the value of the previous encountered point.
 *
 */
public final class RightContinuousStepFunction extends StepFunction {

    private final double[] y;

    /**
     * Represents the value that should be returned by evaluate if there are points prior to the time the function is being evaluated at.
     *
     * Map be null.
     */
    private final double defaultY;

    public RightContinuousStepFunction(double[] x, double[] y, double defaultY) {
        super(x);
        this.y = y;
        this.defaultY = defaultY;
    }

    /**
     * This isn't a formal constructor because of limitations with abstract classes.
     *
     * @param pointList
     * @param defaultY
     * @return
     */
    public static RightContinuousStepFunction constructFromPoints(final List<Point> pointList, final double defaultY){

        final double[] x = new double[pointList.size()];
        final double[] y = new double[pointList.size()];

        final ListIterator<Point> pointIterator = pointList.listIterator();
        while(pointIterator.hasNext()){
            final int index = pointIterator.nextIndex();
            final Point currentPoint = pointIterator.next();

            x[index] = currentPoint.getTime();
            y[index] = currentPoint.getY();
        }

        return new RightContinuousStepFunction(x, y, defaultY);

    }

    public double[] getY(){
        return y.clone();
    }

    @Override
    public double evaluate(double time){
        int index = Utils.binarySearchLessThan(0, x.length, x, time);
        if(index < 0){
            return defaultY;
        }
        else{
            return y[index];
        }
    }

    @Override
    public double evaluatePrevious(double time){
        int index = Utils.binarySearchLessThan(0, x.length, x, time) - 1;
        if(index < 0){
            return defaultY;
        }
        else{
            return y[index];
        }
    }

    @Override
    public double evaluateByIndex(int i) {
        if(i < 0){
            return defaultY;
        }

        return y[i];
    }


    @Override
    public String toString(){
        final StringBuilder builder = new StringBuilder();
        builder.append("Default point: ");
        builder.append(defaultY);
        builder.append("\n");

        for(int i=0; i<x.length; i++){
            builder.append("x:");
            builder.append(x[i]);
            builder.append("\ty:");
            builder.append(y[i]);
            builder.append("\n");
        }

        return builder.toString();
    }

}
