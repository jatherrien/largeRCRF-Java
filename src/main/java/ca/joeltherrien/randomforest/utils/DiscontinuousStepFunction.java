package ca.joeltherrien.randomforest.utils;

/**
 * Represents a function represented by discrete points. However, the function may be right-continuous or left-continuous
 * at a given point, with no consistency. This function tracks that.
 */
public final class DiscontinuousStepFunction extends StepFunction {

    private final double[] y;
    private final boolean[] isLeftContinuous;

    /**
     * Represents the value that should be returned by evaluate if there are points prior to the time the function is being evaluated at.
     *
     * Map be null.
     */
    private final double defaultY;

    public DiscontinuousStepFunction(double[] x, double[] y, boolean[] isLeftContinuous, double defaultY) {
        super(x);
        this.y = y;
        this.isLeftContinuous = isLeftContinuous;
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
                return evaluateByIndex(index);
            }
            else{
                return y[index];
            }
        }
    }


    @Override
    public double evaluatePrevious(double time){
        int index = Utils.binarySearchLessThan(0, x.length, x, time) - 1;
        if(index < 0){
            return defaultY;
        }
        else{
            if(x[index] == time){
                return evaluateByIndex(index);
            }
            else{
                return y[index];
            }
        }
    }

    @Override
    public double evaluateByIndex(int i) {
        if(isLeftContinuous[i]){
            i -= 1;
        }

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
            builder.append('\t');

            if(isLeftContinuous[i]){
                builder.append("*y:");
            }
            else{
                builder.append("y*:");
            }
            builder.append(y[i]);
            builder.append("\n");
        }

        return builder.toString();
    }

}
