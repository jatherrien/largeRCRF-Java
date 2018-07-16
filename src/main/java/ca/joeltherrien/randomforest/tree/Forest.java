package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.CovariateRow;
import lombok.Builder;

import java.util.Collection;
import java.util.stream.Collectors;

@Builder
public class Forest<O, FO> { // O = output of trees, FO = forest output. In practice O == FO, even in competing risk & survival settings

    private final Collection<Node<O>> trees;
    private final ResponseCombiner<O, FO> treeResponseCombiner;

    public FO evaluate(CovariateRow row){

        return treeResponseCombiner.combine(
                trees.parallelStream()
                .map(node -> node.evaluate(row))
                .collect(Collectors.toList())
        );

    }

}
