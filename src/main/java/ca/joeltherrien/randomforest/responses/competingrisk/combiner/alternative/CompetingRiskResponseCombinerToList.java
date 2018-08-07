package ca.joeltherrien.randomforest.responses.competingrisk.combiner.alternative;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.tree.ResponseCombiner;

import java.util.List;

/**
 * This class takes all of the observations in a terminal node and 'combines' them into just a list of the observations.
 *
 * This is used in the alternative approach to only compute the functions at the final stage when combining trees.
 *
 */
public class CompetingRiskResponseCombinerToList implements ResponseCombiner<CompetingRiskResponse, CompetingRiskResponse[]> {

    @Override
    public CompetingRiskResponse[] combine(List<CompetingRiskResponse> responses) {
        final CompetingRiskResponse[] array = new CompetingRiskResponse[responses.size()];

        for(int i=0; i<array.length; i++){
            array[i] = responses.get(i);
        }

        return array;
    }



}
