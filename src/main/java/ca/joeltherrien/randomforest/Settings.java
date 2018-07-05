package ca.joeltherrien.randomforest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is saved & loaded using a saved configuration file. It contains all relevant settings when training a forest.
 */
@Data
@Builder
@AllArgsConstructor
public class Settings {

    private int numberOfSplits = 5;
    private int nodeSize = 5;
    private int maxNodeDepth = 1000000; // basically no maxNodeDepth

    private String responseCombiner;
    private String groupDifferentiator;
    private String treeResponseCombiner;

    private List<String> covariates = new ArrayList<>();

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

        final Settings settings = mapper.readValue(file, Settings.class);

        return settings;

    }

    public void save(File file) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        mapper.writeValue(file, this);
    }


}
