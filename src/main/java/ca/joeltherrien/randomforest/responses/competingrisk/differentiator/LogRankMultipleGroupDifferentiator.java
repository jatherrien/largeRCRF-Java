package ca.joeltherrien.randomforest.responses.competingrisk.differentiator;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskSetsImpl;
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
    public Double getScore(List<CompetingRiskResponse> leftHand, List<CompetingRiskResponse> rightHand) {
        if(leftHand.size() == 0 || rightHand.size() == 0){
            return null;
        }

        final CompetingRiskSetsImpl competingRiskSetsLeft = CompetingRiskUtils.calculateSetsEfficiently(leftHand, events);
        final CompetingRiskSetsImpl competingRiskSetsRight = CompetingRiskUtils.calculateSetsEfficiently(rightHand, events);

        double numerator = 0.0;
        double denominatorSquared = 0.0;

        for(final int eventOfFocus : events){
            final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, competingRiskSetsLeft, competingRiskSetsRight);

            numerator += valueOfInterest.getNumerator()*valueOfInterest.getVarianceSqrt();
            denominatorSquared += valueOfInterest.getVariance();

        }

        return Math.abs(numerator / Math.sqrt(denominatorSquared));

    }

}
