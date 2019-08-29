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
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.tree.ForestTrainer;
import ca.joeltherrien.randomforest.tree.TreeTrainer;
import ca.joeltherrien.randomforest.utils.DataUtils;
import ca.joeltherrien.randomforest.utils.ResponseLoader;
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

        final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest = DataUtils.loadForest(directory, new CompetingRiskFunctionCombiner(new int[]{1,2}, null));

        final CovariateRow predictionRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functions = forest.evaluate(predictionRow);
        assertNotNull(functions);
        assertTrue(functions.getCumulativeIncidenceFunction(1).getX().length > 2);


        assertEquals(NTREE, forest.getTrees().size());

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



        final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest = DataUtils.loadForest(directory, new CompetingRiskFunctionCombiner(new int[]{1,2}, null));

        final CovariateRow predictionRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functions = forest.evaluate(predictionRow);
        assertNotNull(functions);
        assertTrue(functions.getCumulativeIncidenceFunction(1).getX().length > 2);


        assertEquals(NTREE, forest.getTrees().size());

        TestUtils.removeFolder(directory);

        assertFalse(directory.exists());

    }



}
