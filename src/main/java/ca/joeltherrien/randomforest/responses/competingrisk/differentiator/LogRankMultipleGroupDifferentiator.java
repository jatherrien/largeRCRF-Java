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
public class LogRankMultipleGroupDifferentiator extends CompetingRiskGroupDifferentiator<CompetingRiskResponse> {

    private final int[] events;

    @Override
    protected CompetingRiskSets<CompetingRiskResponse> createCompetingRiskSets(List<CompetingRiskResponse> leftHand, List<CompetingRiskResponse> rightHand){
        return CompetingRiskUtils.calculateSetsEfficiently(leftHand, rightHand, events, true);
    }

    @Override
    protected Double getScore(final CompetingRiskSets<CompetingRiskResponse> competingRiskSets){
        double numerator = 0.0;
        double denominatorSquared = 0.0;

        for(final int eventOfFocus : events){
            final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, competingRiskSets);

            numerator += valueOfInterest.getNumerator()*valueOfInterest.getVarianceSqrt();
            denominatorSquared += valueOfInterest.getVariance();

        }

        return Math.abs(numerator / Math.sqrt(denominatorSquared));
    }


}
