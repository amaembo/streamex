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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.LongStreamEx;
import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.*;

/**
 * @author Tagir Valeev
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CustomPoolTest {
    ForkJoinPool pool = new ForkJoinPool(3);

    private void checkThread(Object element) {
        Thread thread = Thread.currentThread();
        if (!(thread instanceof ForkJoinWorkerThread))
            throw new IllegalStateException("Not inside FJP (element: " + element + ")");
        if (((ForkJoinWorkerThread) thread).getPool() != pool)
            throw new IllegalStateException("FJP is incorrect (element: " + element + ")");
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckThreadSequential() {
        StreamEx.of("a", "b").peek(this::checkThread).joining();
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckThreadParallel() {
        StreamEx.of("a", "b").parallel().peek(this::checkThread).joining();
    }

    @Test
    public void testStreamEx() {
        StreamEx.of("a", "b", "c").parallel(pool).forEach(this::checkThread);
        assertEquals(Arrays.asList(1, 2), StreamEx.of("a", "bb").parallel(pool).peek(this::checkThread).map(
            String::length).toList());
        assertEquals("a", StreamEx.of("a").parallel(pool).peek(this::checkThread).findAny().get());
        assertEquals("a", StreamEx.of("a", "b").parallel(pool).peek(this::checkThread).findFirst().get());
        assertTrue(StreamEx.of("a", "b").parallel(pool).peek(this::checkThread).anyMatch("a"::equals));
        assertFalse(StreamEx.of("a", "b").parallel(pool).peek(this::checkThread).allMatch("a"::equals));
        assertFalse(StreamEx.of("a", "b").parallel(pool).peek(this::checkThread).noneMatch("a"::equals));
        assertEquals(Arrays.asList("b", "c"), StreamEx.of("a", "b", "c").parallel(pool).peek(this::checkThread).skip(1)
                .collect(Collectors.toList()));
        assertEquals(6, StreamEx.of("a", "bb", "ccc").parallel(pool).peek(this::checkThread).collect(
            StringBuilder::new, StringBuilder::append, StringBuilder::append).length());
        assertArrayEquals(new String[] { "a", "b", "c" }, StreamEx.of("a", "b", "c").parallel(pool).peek(
            this::checkThread).toArray(String[]::new));
        assertArrayEquals(new Object[] { "a", "b", "c" }, StreamEx.of("a", "b", "c").parallel(pool).peek(
            this::checkThread).toArray());
        assertEquals("{ccc={bb={a={}}}}", StreamEx.of("a", "bb", "ccc").parallel(pool).peek(this::checkThread)
                .foldLeft(Collections.emptyMap(),
                    (Map<String, Object> acc, String v) -> Collections.singletonMap(v, acc)).toString());
        assertEquals(1000, IntStreamEx.constant(1, 1000).boxed().parallel(pool).peek(this::checkThread).foldLeft(0,
            Integer::sum).intValue());

        assertEquals(2, StreamEx.of("aa", "bbb", "cccc").parallel(pool).peek(this::checkThread).filter(
            x -> x.length() > 2).count());
        assertEquals("bbbcccc", StreamEx.of("aa", "bbb", "cccc").parallel(pool).peek(this::checkThread).filter(
            x -> x.length() > 2).reduce(String::concat).get());
        assertEquals("bbbcccc", StreamEx.of("aa", "bbb", "cccc").parallel(pool).peek(this::checkThread).filter(
            x -> x.length() > 2).reduce("", String::concat));
        assertEquals(7, (int) StreamEx.of("aa", "bbb", "cccc").parallel(pool).peek(this::checkThread).filter(
            x -> x.length() > 2).reduce(0, (x, s) -> x + s.length(), Integer::sum));
        assertEquals("aabbbcccc", StreamEx.of("aa", "bbb", "cccc").parallel(pool).peek(this::checkThread).foldLeft("",
            String::concat));
        assertEquals(Arrays.asList(1, 2, 3), StreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).toListAndThen(
            list -> {
                this.checkThread(list);
                return list;
            }));
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), StreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread)
                .toSetAndThen(list -> {
                    this.checkThread(list);
                    return list;
                }));

        assertEquals(Collections.singletonMap(1, 3L), StreamEx.of(1, 1, 1).parallel(pool).peek(this::checkThread)
                .runLengths().toMap());
    }

    @Test
    public void testEntryStream() {
        EntryStream.of("a", 1).parallel(pool).forEach(this::checkThread);
        assertEquals(Integer.valueOf(1), EntryStream.of("a", 1).parallel(pool).peek(this::checkThread).toMap().get("a"));
        assertEquals(Integer.valueOf(1), EntryStream.of("a", 1).parallel(pool).peek(this::checkThread).findAny(
            e -> e.getKey().equals("a")).get().getValue());
        assertEquals(Integer.valueOf(1), EntryStream.of("a", 1).parallel(pool).peek(this::checkThread).findFirst(
            e -> e.getKey().equals("a")).get().getValue());
        assertTrue(EntryStream.of("a", 1).parallel(pool).peek(this::checkThread).anyMatch(e -> e.getKey().equals("a")));
        assertTrue(EntryStream.of("a", 1).parallel(pool).peek(this::checkThread).allMatch(e -> e.getKey().equals("a")));
        assertFalse(EntryStream.of("a", 1).parallel(pool).peek(this::checkThread)
                .noneMatch(e -> e.getKey().equals("a")));
        assertEquals(2, EntryStream.of("a", 1, "b", 2, "c", 3).parallel(pool).peek(this::checkThread).filterValues(
            v -> v > 1).count());
        List<Integer> res = new ArrayList<>();
        EntryStream.of("a", 1, "b", 2, "c", 3).parallel(pool).peek(this::checkThread).filterValues(v -> v > 1)
                .forEachOrdered(entry -> res.add(entry.getValue()));
        assertEquals(Arrays.asList(2, 3), res);
        assertEquals(2L, (long) EntryStream.of("a", 1, "b", 2, "c", 3).parallel(pool).peek(this::checkThread)
                .filterValues(v -> v > 1).collect(Collectors.counting()));
        assertEquals(6, (int) EntryStream.of("a", 1, "b", 2, "c", 3).parallel(pool).peek(this::checkThread).reduce(0,
            (sum, e) -> sum + e.getValue(), Integer::sum));
        assertEquals(Arrays.asList(1, 2, 3), EntryStream.of("a", 1, "b", 2, "c", 3).parallel(pool).peek(
            this::checkThread).collect(ArrayList::new,
            (List<Integer> list, Entry<String, Integer> e) -> list.add(e.getValue()), List::addAll));
        @SuppressWarnings("unchecked")
        Entry<String, Integer>[] array = EntryStream.of("a", 1, "b", 2, "c", 3).parallel(pool).peek(this::checkThread)
                .filterValues(v -> v > 1).toArray(Entry[]::new);
        assertEquals(2, array.length);
        assertEquals(new SimpleEntry<>("b", 2), array[0]);
        assertEquals(new SimpleEntry<>("c", 3), array[1]);

        List<Entry<String, Integer>> list = EntryStream.of("a", 1, "b", 2, "c", 3).parallel(pool).peek(
            this::checkThread).filterValues(v -> v > 1).toListAndThen(l -> {
            this.checkThread(l);
            return l;
        });
        assertEquals(Arrays.asList(array), list);
        Map<String, Integer> map = EntryStream.of("a", 1, "b", 2, "c", 3).parallel(pool).peek(
            this::checkThread).filterValues(v -> v > 1).toMapAndThen(m -> {
            this.checkThread(m);
            return m;
        });
        assertEquals(EntryStream.of("b", 2, "c", 3).toMap(), map);

        assertEquals(new SimpleEntry<>("abc", 6), EntryStream.of("a", 1, "b", 2, "c", 3).parallel(pool).peek(
            this::checkThread).reduce(
            (e1, e2) -> new SimpleEntry<>(e1.getKey() + e2.getKey(), e1.getValue() + e2.getValue())).orElse(null));
        assertEquals(new SimpleEntry<>("abc", 6), EntryStream.of("a", 1, "b", 2, "c", 3).parallel(pool).peek(
            this::checkThread).reduce(new SimpleEntry<>("", 0),
            (e1, e2) -> new SimpleEntry<>(e1.getKey() + e2.getKey(), e1.getValue() + e2.getValue())));
    }

    @Test
    public void testIntStreamEx() {
        IntStreamEx.range(0, 4).parallel(pool).forEach(this::checkThread);
        assertEquals(6, IntStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).sum());
        assertEquals(3, IntStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).max().getAsInt());
        assertEquals(0, IntStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).min().getAsInt());
        assertEquals(1.5, IntStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).average().getAsDouble(),
            0.000001);
        assertEquals(4, IntStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).summaryStatistics().getCount());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStreamEx.range(0, 5).parallel(pool).peek(this::checkThread).skip(1)
                .limit(3).toArray());
        assertEquals(6, IntStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).reduce(Integer::sum).getAsInt());
        assertTrue(IntStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).has(1));
        assertTrue(IntStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).anyMatch(x -> x == 2));
        assertFalse(IntStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).allMatch(x -> x == 2));
        assertFalse(IntStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).noneMatch(x -> x == 2));
        assertEquals(6, IntStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).reduce(1, (a, b) -> a * b));
        assertEquals(2, IntStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).atLeast(2).count());
        assertEquals(2, IntStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).findAny(x -> x % 2 == 0)
                .getAsInt());
        assertEquals(2, IntStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).findFirst(x -> x % 2 == 0)
                .getAsInt());
        List<Integer> res = new ArrayList<>();
        IntStreamEx.of(1, 5, 10, Integer.MAX_VALUE).parallel(pool).peek(this::checkThread).map(x -> x * 2)
                .forEachOrdered(res::add);
        assertEquals(Arrays.asList(2, 10, 20, Integer.MAX_VALUE * 2), res);
        assertArrayEquals(new int[] { 1, 3, 6, 10 }, IntStreamEx.of(1, 2, 3, 4).parallel(pool).peek(this::checkThread)
                .scanLeft((a, b) -> {
                    checkThread(b);
                    return a + b;
                }));
    }

    @Test
    public void testLongStreamEx() {
        LongStreamEx.range(0, 4).parallel(pool).forEach(this::checkThread);
        assertEquals(999999000000L, IntStreamEx.range(1000000).parallel(pool).peek(this::checkThread).asLongStream()
                .map(x -> x * 2).sum());
        assertEquals(6, LongStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).sum());
        assertEquals(3, LongStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).max().getAsLong());
        assertEquals(0, LongStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).min().getAsLong());
        assertEquals(1.5, LongStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).average().getAsDouble(),
            0.000001);
        assertEquals(4, LongStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).summaryStatistics().getCount());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStreamEx.range(0, 5).parallel(pool).peek(this::checkThread).skip(
            1).limit(3).toArray());
        assertEquals(6, LongStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).reduce(Long::sum).getAsLong());
        assertTrue(LongStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).has(1));
        assertTrue(LongStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).anyMatch(x -> x == 2));
        assertFalse(LongStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).allMatch(x -> x == 2));
        assertFalse(LongStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).noneMatch(x -> x == 2));
        assertEquals(6, LongStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).reduce(1, (a, b) -> a * b));
        assertEquals(2, LongStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).atLeast(2).count());
        assertEquals(2, LongStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).findAny(x -> x % 2 == 0)
                .getAsLong());
        assertEquals(2, LongStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).findFirst(x -> x % 2 == 0)
                .getAsLong());
        List<Long> res = new ArrayList<>();
        LongStreamEx.of(1, 5, 10, Integer.MAX_VALUE).parallel(pool).peek(this::checkThread).map(x -> x * 2)
                .forEachOrdered(res::add);
        assertEquals(Arrays.asList(2L, 10L, 20L, Integer.MAX_VALUE * 2L), res);
        assertArrayEquals(new long[] { 1, 3, 6, 10 }, LongStreamEx.of(1, 2, 3, 4).parallel(pool)
                .peek(this::checkThread).scanLeft((a, b) -> {
                    checkThread(b);
                    return a + b;
                }));
    }

    @Test
    public void testDoubleStreamEx() {
        LongStreamEx.range(0, 4).asDoubleStream().parallel(pool).forEach(this::checkThread);
        assertEquals(6, IntStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).asDoubleStream().sum(), 0);
        assertEquals(3, IntStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).asDoubleStream().max()
                .getAsDouble(), 0);
        assertEquals(0, IntStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).asDoubleStream().min()
                .getAsDouble(), 0);
        assertEquals(1.5, IntStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).asDoubleStream().average()
                .getAsDouble(), 0.000001);
        assertEquals(4, IntStreamEx.range(0, 4).parallel(pool).peek(this::checkThread).asDoubleStream()
                .summaryStatistics().getCount());
        assertArrayEquals(new double[] { 1, 2, 3 }, IntStreamEx.range(0, 5).asDoubleStream().skip(1).limit(3).parallel(
            pool).peek(this::checkThread).toArray(), 0.0);
        assertEquals(6.0, DoubleStreamEx.of(1.0, 2.0, 3.0).parallel(pool).peek(this::checkThread).reduce(Double::sum)
                .getAsDouble(), 0.0);
        assertTrue(DoubleStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).anyMatch(x -> x == 2));
        assertFalse(DoubleStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).allMatch(x -> x == 2));
        assertFalse(DoubleStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).noneMatch(x -> x == 2));
        assertEquals(6.0, DoubleStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).reduce(1, (a, b) -> a * b),
            0.0);
        assertEquals(2, DoubleStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).atLeast(2.0).count());
        assertEquals(2.0, DoubleStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).findAny(x -> x % 2 == 0)
                .getAsDouble(), 0.0);
        assertEquals(2.0, DoubleStreamEx.of(1, 2, 3).parallel(pool).peek(this::checkThread).findFirst(x -> x % 2 == 0)
                .getAsDouble(), 0.0);
        List<Double> res = new ArrayList<>();
        DoubleStreamEx.of(1.0, 2.0, 3.5, 4.5).parallel(pool).peek(this::checkThread).map(x -> x * 2).forEachOrdered(
            res::add);
        assertEquals(Arrays.asList(2.0, 4.0, 7.0, 9.0), res);
        assertArrayEquals(new double[] { 1, 3, 6, 10 }, DoubleStreamEx.of(1, 2, 3, 4).parallel(pool).peek(
            this::checkThread).scanLeft((a, b) -> {
            checkThread(b);
            return a + b;
        }), 0.0);
    }

    @Test
    public void testPairMap() {
        BitSet bits = IntStreamEx.range(3, 199).toBitSet();
        IntStreamEx.range(200).parallel(pool).filter(i -> {
            checkThread(i);
            return i > 2;
        }).boxed().pairMap(SimpleEntry<Integer, Integer>::new).forEach(p -> {
            checkThread(p);
            assertEquals(1, p.getValue() - p.getKey());
            assertTrue(p.getKey().toString(), bits.get(p.getKey()));
            bits.clear(p.getKey());
        });
    }

    @Test
    public void testShortCircuit() {
        AtomicInteger counter = new AtomicInteger(0);
        assertEquals(Optional.empty(), IntStreamEx.range(0, 10000).boxed().parallel(pool).peek(this::checkThread).peek(
            t -> counter.incrementAndGet()).collect(MoreCollectors.onlyOne()));
        assertTrue(counter.get() < 10000);
        counter.set(0);
        assertEquals(Optional.empty(), IntStreamEx.range(0, 10000).boxed().mapToEntry(x -> x).parallel(pool).peek(
            this::checkThread).peek(t -> counter.incrementAndGet()).collect(MoreCollectors.onlyOne()));
        assertTrue(counter.get() < 10000);
    }
}
