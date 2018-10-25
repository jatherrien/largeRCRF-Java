package ca.joeltherrien.randomforest.utils;

public abstract class StepFunction implements MathFunction{

    protected final double[] x;

    StepFunction(double[] x){
        this.x = x;
    }

    public double[] getX() {
        return x.clone();
    }

    public abstract double evaluateByIndex(int i);

    /**
     * Evaluate the function at the time *point* that occurred previous to time. This is NOT time - some delta, but rather
     * time[i-1].
     *
     * @param time
     * @return
     */
    public abstract double evaluatePrevious(double time);

}
