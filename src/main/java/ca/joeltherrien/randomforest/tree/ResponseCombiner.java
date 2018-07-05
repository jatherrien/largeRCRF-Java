package ca.joeltherrien.randomforest.tree;

import java.util.List;
import java.util.stream.Collector;

public interface ResponseCombiner<Y, K> extends Collector<Y, K, Y> {

    Y combine(List<Y> responses);

}
