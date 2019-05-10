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

import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.settings.BooleanCovariateSettings;
import ca.joeltherrien.randomforest.covariates.settings.FactorCovariateSettings;
import ca.joeltherrien.randomforest.covariates.settings.NumericCovariateSettings;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskErrorRateCalculator;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponseWithCensorTime;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskFunctionCombiner;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.tree.ForestTrainer;
import ca.joeltherrien.randomforest.tree.ResponseCombiner;
import ca.joeltherrien.randomforest.utils.DataUtils;
import ca.joeltherrien.randomforest.utils.StepFunction;
import ca.joeltherrien.randomforest.utils.Utils;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Random;

public class Main {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if(args.length < 2){
            System.out.println("Must provide two arguments - the path to the settings.yaml file and instructions to either train or analyze.");
            System.out.println("Note that analyzing only supports competing risk data, and that you must then specify a sample size for testing errors.");
            if(args.length == 0){
                final File templateFile = new File("template.yaml");
                if(templateFile.exists()){
                    System.out.println("Template file exists; not creating a new one");
                }
                else{
                    System.out.println("Generating template file.");
                    defaultTemplate().save(templateFile);
                }


            }
            return;
        }
        final File settingsFile = new File(args[0]);
        final Settings settings = Settings.load(settingsFile);

        final List<Covariate> covariates = settings.getCovariates();

        if(args[1].equalsIgnoreCase("train")){
            final List<Row> dataset = DataUtils.loadData(covariates, settings.getResponseLoader(), settings.getTrainingDataLocation());

            final ForestTrainer forestTrainer = new ForestTrainer(settings, dataset, covariates);

            if(settings.isSaveProgress()){
                if(settings.getNumberOfThreads() > 1){
                    forestTrainer.trainParallelOnDisk(settings.getNumberOfThreads());
                } else{
                    forestTrainer.trainSerialOnDisk();
                }
            }
            else{
                if(settings.getNumberOfThreads() > 1){
                    forestTrainer.trainParallelInMemory(settings.getNumberOfThreads());
                } else{
                    forestTrainer.trainSerialInMemory();
                }
            }
        }
        else if(args[1].equalsIgnoreCase("analyze")){
            // Perform different prediction measures

            if(args.length < 3){
                System.out.println("Specify error sample size");
                return;
            }

            final String yVarType = settings.getYVarSettings().get("type").asText();

            if(!yVarType.equalsIgnoreCase("CompetingRiskResponse") && !yVarType.equalsIgnoreCase("CompetingRiskResponseWithCensorTime")){
                System.out.println("Analyze currently only works on competing risk data");
                return;
            }

            final ResponseCombiner<?, CompetingRiskFunctions> responseCombiner = settings.getTreeCombiner();
            final int[] events;

            if(responseCombiner instanceof CompetingRiskFunctionCombiner){
                events = ((CompetingRiskFunctionCombiner) responseCombiner).getEvents();
            }
            else{
                System.out.println("Unsupported tree combiner");
                return;
            }

            final List<Row<CompetingRiskResponse>> dataset = DataUtils.loadData(covariates, settings.getResponseLoader(), settings.getValidationDataLocation());

            // Let's reduce this down to n
            final int n = Integer.parseInt(args[2]);
            Utils.reduceListToSize(dataset, n, new Random());

            final File folder = new File(settings.getSaveTreeLocation());
            final Forest<?, CompetingRiskFunctions> forest = DataUtils.loadForest(folder, responseCombiner);

            final boolean useBootstrapPredictions = settings.getTrainingDataLocation().equals(settings.getValidationDataLocation());

            if(useBootstrapPredictions){
                System.out.println("Finished loading trees + dataset; creating calculator and evaluating OOB predictions");
            }
            else{
                System.out.println("Finished loading trees + dataset; creating calculator and evaluating predictions");
            }


            final CompetingRiskErrorRateCalculator errorRateCalculator = new CompetingRiskErrorRateCalculator(dataset, forest, useBootstrapPredictions);
            final PrintWriter printWriter = new PrintWriter(settings.getSaveTreeLocation() + "/errors.txt");

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
                final StepFunction censorDistribution = Utils.estimateOneMinusECDF(censorTimes);

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

        final ObjectNode splitFinderSettings = new ObjectNode(JsonNodeFactory.instance);
        splitFinderSettings.set("type", new TextNode("WeightedVarianceSplitFinder"));

        final ObjectNode responseCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
        responseCombinerSettings.set("type", new TextNode("MeanResponseCombiner"));

        final ObjectNode treeCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
        treeCombinerSettings.set("type", new TextNode("MeanResponseCombiner"));

        final ObjectNode yVarSettings = new ObjectNode(JsonNodeFactory.instance);
        yVarSettings.set("type", new TextNode("Double"));
        yVarSettings.set("name", new TextNode("y"));

        return Settings.builder()
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
    }

}
