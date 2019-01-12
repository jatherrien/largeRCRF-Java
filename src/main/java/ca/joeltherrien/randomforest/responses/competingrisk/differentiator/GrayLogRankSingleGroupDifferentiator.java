package ca.joeltherrien.randomforest.responses.competingrisk.differentiator;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponseWithCensorTime;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskSets;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskUtils;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * See page 761 of Random survival forests for competing risks by Ishwaran et al.
 *
 */
@RequiredArgsConstructor
public class GrayLogRankSingleGroupDifferentiator extends CompetingRiskGroupDifferentiator<CompetingRiskResponseWithCensorTime> {

    private final int eventOfFocus;
    private final int[] events;

    @Override
    protected CompetingRiskSets<CompetingRiskResponseWithCensorTime> createCompetingRiskSets(List<CompetingRiskResponseWithCensorTime> leftHand, List<CompetingRiskResponseWithCensorTime> rightHand){
        return CompetingRiskUtils.calculateGraySetsEfficiently(leftHand, rightHand, events);
    }

    @Override
    protected Double getScore(final CompetingRiskSets<CompetingRiskResponseWithCensorTime> competingRiskSets){
        final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, competingRiskSets);
        return Math.abs(valueOfInterest.getNumerator() / valueOfInterest.getVarianceSqrt());
    }

}
