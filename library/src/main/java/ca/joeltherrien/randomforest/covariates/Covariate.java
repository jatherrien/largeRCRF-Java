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

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.tree.Split;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public interface Covariate<V> extends Serializable, Comparable<Covariate> {

    String getName();

    int getIndex();

    <Y> Iterator<Split<Y, V>> generateSplitRuleUpdater(final List<Row<Y>> data, final int number, final Random random);

    Value<V> createValue(V value);

    /**
     * Creates a Value of the appropriate type from a String; primarily used when parsing CSVs.
     *
     * @param value
     * @return
     */
    Value<V> createValue(String value);

    boolean hasNAs();

    default int compareTo(Covariate other){
        return getIndex() - other.getIndex();
    }

    boolean haveNASplitPenalty();

    interface Value<V> extends Serializable{

        Covariate<V> getParent();

        V getValue();

        boolean isNA();

    }

    interface SplitRuleUpdater<Y, V> extends Iterator<Split<Y, V>>{
        Split<Y, V> currentSplit();
        boolean currentSplitValid();
        SplitUpdate<Y, V> nextUpdate();
    }

    interface SplitUpdate<Y, V> {
        SplitRule<V> getSplitRule();
        Collection<Row<Y>> rowsMovedToLeftHand();
    }




}
