package ca.joeltherrien.randomforest;


public interface Value<V> {

    V getValue();

    SplitRule generateSplitRule(String covariateName);


}
