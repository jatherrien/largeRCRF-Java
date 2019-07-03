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

import java.io.Serializable;
import java.util.Iterator;

/**
 * When choosing an optimal node to split on, we choose the split that maximizes the difference between the two groups.
 * The SplitFinder has one method that cycles through an iterator of Splits (FYI; check if the iterator is an
 * instance of Covariate.SplitRuleUpdater; in which case you get access to the rows that change between splits)
 *
 *  If you want to implement a very trivial SplitFinder that just takes two Lists as arguments, try extending
 *  SimpleSplitFinder.
 */
public interface SplitFinder<Y> extends Serializable {

    SplitAndScore<Y, ?> findBestSplit(Iterator<Split<Y, ?>> splitIterator);

}
