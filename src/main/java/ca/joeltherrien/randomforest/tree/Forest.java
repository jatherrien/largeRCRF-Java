package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.CovariateRow;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Builder
public class Forest<O, FO> { // O = output of trees, FO = forest output. In practice O == FO, even in competing risk & survival settings

    private final Collection<Tree<O>> trees;
    private final ResponseCombiner<O, FO> treeResponseCombiner;

    public FO evaluate(CovariateRow row){

        return treeResponseCombiner.combine(
                trees.parallelStream()
                .map(node -> node.evaluate(row))
                .collect(Collectors.toList())
        );

    }

    public FO evaluateOOB(CovariateRow row){

        return treeResponseCombiner.combine(
          trees.parallelStream()
          .filter(tree -> !tree.idInBootstrapSample(row.getId()))
          .map(node -> node.evaluate(row))
          .collect(Collectors.toList())
        );

    }

    public Collection<Tree<O>> getTrees(){
        return Collections.unmodifiableCollection(trees);
    }

}
