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

import static org.junit.Assert.*;

import java.util.BitSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import one.util.streamex.DoubleCollector;
import one.util.streamex.IntCollector;
import one.util.streamex.IntStreamEx;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author Tagir Valeev
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IntCollectorTest {
    @Test
    public void testJoining() {
        String expected = IntStream.range(0, 10000).mapToObj(String::valueOf).collect(Collectors.joining(", "));
        assertEquals(expected, IntStreamEx.range(10000).collect(IntCollector.joining(", ")));
        assertEquals(expected, IntStreamEx.range(10000).parallel().collect(IntCollector.joining(", ")));
        String expected2 = IntStreamEx.range(0, 1000).boxed().toList().toString();
        assertEquals(expected2, IntStreamEx.range(1000).collect(IntCollector.joining(", ", "[", "]")));
        assertEquals(expected2, IntStreamEx.range(1000).parallel().collect(IntCollector.joining(", ", "[", "]")));
    }

    @Test
    public void testCounting() {
        assertEquals(5000L, (long) IntStreamEx.range(10000).atLeast(5000).collect(IntCollector.counting()));
        assertEquals(5000L, (long) IntStreamEx.range(10000).parallel().atLeast(5000).collect(IntCollector.counting()));
        assertEquals(5000, (int) IntStreamEx.range(10000).atLeast(5000).collect(IntCollector.countingInt()));
        assertEquals(5000, (int) IntStreamEx.range(10000).parallel().atLeast(5000).collect(IntCollector.countingInt()));
    }

    @Test
    public void testReducing() {
        assertEquals(120, (int) IntStreamEx.rangeClosed(1, 5).collect(IntCollector.reducing(1, (a, b) -> a * b)));
        assertEquals(120, (int) IntStreamEx.rangeClosed(1, 5).parallel().collect(
            IntCollector.reducing(1, (a, b) -> a * b)));
    }

    @Test
    public void testCollectingAndThen() {
        assertEquals(9, (int) IntStreamEx.rangeClosed(1, 5).collect(IntCollector.joining(",").andThen(String::length)));
    }

    @Test
    public void testSumming() {
        assertEquals(3725, (int) IntStreamEx.range(100).atLeast(50).collect(IntCollector.summing()));
        assertEquals(3725, (int) IntStreamEx.range(100).parallel().atLeast(50).collect(IntCollector.summing()));

        int[] input = IntStreamEx.of(new Random(1), 10000, 1, 1000).toArray();
        Map<Boolean, Integer> expected = IntStream.of(input).boxed().collect(
            Collectors.partitioningBy(i -> i % 2 == 0, Collectors.summingInt(Integer::intValue)));
        Map<Boolean, Integer> sumEvenOdd = IntStreamEx.of(input).collect(
            IntCollector.partitioningBy(i -> i % 2 == 0, IntCollector.summing()));
        assertEquals(expected, sumEvenOdd);
        sumEvenOdd = IntStreamEx.of(input).parallel().collect(
            IntCollector.partitioningBy(i -> i % 2 == 0, IntCollector.summing()));
        assertEquals(expected, sumEvenOdd);
    }

    @Test
    public void testMin() {
        assertEquals(50, IntStreamEx.range(100).atLeast(50).collect(IntCollector.min()).getAsInt());
        assertFalse(IntStreamEx.range(100).atLeast(200).collect(IntCollector.min()).isPresent());
    }

    @Test
    public void testMax() {
        assertEquals(99, IntStreamEx.range(100).atLeast(50).collect(IntCollector.max()).getAsInt());
        assertEquals(99, IntStreamEx.range(100).parallel().atLeast(50).collect(IntCollector.max()).getAsInt());
        assertFalse(IntStreamEx.range(100).atLeast(200).collect(IntCollector.max()).isPresent());
    }

    @Test
    public void testSummarizing() {
        int[] data = IntStreamEx.of(new Random(1), 1000, 1, Integer.MAX_VALUE).toArray();
        IntSummaryStatistics expected = IntStream.of(data).summaryStatistics();
        IntSummaryStatistics statistics = IntStreamEx.of(data).collect(IntCollector.summarizing());
        assertEquals(expected.getCount(), statistics.getCount());
        assertEquals(expected.getSum(), statistics.getSum());
        assertEquals(expected.getMax(), statistics.getMax());
        assertEquals(expected.getMin(), statistics.getMin());
        statistics = IntStreamEx.of(data).parallel().collect(IntCollector.summarizing());
        assertEquals(expected.getCount(), statistics.getCount());
        assertEquals(expected.getSum(), statistics.getSum());
        assertEquals(expected.getMax(), statistics.getMax());
        assertEquals(expected.getMin(), statistics.getMin());
    }

    @Test
    public void testToArray() {
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStreamEx.of(0, 1, 2, 3, 4).collect(IntCollector.toArray()));
        assertArrayEquals(IntStreamEx.range(1000).toByteArray(), IntStreamEx.range(1000).collect(
            IntCollector.toByteArray()));
        assertArrayEquals(IntStreamEx.range(1000).toCharArray(), IntStreamEx.range(1000).collect(
            IntCollector.toCharArray()));
        assertArrayEquals(IntStreamEx.range(1000).toShortArray(), IntStreamEx.range(1000).collect(
            IntCollector.toShortArray()));
    }

    @Test
    public void testPartitioning() {
        int[] expectedEven = IntStream.range(0, 1000).map(i -> i * 2).toArray();
        int[] expectedOdd = IntStream.range(0, 1000).map(i -> i * 2 + 1).toArray();
        Map<Boolean, int[]> oddEven = IntStreamEx.range(2000).collect(IntCollector.partitioningBy(i -> i % 2 == 0));
        assertTrue(oddEven.containsKey(true));
        assertTrue(oddEven.containsKey(false));
        assertFalse(oddEven.containsKey(null));
        assertFalse(oddEven.containsKey(0));
        assertArrayEquals(expectedEven, oddEven.get(true));
        assertArrayEquals(expectedOdd, oddEven.get(false));
        assertNull(oddEven.get(null));
        assertNull(oddEven.get(0));
        assertEquals(2, oddEven.entrySet().size());
        oddEven = IntStreamEx.range(2000).parallel().collect(IntCollector.partitioningBy(i -> i % 2 == 0));
        assertArrayEquals(expectedEven, oddEven.get(true));
        assertArrayEquals(expectedOdd, oddEven.get(false));

        IntCollector<?, Map<Boolean, int[]>> partitionMapToArray = IntCollector.partitioningBy(i -> i % 2 == 0,
            IntCollector.mapping(i -> i / 2, IntCollector.toArray()));
        oddEven = IntStreamEx.range(2000).collect(partitionMapToArray);
        int[] ints = IntStreamEx.range(1000).toArray();
        assertArrayEquals(ints, oddEven.get(true));
        assertArrayEquals(ints, oddEven.get(false));

        Map<Boolean, IntSummaryStatistics> sums = IntStreamEx.rangeClosed(0, 100).collect(
            IntCollector.partitioningBy(i -> i % 2 == 0, IntCollector.summarizing()));
        assertEquals(2500, sums.get(false).getSum());
        assertEquals(2550, sums.get(true).getSum());
    }

    @Test
    public void testSumBySign() {
        int[] input = new Random(1).ints(2000, -1000, 1000).toArray();
        Map<Boolean, Integer> sums = IntStreamEx.of(input).collect(
            IntCollector.partitioningBy(i -> i > 0, IntCollector.summing()));
        Map<Boolean, Integer> sumsBoxed = IntStream.of(input).boxed().collect(
            Collectors.partitioningBy(i -> i > 0, Collectors.summingInt(Integer::intValue)));
        assertEquals(sumsBoxed, sums);
    }

    @Test
    public void testGroupingBy() {
        Map<Integer, int[]> collected = IntStreamEx.range(2000).collect(IntCollector.groupingBy(i -> i % 3));
        for (int i = 0; i < 3; i++) {
            int rem = i;
            assertArrayEquals(IntStream.range(0, 2000).filter(a -> a % 3 == rem).toArray(), collected.get(i));
        }
        collected = IntStreamEx.range(2000).parallel().collect(IntCollector.groupingBy(i -> i % 3));
        for (int i = 0; i < 3; i++) {
            int rem = i;
            assertArrayEquals(IntStream.range(0, 2000).filter(a -> a % 3 == rem).toArray(), collected.get(i));
        }

        Map<Integer, BitSet> mapBitSet = IntStreamEx.range(10).collect(
            IntCollector.groupingBy(i -> i % 3, IntCollector.toBitSet()));
        assertEquals("{0, 3, 6, 9}", mapBitSet.get(0).toString());
        assertEquals("{1, 4, 7}", mapBitSet.get(1).toString());
        assertEquals("{2, 5, 8}", mapBitSet.get(2).toString());
    }

    @Test
    public void testByDigit() {
        int[] input = new Random(1).ints(2000, -1000, 1000).toArray();
        IntCollector<?, Map<Integer, List<Integer>>> collector = IntCollector.groupingBy(i -> i % 10, IntCollector
                .of(Collectors.toList()));
        Map<Integer, List<Integer>> groups = IntStreamEx.of(input).collect(collector);
        Map<Integer, List<Integer>> groupsBoxed = IntStream.of(input).boxed().collect(
            Collectors.groupingBy(i -> i % 10));
        assertEquals(groupsBoxed, groups);
    }

    @Test
    public void testAsCollector() {
        assertEquals(499500, (int) IntStream.range(0, 1000).boxed().collect(IntCollector.summing()));
        assertEquals(499500, (int) IntStream.range(0, 1000).boxed().parallel().collect(IntCollector.summing()));
        assertEquals(1000, (long) IntStream.range(0, 1000).boxed().collect(IntCollector.counting()));
    }

    @Test
    public void testAdaptor() {
        assertEquals(499500, (int) IntStreamEx.range(0, 1000).collect(IntCollector.of(IntCollector.summing())));
        assertEquals(499500, (int) IntStreamEx.range(0, 1000).collect(
            IntCollector.of(Collectors.summingInt(Integer::intValue))));
    }

    @Test
    public void testMapping() {
        assertArrayEquals(IntStreamEx.range(1000).asDoubleStream().toArray(), IntStreamEx.range(1000).collect(
            IntCollector.mappingToObj(i -> (double) i, DoubleCollector.toArray())), 0.0);
    }

    @Test
    public void testAveraging() {
        assertFalse(IntStreamEx.empty().collect(IntCollector.averaging()).isPresent());
        assertEquals(Integer.MAX_VALUE, IntStreamEx.of(Integer.MAX_VALUE, Integer.MAX_VALUE).collect(
            IntCollector.averaging()).getAsDouble(), 1);
        assertEquals(Integer.MAX_VALUE, IntStreamEx.of(Integer.MAX_VALUE, Integer.MAX_VALUE).parallel().collect(
            IntCollector.averaging()).getAsDouble(), 1);
    }

    @Test
    public void testToBooleanArray() {
        assertArrayEquals(new boolean[0], IntStreamEx.empty().collect(IntCollector.toBooleanArray(x -> true)));
        boolean[] expected = new boolean[] { true, false, false, true };
        assertArrayEquals(expected, IntStreamEx.of(-1, 2, 3, -4).collect(IntCollector.toBooleanArray(x -> x < 0)));
        assertArrayEquals(expected, IntStreamEx.of(-1, 2, 3, -4).parallel().collect(
            IntCollector.toBooleanArray(x -> x < 0)));
    }
}
