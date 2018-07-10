package ca.joeltherrien.randomforest.tree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

public interface ResponseCombiner<Y, K> extends Collector<Y, K, Y> {

    Y combine(List<Y> responses);

    final static Map<String, ResponseCombiner> RESPONSE_COMBINER_MAP = new HashMap<>();
    static ResponseCombiner loadResponseCombinerByName(final String name){
        return RESPONSE_COMBINER_MAP.get(name);
    }
    static void registerResponseCombiner(final String name, final ResponseCombiner responseCombiner){
        RESPONSE_COMBINER_MAP.put(name, responseCombiner);
    }

}
