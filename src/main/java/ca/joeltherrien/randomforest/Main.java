package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.covariates.BooleanCovariateSettings;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.FactorCovariateSettings;
import ca.joeltherrien.randomforest.covariates.NumericCovariateSettings;
import ca.joeltherrien.randomforest.tree.ForestTrainer;
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

        final List<Row<Double>> dataset = loadData(covariates, settings);

        final ForestTrainer<Double> forestTrainer = new ForestTrainer<>(settings, dataset, covariates);

        if(settings.isSaveProgress()){
            forestTrainer.trainParallelOnDisk(settings.getNumberOfThreads());
        }
        else{
            forestTrainer.trainParallelInMemory(settings.getNumberOfThreads());
        }


    }


    public static List<Row<Double>> loadData(final List<Covariate> covariates, final Settings settings) throws IOException {

        final List<Row<Double>> dataset = new ArrayList<>();

        final Reader input = new FileReader(settings.getDataFileLocation());
        final CSVParser parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(input);


        int id = 1;
        for(final CSVRecord record : parser){
            final Map<String, Covariate.Value> covariateValueMap = new HashMap<>();

            for(final Covariate<?> covariate : covariates){
                covariateValueMap.put(covariate.getName(), covariate.createValue(record.get(covariate.getName())));
            }

            final String yStr = record.get(settings.getYVar());
            final Double yNum = Double.parseDouble(yStr);

            dataset.add(new Row<>(covariateValueMap, id++, yNum));

        }

        return dataset;

    }

    private static Settings defaultTemplate(){
        return Settings.builder()
                .covariates(List.of(
                        new NumericCovariateSettings("x1"),
                        new BooleanCovariateSettings("x2"),
                        new FactorCovariateSettings("x3", List.of("cat", "mouse", "dog"))
                        )
                )
                .yVar("y")
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
    }

}
