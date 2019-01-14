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
