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
import ca.joeltherrien.randomforest.covariates.numeric.NumericCovariate;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskFunctionCombiner;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskResponseCombiner;
import ca.joeltherrien.randomforest.responses.competingrisk.splitfinder.LogRankSplitFinder;
import ca.joeltherrien.randomforest.tree.*;
import ca.joeltherrien.randomforest.utils.DataUtils;
import ca.joeltherrien.randomforest.utils.ResponseLoader;
import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestSavingLoading {

    private static final int NTREE = 10;
    private static final String DEFAULT_FILEPATH = "src/test/resources/wihs.csv";
    private static final String SAVE_TREE_LOCATION = "src/test/resources/trees/";

    public List<Covariate> getCovariates(){
        return Utils.easyList(
                new NumericCovariate("ageatfda", 0, false),
                new BooleanCovariate("idu", 1, false),
                new BooleanCovariate("black", 2, false),
                new NumericCovariate("cd4nadir", 3, false)
        );
    }

    public ForestTrainer.ForestTrainerBuilder<CompetingRiskResponse, CompetingRiskFunctions, CompetingRiskFunctions> getForestBuilder(
            List<Covariate> covariates,
            List<Row<CompetingRiskResponse>> data,
            TreeTrainer<CompetingRiskResponse, CompetingRiskFunctions> treeTrainer) {

        return ForestTrainer.<CompetingRiskResponse, CompetingRiskFunctions, CompetingRiskFunctions>builder()
                .treeResponseCombiner(new CompetingRiskFunctionCombiner(new int[]{1,2}, null))
                .ntree(NTREE)
                .saveTreeLocation("src/test/resources/trees/")
                .displayProgress(false)
                .covariates(covariates)
                .data(data)
                .treeTrainer(treeTrainer);

    }

    public List<Row<CompetingRiskResponse>> getData(List<Covariate> covariates, String filepath) throws IOException {
        return TestUtils.loadData(
                covariates, new ResponseLoader.CompetingRisksResponseLoader("status", "time"),
                filepath);
    }

    public TreeTrainer.TreeTrainerBuilder<CompetingRiskResponse, CompetingRiskFunctions> getTreeTrainerBuilder(List<Covariate> covariates){
        return TreeTrainer.<CompetingRiskResponse, CompetingRiskFunctions>builder()
                .covariates(covariates)
                .splitFinder(new LogRankSplitFinder(new int[]{1}, new int[]{1,2}))
                .responseCombiner(new CompetingRiskResponseCombiner(new int[]{1,2}))
                .maxNodeDepth(100000)
                .mtry(2)
                .nodeSize(6)
                .numberOfSplits(5);
    }


    public CovariateRow getPredictionRow(List<Covariate> covariates){
        return CovariateRow.createSimple(Utils.easyMap(
                "ageatfda", "35",
                "idu", "false",
                "black", "false",
                "cd4nadir", "0.81")
                , covariates, 1);
    }

    @Test
    public void testSavingLoadingSerial() throws IOException, ClassNotFoundException {
        final List<Covariate> covariates = getCovariates();
        final List<Row<CompetingRiskResponse>> dataset = getData(covariates, DEFAULT_FILEPATH);


        final File directory = new File(SAVE_TREE_LOCATION);
        if(directory.exists()){
            TestUtils.removeFolder(directory);
        }
        assertFalse(directory.exists());
        directory.mkdir();

        final ForestTrainer<CompetingRiskResponse, CompetingRiskFunctions, CompetingRiskFunctions> forestTrainer =
                getForestBuilder(covariates, dataset, getTreeTrainerBuilder(covariates).build()).build();

        forestTrainer.trainSerialOnDisk(Optional.empty());

        assertTrue(directory.exists());
        assertTrue(directory.isDirectory());
        assertEquals(NTREE, directory.listFiles().length);

        final CompetingRiskFunctionCombiner treeResponseCombiner = new CompetingRiskFunctionCombiner(new int[]{1,2}, null);
        final OnlineForest<CompetingRiskFunctions, CompetingRiskFunctions> onlineForest = DataUtils.loadOnlineForest(directory, treeResponseCombiner);
        final OfflineForest<CompetingRiskFunctions, CompetingRiskFunctions> offlineForest = new OfflineForest<>(directory, treeResponseCombiner);

        final CovariateRow predictionRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functionsOnline = onlineForest.evaluate(predictionRow);
        assertNotNull(functionsOnline);
        assertTrue(functionsOnline.getCumulativeIncidenceFunction(1).getX().length > 2);

        final CompetingRiskFunctions functionsOffline = offlineForest.evaluate(predictionRow);
        assertTrue(competingFunctionsEqual(functionsOffline, functionsOnline));


        assertEquals(NTREE, onlineForest.getTrees().size());

        TestUtils.removeFolder(directory);

        assertFalse(directory.exists());

    }


    @Test
    public void testSavingLoadingParallel() throws IOException, ClassNotFoundException {
        final List<Covariate> covariates = getCovariates();
        final List<Row<CompetingRiskResponse>> dataset = getData(covariates, DEFAULT_FILEPATH);

        final File directory = new File(SAVE_TREE_LOCATION);
        if(directory.exists()){
            TestUtils.removeFolder(directory);
        }
        assertFalse(directory.exists());
        directory.mkdir();

        final ForestTrainer<CompetingRiskResponse, CompetingRiskFunctions, CompetingRiskFunctions> forestTrainer =
                getForestBuilder(covariates, dataset, getTreeTrainerBuilder(covariates).build()).build();

        forestTrainer.trainParallelOnDisk(Optional.empty(), 2);

        assertTrue(directory.exists());
        assertTrue(directory.isDirectory());
        assertEquals(NTREE, directory.listFiles().length);


        final CompetingRiskFunctionCombiner treeResponseCombiner = new CompetingRiskFunctionCombiner(new int[]{1,2}, null);
        final OnlineForest<CompetingRiskFunctions, CompetingRiskFunctions> onlineForest = DataUtils.loadOnlineForest(directory, treeResponseCombiner);
        final OfflineForest<CompetingRiskFunctions, CompetingRiskFunctions> offlineForest = new OfflineForest<>(directory, treeResponseCombiner);

        final CovariateRow predictionRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functionsOnline = onlineForest.evaluate(predictionRow);
        assertNotNull(functionsOnline);
        assertTrue(functionsOnline.getCumulativeIncidenceFunction(1).getX().length > 2);


        final CompetingRiskFunctions functionsOffline = offlineForest.evaluate(predictionRow);
        assertTrue(competingFunctionsEqual(functionsOffline, functionsOnline));


        assertEquals(NTREE, onlineForest.getTrees().size());

        TestUtils.removeFolder(directory);

        assertFalse(directory.exists());

    }

    /*
        We don't implement equals() methods on the below mentioned classes because then we'd need to implement an
        appropriate hashCode() method that's consistent with the equals(), and we only need plain equals() for
        these tests.
     */

    private boolean competingFunctionsEqual(CompetingRiskFunctions f1 ,CompetingRiskFunctions f2){
        if(!functionsEqual(f1.getSurvivalCurve(), f2.getSurvivalCurve())){
            return false;
        }

        for(int i=1; i<=2; i++){
            if(!functionsEqual(f1.getCauseSpecificHazardFunction(i), f2.getCauseSpecificHazardFunction(i))){
                return false;
            }
            if(!functionsEqual(f1.getCumulativeIncidenceFunction(i), f2.getCumulativeIncidenceFunction(i))){
                return false;
            }
        }

        return true;
    }

    private boolean functionsEqual(RightContinuousStepFunction f1, RightContinuousStepFunction f2){

        final double[] f1X = f1.getX();
        final double[] f2X = f2.getX();

        final double[] f1Y = f1.getY();
        final double[] f2Y = f2.getY();

        // first compare array lengths
        if(f1X.length != f2X.length){
            return false;
        }
        if(f1Y.length != f2Y.length){
            return false;
        }

        // TODO - better comparisons of doubles. I don't really care too much though as this equals method is only being used in tests
        final double delta = 0.000001;

        if(Math.abs(f1.getDefaultY() - f2.getDefaultY()) > delta){
            return false;
        }

        for(int i=0; i < f1X.length; i++){
            if(Math.abs(f1X[i] - f2X[i]) > delta){
                return false;
            }
            if(Math.abs(f1Y[i] - f2Y[i]) > delta){
                return false;
            }
        }

        return true;

    }


}
