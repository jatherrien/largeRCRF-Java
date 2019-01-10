package ca.joeltherrien.randomforest.utils;


import lombok.Getter;

import java.util.Iterator;

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
