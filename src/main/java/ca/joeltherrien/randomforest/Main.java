package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.covariates.BooleanCovariateSettings;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.FactorCovariateSettings;
import ca.joeltherrien.randomforest.covariates.NumericCovariateSettings;
import ca.joeltherrien.randomforest.tree.ForestTrainer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {


    public static void main(String[] args) throws IOException {
        if(args.length != 1){
            System.out.println("Must provide one argument - the path to the settings.yaml file.");
            if(args.length == 0){
                System.out.println("Generating template file.");
                defaultTemplate().save(new File("template.yaml"));
            }
            return;
        }
        final File settingsFile = new File(args[0]);
        final Settings settings = Settings.load(settingsFile);

        final List<Covariate> covariates = settings.getCovariates().stream()
                .map(cs -> cs.build()).collect(Collectors.toList());



        final List<Row<Double>> dataset = DataLoader.loadData(covariates, settings.getResponseLoader(), settings.getDataFileLocation());

        final ForestTrainer<Double, Double, Double> forestTrainer = new ForestTrainer<>(settings, dataset, covariates);

        if(settings.isSaveProgress()){
            forestTrainer.trainParallelOnDisk(settings.getNumberOfThreads());
        }
        else{
            forestTrainer.trainParallelInMemory(settings.getNumberOfThreads());
        }


    }



    private static Settings defaultTemplate(){

        final ObjectNode groupDifferentiatorSettings = new ObjectNode(JsonNodeFactory.instance);
        groupDifferentiatorSettings.set("type", new TextNode("WeightedVarianceGroupDifferentiator"));

        final ObjectNode responseCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
        responseCombinerSettings.set("type", new TextNode("MeanResponseCombiner"));

        final ObjectNode treeCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
        treeCombinerSettings.set("type", new TextNode("MeanResponseCombiner"));

        final ObjectNode yVarSettings = new ObjectNode(JsonNodeFactory.instance);
        yVarSettings.set("type", new TextNode("Double"));
        yVarSettings.set("name", new TextNode("y"));

        final Settings settings =  Settings.builder()
                .covariates(List.of(
                        new NumericCovariateSettings("x1"),
                        new BooleanCovariateSettings("x2"),
                        new FactorCovariateSettings("x3", List.of("cat", "mouse", "dog"))
                        )
                )
                .dataFileLocation("data.csv")
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


        return settings;
    }

}
