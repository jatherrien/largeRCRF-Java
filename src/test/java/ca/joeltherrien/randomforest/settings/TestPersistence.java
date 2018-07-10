package ca.joeltherrien.randomforest.settings;

import ca.joeltherrien.randomforest.Settings;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.joeltherrien.randomforest.covariates.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestPersistence {

    @Test
    public void testSaving() throws IOException {
        final ObjectNode groupDifferentiatorSettings = new ObjectNode(JsonNodeFactory.instance);
        groupDifferentiatorSettings.set("type", new TextNode("WeightedVarianceGroupDifferentiator"));

        final ObjectNode yVarSettings = new ObjectNode(JsonNodeFactory.instance);
        yVarSettings.set("type", new TextNode("Double"));
        yVarSettings.set("name", new TextNode("y"));

        final Settings settingsOriginal =  Settings.builder()
                .covariates(List.of(
                        new NumericCovariateSettings("x1"),
                        new BooleanCovariateSettings("x2"),
                        new FactorCovariateSettings("x3", List.of("cat", "mouse", "dog"))
                        )
                )
                .dataFileLocation("data.csv")
                .responseCombiner("MeanResponseCombiner")
                .treeResponseCombiner("MeanResponseCombiner")
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