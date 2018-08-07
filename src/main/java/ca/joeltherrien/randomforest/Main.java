package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.covariates.BooleanCovariateSettings;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.FactorCovariateSettings;
import ca.joeltherrien.randomforest.covariates.NumericCovariateSettings;
import ca.joeltherrien.randomforest.responses.competingrisk.*;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskFunctionCombiner;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.tree.ForestTrainer;
import ca.joeltherrien.randomforest.utils.MathFunction;
import ca.joeltherrien.randomforest.utils.Utils;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

public class Main {


    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if(args.length < 2){
            System.out.println("Must provide two arguments - the path to the settings.yaml file and instructions to either train or analyze.");
            System.out.println("Note that analyzing only supports competing risk data, and that you must then specify a sample size for testing errors.");
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

        if(args[1].equalsIgnoreCase("train")){
            final List<Row> dataset = DataLoader.loadData(covariates, settings.getResponseLoader(), settings.getDataFileLocation());

            final ForestTrainer forestTrainer = new ForestTrainer(settings, dataset, covariates);

            if(settings.isSaveProgress()){
                forestTrainer.trainParallelOnDisk(settings.getNumberOfThreads());
            }
            else{
                forestTrainer.trainParallelInMemory(settings.getNumberOfThreads());
            }
        }
        else if(args[1].equalsIgnoreCase("analyze")){
            // Perform different prediction measures

            if(args.length < 3){
                System.out.println("Specify error sample size");
            }

            final String yVarType = settings.getYVarSettings().get("type").asText();

            if(!yVarType.equalsIgnoreCase("CompetingRiskResponse") && !yVarType.equalsIgnoreCase("CompetingRiskResponseWithCensorTime")){
                System.out.println("Analyze currently only works on competing risk data");
            }

            final CompetingRiskFunctionCombiner responseCombiner = (CompetingRiskFunctionCombiner) settings.getTreeCombiner();
            final int[] events = responseCombiner.getEvents();

            final List<Row<CompetingRiskResponse>> dataset = DataLoader.loadData(covariates, settings.getResponseLoader(), settings.getDataFileLocation());

            // Let's reduce this down to n
            final int n = Integer.parseInt(args[2]);
            Utils.reduceListToSize(dataset, n);

            final File folder = new File(settings.getSaveTreeLocation());
            final Forest<CompetingRiskFunctions, CompetingRiskFunctions> forest = DataLoader.loadForest(folder, responseCombiner);

            System.out.println("Finished loading trees + dataset; creating calculator and evaluating OOB predictions");

            final CompetingRiskErrorRateCalculator errorRateCalculator = new CompetingRiskErrorRateCalculator(dataset, forest);
            final PrintWriter printWriter = new PrintWriter(settings.getSaveTreeLocation() + "/errors.txt");

            System.out.println("Running Naive Mortality");

            final double naiveMortality = errorRateCalculator.calculateNaiveMortalityError(events);
            printWriter.write("Naive Mortality: ");
            printWriter.write(Double.toString(naiveMortality));
            printWriter.write('\n');

            System.out.println("Running Naive Concordance");

            final double[] naiveConcordance = errorRateCalculator.calculateConcordance(events);
            printWriter.write("Naive concordance:\n");
            for(int i=0; i<events.length; i++){
                printWriter.write('\t');
                printWriter.write(Integer.toString(events[i]));
                printWriter.write(": ");
                printWriter.write(Double.toString(naiveConcordance[i]));
                printWriter.write('\n');
            }

            if(yVarType.equalsIgnoreCase("CompetingRiskResponseWithCensorTime")){
                System.out.println("Running IPCW Concordance - creating censor distribution");

                final double[] censorTimes = dataset.stream()
                        .mapToDouble(row -> ((CompetingRiskResponseWithCensorTime) row.getResponse()).getC())
                        .toArray();
                final MathFunction censorDistribution = Utils.estimateOneMinusECDF(censorTimes);

                System.out.println("Finished generating censor distribution - running concordance");

                final double[] ipcwConcordance = errorRateCalculator.calculateIPCWConcordance(events, censorDistribution);
                printWriter.write("IPCW concordance:\n");
                for(int i=0; i<events.length; i++){
                    printWriter.write('\t');
                    printWriter.write(Integer.toString(events[i]));
                    printWriter.write(": ");
                    printWriter.write(Double.toString(ipcwConcordance[i]));
                    printWriter.write('\n');
                }
            }


            printWriter.close();
        }
        else{
            System.out.println("Invalid instruction; use either train or analyze.");
            System.out.println("Note that analyzing only supports competing risk data.");
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
