package ca.joeltherrien.randomforest.utils;

import java.io.Serializable;

public interface MathFunction extends Serializable {

    double evaluate(double time);

}
