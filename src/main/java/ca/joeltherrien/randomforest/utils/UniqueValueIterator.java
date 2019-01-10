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
