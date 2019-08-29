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
import ca.joeltherrien.randomforest.covariates.bool.BooleanCovariate;
import ca.joeltherrien.randomforest.covariates.factor.FactorCovariate;
import ca.joeltherrien.randomforest.covariates.numeric.NumericCovariate;
import ca.joeltherrien.randomforest.responses.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.responses.regression.WeightedVarianceSplitFinder;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.tree.ForestTrainer;
import ca.joeltherrien.randomforest.tree.TreeTrainer;
import ca.joeltherrien.randomforest.utils.DataUtils;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class TestDeterministicForests {

    private final String saveTreeLocation = "src/test/resources/trees/";

    private List<Covariate> generateCovariates(){
        final List<Covariate> covariateList = new ArrayList<>();

        int index = 0;
        for(int j=0; j<5; j++){
            final NumericCovariate numericCovariate = new NumericCovariate("numeric"+j, index, false);
            covariateList.add(numericCovariate);
            index++;
        }

        for(int j=0; j<5; j++){
            final BooleanCovariate booleanCovariate = new BooleanCovariate("boolean"+j, index, false);
            covariateList.add(booleanCovariate);
            index++;
        }

        final List<String> levels = Utils.easyList("cat", "dog", "mouse");
        for(int j=0; j<5; j++){
            final FactorCovariate factorCovariate = new FactorCovariate("factor"+j, index, levels, false);
            covariateList.add(factorCovariate);
            index++;
        }

        return covariateList;
    }

    private Covariate.Value generateRandomValue(Covariate covariate, Random random){
        if(covariate instanceof NumericCovariate){
            return covariate.createValue(random.nextGaussian());
        }
        if(covariate instanceof BooleanCovariate){
            return covariate.createValue(random.nextBoolean());
        }
        if(covariate instanceof FactorCovariate){
            final double itemSelection = random.nextDouble();
            final String item;
            if(itemSelection < 1.0/3.0){
                item = "cat";
            }
            else if(itemSelection < 2.0/3.0){
                item = "dog";
            }
            else{
                item = "mouse";
            }

            return covariate.createValue(item);
        }
        else{
            throw new IllegalArgumentException("Unknown covariate type of class " + covariate.getClass().getName());
        }

    }

    private List<Row<Double>> generateTestData(List<Covariate> covariateList, int n, Random random){
        final List<Row<Double>> rowList = new ArrayList<>();
        for(int i=0; i<n; i++){
            final double response = random.nextGaussian();
            final Covariate.Value[] valueArray = new Covariate.Value[covariateList.size()];

            for(int j=0; j<covariateList.size(); j++){
                valueArray[j] = generateRandomValue(covariateList.get(j), random);
            }

            rowList.add(new Row(valueArray, i, response));
        }

        return rowList;
    }

    @Test
    public void testResultsAlwaysSame() throws IOException, ClassNotFoundException {
        final List<Covariate> covariateList = generateCovariates();

        final Random dataGeneratingRandom = new Random();

        final List<Row<Double>> trainingData = generateTestData(covariateList, 100, dataGeneratingRandom);
        final List<Row<Double>> testData = generateTestData(covariateList, 10, dataGeneratingRandom);

        // pick a new seed at random
        final long trainingSeed = dataGeneratingRandom.nextLong();

        final TreeTrainer<Double, Double> treeTrainer = TreeTrainer.<Double, Double>builder()
                .checkNodePurity(false)
                .covariates(covariateList)
                .maxNodeDepth(100)
                .mtry(1)
                .nodeSize(10)
                .numberOfSplits(1) // want results to be dominated by randomness
                .responseCombiner(new MeanResponseCombiner())
                .splitFinder(new WeightedVarianceSplitFinder())
                .build();

        final ForestTrainer<Double, Double, Double> forestTrainer = ForestTrainer.<Double, Double, Double>builder()
                .treeTrainer(treeTrainer)
                .covariates(covariateList)
                .data(trainingData)
                .displayProgress(false)
                .ntree(10)
                .randomSeed(trainingSeed)
                .treeResponseCombiner(new MeanResponseCombiner())
                .saveTreeLocation(saveTreeLocation)
                .build();

        // By training the referenceForest through one method we also verify that all the methods produce the same forests for a given seed.
        final Forest<Double, Double> referenceForest = forestTrainer.trainSerialInMemory(Optional.empty());

        verifySerialInMemoryTraining(referenceForest, forestTrainer, testData);
        verifyParallelInMemoryTraining(referenceForest, forestTrainer, testData);
        verifySerialOnDiskTraining(referenceForest, forestTrainer, testData);
        verifyParallelOnDiskTraining(referenceForest, forestTrainer, testData);

    }

    /**
     * Tests that if we train a forest under a specified seed for 10 trees, that it is equal to training a forest
     * for 5 trees only, and then starting from that point to train the last 5.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void testInterupptedTrainingProducesSameResults() throws IOException, ClassNotFoundException {
        final List<Covariate> covariateList = generateCovariates();

        final Random dataGeneratingRandom = new Random();

        final List<Row<Double>> trainingData = generateTestData(covariateList, 100, dataGeneratingRandom);
        final List<Row<Double>> testData = generateTestData(covariateList, 10, dataGeneratingRandom);

        // pick a new seed at random
        final long trainingSeed = dataGeneratingRandom.nextLong();

        final TreeTrainer<Double, Double> treeTrainer = TreeTrainer.<Double, Double>builder()
                .checkNodePurity(false)
                .covariates(covariateList)
                .maxNodeDepth(100)
                .mtry(1)
                .nodeSize(10)
                .numberOfSplits(1) // want results to be dominated by randomness
                .responseCombiner(new MeanResponseCombiner())
                .splitFinder(new WeightedVarianceSplitFinder())
                .build();

        final ForestTrainer<Double, Double, Double> forestTrainer5Trees = ForestTrainer.<Double, Double, Double>builder()
                .treeTrainer(treeTrainer)
                .covariates(covariateList)
                .data(trainingData)
                .displayProgress(false)
                .ntree(5)
                .randomSeed(trainingSeed)
                .treeResponseCombiner(new MeanResponseCombiner())
                .saveTreeLocation(saveTreeLocation)
                .build();

        final ForestTrainer<Double, Double, Double> forestTrainer10Trees = ForestTrainer.<Double, Double, Double>builder()
                .treeTrainer(treeTrainer)
                .covariates(covariateList)
                .data(trainingData)
                .displayProgress(false)
                .ntree(10)
                .randomSeed(trainingSeed)
                .treeResponseCombiner(new MeanResponseCombiner())
                .saveTreeLocation(saveTreeLocation)
                .build();

        // By training the referenceForest through one method we also verify that all the methods produce the same forests for a given seed.
        final Forest<Double, Double> referenceForest = forestTrainer10Trees.trainSerialInMemory(Optional.empty());

        final File saveTreeFile = new File(saveTreeLocation);


        forestTrainer5Trees.trainSerialOnDisk(Optional.empty());
        forestTrainer10Trees.trainSerialOnDisk(Optional.empty());
        final Forest<Double, Double> forestSerial = DataUtils.loadForest(saveTreeFile, new MeanResponseCombiner());
        TestUtils.removeFolder(saveTreeFile);
        verifyTwoForestsEqual(testData, referenceForest, forestSerial);


        forestTrainer5Trees.trainParallelOnDisk(Optional.empty(), 4);
        forestTrainer10Trees.trainParallelOnDisk(Optional.empty(), 4);
        final Forest<Double, Double> forestParallel = DataUtils.loadForest(saveTreeFile, new MeanResponseCombiner());
        TestUtils.removeFolder(saveTreeFile);
        verifyTwoForestsEqual(testData, referenceForest, forestParallel);

    }

    private void verifySerialInMemoryTraining(
            final Forest<Double, Double> referenceForest,
            ForestTrainer<Double, Double, Double> forestTrainer,
            List<Row<Double>> testData){

        for(int k=0; k<3; k++){
            final Forest<Double, Double> replicantForest = forestTrainer.trainSerialInMemory(Optional.empty());
            verifyTwoForestsEqual(testData, referenceForest, replicantForest);
        }
    }

    private void verifyParallelInMemoryTraining(
            Forest<Double, Double> referenceForest,
            ForestTrainer<Double, Double, Double> forestTrainer,
            List<Row<Double>> testData){

        for(int k=0; k<3; k++){
            final Forest<Double, Double> replicantForest = forestTrainer.trainParallelInMemory(Optional.empty(), 4);
            verifyTwoForestsEqual(testData, referenceForest, replicantForest);
        }
    }

    private void verifySerialOnDiskTraining(
            Forest<Double, Double> referenceForest,
            ForestTrainer<Double, Double, Double> forestTrainer,
            List<Row<Double>> testData) throws IOException, ClassNotFoundException {

        final MeanResponseCombiner responseCombiner = new MeanResponseCombiner();
        final File saveTreeFile = new File(saveTreeLocation);

        for(int k=0; k<3; k++){
            forestTrainer.trainSerialOnDisk(Optional.empty());
            final Forest<Double, Double> replicantForest = DataUtils.loadForest(saveTreeFile, responseCombiner);
            TestUtils.removeFolder(saveTreeFile);
            verifyTwoForestsEqual(testData, referenceForest, replicantForest);
        }
    }

    private void verifyParallelOnDiskTraining(
            final Forest<Double, Double> referenceForest, ForestTrainer<Double, Double, Double> forestTrainer,
            List<Row<Double>> testData) throws IOException, ClassNotFoundException {

        final MeanResponseCombiner responseCombiner = new MeanResponseCombiner();
        final File saveTreeFile = new File(saveTreeLocation);

        for(int k=0; k<3; k++){
            forestTrainer.trainParallelOnDisk(Optional.empty(), 4);
            final Forest<Double, Double> replicantForest = DataUtils.loadForest(saveTreeFile, responseCombiner);
            TestUtils.removeFolder(saveTreeFile);
            verifyTwoForestsEqual(testData, referenceForest, replicantForest);
        }
    }

    // Technically verifies the two forests give equal predictions on a given test dataset
    private void verifyTwoForestsEqual(final List<Row<Double>> testData,
                                      final Forest<Double, Double> forest1,
                                      final Forest<Double, Double> forest2){

        for(Row row : testData){
            final Double prediction1 = forest1.evaluate(row);
            final Double prediction2 = forest2.evaluate(row);

            // I've noticed that results aren't necessarily always *identical*
            TestUtils.closeEnough(prediction1, prediction2, 0.0000000001);
        }

    }


}
