package ca.joeltherrien.randomforest.utils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SumFunction implements MathFunction{

    private final MathFunction function1;
    private final MathFunction function2;

    @Override
    public double evaluate(double time) {
        return function1.evaluate(time) + function2.evaluate(time);
    }
}
