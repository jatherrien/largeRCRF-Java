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

package ca.joeltherrien.randomforest.responses.competingrisk.splitfinder;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskSets;
import ca.joeltherrien.randomforest.tree.SplitFinder;
import ca.joeltherrien.randomforest.tree.Split;
import ca.joeltherrien.randomforest.tree.SplitAndScore;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See page 761 of Random survival forests for competing risks by Ishwaran et al. The class is abstract as Gray's test
 * modifies the abstract method.
 *
 */
public abstract class CompetingRiskSplitFinder<Y extends CompetingRiskResponse> implements SplitFinder<Y> {

    abstract protected CompetingRiskSets<Y> createCompetingRiskSets(List<Y> leftHand, List<Y> rightHand);

    abstract protected Double getScore(final CompetingRiskSets<Y> competingRiskSets);

    @Override
    public SplitAndScore<Y, ?> findBestSplit(Iterator<Split<Y, ?>> splitIterator) {

        if(splitIterator instanceof Covariate.SplitRuleUpdater){
            return findBestSplitWithSplitUpdater((Covariate.SplitRuleUpdater) splitIterator);
        }
        else{
            return findBestSplitWithBasicIterator(splitIterator);
        }
    }

    private SplitAndScore<Y, ?> findBestSplitWithBasicIterator(Iterator<Split<Y, ?>> splitIterator){
        Double bestScore = null;
        Split<Y, ?> bestSplit = null;

        while(splitIterator.hasNext()){
            final Split<Y, ?> candidateSplit = splitIterator.next();

            final List<Y> leftHand = candidateSplit.getLeftHand().stream().map(Row::getResponse).collect(Collectors.toList());
            final List<Y> rightHand = candidateSplit.getRightHand().stream().map(Row::getResponse).collect(Collectors.toList());

            if(leftHand.isEmpty() || rightHand.isEmpty()){
                continue;
            }

            final CompetingRiskSets<Y> competingRiskSets = createCompetingRiskSets(leftHand, rightHand);

            final Double score = getScore(competingRiskSets);

            if(score != null && !Double.isNaN(score) && Double.isFinite(score) && (bestScore == null || score > bestScore)){
                bestScore = score;
                bestSplit = candidateSplit;
            }
        }

        if(bestSplit == null){
            return null;
        }

        return new SplitAndScore<>(bestSplit, bestScore);
    }

    private SplitAndScore<Y, ?> findBestSplitWithSplitUpdater(Covariate.SplitRuleUpdater<Y, ?> splitRuleUpdater) {

        final List<Y> leftInitialSplit = splitRuleUpdater.currentSplit().getLeftHand()
                .stream().map(Row::getResponse).collect(Collectors.toList());
        final List<Y> rightInitialSplit = splitRuleUpdater.currentSplit().getRightHand()
                .stream().map(Row::getResponse).collect(Collectors.toList());

        final CompetingRiskSets<Y> competingRiskSets = createCompetingRiskSets(leftInitialSplit, rightInitialSplit);

        Double bestScore = null;
        Split<Y, ?> bestSplit = null;

        while(splitRuleUpdater.hasNext()){
            for(Row<Y> rowMoved : splitRuleUpdater.nextUpdate().rowsMovedToLeftHand()){
                competingRiskSets.update(rowMoved.getResponse());
            }

            final Double score = getScore(competingRiskSets);

            if(score != null && !Double.isNaN(score) && Double.isFinite(score) && (bestScore == null || score > bestScore)){
                bestScore = score;
                bestSplit = splitRuleUpdater.currentSplit();
            }
        }

        if(bestSplit == null){
            return null;
        }

        return new SplitAndScore<>(bestSplit, bestScore);

    }

    /**
     * Calculates the log rank value (or the Gray's test value) for a *specific* event cause.
     *
     * @param eventOfFocus
     * @param competingRiskSets A summary of the different sets used in the calculation
     * @return
     */
    LogRankValue specificLogRankValue(final int eventOfFocus, final CompetingRiskSets<Y> competingRiskSets){

        double summation = 0.0;
        double variance = 0.0;

        final double[] distinctTimes = competingRiskSets.getDistinctTimes();

        for(int k = 0; k<distinctTimes.length; k++){
            final double time_k = distinctTimes[k];
            final double weight = weight(time_k); // W_j(t_k)
            final double numberEventsAtTimeDaughterLeft = competingRiskSets.getNumberOfEventsLeft(k, eventOfFocus); // // d_{j,l}(t_k)
            final double numberEventsAtTimeDaughterTotal = competingRiskSets.getNumberOfEventsTotal(k, eventOfFocus); // d_j(t_k)

            final double individualsAtRiskDaughterLeft = competingRiskSets.getRiskSetLeft(k, eventOfFocus); // Y_l(t_k)
            final double individualsAtRiskDaughterTotal = competingRiskSets.getRiskSetTotal(k, eventOfFocus); // Y(t_k)

            final double deltaSummation = weight*(numberEventsAtTimeDaughterLeft - numberEventsAtTimeDaughterTotal*individualsAtRiskDaughterLeft/individualsAtRiskDaughterTotal);
            final double deltaVariance = weight*weight*numberEventsAtTimeDaughterTotal*individualsAtRiskDaughterLeft/individualsAtRiskDaughterTotal
                    * (1.0 - individualsAtRiskDaughterLeft / individualsAtRiskDaughterTotal)
                    * ((individualsAtRiskDaughterTotal - numberEventsAtTimeDaughterTotal) / (individualsAtRiskDaughterTotal - 1.0));

            // Note - notation differs slightly with what is found in STAT 855 notes, but they are equivalent.
            // Note - if individualsAtRisk == 1 then variance will be NaN.
            if(!Double.isNaN(deltaVariance)){
                summation += deltaSummation;
                variance += deltaVariance;
            }
        }

        return new LogRankValue(summation, variance);
    }

    double weight(double time){
        return 1.0; // TODO - make configurable
        // A value of 1 "corresponds to the standard log-rank test which has optimal power for detecting alternatives where the cause-specific hazards are proportional"
        //TODO - look into what weights might be more appropriate.
    }

    @Data
    @AllArgsConstructor
    static class LogRankValue{
        private final double numerator;
        private final double variance;

        public double getVarianceSqrt(){
            return Math.sqrt(variance);
        }
    }


}
