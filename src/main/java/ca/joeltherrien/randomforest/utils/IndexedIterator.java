package ca.joeltherrien.randomforest.utils;

import java.util.Iterator;

public interface IndexedIterator<E> extends Iterator<E> {

    int getIndex();

}
