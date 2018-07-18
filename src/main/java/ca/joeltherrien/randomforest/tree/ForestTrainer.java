package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.Bootstrapper;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.Settings;
import ca.joeltherrien.randomforest.covariates.Covariate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

@Builder
@AllArgsConstructor(access=AccessLevel.PRIVATE)
public class ForestTrainer<Y, TO, FO> {

    private final TreeTrainer<Y, TO> treeTrainer;
    private final List<Covariate> covariates;
    private final ResponseCombiner<TO, FO> treeResponseCombiner;
    private final List<Row<Y>> data;

    // number of covariates to randomly try
    private final int mtry;

    // number of trees to try
    private final int ntree;

    private final boolean displayProgress;
    private final String saveTreeLocation;

    public ForestTrainer(final Settings settings, final List<Row<Y>> data, final List<Covariate> covariates){
        this.mtry = settings.getMtry();
        this.ntree = settings.getNtree();
        this.data = data;
        this.displayProgress = true;
        this.saveTreeLocation = settings.getSaveTreeLocation();

        this.covariates = covariates;
        this.treeResponseCombiner = settings.getTreeCombiner();
        this.treeTrainer = new TreeTrainer<>(settings, covariates);

    }

    public Forest<TO, FO> trainSerial(){

        final List<Tree<TO>> trees = new ArrayList<>(ntree);
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

        return Forest.<TO, FO>builder()
                .treeResponseCombiner(treeResponseCombiner)
                .trees(trees)
                .build();

    }

    public Forest<TO, FO> trainParallelInMemory(int threads){

        // create a list that is prespecified in size (I can call the .set method at any index < ntree without
        // the earlier indexes being filled.
        final List<Tree<TO>> trees = Stream.<Tree<TO>>generate(() -> null).limit(ntree).collect(Collectors.toList());

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
                for (final Tree<TO> tree : trees) {
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

        return Forest.<TO, FO>builder()
                .treeResponseCombiner(treeResponseCombiner)
                .trees(trees)
                .build();

    }


    public void trainParallelOnDisk(int threads){
        final ExecutorService executorService = Executors.newFixedThreadPool(threads);
        final AtomicInteger treeCount = new AtomicInteger(0); // tracks how many trees are finished

        for(int j=0; j<ntree; j++){
            final Runnable worker = new TreeSavedWorker(data, "tree-" + (j+1) + ".tree", treeCount);
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

    private Tree<TO> trainTree(final Bootstrapper<Row<Y>> bootstrapper){
        final List<Row<Y>> bootstrappedData = bootstrapper.bootstrap();
        return treeTrainer.growTree(bootstrappedData);
    }

    public void saveTree(final Tree<TO> tree, String name) throws IOException {
        final String filename = saveTreeLocation + "/" + name;

        final ObjectOutputStream outputStream = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filename)));

        outputStream.writeObject(tree);

        outputStream.close();

    }

    private class TreeInMemoryWorker implements Runnable {

        private final Bootstrapper<Row<Y>> bootstrapper;
        private final int treeIndex;
        private final List<Tree<TO>> treeList;

        public TreeInMemoryWorker(final List<Row<Y>> data, final int treeIndex, final List<Tree<TO>> treeList) {
            this.bootstrapper = new Bootstrapper<>(data);
            this.treeIndex = treeIndex;
            this.treeList = treeList;
        }

        @Override
        public void run() {

            final Tree<TO> tree = trainTree(bootstrapper);

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

            final Tree<TO> tree = trainTree(bootstrapper);

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
