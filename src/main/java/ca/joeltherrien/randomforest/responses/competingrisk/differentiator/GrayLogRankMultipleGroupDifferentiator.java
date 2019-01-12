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
public class GrayLogRankMultipleGroupDifferentiator extends CompetingRiskGroupDifferentiator<CompetingRiskResponseWithCensorTime> {

    private final int[] events;

    @Override
    protected CompetingRiskSets<CompetingRiskResponseWithCensorTime> createCompetingRiskSets(List<CompetingRiskResponseWithCensorTime> leftHand, List<CompetingRiskResponseWithCensorTime> rightHand){
        return CompetingRiskUtils.calculateGraySetsEfficiently(leftHand, rightHand, events);
    }

    @Override
    protected Double getScore(final CompetingRiskSets<CompetingRiskResponseWithCensorTime> competingRiskSets){
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
