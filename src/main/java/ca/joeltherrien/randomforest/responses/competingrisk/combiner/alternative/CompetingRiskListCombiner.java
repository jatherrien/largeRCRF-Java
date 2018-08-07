package ca.joeltherrien.randomforest.responses.competingrisk.combiner.alternative;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskResponseCombiner;
import ca.joeltherrien.randomforest.tree.ResponseCombiner;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CompetingRiskListCombiner implements ResponseCombiner<CompetingRiskResponse[], CompetingRiskFunctions> {

    @Getter
    private final CompetingRiskResponseCombiner originalCombiner;

    @Override
    public CompetingRiskFunctions combine(List<CompetingRiskResponse[]> responses) {
        final List<CompetingRiskResponse> completeList = responses.stream().flatMap(Arrays::stream).collect(Collectors.toList());

        return originalCombiner.combine(completeList);
    }
}
