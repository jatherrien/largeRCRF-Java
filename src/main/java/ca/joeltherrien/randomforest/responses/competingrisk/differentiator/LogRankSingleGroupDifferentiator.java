package ca.joeltherrien.randomforest.responses.competingrisk.differentiator;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * See page 761 of Random survival forests for competing risks by Ishwaran et al.
 *
 */
@RequiredArgsConstructor
public class LogRankSingleGroupDifferentiator extends CompetingRiskGroupDifferentiator<CompetingRiskResponse> {

    private final int eventOfFocus;

    @Override
    public Double differentiate(List<CompetingRiskResponse> leftHand, List<CompetingRiskResponse> rightHand) {
        if(leftHand.size() == 0 || rightHand.size() == 0){
            return null;
        }

        final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, leftHand, rightHand);

        return Math.abs(valueOfInterest.getNumerator() / valueOfInterest.getVarianceSqrt());

    }

    @Override
    double riskSet(List<CompetingRiskResponse> eventList, double time, int eventOfFocus) {
        return eventList.stream()
                .filter(event -> event.getU() >= time)
                .count();
    }

}
