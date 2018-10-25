package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.CovariateSettings;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponseWithCensorTime;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskFunctionCombiner;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskResponseCombiner;
import ca.joeltherrien.randomforest.responses.competingrisk.differentiator.GrayLogRankMultipleGroupDifferentiator;
import ca.joeltherrien.randomforest.responses.competingrisk.differentiator.GrayLogRankSingleGroupDifferentiator;
import ca.joeltherrien.randomforest.responses.competingrisk.differentiator.LogRankMultipleGroupDifferentiator;
import ca.joeltherrien.randomforest.responses.competingrisk.differentiator.LogRankSingleGroupDifferentiator;
import ca.joeltherrien.randomforest.responses.regression.MeanGroupDifferentiator;
import ca.joeltherrien.randomforest.responses.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.responses.regression.WeightedVarianceGroupDifferentiator;
import ca.joeltherrien.randomforest.tree.GroupDifferentiator;
import ca.joeltherrien.randomforest.tree.ResponseCombiner;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.*;
import java.util.function.Function;

/**
 * This class is saved & loaded using a saved configuration file. It contains all relevant settings when training a forest.
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Settings {

    private static Map<String, Function<ObjectNode, DataLoader.ResponseLoader>> RESPONSE_LOADER_MAP = new HashMap<>();
    public static Function<ObjectNode, DataLoader.ResponseLoader> getResponseLoaderConstructor(final String name){
        return RESPONSE_LOADER_MAP.get(name.toLowerCase());
    }
    public static void registerResponseLoaderConstructor(final String name, final Function<ObjectNode, DataLoader.ResponseLoader> responseLoaderConstructor){
        RESPONSE_LOADER_MAP.put(name.toLowerCase(), responseLoaderConstructor);
    }

    static{
        registerResponseLoaderConstructor("double",
                node -> new DataLoader.DoubleLoader(node)
        );
        registerResponseLoaderConstructor("CompetingRiskResponse",
                node -> new CompetingRiskResponse.CompetingResponseLoader(node)
        );
        registerResponseLoaderConstructor("CompetingRiskResponseWithCensorTime",
                node -> new CompetingRiskResponseWithCensorTime.CompetingResponseWithCensorTimeLoader(node)
        );
    }

    private static Map<String, Function<ObjectNode, GroupDifferentiator>> GROUP_DIFFERENTIATOR_MAP = new HashMap<>();
    public static Function<ObjectNode, GroupDifferentiator> getGroupDifferentiatorConstructor(final String name){
        return GROUP_DIFFERENTIATOR_MAP.get(name.toLowerCase());
    }
    public static void registerGroupDifferentiatorConstructor(final String name, final Function<ObjectNode, GroupDifferentiator> groupDifferentiatorConstructor){
        GROUP_DIFFERENTIATOR_MAP.put(name.toLowerCase(), groupDifferentiatorConstructor);
    }
    static{
        registerGroupDifferentiatorConstructor("MeanGroupDifferentiator",
                (node) -> new MeanGroupDifferentiator()
        );
        registerGroupDifferentiatorConstructor("WeightedVarianceGroupDifferentiator",
                (node) -> new WeightedVarianceGroupDifferentiator()
        );
        registerGroupDifferentiatorConstructor("LogRankSingleGroupDifferentiator",
                (objectNode) -> {
                    final int eventOfFocus = objectNode.get("eventOfFocus").asInt();

                    final Iterator<JsonNode> elements = objectNode.get("events").elements();
                    final List<JsonNode> elementList = new ArrayList<>();
                    elements.forEachRemaining(node -> elementList.add(node));

                    final int[] eventArray = elementList.stream().mapToInt(node -> node.asInt()).toArray();

                    return new LogRankSingleGroupDifferentiator(eventOfFocus, eventArray);
                }
        );
        registerGroupDifferentiatorConstructor("GrayLogRankMultipleGroupDifferentiator",
                (objectNode) -> {
                    final Iterator<JsonNode> elements = objectNode.get("events").elements();
                    final List<JsonNode> elementList = new ArrayList<>();
                    elements.forEachRemaining(node -> elementList.add(node));

                    final int[] eventArray = elementList.stream().mapToInt(node -> node.asInt()).toArray();

                    return new GrayLogRankMultipleGroupDifferentiator(eventArray);
                }
        );
        registerGroupDifferentiatorConstructor("LogRankMultipleGroupDifferentiator",
                (objectNode) -> {
                    final Iterator<JsonNode> elements = objectNode.get("events").elements();
                    final List<JsonNode> elementList = new ArrayList<>();
                    elements.forEachRemaining(node -> elementList.add(node));

                    final int[] eventArray = elementList.stream().mapToInt(node -> node.asInt()).toArray();

                    return new LogRankMultipleGroupDifferentiator(eventArray);
                }
        );
        registerGroupDifferentiatorConstructor("GrayLogRankSingleGroupDifferentiator",
                (objectNode) -> {
                    final int eventOfFocus = objectNode.get("eventOfFocus").asInt();

                    final Iterator<JsonNode> elements = objectNode.get("events").elements();
                    final List<JsonNode> elementList = new ArrayList<>();
                    elements.forEachRemaining(node -> elementList.add(node));

                    final int[] eventArray = elementList.stream().mapToInt(node -> node.asInt()).toArray();


                    return new GrayLogRankSingleGroupDifferentiator(eventOfFocus, eventArray);
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
                    final List<Integer> eventList = new ArrayList<>();
                    node.get("events").elements().forEachRemaining(event -> eventList.add(event.asInt()));
                    final int[] events = eventList.stream().mapToInt(i -> i).toArray();


                    return new CompetingRiskResponseCombiner(events);

                }
        );

        registerResponseCombinerConstructor("CompetingRiskFunctionCombiner",
                (node) -> {
                    final List<Integer> eventList = new ArrayList<>();
                    node.get("events").elements().forEachRemaining(event -> eventList.add(event.asInt()));
                    final int[] events = eventList.stream().mapToInt(i -> i).toArray();

                    double[] times = null;
                    // note that times may be null
                    if(node.hasNonNull("times")){
                        final List<Double> timeList = new ArrayList<>();
                        node.get("times").elements().forEachRemaining(time -> timeList.add(time.asDouble()));
                        times = timeList.stream().mapToDouble(db -> db).toArray();
                    }

                    return new CompetingRiskFunctionCombiner(events, times);

                }
        );


    }

    private int numberOfSplits = 5;
    private int nodeSize = 5;
    private int maxNodeDepth = 1000000; // basically no maxNodeDepth
    private boolean checkNodePurity = false;

    private ObjectNode responseCombinerSettings = new ObjectNode(JsonNodeFactory.instance);
    private ObjectNode groupDifferentiatorSettings = new ObjectNode(JsonNodeFactory.instance);
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
    public GroupDifferentiator getGroupDifferentiator(){
        final String type = groupDifferentiatorSettings.get("type").asText();

        return getGroupDifferentiatorConstructor(type).apply(groupDifferentiatorSettings);
    }

    @JsonIgnore
    public DataLoader.ResponseLoader getResponseLoader(){
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
