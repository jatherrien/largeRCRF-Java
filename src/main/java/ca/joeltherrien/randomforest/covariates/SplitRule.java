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

package ca.joeltherrien.randomforest.covariates;

import ca.joeltherrien.randomforest.CovariateRow;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.tree.Split;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public interface SplitRule<V> extends Serializable{

    int getParentCovariateIndex();

    /**
     * Applies the SplitRule to a list of rows and returns a Split object, which contains two lists for both sides.
     * This method is primarily used during the training of a tree when splits are being tested.
     *
     * @param rows
     * @param <Y>
     * @return
     */
    default <Y> Split<Y, V> applyRule(List<Row<Y>> rows) {

         /*
            When working with really large List<Row<Y>> we need to be careful about memory.
            If the lefthand and righthand lists are too small they grow, but for a moment copies exist
            and memory issues arise.

            If they're too large, we waste memory yet again
         */

        // value of 0 = rightHand, value of 1 = leftHand, value of 2 = missingValueHand
        final byte[] whichHand = new byte[rows.size()];
        int countLeftHand = 0;
        int countRightHand = 0;
        int countMissingHand = 0;



        for(int i=0; i<whichHand.length; i++){
            final Row<Y> row = rows.get(i);

            final Covariate.Value<V> value = row.getValueByIndex(getParentCovariateIndex());

            if(value.isNA()){
                countMissingHand++;
                whichHand[i] = 2;
            }

            if(isLeftHand(value)){
                countLeftHand++;
                whichHand[i] = 1;
            }
            else{
                countRightHand++;
                whichHand[i] = 0;
            }

        }


        final List<Row<Y>> missingValueRows = new ArrayList<>(countMissingHand);
        final List<Row<Y>> leftHand = new ArrayList<>(countLeftHand);
        final List<Row<Y>> rightHand = new ArrayList<>(countRightHand);

        for(int i=0; i<whichHand.length; i++){
            final Row<Y> row = rows.get(i);

            if(whichHand[i] == 0){
                rightHand.add(row);
            }
            else if(whichHand[i] == 1){
                leftHand.add(row);
            }
            else{
                missingValueRows.add(row);
            }

        }

        return new Split<>(this, leftHand, rightHand, missingValueRows);
    }

    default boolean isLeftHand(CovariateRow row, final double probabilityNaLeftHand){
        final Covariate.Value<V> value = row.getValueByIndex(getParentCovariateIndex());

        if(value.isNA()){
            return ThreadLocalRandom.current().nextDouble() <= probabilityNaLeftHand;
        }

        return isLeftHand(value);
    }

    boolean isLeftHand(Covariate.Value<V> value);


}
