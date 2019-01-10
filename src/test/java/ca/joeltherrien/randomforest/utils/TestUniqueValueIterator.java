package ca.joeltherrien.randomforest.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestUniqueValueIterator {

    @Test
    public void testIterator1(){
        final List<Integer> testData = Arrays.asList(
                1,1,2,3,5,5,5,6,7,7
        );

        final UniqueValueIterator<Integer> iterator = new UniqueValueIterator<>(testData.iterator());

        assertEquals(iterator.getIndex(), 0);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 1);
        assertEquals(iterator.getIndex(), 2);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 2);
        assertEquals(iterator.getIndex(), 3);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 3);
        assertEquals(iterator.getIndex(), 4);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 5);
        assertEquals(iterator.getIndex(), 7);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 6);
        assertEquals(iterator.getIndex(), 8);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 7);
        assertEquals(iterator.getIndex(), 10);
        assertTrue(!iterator.hasNext());

    }

    @Test
    public void testIterator2(){
        final List<Integer> testData = Arrays.asList(
                1,2,3,5,5,5,6,7 // same numbers; but 1 and 7 only appear once each
        );

        final UniqueValueIterator<Integer> iterator = new UniqueValueIterator<>(testData.iterator());

        assertEquals(iterator.getIndex(), 0);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 1);
        assertEquals(iterator.getIndex(), 1);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 2);
        assertEquals(iterator.getIndex(), 2);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 3);
        assertEquals(iterator.getIndex(), 3);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 5);
        assertEquals(iterator.getIndex(), 6);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 6);
        assertEquals(iterator.getIndex(), 7);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 7);
        assertEquals(iterator.getIndex(), 8);
        assertFalse(iterator.hasNext());

    }

    @Test
    public void testIterator3(){
        final List<Integer> testData = Arrays.asList(
                1,1,1,1,1,1,1,2,2,2,2,2,3
        );

        final UniqueValueIterator<Integer> iterator = new UniqueValueIterator<>(testData.iterator());

        assertEquals(iterator.getIndex(), 0);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 1);
        assertEquals(iterator.getIndex(), 7);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 2);
        assertEquals(iterator.getIndex(), 12);
        assertTrue(iterator.hasNext());

        assertEquals(iterator.next().intValue(), 3);
        assertEquals(iterator.getIndex(), 13);
        assertFalse(iterator.hasNext());


    }

}
