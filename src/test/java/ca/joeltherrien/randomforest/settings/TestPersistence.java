package ca.joeltherrien.randomforest.settings;

import ca.joeltherrien.randomforest.Settings;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.joeltherrien.randomforest.covariates.*;
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
        final ObjectNode groupDifferentiatorSettings = new ObjectNode(JsonNodeFactory.instance);
        groupDifferentiatorSettings.set("type", new TextNode("WeightedVarianceGroupDifferentiator"));

        final ObjectNode responseCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
        responseCombinerSettings.set("type", new TextNode("MeanResponseCombiner"));

        final ObjectNode treeCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
        treeCombinerSettings.set("type", new TextNode("MeanResponseCombiner"));

        final ObjectNode yVarSettings = new ObjectNode(JsonNodeFactory.instance);
        yVarSettings.set("type", new TextNode("Double"));
        yVarSettings.set("name", new TextNode("y"));

        final Settings settingsOriginal =  Settings.builder()
                .covariates(Utils.easyList(
                        new NumericCovariateSettings("x1"),
                        new BooleanCovariateSettings("x2"),
                        new FactorCovariateSettings("x3", Utils.easyList("cat", "mouse", "dog"))
                        )
                )
                .trainingDataLocation("training_data.csv")
                .validationDataLocation("validation_data.csv")
                .responseCombinerSettings(responseCombinerSettings)
                .treeCombinerSettings(treeCombinerSettings)
                .groupDifferentiatorSettings(groupDifferentiatorSettings)
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