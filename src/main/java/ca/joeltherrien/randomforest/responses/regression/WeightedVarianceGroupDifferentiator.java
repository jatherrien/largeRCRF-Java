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

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.tree.GroupDifferentiator;
import ca.joeltherrien.randomforest.tree.Split;
import ca.joeltherrien.randomforest.tree.SplitAndScore;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class WeightedVarianceGroupDifferentiator implements GroupDifferentiator<Double> {

    private Double getScore(Set leftHand, Set rightHand) {

        if(leftHand.n == 0 || rightHand.n == 0){
            return null;
        }

        final double leftHandMean = leftHand.getMean();
        final double rightHandMean = rightHand.getMean();

        final double leftVariance = leftHand.summationSquared - ((double) leftHand.n) * leftHandMean*leftHandMean;
        final double rightVariance = rightHand.summationSquared - ((double) rightHand.n) * rightHandMean*rightHandMean;

        return -(leftVariance + rightVariance) / (leftHand.n + rightHand.n);
    }

    @Override
    public SplitAndScore<Double, ?> differentiate(Iterator<Split<Double, ?>> splitIterator) {

        if(splitIterator instanceof Covariate.SplitRuleUpdater){
            return differentiateWithSplitUpdater((Covariate.SplitRuleUpdater) splitIterator);
        }
        else{
            return differentiateWithBasicIterator(splitIterator);
        }
    }

    private SplitAndScore<Double, ?> differentiateWithBasicIterator(Iterator<Split<Double, ?>> splitIterator){
        Double bestScore = null;
        Split<Double, ?> bestSplit = null;

        while(splitIterator.hasNext()){
            final Split<Double, ?> candidateSplit = splitIterator.next();

            final List<Double> leftHandList = candidateSplit.getLeftHand().stream().map(Row::getResponse).collect(Collectors.toList());
            final List<Double> rightHandList = candidateSplit.getRightHand().stream().map(Row::getResponse).collect(Collectors.toList());

            if(leftHandList.isEmpty() || rightHandList.isEmpty()){
                continue;
            }

            final Set setLeft = new Set(leftHandList);
            final Set setRight = new Set(rightHandList);

            final Double score = getScore(setLeft, setRight);

            if(score != null && Double.isFinite(score) && (bestScore == null || score > bestScore)){
                bestScore = score;
                bestSplit = candidateSplit;
            }
        }

        if(bestSplit == null){
            return null;
        }

        return new SplitAndScore<>(bestSplit, bestScore);
    }

    private SplitAndScore<Double, ?> differentiateWithSplitUpdater(Covariate.SplitRuleUpdater<Double, ?> splitRuleUpdater) {

        final List<Double> leftInitialSplit = splitRuleUpdater.currentSplit().getLeftHand()
                .stream().map(Row::getResponse).collect(Collectors.toList());
        final List<Double> rightInitialSplit = splitRuleUpdater.currentSplit().getRightHand()
                .stream().map(Row::getResponse).collect(Collectors.toList());

        final Set setLeft = new Set(leftInitialSplit);
        final Set setRight = new Set(rightInitialSplit);

        Double bestScore = null;
        Split<Double, ?> bestSplit = null;

        while(splitRuleUpdater.hasNext()){
            for(Row<Double> rowMoved : splitRuleUpdater.nextUpdate().rowsMovedToLeftHand()){
                setLeft.updateAdd(rowMoved.getResponse());
                setRight.updateRemove(rowMoved.getResponse());
            }

            final Double score = getScore(setLeft, setRight);

            if(score != null && Double.isFinite(score) && (bestScore == null || score > bestScore)){
                bestScore = score;
                bestSplit = splitRuleUpdater.currentSplit();
            }
        }

        if(bestSplit == null){
            return null;
        }

        return new SplitAndScore<>(bestSplit, bestScore);

    }

    private class Set {
        private int n = 0;
        private double summation = 0.0;
        private double summationSquared = 0.0;

        private Set(List<Double> list){
            for(Double number : list){
                updateAdd(number);
            }
        }

        private double getMean(){
            return summation / n;
        }

        private void updateAdd(double number){
            summation += number;
            summationSquared += number*number;
            n++;
        }

        private void updateRemove(double number){
            summation -= number;
            summationSquared -= number*number;
            n--;
        }
    }

}
