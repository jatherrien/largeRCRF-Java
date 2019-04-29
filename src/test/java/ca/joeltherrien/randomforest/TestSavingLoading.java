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

import ca.joeltherrien.randomforest.covariates.settings.BooleanCovariateSettings;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.settings.NumericCovariateSettings;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskFunctionCombiner;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.tree.ForestTrainer;
import ca.joeltherrien.randomforest.utils.DataUtils;
import ca.joeltherrien.randomforest.utils.Utils;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestSavingLoading {

    private final int NTREE = 10;

    /**
     * By default uses single log-rank test.
     *
     * @return
     */
    public Settings getSettings(){
        final ObjectNode groupDifferentiatorSettings = new ObjectNode(JsonNodeFactory.instance);
        groupDifferentiatorSettings.set("type", new TextNode("LogRankDifferentiator"));
        groupDifferentiatorSettings.set("eventsOfFocus",
                new ArrayNode(JsonNodeFactory.instance, Utils.easyList(new IntNode(1)))
        );
        groupDifferentiatorSettings.set("events",
                new ArrayNode(JsonNodeFactory.instance, Utils.easyList(new IntNode(1), new IntNode(2)))
        );

        final ObjectNode responseCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
        responseCombinerSettings.set("type", new TextNode("CompetingRiskResponseCombiner"));
        responseCombinerSettings.set("events",
                new ArrayNode(JsonNodeFactory.instance, Utils.easyList(new IntNode(1), new IntNode(2)))
        );
        // not setting times


        final ObjectNode treeCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
        treeCombinerSettings.set("type", new TextNode("CompetingRiskFunctionCombiner"));
        treeCombinerSettings.set("events",
                new ArrayNode(JsonNodeFactory.instance, Utils.easyList(new IntNode(1), new IntNode(2)))
        );
        // not setting times

        final ObjectNode yVarSettings = new ObjectNode(JsonNodeFactory.instance);
        yVarSettings.set("type", new TextNode("CompetingRiskResponse"));
        yVarSettings.set("u", new TextNode("time"));
        yVarSettings.set("delta", new TextNode("status"));

        return Settings.builder()
                .covariateSettings(Utils.easyList(
                        new NumericCovariateSettings("ageatfda"),
                        new BooleanCovariateSettings("idu"),
                        new BooleanCovariateSettings("black"),
                        new NumericCovariateSettings("cd4nadir")
                        )
                )
                .trainingDataLocation("src/test/resources/wihs.csv")
                .validationDataLocation("src/test/resources/wihs.csv")
                .responseCombinerSettings(responseCombinerSettings)
                .treeCombinerSettings(treeCombinerSettings)
                .groupDifferentiatorSettings(groupDifferentiatorSettings)
                .yVarSettings(yVarSettings)
                .maxNodeDepth(100000)
                // TODO fill in these settings
                .mtry(2)
                .nodeSize(6)
                .ntree(NTREE)
                .numberOfSplits(5)
                .numberOfThreads(3)
                .saveProgress(true)
                .saveTreeLocation("src/test/resources/trees/")
                .build();
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
        final Settings settings = getSettings();
        final List<Covariate> covariates = settings.getCovariates();
        final List<Row<CompetingRiskResponse>> dataset = DataUtils.loadData(covariates, settings.getResponseLoader(), settings.getTrainingDataLocation());

        final File directory = new File(settings.getSaveTreeLocation());
        if(directory.exists()){
            cleanup(directory);
        }
        assertFalse(directory.exists());
        directory.mkdir();

        final ForestTrainer<CompetingRiskResponse, CompetingRiskFunctions, CompetingRiskFunctions> forestTrainer = new ForestTrainer<>(settings, dataset, covariates);

        forestTrainer.trainSerialOnDisk();

        assertTrue(directory.exists());
        assertTrue(directory.isDirectory());
        assertEquals(NTREE, directory.listFiles().length);



        final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest = DataUtils.loadForest(directory, new CompetingRiskFunctionCombiner(new int[]{1,2}, null));

        final CovariateRow predictionRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functions = forest.evaluate(predictionRow);
        assertNotNull(functions);
        assertTrue(functions.getCumulativeIncidenceFunction(1).getX().length > 2);


        assertEquals(NTREE, forest.getTrees().size());

        cleanup(directory);

        assertFalse(directory.exists());

    }


    @Test
    public void testSavingLoadingParallel() throws IOException, ClassNotFoundException {
        final Settings settings = getSettings();
        final List<Covariate> covariates = settings.getCovariates();
        final List<Row<CompetingRiskResponse>> dataset = DataUtils.loadData(covariates, settings.getResponseLoader(), settings.getTrainingDataLocation());

        final File directory = new File(settings.getSaveTreeLocation());
        if(directory.exists()){
            cleanup(directory);
        }
        assertFalse(directory.exists());
        directory.mkdir();

        final ForestTrainer<CompetingRiskResponse, CompetingRiskFunctions, CompetingRiskFunctions> forestTrainer = new ForestTrainer<>(settings, dataset, covariates);

        forestTrainer.trainParallelOnDisk(settings.getNumberOfThreads());

        assertTrue(directory.exists());
        assertTrue(directory.isDirectory());
        assertEquals(NTREE, directory.listFiles().length);



        final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest = DataUtils.loadForest(directory, new CompetingRiskFunctionCombiner(new int[]{1,2}, null));

        final CovariateRow predictionRow = getPredictionRow(covariates);

        final CompetingRiskFunctions functions = forest.evaluate(predictionRow);
        assertNotNull(functions);
        assertTrue(functions.getCumulativeIncidenceFunction(1).getX().length > 2);


        assertEquals(NTREE, forest.getTrees().size());

        cleanup(directory);

        assertFalse(directory.exists());

    }

    private void cleanup(File file){
        if(file.isFile()){
            file.delete();
        }
        else{
            for(final File inner : file.listFiles()){
                cleanup(inner);
            }
            file.delete();
        }


    }

}
