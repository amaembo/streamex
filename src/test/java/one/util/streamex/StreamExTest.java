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
package one.util.streamex;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import one.util.streamex.IntStreamEx;
import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;
import one.util.streamex.TestHelpers.StreamExSupplier;

import org.junit.Test;

import static one.util.streamex.TestHelpers.*;
import static org.junit.Assert.*;

public class StreamExTest {
    @Test
    public void testCreate() {
        assertEquals(Arrays.asList(), StreamEx.empty().toList());
        // double test is intended
        assertEquals(Arrays.asList(), StreamEx.empty().toList());
        assertEquals(Arrays.asList("a"), StreamEx.of("a").toList());
        assertEquals(Arrays.asList("a"), StreamEx.of(Optional.of("a")).toList());
        assertEquals(Arrays.asList(), StreamEx.of(Optional.ofNullable(null)).toList());
        assertEquals(Arrays.asList(), StreamEx.ofNullable(null).toList());
        assertEquals(Arrays.asList("a"), StreamEx.ofNullable("a").toList());
        assertEquals(Arrays.asList((String) null), StreamEx.of((String) null).toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.of("a", "b").toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.of(Arrays.asList("a", "b")).toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.of(Arrays.asList("a", "b").stream()).toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.split("a,b", ",").toList());
        assertEquals(Arrays.asList("a", "c", "d"),
            StreamEx.split("abcBd", Pattern.compile("b", Pattern.CASE_INSENSITIVE)).toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.ofLines(new StringReader("a\nb")).toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.ofLines(new BufferedReader(new StringReader("a\nb"))).toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.ofLines(getReader()).toList());
        assertEquals(Arrays.asList("a", "aa", "aaa", "aaaa"), StreamEx.iterate("a", x -> x + "a").limit(4).toList());
        assertEquals(Arrays.asList("a", "a", "a", "a"), StreamEx.generate(() -> "a").limit(4).toList());
        assertEquals(Arrays.asList("a", "a", "a", "a"), StreamEx.constant("a", 4).toList());
        assertEquals(Arrays.asList("c", "d", "e"), StreamEx.of("abcdef".split(""), 2, 5).toList());

        StreamEx<String> stream = StreamEx.of("foo", "bar");
        assertSame(stream.stream, StreamEx.of(stream).stream);

        assertEquals(Arrays.asList("a1", "b2", "c3"),
            StreamEx.zip(Arrays.asList("a", "b", "c"), Arrays.asList(1, 2, 3), (s, i) -> s + i).toList());
        assertEquals(Arrays.asList("a1", "b2", "c3"),
            StreamEx.zip(new String[] { "a", "b", "c" }, new Integer[] { 1, 2, 3 }, (s, i) -> s + i).toList());

        assertEquals(Arrays.asList("a", "b"), StreamEx.of(Arrays.asList("a", "b").spliterator()).toList());
    }

    private Reader getReader() {
        return new BufferedReader(new StringReader("a\nb"));
    }

