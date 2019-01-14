package ca.joeltherrien.randomforest.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSingletonIterator {

    @Test
    public void verifyBehaviour(){
        final Integer element = 5;

        final SingletonIterator<Integer> iterator = new SingletonIterator<>(element);

        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());

        assertEquals(Integer.valueOf(5), iterator.next());

        assertFalse(iterator.hasNext());
        assertNull(iterator.next());

    }

}
