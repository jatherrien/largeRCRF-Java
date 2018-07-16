package ca.joeltherrien.randomforest.tree;

import java.util.List;

public interface ResponseCombiner<I, O> {

    O combine(List<I> responses);

}
