package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.CovariateRow;
import lombok.Builder;

import java.util.Collection;

@Builder
public class Forest<Y> {

    private final Collection<Node<Y>> trees;
    private final ResponseCombiner<Y, ?> treeResponseCombiner;

    public Y evaluate(CovariateRow row){
        return trees.parallelStream()
                .map(node -> node.evaluate(row))
                .collect(treeResponseCombiner);
    }

}
