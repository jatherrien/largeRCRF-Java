package ca.joeltherrien.randomforest.responses.competingrisk.differentiator;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskSets;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskUtils;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * See page 761 of Random survival forests for competing risks by Ishwaran et al.
 *
 */
@RequiredArgsConstructor
public class LogRankSingleGroupDifferentiator extends CompetingRiskGroupDifferentiator<CompetingRiskResponse> {

    private final int eventOfFocus;
    private final int[] events;

    @Override
    protected CompetingRiskSets<CompetingRiskResponse> createCompetingRiskSets(List<CompetingRiskResponse> leftHand, List<CompetingRiskResponse> rightHand){
        return CompetingRiskUtils.calculateSetsEfficiently(leftHand, rightHand, events, true);
    }

    @Override
    protected Double getScore(final CompetingRiskSets<CompetingRiskResponse> competingRiskSets){
        final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, competingRiskSets);
        return Math.abs(valueOfInterest.getNumerator() / valueOfInterest.getVarianceSqrt());
    }

}
