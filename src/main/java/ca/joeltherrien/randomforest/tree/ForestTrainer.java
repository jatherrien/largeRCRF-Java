/*
 * Copyright (c) 2019 Joel Therrien.
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.Bootstrapper;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.Settings;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.utils.DataUtils;
import lombok.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@AllArgsConstructor(access=AccessLevel.PRIVATE)
public class ForestTrainer<Y, TO, FO> {

    private final TreeTrainer<Y, TO> treeTrainer;
    private final List<Covariate> covariates;
    private final ResponseCombiner<TO, FO> treeResponseCombiner;
    private final List<Row<Y>> data;

    // number of trees to try
    @Getter
    @Setter
    private int ntree;

    private final boolean displayProgress; // whether to print to standard output our progress; not always desirable
    private final String saveTreeLocation;
    private final long randomSeed;

    public ForestTrainer(final Settings settings, final List<Row<Y>> data, final List<Covariate> covariates){
        this.ntree = settings.getNtree();
        this.data = data;
        this.displayProgress = true;
        this.saveTreeLocation = settings.getSaveTreeLocation();

        this.covariates = covariates;
        this.treeResponseCombiner = settings.getTreeCombiner();
        this.treeTrainer = new TreeTrainer<>(settings, covariates);

        if(settings.getRandomSeed() != null){
            this.randomSeed = settings.getRandomSeed();
        }
        else{
            this.randomSeed = System.nanoTime();
        }
    }

    /**
     * Train a forest in memory using a single core
     *
     * @param initialForest An Optional possibly containing a pre-trained forest,
     *                      in which case its trees are combined with the new one.
     * @return A trained forest.
     */
    public Forest<TO, FO> trainSerialInMemory(Optional<Forest<TO, FO>> initialForest){

        final List<Tree<TO>> trees = new ArrayList<>(ntree);
        initialForest.ifPresent(forest -> trees.addAll(forest.getTrees()));

        final Bootstrapper<Row<Y>> bootstrapper = new Bootstrapper<>(data);

        for(int j=trees.size(); j<ntree; j++){
            if(displayProgress){
                System.out.print("\rFinished tree " + j + "/" + ntree + " trees");
            }
            final Random random = new Random(this.randomSeed + j);
            trees.add(trainTree(bootstrapper, random));
        }

        if(displayProgress){
            System.out.println("\rFinished tree " + ntree + "/" + ntree + " trees");
            System.out.println("Finished");
        }


        return Forest.<TO, FO>builder()
                .treeResponseCombiner(treeResponseCombiner)
                .trees(trees)
                .covariateList(covariates)
                .build();

    }

    /**
     * Train a forest on the disk using a single core.
     *
     * @param initialForest An Optional possibly containing a pre-trained forest,
     *                      in which case its trees are combined with the new one.
     *                      There cannot be existing trees if the initial forest is
     *                      specified.
     */
    public void trainSerialOnDisk(Optional<Forest<TO, FO>> initialForest){
        // First we need to see how many trees there currently are
        final File folder = new File(saveTreeLocation);
        if(!folder.exists()){
            folder.mkdir();
        }

        if(!folder.isDirectory()){
            throw new IllegalArgumentException("Tree directory must be a directory!");
        }

        final File[] treeFiles = folder.listFiles((file, s) -> s.endsWith(".tree"));
        final List<String> treeFileNames = Arrays.stream(treeFiles).map(file -> file.getName()).collect(Collectors.toList());

        if(initialForest.isPresent() & treeFiles.length > 0){
            throw new IllegalArgumentException("An initial forest is present but trees are also present; not clear how to integrate initial forest into new forest");
        }

        final AtomicInteger treeCount; // tracks how many trees are finished
        // Using an AtomicInteger is overkill for serial code, but this lets us reuse TreeSavedWorker
        if(initialForest.isPresent()){
            final List<Tree<TO>> initialTrees = initialForest.get().getTrees();

            for(int j=0; j<initialTrees.size(); j++){
                final String filename = "tree-" + (j+1) + ".tree";
                final Tree<TO> tree = initialTrees.get(j);

                saveTree(tree, filename);

            }

            treeCount = new AtomicInteger(initialTrees.size());
        } else{
            treeCount = new AtomicInteger(treeFiles.length);
        }


        while(treeCount.get() < ntree){
            if(displayProgress) {
                System.out.print("\rFinished " + treeCount.get() + "/" + ntree + " trees");
            }

            final String treeFileName = "tree-" + (treeCount.get() + 1) + ".tree";

            if(treeFileNames.contains(treeFileName)){
                continue;
            }

            final Random random = new Random(this.randomSeed + treeCount.get());
            final Runnable worker = new TreeSavedWorker(data, treeFileName, treeCount, random);
            worker.run();

        }

        if(displayProgress){
            System.out.println("\rFinished tree " + ntree + "/" + ntree + " trees");
            System.out.println("Finished");
        }

    }

    /**
     * Train a forest in memory using the specified number of threads.
     *
     * @param initialForest An Optional possibly containing a pre-trained forest,
     *                      in which case its trees are combined with the new one.
     * @param threads The number of trees to train at once.
     */
    public Forest<TO, FO> trainParallelInMemory(Optional<Forest<TO, FO>> initialForest, int threads){

        // create a list that is pre-specified in size (I can call the .set method at any index < ntree without
        // the earlier indexes being filled.
        final List<Tree<TO>> trees = Stream.<Tree<TO>>generate(() -> null).limit(ntree).collect(Collectors.toList());

        final int startingCount;
        if(initialForest.isPresent()){
            final List<Tree<TO>> initialTrees = initialForest.get().getTrees();
            for(int j=0; j<initialTrees.size(); j++) {
                trees.set(j, initialTrees.get(j));
            }
            startingCount = initialTrees.size();
        }
        else{
            startingCount = 0;
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);

        for(int j=startingCount; j<ntree; j++){
            final Random random = new Random(this.randomSeed + j);
            final Runnable worker = new TreeInMemoryWorker(data, j, trees, random);
            executorService.execute(worker);
        }

        executorService.shutdown();

        int prevNumberTreesSet = -1;
        boolean running = true;
        while(running){
            try {
                if (executorService.awaitTermination(1, TimeUnit.SECONDS)) running = false;
            } catch (InterruptedException e) {
                // do nothing and just continue; this won't happen and if it did we'd just update the counter quicker
            }

            int numberTreesSet = 0;
            for (final Tree<TO> tree : trees) {
                if (tree != null) {
                    numberTreesSet++;
                }
            }

            if(displayProgress && numberTreesSet != prevNumberTreesSet) {
                // Only output trees set on screen if there was a change
                // In some environments where standard output is streamed to a file this method below causes frequent writes to output
                System.out.print("\rFinished " + numberTreesSet + "/" + ntree + " trees");
                prevNumberTreesSet = numberTreesSet;
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

    /**
     * Train a forest on the disk using a specified number of threads.
     *
     * @param initialForest An Optional possibly containing a pre-trained forest,
     *                      in which case its trees are combined with the new one.
     *                      There cannot be existing trees if the initial forest is
     *                      specified.
     * @param threads The number of trees to train at once.
     */
    public void trainParallelOnDisk(Optional<Forest<TO, FO>> initialForest, int threads){
        // First we need to see how many trees there currently are
        final File folder = new File(saveTreeLocation);
        if(!folder.exists()){
            folder.mkdir();
        }

        if(!folder.isDirectory()){
            throw new IllegalArgumentException("Tree directory must be a directory!");
        }

        final File[] treeFiles = folder.listFiles((file, s) -> s.endsWith(".tree"));
        final List<String> treeFileNames = Arrays.stream(treeFiles).map(file -> file.getName()).collect(Collectors.toList());

        if(initialForest.isPresent() & treeFiles.length > 0){
            throw new IllegalArgumentException("An initial forest is present but trees are also present; not clear how to integrate initial forest into new forest");
        }

        final AtomicInteger treeCount; // tracks how many trees are finished
        if(initialForest.isPresent()){
            final List<Tree<TO>> initialTrees = initialForest.get().getTrees();

            for(int j=0; j<initialTrees.size(); j++){
                final String filename = "tree-" + (j+1) + ".tree";
                final Tree<TO> tree = initialTrees.get(j);

                saveTree(tree, filename);

            }

            treeCount = new AtomicInteger(initialTrees.size());
        } else{
            treeCount = new AtomicInteger(treeFiles.length);
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);

        for(int j=treeCount.get(); j<ntree; j++){
            final String treeFileName = "tree-" + (j+1) + ".tree";
            if(treeFileNames.contains(treeFileName)){
                continue;
            }

            final Random random = new Random(this.randomSeed + j);
            final Runnable worker = new TreeSavedWorker(data, treeFileName, treeCount, random);
            executorService.execute(worker);
        }

        executorService.shutdown();

        int prevNumberTreesSet = -1;
        boolean running = true;
        while(running){
            try {
                if (executorService.awaitTermination(1, TimeUnit.SECONDS)) running = false;
            } catch (InterruptedException e) {
                // do nothing and just continue; this won't happen and if it did we'd just update the counter quicker
            }
            int numberTreesSet = treeCount.get();

            if(displayProgress && numberTreesSet != prevNumberTreesSet) {
                // Only output trees set on screen if there was a change
                // In some environments where standard output is streamed to a file this method below causes frequent writes to output
                System.out.print("\rFinished " + numberTreesSet + "/" + ntree + " trees");
                prevNumberTreesSet = numberTreesSet;

            }

        }

        if(displayProgress){
            System.out.println("\nFinished");
        }

    }

    private Tree<TO> trainTree(final Bootstrapper<Row<Y>> bootstrapper, Random random){
        final List<Row<Y>> bootstrappedData = bootstrapper.bootstrap(random);
        return treeTrainer.growTree(bootstrappedData, random);
    }

    private void saveTree(Tree<TO> tree, String filename){
        try {
            DataUtils.saveObject(tree, saveTreeLocation + "/" + filename);
        } catch (IOException e) {
            System.err.println("IOException while saving " + filename);
            e.printStackTrace();
            System.err.println("Quitting program");
            System.exit(1);
        }
    }


    private class TreeInMemoryWorker implements Runnable {

        private final Bootstrapper<Row<Y>> bootstrapper;
        private final int treeIndex;
        private final List<Tree<TO>> treeList;
        private final Random random;

        TreeInMemoryWorker(final List<Row<Y>> data, final int treeIndex, final List<Tree<TO>> treeList, final Random random) {
            this.bootstrapper = new Bootstrapper<>(data);
            this.treeIndex = treeIndex;
            this.treeList = treeList;
            this.random = random;
        }

        @Override
        public void run() {
            final Tree<TO> tree = trainTree(bootstrapper, random);

            // should be okay as the list structure isn't changing
            treeList.set(treeIndex, tree);

        }
    }


    private class TreeSavedWorker implements Runnable {

        private final Bootstrapper<Row<Y>> bootstrapper;
        private final String filename;
        private final AtomicInteger treeCount;
        private final Random random;

        public TreeSavedWorker(final List<Row<Y>> data, final String filename, final AtomicInteger treeCount, final Random random) {
            this.bootstrapper = new Bootstrapper<>(data);
            this.filename = filename;
            this.treeCount = treeCount;
            this.random = random;
        }

        @Override
        public void run() {
            final Tree<TO> tree = trainTree(bootstrapper, random);

            saveTree(tree, filename);

            treeCount.incrementAndGet();

        }
    }

}
