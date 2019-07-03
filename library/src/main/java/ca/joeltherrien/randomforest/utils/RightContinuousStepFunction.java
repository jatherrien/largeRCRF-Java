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

    public double integrate(final double from, final double to){

        if(to == from){
            return 0;
        }

        if(to < from){
            return integrate(to, from);
        }

        double summation = 0.0;
        final double[] xPoints = getX();
        final int startingIndex;

        if(from > xPoints[0]){
            // find largest index of time such that it's less than or equal to from

            final int firstLevelIndex = Utils.binarySearchLessThan(0, xPoints.length, xPoints, from);
            final double currentHeight = evaluateByIndex(firstLevelIndex);
            startingIndex = firstLevelIndex+1; // the looping part of the code will start at the next index

            if(startingIndex == xPoints.length){ // turns out that 'from' is beyond the last point
                return currentHeight * (to - from);
            }

            final double timeAtNextHeight = xPoints[startingIndex];

            if(timeAtNextHeight >= to){
                return currentHeight * (to - from);
            }

            summation += currentHeight * (timeAtNextHeight - from);
        }
        else if(from < xPoints[0]){

            if(to <= xPoints[0]){
                return defaultY*(to - from);
            }
            startingIndex = 0;
            summation += defaultY * (xPoints[0] - from);
        }
        else{
            startingIndex = 0;
        }

        int i;
        for(i=startingIndex; i<xPoints.length; i++){
            final double currentTime = xPoints[i];
            final double currentHeight = evaluateByIndex(i);

            if(i == xPoints.length-1 || xPoints[i+1] > to){
                summation += currentHeight * (to - currentTime);
                return summation;
            }

            final double nextTime = xPoints[i+1];
            summation += currentHeight * (nextTime - currentTime);


        }

        // We should have returned at some point before now
        throw new RuntimeException("Serious bug in integration code; this line should have never been run");

    }


}
