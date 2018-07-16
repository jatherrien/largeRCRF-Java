package ca.joeltherrien.randomforest.responses.regression;

import ca.joeltherrien.randomforest.tree.ResponseCombiner;

import java.util.List;

/**
 * This implementation of the collector isn't great... but good enough given that I'm not planning to fully support regression trees.
 *
 * (It's not great because you'll lose accuracy as you sum up the doubles, since dividing by n is the very last step.)
 *
 */
public class MeanResponseCombiner implements ResponseCombiner<Double, Double> {

    @Override
    public Double combine(List<Double> responses) {
        double size = responses.size();

        return responses.stream().mapToDouble(db -> db/size).sum();

    }


}
