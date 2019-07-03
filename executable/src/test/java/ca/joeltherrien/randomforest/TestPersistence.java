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

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.joeltherrien.randomforest.covariates.settings.BooleanCovariateSettings;
import ca.joeltherrien.randomforest.covariates.settings.FactorCovariateSettings;
import ca.joeltherrien.randomforest.covariates.settings.NumericCovariateSettings;
import ca.joeltherrien.randomforest.utils.Utils;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class TestPersistence {

    @Test
    public void testSaving() throws IOException {
        final ObjectNode splitFinderSettings = new ObjectNode(JsonNodeFactory.instance);
        splitFinderSettings.set("type", new TextNode("WeightedVarianceSplitFinder"));

        final ObjectNode responseCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
        responseCombinerSettings.set("type", new TextNode("MeanResponseCombiner"));

        final ObjectNode treeCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
        treeCombinerSettings.set("type", new TextNode("MeanResponseCombiner"));

        final ObjectNode yVarSettings = new ObjectNode(JsonNodeFactory.instance);
        yVarSettings.set("type", new TextNode("Double"));
        yVarSettings.set("name", new TextNode("y"));

        final Settings settingsOriginal =  Settings.builder()
                .covariateSettings(Utils.easyList(
                        new NumericCovariateSettings("x1"),
                        new BooleanCovariateSettings("x2"),
                        new FactorCovariateSettings("x3", Utils.easyList("cat", "mouse", "dog"))
                        )
                )
                .trainingDataLocation("training_data.csv")
                .validationDataLocation("validation_data.csv")
                .responseCombinerSettings(responseCombinerSettings)
                .treeCombinerSettings(treeCombinerSettings)
                .splitFinderSettings(splitFinderSettings)
                .yVarSettings(yVarSettings)
                .maxNodeDepth(100000)
                .mtry(2)
                .nodeSize(5)
                .ntree(500)
                .numberOfSplits(5)
                .numberOfThreads(1)
                .saveProgress(true)
                .saveTreeLocation("trees/")
                .build();

        final File templateFile = new File("template.yaml");
        settingsOriginal.save(templateFile);

        final Settings reloadedSettings = Settings.load(templateFile);

        assertEquals(settingsOriginal, reloadedSettings);

        //templateFile.delete();


    }
}