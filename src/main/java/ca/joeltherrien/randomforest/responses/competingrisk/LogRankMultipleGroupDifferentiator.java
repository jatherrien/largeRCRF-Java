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
public class LogRankMultipleGroupDifferentiator extends CompetingRiskGroupDifferentiator<CompetingResponse> {

    private final int[] events;

    @Override
    public Double differentiate(List<CompetingResponse> leftHand, List<CompetingResponse> rightHand) {
        if(leftHand.size() == 0 || rightHand.size() == 0){
            return null;
        }

        double numerator = 0.0;
        double denominatorSquared = 0.0;

        for(final int eventOfFocus : events){
            final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, leftHand, rightHand);

            numerator += valueOfInterest.getNumerator()*valueOfInterest.getVariance();
            denominatorSquared += valueOfInterest.getVarianceSquared();

        }

        return Math.abs(numerator / Math.sqrt(denominatorSquared));

    }

    @Override
    double riskSet(List<CompetingResponse> eventList, double time, int eventOfFocus) {
        return eventList.stream()
                .filter(event -> event.getU() >= time)
                .count();
    }

}
