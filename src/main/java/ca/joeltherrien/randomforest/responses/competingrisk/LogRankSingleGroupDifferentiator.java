package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.tree.GroupDifferentiator;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * See page 761 of Random survival forests for competing risks by Ishwaran et al.
 *
 */
@RequiredArgsConstructor
public class LogRankSingleGroupDifferentiator extends CompetingRiskGroupDifferentiator<CompetingResponse> {

    private final int eventOfFocus;

    @Override
    public Double differentiate(List<CompetingResponse> leftHand, List<CompetingResponse> rightHand) {
        if(leftHand.size() == 0 || rightHand.size() == 0){
            return null;
        }

        final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, leftHand, rightHand);

        return Math.abs(valueOfInterest.getNumerator() / valueOfInterest.getVariance());

    }

    @Override
    double riskSet(List<CompetingResponse> eventList, double time, int eventOfFocus) {
        return eventList.stream()
                .filter(event -> event.getU() >= time)
                .count();
    }

}
