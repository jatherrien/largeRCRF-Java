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

package ca.joeltherrien.randomforest.responses.regression;

import ca.joeltherrien.randomforest.tree.SimpleGroupDifferentiator;

import java.util.List;

public class WeightedVarianceGroupDifferentiator extends SimpleGroupDifferentiator<Double> {

    @Override
    public Double getScore(List<Double> leftHand, List<Double> rightHand) {

        final double leftHandSize = leftHand.size();
        final double rightHandSize = rightHand.size();
        final double n = leftHandSize + rightHandSize;

        if(leftHandSize == 0 || rightHandSize == 0){
            return null;
        }

        final double leftHandMean = leftHand.stream().mapToDouble(db -> db/leftHandSize).sum();
        final double rightHandMean = rightHand.stream().mapToDouble(db -> db/rightHandSize).sum();

        final double leftVariance = leftHand.stream().mapToDouble(db -> (db - leftHandMean)*(db - leftHandMean)).sum();
        final double rightVariance = rightHand.stream().mapToDouble(db -> (db - rightHandMean)*(db - rightHandMean)).sum();

        return -(leftVariance + rightVariance) / n;

    }

}
