/*
 * Copyright (c) 2019 Joel Therrien.
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.joeltherrien.randomforest.responses.competingrisk.differentiator;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponseWithCensorTime;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskSets;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskUtils;

import java.util.Arrays;
import java.util.List;

/**
 * See page 761 of Random survival forests for competing risks by Ishwaran et al.
 *
 */
public class GrayLogRankDifferentiator extends CompetingRiskGroupDifferentiator<CompetingRiskResponseWithCensorTime> {

    private final int[] eventsOfFocus;
    private final int[] events;

    public GrayLogRankDifferentiator(int[] eventsOfFocus, int[] events){
        this.eventsOfFocus = eventsOfFocus;
        this.events = events;

        if(eventsOfFocus.length == 0){
            throw new IllegalArgumentException("eventsOfFocus must have length greater than 0");
        }

        for(final int eventOfFocus : eventsOfFocus){
            if(Arrays.binarySearch(events, eventOfFocus) == -1){ // i.e. eventOfFocus is not in events
                throw new IllegalArgumentException("Array events must contain every eventOfFocus. Event " + eventOfFocus + " not found.");
            }
        }
    }

    @Override
    protected CompetingRiskSets<CompetingRiskResponseWithCensorTime> createCompetingRiskSets(List<CompetingRiskResponseWithCensorTime> leftHand, List<CompetingRiskResponseWithCensorTime> rightHand){
        return CompetingRiskUtils.calculateGraySetsEfficiently(leftHand, rightHand, events);
    }

    @Override
    protected Double getScore(final CompetingRiskSets<CompetingRiskResponseWithCensorTime> competingRiskSets){
        double numerator = 0.0;
        double denominatorSquared = 0.0;

        for(final int eventOfFocus : eventsOfFocus){
            final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, competingRiskSets);

            // Important note - we follow what randomForestSRC does in its code; not in its documentation.
            // See https://github.com/kogalur/randomForestSRC/issues/27#issuecomment-486017647
            numerator += valueOfInterest.getNumerator();
            denominatorSquared += valueOfInterest.getVariance();

        }

        return Math.abs(numerator / Math.sqrt(denominatorSquared));
    }


}
