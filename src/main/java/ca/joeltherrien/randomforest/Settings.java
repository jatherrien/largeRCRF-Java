package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.CovariateSettings;
import ca.joeltherrien.randomforest.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.tree.GroupDifferentiator;
import ca.joeltherrien.randomforest.tree.ResponseCombiner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is saved & loaded using a saved configuration file. It contains all relevant settings when training a forest.
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Settings {

    private int numberOfSplits = 5;
    private int nodeSize = 5;
    private int maxNodeDepth = 1000000; // basically no maxNodeDepth

    private String responseCombiner;
    private String groupDifferentiator;
    private String treeResponseCombiner;

    private List<CovariateSettings> covariates = new ArrayList<>();
    private String yVar = "y";

    // number of covariates to randomly try
    private int mtry = 0;

    // number of trees to try
    private int ntree = 500;

    private String dataFileLocation = "data.csv";
    private String saveTreeLocation = "trees/";

    private int numberOfThreads = 1;
    private boolean saveProgress = false;

    public Settings(){} // required for Jackson

    public static Settings load(File file) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        //mapper.enableDefaultTyping();

        final Settings settings = mapper.readValue(file, Settings.class);

        return settings;

    }

    public void save(File file) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        //mapper.enableDefaultTyping();

        // Jackson can struggle with some types of Lists, such as that returned by the useful List.of(...)
        this.covariates = new ArrayList<>(this.covariates);

        mapper.writeValue(file, this);
    }

}
