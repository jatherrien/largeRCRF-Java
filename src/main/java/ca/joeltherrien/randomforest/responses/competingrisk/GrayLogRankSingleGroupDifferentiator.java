package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.tree.GroupDifferentiator;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * See page 761 of Random survival forests for competing risks by Ishwaran et al.
 *
 */
@RequiredArgsConstructor
public class GrayLogRankSingleGroupDifferentiator extends CompetingRiskGroupDifferentiator<CompetingResponseWithCensorTime> {

    private final int eventOfFocus;

    @Override
    public Double differentiate(List<CompetingResponseWithCensorTime> leftHand, List<CompetingResponseWithCensorTime> rightHand) {
        if(leftHand.size() == 0 || rightHand.size() == 0){
            return null;
        }

        final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, leftHand, rightHand);

        return Math.abs(valueOfInterest.getNumerator() / valueOfInterest.getVariance());

    }

    @Override
    double riskSet(List<CompetingResponseWithCensorTime> eventList, double time, int eventOfFocus) {
        return eventList.stream()
                .filter(event -> event.getU() >= time ||
                        (event.getU() < time && event.getDelta() != eventOfFocus && event.getC() > time)
                )
                .count();
    }

}
