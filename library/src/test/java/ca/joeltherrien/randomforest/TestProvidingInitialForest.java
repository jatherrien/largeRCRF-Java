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

package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.numeric.NumericCovariate;
import ca.joeltherrien.randomforest.responses.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.responses.regression.WeightedVarianceSplitFinder;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.tree.ForestTrainer;
import ca.joeltherrien.randomforest.tree.Tree;
import ca.joeltherrien.randomforest.tree.TreeTrainer;
import ca.joeltherrien.randomforest.utils.DataUtils;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestProvidingInitialForest {

    private Forest<Double, Double> initialForest;
    private List<Covariate> covariateList;
    private List<Row<Double>> data;

    public TestProvidingInitialForest(){
        covariateList = Collections.singletonList(new NumericCovariate("x", 0));

        data = Utils.easyList(
                Row.createSimple(Utils.easyMap("x", "1.0"), covariateList, 1, 1.0),
                Row.createSimple(Utils.easyMap("x", "1.0"), covariateList, 2, 1.5),
                Row.createSimple(Utils.easyMap("x", "2.0"), covariateList, 3, 5.0),
                Row.createSimple(Utils.easyMap("x", "2.0"), covariateList, 4, 6.0)
        );

        final TreeTrainer<Double, Double> treeTrainer = TreeTrainer.<Double, Double>builder()
                .splitFinder(new WeightedVarianceSplitFinder())
                .responseCombiner(new MeanResponseCombiner())
                .checkNodePurity(false)
                .numberOfSplits(0)
                .nodeSize(1)
                .mtry(1)
                .maxNodeDepth(100000)
                .covariates(covariateList)
                .build();

        final ForestTrainer<Double, Double, Double> forestTrainer = ForestTrainer.<Double, Double, Double>builder()
                .treeResponseCombiner(new MeanResponseCombiner())
                .ntree(10)
                .displayProgress(false)
                .data(data)
                .covariates(covariateList)
                .treeTrainer(treeTrainer)
                .build();

        initialForest = forestTrainer.trainSerialInMemory(Optional.empty());
    }

    private final int NTREE = 10;

    private ForestTrainer<Double, Double, Double> getForestTrainer(String saveTreeLocation, int ntree){
        final TreeTrainer<Double, Double> treeTrainer = TreeTrainer.<Double, Double>builder()
                .splitFinder(new WeightedVarianceSplitFinder())
                .responseCombiner(new MeanResponseCombiner())
                .checkNodePurity(false)
                .numberOfSplits(0)
                .nodeSize(1)
                .mtry(1)
                .maxNodeDepth(100000)
                .covariates(covariateList)
                .build();

        final ForestTrainer<Double, Double, Double> forestTrainer = ForestTrainer.<Double, Double, Double>builder()
                .treeResponseCombiner(new MeanResponseCombiner())
                .ntree(ntree)
                .displayProgress(false)
                .data(data)
                .covariates(covariateList)
                .treeTrainer(treeTrainer)
                .saveTreeLocation(saveTreeLocation)
                .build();

        return forestTrainer;
    }

    @Test
    public void testSerialInMemory(){
        final ForestTrainer<Double, Double, Double> forestTrainer = getForestTrainer(null, 20);

        final Forest<Double, Double> newForest = forestTrainer.trainSerialInMemory(Optional.of(initialForest));
        assertEquals(20, newForest.getTrees().size());

        for(Tree<Double> initialTree : initialForest.getTrees()){
            assertTrue(newForest.getTrees().contains(initialTree));
        }
        for(int j=10; j<20; j++){
            final Tree<Double> newTree = newForest.getTrees().get(j);
            assertFalse(initialForest.getTrees().contains(newTree));
        }

    }

    @Test
    public void testParallelInMemory(){
        final ForestTrainer<Double, Double, Double> forestTrainer = getForestTrainer(null, 20);

        final Forest<Double, Double> newForest = forestTrainer.trainParallelInMemory(Optional.of(initialForest), 2);
        assertEquals(20, newForest.getTrees().size());

        for(Tree<Double> initialTree : initialForest.getTrees()){
            assertTrue(newForest.getTrees().contains(initialTree));
        }
        for(int j=10; j<20; j++){
            final Tree<Double> newTree = newForest.getTrees().get(j);
            assertFalse(initialForest.getTrees().contains(newTree));
        }
    }

    @Test
    public void testParallelOnDisk() throws IOException, ClassNotFoundException {
        final String filePath = "src/test/resources/trees/";
        final File directory = new File(filePath);
        if(directory.exists()){
            TestUtils.removeFolder(directory);
        }

        final ForestTrainer<Double, Double, Double> forestTrainer = getForestTrainer(filePath, 20);

        forestTrainer.trainParallelOnDisk(Optional.of(initialForest), 2);

        assertEquals(20, directory.listFiles().length);
        final Forest<Double, Double> newForest = DataUtils.loadForest(directory, new MeanResponseCombiner());



        assertEquals(20, newForest.getTrees().size());

        final List<String> newForestTreesAsStrings = newForest.getTrees().stream()
                .map(tree -> tree.toString()).collect(Collectors.toList());

        for(Tree<Double> initialTree : initialForest.getTrees()){
            assertTrue(newForestTreesAsStrings.contains(initialTree.toString()));
        }

        TestUtils.removeFolder(directory);
    }

    @Test
    public void testSerialOnDisk() throws IOException, ClassNotFoundException {
        final String filePath = "src/test/resources/trees/";
        final File directory = new File(filePath);
        if(directory.exists()){
            TestUtils.removeFolder(directory);
        }
        final ForestTrainer<Double, Double, Double> forestTrainer = getForestTrainer(filePath, 20);


        forestTrainer.trainSerialOnDisk(Optional.of(initialForest));

        assertEquals(20, directory.listFiles().length);

        final Forest<Double, Double> newForest = DataUtils.loadForest(directory, new MeanResponseCombiner());

        assertEquals(20, newForest.getTrees().size());

        final List<String> newForestTreesAsStrings = newForest.getTrees().stream()
                .map(tree -> tree.toString()).collect(Collectors.toList());

        for(Tree<Double> initialTree : initialForest.getTrees()){
            assertTrue(newForestTreesAsStrings.contains(initialTree.toString()));
        }

        TestUtils.removeFolder(directory);
    }

    /*
        We throw IllegalArgumentExceptions when we try providing an initial forest when trees were already saved, because
        it's not clear if the forest being provided is the same one that trees were saved from.
     */
    @Test
    public void verifyExceptions(){
        final String filePath = "src/test/resources/trees/";
        final File directory = new File(filePath);
        if(directory.exists()){
            TestUtils.removeFolder(directory);
        }
        final ForestTrainer<Double, Double, Double> forestTrainer = getForestTrainer(filePath, 10);
        forestTrainer.trainSerialOnDisk(Optional.empty());

        forestTrainer.setNtree(20);
        assertThrows(IllegalArgumentException.class, () -> forestTrainer.trainSerialOnDisk(Optional.of(initialForest)));
        assertThrows(IllegalArgumentException.class, () -> forestTrainer.trainParallelOnDisk(Optional.of(initialForest), 2));

        TestUtils.removeFolder(directory);
    }

}
