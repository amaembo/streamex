/*
 * Copyright 2015, 2016 Tagir Valeev
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
package one.util.streamex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Scanner;
import java.util.PrimitiveIterator.OfLong;
import java.util.Random;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import java.util.stream.LongStream.Builder;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static one.util.streamex.TestHelpers.*;
import static org.junit.Assert.*;

/**
 * @author Tagir Valeev
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LongStreamExTest {
    LongConsumer EMPTY = l -> {
        // nothing
    };

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
        assertArrayEquals(new long[] { 1, 1, 1, 1 }, LongStreamEx.generate(() -> 1).limit(4).toArray());
        assertArrayEquals(new long[] { 1, 1, 1, 1 }, LongStreamEx.constant(1L, 4).toArray());
        assertEquals(10, LongStreamEx.of(new Random(), 10).count());
        assertTrue(LongStreamEx.of(new Random(), 100, 1, 10).allMatch(x -> x >= 1 && x < 10));
        assertArrayEquals(LongStreamEx.of(new Random(1), 100, 1, 10).toArray(), LongStreamEx.of(new Random(1), 1, 10)
                .limit(100).toArray());
        assertArrayEquals(LongStreamEx.of(new Random(1), 100).toArray(), LongStreamEx.of(new Random(1)).limit(100)
                .toArray());

        LongStream stream = LongStreamEx.of(1, 2, 3);
        assertSame(stream, LongStreamEx.of(stream));

        assertArrayEquals(new long[] { 4, 2, 0, -2, -4 }, LongStreamEx.zip(new long[] { 5, 4, 3, 2, 1 },
            new long[] { 1, 2, 3, 4, 5 }, (a, b) -> a - b).toArray());

        assertArrayEquals(new long[] { 1, 5, 3 }, LongStreamEx.of(Spliterators.spliterator(new long[] { 1, 5, 3 }, 0))
                .toArray());
        assertArrayEquals(new long[] { 1, 5, 3 }, LongStreamEx.of(
            Spliterators.iterator(Spliterators.spliterator(new long[] { 1, 5, 3 }, 0))).toArray());
        assertArrayEquals(new long[0], LongStreamEx.of(Spliterators.iterator(Spliterators.emptyLongSpliterator()))
                .parallel().toArray());

        assertArrayEquals(new long[] { 2, 4, 6 }, LongStreamEx.of(new Long[] { 2L, 4L, 6L }).toArray());
    }
    
    @Test
    public void testIterate() {
        assertArrayEquals(new long[] { 1, 2, 4, 8, 16 }, LongStreamEx.iterate(1, x -> x * 2).limit(5).toArray());
        assertArrayEquals(new long[] { 1, 2, 4, 8, 16, 32, 64 }, LongStreamEx.iterate(1, x -> x < 100, x -> x * 2).toArray());
        assertEquals(0, LongStreamEx.iterate(0, x -> x < 0, x -> 1 / x).count());
        assertFalse(LongStreamEx.iterate(1, x -> x < 100, x -> x * 2).has(10));
        checkSpliterator("iterate", () -> LongStreamEx.iterate(1, x -> x < 100, x -> x * 2).spliterator());
    }

    @Test
    public void testLongs() {
        assertEquals(Long.MAX_VALUE, LongStreamEx.longs().spliterator().getExactSizeIfKnown());
        assertArrayEquals(new long[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStreamEx.longs().limit(10).toArray());
    }

    @Test
    public void testRangeStep() {
        assertArrayEquals(new long[] { 0 }, LongStreamEx.range(0, 1000, 100000).toArray());
        assertArrayEquals(new long[] { 0, Long.MAX_VALUE - 1 }, LongStreamEx.range(0, Long.MAX_VALUE,
            Long.MAX_VALUE - 1).toArray());
        assertArrayEquals(new long[] { Long.MIN_VALUE, -1, Long.MAX_VALUE - 1 }, LongStreamEx.range(Long.MIN_VALUE,
            Long.MAX_VALUE, Long.MAX_VALUE).toArray());
        assertArrayEquals(new long[] { Long.MIN_VALUE, -1 }, LongStreamEx.range(Long.MIN_VALUE, Long.MAX_VALUE - 1,
            Long.MAX_VALUE).toArray());
        assertArrayEquals(new long[] { Long.MAX_VALUE, -1 }, LongStreamEx.range(Long.MAX_VALUE, Long.MIN_VALUE,
            Long.MIN_VALUE).toArray());
        assertArrayEquals(new long[] { Long.MAX_VALUE }, LongStreamEx.range(Long.MAX_VALUE, 0, Long.MIN_VALUE)
                .toArray());
        assertArrayEquals(new long[] { 1, Long.MIN_VALUE + 1 }, LongStreamEx.range(1, Long.MIN_VALUE, Long.MIN_VALUE)
                .toArray());
        assertArrayEquals(new long[] { 0 }, LongStreamEx.range(0, Long.MIN_VALUE, Long.MIN_VALUE).toArray());
        assertArrayEquals(new long[] { 0, 2, 4, 6, 8 }, LongStreamEx.range(0, 9, 2).toArray());
        assertArrayEquals(new long[] { 0, 2, 4, 6 }, LongStreamEx.range(0, 8, 2).toArray());
        assertArrayEquals(new long[] { 0, -2, -4, -6, -8 }, LongStreamEx.range(0, -9, -2).toArray());
        assertArrayEquals(new long[] { 0, -2, -4, -6 }, LongStreamEx.range(0, -8, -2).toArray());
        assertArrayEquals(new long[] { 5, 4, 3, 2, 1, 0 }, LongStreamEx.range(5, -1, -1).toArray());
        assertEquals(Integer.MAX_VALUE + 1L, LongStreamEx.range(Integer.MIN_VALUE, Integer.MAX_VALUE, 2).spliterator()
                .getExactSizeIfKnown());
        assertEquals(Long.MAX_VALUE, LongStreamEx.range(Long.MIN_VALUE, Long.MAX_VALUE - 1, 2).spliterator()
                .getExactSizeIfKnown());
        java.util.Spliterator.OfLong spliterator = LongStreamEx.range(Long.MAX_VALUE, Long.MIN_VALUE, -2).spliterator();
        assertEquals(-1, spliterator.getExactSizeIfKnown());
        assertTrue(spliterator.tryAdvance(EMPTY));
        assertEquals(Long.MAX_VALUE, spliterator.estimateSize());
        assertTrue(spliterator.tryAdvance(EMPTY));
        assertEquals(Long.MAX_VALUE - 1, spliterator.estimateSize());
        assertEquals(Long.MAX_VALUE, LongStreamEx.range(Long.MAX_VALUE, Long.MIN_VALUE + 1, -2).spliterator()
                .getExactSizeIfKnown());
        assertEquals(-1, LongStreamEx.range(Long.MIN_VALUE, Long.MAX_VALUE, 1).spliterator().getExactSizeIfKnown());
        assertEquals(-1, LongStreamEx.range(Long.MAX_VALUE, Long.MIN_VALUE, -1).spliterator().getExactSizeIfKnown());
        assertEquals(0, LongStreamEx.range(0, -1000, 1).count());
        assertEquals(0, LongStreamEx.range(0, 1000, -1).count());
        assertEquals(0, LongStreamEx.range(0, 0, -1).count());
        assertEquals(0, LongStreamEx.range(0, 0, 1).count());
        assertEquals(0, LongStreamEx.range(0, -1000, 2).count());
        assertEquals(0, LongStreamEx.range(0, 1000, -2).count());
        assertEquals(0, LongStreamEx.range(0, 0, -2).count());
        assertEquals(0, LongStreamEx.range(0, 0, 2).count());

        assertEquals(0, LongStreamEx.range(0, Long.MIN_VALUE, 2).spliterator().getExactSizeIfKnown());
        assertEquals(0, LongStreamEx.range(0, Long.MAX_VALUE, -2).spliterator().getExactSizeIfKnown());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeIllegalStep() {
        LongStreamEx.range(0, 1000, 0);
    }

    @Test
    public void testRangeClosedStep() {
        assertArrayEquals(new long[] { 0 }, LongStreamEx.rangeClosed(0, 1000, 100000).toArray());
        assertArrayEquals(new long[] { 0, 1000 }, LongStreamEx.rangeClosed(0, 1000, 1000).toArray());
        assertArrayEquals(new long[] { 0, Long.MAX_VALUE - 1 }, LongStreamEx.rangeClosed(0, Long.MAX_VALUE - 1,
            Long.MAX_VALUE - 1).toArray());
        assertArrayEquals(new long[] { Long.MIN_VALUE, -1, Long.MAX_VALUE - 1 }, LongStreamEx.rangeClosed(
            Long.MIN_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE).toArray());
        assertArrayEquals(new long[] { Long.MIN_VALUE, -1 }, LongStreamEx.rangeClosed(Long.MIN_VALUE,
            Long.MAX_VALUE - 2, Long.MAX_VALUE).toArray());
        assertArrayEquals(new long[] { Long.MAX_VALUE, -1 }, LongStreamEx.rangeClosed(Long.MAX_VALUE, Long.MIN_VALUE,
            Long.MIN_VALUE).toArray());
        assertArrayEquals(new long[] { Long.MAX_VALUE }, LongStreamEx.rangeClosed(Long.MAX_VALUE, 0, Long.MIN_VALUE)
                .toArray());
        assertArrayEquals(new long[] { 0, Long.MIN_VALUE }, LongStreamEx.rangeClosed(0, Long.MIN_VALUE, Long.MIN_VALUE)
                .toArray());
        assertArrayEquals(new long[] { 0, 2, 4, 6, 8 }, LongStreamEx.rangeClosed(0, 9, 2).toArray());
        assertArrayEquals(new long[] { 0, 2, 4, 6, 8 }, LongStreamEx.rangeClosed(0, 8, 2).toArray());
        assertArrayEquals(new long[] { 0, 2, 4, 6 }, LongStreamEx.rangeClosed(0, 7, 2).toArray());
        assertArrayEquals(new long[] { 0, -2, -4, -6, -8 }, LongStreamEx.rangeClosed(0, -9, -2).toArray());
        assertArrayEquals(new long[] { 0, -2, -4, -6, -8 }, LongStreamEx.rangeClosed(0, -8, -2).toArray());
        assertArrayEquals(new long[] { 0, -2, -4, -6 }, LongStreamEx.rangeClosed(0, -7, -2).toArray());
        assertArrayEquals(new long[] { 5, 4, 3, 2, 1, 0 }, LongStreamEx.rangeClosed(5, 0, -1).toArray());
        assertEquals(Integer.MAX_VALUE + 1L, LongStreamEx.rangeClosed(Integer.MIN_VALUE, Integer.MAX_VALUE, 2)
                .spliterator().getExactSizeIfKnown());
        assertEquals(Long.MAX_VALUE, LongStreamEx.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE - 2, 2).spliterator()
                .getExactSizeIfKnown());
        java.util.Spliterator.OfLong spliterator = LongStreamEx.rangeClosed(Long.MAX_VALUE, Long.MIN_VALUE, -2)
                .spliterator();
        assertEquals(-1, spliterator.getExactSizeIfKnown());
        assertTrue(spliterator.tryAdvance(EMPTY));
        assertEquals(Long.MAX_VALUE, spliterator.estimateSize());
        assertTrue(spliterator.tryAdvance(EMPTY));
        assertEquals(Long.MAX_VALUE - 1, spliterator.estimateSize());
        assertEquals(Long.MAX_VALUE, LongStreamEx.rangeClosed(Long.MAX_VALUE, Long.MIN_VALUE + 2, -2).spliterator()
                .getExactSizeIfKnown());
        assertEquals(-1, LongStreamEx.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, 1).spliterator()
                .getExactSizeIfKnown());
        assertEquals(-1, LongStreamEx.rangeClosed(Long.MAX_VALUE, Long.MIN_VALUE, -1).spliterator()
                .getExactSizeIfKnown());
        assertEquals(0, LongStreamEx.rangeClosed(0, -1000, 1).count());
        assertEquals(0, LongStreamEx.rangeClosed(0, 1000, -1).count());
        assertEquals(0, LongStreamEx.rangeClosed(0, 1, -1).count());
        assertEquals(0, LongStreamEx.rangeClosed(0, -1, 1).count());
        assertEquals(0, LongStreamEx.rangeClosed(0, -1000, 2).count());
        assertEquals(0, LongStreamEx.rangeClosed(0, 1000, -2).count());
        assertEquals(0, LongStreamEx.rangeClosed(0, 1, -2).count());
        assertEquals(0, LongStreamEx.rangeClosed(0, -1, 2).count());

        assertEquals(0, LongStreamEx.rangeClosed(0, Long.MIN_VALUE, 2).spliterator().getExactSizeIfKnown());
        assertEquals(0, LongStreamEx.rangeClosed(0, Long.MAX_VALUE, -2).spliterator().getExactSizeIfKnown());
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

        assertTrue(LongStreamEx.of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.ORDERED));
        assertFalse(LongStreamEx.of(1, 2, 3).unordered().spliterator().hasCharacteristics(Spliterator.ORDERED));

        OfLong iterator = LongStreamEx.of(1, 2, 3).iterator();
        assertEquals(1L, iterator.nextLong());
        assertEquals(2L, iterator.nextLong());
        assertEquals(3L, iterator.nextLong());
        assertFalse(iterator.hasNext());

        AtomicInteger idx = new AtomicInteger();
        long[] result = new long[500];
        LongStreamEx.range(1000).atLeast(500).parallel().forEachOrdered(val -> result[idx.getAndIncrement()] = val);
        assertArrayEquals(LongStreamEx.range(500, 1000).toArray(), result);

        assertTrue(LongStreamEx.empty().noneMatch(x -> true));
        assertFalse(LongStreamEx.of(1).noneMatch(x -> true));
        assertTrue(LongStreamEx.of(1).noneMatch(x -> false));
    }
    
    @Test
    public void testForEach() {
        List<Long> list = new ArrayList<>();
        LongStreamEx.of(1).forEach(list::add);
        assertEquals(Arrays.asList(1L), list);
    }

    @Test
    public void testFlatMap() {
        assertArrayEquals(new long[] { 0, 0, 1, 0, 1, 2 }, LongStreamEx.of(1, 2, 3).flatMap(LongStreamEx::range)
                .toArray());
        assertArrayEquals(new int[] { 1, 5, 1, 4, 2, 0, 9, 2, 2, 3, 3, 7, 2, 0, 3, 6, 8, 5, 4, 7, 7, 5, 8, 0, 7 },
            LongStreamEx.of(15, 14, 20, Long.MAX_VALUE).flatMapToInt(n -> String.valueOf(n).chars().map(x -> x - '0'))
                    .toArray());

        String expected = LongStreamEx.range(200).boxed().flatMap(
            i -> LongStreamEx.range(0, i).<String> mapToObj(j -> i + ":" + j)).joining("/");
        String res = LongStreamEx.range(200).flatMapToObj(i -> LongStreamEx.range(i).mapToObj(j -> i + ":" + j))
                .joining("/");
        String parallel = LongStreamEx.range(200).parallel().flatMapToObj(
            i -> LongStreamEx.range(i).mapToObj(j -> i + ":" + j)).joining("/");
        assertEquals(expected, res);
        assertEquals(expected, parallel);

        double[] fractions = LongStreamEx.range(1, 5).flatMapToDouble(
            i -> LongStreamEx.range(1, i).mapToDouble(j -> ((double) j) / i)).toArray();
        assertArrayEquals(new double[] { 1 / 2.0, 1 / 3.0, 2 / 3.0, 1 / 4.0, 2 / 4.0, 3 / 4.0 }, fractions, 0.000001);
    }

    @Test
    public void testPrepend() {
        assertArrayEquals(new long[] { -1, 0, 1, 2, 3 }, LongStreamEx.of(1, 2, 3).prepend(-1, 0).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.of(1, 2, 3).prepend().toArray());
        assertArrayEquals(new long[] { 10, 11, 0, 1, 2, 3 }, LongStreamEx.range(0, 4).prepend(
            LongStreamEx.range(10, 12)).toArray());
    }

    @Test
    public void testAppend() {
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStreamEx.of(1, 2, 3).append(4, 5).toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.of(1, 2, 3).append().toArray());
        assertArrayEquals(new long[] { 0, 1, 2, 3, 10, 11 }, LongStreamEx.range(0, 4)
                .append(LongStreamEx.range(10, 12)).toArray());
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
        LongStreamEx lse = LongStreamEx.range(5);
        assertSame(lse, lse.without());
        assertArrayEquals(new long[] { 0, 1, 3, 4 }, LongStreamEx.range(5).without(new long[] { 2 }).toArray());
        assertArrayEquals(new long[] { 0 }, LongStreamEx.range(5).without(1, 2, 3, 4, 5, 6).toArray());
    }

    @Test
    public void testRanges() {
        assertArrayEquals(new long[] { 5, 4, Long.MAX_VALUE }, LongStreamEx.of(1, 5, 3, 4, -1, Long.MAX_VALUE).greater(
            3).toArray());
        assertArrayEquals(new long[] {}, LongStreamEx.of(1, 5, 3, 4, -1, Long.MAX_VALUE).greater(Long.MAX_VALUE)
                .toArray());
        assertArrayEquals(new long[] { 5, 3, 4, Long.MAX_VALUE }, LongStreamEx.of(1, 5, 3, 4, -1, Long.MAX_VALUE)
                .atLeast(3).toArray());
        assertArrayEquals(new long[] { Long.MAX_VALUE }, LongStreamEx.of(1, 5, 3, 4, -1, Long.MAX_VALUE).atLeast(
            Long.MAX_VALUE).toArray());
        assertArrayEquals(new long[] { 1, -1 }, LongStreamEx.of(1, 5, 3, 4, -1, Long.MAX_VALUE).less(3).toArray());
        assertArrayEquals(new long[] { 1, 3, -1 }, LongStreamEx.of(1, 5, 3, 4, -1, Long.MAX_VALUE).atMost(3).toArray());
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
        assertArrayEquals(new long[] { 0, 3, 6, 1, 4, 7, 2, 5, 8 }, LongStreamEx.range(0, 9).sortedByLong(
            i -> i % 3 * 3 + i / 3).toArray());
        assertArrayEquals(new long[] { 0, 4, 8, 1, 5, 9, 2, 6, 3, 7 }, LongStreamEx.range(0, 10).sortedByInt(
            i -> (int) i % 4).toArray());
        assertArrayEquals(new long[] { 10, 11, 5, 6, 7, 8, 9 }, LongStreamEx.range(5, 12).sortedBy(String::valueOf)
                .toArray());
        assertArrayEquals(new long[] { Long.MAX_VALUE, 1000, 1, 0, -10, Long.MIN_VALUE }, LongStreamEx.of(0, 1, 1000,
            -10, Long.MIN_VALUE, Long.MAX_VALUE).reverseSorted().toArray());
        assertArrayEquals(new long[] { Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1 },
            LongStreamEx.of(Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1, Long.MAX_VALUE).sortedByLong(
                l -> l + 1).toArray());
        assertArrayEquals(new long[] { -10, Long.MIN_VALUE, Long.MAX_VALUE, 1000, 1, 0 }, LongStreamEx.of(0, 1, 1000,
            -10, Long.MIN_VALUE, Long.MAX_VALUE).sortedByDouble(x -> 1.0 / x).toArray());
    }

    @SafeVarargs
    private final void checkEmpty(Function<LongStreamEx, OptionalLong>... fns) {
        int i = 0;
        for (Function<LongStreamEx, OptionalLong> fn : fns) {
            assertFalse("#" + i, fn.apply(LongStreamEx.empty()).isPresent());
            assertFalse("#" + i, fn.apply(LongStreamEx.of(1, 2, 3, 4).greater(5).parallel()).isPresent());
            assertEquals("#" + i, 10, fn.apply(LongStreamEx.of(1, 1, 1, 1, 10, 10, 10, 10).greater(5).parallel())
                    .getAsLong());
            i++;
        }
    }

    @Test
    public void testMinMax() {
        checkEmpty(s -> s.maxBy(Long::valueOf), s -> s.maxByInt(x -> (int) x), s -> s.maxByLong(x -> x), s -> s
                .maxByDouble(x -> x), s -> s.minBy(Long::valueOf), s -> s.minByInt(x -> (int) x), s -> s
                .minByLong(x -> x), s -> s.minByDouble(x -> x));
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

        Supplier<LongStreamEx> s = () -> LongStreamEx.of(1, 50, 120, 35, 130, 12, 0);
        LongToIntFunction intKey = x -> String.valueOf(x).length();
        LongUnaryOperator longKey = x -> String.valueOf(x).length();
        LongToDoubleFunction doubleKey = x -> String.valueOf(x).length();
        LongFunction<Integer> objKey = x -> String.valueOf(x).length();
        List<Function<LongStreamEx, OptionalLong>> minFns = Arrays.asList(is -> is.minByInt(intKey), is -> is
                .minByLong(longKey), is -> is.minByDouble(doubleKey), is -> is.minBy(objKey));
        List<Function<LongStreamEx, OptionalLong>> maxFns = Arrays.asList(is -> is.maxByInt(intKey), is -> is
                .maxByLong(longKey), is -> is.maxByDouble(doubleKey), is -> is.maxBy(objKey));
        minFns.forEach(fn -> assertEquals(1, fn.apply(s.get()).getAsLong()));
        minFns.forEach(fn -> assertEquals(1, fn.apply(s.get().parallel()).getAsLong()));
        maxFns.forEach(fn -> assertEquals(120, fn.apply(s.get()).getAsLong()));
        maxFns.forEach(fn -> assertEquals(120, fn.apply(s.get().parallel()).getAsLong()));
    }

    @Test
    public void testPairMap() {
        assertEquals(0, LongStreamEx.range(0).pairMap(Long::sum).count());
        assertEquals(0, LongStreamEx.range(1).pairMap(Long::sum).count());
        assertArrayEquals(new long[] { 6, 7, 8, 9, 10 }, LongStreamEx.of(1, 5, 2, 6, 3, 7).pairMap(Long::sum).toArray());
        assertArrayEquals(LongStreamEx.range(999).map(x -> x * 2 + 1).toArray(), LongStreamEx.range(1000).parallel()
                .map(x -> x * x).pairMap((a, b) -> b - a).toArray());

        assertArrayEquals(LongStreamEx.range(1, 100).toArray(), LongStreamEx.range(100).map(i -> i * (i + 1) / 2)
                .append(LongStream.empty()).parallel().pairMap((a, b) -> b - a).toArray());
        assertArrayEquals(LongStreamEx.range(1, 100).toArray(), LongStreamEx.range(100).map(i -> i * (i + 1) / 2)
                .prepend(LongStream.empty()).parallel().pairMap((a, b) -> b - a).toArray());

        assertEquals(1, LongStreamEx.range(1000).map(x -> x * x).pairMap((a, b) -> b - a).pairMap((a, b) -> b - a)
                .distinct().count());

        assertFalse(LongStreamEx.range(1000).greater(2000).parallel().pairMap((a, b) -> a).findFirst().isPresent());
    }

    @Test
    public void testJoining() {
        assertEquals("0,1,2,3,4,5,6,7,8,9", LongStreamEx.range(10).joining(","));
        assertEquals("0,1,2,3,4,5,6,7,8,9", LongStreamEx.range(10).parallel().joining(","));
        assertEquals("[0,1,2,3,4,5,6,7,8,9]", LongStreamEx.range(10).joining(",", "[", "]"));
        assertEquals("[0,1,2,3,4,5,6,7,8,9]", LongStreamEx.range(10).parallel().joining(",", "[", "]"));
    }

    @Test
    public void testMapToEntry() {
        Map<Long, List<Long>> result = LongStreamEx.range(10).mapToEntry(x -> x % 2, x -> x).grouping();
        assertEquals(Arrays.asList(0l, 2l, 4l, 6l, 8l), result.get(0l));
        assertEquals(Arrays.asList(1l, 3l, 5l, 7l, 9l), result.get(1l));
    }

    @Test
    public void testRecreate() {
        assertEquals(500, (long) LongStreamEx.iterate(0, i -> i + 1).skipOrdered(1).greater(0).boxed().parallel()
                .findAny(i -> i == 500).get());
        assertEquals(500, (long) LongStreamEx.iterate(0, i -> i + 1).parallel().skipOrdered(1).greater(0).boxed()
                .findAny(i -> i == 500).get());
    }

    @Test
    public void testTakeWhile() {
        assertArrayEquals(LongStreamEx.range(100).toArray(), LongStreamEx.iterate(0, i -> i + 1)
                .takeWhile(i -> i < 100).toArray());
        assertEquals(0, LongStreamEx.iterate(0, i -> i + 1).takeWhile(i -> i < 0).count());
        assertEquals(1, LongStreamEx.of(1, 3, 2).takeWhile(i -> i < 3).count());
        assertEquals(3, LongStreamEx.of(1, 2, 3).takeWhile(i -> i < 100).count());
    }

    @Test
    public void testTakeWhileInclusive() {
        assertArrayEquals(LongStreamEx.range(101).toArray(), LongStreamEx.iterate(0, i -> i + 1)
            .takeWhileInclusive(i -> i < 100).toArray());
        assertEquals(1, LongStreamEx.iterate(0, i -> i + 1).takeWhileInclusive(i -> i < 0).count());
        assertEquals(2, LongStreamEx.of(1, 3, 2).takeWhileInclusive(i -> i < 3).count());
        assertEquals(3, LongStreamEx.of(1, 2, 3).takeWhileInclusive(i -> i < 100).count());
    }
    
    @Test
    public void testDropWhile() {
        assertArrayEquals(new long[] { 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }, LongStreamEx.range(100).dropWhile(
            i -> i % 10 < 5).limit(10).toArray());
        assertEquals(100, LongStreamEx.range(100).dropWhile(i -> i % 10 < 0).count());
        assertEquals(0, LongStreamEx.range(100).dropWhile(i -> i % 10 < 10).count());
        assertEquals(OptionalLong.of(0), LongStreamEx.range(100).dropWhile(i -> i % 10 < 0).findFirst());
        assertEquals(OptionalLong.empty(), LongStreamEx.range(100).dropWhile(i -> i % 10 < 10).findFirst());

        java.util.Spliterator.OfLong spltr = LongStreamEx.range(100).dropWhile(i -> i % 10 < 1).spliterator();
        assertTrue(spltr.tryAdvance((long x) -> assertEquals(1, x)));
        Builder builder = LongStream.builder();
        spltr.forEachRemaining(builder);
        assertArrayEquals(LongStreamEx.range(2, 100).toArray(), builder.build().toArray());
    }

    @Test
    public void testIndexOf() {
        assertEquals(5, LongStreamEx.range(50, 100).indexOf(55).getAsLong());
        assertFalse(LongStreamEx.range(50, 100).indexOf(200).isPresent());
        assertEquals(5, LongStreamEx.range(50, 100).parallel().indexOf(55).getAsLong());
        assertFalse(LongStreamEx.range(50, 100).parallel().indexOf(200).isPresent());

        assertEquals(11, LongStreamEx.range(50, 100).indexOf(x -> x > 60).getAsLong());
        assertFalse(LongStreamEx.range(50, 100).indexOf(x -> x < 0).isPresent());
        assertEquals(11, LongStreamEx.range(50, 100).parallel().indexOf(x -> x > 60).getAsLong());
        assertFalse(LongStreamEx.range(50, 100).parallel().indexOf(x -> x < 0).isPresent());
    }

    @Test
    public void testFoldLeft() {
        // non-associative
        LongBinaryOperator accumulator = (x, y) -> (x + y) * (x + y);
        assertEquals(2322576, LongStreamEx.constant(3, 4).foldLeft(accumulator).orElse(-1));
        assertEquals(2322576, LongStreamEx.constant(3, 4).parallel().foldLeft(accumulator).orElse(-1));
        assertFalse(LongStreamEx.empty().foldLeft(accumulator).isPresent());
        assertEquals(144, LongStreamEx.rangeClosed(1, 3).foldLeft(0L, accumulator));
        assertEquals(144, LongStreamEx.rangeClosed(1, 3).parallel().foldLeft(0L, accumulator));
    }

    @Test
    public void testMapFirstLast() {
        assertArrayEquals(new long[] { -1, 2, 3, 4, 7 }, LongStreamEx.of(1, 2, 3, 4, 5).mapFirst(x -> x - 2L).mapLast(
            x -> x + 2L).toArray());
    }

    @Test
    public void testPeekFirst() {
        long[] input = {1, 10, 100, 1000};
        
        AtomicLong firstElement = new AtomicLong();
        assertArrayEquals(new long[] {10, 100, 1000}, LongStreamEx.of(input).peekFirst(firstElement::set).skip(1).toArray());
        assertEquals(1, firstElement.get());

        assertArrayEquals(new long[] {10, 100, 1000}, LongStreamEx.of(input).skip(1).peekFirst(firstElement::set).toArray());
        assertEquals(10, firstElement.get());
        
        firstElement.set(-1);
        assertArrayEquals(new long[] {}, LongStreamEx.of(input).skip(4).peekFirst(firstElement::set).toArray());
        assertEquals(-1, firstElement.get());
    }
    
    @Test
    public void testPeekLast() {
        long[] input = {1, 10, 100, 1000};
        AtomicLong lastElement = new AtomicLong(-1);
        assertArrayEquals(new long[] {1, 10, 100}, LongStreamEx.of(input).peekLast(lastElement::set).limit(3).toArray());
        assertEquals(-1, lastElement.get());

        assertArrayEquals(new long[] { 1, 10, 100 }, LongStreamEx.of(input).less(1000).peekLast(lastElement::set)
                .limit(3).toArray());
        assertEquals(100, lastElement.get());
        
        assertArrayEquals(input, LongStreamEx.of(input).peekLast(lastElement::set).limit(4).toArray());
        assertEquals(1000, lastElement.get());
        
        assertArrayEquals(new long[] {1, 10, 100}, LongStreamEx.of(input).limit(3).peekLast(lastElement::set).toArray());
        assertEquals(100, lastElement.get());
    }
    
    @Test
    public void testScanLeft() {
        assertArrayEquals(new long[] { 0, 1, 3, 6, 10, 15, 21, 28, 36, 45 }, LongStreamEx.range(10).scanLeft(Long::sum));
        assertArrayEquals(new long[] { 0, 1, 3, 6, 10, 15, 21, 28, 36, 45 }, LongStreamEx.range(10).parallel()
                .scanLeft(Long::sum));
        assertArrayEquals(new long[] { 0, 1, 3, 6, 10, 15, 21, 28, 36, 45 }, LongStreamEx.range(10).filter(x -> true)
                .scanLeft(Long::sum));
        assertArrayEquals(new long[] { 0, 1, 3, 6, 10, 15, 21, 28, 36, 45 }, LongStreamEx.range(10).filter(x -> true)
                .parallel().scanLeft(Long::sum));
        assertArrayEquals(new long[] { 1, 1, 2, 6, 24, 120 }, LongStreamEx.rangeClosed(1, 5).scanLeft(1,
            (a, b) -> a * b));
        assertArrayEquals(new long[] { 1, 1, 2, 6, 24, 120 }, LongStreamEx.rangeClosed(1, 5).parallel().scanLeft(1,
            (a, b) -> a * b));
    }

    // Reads numbers from scanner stopping when non-number is encountered
    // leaving scanner in known state
    public static LongStreamEx scannerLongs(Scanner sc) {
        return LongStreamEx.produce(action -> {
            if(sc.hasNextLong())
                action.accept(sc.nextLong());
            return sc.hasNextLong();
        });
    }

    @Test
    public void testProduce() {
        Scanner sc = new Scanner("1 2 3 4 20000000000 test");
        assertArrayEquals(new long[] {1, 2, 3, 4, 20000000000L}, scannerLongs(sc).stream().toArray());
        assertEquals("test", sc.next());
    }
}
