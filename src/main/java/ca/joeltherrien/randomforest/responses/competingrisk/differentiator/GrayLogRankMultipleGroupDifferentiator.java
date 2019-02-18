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
    protected CompetingRiskSets<CompetingRiskResponseWithCensorTime> createCompetingRiskSets(List<CompetingRiskResponseWithCensorTime> leftHand, List<CompetingRiskResponseWithCensorTime> rightHand){
        return CompetingRiskUtils.calculateGraySetsEfficiently(leftHand, rightHand, events);
    }

    @Override
    protected Double getScore(final CompetingRiskSets<CompetingRiskResponseWithCensorTime> competingRiskSets){
        double numerator = 0.0;
        double denominatorSquared = 0.0;

        for(final int eventOfFocus : events){
            final LogRankValue valueOfInterest = specificLogRankValue(eventOfFocus, competingRiskSets);

            // we use varianceSqrt instead of variance because numerator is not the same as the individual score
            numerator += valueOfInterest.getNumerator()*valueOfInterest.getVarianceSqrt();
            denominatorSquared += valueOfInterest.getVariance();

        }

        return Math.abs(numerator / Math.sqrt(denominatorSquared));
    }


}
