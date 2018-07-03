package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.Bootstrapper;
import ca.joeltherrien.randomforest.ResponseCombiner;
import ca.joeltherrien.randomforest.Row;
import lombok.Builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Builder
public class ForestTrainer<Y> {

    private final TreeTrainer<Y> treeTrainer;
    private final Bootstrapper<Row<Y>> bootstrapper;
    private final List<String> covariatesToTry;
    private final ResponseCombiner<Y, ?> treeResponseCombiner;

    // number of covariates to randomly try
    private final int mtry;

    // number of trees to try
    private final int ntree;

    private final boolean displayProgress;

    public Forest<Y> trainSerial(){

        final List<Node<Y>> trees = new ArrayList<>(ntree);

        for(int j=0; j<ntree; j++){
            final List<String> treeCovariates = new ArrayList<>(covariatesToTry);
            Collections.shuffle(treeCovariates);

            for(int treeIndex = covariatesToTry.size()-1; treeIndex >= mtry; treeIndex--){
                treeCovariates.remove(treeIndex);
            }

            final List<Row<Y>> bootstrappedData = bootstrapper.bootstrap();

            trees.add(treeTrainer.growTree(bootstrappedData, treeCovariates));

            if(displayProgress){
                if(j==0) {
                    System.out.println();
                }
                System.out.print("\rFinished tree " + (j+1) + "/" + ntree);
                if(j==ntree-1){
                    System.out.println();
                }
            }
        }

        return Forest.<Y>builder()
                .treeResponseCombiner(treeResponseCombiner)
                .trees(trees)
                .build();

    }

}
