package ca.joeltherrien.randomforest.regression;

import ca.joeltherrien.randomforest.ResponseCombiner;

import java.util.List;

public class MeanResponseCombiner implements ResponseCombiner<Double> {

    @Override
    public Double combine(List<Double> responses) {
        double size = responses.size();

        return responses.stream().mapToDouble(db -> db/size).sum();

    }
}