    @Test
    public void testReader() {
        String input = IntStreamEx.range(5000).joining("\n");
        List<String> expectedList = IntStreamEx.range(1, 5000).mapToObj(String::valueOf).toList();
        Set<String> expectedSet = IntStreamEx.range(1, 5000).mapToObj(String::valueOf).toSet();
        assertEquals(expectedList, StreamEx.ofLines(new StringReader(input)).skip(1).parallel().toList());
        assertEquals(expectedList, StreamEx.ofLines(new StringReader(input)).pairMap((a, b) -> b).parallel().toList());

        assertEquals(expectedSet, StreamEx.ofLines(new StringReader(input)).pairMap((a, b) -> b).parallel().toSet());
        assertEquals(expectedSet,
            StreamEx.ofLines(new StringReader(input)).skip(1).parallel().toCollection(HashSet::new));

        assertEquals(expectedSet, StreamEx.ofLines(new StringReader(input)).parallel().skipOrdered(1).toSet());
        assertEquals(expectedSet, StreamEx.ofLines(new StringReader(input)).skipOrdered(1).parallel().toSet());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZipThrows() {
        StreamEx.zip(Arrays.asList("A"), Arrays.asList("b", "c"), String::concat);
    }

    @Test
    public void testBasics() {
        assertFalse(StreamEx.of("a").isParallel());
        assertTrue(StreamEx.of("a").parallel().isParallel());
        assertFalse(StreamEx.of("a").parallel().sequential().isParallel());
        AtomicInteger i = new AtomicInteger();
        try (Stream<String> s = StreamEx.of("a").onClose(() -> i.incrementAndGet())) {
            assertEquals(1, s.count());
        }
        assertEquals(1, i.get());
        assertEquals(Arrays.asList(1, 2), StreamEx.of("a", "bb").map(String::length).toList());
        assertFalse(StreamEx.empty().findAny().isPresent());
        assertEquals("a", StreamEx.of("a").findAny().get());
        assertFalse(StreamEx.empty().findFirst().isPresent());
        assertEquals("a", StreamEx.of("a", "b").findFirst().get());
        assertEquals(Arrays.asList("b", "c"), StreamEx.of("a", "b", "c").skip(1).toList());

        AtomicBoolean b = new AtomicBoolean(false);
        try (Stream<String> stream = StreamEx.of("a").onClose(() -> b.set(true))) {
            assertFalse(b.get());
            assertEquals(1, stream.count());
            assertFalse(b.get());
        }
        assertTrue(b.get());

        assertTrue(StreamEx.of("a", "b").anyMatch("a"::equals));
        assertFalse(StreamEx.of("a", "b").anyMatch("c"::equals));
        assertFalse(StreamEx.of("a", "b").allMatch("a"::equals));
        assertFalse(StreamEx.of("a", "b").allMatch("c"::equals));
        assertFalse(StreamEx.of("a", "b").noneMatch("a"::equals));
        assertTrue(StreamEx.of("a", "b").noneMatch("c"::equals));
        assertTrue(StreamEx.of().noneMatch("a"::equals));
        assertTrue(StreamEx.of().allMatch("a"::equals));
        assertFalse(StreamEx.of().anyMatch("a"::equals));

        assertEquals("abbccc",
            StreamEx.of("a", "bb", "ccc").collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                    .toString());
        assertArrayEquals(new String[] { "a", "b", "c" }, StreamEx.of("a", "b", "c").toArray(String[]::new));
        assertArrayEquals(new Object[] { "a", "b", "c" }, StreamEx.of("a", "b", "c").toArray());
        assertEquals(3, StreamEx.of("a", "b", "c").spliterator().getExactSizeIfKnown());

        assertTrue(StreamEx.of("a", "b", "c").spliterator().hasCharacteristics(Spliterator.ORDERED));
        assertFalse(StreamEx.of("a", "b", "c").unordered().spliterator().hasCharacteristics(Spliterator.ORDERED));
    }

    @Test
    public void testToList() {
        List<Integer> list = StreamEx.of(1, 2, 3).toList();
        // Test that returned list is mutable
        List<Integer> list2 = StreamEx.of(4, 5, 6).parallel().toList();
        list2.add(7);
        list.addAll(list2);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), list);
    }

    @Test
    public void testFlatMap() {
        assertArrayEquals(new int[] { 0, 0, 1, 0, 0, 1, 0, 0 },
            StreamEx.of("111", "222", "333").flatMapToInt(s -> s.chars().map(ch -> ch - '0')).pairMap((a, b) -> b - a)
                    .toArray());
        assertArrayEquals(
            new long[] { 0, 0, 1, 0, 0, 1, 0, 0 },
            StreamEx.of("111", "222", "333").flatMapToLong(s -> s.chars().mapToLong(ch -> ch - '0'))
                    .pairMap((a, b) -> b - a).toArray());
        assertArrayEquals(
            new double[] { 0, 0, 1, 0, 0, 1, 0, 0 },
            StreamEx.of("111", "222", "333").flatMapToDouble(s -> s.chars().mapToDouble(ch -> ch - '0'))
                    .pairMap((a, b) -> b - a).toArray(), 0.0);
    }

    @Test
    public void testAndThen() {
        HashSet<String> set = StreamEx.of("a", "bb", "ccc").toListAndThen(HashSet<String>::new);
        assertEquals(3, set.size());
        assertTrue(set.contains("bb"));

        ArrayList<String> list = StreamEx.of("a", "bb", "ccc").toSetAndThen(ArrayList<String>::new);
        assertEquals(3, list.size());
        assertTrue(list.contains("bb"));
    }

    @Test
    public void testToMap() {
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("bb", 2);
        expected.put("ccc", 3);

        Map<Integer, String> expected2 = new HashMap<>();
        expected2.put(1, "a");
        expected2.put(2, "bb");
        expected2.put(3, "ccc");

        for (StreamExSupplier<String> supplier : streamEx(() -> Stream.of("a", "bb", "ccc"))) {
            Map<String, Integer> map = supplier.get().toMap(String::length);
            assertEquals(supplier.toString(), supplier.get().isParallel(), map instanceof ConcurrentMap);
            assertEquals(supplier.toString(), expected, map);

            Map<Integer, String> map2 = supplier.get().toMap(String::length, Function.identity());
            assertEquals(supplier.toString(), supplier.get().isParallel(), map2 instanceof ConcurrentMap);
            assertEquals(supplier.toString(), expected2, map2);
        }

        Map<Integer, String> expected3 = new HashMap<>();
        expected3.put(1, "a");
        expected3.put(2, "bbdd");
        expected3.put(3, "ccc");
        for (StreamExSupplier<String> supplier : streamEx(() -> Stream.of("a", "bb", "ccc", "dd"))) {
            Map<Integer, String> seqMap3 = supplier.get().toMap(String::length, Function.identity(), String::concat);
            assertEquals(supplier.toString(), expected3, seqMap3);

            try {
                supplier.get().toMap(String::length, Function.identity());
            } catch (IllegalStateException ex) {
                if (!ex.getMessage().equals("Duplicate entry for key '2' (attempt to merge values 'bb' and 'dd')")
                    && !ex.getMessage().equals("Duplicate entry for key '2' (attempt to merge values 'dd' and 'bb')"))
                    fail(supplier + ": wrong exception message: " + ex.getMessage());
                continue;
            }
            fail(supplier + ": no exception");
        }
    }

    @Test
    public void testToSortedMap() {
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("bb", 2);
        expected.put("ccc", 3);

        Map<Integer, String> expected2 = new HashMap<>();
        expected2.put(1, "a");
        expected2.put(2, "bb");
        expected2.put(3, "ccc");

        for (StreamExSupplier<String> supplier : streamEx(() -> Stream.of("a", "bb", "ccc"))) {
            SortedMap<String, Integer> map = supplier.get().toSortedMap(String::length);
            assertEquals(supplier.toString(), supplier.get().isParallel(), map instanceof ConcurrentMap);
            assertEquals(supplier.toString(), expected, map);

            SortedMap<Integer, String> map2 = supplier.get().toSortedMap(String::length, Function.identity());
            assertEquals(supplier.toString(), supplier.get().isParallel(), map2 instanceof ConcurrentMap);
            assertEquals(supplier.toString(), expected2, map2);
        }

        Map<Integer, String> expected3 = new HashMap<>();
        expected3.put(1, "a");
        expected3.put(2, "bbdd");
        expected3.put(3, "ccc");
        for (StreamExSupplier<String> supplier : streamEx(() -> Stream.of("a", "bb", "ccc", "dd"))) {
            SortedMap<Integer, String> seqMap3 = supplier.get().toSortedMap(String::length, Function.identity(),
                String::concat);
            assertEquals(supplier.toString(), expected3, seqMap3);

            try {
                supplier.get().toSortedMap(String::length, Function.identity());
            } catch (IllegalStateException ex) {
                if (!ex.getMessage().equals("Duplicate entry for key '2' (attempt to merge values 'bb' and 'dd')")
                    && !ex.getMessage().equals("Duplicate entry for key '2' (attempt to merge values 'dd' and 'bb')"))
                    fail(supplier + ": wrong exception message: " + ex.getMessage());
                continue;
            }
            fail(supplier + ": no exception");
        }
    }

    @Test
    public void testGroupingBy() {
        Map<Integer, List<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("a"));
        expected.put(2, Arrays.asList("bb", "dd"));
        expected.put(3, Arrays.asList("ccc"));

        Map<Integer, Set<String>> expectedMapSet = new HashMap<>();
        expectedMapSet.put(1, new HashSet<>(Arrays.asList("a")));
        expectedMapSet.put(2, new HashSet<>(Arrays.asList("bb", "dd")));
        expectedMapSet.put(3, new HashSet<>(Arrays.asList("ccc")));

        for (StreamExSupplier<String> supplier : streamEx(() -> StreamEx.of("a", "bb", "dd", "ccc"))) {
            assertEquals(supplier.toString(), expected, supplier.get().groupingBy(String::length));
            Map<Integer, List<String>> map = supplier.get().groupingTo(String::length, LinkedList::new);
            assertEquals(supplier.toString(), expected, map);
            assertTrue(map.get(1) instanceof LinkedList);
            assertEquals(supplier.toString(), expectedMapSet,
                supplier.get().groupingBy(String::length, Collectors.toSet()));
            assertEquals(supplier.toString(), expectedMapSet,
                supplier.get().groupingBy(String::length, HashMap::new, Collectors.toSet()));
            ConcurrentHashMap<Integer, Set<String>> chm = supplier.get().groupingBy(String::length,
                ConcurrentHashMap::new, Collectors.toSet());
            assertEquals(supplier.toString(), expectedMapSet, chm);
            chm = supplier.get().groupingTo(String::length, ConcurrentHashMap::new, TreeSet::new);
            assertTrue(chm.get(1) instanceof TreeSet);
        }
    }

    @Test
    public void testPartitioning() {
        Map<Boolean, List<String>> map = StreamEx.of("a", "bb", "c", "dd").partitioningBy(s -> s.length() > 1);
        assertEquals(Arrays.asList("bb", "dd"), map.get(true));
        assertEquals(Arrays.asList("a", "c"), map.get(false));
        Map<Boolean, Long> counts = StreamEx.of("a", "bb", "c", "dd", "eee").partitioningBy(s -> s.length() > 1,
            Collectors.counting());
        assertEquals(3L, (long) counts.get(true));
        assertEquals(2L, (long) counts.get(false));
        Map<Boolean, List<String>> mapLinked = StreamEx.of("a", "bb", "c", "dd").partitioningTo(s -> s.length() > 1,
            LinkedList::new);
        assertEquals(Arrays.asList("bb", "dd"), mapLinked.get(true));
        assertEquals(Arrays.asList("a", "c"), mapLinked.get(false));
        assertTrue(mapLinked.get(true) instanceof LinkedList);
    }

    @Test
    public void testIterable() {
        List<String> result = new ArrayList<>();
        for (String s : StreamEx.of("a", "b", "cc").filter(s -> s.length() < 2)) {
            result.add(s);
        }
        assertEquals(Arrays.asList("a", "b"), result);
    }

    @Test
    public void testCreateFromMap() {
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("aaa", 10);
        data.put("bb", 25);
        data.put("c", 37);
        assertEquals(Arrays.asList("aaa", "bb", "c"), StreamEx.ofKeys(data).toList());
        assertEquals(Arrays.asList("aaa"), StreamEx.ofKeys(data, x -> x % 2 == 0).toList());
        assertEquals(Arrays.asList(10, 25, 37), StreamEx.ofValues(data).toList());
        assertEquals(Arrays.asList(10, 25), StreamEx.ofValues(data, s -> s.length() > 1).toList());
    }

    @Test
    public void testSelect() {
        assertEquals(Arrays.asList("a", "b"),
            StreamEx.of(1, "a", 2, "b", 3, "cc").select(String.class).filter(s -> s.length() == 1).toList());
    }

    @Test
    public void testFlatCollection() {
        Map<Integer, List<String>> data = new LinkedHashMap<>();
        data.put(1, Arrays.asList("a", "b"));
        data.put(2, Arrays.asList("c", "d"));
        data.put(3, null);
        assertEquals(Arrays.asList("a", "b", "c", "d"), StreamEx.of(data.entrySet()).flatCollection(Entry::getValue)
                .toList());
    }

    @Test
    public void testAppend() {
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"),
            StreamEx.of("a", "b", "c", "dd").remove(s -> s.length() > 1).append("d", "e").toList());
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"),
            StreamEx.of("a", "b", "c").append(Arrays.asList("d", "e").stream()).toList());
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), StreamEx.of("a", "b", "c").append(Arrays.asList("d", "e"))
                .toList());

        List<Integer> list = Arrays.asList(1, 2, 3, 4);
        assertEquals(Arrays.asList(1.0, 2, 3L, 1, 2, 3, 4), StreamEx.of(1.0, 2, 3L).append(list).toList());

        StreamEx<Integer> s = StreamEx.of(1, 2, 3);
        assertSame(s, s.append());
        assertSame(s, s.append(Collections.emptyList()));
        assertSame(s, s.prepend());
        assertSame(s, s.prepend(Collections.emptyList()));
    }

    @Test
    public void testPrepend() {
        assertEquals(Arrays.asList("d", "e", "a", "b", "c"),
            StreamEx.of("a", "b", "c", "dd").remove(s -> s.length() > 1).prepend("d", "e").toList());
        assertEquals(Arrays.asList("d", "e", "a", "b", "c"),
            StreamEx.of("a", "b", "c").prepend(Arrays.asList("d", "e").stream()).toList());
        assertEquals(Arrays.asList("d", "e", "a", "b", "c"), StreamEx.of("a", "b", "c")
                .prepend(Arrays.asList("d", "e")).toList());
    }

    @Test
    public void testNonNull() {
        List<String> data = Arrays.asList("a", null, "b");
        assertEquals(Arrays.asList("a", null, "b"), StreamEx.of(data).toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.of(data).nonNull().toList());
    }

    @Test
    public void testSorting() {
        assertEquals(Arrays.asList("a", "b", "c", "d"), StreamEx.of("b", "c", "a", "d").sorted().toList());
        assertEquals(Arrays.asList("d", "c", "b", "a"), StreamEx.of("b", "c", "a", "d").reverseSorted().toList());

        List<String> data = Arrays.asList("a", "bbb", "cc");
        assertEquals(Arrays.asList("a", "cc", "bbb"), StreamEx.of(data).sorted(Comparator.comparingInt(String::length))
                .toList());
        assertEquals(Arrays.asList("bbb", "cc", "a"),
            StreamEx.of(data).reverseSorted(Comparator.comparingInt(String::length)).toList());
        assertEquals(Arrays.asList("a", "cc", "bbb"), StreamEx.of(data).sortedByInt(String::length).toList());
        assertEquals(Arrays.asList("a", "cc", "bbb"), StreamEx.of(data).sortedByLong(String::length).toList());
        assertEquals(Arrays.asList("a", "cc", "bbb"), StreamEx.of(data).sortedByDouble(String::length).toList());
        assertEquals(Arrays.asList("a", "cc", "bbb"), StreamEx.of(data).sortedBy(s -> s.length()).toList());
    }

    @Test
    public void testMinMax() {
        Random random = new Random(1);
        List<String> data = IntStreamEx.of(random, 1000, 1, 100)
                .mapToObj(len -> IntStreamEx.constant(random.nextInt('z' - 'a' + 1) + 'a', len).charsToString())
                .toList();
        String minStr = null, maxStr = null;
        for (String str : data) {
            if (minStr == null || minStr.length() > str.length())
                minStr = str;
            if (maxStr == null || maxStr.length() < str.length())
                maxStr = str;
        }
        for (StreamExSupplier<String> supplier : streamEx(data::stream)) {
            assertEquals(supplier.toString(), maxStr, supplier.get().max(Comparator.comparingInt(String::length)).get());
            assertEquals(supplier.toString(), maxStr, supplier.get().maxByInt(String::length).get());
            assertEquals(supplier.toString(), maxStr, supplier.get().maxByLong(String::length).get());
            assertEquals(supplier.toString(), maxStr, supplier.get().maxByDouble(String::length).get());
            assertEquals(supplier.toString(), maxStr, supplier.get().maxBy(String::length).get());

            assertEquals(supplier.toString(), maxStr,
                supplier.get().min(Comparator.comparingInt(String::length).reversed()).get());
            assertEquals(supplier.toString(), minStr, supplier.get().minByInt(String::length).get());
            assertEquals(supplier.toString(), minStr, supplier.get().minByLong(String::length).get());
            assertEquals(supplier.toString(), minStr, supplier.get().minByDouble(String::length).get());
            assertEquals(supplier.toString(), minStr, supplier.get().minBy(String::length).get());
        }
        assertFalse(StreamEx.<String>empty().minByInt(String::length).isPresent());
    }

    @Test
    public void testFind() {
        assertEquals("bb", StreamEx.of("a", "bb", "c").findFirst(s -> s.length() == 2).get());
        assertFalse(StreamEx.of("a", "bb", "c").findFirst(s -> s.length() == 3).isPresent());
    }

    @Test
    public void testHas() {
        assertTrue(StreamEx.of("a", "bb", "c").has("bb"));
        assertFalse(StreamEx.of("a", "bb", "c").has("cc"));
        assertFalse(StreamEx.of("a", "bb", "c").has(null));
        assertTrue(StreamEx.of("a", "bb", null, "c").has(null));
    }

    @Test
    public void testWithout() {
        assertEquals(Arrays.asList("a", "bb", null), StreamEx.of("a", "bb", null, "c").without("c").toList());
        assertEquals(Arrays.asList("a", "bb", "c"), StreamEx.of("a", "bb", null, "c", null).without(null).toList());
        assertTrue(StreamEx.of("bb", "bb", "bb").without("bb").toList().isEmpty());
        assertEquals(Arrays.asList("bb", "bb", "bb"), StreamEx.of("bb", "bb", "bb").without(null).toList());
    }

    @Test
    public void testJoining() {
        assertEquals("abc", StreamEx.of("a", "b", "c").joining());
        assertEquals("a,b,c", StreamEx.of("a", "b", "c").joining(","));
        assertEquals("[1;2;3]", StreamEx.of(1, 2, 3).joining(";", "[", "]"));

        Random r = new Random(1);
        List<Integer> input1 = IntStreamEx.of(r, 1000, 0, 1000).boxed().toList();
        List<String> input2 = IntStreamEx.of(r, 1000, 0, 1000).mapToObj(String::valueOf).toList();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input1.size(); i++) {
            if (sb.length() > 0)
                sb.append(',');
            sb.append(input1.get(i)).append(':').append(input2.get(i));
        }
        String expected = sb.toString();
        assertEquals(expected, StreamEx.zip(input1, input2, (i, s) -> i + ":" + s).joining(","));
        assertEquals(expected, StreamEx.zip(input1, input2, (i, s) -> i + ":" + s).parallel().joining(","));
    }

    @Test
    public void testFoldLeft() {
        List<String> input = Arrays.asList("a", "bb", "ccc");
        for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
            assertEquals(supplier.toString(), "ccc;bb;a;", supplier.get().foldLeft("", (u, v) -> v + ";" + u));
            // Removing types here causes internal error in Javac compiler
            // java.lang.AssertionError: attribution shouldn't be happening here
            // Bug appears in javac 1.8.0.20 and javac 1.8.0.45
            // javac 1.9.0b55 and ecj compiles normally
            // Probably this ticket:
            // https://bugs.openjdk.java.net/browse/JDK-8068399
            assertTrue(supplier.toString(),
                supplier.get().foldLeft(false, (Boolean acc, String s) -> acc || s.equals("bb")));
            assertFalse(supplier.toString(),
                supplier.get().foldLeft(false, (Boolean acc, String s) -> acc || s.equals("d")));
            assertEquals(supplier.toString(), 6, (int) supplier.get().foldLeft(0, (acc, v) -> acc + v.length()));
            assertEquals(
                supplier.toString(),
                "{ccc={bb={a={}}}}",
                supplier.get()
                        .foldLeft(Collections.emptyMap(),
                            (Map<String, Object> acc, String v) -> Collections.singletonMap(v, acc)).toString());
        }
    }

    @Test
    public void testFoldLeftOptional() {
        // non-associative
        BinaryOperator<Integer> accumulator = (x, y) -> (x + y) * (x + y);
        for (StreamExSupplier<Integer> supplier : streamEx(() -> StreamEx.constant(3, 4))) {
            assertEquals(supplier.toString(), 2322576, (int) supplier.get().foldLeft(accumulator).orElse(-1));
        }
        for (StreamExSupplier<Integer> supplier : streamEx(() -> StreamEx.of(1, 2, 3))) {
            assertEquals(supplier.toString(), 144, (int) supplier.get().foldLeft(accumulator).orElse(-1));
        }
        for (StreamExSupplier<Integer> supplier : emptyStreamEx(Integer.class)) {
            assertFalse(supplier.toString(), supplier.get().foldLeft(accumulator).isPresent());
        }
    }

    @Test
    public void testFoldRight() {
        assertEquals(";c;b;a", StreamEx.of("a", "b", "c").parallel().foldRight("", (u, v) -> v + ";" + u));
        assertEquals(
            "{a={bb={ccc={}}}}",
            StreamEx.of("a", "bb", "ccc")
                    .foldRight(Collections.emptyMap(),
                        (String v, Map<String, Object> acc) -> Collections.singletonMap(v, acc)).toString());
        assertEquals(
            "{a={bb={ccc={}}}}",
            StreamEx.of("a", "bb", "ccc")
                    .parallel()
                    .foldRight(Collections.emptyMap(),
                        (String v, Map<String, Object> acc) -> Collections.singletonMap(v, acc)).toString());
    }

    @Test
    public void testFoldRightOptional() {
        // non-associative
        BinaryOperator<Integer> accumulator = (x, y) -> (x + y) * (x + y);
        for (StreamExSupplier<Integer> supplier : streamEx(() -> StreamEx.constant(3, 4))) {
            assertEquals(supplier.toString(), 2322576, (int) supplier.get().foldRight(accumulator).orElse(-1));
        }
        for (StreamExSupplier<Integer> supplier : streamEx(() -> StreamEx.of(1, 2, 3, 0))) {
            assertEquals(supplier.toString(), 14884, (int) supplier.get().foldRight(accumulator).orElse(-1));
        }
        for (StreamExSupplier<Integer> supplier : emptyStreamEx(Integer.class)) {
            assertFalse(supplier.toString(), supplier.get().foldRight(accumulator).isPresent());
        }
    }

    @Test
    public void testDistinctAtLeast() {
        assertEquals(0, StreamEx.of("a", "b", "c").distinct(2).count());
        assertEquals(StreamEx.of("a", "b", "c").distinct().toList(), StreamEx.of("a", "b", "c").distinct(1).toList());
        assertEquals(Arrays.asList("b"), StreamEx.of("a", "b", "c", "b", null).distinct(2).toList());
        assertEquals(Arrays.asList("b", null), StreamEx.of("a", "b", null, "c", "b", null).distinct(2).toList());
        assertEquals(Arrays.asList(null, "b"), StreamEx.of("a", "b", null, "c", null, "b", null, "b").distinct(2)
                .toList());
        for (StreamExSupplier<Integer> supplier : streamEx(() -> IntStreamEx.range(0, 1000).map(x -> x / 3).boxed())) {
            assertEquals(supplier.toString(), 334, supplier.get().distinct().count());
            assertEquals(supplier.toString(), 333, supplier.get().distinct(2).count());
            assertEquals(supplier.toString(), 333, supplier.get().distinct(3).count());
            assertEquals(supplier.toString(), 0, supplier.get().distinct(4).count());

            List<Integer> distinct3List = supplier.get().distinct(3).toList();
            assertEquals(supplier.toString(), 333, distinct3List.size());
            Map<Integer, Long> map = supplier.get().collect(
                Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
            List<Integer> expectedList = StreamEx.ofKeys(map, val -> val >= 3).toList();
            assertEquals(supplier.toString(), 333, expectedList.size());
            assertEquals(supplier.toString(), distinct3List, expectedList);
        }

        assertEquals(0, StreamEx.of("a", "b", "c").parallel().distinct(2).count());
        assertEquals(StreamEx.of("a", "b", "c").parallel().distinct().toList(), StreamEx.of("a", "b", "c").parallel()
                .distinct(1).toList());
        assertEquals(Arrays.asList("b"), StreamEx.of("a", "b", "c", "b").parallel().distinct(2).toList());
        assertEquals(new HashSet<>(Arrays.asList("b", null)), StreamEx.of("a", "b", null, "c", "b", null).parallel()
                .distinct(2).toSet());
        assertEquals(new HashSet<>(Arrays.asList(null, "b")), StreamEx.of("a", "b", null, "c", null, "b", null, "b")
                .parallel().distinct(2).toSet());

        for (int i = 1; i < 1000; i += 100) {
            List<Integer> input = IntStreamEx.of(new Random(1), i, 1, 100).boxed().toList();
            for (int n : IntStreamEx.range(2, 10).boxed()) {
                Set<Integer> expected = input.stream().collect(
                    Collectors.collectingAndThen(Collectors.groupingBy(Function.identity(), Collectors.counting()),
                        m -> {
                            m.values().removeIf(l -> l < n);
                            return m.keySet();
                        }));
                assertEquals(expected, StreamEx.of(input).distinct(n).toSet());
                assertEquals(expected, StreamEx.of(input).parallel().distinct(n).toSet());
                assertEquals(0, StreamEx.of(expected).distinct(2).toSet().size());
            }
        }

        assertEquals(IntStreamEx.range(10).boxed().toList(), IntStreamEx.range(100).mapToObj(x -> x / 10).sorted()
                .distinct(3).sorted().toList());
        
        assertEquals(Arrays.asList("a", "b"), StreamEx.split("a,b,a,c,d,b,a", ",").parallel().distinct(2).sorted()
                .toList());
    }

    @Test
    public void testDistinctAtLeastPairMap() {
        int last = -1;
        int cur = -1;
        int count = 0;
        List<Integer> expected = new ArrayList<>();
        for (int i : IntStreamEx.of(new Random(1), 1000, 0, 100).sorted().boxed()) {
            if (i == cur) {
                count++;
                if (count == 15) {
                    if (last >= 0) {
                        expected.add(cur - last);
                    }
                    last = cur;
                }
            } else {
                count = 1;
                cur = i;
            }
        }
        for (StreamExSupplier<Integer> supplier : streamEx(() -> new Random(1).ints(1000, 0, 100).sorted().boxed())) {
            assertEquals(supplier.toString(), expected, supplier.get().distinct(15).pairMap((a, b) -> b - a).toList());
        }
    }

    private <T extends Comparable<? super T>> boolean isSorted(Collection<T> c) {
        return StreamEx.of(c).parallel().pairMap(Comparable::compareTo).allMatch(r -> r <= 0);
    }

    private <T extends Comparable<? super T>> Optional<T> firstMisplaced(Collection<T> c) {
        return StreamEx.of(c).parallel().pairMap((a, b) -> a.compareTo(b) > 0 ? a : null).nonNull().findFirst();
    }

    static class Point {
        double x, y;

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        double distance(Point o) {
            return Math.sqrt((x - o.x) * (x - o.x) + (y - o.y) * (y - o.y));
        }
    }

    @Test
    public void testPairMap() {
        assertEquals(0, StreamEx.<String> empty().pairMap(String::concat).count());
        assertArrayEquals(new Object[0], StreamEx.<String> empty().pairMap(String::concat).toArray());
        assertEquals(0, StreamEx.of("a").pairMap(String::concat).count());
        assertEquals(Arrays.asList("aa", "aa", "aa"), StreamEx.generate(() -> "a").pairMap(String::concat).limit(3)
                .toList());
        AtomicBoolean flag = new AtomicBoolean();
        assertFalse(flag.get());
        StreamEx<String> stream = StreamEx.of("a", "b").onClose(() -> flag.set(true)).pairMap(String::concat);
        stream.close();
        assertTrue(flag.get());
        assertEquals(Collections.singletonMap(1, 1999L), IntStreamEx.range(2000).boxed().pairMap((a, b) -> b - a)
                .groupingBy(Function.identity(), Collectors.counting()));
        assertEquals(
            Collections.singletonMap(1, 1999L),
            IntStreamEx.range(2000).parallel().boxed().pairMap((a, b) -> b - a)
                    .groupingBy(Function.identity(), Collectors.counting()));
        Integer[] data = new Random(1).ints(1000, 1, 1000).boxed().toArray(Integer[]::new);
        Double[] expected = new Double[data.length - 1];
        for (int i = 0; i < expected.length; i++)
            expected[i] = (data[i + 1] - data[i]) * 1.23;
        Double[] result = StreamEx.of(data).parallel().pairMap((a, b) -> (b - a) * 1.23).toArray(Double[]::new);
        assertArrayEquals(expected, result);
        result = StreamEx.of(data).pairMap((a, b) -> (b - a) * 1.23).toArray(Double[]::new);
        assertArrayEquals(expected, result);

        // Find all numbers where the integer preceded a larger value.
        Collection<Integer> numbers = Arrays.asList(10, 1, 15, 30, 2, 6);
        List<Integer> res = StreamEx.of(numbers).pairMap((a, b) -> a < b ? a : null).nonNull().toList();
        assertEquals(Arrays.asList(1, 15, 2), res);

        // Check whether stream is sorted
        assertTrue(isSorted(Arrays.asList("a", "bb", "bb", "c")));
        assertFalse(isSorted(Arrays.asList("a", "bb", "bb", "bba", "bb", "c")));
        assertTrue(isSorted(IntStreamEx.of(new Random(1)).boxed().distinct().limit(1000).toCollection(TreeSet::new)));

        // Find first element which violates the sorting
        assertEquals("bba", firstMisplaced(Arrays.asList("a", "bb", "bb", "bba", "bb", "c")).get());
        assertFalse(firstMisplaced(Arrays.asList("a", "bb", "bb", "bb", "c")).isPresent());
    }

    @Test
    public void testPairMapCornerCase() {
        for (int i = 0; i < 100; i++) {
            for (StreamExSupplier<Integer> supplier : streamEx(() -> IntStreamEx.range(1000)
                    .filter(x -> x == 0 || x == 999).boxed())) {
                assertEquals(supplier.toString(), Collections.singletonList(-999),
                    supplier.get().pairMap((a, b) -> a - b).toList());
            }
        }
    }

    @Test
    public void testPairMapFlatMapBug() {
        Integer[][] input = { { 1 }, { 2, 3 }, { 4, 5, 6 }, { 7, 8 }, { 9 } };
        for (StreamExSupplier<Integer> supplier : streamEx(() -> StreamEx.of(input).<Integer> flatMap(Arrays::stream))) {
            assertEquals(supplier.toString(), 1L, supplier.get().pairMap((a, b) -> b - a).distinct().count());
        }
    }

    private double interpolate(Point[] points, double x) {
        return StreamEx.of(points).parallel()
                .pairMap((p1, p2) -> p1.x <= x && p2.x >= x ? (x - p1.x) / (p2.x - p1.x) * (p2.y - p1.y) + p1.y : null)
                .nonNull().findAny().orElse(Double.NaN);
    }

    @Test
    public void testPairMapInterpolation() {
        Point[] points = IntStreamEx.range(1000).mapToObj(i -> new Point(i, i % 2 == 0 ? 1 : 0)).toArray(Point[]::new);
        assertEquals(1, interpolate(points, 10), 0.0);
        assertEquals(0, interpolate(points, 999), 0.0);
        assertTrue(Double.isNaN(interpolate(points, -10)));
        assertEquals(0.4, interpolate(points, 100.6), 0.000001);
    }

    @Test
    public void testScanLeftPairMap() {
        int[] random = IntStreamEx.of(new Random(1), 1000).toArray();
        List<Integer> scanLeft = IntStreamEx.of(random).boxed().parallel().scanLeft(0, Integer::sum);
        assertArrayEquals(random, IntStreamEx.of(scanLeft).parallel().pairMap((a, b) -> (b - a)).toArray());
    }

    @Test
    public void testPairMapCapitalization() {
        assertEquals(
            "Test Capitalization Stream",
            IntStreamEx
                    .ofChars("test caPiTaliZation streaM")
                    .parallel()
                    .prepend(0)
                    .mapToObj(c -> Character.valueOf((char) c))
                    .pairMap(
                        (c1, c2) -> !Character.isLetter(c1) && Character.isLetter(c2) ? Character.toTitleCase(c2)
                                : Character.toLowerCase(c2)).joining());
    }

    @Test
    public void testPairMapAddHeaders() {
        List<String> result = StreamEx
                .of("aaa", "abc", "bar", "foo", "baz", "argh")
                .sorted()
                .prepend("")
                .pairMap(
                    (a, b) -> a.isEmpty() || a.charAt(0) != b.charAt(0) ? Stream.of("=== "
                        + b.substring(0, 1).toUpperCase() + " ===", b) : Stream.of(b)).flatMap(Function.identity())
                .toList();
        List<String> expected = Arrays.asList("=== A ===", "aaa", "abc", "argh", "=== B ===", "bar", "baz",
            "=== F ===", "foo");
        assertEquals(expected, result);
    }

    static class Node {
        Node parent;
        String name;

        public Node(String name) {
            this.name = name;
        }

        public void link(Node parent) {
            this.parent = parent;
        }

        @Override
        public String toString() {
            return parent == null ? name : parent + ":" + name;
        }
    }

    @Test
    public void testForPairs() {
        List<Node> nodes = StreamEx.of("one", "two", "three", "four").map(Node::new).toList();
        StreamEx.of(nodes).forPairs(Node::link);
        assertEquals("four:three:two:one", nodes.get(0).toString());
        nodes = StreamEx.of("one", "two", "three", "four").map(Node::new).toList();
        StreamEx.of(nodes).parallel().forPairs(Node::link);
        assertEquals("four:three:two:one", nodes.get(0).toString());
    }

    @Test
    public void testScanLeft() {
        for (StreamExSupplier<Integer> supplier : streamEx(() -> IntStreamEx.rangeClosed(1, 4).boxed())) {
            assertEquals(supplier.toString(), Arrays.asList(0, 1, 3, 6, 10), supplier.get().scanLeft(0, Integer::sum));
            assertEquals(supplier.toString(), Arrays.asList(1, 3, 6, 10), supplier.get().scanLeft(Integer::sum));
        }
        for (StreamExSupplier<Integer> supplier : emptyStreamEx(Integer.class)) {
            assertTrue(supplier.toString(), supplier.get().scanLeft(Integer::sum).isEmpty());
            assertEquals(supplier.toString(), Arrays.asList(0), supplier.get().scanLeft(0, Integer::sum));
        }
        assertEquals(167167000, IntStreamEx.rangeClosed(1, 1000).boxed().parallel().scanLeft(0, Integer::sum).stream()
                .mapToLong(x -> x).sum());
    }

    @Test
    public void testScanRight() {
        for (StreamExSupplier<Integer> supplier : streamEx(() -> IntStreamEx.rangeClosed(1, 4).boxed())) {
            assertEquals(supplier.toString(), Arrays.asList(10, 9, 7, 4, 0), supplier.get().scanRight(0, Integer::sum));
            assertEquals(supplier.toString(), Arrays.asList(10, 9, 7, 4), supplier.get().scanRight(Integer::sum));
        }
        for (StreamExSupplier<Integer> supplier : emptyStreamEx(Integer.class)) {
            assertTrue(supplier.toString(), supplier.get().scanRight(Integer::sum).isEmpty());
            assertEquals(supplier.toString(), Arrays.asList(0), supplier.get().scanRight(0, Integer::sum));
        }
        assertEquals(333833500, IntStreamEx.rangeClosed(1, 1000).boxed().parallel().scanRight(0, Integer::sum).stream()
                .mapToLong(x -> x).sum());
    }

    @Test
    public void testPermutations() {
        assertEquals("[]", StreamEx.ofPermutations(0).map(Arrays::toString).joining(";"));
        assertEquals("[0, 1, 2];[0, 2, 1];[1, 0, 2];[1, 2, 0];[2, 0, 1];[2, 1, 0]",
            StreamEx.ofPermutations(3).map(Arrays::toString).joining(";"));
        assertEquals(720, StreamEx.ofPermutations(7).parallel().filter(i -> i[3] == 5).count());
    }

    static class TreeNode {
        String title;

        public TreeNode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }

        public StreamEx<TreeNode> flatStream() {
            return StreamEx.ofTree(this, CompositeNode.class, CompositeNode::elements);
        }
    }

    static class CompositeNode extends TreeNode {
        List<TreeNode> nodes = new ArrayList<>();

        public CompositeNode(String title) {
            super(title);
        }

        public CompositeNode add(TreeNode node) {
            nodes.add(node);
            return this;
        }

        public Stream<TreeNode> elements() {
            return nodes.stream();
        }
    }

    @Test
    public void testOfTree() {
        String inputSimple = "bbb";
        List<Object> input = Arrays.asList(
            "aa",
            null,
            Arrays.asList(Arrays.asList("bbbb", "cc", null, Arrays.asList()), "ddd", Arrays.asList("e"),
                Arrays.asList("fff")), "ggg");
        assertEquals("bbb", StreamEx.ofTree(inputSimple, List.class, List::stream).select(String.class).joining(","));
        StreamEx<Object> ofTree = StreamEx.ofTree(input, List.class, List::stream);
        assertEquals("aa,bbbb,cc,ddd,e,fff,ggg", ofTree.select(String.class).joining(","));
        assertEquals(14, StreamEx.ofTree(input, List.class, List::stream).select(List.class).mapToInt(List::size).sum());

        CompositeNode r = new CompositeNode("root");
        r.add(new CompositeNode("childA").add(new TreeNode("grandA1")).add(new TreeNode("grandA2")));
        r.add(new CompositeNode("childB").add(new TreeNode("grandB1")));
        r.add(new TreeNode("childC"));
        assertEquals("root,childA,grandA1,grandA2,childB,grandB1,childC", r.flatStream().joining(","));
        assertEquals("root,childA,grandA1,grandA2,childB,grandB1,childC", r.flatStream().parallel().joining(","));
    }

    @Test
    public void testCross() {
        assertEquals("a-1, a-2, a-3, b-1, b-2, b-3, c-1, c-2, c-3", StreamEx.of("a", "b", "c").cross(1, 2, 3).join("-")
                .joining(", "));
        assertEquals("a-1, b-1, c-1", StreamEx.of("a", "b", "c").cross(1).join("-").joining(", "));
        assertEquals("", StreamEx.of("a", "b", "c").cross().join("-").joining(", "));
        List<String> inputs = Arrays.asList("i", "j", "k");
        List<String> outputs = Arrays.asList("x", "y", "z");
        assertEquals("i->x, i->y, i->z, j->x, j->y, j->z, k->x, k->y, k->z", StreamEx.of(inputs).cross(outputs)
                .mapKeyValue((input, output) -> input + "->" + output).joining(", "));
        assertEquals("", StreamEx.of(inputs).cross(Collections.emptyList()).join("->").joining(", "));
        assertEquals("i-i, j-j, k-k", StreamEx.of(inputs).cross(Stream::of).join("-").joining(", "));
        assertEquals("j-j, k-k",
            StreamEx.of(inputs).cross(x -> x.equals("i") ? null : Stream.of(x)).join("-").joining(", "));
    }

    @Test
    public void testCollapse() {
        for (StreamExSupplier<Integer> supplier : streamEx(() -> StreamEx.constant(1, 1000))) {
            assertEquals(supplier.toString(), Collections.singletonList(1), supplier.get().collapse(Objects::equals)
                    .toList());
        }
    }

    @Test
    public void testCollapseEmptyLines() {
        Random r = new Random(1);
        for (int i = 0; i < 100; i++) {
            List<String> input = IntStreamEx.range(r.nextInt(i + 1))
                    .mapToObj(n -> r.nextBoolean() ? "" : String.valueOf(n)).toList();
            List<String> resultSpliterator = StreamEx.of(input)
                    .collapse((str1, str2) -> str1.isEmpty() && str2.isEmpty()).toList();
            List<String> resultSpliteratorParallel = StreamEx.of(input).parallel()
                    .collapse((str1, str2) -> str1.isEmpty() && str2.isEmpty()).toList();
            List<String> expected = new ArrayList<>();
            boolean lastSpace = false;
            for (String str : input) {
                if (str.isEmpty()) {
                    if (!lastSpace) {
                        expected.add(str);
                    }
                    lastSpace = true;
                } else {
                    expected.add(str);
                    lastSpace = false;
                }
            }
            assertEquals("#" + i, expected, resultSpliterator);
            assertEquals("#" + i, expected, resultSpliteratorParallel);
        }
    }

    static class Interval {
        final int from, to;

        public Interval(int from) {
            this(from, from);
        }

        public Interval(int from, int to) {
            this.from = from;
            this.to = to;
        }

        public Interval merge(Interval other) {
            return new Interval(this.from, other.to);
        }

        public boolean adjacent(Interval other) {
            return other.from == this.to + 1;
        }

        @Override
        public String toString() {
            return from == to ? "{" + from + "}" : "[" + from + ".." + to + "]";
        }
    }

    @Test
    public void testCollapseIntervals() {
        Random r = new Random(1);
        for (int i = 0; i < 100; i++) {
            int size = r.nextInt(i * 5 + 1);
            int[] input = IntStreamEx.of(r, size, 0, size * 3 / 2 + 2).toArray();
            String result = IntStreamEx.of(input).sorted().boxed().distinct().map(Interval::new)
                    .collapse(Interval::adjacent, Interval::merge).joining(" & ");
            String resultIntervalMap = IntStreamEx.of(input).sorted().boxed().distinct()
                    .intervalMap((a, b) -> b - a == 1, Interval::new).joining(" & ");
            String resultIntervalMapParallel = IntStreamEx.of(input).sorted().boxed().distinct()
                    .intervalMap((a, b) -> b - a == 1, Interval::new).parallel().joining(" & ");
            String resultParallel = IntStreamEx.of(input).parallel().sorted().boxed().distinct().map(Interval::new)
                    .collapse(Interval::adjacent, Interval::merge).joining(" & ");
            String resultParallel2 = IntStreamEx.of(input).sorted().boxed().distinct().map(Interval::new)
                    .collapse(Interval::adjacent, Interval::merge).parallel().joining(" & ");
            String resultCollector = IntStreamEx.of(input).sorted().boxed().distinct().map(Interval::new)
                    .collapse(Interval::adjacent, Collectors.reducing(Interval::merge)).map(Optional::get).parallel()
                    .joining(" & ");
            int[] sorted = Arrays.copyOf(input, input.length);
            Arrays.sort(sorted);
            List<String> expected = new ArrayList<>();
            Interval last = null;
            for (int num : sorted) {
                if (last != null) {
                    if (last.to == num)
                        continue;
                    if (last.to == num - 1) {
                        last = new Interval(last.from, num);
                        continue;
                    }
                    expected.add(last.toString());
                }
                last = new Interval(num);
            }
            if (last != null)
                expected.add(last.toString());
            String expectedStr = String.join(" & ", expected);
            assertEquals("#" + i, expectedStr, result);
            assertEquals("#" + i, expectedStr, resultParallel);
            assertEquals("#" + i, expectedStr, resultParallel2);
            assertEquals("#" + i, expectedStr, resultIntervalMap);
            assertEquals("#" + i, expectedStr, resultIntervalMapParallel);
            assertEquals("#" + i, expectedStr, resultCollector);
        }
    }

    @Test
    public void testCollapseDistinct() {
        Random r = new Random(1);
        for (int i = 0; i < 100; i++) {
            int size = r.nextInt(i * 5 + 1);
            List<Integer> input = IntStreamEx.of(r, size, 0, size * 3 / 2 + 2).boxed().sorted().toList();
            List<Integer> distinct = StreamEx.of(input).collapse(Integer::equals).toList();
            List<Integer> distinctCollector = StreamEx.of(input).collapse(Integer::equals, MoreCollectors.first())
                    .map(Optional::get).toList();
            List<Integer> distinctParallel = StreamEx.of(input).parallel().collapse(Integer::equals).toList();
            List<Integer> distinctCollectorParallel = StreamEx.of(input).parallel()
                    .collapse(Integer::equals, MoreCollectors.first()).map(Optional::get).toList();
            List<Integer> expected = input.stream().distinct().collect(Collectors.toList());
            assertEquals(expected, distinct);
            assertEquals(expected, distinctParallel);
            assertEquals(expected, distinctCollector);
            assertEquals(expected, distinctCollectorParallel);
        }
    }

    @Test
    public void testCollapsePairMap() {
        int[] input = { 0, 0, 1, 1, 1, 1, 4, 6, 6, 3, 3, 10 };
        List<Integer> expected = IntStreamEx.of(input).pairMap((a, b) -> b - a).without(0).boxed().toList();
        for (StreamExSupplier<Integer> supplier : streamEx(() -> IntStreamEx.of(input).boxed())) {
            assertEquals(supplier.toString(), expected,
                supplier.get().collapse(Integer::equals).pairMap((a, b) -> b - a).toList());
        }
    }

    static StreamEx<String> sentences(StreamEx<String> source) {
        return source.flatMap(Pattern.compile("(?<=\\.)")::splitAsStream)
                .collapse((a, b) -> !a.endsWith("."), (a, b) -> a + ' ' + b).map(String::trim);
    }

    @Test
    public void testStreamOfSentences() {
        List<String> lines = Arrays.asList("This is the", "first sentence.  This is the",
            "second sentence. Third sentence. Fourth", "sentence. Fifth sentence.", "The last");
        for (StreamExSupplier<String> supplier : streamEx(lines::stream)) {
            assertEquals(supplier.toString(), Arrays.asList("This is the first sentence.",
                "This is the second sentence.", "Third sentence.", "Fourth sentence.", "Fifth sentence.", "The last"),
                sentences(supplier.get()).toList());
        }
    }

    @Test
    public void testCollapseCollector() {
        List<String> input = Arrays.asList("aaa", "bb", "baz", "bar", "foo", "fee", "abc");
        for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
            assertEquals(supplier.toString(), Arrays.asList("aaa", "bb:baz:bar", "foo:fee", "abc"), supplier.get()
                    .collapse((a, b) -> a.charAt(0) == b.charAt(0), Collectors.joining(":")).toList());
        }
    }

    @Test
    public void testLongestSeries() {
        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 2, 2, 2, 3, 4, 4, 4, 4, 4, 2);
        for (StreamExSupplier<Integer> supplier : streamEx(input::stream)) {
            assertEquals(supplier.toString(), 5L, (long) supplier.get().collapse(Object::equals, Collectors.counting())
                    .maxBy(Function.identity()).get());
        }
    }

    @Test
    public void testGroupRuns() {
        List<String> input = Arrays.asList("aaa", "bb", "baz", "bar", "foo", "fee", "abc");
        List<List<String>> expected = Arrays.asList(Arrays.asList("aaa"), Arrays.asList("bb", "baz", "bar"),
            Arrays.asList("foo", "fee"), Arrays.asList("abc"));
        for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
            assertEquals(supplier.toString(), expected, supplier.get().groupRuns((a, b) -> a.charAt(0) == b.charAt(0))
                    .toList());
            assertEquals(supplier.toString(), expected,
                supplier.get().collapse((a, b) -> a.charAt(0) == b.charAt(0), Collectors.toList()).toList());
        }
    }

    @Test
    public void testGroupRunsRandom() {
        Random r = new Random(1);
        List<Integer> input = IntStreamEx.of(r, 1000, 1, 100).sorted().boxed().toList();
        List<List<Integer>> res1 = StreamEx.of(input).groupRuns(Integer::equals).toList();
        List<List<Integer>> res1p = StreamEx.of(input).parallel().groupRuns(Integer::equals).toList();
        List<List<Integer>> expected = new ArrayList<>();
        List<Integer> last = null;
        for (Integer num : input) {
            if (last != null) {
                if (last.get(last.size() - 1).equals(num)) {
                    last.add(num);
                    continue;
                }
                expected.add(last);
            }
            last = new ArrayList<>();
            last.add(num);
        }
        if (last != null)
            expected.add(last);
        assertEquals(expected, res1);
        assertEquals(expected, res1p);
    }

    @Test
    public void testGroupRunsSeparated() {
        List<String> input = Arrays.asList("a", "b", null, "c", null, "d", "e");
        for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
            assertEquals(supplier.toString(),
                Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c"), Arrays.asList("d", "e")), supplier.get()
                        .groupRuns((a, b) -> a != null && b != null).remove(list -> list.get(0) == null).toList());
        }
    }

    @Test
    public void testGroupRunsByStart() {
        List<String> input = Arrays.asList("str1", "str2", "START: str3", "str4", "START: str5", "START: str6",
            "START: str7", "str8", "str9");
        Pattern start = Pattern.compile("^START:");
        for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
            assertEquals(
                supplier.toString(),
                Arrays.asList(Arrays.asList("str1", "str2"), Arrays.asList("START: str3", "str4"),
                    Arrays.asList("START: str5"), Arrays.asList("START: str6"),
                    Arrays.asList("START: str7", "str8", "str9")),
                supplier.get().groupRuns((a, b) -> !start.matcher(b).find()).toList());
        }
    }

    private String format(StreamEx<Integer> ints) {
        return ints
                .distinct()
                .sorted()
                .<String> intervalMap((i, j) -> j == i + 1,
                    (i, j) -> j.equals(i) ? i.toString() : j == i + 1 ? i + "," + j : i + ".." + j).joining(",");
    }

    private String formatNaive(int[] input) {
        StringBuilder msg = new StringBuilder();
        int[] data = IntStreamEx.of(input).sorted().distinct().toArray();
        int endNum;
        for (int i = 0; i < data.length; i++) {
            endNum = -1;
            for (int j = i + 1; j < data.length && (data[j] - data[j - 1] == 1); j++)
                endNum = j;

            if (msg.length() > 0)
                msg.append(',');
            msg.append(data[i]);
            if (endNum != -1 && (endNum - i) > 1) {
                msg.append("..").append(data[endNum]);
                i = endNum;
            }
        }
        return msg.toString();
    }

    @Test
    public void testIntervalMapString() {
        int[] input = { 1, 5, 2, 10, 8, 11, 7, 15, 6, 5 };
        String expected = formatNaive(input);
        assertEquals(expected, format(IntStreamEx.of(input).boxed()));
        for (int i = 0; i < 100; i++)
            assertEquals(expected, format(IntStreamEx.of(input).boxed().parallel()));

        input = IntStreamEx.range(3, 100).prepend(1).toArray();
        assertEquals("1,3..99", format(IntStreamEx.of(input).boxed()));
        for (int i = 0; i < 100; i++)
            assertEquals("1,3..99", format(IntStreamEx.of(input).boxed().parallel()));

        input = IntStreamEx.of(new Random(1), 1000, 0, 2000).toArray();
        expected = formatNaive(input);
        assertEquals(expected, format(IntStreamEx.of(input).boxed()));
        assertEquals(expected, format(IntStreamEx.of(input).boxed().parallel()));
    }

    @Test
    public void testRunLenghts() {
        Integer[] input = { 1, 2, 2, 4, 2, null, null, 1, 1, 1, null, null };
        String res = StreamEx.of(input).runLengths().join(": ").joining(", ");
        String resParallel = StreamEx.of(input).parallel().runLengths().join(": ").joining(", ");
        assertEquals("1: 1, 2: 2, 4: 1, 2: 1, null: 2, 1: 3, null: 2", res);
        assertEquals("1: 1, 2: 2, 4: 1, 2: 1, null: 2, 1: 3, null: 2", resParallel);
        assertEquals("1=1, 2=2, 4=1, 2=1, null=2, 1=3",
            StreamEx.of(input).parallel().runLengths().distinct().map(String::valueOf).joining(", "));
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void testRunLengthsModify() {
        StreamEx.of("1", "1", "1").runLengths().forEach(e -> e.setValue(5L));
    }

    @Test
    public void testRunLengthsSorted() {
        int[] input = IntStreamEx.of(new Random(1), 1000, 1, 20).sorted().toArray();
        Map<Integer, Long> expected = new HashMap<>();
        long len = 1;
        for (int i = 0; i < input.length - 1; i++) {
            if (input[i] == input[i + 1]) {
                len++;
            } else {
                expected.put(input[i], len);
                len = 1;
            }
        }
        expected.put(input[input.length - 1], len);
        assertEquals(expected, IntStreamEx.of(input).sorted().boxed().runLengths().toMap());
        assertEquals(expected, IntStreamEx.of(input).parallel().sorted().boxed().runLengths().toMap());
    }

    /*
     * Returns longest input stream segment for which the predicate holds (like
     * the corresponding Scala method)
     */
    private long segmentLength(IntStreamEx source, IntPredicate predicate) {
        return source.mapToObj(predicate::test).runLengths().removeKeys(Boolean.FALSE::equals)
                .mapToLong(Entry::getValue).max().orElse(0);
    }

    @Test
    public void testSegmentLength() {
        int[] input = IntStreamEx.of(new Random(1), 1000, -10, 100).toArray();
        // get maximal count of consecutive positive numbers
        long res = segmentLength(IntStreamEx.of(input), x -> x > 0);
        long resParallel = segmentLength(IntStreamEx.of(input).parallel(), x -> x > 0);
        long expected = 0;
        long cur = 0;
        for (int i = 0; i < input.length - 1; i++) {
            if (input[i] > 0 && input[i + 1] > 0)
                cur++;
            else {
                if (cur > expected)
                    expected = cur;
                cur = 1;
            }
        }
        assertEquals(expected, res);
        assertEquals(expected, resParallel);
    }

    private static final class SeqList extends AbstractList<Integer> {
        final int size;

        SeqList(int size) {
            this.size = size;
        }

        @Override
        public Integer get(int index) {
            return index;
        }

        @Override
        public int size() {
            return size;
        }
    }

    @Test
    public void testSubLists() {
        List<Integer> input = IntStreamEx.range(12).boxed().toList();
        assertEquals("[0, 1, 2, 3, 4]-[5, 6, 7, 8, 9]-[10, 11]", StreamEx.ofSubLists(input, 5).joining("-"));
        assertEquals("[0, 1, 2, 3]-[4, 5, 6, 7]-[8, 9, 10, 11]", StreamEx.ofSubLists(input, 4).joining("-"));
        assertEquals("[0]-[1]-[2]-[3]-[4]-[5]-[6]-[7]-[8]-[9]-[10]-[11]", StreamEx.ofSubLists(input, 1).joining("-"));
        assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]", StreamEx.ofSubLists(input, 12).joining("-"));
        assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]",
            StreamEx.ofSubLists(input, Integer.MAX_VALUE).joining("-"));
        assertEquals("", StreamEx.ofSubLists(Collections.emptyList(), 1).joining("-"));
        assertEquals("", StreamEx.ofSubLists(Collections.emptyList(), Integer.MAX_VALUE).joining("-"));

        List<Integer> myList = new SeqList(Integer.MAX_VALUE - 2);
        assertEquals(1, StreamEx.ofSubLists(myList, Integer.MAX_VALUE - 1).count());
        assertEquals(Integer.MAX_VALUE - 2, StreamEx.ofSubLists(myList, Integer.MAX_VALUE - 1).findFirst().get().size());
        assertEquals(1, StreamEx.ofSubLists(myList, Integer.MAX_VALUE - 2).count());
        assertEquals(1, StreamEx.ofSubLists(myList, Integer.MAX_VALUE - 3).skip(1).findFirst().get().size());
    }

    @Test
    public void testSubListsStep() {
        List<Integer> input = IntStreamEx.range(12).boxed().toList();
        assertEquals("[0, 1, 2, 3, 4]", StreamEx.ofSubLists(input, 5, Integer.MAX_VALUE).joining("-"));
        assertEquals("[0, 1, 2, 3, 4]", StreamEx.ofSubLists(input, 5, 12).joining("-"));
        assertEquals("[0, 1, 2, 3, 4]-[11]", StreamEx.ofSubLists(input, 5, 11).joining("-"));
        assertEquals("[0, 1, 2, 3, 4]-[9, 10, 11]", StreamEx.ofSubLists(input, 5, 9).joining("-"));
        assertEquals("[0, 1, 2, 3, 4]-[8, 9, 10, 11]", StreamEx.ofSubLists(input, 5, 8).joining("-"));
        assertEquals("[0, 1, 2, 3, 4]-[7, 8, 9, 10, 11]", StreamEx.ofSubLists(input, 5, 7).joining("-"));
        assertEquals("[0, 1, 2, 3, 4]-[6, 7, 8, 9, 10]", StreamEx.ofSubLists(input, 5, 6).joining("-"));
        assertEquals("[0, 1, 2, 3, 4]-[4, 5, 6, 7, 8]-[8, 9, 10, 11]", StreamEx.ofSubLists(input, 5, 4).joining("-"));
        assertEquals("[0, 1, 2, 3, 4]-[3, 4, 5, 6, 7]-[6, 7, 8, 9, 10]-[9, 10, 11]", StreamEx.ofSubLists(input, 5, 3)
                .joining("-"));
        assertEquals("[0, 1, 2, 3, 4]-[2, 3, 4, 5, 6]-[4, 5, 6, 7, 8]-[6, 7, 8, 9, 10]-[8, 9, 10, 11]", StreamEx
                .ofSubLists(input, 5, 2).joining("-"));
        assertEquals("[0, 1, 2, 3, 4]-[1, 2, 3, 4, 5]-[2, 3, 4, 5, 6]-[3, 4, 5, 6, 7]-"
            + "[4, 5, 6, 7, 8]-[5, 6, 7, 8, 9]-[6, 7, 8, 9, 10]-[7, 8, 9, 10, 11]", StreamEx.ofSubLists(input, 5, 1)
                .joining("-"));

        List<Integer> myList = new SeqList(Integer.MAX_VALUE - 2);
        assertEquals(1, StreamEx.ofSubLists(myList, Integer.MAX_VALUE - 1, 1).count());
        assertEquals("[0]", StreamEx.ofSubLists(myList, 1, Integer.MAX_VALUE - 1).joining());
        assertEquals("[0]", StreamEx.ofSubLists(myList, 1, Integer.MAX_VALUE - 2).joining());
        assertEquals("[0][2147483644]", StreamEx.ofSubLists(myList, 1, Integer.MAX_VALUE - 3).joining());
        assertEquals("[0, 1]", StreamEx.ofSubLists(myList, 2, Integer.MAX_VALUE - 1).joining());
        assertEquals("[0, 1]", StreamEx.ofSubLists(myList, 2, Integer.MAX_VALUE - 2).joining());
        assertEquals("[0, 1][2147483644]", StreamEx.ofSubLists(myList, 2, Integer.MAX_VALUE - 3).joining());
        assertEquals(Integer.MAX_VALUE - 2, StreamEx.ofSubLists(myList, Integer.MAX_VALUE - 1, 1).findFirst().get()
                .size());
        assertEquals(1, StreamEx.ofSubLists(myList, Integer.MAX_VALUE - 2, 1).count());
        assertEquals(998, StreamEx.ofSubLists(myList, Integer.MAX_VALUE - 3, Integer.MAX_VALUE - 1000).skip(1)
                .findFirst().get().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubListsArg() {
        StreamEx.ofSubLists(Collections.emptyList(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubListsStepArg() {
        StreamEx.ofSubLists(Collections.emptyList(), 1, 0);
    }

    @Test
    public void testTakeWhile() {
        assertEquals(Arrays.asList("aaa"), StreamEx.of("aaa", "b", "cccc").takeWhile(x -> x.length() > 1).toList());
        assertEquals(Arrays.asList("aaa"), StreamEx.of("aaa", "b", "cccc").sorted().takeWhile(x -> x.length() > 1)
                .toList());
        assertEquals(Arrays.asList("aaa", "b", "cccc"), StreamEx.of("aaa", "b", "cccc").takeWhile(x -> x.length() > 0)
                .toList());
        assertEquals(Collections.emptyList(), StreamEx.of("aaa", "b", "cccc").takeWhile(x -> x.length() > 5).toList());
    }

    @Test
    public void testDropWhile() {
        assertEquals(Arrays.asList("b", "cccc"), StreamEx.of("aaa", "b", "cccc").dropWhile(x -> x.length() > 1)
                .toList());
        assertEquals(Collections.emptyList(), StreamEx.of("aaa", "b", "cccc").dropWhile(x -> x.length() > 0).toList());
        assertEquals(Arrays.asList("aaa", "b", "cccc"), StreamEx.of("aaa", "b", "cccc").dropWhile(x -> x.length() > 5)
                .toList());

        // When testing with JDK9, "dropWhile" must redirect the call to JDK 9
        // dropWhile which propagates the "parallel" mode.
        // When testing with JDK8, "dropWhile" is quasi-intermediate and
        // "parallel" mode is not propagated.
        boolean propagate = true;
        try {
            Stream.class.getDeclaredMethod("dropWhile", Predicate.class);
        } catch (NoSuchMethodException e) {
            propagate = false;
        }
        StreamEx<String> input = StreamEx.of("aaa", "b", "cccc");
        input.dropWhile(x -> x.length() > 1).parallel();
        assertEquals(propagate, input.isParallel());
    }

    @Test
    public void testOfPairs() {
        Random r = new Random(1);
        Point[] pts = StreamEx.generate(() -> new Point(r.nextDouble(), r.nextDouble())).limit(100)
                .toArray(Point[]::new);
        double expected = StreamEx.of(pts).cross(pts).mapKeyValue(Point::distance).mapToDouble(Double::doubleValue)
                .max().getAsDouble();
        for (StreamExSupplier<Double> supplier : streamEx(() -> StreamEx.ofPairs(pts, Point::distance))) {
            assertEquals(supplier.toString(), expected, supplier.get().mapToDouble(Double::doubleValue).max()
                    .getAsDouble(), 0.0);
        }
    }

    @Test
    public void testToFlatCollection() {
        List<List<String>> strings = IntStreamEx.range(100).mapToObj(String::valueOf)
                .groupRuns((a, b) -> a.charAt(0) == b.charAt(0)).toList();
        Set<String> expected = IntStreamEx.range(100).mapToObj(String::valueOf).toSet();
        List<String> expectedList = IntStreamEx.range(100).mapToObj(String::valueOf).toList();
        for (StreamExSupplier<List<String>> supplier : streamEx(strings::stream)) {
            assertEquals(supplier.toString(), expected,
                supplier.get().toFlatCollection(Function.identity(), HashSet::new));
            assertEquals(supplier.toString(), expectedList, supplier.get().toFlatList(Function.identity()));
        }
    }

    @Test
    public void testCartesian() {
        List<List<Integer>> expected = IntStreamEx.range(32)
                .mapToObj(i -> IntStreamEx.range(5).mapToObj(n -> (i >> (4 - n)) & 1).toList()).toList();
        for (StreamExSupplier<List<Integer>> supplier : streamEx(() -> StreamEx.cartesianPower(5, Arrays.asList(0, 1)))) {
            assertEquals(supplier.toString(), expected, supplier.get().toList());
        }
        List<List<Integer>> input2 = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(), Arrays.asList(4, 5, 6));
        for (StreamExSupplier<List<Integer>> supplier : streamEx(() -> StreamEx.cartesianProduct(input2))) {
            assertFalse(supplier.toString(), supplier.get().findAny().isPresent());
        }
        List<List<Integer>> input3 = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3), Arrays.asList(4, 5));
        for (StreamExSupplier<List<Integer>> supplier : streamEx(() -> StreamEx.cartesianProduct(input3))) {
            assertEquals(supplier.toString(), "[1, 3, 4],[1, 3, 5],[2, 3, 4],[2, 3, 5]", supplier.get().joining(","));
        }
        Set<Integer> input4 = IntStreamEx.range(10).boxed().toCollection(TreeSet::new);
        for (StreamExSupplier<List<Integer>> supplier : streamEx(() -> StreamEx.cartesianPower(3, input4))) {
            assertEquals(supplier.toString(), IntStreamEx.range(1000).boxed().toList(),
                supplier.get().map(list -> list.get(0) * 100 + list.get(1) * 10 + list.get(2)).toList());
        }
        assertEquals(Arrays.asList(Collections.emptyList()), StreamEx.cartesianProduct(Collections.emptyList())
                .toList());
        assertEquals(Arrays.asList(Collections.emptyList()), StreamEx.cartesianPower(0, Arrays.asList(1, 2, 3))
                .toList());
    }

    @Test
    public void testCartesianReduce() {
        List<String> expected = IntStreamEx.range(32)
                .mapToObj(i -> IntStreamEx.range(5).mapToObj(n -> (i >> (4 - n)) & 1).joining()).toList();
        for (StreamExSupplier<String> supplier : streamEx(() -> StreamEx.cartesianPower(5, Arrays.asList(0, 1), "", (a,
                b) -> a + b))) {
            assertEquals(supplier.toString(), expected, supplier.get().toList());
        }
        List<List<Integer>> input2 = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(), Arrays.asList(4, 5, 6));
        for (StreamExSupplier<String> supplier : streamEx(() -> StreamEx.cartesianProduct(input2, "", (a, b) -> a + b))) {
            assertFalse(supplier.toString(), supplier.get().findAny().isPresent());
        }
        List<List<Integer>> input3 = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3), Arrays.asList(4, 5));
        for (StreamExSupplier<String> supplier : streamEx(() -> StreamEx.cartesianProduct(input3, "", (a, b) -> a + b))) {
            assertEquals(supplier.toString(), "134,135,234,235", supplier.get().joining(","));
        }
        assertEquals(Arrays.asList(""),
            StreamEx.cartesianProduct(Collections.<List<String>> emptyList(), "", String::concat).toList());
        assertEquals(Arrays.asList(""), StreamEx.cartesianPower(0, Arrays.asList(1, 2, 3), "", (a, b) -> a + b)
                .toList());
    }

    @Test
    public void testDistinct() {
        List<String> input = Arrays.asList("str", "a", "foo", "", "bbbb", null, "abcd", "s");
        for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
            assertEquals(supplier.toString(), input, supplier.get().distinct(x -> x).toList());
            assertEquals(supplier.toString(), Arrays.asList("str", "a", "", "bbbb"),
                supplier.get().distinct(x -> x == null ? 0 : x.length()).toList());
        }
    }

    @Test
    public void testIndexOf() {
        List<Integer> input = IntStreamEx.range(100).boxed().toList();
        input.addAll(input);
        AtomicInteger counter = new AtomicInteger();
        assertEquals(10, StreamEx.of(input).peek(t -> counter.incrementAndGet()).indexOf(10).getAsLong());
        assertEquals(11, counter.get());
        for (int i = 0; i < 100; i++) {
            assertEquals(99, StreamEx.of(input).parallel().prepend().peek(t -> counter.incrementAndGet()).indexOf(99)
                    .getAsLong());
        }
        for (StreamExSupplier<Integer> supplier : streamEx(input::stream)) {
            for (int i : new int[] { 0, 1, 10, 50, 78, 99 }) {
                assertEquals(supplier + "/#" + i, i,
                    supplier.get().peek(t -> counter.incrementAndGet()).indexOf(x -> x == i).getAsLong());
            }
            assertFalse(supplier.toString(), supplier.get().indexOf(""::equals).isPresent());
        }
    }
    
    @Test
    public void testIndexOfSimple() {
        List<Integer> input = IntStreamEx.range(10).boxed().toList();
        for (int i = 0; i < 100; i++) {
            assertEquals(9, StreamEx.of(input).parallel().indexOf(9).getAsLong());
        }
    }
    
    @Test
    public void testMapFirstLast() {
        for(StreamExSupplier<Integer> s : streamEx(() -> StreamEx.of(0, 343, 999))) {
            assertEquals(s.toString(), Arrays.asList(2, 343, 997), s.get()
                    .mapFirst(x -> x + 2).mapLast(x -> x - 2).toList());
        }
        for(StreamExSupplier<Integer> s : streamEx(() -> IntStreamEx.range(1000).boxed())) {
            assertEquals(s.toString(), Arrays.asList(2, 343, 997), s.get().filter(x -> x == 0 || x == 343 || x == 999)
                .mapFirst(x -> x + 2).mapLast(x -> x - 2).toList());
        }
        for (StreamExSupplier<Integer> s : streamEx(() -> IntStreamEx.range(50).boxed()
                .foldLeft(StreamEx.of(0), (stream, i) -> stream.append(i).mapLast(x -> x + 2)))) {
            assertEquals(s.toString(), IntStreamEx.range(2, 52).boxed().prepend(0).toList(), s.get().toList());
        }
        for (StreamExSupplier<String> s : streamEx(Arrays.asList("red", "green", "blue", "orange")::stream)) {
            assertEquals(s.toString(), "red, green, blue, or orange", s.get().mapLast("or "::concat).joining(", "));
        }
    }
}
