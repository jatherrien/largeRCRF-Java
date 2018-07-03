package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.Bootstrapper;
import ca.joeltherrien.randomforest.ResponseCombiner;
import ca.joeltherrien.randomforest.Row;
import lombok.Builder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final String saveTreeLocation;

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

    public Forest<Y> trainParallelInMemory(int threads){

        // create a list that is prespecified in size (I can call the .set method at any index < ntree without
        // the earlier indexes being filled.
        final List<Node<Y>> trees = Stream.<Node<Y>>generate(() -> null).limit(ntree).collect(Collectors.toList());

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);

        for(int j=0; j<ntree; j++){
            final Runnable worker = new TreeInMemoryWorker(data, j, trees);
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


    public void trainParallelOnDisk(int threads){
        final ExecutorService executorService = Executors.newFixedThreadPool(threads);
        final AtomicInteger treeCount = new AtomicInteger(0); // tracks how many trees are finished

        for(int j=0; j<ntree; j++){
            final Runnable worker = new TreeSavedWorker(data, "tree-" + (j+1), treeCount);
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

                System.out.print("\rFinished " + treeCount.get() + "/" + ntree + " trees");
            }

        }

        if(displayProgress){
            System.out.println("\nFinished");
        }

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

    public void saveTree(final Node<Y> tree, String name) throws IOException {
        final String filename = saveTreeLocation + "/" + name;

        final ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(filename));

        outputStream.writeObject(tree);

        outputStream.close();

    }

    private class TreeInMemoryWorker implements Runnable {

        private final Bootstrapper<Row<Y>> bootstrapper;
        private final int treeIndex;
        private final List<Node<Y>> treeList;

        public TreeInMemoryWorker(final List<Row<Y>> data, final int treeIndex, final List<Node<Y>> treeList) {
            this.bootstrapper = new Bootstrapper<>(data);
            this.treeIndex = treeIndex;
            this.treeList = treeList;
        }

        @Override
        public void run() {

            final Node<Y> tree = trainTree(bootstrapper);

            // should be okay as the list structure isn't changing
            treeList.set(treeIndex, tree);

        }
    }

    private class TreeSavedWorker implements Runnable {

        private final Bootstrapper<Row<Y>> bootstrapper;
        private final String filename;
        private final AtomicInteger treeCount;

        public TreeSavedWorker(final List<Row<Y>> data, final String filename, final AtomicInteger treeCount) {
            this.bootstrapper = new Bootstrapper<>(data);
            this.filename = filename;
            this.treeCount = treeCount;
        }

        @Override
        public void run() {

            final Node<Y> tree = trainTree(bootstrapper);

            try {
                saveTree(tree, filename);
            } catch (IOException e) {
                System.err.println("IOException while saving " + filename);
                e.printStackTrace();
                System.err.println("Quitting program");
                System.exit(1);
            }

            treeCount.incrementAndGet();

        }
    }

}
