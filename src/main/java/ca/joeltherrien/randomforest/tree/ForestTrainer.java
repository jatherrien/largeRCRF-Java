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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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
    private final int ntree;

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

    public Forest<TO, FO> trainSerialInMemory(){

        final List<Tree<TO>> trees = new ArrayList<>(ntree);
        final Bootstrapper<Row<Y>> bootstrapper = new Bootstrapper<>(data);

        for(int j=0; j<ntree; j++){
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

    public void trainSerialOnDisk(){
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
        final AtomicInteger treeCount = new AtomicInteger(treeFiles.length); // tracks how many trees are finished
        // Using an AtomicInteger is overkill for serial code, but this lets use reuse TreeSavedWorker

        for(int j=0; j<ntree; j++){
            if(displayProgress) {
                System.out.print("\rFinished " + treeCount.get() + "/" + ntree + " trees");
            }

            final String treeFileName = "tree-" + (j+1) + ".tree";

            if(treeFileNames.contains(treeFileName)){
                continue;
            }

            final Random random = new Random(this.randomSeed + j);
            final Runnable worker = new TreeSavedWorker(data, treeFileName, treeCount, random);
            worker.run();

        }

        if(displayProgress){
            System.out.println("\rFinished tree " + ntree + "/" + ntree + " trees");
            System.out.println("Finished");
        }

    }

    public Forest<TO, FO> trainParallelInMemory(int threads){

        // create a list that is prespecified in size (I can call the .set method at any index < ntree without
        // the earlier indexes being filled.
        final List<Tree<TO>> trees = Stream.<Tree<TO>>generate(() -> null).limit(ntree).collect(Collectors.toList());

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);

        for(int j=0; j<ntree; j++){
            final Random random = new Random(this.randomSeed + j);
            final Runnable worker = new TreeInMemoryWorker(data, j, trees, random);
            executorService.execute(worker);
        }

        int prevNumberTreesSet = -1;
        while(true){
            try {
                if (executorService.awaitTermination(1, TimeUnit.SECONDS)) break;
            } catch (InterruptedException e) {
                System.err.println("There was an InterruptedException while waiting for the forest to finish training; this is unusual but on its own shouldn't be a problem.");
                System.err.println("Please send a bug report about it to joelt@sfu.ca");
                e.printStackTrace();
                // do nothing; this shouldn't be an issue
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

            if(numberTreesSet == ntree){
                executorService.shutdown();
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
        final AtomicInteger treeCount = new AtomicInteger(treeFiles.length); // tracks how many trees are finished

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);

        for(int j=0; j<ntree; j++){
            final String treeFileName = "tree-" + (j+1) + ".tree";
            if(treeFileNames.contains(treeFileName)){
                continue;
            }

            final Random random = new Random(this.randomSeed + j);
            final Runnable worker = new TreeSavedWorker(data, treeFileName, treeCount, random);
            executorService.execute(worker);
        }

        int prevNumberTreesSet = -1;
        while(true){
            try {
                if (executorService.awaitTermination(1, TimeUnit.SECONDS)) break;
            } catch (InterruptedException e) {
                System.err.println("There was an InterruptedException while waiting for the forest to finish training; this is unusual but on its own shouldn't be a problem.");
                System.err.println("Please send a bug report about it to joelt@sfu.ca");
                e.printStackTrace();
                // do nothing; this shouldn't be an issue
            }
            int numberTreesSet = treeCount.get();

            if(displayProgress && numberTreesSet != prevNumberTreesSet) {
                // Only output trees set on screen if there was a change
                // In some environments where standard output is streamed to a file this method below causes frequent writes to output
                System.out.print("\rFinished " + numberTreesSet + "/" + ntree + " trees");
                prevNumberTreesSet = numberTreesSet;

            }

            if(numberTreesSet == ntree){
                executorService.shutdown();
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

            try {
                DataUtils.saveObject(tree, saveTreeLocation + "/" + filename);
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
