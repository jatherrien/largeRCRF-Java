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

package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.CovariateRow;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.TestUtils;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.bool.BooleanCovariate;
import ca.joeltherrien.randomforest.covariates.numeric.NumericCovariate;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskErrorRateCalculator;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskFunctionCombiner;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskResponseCombiner;
import ca.joeltherrien.randomforest.responses.competingrisk.splitfinder.LogRankSplitFinder;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.tree.ForestTrainer;
import ca.joeltherrien.randomforest.tree.Node;
import ca.joeltherrien.randomforest.tree.TreeTrainer;
import ca.joeltherrien.randomforest.utils.ResponseLoader;
import ca.joeltherrien.randomforest.utils.StepFunction;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static ca.joeltherrien.randomforest.TestUtils.assertCumulativeFunction;
import static ca.joeltherrien.randomforest.TestUtils.closeEnough;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCompetingRisk {

    private static final String DEFAULT_FILEPATH = "src/test/resources/wihs.csv";

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
                .ntree(100)
                .saveTreeLocation("trees/")
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
    public void testSingleTree() throws IOException {

        // by only using BooleanCovariates (only one split rule) we can guarantee identical results with randomForestSRC on one tree.
        final List<Covariate> covariates = Utils.easyList(
                new BooleanCovariate("idu", 0, false),
                new BooleanCovariate("black", 1, false)
        );

        final List<Row<CompetingRiskResponse>> dataset = getData(covariates, "src/test/resources/wihs.bootstrapped.csv");

        final TreeTrainer<CompetingRiskResponse, CompetingRiskFunctions> treeTrainer = getTreeTrainerBuilder(covariates).build();

        final Node<CompetingRiskFunctions> node = treeTrainer.growTree(dataset, new Random());

        final CovariateRow newRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functions = node.evaluate(newRow);

        final StepFunction causeOneCIFFunction = functions.getCumulativeIncidenceFunction(1);
        final StepFunction causeTwoCIFFunction = functions.getCumulativeIncidenceFunction(2);
        final StepFunction cumHazOneFunction = functions.getCauseSpecificHazardFunction(1);
        final StepFunction cumHazTwoFunction = functions.getCauseSpecificHazardFunction(2);

        final double margin = 0.0000001;
        closeEnough(0.003003003, causeOneCIFFunction.evaluate(0.02), margin);
        closeEnough(0.166183852, causeOneCIFFunction.evaluate(1.00), margin);
        closeEnough(0.715625487, causeOneCIFFunction.evaluate(6.50), margin);
        closeEnough(0.794796334, causeOneCIFFunction.evaluate(10.60), margin);
        closeEnough(0.794796334, causeOneCIFFunction.evaluate(10.80), margin);


        closeEnough(0.08149211, causeTwoCIFFunction.evaluate(1.00), margin);
        closeEnough(0.14926318, causeTwoCIFFunction.evaluate(6.50), margin);
        closeEnough(0.15332850, causeTwoCIFFunction.evaluate(10.80), margin);


        closeEnough(0.1888601, cumHazOneFunction.evaluate(1.00), margin);
        closeEnough(1.6189759, cumHazOneFunction.evaluate(6.50), margin);
        closeEnough(2.4878342, cumHazOneFunction.evaluate(10.80), margin);


        closeEnough(0.08946513, cumHazTwoFunction.evaluate(1.00), margin);
        closeEnough(0.32801830, cumHazTwoFunction.evaluate(6.50), margin);
        closeEnough(0.36505534, cumHazTwoFunction.evaluate(10.80), margin);


    }

    /**
     * Note - this test triggers a situation where the variance calculation in the log-rank test experiences an NaN.
     *
     * @throws IOException
     */
    @Test
    public void testSingleTree2() throws IOException {

        final List<Covariate> covariates = getCovariates();
        final List<Row<CompetingRiskResponse>> dataset = getData(covariates, "src/test/resources/wihs.bootstrapped2.csv");

        final TreeTrainer<CompetingRiskResponse, CompetingRiskFunctions> treeTrainer = getTreeTrainerBuilder(covariates)
                .mtry(4)
                .numberOfSplits(0)
                .build();

        final Node<CompetingRiskFunctions> node = treeTrainer.growTree(dataset, new Random());

        final CovariateRow newRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functions = node.evaluate(newRow);

        final StepFunction causeOneCIFFunction = functions.getCumulativeIncidenceFunction(1);
        final StepFunction causeTwoCIFFunction = functions.getCumulativeIncidenceFunction(2);
        final StepFunction cumHazOneFunction = functions.getCauseSpecificHazardFunction(1);
        final StepFunction cumHazTwoFunction = functions.getCauseSpecificHazardFunction(2);


        final double margin = 0.0000001;
        closeEnough(0, causeOneCIFFunction.evaluate(0.02), margin);
        closeEnough(0.555555555, causeOneCIFFunction.evaluate(0.4), margin);
        closeEnough(0.66666666666, causeOneCIFFunction.evaluate(0.8), margin);
        closeEnough(0.88888888888, causeOneCIFFunction.evaluate(0.9), margin);
        closeEnough(1.0, causeOneCIFFunction.evaluate(1.0), margin);

        /*
        closeEnough(0.08149211, causeTwoCIFFunction.evaluate(1.00).getY(), margin);
        closeEnough(0.14926318, causeTwoCIFFunction.evaluate(6.50).getY(), margin);
        closeEnough(0.15332850, causeTwoCIFFunction.evaluate(10.80).getY(), margin);


        closeEnough(0.1888601, cumHazOneFunction.evaluate(1.00).getY(), margin);
        closeEnough(1.6189759, cumHazOneFunction.evaluate(6.50).getY(), margin);
        closeEnough(2.4878342, cumHazOneFunction.evaluate(10.80).getY(), margin);


        closeEnough(0.08946513, cumHazTwoFunction.evaluate(1.00).getY(), margin);
        closeEnough(0.32801830, cumHazTwoFunction.evaluate(6.50).getY(), margin);
        closeEnough(0.36505534, cumHazTwoFunction.evaluate(10.80).getY(), margin);
        */

    }

    @Test
    public void testLogRankSplitFinderTwoBooleans() throws IOException {
        // by only using BooleanCovariates (only one split rule) we can guarantee identical results with randomForestSRC on one tree.
        final List<Covariate> covariates = Utils.easyList(
                new BooleanCovariate("idu", 0, false),
                new BooleanCovariate("black", 1, false)
        );


        final List<Row<CompetingRiskResponse>> dataset = getData(covariates, DEFAULT_FILEPATH);

        final ForestTrainer<CompetingRiskResponse, CompetingRiskFunctions, CompetingRiskFunctions> forestTrainer =
                getForestBuilder(covariates, dataset, getTreeTrainerBuilder(covariates).build()).build();

        final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest = forestTrainer.trainSerialInMemory(Optional.empty());

        // prediction row
        //    time status ageatfda   idu black cd4nadir
        //409  1.3      1       35 FALSE FALSE     0.81
        final CovariateRow newRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functions = forest.evaluate(newRow);

        assertCumulativeFunction(functions.getCauseSpecificHazardFunction(1));
        assertCumulativeFunction(functions.getCauseSpecificHazardFunction(2));
        assertCumulativeFunction(functions.getCumulativeIncidenceFunction(1));
        assertCumulativeFunction(functions.getCumulativeIncidenceFunction(2));


        closeEnough(0.63, functions.getCumulativeIncidenceFunction(1).evaluate(4.0), 0.01);
        closeEnough(0.765, functions.getCumulativeIncidenceFunction(1).evaluate(10.8), 0.01);

        closeEnough(0.163, functions.getCumulativeIncidenceFunction(2).evaluate(4.0), 0.01);
        closeEnough(0.195, functions.getCumulativeIncidenceFunction(2).evaluate(10.8), 0.01);

        final CompetingRiskErrorRateCalculator errorRateCalculator = new CompetingRiskErrorRateCalculator(dataset, forest, true);
        final double[] errorRates = errorRateCalculator.calculateConcordance(new int[]{1,2});

        // Error rates happen to be about the same
        /* randomForestSRC results; ignored for now
        closeEnough(0.4795, errorRates[0], 0.007);
        closeEnough(0.478, errorRates[1], 0.008);
        */

        System.out.println(errorRates[0]);
        System.out.println(errorRates[1]);


        closeEnough(0.452, errorRates[0], 0.02);
        closeEnough(0.446, errorRates[1], 0.02);
    }

    @Test
    public void testDataset() throws IOException {
        final List<Covariate> covariates = getCovariates();

        final List<Row<CompetingRiskResponse>> dataset = getData(covariates, DEFAULT_FILEPATH);

        // Let's count the events and make sure the data was correctly read.
        int countCensored = 0;
        int countEventOne = 0;
        int countEventTwo = 0;
        for(final Row<CompetingRiskResponse> row : dataset){
            final CompetingRiskResponse response = row.getResponse();

            if(response.getDelta() == 0){
                countCensored++;
            }
            else if(response.getDelta() == 1){
                countEventOne++;
            }
            else if(response.getDelta() == 2){
                countEventTwo++;
            }
            else{
                throw new RuntimeException("There's an event of type " + response.getDelta());
            }

        }

        assertEquals(126, countCensored);
        assertEquals(679, countEventOne);
        assertEquals(359, countEventTwo);
    }


    @Test
    public void testLogRankSplitFinderAllCovariates() throws IOException {
        final List<Covariate> covariates = getCovariates();
        final List<Row<CompetingRiskResponse>> dataset = getData(covariates, DEFAULT_FILEPATH);


        final ForestTrainer<CompetingRiskResponse, CompetingRiskFunctions, CompetingRiskFunctions> forestTrainer =
                getForestBuilder(covariates, dataset, getTreeTrainerBuilder(covariates).build())
                .ntree(300) // results are too variable at 100
                .build();

        final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest = forestTrainer.trainSerialInMemory(Optional.empty());

        // prediction row
        //    time status ageatfda   idu black cd4nadir
        //409  1.3      1       35 FALSE FALSE     0.81
        final CovariateRow newRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functions = forest.evaluate(newRow);

        assertCumulativeFunction(functions.getCauseSpecificHazardFunction(1));
        assertCumulativeFunction(functions.getCauseSpecificHazardFunction(2));
        assertCumulativeFunction(functions.getCumulativeIncidenceFunction(1));
        assertCumulativeFunction(functions.getCumulativeIncidenceFunction(2));

        // We seem to consistently underestimate the results.
        final double endProbability = functions.getCumulativeIncidenceFunction(1).evaluate(10000000);
        assertTrue(endProbability > 0.74, "Results should match randomForestSRC; had " + endProbability); // note; most observations from randomForestSRC hover around 0.78 but I've seen it as low as 0.72

        final CompetingRiskErrorRateCalculator errorRateCalculator = new CompetingRiskErrorRateCalculator(dataset, forest, true);
        final double[] errorRates = errorRateCalculator.calculateConcordance(new int[]{1,2});

        System.out.println(errorRates[0]);
        System.out.println(errorRates[1]);

        /* randomForestSRC results; ignored for now
        closeEnough(0.412, errorRates[0], 0.007);
        closeEnough(0.384, errorRates[1], 0.007);
        */

        // Consistency results
        closeEnough(0.395, errorRates[0], 0.02);
        closeEnough(0.345, errorRates[1], 0.02);

    }

}
