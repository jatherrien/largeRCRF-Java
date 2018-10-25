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
public class LogRankSingleGroupDifferentiator extends CompetingRiskGroupDifferentiator<CompetingRiskResponse> {

    private final int eventOfFocus;
    private final int[] events;

    @Override
    public Double differentiate(List<CompetingRiskResponse> leftHand, List<CompetingRiskResponse> rightHand) {
        if(leftHand.size() == 0 || rightHand.size() == 0){
            return null;
        }

        final CompetingRiskSetsImpl competingRiskSetsLeft = CompetingRiskUtils.calculateSetsEfficiently(leftHand, events);
        final CompetingRiskSetsImpl competingRiskSetsRight = CompetingRiskUtils.calculateSetsEfficiently(rightHand, events);

        final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, competingRiskSetsLeft, competingRiskSetsRight);

        return Math.abs(valueOfInterest.getNumerator() / valueOfInterest.getVarianceSqrt());

    }

}
