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

package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.Settings;
import ca.joeltherrien.randomforest.covariates.Covariate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.*;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TreeTrainer<Y, O> {

    private final ResponseCombiner<Y, O> responseCombiner;
    private final GroupDifferentiator<Y> groupDifferentiator;

    /**
     * The number of splits to perform on each covariate. A value of 0 means all possible splits are tried.
     *
     */
    private final int numberOfSplits;
    private final int nodeSize;
    private final int maxNodeDepth;
    private final int mtry;

    /**
     * Whether to check if a node is pure or not when deciding to split. Splitting on a pure node won't change predictive accuracy,
     * but (depending on conditions) may hurt performance.
     */
    private final boolean checkNodePurity;

    private final List<Covariate> covariates;

    public TreeTrainer(final Settings settings, final List<Covariate> covariates){
        this.numberOfSplits = settings.getNumberOfSplits();
        this.nodeSize = settings.getNodeSize();
        this.maxNodeDepth = settings.getMaxNodeDepth();
        this.mtry = settings.getMtry();
        this.checkNodePurity = settings.isCheckNodePurity();

        this.responseCombiner = settings.getResponseCombiner();
        this.groupDifferentiator = settings.getGroupDifferentiator();
        this.covariates = covariates;
    }

    public Tree<O> growTree(List<Row<Y>> data, Random random){

        final Node<O> rootNode = growNode(data, 0, random);
        return new Tree<>(rootNode, data.stream().mapToInt(Row::getId).toArray());

    }

    private Node<O> growNode(List<Row<Y>> data, int depth, Random random){
        // See https://kogalur.github.io/randomForestSRC/theory.html#section3.1 (near bottom)
        if(data.size() >= 2*nodeSize && depth < maxNodeDepth && !nodeIsPure(data)){
            final List<Covariate> covariatesToTry = selectCovariates(this.mtry, random);
            final Split<Y,?> bestSplit = findBestSplitRule(data, covariatesToTry, random);


            if(bestSplit == null){
                return new TerminalNode<>(
                        responseCombiner.combine(
                                data.stream().map(row -> row.getResponse()).collect(Collectors.toList())
                        ), data.size()
                );


            }


            // Now that we have the best split; we need to handle any NAs that were dropped off
            final double probabilityLeftHand = (double) bestSplit.leftHand.size() /
                    (double) (bestSplit.leftHand.size() + bestSplit.rightHand.size());

            // Assign missing values to the split if necessary
            if(bestSplit.getSplitRule().getParent().hasNAs()){
                for(Row<Y> row : data) {
                    if(row.getCovariateValue(bestSplit.getSplitRule().getParent()).isNA()) {
                        final boolean randomDecision = random.nextDouble() <= probabilityLeftHand;

                        if(randomDecision){
                            bestSplit.getLeftHand().add(row);
                        }
                        else{
                            bestSplit.getRightHand().add(row);
                        }

                    }
                }
            }



            final Node<O> leftNode = growNode(bestSplit.leftHand, depth+1, random);
            final Node<O> rightNode = growNode(bestSplit.rightHand, depth+1, random);

            return new SplitNode<>(leftNode, rightNode, bestSplit.getSplitRule(), probabilityLeftHand);

        }
        else{
            return new TerminalNode<>(
                    responseCombiner.combine(
                            data.stream().map(row -> row.getResponse()).collect(Collectors.toList())
                    ), data.size()
            );
        }


    }

    private List<Covariate> selectCovariates(int mtry, Random random){
        if(mtry >= covariates.size()){
            return covariates;
        }

        final List<Covariate> splitCovariates = new ArrayList<>(covariates);
        Collections.shuffle(splitCovariates, random);

        if (splitCovariates.size() > mtry) {
            splitCovariates.subList(mtry, splitCovariates.size()).clear();
        }

        return splitCovariates;
    }

    private Split<Y, ?> findBestSplitRule(List<Row<Y>> data, List<Covariate> covariatesToTry, Random random){

        SplitAndScore<Y, ?> bestSplitAndScore = null;
        final GroupDifferentiator noGenericDifferentiator = groupDifferentiator; // cause Java generics suck

        for(final Covariate covariate : covariatesToTry) {
            final Iterator<Split> iterator = covariate.generateSplitRuleUpdater(data, this.numberOfSplits, random);

            final SplitAndScore<Y, ?> candidateSplitAndScore = noGenericDifferentiator.differentiate(iterator);

            if(candidateSplitAndScore != null && (bestSplitAndScore == null ||
                    candidateSplitAndScore.getScore() > bestSplitAndScore.getScore())) {
                bestSplitAndScore = candidateSplitAndScore;
            }

        }

        if(bestSplitAndScore == null){
            return null;
        }

        return bestSplitAndScore.getSplit();

    }

    private boolean nodeIsPure(List<Row<Y>> data){
        if(!checkNodePurity){
            return false;
        }

        if(data.size() <= 1){
            return true;
        }

        final Y first = data.get(0).getResponse();
        for(int i = 1; i< data.size(); i++){
            if(!data.get(i).getResponse().equals(first)){
                return false;
            }
        }

        return true;
    }

}
