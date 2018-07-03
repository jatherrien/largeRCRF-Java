package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.Bootstrapper;
import ca.joeltherrien.randomforest.ResponseCombiner;
import ca.joeltherrien.randomforest.Row;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
public class ForestTrainer<Y> {

    private final TreeTrainer<Y> treeTrainer;
    private final List<String> covariatesToTry;
    private final ResponseCombiner<Y, ?> treeResponseCombiner;
    private final List<Row<Y>> data;

    // number of covariates to randomly try
    private final int mtry;

    // number of trees to try
    private final int ntree;

    private final boolean displayProgress;

    public Forest<Y> trainSerial(){

        final List<Node<Y>> trees = new ArrayList<>(ntree);
        final Bootstrapper<Row<Y>> bootstrapper = new Bootstrapper<>(data);

        for(int j=0; j<ntree; j++){

            trees.add(trainTree(bootstrapper));

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

    public Forest<Y> trainParallel(int threads){

        // create a list that is prespecified in size (I can call the .set method at any index < ntree without
        // the earlier indexes being filled.
        final List<Node<Y>> trees = Stream.<Node<Y>>generate(() -> null).limit(ntree).collect(Collectors.toList());

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);

        for(int j=0; j<ntree; j++){
            final Runnable worker = new Worker(data, j, trees);
            executorService.execute(worker);
        }

        executorService.shutdown();
        while(!executorService.isTerminated()){

            try{
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // do nothing; who cares?
            }

            if(displayProgress) {
                int numberTreesSet = 0;
                for (final Node<Y> tree : trees) {
                    if (tree != null) {
                        numberTreesSet++;
                    }
                }

                System.out.print("\rFinished " + numberTreesSet + "/" + ntree + " trees");
            }

        }

        if(displayProgress){
            System.out.println("\nFinished");
        }

        return Forest.<Y>builder()
                .treeResponseCombiner(treeResponseCombiner)
                .trees(trees)
                .build();

    }

    private Node<Y> trainTree(final Bootstrapper<Row<Y>> bootstrapper){
        final List<String> treeCovariates = new ArrayList<>(covariatesToTry);
        Collections.shuffle(treeCovariates);

        for(int treeIndex = covariatesToTry.size()-1; treeIndex >= mtry; treeIndex--){
            treeCovariates.remove(treeIndex);
        }

        final List<Row<Y>> bootstrappedData = bootstrapper.bootstrap();

        return treeTrainer.growTree(bootstrappedData, treeCovariates);
    }

    private class Worker implements Runnable {

        private final Bootstrapper<Row<Y>> bootstrapper;
        private final int treeIndex;
        private final List<Node<Y>> treeList;

        public Worker(final List<Row<Y>> data, final int treeIndex, final List<Node<Y>> treeList) {
            this.bootstrapper = new Bootstrapper<>(data);
            this.treeIndex = treeIndex;
            this.treeList = treeList;
        }

        @Override
        public void run() {

            final Node<Y> tree = trainTree(bootstrapper);

            // should be okay as the list structure isn't changing
            treeList.set(treeIndex, tree);

            //if(displayProgress){
            //   System.out.println("Finished tree " + (treeIndex+1));
            //}


        }
    }

}
