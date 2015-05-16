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
import java.util.OptionalLong;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import org.junit.Test;

import static org.junit.Assert.*;

public class LongStreamExTest {
    @Test
    public void testCreate() {
        assertArrayEquals(new long[] {}, LongStreamEx.empty().toArray());
        // double test is intended
        assertArrayEquals(new long[] {}, LongStreamEx.empty().toArray());
        assertArrayEquals(new long[] { 1 }, LongStreamEx.of(1).toArray());
        assertArrayEquals(new long[] { 1 }, LongStreamEx.of(OptionalLong.of(1)).toArray());
        assertArrayEquals(new long[] {}, LongStreamEx.of(OptionalLong.empty()).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.of(1, 2, 3).toArray());
        assertArrayEquals(new long[] { 4, 6 }, LongStreamEx.of(new long[] { 2, 4, 6, 8, 10 }, 1, 3).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.of(LongStream.of(1, 2, 3)).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.of(Arrays.asList(1L, 2L, 3L)).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.range(1L, 4L).toArray());
        assertArrayEquals(new long[] { 0, 1, 2 }, LongStreamEx.range(3L).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.rangeClosed(1, 3).toArray());
        assertArrayEquals(new long[] { 1, 2, 4, 8, 16 }, LongStreamEx.iterate(1, x -> x * 2).limit(5).toArray());
        assertArrayEquals(new long[] { 1, 1, 1, 1 }, LongStreamEx.generate(() -> 1).limit(4).toArray());
        assertArrayEquals(new long[] { 1, 1, 1, 1 }, LongStreamEx.constant(1L, 4).toArray());
        assertEquals(10, LongStreamEx.of(new Random(), 10).count());
        assertTrue(LongStreamEx.of(new Random(), 100, 1, 10).allMatch(x -> x >= 1 && x < 10));
        assertArrayEquals(LongStreamEx.of(new Random(1), 100, 1, 10).toArray(), LongStreamEx.of(new Random(1), 1, 10)
                .limit(100).toArray());

        LongStream stream = LongStreamEx.of(1, 2, 3);
        assertSame(stream, LongStreamEx.of(stream));

        assertArrayEquals(new long[] { 4, 2, 0, -2, -4 },
                LongStreamEx.zip(new long[] { 5, 4, 3, 2, 1 }, new long[] { 1, 2, 3, 4, 5 }, (a, b) -> a - b).toArray());
    }

