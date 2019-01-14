package ca.joeltherrien.randomforest.utils;

import lombok.RequiredArgsConstructor;

import java.util.Iterator;

@RequiredArgsConstructor
public class SingletonIterator<E> implements Iterator<E> {

    private final E value;

    private boolean beenCalled = false;


    @Override
    public boolean hasNext() {
        return !beenCalled;
    }

    @Override
    public E next() {
        if(!beenCalled){
            beenCalled = true;
            return value;
        }

        return null;
    }
}
