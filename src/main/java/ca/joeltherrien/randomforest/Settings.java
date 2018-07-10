package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.covariates.CovariateSettings;
import ca.joeltherrien.randomforest.responses.competingrisk.*;
import ca.joeltherrien.randomforest.responses.regression.MeanGroupDifferentiator;
import ca.joeltherrien.randomforest.responses.regression.WeightedVarianceGroupDifferentiator;
import ca.joeltherrien.randomforest.tree.GroupDifferentiator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class is saved & loaded using a saved configuration file. It contains all relevant settings when training a forest.
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Settings {

    private static Map<String, DataLoader.ResponseLoaderConstructor> RESPONSE_LOADER_MAP = new HashMap<>();
    public static DataLoader.ResponseLoaderConstructor getResponseLoaderConstructor(final String name){
        return RESPONSE_LOADER_MAP.get(name.toLowerCase());
    }
    public static void registerResponseLoaderConstructor(final String name, final DataLoader.ResponseLoaderConstructor responseLoaderConstructor){
        RESPONSE_LOADER_MAP.put(name.toLowerCase(), responseLoaderConstructor);
    }

    static{
        registerResponseLoaderConstructor("double",
                node -> new DataLoader.DoubleLoader(node)
        );
        registerResponseLoaderConstructor("CompetingResponse",
                node -> new CompetingResponse.CompetingResponseLoader(node)
        );
        registerResponseLoaderConstructor("CompetingResponseWithCensorTime",
                node -> new CompetingResponseWithCensorTime.CompetingResponseWithCensorTimeLoader(node)
        );
    }

    private static Map<String, GroupDifferentiator.GroupDifferentiatorConstructor> GROUP_DIFFERENTIATOR_MAP = new HashMap<>();
    public static GroupDifferentiator.GroupDifferentiatorConstructor getGroupDifferentiatorConstructor(final String name){
        return GROUP_DIFFERENTIATOR_MAP.get(name.toLowerCase());
    }
    public static void registerGroupDifferentiatorConstructor(final String name, final GroupDifferentiator.GroupDifferentiatorConstructor groupDifferentiatorConstructor){
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

                    return new LogRankSingleGroupDifferentiator(eventOfFocus);
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

                    return new GrayLogRankSingleGroupDifferentiator(eventOfFocus);
                }
        );
    }

    private int numberOfSplits = 5;
    private int nodeSize = 5;
    private int maxNodeDepth = 1000000; // basically no maxNodeDepth

    private String responseCombiner;
    private ObjectNode groupDifferentiatorSettings = new ObjectNode(JsonNodeFactory.instance);
    private String treeResponseCombiner;

    private List<CovariateSettings> covariates = new ArrayList<>();
    private ObjectNode yVarSettings = new ObjectNode(JsonNodeFactory.instance);

    // number of covariates to randomly try
    private int mtry = 0;

    // number of trees to try
    private int ntree = 500;

    private String dataFileLocation = "data.csv";
    private String saveTreeLocation = "trees/";

    private int numberOfThreads = 1;
    private boolean saveProgress = false;

    public Settings(){
    } // required for Jackson

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

    @JsonIgnore
    public GroupDifferentiator getGroupDifferentiator(){
        final String type = groupDifferentiatorSettings.get("type").asText();

        return getGroupDifferentiatorConstructor(type).construct(groupDifferentiatorSettings);
    }

    @JsonIgnore
    public DataLoader.ResponseLoader getResponseLoader(){
        final String type = yVarSettings.get("type").asText();

        return getResponseLoaderConstructor(type).construct(yVarSettings);
    }

}
