package ca.joeltherrien.randomforest.responses.competingrisk.differentiator;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponseWithCensorTime;
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
    public Double differentiate(List<CompetingRiskResponseWithCensorTime> leftHand, List<CompetingRiskResponseWithCensorTime> rightHand) {
        if(leftHand.size() == 0 || rightHand.size() == 0){
            return null;
        }

        double numerator = 0.0;
        double denominatorSquared = 0.0;

        for(final int eventOfFocus : events){
            final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, leftHand, rightHand);

            numerator += valueOfInterest.getNumerator()*valueOfInterest.getVarianceSqrt();
            denominatorSquared += valueOfInterest.getVariance();

        }

        return Math.abs(numerator / Math.sqrt(denominatorSquared));

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
