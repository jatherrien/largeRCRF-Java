package ca.joeltherrien.randomforest;

import java.util.List;

public interface ResponseCombiner<Y> {

    Y combine(List<Y> responses);

}
