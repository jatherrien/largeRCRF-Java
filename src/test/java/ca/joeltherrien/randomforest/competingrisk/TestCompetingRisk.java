package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.*;
import ca.joeltherrien.randomforest.covariates.*;
import ca.joeltherrien.randomforest.responses.competingrisk.*;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.tree.ForestTrainer;
import ca.joeltherrien.randomforest.tree.Node;
import ca.joeltherrien.randomforest.tree.TreeTrainer;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestCompetingRisk {


    /**
     * By default uses single log-rank test.
     *
     * @return
     */
    public Settings getSettings(){
        final ObjectNode groupDifferentiatorSettings = new ObjectNode(JsonNodeFactory.instance);
        groupDifferentiatorSettings.set("type", new TextNode("LogRankSingleGroupDifferentiator"));
        groupDifferentiatorSettings.set("eventOfFocus", new IntNode(1));

        final ObjectNode responseCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
        responseCombinerSettings.set("type", new TextNode("CompetingRiskResponseCombiner"));
        responseCombinerSettings.set("events",
                new ArrayNode(JsonNodeFactory.instance, List.of(new IntNode(1), new IntNode(2)))
        );
        // not setting times


        final ObjectNode treeCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
        treeCombinerSettings.set("type", new TextNode("CompetingRiskFunctionCombiner"));
        treeCombinerSettings.set("events",
                new ArrayNode(JsonNodeFactory.instance, List.of(new IntNode(1), new IntNode(2)))
        );
        // not setting times

        final ObjectNode yVarSettings = new ObjectNode(JsonNodeFactory.instance);
        yVarSettings.set("type", new TextNode("CompetingRiskResponse"));
        yVarSettings.set("u", new TextNode("time"));
        yVarSettings.set("delta", new TextNode("status"));

        return Settings.builder()
                .covariates(List.of(
                        new NumericCovariateSettings("ageatfda"),
                        new BooleanCovariateSettings("idu"),
                        new BooleanCovariateSettings("black"),
                        new NumericCovariateSettings("cd4nadir")
                        )
                )
                .dataFileLocation("src/test/resources/wihs.csv")
                .responseCombinerSettings(responseCombinerSettings)
                .treeCombinerSettings(treeCombinerSettings)
                .groupDifferentiatorSettings(groupDifferentiatorSettings)
                .yVarSettings(yVarSettings)
                .maxNodeDepth(100000)
                // TODO fill in these settings
                .mtry(2)
                .nodeSize(6)
                .ntree(100)
                .numberOfSplits(5)
                .numberOfThreads(3)
                .saveProgress(true)
                .saveTreeLocation("trees/")
                .build();
    }

    public List<Covariate> getCovariates(Settings settings){
        return settings.getCovariates().stream().map(covariateSettings -> covariateSettings.build()).collect(Collectors.toList());
    }

    public CovariateRow getPredictionRow(List<Covariate> covariates){
        return CovariateRow.createSimple(Map.of(
                "ageatfda", "35",
                "idu", "false",
                "black", "false",
                "cd4nadir", "0.81")
                , covariates, 1);
    }

    @Test
    public void testSingleTree() throws IOException {
        final Settings settings = getSettings();
        settings.setDataFileLocation("src/test/resources/wihs.bootstrapped.csv");
        settings.setCovariates(List.of(
                new BooleanCovariateSettings("idu"),
                new BooleanCovariateSettings("black")
                )); // by only using BooleanCovariates (only one split rule) we can guarantee identical results with randomForestSRC on one tree.

        final List<Covariate> covariates = getCovariates(settings);

        final List<Row<CompetingRiskResponse>> dataset = DataLoader.loadData(covariates, settings.getResponseLoader(), settings.getDataFileLocation());

        final TreeTrainer<CompetingRiskResponse, CompetingRiskFunctions> treeTrainer = new TreeTrainer<>(settings, covariates);
        final Node<CompetingRiskFunctions> node = treeTrainer.growTree(dataset);

        final CovariateRow newRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functions = node.evaluate(newRow);

        final MathFunction causeOneCIFFunction = functions.getCumulativeIncidenceFunction(1);
        final MathFunction causeTwoCIFFunction = functions.getCumulativeIncidenceFunction(2);
        final MathFunction cumHazOneFunction = functions.getCauseSpecificHazardFunction(1);
        final MathFunction cumHazTwoFunction = functions.getCauseSpecificHazardFunction(2);

        final double margin = 0.0000001;
        closeEnough(0.003003003, causeOneCIFFunction.evaluate(0.02).getY(), margin);
        closeEnough(0.166183852, causeOneCIFFunction.evaluate(1.00).getY(), margin);
        closeEnough(0.715625487, causeOneCIFFunction.evaluate(6.50).getY(), margin);
        closeEnough(0.794796334, causeOneCIFFunction.evaluate(10.60).getY(), margin);
        closeEnough(0.794796334, causeOneCIFFunction.evaluate(10.80).getY(), margin);


        closeEnough(0.08149211, causeTwoCIFFunction.evaluate(1.00).getY(), margin);
        closeEnough(0.14926318, causeTwoCIFFunction.evaluate(6.50).getY(), margin);
        closeEnough(0.15332850, causeTwoCIFFunction.evaluate(10.80).getY(), margin);


        closeEnough(0.1888601, cumHazOneFunction.evaluate(1.00).getY(), margin);
        closeEnough(1.6189759, cumHazOneFunction.evaluate(6.50).getY(), margin);
        closeEnough(2.4878342, cumHazOneFunction.evaluate(10.80).getY(), margin);


        closeEnough(0.08946513, cumHazTwoFunction.evaluate(1.00).getY(), margin);
        closeEnough(0.32801830, cumHazTwoFunction.evaluate(6.50).getY(), margin);
        closeEnough(0.36505534, cumHazTwoFunction.evaluate(10.80).getY(), margin);


    }

    /**
     * Note - this test triggers a situation where the variance calculation in the log-rank test experiences an NaN.
     *
     * @throws IOException
     */
    @Test
    public void testSingleTree2() throws IOException {
        final Settings settings = getSettings();
        settings.setMtry(4);
        settings.setNumberOfSplits(0);
        settings.setDataFileLocation("src/test/resources/wihs.bootstrapped2.csv");

        final List<Covariate> covariates = getCovariates(settings);

        final List<Row<CompetingRiskResponse>> dataset = DataLoader.loadData(covariates, settings.getResponseLoader(), settings.getDataFileLocation());

        final TreeTrainer<CompetingRiskResponse, CompetingRiskFunctions> treeTrainer = new TreeTrainer<>(settings, covariates);
        final Node<CompetingRiskFunctions> node = treeTrainer.growTree(dataset);

        final CovariateRow newRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functions = node.evaluate(newRow);

        final MathFunction causeOneCIFFunction = functions.getCumulativeIncidenceFunction(1);
        final MathFunction causeTwoCIFFunction = functions.getCumulativeIncidenceFunction(2);
        final MathFunction cumHazOneFunction = functions.getCauseSpecificHazardFunction(1);
        final MathFunction cumHazTwoFunction = functions.getCauseSpecificHazardFunction(2);


        final double margin = 0.0000001;
        closeEnough(0, causeOneCIFFunction.evaluate(0.02).getY(), margin);
        closeEnough(0.555555555, causeOneCIFFunction.evaluate(0.4).getY(), margin);
        closeEnough(0.66666666666, causeOneCIFFunction.evaluate(0.8).getY(), margin);
        closeEnough(0.88888888888, causeOneCIFFunction.evaluate(0.9).getY(), margin);
        closeEnough(1.0, causeOneCIFFunction.evaluate(1.0).getY(), margin);

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
    public void testLogRankSingleGroupDifferentiatorTwoBooleans() throws IOException {
        final Settings settings = getSettings();
        settings.setCovariates(List.of(
                new BooleanCovariateSettings("idu"),
                new BooleanCovariateSettings("black")
        ));

        final List<Covariate> covariates = getCovariates(settings);

        final List<Row<CompetingRiskResponse>> dataset = DataLoader.loadData(covariates, settings.getResponseLoader(), settings.getDataFileLocation());

        final ForestTrainer<CompetingRiskResponse, CompetingRiskFunctions, CompetingRiskFunctions> forestTrainer = new ForestTrainer<>(settings, dataset, covariates);

        final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest = forestTrainer.trainSerial();

        // prediction row
        //    time status ageatfda   idu black cd4nadir
        //409  1.3      1       35 FALSE FALSE     0.81
        final CovariateRow newRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functions = forest.evaluate(newRow);

        assertCumulativeFunction(functions.getCauseSpecificHazardFunction(1));
        assertCumulativeFunction(functions.getCauseSpecificHazardFunction(2));
        assertCumulativeFunction(functions.getCumulativeIncidenceFunction(1));
        assertCumulativeFunction(functions.getCumulativeIncidenceFunction(2));


        closeEnough(0.63, functions.getCumulativeIncidenceFunction(1).evaluate(4.0).getY(), 0.01);
        closeEnough(0.765, functions.getCumulativeIncidenceFunction(1).evaluate(10.8).getY(), 0.01);

        closeEnough(0.163, functions.getCumulativeIncidenceFunction(2).evaluate(4.0).getY(), 0.01);
        closeEnough(0.195, functions.getCumulativeIncidenceFunction(2).evaluate(10.8).getY(), 0.01);

        final CompetingRiskErrorRateCalculator errorRateCalculator = new CompetingRiskErrorRateCalculator(new int[]{1,2}, null);
        final double[] errorRates = errorRateCalculator.calculateAll(dataset, forest);

        // Error rates happen to be about the same
        closeEnough(0.4795, errorRates[0], 0.007);
        closeEnough(0.478, errorRates[1], 0.008);


    }

    @Test
    public void verifyDataset() throws IOException {
        final Settings settings = getSettings();

        final List<Covariate> covariates = getCovariates(settings);

        final List<Row<CompetingRiskResponse>> dataset = DataLoader.loadData(covariates, settings.getResponseLoader(), settings.getDataFileLocation());

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
    public void testLogRankSingleGroupDifferentiatorAllCovariates() throws IOException {

        final Settings settings = getSettings();

        final List<Covariate> covariates = getCovariates(settings);
        final List<Row<CompetingRiskResponse>> dataset = DataLoader.loadData(covariates, settings.getResponseLoader(), settings.getDataFileLocation());
        final ForestTrainer<CompetingRiskResponse, CompetingRiskFunctions, CompetingRiskFunctions> forestTrainer = new ForestTrainer<>(settings, dataset, covariates);
        final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest = forestTrainer.trainSerial();

        // prediction row
        //    time status ageatfda   idu black cd4nadir
        //409  1.3      1       35 FALSE FALSE     0.81
        final CovariateRow newRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functions = forest.evaluate(newRow);

        assertCumulativeFunction(functions.getCauseSpecificHazardFunction(1));
        assertCumulativeFunction(functions.getCauseSpecificHazardFunction(2));
        assertCumulativeFunction(functions.getCumulativeIncidenceFunction(1));
        assertCumulativeFunction(functions.getCumulativeIncidenceFunction(2));

        final List<Point> causeOneCIFPoints = functions.getCumulativeIncidenceFunction(1).getPoints();

        // We seem to consistently underestimate the results.
        assertTrue(causeOneCIFPoints.get(causeOneCIFPoints.size()-1).getY() > 0.75, "Results should match randomForestSRC; had " + causeOneCIFPoints.get(causeOneCIFPoints.size()-1).getY()); // note; most observations from randomForestSRC hover around 0.78 but I've seen it as low as 0.72

        final CompetingRiskErrorRateCalculator errorRate = new CompetingRiskErrorRateCalculator((CompetingRiskFunctionCombiner) settings.getTreeCombiner(), new int[]{1,2});
        final double[] errorRates = errorRate.calculateAll(dataset, forest);

        System.out.println(errorRates[0]);
        System.out.println(errorRates[1]);

        closeEnough(0.41, errorRates[0], 0.02);
        closeEnough(0.38, errorRates[1], 0.02);

    }

    /**
     * We know the function is cumulative; make sure it is ordered correctly and that that function is monotone.
     *
     * @param function
     */
    private void assertCumulativeFunction(MathFunction function){
        Point previousPoint = null;
        for(final Point point : function.getPoints()){

            if(previousPoint != null){
                assertTrue(previousPoint.getTime() < point.getTime(), "Points should be ordered and strictly different");
                assertTrue(previousPoint.getY() <= point.getY(), "Cumulative incidence functions are monotone");
            }


            previousPoint = point;
        }
    }

    private void closeEnough(double expected, double actual, double margin){
        assertTrue(Math.abs(expected - actual) < margin, "Expected " + expected + " but saw " + actual);
    }

}