    @Test
    public void testBasics() {
        assertFalse(LongStreamEx.of(1).isParallel());
        assertTrue(LongStreamEx.of(1).parallel().isParallel());
        assertFalse(LongStreamEx.of(1).parallel().sequential().isParallel());
        AtomicInteger i = new AtomicInteger();
        try (LongStreamEx s = LongStreamEx.of(1).onClose(() -> i.incrementAndGet())) {
            assertEquals(1, s.count());
        }
        assertEquals(1, i.get());
        assertEquals(6, LongStreamEx.range(0, 4).sum());
        assertEquals(3, LongStreamEx.range(0, 4).max().getAsLong());
        assertEquals(0, LongStreamEx.range(0, 4).min().getAsLong());
        assertEquals(1.5, LongStreamEx.range(0, 4).average().getAsDouble(), 0.000001);
        assertEquals(4, LongStreamEx.range(0, 4).summaryStatistics().getCount());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.range(0, 5).skip(1).limit(3).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.of(3, 1, 2).sorted().toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.of(1, 2, 1, 3, 2).distinct().toArray());
        assertArrayEquals(new int[] { 2, 4, 6 }, LongStreamEx.range(1, 4).mapToInt(x -> (int) x * 2).toArray());
        assertArrayEquals(new long[] { 2, 4, 6 }, LongStreamEx.range(1, 4).map(x -> x * 2).toArray());
        assertArrayEquals(new double[] { 2, 4, 6 }, LongStreamEx.range(1, 4).mapToDouble(x -> x * 2).toArray(), 0.0);
        assertArrayEquals(new long[] { 1, 3 }, LongStreamEx.range(0, 5).filter(x -> x % 2 == 1).toArray());
        assertEquals(6, LongStreamEx.of(1, 2, 3).reduce(Long::sum).getAsLong());
        assertEquals(Long.MAX_VALUE, LongStreamEx.rangeClosed(1, Long.MAX_VALUE).spliterator().getExactSizeIfKnown());
    }

    @Test
    public void testPrepend() {
        assertArrayEquals(new long[] { -1, 0, 1, 2, 3 }, LongStreamEx.of(1, 2, 3).prepend(-1, 0).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.of(1, 2, 3).prepend().toArray());
        assertArrayEquals(new long[] { 10, 11, 0, 1, 2, 3 }, LongStreamEx.range(0, 4).prepend(LongStreamEx.range(10, 12)).toArray());
    }

    @Test
    public void testAppend() {
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStreamEx.of(1, 2, 3).append(4, 5).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.of(1, 2, 3).append().toArray());
        assertArrayEquals(new long[] { 0, 1, 2, 3, 10, 11 }, LongStreamEx.range(0, 4).append(LongStreamEx.range(10, 12)).toArray());
    }

    @Test
    public void testHas() {
        assertTrue(LongStreamEx.range(1, 4).has(3));
        assertFalse(LongStreamEx.range(1, 4).has(4));
    }

    @Test
    public void testWithout() {
        assertArrayEquals(new long[] { 1, 2 }, LongStreamEx.range(1, 4).without(3).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.range(1, 4).without(5).toArray());
    }

    @Test
    public void testRanges() {
        assertArrayEquals(new long[] { 5, 4, Long.MAX_VALUE }, LongStreamEx.of(1, 5, 3, 4, -1, Long.MAX_VALUE)
                .greater(3).toArray());
        assertArrayEquals(new long[] { }, LongStreamEx.of(1, 5, 3, 4, -1, Long.MAX_VALUE)
                .greater(Long.MAX_VALUE).toArray());
        assertArrayEquals(new long[] { 5, 3, 4, Long.MAX_VALUE }, LongStreamEx.of(1, 5, 3, 4, -1, Long.MAX_VALUE)
                .atLeast(3).toArray());
        assertArrayEquals(new long[] { Long.MAX_VALUE }, LongStreamEx.of(1, 5, 3, 4, -1, Long.MAX_VALUE)
                .atLeast(Long.MAX_VALUE).toArray());
        assertArrayEquals(new long[] { 1, -1 }, LongStreamEx.of(1, 5, 3, 4, -1, Long.MAX_VALUE)
                .less(3).toArray());
        assertArrayEquals(new long[] { 1, 3, -1 }, LongStreamEx.of(1, 5, 3, 4, -1, Long.MAX_VALUE)
                .atMost(3).toArray());
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

    @Test
    public void testMinMax() {
        assertEquals(9, LongStreamEx.range(5, 12).max((a, b) -> String.valueOf(a).compareTo(String.valueOf(b)))
                .getAsLong());
        assertEquals(10, LongStreamEx.range(5, 12).min((a, b) -> String.valueOf(a).compareTo(String.valueOf(b)))
                .getAsLong());
        assertEquals(9, LongStreamEx.range(5, 12).maxBy(String::valueOf).getAsLong());
        assertEquals(10, LongStreamEx.range(5, 12).minBy(String::valueOf).getAsLong());
        assertEquals(5, LongStreamEx.range(5, 12).maxByDouble(x -> 1.0 / x).getAsLong());
        assertEquals(11, LongStreamEx.range(5, 12).minByDouble(x -> 1.0 / x).getAsLong());
        assertEquals(29, LongStreamEx.of(15, 8, 31, 47, 19, 29).maxByInt(x -> (int) (x % 10 * 10 + x / 10)).getAsLong());
        assertEquals(31, LongStreamEx.of(15, 8, 31, 47, 19, 29).minByInt(x -> (int) (x % 10 * 10 + x / 10)).getAsLong());
        assertEquals(29, LongStreamEx.of(15, 8, 31, 47, 19, 29).maxByLong(x -> x % 10 * 10 + x / 10).getAsLong());
        assertEquals(31, LongStreamEx.of(15, 8, 31, 47, 19, 29).minByLong(x -> x % 10 * 10 + x / 10).getAsLong());
    }

    @Test
    public void testPairMap() {
        assertEquals(0, LongStreamEx.range(0).pairMap(Long::sum).count());
        assertEquals(0, LongStreamEx.range(1).pairMap(Long::sum).count());
        assertArrayEquals(new long[] { 6, 7, 8, 9, 10 }, LongStreamEx.of(1, 5, 2, 6, 3, 7).pairMap(Long::sum).toArray());
        assertArrayEquals(LongStreamEx.range(999).map(x -> x * 2 + 1).toArray(), LongStreamEx.range(1000).parallel()
                .map(x -> x * x).pairMap((a, b) -> b - a).toArray());
    }
}
