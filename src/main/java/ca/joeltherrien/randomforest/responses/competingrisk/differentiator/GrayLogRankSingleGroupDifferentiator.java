package ca.joeltherrien.randomforest.responses.competingrisk.differentiator;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskGraySetsImpl;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponseWithCensorTime;
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
    public Double differentiate(List<CompetingRiskResponseWithCensorTime> leftHand, List<CompetingRiskResponseWithCensorTime> rightHand) {
        if(leftHand.size() == 0 || rightHand.size() == 0){
            return null;
        }

        final CompetingRiskGraySetsImpl competingRiskSetsLeft = CompetingRiskUtils.calculateGraySetsEfficiently(leftHand, events);
        final CompetingRiskGraySetsImpl competingRiskSetsRight = CompetingRiskUtils.calculateGraySetsEfficiently(rightHand, events);

        final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, competingRiskSetsLeft, competingRiskSetsRight);

        return Math.abs(valueOfInterest.getNumerator() / valueOfInterest.getVarianceSqrt());

    }

}
