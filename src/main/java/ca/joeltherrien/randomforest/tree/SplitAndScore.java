package ca.joeltherrien.randomforest.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class SplitAndScore<Y, V> {

    @Getter
    private final Split<Y, V> split;

    @Getter
    private final Double score;

}
