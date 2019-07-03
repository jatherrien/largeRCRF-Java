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

package ca.joeltherrien.randomforest.utils;


/**
 * Iterator that wraps around a UniqueValueIterator. It continues to iterate until it gets to one of the prespecified indexes,
 * and then proceeds just past that to the end of the existing values it's at.
 *
 * The wrapped iterator must be from a sorted collection of some sort such that equal values are clumped together.
 *  I.e. "b b c c c d d a a" is okay but "a b b c c a" is not as 'a' appears twice at different locations
 *
 * @param <E>
 */
public class UniqueSubsetValueIterator<E> implements IndexedIterator<E> {

    private final UniqueValueIterator<E> iterator;
    private final Integer[] indexValues;

    private int currentIndexSpot = 0;

    public UniqueSubsetValueIterator(final UniqueValueIterator<E> iterator, final Integer[] indexValues){
        this.iterator = iterator;
        this.indexValues = indexValues;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext() && iterator.getIndex() <= indexValues[indexValues.length-1];
    }

    @Override
    public E next() {
        if(hasNext()){
            final int indexToStopBy = indexValues[currentIndexSpot];

            while(iterator.getIndex() <= indexToStopBy){
                iterator.next();
            }

            for(int i = currentIndexSpot + 1; i < indexValues.length; i++){
                if(iterator.getIndex() <= indexValues[i]){
                    currentIndexSpot = i;
                    break;
                }
            }


            return iterator.getCurrentValue();

        }

        return null;

    }

    @Override
    public int getIndex(){
        return iterator.getIndex();
    }


}
