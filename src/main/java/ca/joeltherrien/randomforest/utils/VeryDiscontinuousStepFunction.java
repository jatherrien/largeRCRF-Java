package ca.joeltherrien.randomforest.utils;

/**
 * Represents a step function represented by discrete points. However, there may be individual time values that has
 * a y value that doesn't belong to a particular 'step'.
 */
public final class VeryDiscontinuousStepFunction implements MathFunction {

    private final double[] x;
    private final double[] yAt;
    private final double[] yRight;

    /**
     * Represents the value that should be returned by evaluate if there are points prior to the time the function is being evaluated at.
     *
     * Map be null.
     */
    private final double defaultY;

    public VeryDiscontinuousStepFunction(double[] x, double[] yAt, double[] yRight, double defaultY) {
        this.x = x;
        this.yAt = yAt;
        this.yRight = yRight;
        this.defaultY = defaultY;
    }

    @Override
    public double evaluate(double time){
        int index = Utils.binarySearchLessThan(0, x.length, x, time);
        if(index < 0){
            return defaultY;
        }
        else{
            if(x[index] == time){
                return yAt[index];
            }
            else{ // time > x[index]
                return yRight[index];
            }
        }
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
            builder.append("\tyAt:");
            builder.append(yAt[i]);
            builder.append("\tyRight:");
            builder.append(yRight[i]);
            builder.append("\n");
        }

        return builder.toString();
    }

}
