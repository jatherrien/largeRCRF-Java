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

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SimpleSplitFinder<Y> implements SplitFinder<Y> {

    @Override
    public SplitAndScore<Y, ?> findBestSplit(Iterator<Split<Y, ?>> splitIterator) {
        Double bestScore = null;
        Split<Y, ?> bestSplit = null;

        while(splitIterator.hasNext()){
            final Split<Y, ?> candidateSplit = splitIterator.next();

            final List<Y> leftHand = candidateSplit.getLeftHand().stream().map(Row::getResponse).collect(Collectors.toList());
            final List<Y> rightHand = candidateSplit.getRightHand().stream().map(Row::getResponse).collect(Collectors.toList());

            if(leftHand.isEmpty() || rightHand.isEmpty()){
                continue;
            }

            final Double score = getScore(leftHand, rightHand);

            if(score != null && (bestScore == null || score > bestScore)){
                bestScore = score;
                bestSplit = candidateSplit;
            }
        }

        if(bestSplit == null){
            return null;
        }

        return new SplitAndScore<>(bestSplit, bestScore);
    }

    /**
     * Return a score; higher is better.
     *
     * @param leftHand
     * @param rightHand
     * @return
     */
    public abstract Double getScore(List<Y> leftHand, List<Y> rightHand);

}
