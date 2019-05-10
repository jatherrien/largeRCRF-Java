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
import ca.joeltherrien.randomforest.covariates.settings.CovariateSettings;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponseWithCensorTime;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskFunctionCombiner;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskResponseCombiner;
import ca.joeltherrien.randomforest.responses.competingrisk.splitfinder.GrayLogRankSplitFinder;
import ca.joeltherrien.randomforest.responses.competingrisk.splitfinder.LogRankSplitFinder;
import ca.joeltherrien.randomforest.responses.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.responses.regression.WeightedVarianceSplitFinder;
import ca.joeltherrien.randomforest.tree.SplitFinder;
import ca.joeltherrien.randomforest.tree.ResponseCombiner;
import ca.joeltherrien.randomforest.utils.DataUtils;
import ca.joeltherrien.randomforest.utils.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * This class is saved & loaded using a saved configuration file. It contains all relevant settings when training a forest.
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Settings {

    private static Map<String, Function<ObjectNode, DataUtils.ResponseLoader>> RESPONSE_LOADER_MAP = new HashMap<>();
    public static Function<ObjectNode, DataUtils.ResponseLoader> getResponseLoaderConstructor(final String name){
        return RESPONSE_LOADER_MAP.get(name.toLowerCase());
    }
    public static void registerResponseLoaderConstructor(final String name, final Function<ObjectNode, DataUtils.ResponseLoader> responseLoaderConstructor){
        RESPONSE_LOADER_MAP.put(name.toLowerCase(), responseLoaderConstructor);
    }

    static{
        registerResponseLoaderConstructor("double",
                node -> new DataUtils.DoubleLoader(node)
        );
        registerResponseLoaderConstructor("CompetingRiskResponse",
                node -> new CompetingRiskResponse.CompetingResponseLoader(node)
        );
        registerResponseLoaderConstructor("CompetingRiskResponseWithCensorTime",
                node -> new CompetingRiskResponseWithCensorTime.CompetingResponseWithCensorTimeLoader(node)
        );
    }

    private static Map<String, Function<ObjectNode, SplitFinder>> SPLIT_FINDER_MAP = new HashMap<>();
    public static Function<ObjectNode, SplitFinder> getSplitFinderConstructor(final String name){
        return SPLIT_FINDER_MAP.get(name.toLowerCase());
    }
    public static void registerSplitFinderConstructor(final String name, final Function<ObjectNode, SplitFinder> splitFinderConstructor){
        SPLIT_FINDER_MAP.put(name.toLowerCase(), splitFinderConstructor);
    }
    static{
        registerSplitFinderConstructor("WeightedVarianceSplitFinder",
                (node) -> new WeightedVarianceSplitFinder()
        );
        registerSplitFinderConstructor("GrayLogRankSplitFinder",
                (objectNode) -> {
                    final int[] eventsOfFocusArray = Utils.jsonToIntArray(objectNode.get("eventsOfFocus"));
                    final int[] eventArray = Utils.jsonToIntArray(objectNode.get("events"));

                    return new GrayLogRankSplitFinder(eventsOfFocusArray, eventArray);
                }
        );
        registerSplitFinderConstructor("LogRankSplitFinder",
                (objectNode) -> {
                    final int[] eventsOfFocusArray = Utils.jsonToIntArray(objectNode.get("eventsOfFocus"));
                    final int[] eventArray = Utils.jsonToIntArray(objectNode.get("events"));

                    return new LogRankSplitFinder(eventsOfFocusArray, eventArray);
                }
        );
    }



    private static Map<String, Function<ObjectNode, ResponseCombiner>> RESPONSE_COMBINER_MAP = new HashMap<>();
    public static Function<ObjectNode, ResponseCombiner> getResponseCombinerConstructor(final String name){
        return RESPONSE_COMBINER_MAP.get(name.toLowerCase());
    }
    public static void registerResponseCombinerConstructor(final String name, final Function<ObjectNode, ResponseCombiner> responseCombinerConstructor){
        RESPONSE_COMBINER_MAP.put(name.toLowerCase(), responseCombinerConstructor);
    }

    static{

        registerResponseCombinerConstructor("MeanResponseCombiner",
                (node) -> new MeanResponseCombiner()
        );
        registerResponseCombinerConstructor("CompetingRiskResponseCombiner",
                (node) -> {
                    final int[] events = Utils.jsonToIntArray(node.get("events"));

                    return new CompetingRiskResponseCombiner(events);

                }
        );

        registerResponseCombinerConstructor("CompetingRiskFunctionCombiner",
                (node) -> {
                    final int[] events = Utils.jsonToIntArray(node.get("events"));

                    double[] times = null;
                    if(node.hasNonNull("times")){
                        times = Utils.jsonToDoubleArray(node.get("times"));
                    }

                    return new CompetingRiskFunctionCombiner(events, times);

                }
        );


    }

    private int numberOfSplits = 5;
    private int nodeSize = 5;
    private int maxNodeDepth = 1000000; // basically no maxNodeDepth
    private boolean checkNodePurity = false;
    private Long randomSeed;

    private ObjectNode responseCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
    private ObjectNode splitFinderSettings = new ObjectNode(JsonNodeFactory.instance);
    private ObjectNode treeCombinerSettings = new ObjectNode(JsonNodeFactory.instance);

    private List<CovariateSettings> covariateSettings = new ArrayList<>();
    private ObjectNode yVarSettings = new ObjectNode(JsonNodeFactory.instance);

    // number of covariates to randomly try
    private int mtry = 0;

    // number of trees to try
    private int ntree = 500;

    private String trainingDataLocation = "data.csv";
    private String validationDataLocation = "data.csv";
    private String saveTreeLocation = "trees/";

    private int numberOfThreads = 1;
    private boolean saveProgress = false;

    public Settings(){
    } // required for Jackson

    public static Settings load(File file) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        //mapper.enableDefaultTyping();

        return mapper.readValue(file, Settings.class);

    }

    public void save(File file) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        //mapper.enableDefaultTyping();

        // Jackson can struggle with some types of Lists, such as that returned by the useful List.of(...)
        this.covariateSettings = new ArrayList<>(this.covariateSettings);

        mapper.writeValue(file, this);
    }

    @JsonIgnore
    public SplitFinder getSplitFinder(){
        final String type = splitFinderSettings.get("type").asText();

        return getSplitFinderConstructor(type).apply(splitFinderSettings);
    }

    @JsonIgnore
    public DataUtils.ResponseLoader getResponseLoader(){
        final String type = yVarSettings.get("type").asText();

        return getResponseLoaderConstructor(type).apply(yVarSettings);
    }

    @JsonIgnore
    public ResponseCombiner getResponseCombiner(){
        final String type = responseCombinerSettings.get("type").asText();

        return getResponseCombinerConstructor(type).apply(responseCombinerSettings);
    }

    @JsonIgnore
    public ResponseCombiner getTreeCombiner(){
        final String type = treeCombinerSettings.get("type").asText();

        return getResponseCombinerConstructor(type).apply(treeCombinerSettings);
    }

    @JsonIgnore
    public List<Covariate> getCovariates(){
        final List<CovariateSettings> covariateSettingsList = this.getCovariateSettings();
        final List<Covariate> covariates = new ArrayList<>(covariateSettingsList.size());
        for(int i = 0; i < covariateSettingsList.size(); i++){
            covariates.add(covariateSettingsList.get(i).build(i));
        }
        return covariates;
    }

}
