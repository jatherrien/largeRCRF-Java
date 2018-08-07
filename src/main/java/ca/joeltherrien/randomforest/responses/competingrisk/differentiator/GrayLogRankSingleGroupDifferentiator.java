package ca.joeltherrien.randomforest.responses.competingrisk.differentiator;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponseWithCensorTime;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * See page 761 of Random survival forests for competing risks by Ishwaran et al.
 *
 */
@RequiredArgsConstructor
public class GrayLogRankSingleGroupDifferentiator extends CompetingRiskGroupDifferentiator<CompetingRiskResponseWithCensorTime> {

    private final int eventOfFocus;

    @Override
    public Double differentiate(List<CompetingRiskResponseWithCensorTime> leftHand, List<CompetingRiskResponseWithCensorTime> rightHand) {
        if(leftHand.size() == 0 || rightHand.size() == 0){
            return null;
        }

        final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, leftHand, rightHand);

        return Math.abs(valueOfInterest.getNumerator() / valueOfInterest.getVarianceSqrt());

    }

    @Override
    double riskSet(List<CompetingRiskResponseWithCensorTime> eventList, double time, int eventOfFocus) {
        return eventList.stream()
                .filter(event -> event.getU() >= time ||
                        (event.getU() < time && event.getDelta() != eventOfFocus && event.getC() > time)
                )
                .count();
    }

}
