package ca.joeltherrien.randomforest.settings;

import ca.joeltherrien.randomforest.Settings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestPersistence {

    @Test
    public void testSaving() throws IOException {
        final Settings settingsOriginal = Settings.builder()
                .covariates(List.of("x1", "x2", "x3"))
                .dataFileLocation("data.csv")
                .groupDifferentiator("WeightedVarianceGroupDifferentiator")
                .responseCombiner("MeanResponseCombiner")
                .treeResponseCombiner("MeanResponseCombiner")
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

        templateFile.delete();


    }
}