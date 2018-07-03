package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.CovariateRow;
import ca.joeltherrien.randomforest.ResponseCombiner;
import lombok.Builder;

import java.util.List;

@Builder
public class Forest<Y> {

    private final List<Node<Y>> trees;
    private final ResponseCombiner<Y, ?> treeResponseCombiner;

    public Y evaluate(CovariateRow row){
        return trees.parallelStream()
                .map(node -> node.evaluate(row))
                .collect(treeResponseCombiner);
    }

}
