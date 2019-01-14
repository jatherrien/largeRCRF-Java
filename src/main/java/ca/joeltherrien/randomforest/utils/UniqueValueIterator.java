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


import lombok.Getter;

import java.util.Iterator;

/**
 * Iterator that wraps around another iterator. It continues to iterate until it gets to the *end* of a sequence of identical values.
 * It also tracks the current index in the original iterator.
 *
 * The wrapped iterator must be from a sorted collection of some sort such that equal values are clumped together.
 *  I.e. "b b c c c d d a a" is okay but "a b b c c a" is not as 'a' appears twice at different locations
 *
 * @param <E>
 */
public class UniqueValueIterator<E> implements IndexedIterator<E> {

    private final Iterator<E> wrappedIterator;

    @Getter private E currentValue = null;
    @Getter private E nextValue;

    public UniqueValueIterator(final Iterator<E> wrappedIterator){
        this.wrappedIterator = wrappedIterator;
        this.nextValue = wrappedIterator.next();
    }

    // Count must return the index of the last value of the sequence returned by next()
    @Getter
    private int index = 0;

    @Override
    public boolean hasNext() {
        return nextValue != null;
    }

    @Override
    public E next() {

        int count = 1;
        while(wrappedIterator.hasNext()){
            final E currentIteratorValue = wrappedIterator.next();

            if(currentIteratorValue.equals(nextValue)){
                count++;
            }
            else{
                index +=count;
                currentValue = nextValue;
                nextValue = currentIteratorValue;

                return currentValue;
            }

        }

        if(nextValue != null){
            index += count;
            currentValue = nextValue;
            nextValue = null;

            return currentValue;
        }
        else{
            return null;
        }


    }
}
