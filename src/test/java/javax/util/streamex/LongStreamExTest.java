/*
 * Copyright 2015 Tagir Valeev
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.util.streamex;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import org.junit.Test;

import static org.junit.Assert.*;

public class LongStreamExTest {
    @Test
    public void testCreate() {
        assertArrayEquals(new long[] {}, LongStreamEx.empty().toArray());
        assertArrayEquals(new long[] { 1 }, LongStreamEx.of(1).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.of(1, 2, 3).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.of(LongStream.of(1, 2, 3)).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.of(Arrays.asList(1L, 2L, 3L)).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.range(1, 4).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.rangeClosed(1, 3).toArray());
        assertEquals(10, LongStreamEx.of(new Random(), 10).count());
        assertTrue(LongStreamEx.of(new Random(), 100, 1, 10).allMatch(x -> x >= 1 && x < 10));
        
        LongStream stream = LongStreamEx.of(1, 2, 3);
        assertSame(stream, LongStreamEx.of(stream));
    }

    @Test
    public void testBasics() {
        assertFalse(LongStreamEx.of(1).isParallel());
        assertTrue(LongStreamEx.of(1).parallel().isParallel());
        assertFalse(LongStreamEx.of(1).parallel().sequential().isParallel());
        AtomicInteger i = new AtomicInteger();
        try(LongStreamEx s = LongStreamEx.of(1).onClose(() -> i.incrementAndGet())) {
            assertEquals(1, s.count());
        }
        assertEquals(1, i.get());
        assertEquals(6, LongStreamEx.range(0, 4).sum());
        assertEquals(3, LongStreamEx.range(0, 4).max().getAsLong());
        assertEquals(0, LongStreamEx.range(0, 4).min().getAsLong());
        assertEquals(1.5, LongStreamEx.range(0, 4).average().getAsDouble(), 0.000001);
        assertEquals(4, LongStreamEx.range(0, 4).summaryStatistics().getCount());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.range(0, 5).skip(1).limit(3).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.of(3,1,2).sorted().toArray());
        assertArrayEquals(new int[] { 2, 4, 6 }, LongStreamEx.range(1, 4).mapToInt(x -> (int)x*2).toArray());
        assertArrayEquals(new long[] { 2, 4, 6 }, LongStreamEx.range(1, 4).map(x -> x*2).toArray());
        assertArrayEquals(new double[] { 2, 4, 6 }, LongStreamEx.range(1, 4).mapToDouble(x -> x*2).toArray(), 0.0);
        assertArrayEquals(new long[] { 1, 3 }, LongStreamEx.range(0, 5).filter(x -> x % 2 == 1).toArray());
    }

    @Test
    public void testPrepend() {
        assertArrayEquals(new long[] { -1, 0, 1, 2, 3 }, LongStreamEx.of(1, 2, 3).prepend(-1, 0).toArray());
    }

    @Test
    public void testAppend() {
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStreamEx.of(1, 2, 3).append(4, 5).toArray());
    }

    @Test
    public void testHas() {
        assertTrue(LongStreamEx.range(1, 4).has(3));
        assertFalse(LongStreamEx.range(1, 4).has(4));
    }

    @Test
    public void testFind() {
        assertEquals(6, LongStreamEx.range(1, 10).findFirst(i -> i > 5).getAsLong());
        assertFalse(LongStreamEx.range(1, 10).findAny(i -> i > 10).isPresent());
    }

    @Test
    public void testRemove() {
        assertArrayEquals(new long[] { 1, 2 }, LongStreamEx.of(1, 2, 3).remove(x -> x > 2).toArray());
    }

    @Test
    public void testSort() {
        assertArrayEquals(new long[] { 0, 3, 6, 1, 4, 7, 2, 5, 8 },
                LongStreamEx.range(0, 9).sortedByLong(i -> i % 3 * 3 + i / 3).toArray());
        assertArrayEquals(new long[] { 10, 11, 5, 6, 7, 8, 9 }, LongStreamEx.range(5, 12).sortedBy(String::valueOf)
                .toArray());
        assertArrayEquals(new long[] { Long.MAX_VALUE, 1000, 1, 0, -10, Long.MIN_VALUE },
                LongStreamEx.of(0, 1, 1000, -10, Long.MIN_VALUE, Long.MAX_VALUE).reverseSorted().toArray());
    }
}
