package ca.joeltherrien.randomforest.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestUniqueSubsetValueIterator {

    @Test
    public void testIterator1(){
        final List<Integer> testData = Arrays.asList(
                1,1,2,3,5,5,5,6,7,7
        );

        final Integer[] indexes = new Integer[]{2,3,4,5};

        final UniqueValueIterator<Integer> uniqueValueIterator = new UniqueValueIterator<>(testData.iterator());
        final UniqueSubsetValueIterator<Integer> iterator = new UniqueSubsetValueIterator<>(uniqueValueIterator, indexes);

        // we expect to get 2, 3, and 5 back. 5 should happen only once

        assertEquals(iterator.getIndex(), 0);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 2);
        assertEquals(iterator.getIndex(), 3);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 3);
        assertEquals(iterator.getIndex(), 4);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 5);
        assertEquals(iterator.getIndex(), 7);
        assertFalse(iterator.hasNext());


    }

    @Test
    public void testIterator2(){
        final List<Integer> testData = Arrays.asList(
                1,1,2,3,5,5,5,6,7,7
        );

        final Integer[] indexes = new Integer[]{1,8};

        final UniqueValueIterator<Integer> uniqueValueIterator = new UniqueValueIterator<>(testData.iterator());
        final UniqueSubsetValueIterator<Integer> iterator = new UniqueSubsetValueIterator<>(uniqueValueIterator, indexes);

        assertEquals(iterator.getIndex(), 0);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 1);
        assertEquals(iterator.getIndex(), 2);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 7);
        assertEquals(iterator.getIndex(), 10);
        assertFalse(iterator.hasNext());


    }

}
