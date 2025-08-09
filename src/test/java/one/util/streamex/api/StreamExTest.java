/*
 * Copyright 2015, 2024 StreamEx contributors
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
package one.util.streamex.api;

import one.util.streamex.*;
import one.util.streamex.TestHelpers.*;
import org.jspecify.annotations.NonNull;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static one.util.streamex.TestHelpers.*;
import static org.junit.Assert.*;

/**
 * @author Tagir Valeev
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StreamExTest {
    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testCreate() {
        assertEquals(asList(), StreamEx.empty().toList());
        // a repeating test is intended: check that empty() is not a singleton and can be called twice
        assertEquals(asList(), StreamEx.empty().toList());
        assertEquals(asList("a"), StreamEx.of("a").toList());
        assertEquals(asList("a"), StreamEx.of(Optional.of("a")).toList());
        assertEquals(asList(), StreamEx.of(Optional.empty()).toList());
        assertEquals(asList(), StreamEx.ofNullable(null).toList());
        assertEquals(asList("a"), StreamEx.ofNullable("a").toList());
        assertEquals(asList((String) null), StreamEx.of((String) null).toList());
        assertEquals(asList("a", "b"), StreamEx.of("a", "b").toList());
        assertEquals(asList("a", "b"), StreamEx.of(asList("a", "b")).toList());
        assertEquals(asList("a", "b"), StreamEx.of(Stream.of("a", "b")).toList());
        assertEquals(asList("a", "b"), StreamEx.split("a,b", ",").toList());
        assertEquals(asList("a", "c", "d"), StreamEx.split("abcBd", Pattern.compile("b", Pattern.CASE_INSENSITIVE))
                .toList());
        assertEquals(asList("a", "b"), StreamEx.ofLines(new StringReader("a\nb")).toList());
        assertEquals(asList("a", "b"), StreamEx.ofLines(new BufferedReader(new StringReader("a\nb"))).toList());
        assertEquals(asList("a", "b"), StreamEx.ofLines(getReader()).toList());
        assertEquals(asList("a", "a", "a", "a"), StreamEx.generate(() -> "a").limit(4).toList());
        assertEquals(asList("a", "a", "a", "a"), StreamEx.constant("a", 4).toList());
        assertEquals(asList("c", "d", "e"), StreamEx.of("abcdef".split(""), 2, 5).toList());

        assertEquals(asList("a1", "b2", "c3"), StreamEx.zip(asList("a", "b", "c"), asList(1, 2, 3), (s, i) -> s + i)
                .toList());
        assertEquals(asList("a1", "b2", "c3"), StreamEx.zip(new String[]{"a", "b", "c"}, new Integer[]{1, 2, 3}, (
                s, i) -> s + i).toList());

        assertEquals(asList("a", "b"), StreamEx.of(asList("a", "b").spliterator()).toList());
        assertEquals(asList("a", "b"), StreamEx.of(asList("a", "b").iterator()).toList());
        assertEquals(asList(), StreamEx.of(Collections.emptyIterator()).toList());
        assertEquals(asList(), StreamEx.of(Collections.emptyIterator()).parallel().toList());
        assertEquals(asList("a", "b"), StreamEx.of(new Vector<>(asList("a", "b")).elements()).toList());

        assertEquals(asList("a", "b", "c", "d"), StreamEx.ofReversed(asList("d", "c", "b", "a")).toList());
        assertEquals(asList("a", "b", "c", "d"), StreamEx.ofReversed(new String[]{"d", "c", "b", "a"}).toList());
    }

    @Test
    public void testIterate() {
        assertEquals(asList("a", "aa", "aaa", "aaaa"), StreamEx.iterate("a", x -> x + "a").limit(4).toList());
        assertEquals(asList("a", "aa", "aaa", "aaaa"), StreamEx.iterate("a", x -> x.length() <= 4, x -> x + "a")
                .toList());
        assertFalse(StreamEx.iterate("a", x -> x.length() <= 10, x -> x + "a").has("b"));
        assertEquals(0, StreamEx.iterate("", x -> !x.isEmpty(), x -> x.substring(1)).count());
        checkSpliterator("iterate", () -> StreamEx.iterate(1, x -> x < 100, x -> x * 2).spliterator());
    }

    @Test
    public void testCreateFromFile() throws IOException {
        File f = tmp.newFile();
        List<String> input = asList("Some", "Test", "Lines");
        Files.write(f.toPath(), input);
        assertEquals(input, StreamEx.ofLines(f.toPath()).toList());
        Files.write(f.toPath(), input, StandardCharsets.UTF_16);
        assertEquals(input, StreamEx.ofLines(f.toPath(), StandardCharsets.UTF_16).toList());
    }

    private static Reader getReader() {
        return new BufferedReader(new StringReader("a\nb"));
    }

    @Test
    public void testReader() {
        String input = IntStreamEx.range(5000).joining("\n");
        List<String> expectedList = IntStreamEx.range(1, 5000).mapToObj(String::valueOf).toList();
        Set<String> expectedSet = IntStreamEx.range(1, 5000).mapToObj(String::valueOf).toSet();
        List<String> actualList = StreamEx.ofLines(new StringReader(input)).skip(1).parallel().toList();
        assertEquals(expectedList, actualList);
        actualList = StreamEx.ofLines(new StringReader(input)).pairMap((a, b) -> b).parallel().toList();
        assertEquals(expectedList, actualList);

        Set<String> actualSet = StreamEx.ofLines(new StringReader(input)).pairMap((a, b) -> b).parallel().toSet();
        assertEquals(expectedSet, actualSet);
        actualSet = StreamEx.ofLines(new StringReader(input)).skip(1).parallel().toCollection(HashSet::new);
        assertEquals(expectedSet, actualSet);
    }

    @Test
    public void testZip() {
        assertThrows(IllegalArgumentException.class, () -> StreamEx.zip(asList("A"), asList("b", "c"), String::concat));
    }

    @Test
    public void testBasics() {
        assertFalse(StreamEx.of("a").isParallel());
        assertTrue(StreamEx.of("a").parallel().isParallel());
        assertFalse(StreamEx.of("a").parallel().sequential().isParallel());
        AtomicInteger i = new AtomicInteger();
        try (Stream<String> s = StreamEx.of("a").onClose(i::incrementAndGet)) {
            assertEquals(1, s.count());
        }
        assertEquals(1, i.get());
        assertEquals(asList(1, 2), StreamEx.of("a", "bb").map(String::length).toList());
        assertFalse(StreamEx.empty().findAny().isPresent());
        assertEquals("a", StreamEx.of("a").findAny().get());
        assertFalse(StreamEx.empty().findFirst().isPresent());
        assertEquals("a", StreamEx.of("a", "b").findFirst().get());
        assertEquals(asList("b", "c"), StreamEx.of("a", "b", "c").skip(1).toList());

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

        assertEquals("abbccc", StreamEx.of("a", "bb", "ccc").collect(StringBuilder::new, StringBuilder::append,
            StringBuilder::append).toString());
        assertArrayEquals(new String[] { "a", "b", "c" }, StreamEx.of("a", "b", "c").toArray(String[]::new));
        assertArrayEquals(new Object[] { "a", "b", "c" }, StreamEx.of("a", "b", "c").toArray());
        assertEquals(3, StreamEx.of("a", "b", "c").spliterator().getExactSizeIfKnown());

        assertTrue(StreamEx.of("a", "b", "c").spliterator().hasCharacteristics(Spliterator.ORDERED));
        assertFalse(StreamEx.of("a", "b", "c").unordered().spliterator().hasCharacteristics(Spliterator.ORDERED));
    }

    @Test
    public void testCovariance() {
        StreamEx<Number> stream = StreamEx.of(1, 2, 3);
        List<Number> list = stream.toList();
        assertEquals(asList(1, 2, 3), list);

        StreamEx<Object> objStream = StreamEx.of(list.spliterator());
        List<Object> objList = objStream.toList();
        assertEquals(asList(1, 2, 3), objList);
    }

    @Test
    public void testToMutableList() {
        List<Integer> list = StreamEx.of(1, 2, 3).toMutableList();
        // Test that the returned list is mutable
        List<Integer> list2 = StreamEx.of(4, 5, 6).parallel().toMutableList();
        list2.add(7);
        list.addAll(list2);
        assertEquals(asList(1, 2, 3, 4, 5, 6, 7), list);
    }

    @Test
    public void testToMutableSet() {
        Set<Integer> set = StreamEx.of(1, 2, 3).toMutableSet();
        set.add(4);
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4)), set);
    }

    @Test
    public void testToArray() {
        Number[] numbers = StreamEx.of(1, 2, 3).toArray(Number.class);
        assertArrayEquals(new Number[] { 1, 2, 3 }, numbers);
        assertEquals(Number.class, numbers.getClass().getComponentType());
        Integer[] emptyArray = {};
        assertSame(emptyArray, StreamEx.of(1, 2, 3).filter(x -> x > 3).toArray(emptyArray));
        assertArrayEquals(new Integer[] { 1, 2, 3 }, StreamEx.of(1, 2, 3).remove(x -> x > 3).toArray(emptyArray));
        assertThrows(IllegalArgumentException.class, () -> StreamEx.of().toArray(new Integer[1]));
    }

    @Test
    public void testForEach() {
        List<Integer> list = new ArrayList<>();
        StreamEx.of(1, 2, 3).forEach(list::add);
        assertEquals(asList(1, 2, 3), list);
        StreamEx.of(1, 2, 3).forEachOrdered(list::add);
        assertEquals(asList(1, 2, 3, 1, 2, 3), list);
        StreamEx.of(1, 2, 3).parallel().forEachOrdered(list::add);
        assertEquals(asList(1, 2, 3, 1, 2, 3, 1, 2, 3), list);
    }

    @Test
    public void testFlatMap() {
        assertArrayEquals(new int[] { 0, 0, 1, 0, 0, 1, 0, 0 }, StreamEx.of("111", "222", "333").flatMapToInt(s -> s
                .chars().map(ch -> ch - '0')).pairMap((a, b) -> b - a).toArray());
        assertArrayEquals(new long[] { 0, 0, 1, 0, 0, 1, 0, 0 }, StreamEx.of("111", "222", "333").flatMapToLong(s -> s
                .chars().mapToLong(ch -> ch - '0')).pairMap((a, b) -> b - a).toArray());
        assertArrayEquals(new double[] { 0, 0, 1, 0, 0, 1, 0, 0 }, StreamEx.of("111", "222", "333").flatMapToDouble(
            s -> s.chars().mapToDouble(ch -> ch - '0')).pairMap((a, b) -> b - a).toArray(), 0.0);
    }

    @Test
    public void testAndThen() {
        HashSet<String> set = StreamEx.of("a", "bb", "ccc").toListAndThen(HashSet::new);
        assertEquals(3, set.size());
        assertTrue(set.contains("bb"));

        ArrayList<String> list = StreamEx.of("a", "bb", "ccc").toSetAndThen(ArrayList::new);
        assertEquals(3, list.size());
        assertTrue(list.contains("bb"));

        ArrayList<String> linkedHashSet = StreamEx.of("a", "bb", "ccc", "a", "d", "bb", "e")
                .toCollectionAndThen(LinkedHashSet::new, ArrayList::new);
        assertEquals(asList("a", "bb", "ccc", "d", "e"), linkedHashSet);
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

        streamEx(() -> Stream.of("a", "bb", "ccc"), supplier -> {
            Map<String, Integer> map = supplier.get().toMap(String::length);
            assertEquals(supplier.get().isParallel(), map instanceof ConcurrentMap);
            assertEquals(expected, map);

            Map<Integer, String> map2 = supplier.get().toMap(String::length, Function.identity());
            assertEquals(supplier.get().isParallel(), map2 instanceof ConcurrentMap);
            assertEquals(expected2, map2);
        });

        Map<Integer, String> expected3 = new HashMap<>();
        expected3.put(1, "a");
        expected3.put(2, "bbdd");
        expected3.put(3, "ccc");
        streamEx(() -> Stream.of("a", "bb", "ccc", "dd"), supplier -> {
            Map<Integer, String> seqMap3 = supplier.get().toMap(String::length, Function.identity(), String::concat);
            assertEquals(expected3, seqMap3);

            checkIllegalStateException(() -> supplier.get().toMap(String::length, Function.identity()), "2", "dd",
                "bb");
        });
    }

    @Test
    public void testValuesToMap() {
        Map<Integer, String> expected = new HashMap<>();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        streamEx(() -> Stream.of("a", "bb", "ccc"), supplier -> {
            Map<Integer, String> map = supplier.get().valuesToMap(String::length);
            assertEquals(supplier.get().isParallel(), map instanceof ConcurrentMap);
            assertEquals(expected, map);

            SortedMap<Integer, String> smap = supplier.get().valuesToSortedMap(String::length);
            assertEquals(supplier.get().isParallel(), smap instanceof ConcurrentMap);
            assertEquals(expected, smap);

            NavigableMap<Integer, String> nmap = supplier.get().valuesToNavigableMap(String::length);
            assertEquals(supplier.get().isParallel(), nmap instanceof ConcurrentMap);
            assertEquals(expected, nmap);
        });
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

        streamEx(() -> Stream.of("a", "bb", "ccc"), supplier -> {
            SortedMap<String, Integer> map = supplier.get().toSortedMap(String::length);
            assertEquals(supplier.get().isParallel(), map instanceof ConcurrentMap);
            assertEquals(expected, map);

            SortedMap<Integer, String> map2 = supplier.get().toSortedMap(String::length, Function.identity());
            assertEquals(supplier.get().isParallel(), map2 instanceof ConcurrentMap);
            assertEquals(expected2, map2);
        });

        Map<Integer, String> expected3 = new HashMap<>();
        expected3.put(1, "a");
        expected3.put(2, "bbdd");
        expected3.put(3, "ccc");
        streamEx(() -> Stream.of("a", "bb", "ccc", "dd"), supplier -> {
            SortedMap<Integer, String> seqMap3 = supplier.get().toSortedMap(String::length, Function.identity(),
                String::concat);
            assertEquals(supplier.toString(), expected3, seqMap3);

            checkIllegalStateException(() -> supplier.get().toSortedMap(String::length, Function.identity()), "2", "dd",
                "bb");
        });
    }

    @Test
    public void testToNavigableMap() {
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("bb", 2);
        expected.put("ccc", 3);

        Map<Integer, String> expected2 = new HashMap<>();
        expected2.put(1, "a");
        expected2.put(2, "bb");
        expected2.put(3, "ccc");

        streamEx(() -> Stream.of("a", "bb", "ccc"), supplier -> {
            NavigableMap<String, Integer> map = supplier.get().toNavigableMap(String::length);
            assertEquals(supplier.get().isParallel(), map instanceof ConcurrentMap);
            assertEquals(expected, map);

            NavigableMap<Integer, String> map2 = supplier.get().toNavigableMap(String::length, Function.identity());
            assertEquals(supplier.get().isParallel(), map2 instanceof ConcurrentMap);
            assertEquals(expected2, map2);
        });

        Map<Integer, String> expected3 = new HashMap<>();
        expected3.put(1, "a");
        expected3.put(2, "bbdd");
        expected3.put(3, "ccc");
        streamEx(() -> Stream.of("a", "bb", "ccc", "dd"), supplier -> {
            NavigableMap<Integer, String> seqMap3 = supplier.get().toNavigableMap(String::length, Function.identity(),
                String::concat);
            assertEquals(supplier.toString(), expected3, seqMap3);

            checkIllegalStateException(() -> supplier.get().toNavigableMap(String::length, Function.identity()), "2",
                "dd", "bb");
        });
    }

    @Test
    public void testGroupingBy() {
        Map<Integer, List<String>> expected = new HashMap<>();
        expected.put(1, asList("a"));
        expected.put(2, asList("bb", "dd"));
        expected.put(3, asList("ccc"));

        Map<Integer, Set<String>> expectedMapSet = new HashMap<>();
        expectedMapSet.put(1, new HashSet<>(asList("a")));
        expectedMapSet.put(2, new HashSet<>(asList("bb", "dd")));
        expectedMapSet.put(3, new HashSet<>(asList("ccc")));

        streamEx(() -> StreamEx.of("a", "bb", "dd", "ccc"), supplier -> {
            assertEquals(expected, supplier.get().groupingBy(String::length));
            Map<Integer, List<String>> map = supplier.get().groupingTo(String::length, LinkedList::new);
            assertEquals(expected, map);
            assertTrue(map.get(1) instanceof LinkedList);
            assertEquals(expectedMapSet, supplier.get().groupingBy(String::length, Collectors.toSet()));
            assertEquals(expectedMapSet, supplier.get().groupingBy(String::length, HashMap::new, Collectors.toSet()));
            ConcurrentHashMap<Integer, Set<String>> chm = supplier.get().groupingBy(String::length,
                ConcurrentHashMap::new, Collectors.toSet());
            assertEquals(expectedMapSet, chm);
            chm = supplier.get().groupingTo(String::length, ConcurrentHashMap::new, TreeSet::new);
            assertTrue(chm.get(1) instanceof TreeSet);
        });
    }

    @Test
    public void testPartitioning() {
        Map<Boolean, List<String>> map = StreamEx.of("a", "bb", "c", "dd").partitioningBy(s -> s.length() > 1);
        assertEquals(asList("bb", "dd"), map.get(true));
        assertEquals(asList("a", "c"), map.get(false));
        Map<Boolean, Long> counts = StreamEx.of("a", "bb", "c", "dd", "eee").partitioningBy(s -> s.length() > 1,
            Collectors.counting());
        assertEquals(3L, (long) counts.get(true));
        assertEquals(2L, (long) counts.get(false));
        Map<Boolean, List<String>> mapLinked = StreamEx.of("a", "bb", "c", "dd").partitioningTo(s -> s.length() > 1,
            LinkedList::new);
        assertEquals(asList("bb", "dd"), mapLinked.get(true));
        assertEquals(asList("a", "c"), mapLinked.get(false));
        assertTrue(mapLinked.get(true) instanceof LinkedList);
    }

    @Test
    public void testIterable() {
        List<String> result = new ArrayList<>();
        for (String s : StreamEx.of("a", "b", "cc").filter(s -> s.length() < 2)) {
            result.add(s);
        }
        assertEquals(asList("a", "b"), result);
    }

    @Test
    public void testCreateFromMap() {
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("aaa", 10);
        data.put("bb", 25);
        data.put("c", 37);
        assertEquals(asList("aaa", "bb", "c"), StreamEx.ofKeys(data).toList());
        assertEquals(asList("aaa"), StreamEx.ofKeys(data, x -> x % 2 == 0).toList());
        assertEquals(asList(10, 25, 37), StreamEx.ofValues(data).toList());
        assertEquals(asList(10, 25), StreamEx.ofValues(data, s -> s.length() > 1).toList());
    }

    @Test
    public void testSelect() {
        assertEquals(asList("a", "b"), StreamEx.of(1, "a", 2, "b", 3, "cc").select(String.class).filter(s -> s
                .length() == 1).toList());
        StringBuilder sb = new StringBuilder();
        StringBuffer sbb = new StringBuffer();
        StreamEx.<CharSequence>of("test", sb, sbb).select(Appendable.class).forEach(a -> {
            try {
                a.append("b");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        assertEquals("b", sb.toString());
        assertEquals("b", sbb.toString());
    }

    @Test
    public void testFlatCollection() {
        Map<Integer, List<String>> data = new LinkedHashMap<>();
        data.put(1, asList("a", "b"));
        data.put(2, asList("c", "d"));
        data.put(3, null);
        assertEquals(asList("a", "b", "c", "d"), StreamEx.of(data.entrySet()).flatCollection(Entry::getValue).toList());
    }

    @Test
    public void testFlatArray() {
        Map<Integer, String[]> data = new LinkedHashMap<>();
        data.put(1, new String[] {"a", "b"});
        data.put(2, new String[] {"c", "d"});
        data.put(3, null);
        assertEquals(asList("a", "b", "c", "d"), StreamEx.of(data.entrySet()).flatArray(Entry::getValue).toList());
    }

    @Test
    public void testMapPartial() {
        Function<Integer, Optional<String>> literalOf = num -> num == 1
                ? Optional.of("one")
                : Optional.empty();

        List<Integer> original = asList(1, 2, 3, 4);
        List<String> expected = asList("one");
        streamEx(original::stream, s -> assertEquals(expected, s.get().mapPartial(literalOf).toList()));
    }

    @Test
    public void testAppend() {
        assertEquals(asList("a", "b", "c", "d", "e"), StreamEx.of("a", "b", "c", "dd").remove(s -> s.length() > 1)
                .append("d", "e").toList());
        assertEquals(asList("a", "b", "c", "d", "e"), StreamEx.of("a", "b", "c").append(Stream.of("d", "e"))
                .toList());
        assertEquals(asList("a", "b", "c", "d", "e"), StreamEx.of("a", "b", "c").append(asList("d", "e")).toList());

        List<Integer> list = asList(1, 2, 3, 4);
        assertEquals(asList(1.0, 2, 3L, 1, 2, 3, 4), StreamEx.of(1.0, 2, 3L).append(list).toList());

        StreamEx<Integer> s = StreamEx.of(1, 2, 3);
        assertSame(s, s.append());
        assertSame(s, s.append(Collections.emptyList()));
        assertSame(s, s.append(new ArrayList<>()));
        assertSame(s, s.append(Stream.empty()));
        assertNotSame(s, s.append(new ConcurrentLinkedQueue<>()));
    }

    @Test
    public void testPrepend() {
        Supplier<Stream<String>> sized = () -> StreamEx.of("a", "b", "c", "dd");
        Supplier<Stream<String>> notSized = () -> StreamEx.of(StreamEx.of("a", "b", "c", "dd").iterator());
        for (Supplier<Stream<String>> supplier : asList(sized, notSized)) {
            streamEx(supplier, s -> {
                assertEquals(asList("d", "e", "a", "b", "c"), s.get().remove(str -> str.length() > 1).prepend("d", "e")
                        .toList());
                assertEquals(asList("d", "e", "a", "b", "c", "dd"), s.get().prepend(Stream.of("d", "e"))
                        .toList());
                assertEquals(asList("d", "e", "a", "b", "c", "dd"), s.get().prepend(asList("d", "e")).toList());
            });
        }
        assertArrayEquals(new Object[] { 1, 2, 3, 1, 1 }, StreamEx.constant(1, Long.MAX_VALUE - 1).prepend(1, 2, 3)
                .limit(5).toArray());

        assertEquals(asList(4, 3, 2, 1), StreamEx.of(1).prepend(2).prepend(StreamEx.of(3).prepend(4)).toList());

        StreamEx<Integer> s = StreamEx.of(1, 2, 3);
        assertSame(s, s.prepend());
        assertSame(s, s.prepend(Collections.emptyList()));
        assertSame(s, s.prepend(new ArrayList<>()));
        assertSame(s, s.prepend(Stream.empty()));
        assertNotSame(s, s.prepend(new ConcurrentLinkedQueue<>()));

        assertTrue(StreamEx.of("a", "b").prepend(Stream.of("c").parallel()).isParallel());
        assertTrue(StreamEx.of("a", "b").parallel().prepend(Stream.of("c").parallel()).isParallel());
        assertTrue(StreamEx.of("a", "b").parallel().prepend(Stream.of("c")).isParallel());
        assertFalse(StreamEx.of("a", "b").prepend(Stream.of("c")).isParallel());
    }

    @Test
    public void testPrependTSO() {
        List<Integer> expected = IntStreamEx.rangeClosed(19999, 0, -1).boxed().toList();
        assertEquals(expected, IntStreamEx.range(20000).mapToObj(StreamEx::of).reduce(StreamEx::prepend).get()
                .toList());
        assertEquals(expected, IntStreamEx.range(20000).parallel().mapToObj(StreamEx::of).reduce(StreamEx::prepend)
                .get().toList());
    }

    @Test
    public void testNonNull() {
        List<String> data = asList("a", null, "b");
        assertEquals(asList("a", null, "b"), StreamEx.of(data).toList());
        assertEquals(asList("a", "b"), StreamEx.of(data).nonNull().toList());
    }

    @Test
    public void testSorting() {
        assertEquals(asList("a", "b", "c", "d"), StreamEx.of("b", "c", "a", "d").sorted().toList());
        assertEquals(asList("d", "c", "b", "a"), StreamEx.of("b", "c", "a", "d").reverseSorted().toList());

        List<String> data = asList("a", "bbb", "cc");
        assertEquals(asList("a", "cc", "bbb"), StreamEx.of(data).sorted(Comparator.comparingInt(String::length))
                .toList());
        assertEquals(asList("bbb", "cc", "a"), StreamEx.of(data).reverseSorted(Comparator.comparingInt(String::length))
                .toList());
        assertEquals(asList("a", "cc", "bbb"), StreamEx.of(data).sortedByInt(String::length).toList());
        assertEquals(asList("a", "cc", "bbb"), StreamEx.of(data).sortedByLong(String::length).toList());
        assertEquals(asList("a", "cc", "bbb"), StreamEx.of(data).sortedByDouble(String::length).toList());
        assertEquals(asList("a", "cc", "bbb"), StreamEx.of(data).sortedBy(String::length).toList());
    }

    @Test
    public void testMinMax() {
        withRandom(random -> {
            List<String> data = IntStreamEx.of(random, 1000, 1, 100).mapToObj(len -> IntStreamEx.constant(random
                    .nextInt('z' - 'a' + 1) + 'a', len).charsToString()).toList();
            String minStr = Collections.min(data, Comparator.comparingInt(String::length)), maxStr = Collections.max(
                data, Comparator.comparingInt(String::length));
            streamEx(data::stream, supplier -> {
                assertEquals(supplier.toString(), maxStr, supplier.get().max(Comparator.comparingInt(String::length))
                        .get());
                assertEquals(supplier.toString(), maxStr, supplier.get().maxByInt(String::length).get());
                assertEquals(supplier.toString(), maxStr, supplier.get().maxByLong(String::length).get());
                assertEquals(supplier.toString(), maxStr, supplier.get().maxByDouble(String::length).get());
                assertEquals(supplier.toString(), maxStr, supplier.get().maxBy(String::length).get());

                assertEquals(supplier.toString(), maxStr, supplier.get().min(Comparator.comparingInt(String::length)
                        .reversed()).get());
                assertEquals(supplier.toString(), minStr, supplier.get().minByInt(String::length).get());
                assertEquals(supplier.toString(), minStr, supplier.get().minByLong(String::length).get());
                assertEquals(supplier.toString(), minStr, supplier.get().minByDouble(String::length).get());
                assertEquals(supplier.toString(), minStr, supplier.get().minBy(String::length).get());
            });
            assertFalse(StreamEx.<String>empty().minByInt(String::length).isPresent());
        });
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
        assertEquals(asList("a", "bb", null), StreamEx.of("a", "bb", null, "c").without("c").toList());
        String s = null;
        assertEquals(asList("a", "bb", "c"), StreamEx.of("a", "bb", null, "c", null).without(s).toList());
        assertTrue(StreamEx.of("bb", "bb", "bb").without("bb").toList().isEmpty());
        assertEquals(asList("bb", "bb", "bb"), StreamEx.of("bb", "bb", "bb").without(s).toList());

        StreamEx<String> stream = StreamEx.of("a", "b", "c");
        assertSame(stream, stream.without());
        assertEquals(asList("a", "b", "c"), StreamEx.of("a", "b", null, "c").without(new String[] { null }).toList());
        assertEquals(asList(), StreamEx.of("a", "b", null, "c").without("c", null, "b", "a").toList());
    }

    @Test
    public void testJoining() {
        assertEquals("abc", StreamEx.of("a", "b", "c").joining());
        assertEquals("a,b,c", StreamEx.of("a", "b", "c").joining(","));
        assertEquals("[1;2;3]", StreamEx.of(1, 2, 3).joining(";", "[", "]"));

        withRandom(r -> {
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
        });
    }

    @Test
    public void testFoldLeft() {
        List<String> input = asList("a", "bb", "ccc");
        streamEx(input::stream, supplier -> {
            assertEquals("ccc;bb;a;", supplier.get().foldLeft("", (u, v) -> v + ";" + u));
            assertTrue(supplier.get().foldLeft(false, (Boolean acc, String s) -> acc || s.equals("bb")));
            assertFalse(supplier.get().foldLeft(false, (Boolean acc, String s) -> acc || s.equals("d")));
            assertEquals(6, (int) supplier.get().foldLeft(0, (acc, v) -> acc + v.length()));
            assertEquals("{ccc={bb={a={}}}}", supplier.get().foldLeft(Collections.emptyMap(), (Map<String, Object> acc,
                    String v) -> Collections.singletonMap(v, acc)).toString());
        });
    }

    @Test
    public void testFoldLeftOptional() {
        // non-associative
        BinaryOperator<Integer> accumulator = (x, y) -> (x + y) * (x + y);
        streamEx(() -> StreamEx.constant(3, 4), supplier -> assertEquals(2322576, (int) supplier.get().foldLeft(
            accumulator).orElse(-1)));
        streamEx(() -> StreamEx.of(1, 2, 3), supplier -> assertEquals(144, (int) supplier.get().foldLeft(accumulator)
                .orElse(-1)));
        emptyStreamEx(Integer.class, supplier -> assertFalse(supplier.get().foldLeft(accumulator).isPresent()));
    }

    @Test
    public void testFoldRight() {
        assertEquals(";c;b;a", StreamEx.of("a", "b", "c").parallel().foldRight("", (u, v) -> v + ";" + u));
        assertEquals("{a={bb={ccc={}}}}", StreamEx.of("a", "bb", "ccc").foldRight(Collections.emptyMap(), (BiFunction<String, Map<String, Object>, Map<String, Object>>) Collections::singletonMap).toString());
        assertEquals("{a={bb={ccc={}}}}", StreamEx.of("a", "bb", "ccc").parallel().foldRight(Collections.emptyMap(), (BiFunction<String, Map<String, Object>, Map<String, Object>>) Collections::singletonMap).toString());
    }

    @Test
    public void testFoldRightOptional() {
        // non-associative
        BinaryOperator<Integer> accumulator = (x, y) -> (x + y) * (x + y);
        streamEx(() -> StreamEx.constant(3, 4), supplier -> assertEquals(2322576, (int) supplier.get().foldRight(
            accumulator).orElse(-1)));
        streamEx(() -> StreamEx.of(1, 2, 3, 0), supplier -> assertEquals(14884, (int) supplier.get().foldRight(
            accumulator).orElse(-1)));
        emptyStreamEx(Integer.class, supplier -> assertFalse(supplier.get().foldRight(accumulator).isPresent()));
    }

    @Test
    public void testDistinctAtLeast() {
        assertEquals(0, StreamEx.of("a", "b", "c").distinct(2).count());
        assertEquals(StreamEx.of("a", "b", "c").distinct().toList(), StreamEx.of("a", "b", "c").distinct(1).toList());
        assertEquals(asList("b"), StreamEx.of("a", "b", "c", "b", null).distinct(2).toList());
        assertEquals(asList("b", null), StreamEx.of("a", "b", null, "c", "b", null).distinct(2).toList());
        assertEquals(asList(null, "b"), StreamEx.of("a", "b", null, "c", null, "b", null, "b").distinct(2).toList());
        streamEx(() -> IntStreamEx.range(0, 1000).map(x -> x / 3).boxed(), supplier -> {
            assertEquals(334, supplier.get().distinct().count());
            assertEquals(333, supplier.get().distinct(2).count());
            assertEquals(333, supplier.get().distinct(3).count());
            assertEquals(0, supplier.get().distinct(4).count());

            List<Integer> distinct3List = supplier.get().distinct(3).toList();
            assertEquals(333, distinct3List.size());
            Map<Integer, Long> map = supplier.get().collect(Collectors.groupingBy(Function.identity(),
                LinkedHashMap::new, Collectors.counting()));
            List<Integer> expectedList = StreamEx.ofKeys(map, val -> val >= 3).toList();
            assertEquals(333, expectedList.size());
            assertEquals(distinct3List, expectedList);
        });

        assertEquals(0, StreamEx.of("a", "b", "c").parallel().distinct(2).count());
        assertEquals(StreamEx.of("a", "b", "c").parallel().distinct().toList(), StreamEx.of("a", "b", "c").parallel()
                .distinct(1).toList());
        assertEquals(asList("b"), StreamEx.of("a", "b", "c", "b").parallel().distinct(2).toList());
        assertEquals(new HashSet<>(asList("b", null)), StreamEx.of("a", "b", null, "c", "b", null).parallel().distinct(
            2).toSet());
        assertEquals(new HashSet<>(asList(null, "b")), StreamEx.of("a", "b", null, "c", null, "b", null, "b").parallel()
                .distinct(2).toSet());

        for (int i = 1; i < 1000; i += 100) {
            List<Integer> input = IntStreamEx.of(new Random(1), i, 1, 100).boxed().toList();
            for (int n : IntStreamEx.range(2, 10).boxed()) {
                Set<Integer> expected = input.stream().collect(Collectors.collectingAndThen(Collectors.groupingBy(
                    Function.identity(), Collectors.counting()), m -> {
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

        List<String> actual = StreamEx.split("a,b,a,c,d,b,a", ",").parallel().distinct(2).sorted().toList();
        assertEquals(asList("a", "b"), actual);
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
        streamEx(() -> new Random(1).ints(1000, 0, 100).sorted().boxed(), supplier -> assertEquals(expected, supplier
                .get().distinct(15).pairMap((a, b) -> b - a).toList()));
    }

    private static <T extends Comparable<? super T>> boolean isSorted(Collection<T> c) {
        return StreamEx.of(c).parallel().pairMap(Comparable::compareTo).allMatch(r -> r <= 0);
    }

    private static <T extends Comparable<? super T>> Optional<T> firstMisplaced(Collection<T> c) {
        return StreamEx.of(c).parallel().pairMap((a, b) -> a.compareTo(b) > 0 ? a : null).nonNull().findFirst();
    }

    @Test
    public void testPairMap() {
        assertEquals(0, StreamEx.<String>empty().pairMap(String::concat).count());
        assertArrayEquals(new Object[0], StreamEx.<String>empty().pairMap(String::concat).toArray());
        assertEquals(0, StreamEx.of("a").pairMap(String::concat).count());
        assertEquals(asList("aa", "aa", "aa"), StreamEx.generate(() -> "a").pairMap(String::concat).limit(3).toList());
        AtomicBoolean flag = new AtomicBoolean();
        assertFalse(flag.get());
        StreamEx<String> stream = StreamEx.of("a", "b").onClose(() -> flag.set(true)).pairMap(String::concat);
        stream.close();
        assertTrue(flag.get());
        assertEquals(Collections.singletonMap(1, 1999L), IntStreamEx.range(2000).boxed().pairMap((a, b) -> b - a)
                .groupingBy(Function.identity(), Collectors.counting()));
        assertEquals(Collections.singletonMap(1, 1999L), IntStreamEx.range(2000).parallel().boxed().pairMap((a, b) -> b
            - a).groupingBy(Function.identity(), Collectors.counting()));

        withRandom(r -> {
            Integer[] data = r.ints(1000, 1, 1000).boxed().toArray(Integer[]::new);
            Double[] expected = new Double[data.length - 1];
            for (int i = 0; i < expected.length; i++)
                expected[i] = (data[i + 1] - data[i]) * 1.23;
            Double[] result = StreamEx.of(data).parallel().pairMap((a, b) -> (b - a) * 1.23).toArray(Double[]::new);
            assertArrayEquals(expected, result);
            result = StreamEx.of(data).pairMap((a, b) -> (b - a) * 1.23).toArray(Double[]::new);
            assertArrayEquals(expected, result);
        });

        // Find all numbers where the integer preceded a larger value.
        Collection<Integer> numbers = asList(10, 1, 15, 30, 2, 6);
        List<Integer> res = StreamEx.of(numbers).pairMap((a, b) -> a < b ? a : null).nonNull().toList();
        assertEquals(asList(1, 15, 2), res);

        // Check whether the stream is sorted
        assertTrue(isSorted(asList("a", "bb", "bb", "c")));
        assertFalse(isSorted(asList("a", "bb", "bb", "bba", "bb", "c")));
        withRandom(r -> assertTrue(isSorted(IntStreamEx.of(r).boxed().distinct().limit(1000).toCollection(
            TreeSet::new))));

        // Find the first element that violates the sorting
        assertEquals("bba", firstMisplaced(asList("a", "bb", "bb", "bba", "bb", "c")).get());
        assertFalse(firstMisplaced(asList("a", "bb", "bb", "bb", "c")).isPresent());
        assertFalse(firstMisplaced(Arrays.<String>asList()).isPresent());
        assertFalse(IntStreamEx.range(1000).greater(2000).boxed().parallel().pairMap((a, b) -> a).findFirst()
                .isPresent());
    }

    @Test
    public void testPairMapCornerCase() {
        repeat(100, iter -> streamEx(() -> IntStreamEx.range(1000).filter(x -> x == 0 || x == 999).boxed(),
            supplier -> assertEquals(Collections.singletonList(-999), supplier.get().pairMap((a, b) -> a - b)
                    .toList())));
    }

    @Test
    public void testPairMapFlatMapBug() {
        Integer[][] input = { { 1 }, { 2, 3 }, { 4, 5, 6 }, { 7, 8 }, { 9 } };
        streamEx(() -> StreamEx.of(input).flatMap(Arrays::stream), supplier -> assertEquals(1L, supplier.get()
                .pairMap((a, b) -> b - a).distinct().count()));
    }

    private static double interpolate(Point[] points, double x) {
        return StreamEx.of(points).parallel().pairMap((p1, p2) -> p1.x <= x && p2.x >= x ? (x - p1.x) / (p2.x - p1.x)
            * (p2.y - p1.y) + p1.y : null).nonNull().findAny().orElse(Double.NaN);
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
        withRandom(r -> {
            int[] random = IntStreamEx.of(r, 1000).toArray();
            List<Integer> scanLeft = IntStreamEx.of(random).boxed().parallel().scanLeft(0, Integer::sum);
            assertArrayEquals(random, IntStreamEx.of(scanLeft).parallel().pairMap((a, b) -> (b - a)).toArray());
        });
    }

    @Test
    public void testPairMapCapitalization() {
        assertEquals("Test Capitalization Stream", IntStreamEx.ofChars("test caPiTaliZation streaM").parallel().prepend(
            0).mapToObj(c -> (char) c).pairMap((c1, c2) -> !Character.isLetter(c1) && Character
                    .isLetter(c2) ? Character.toTitleCase(c2) : Character.toLowerCase(c2)).joining());
    }

    @Test
    public void testPairMapAddHeaders() {
        List<String> result = StreamEx.of("aaa", "abc", "bar", "foo", "baz", "argh").sorted().prepend("").pairMap((a,
                b) -> a.isEmpty() || a.charAt(0) != b.charAt(0) ? Stream.of("=== " + b.substring(0, 1).toUpperCase()
                    + " ===", b) : Stream.of(b)).flatMap(Function.identity()).toList();
        List<String> expected = asList("=== A ===", "aaa", "abc", "argh", "=== B ===", "bar", "baz", "=== F ===",
            "foo");
        assertEquals(expected, result);
    }

    static class Node {
        Node parent;
        final String name;

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
        streamEx(() -> IntStreamEx.rangeClosed(1, 4).boxed(), supplier -> {
            assertEquals(asList(0, 1, 3, 6, 10), supplier.get().scanLeft(0, Integer::sum));
            assertEquals(asList(1, 3, 6, 10), supplier.get().scanLeft(Integer::sum));
        });
        emptyStreamEx(Integer.class, supplier -> {
            assertTrue(supplier.get().scanLeft(Integer::sum).isEmpty());
            assertEquals(asList(0), supplier.get().scanLeft(0, Integer::sum));
        });
        assertEquals(167167000, IntStreamEx.rangeClosed(1, 1000).boxed().parallel().scanLeft(0, Integer::sum).stream()
                .mapToLong(x -> x).sum());
    }

    @Test
    public void testScanRight() {
        streamEx(() -> IntStreamEx.rangeClosed(1, 4).boxed(), supplier -> {
            assertEquals(asList(10, 9, 7, 4, 0), supplier.get().scanRight(0, Integer::sum));
            assertEquals(asList(10, 9, 7, 4), supplier.get().scanRight(Integer::sum));
        });
        emptyStreamEx(Integer.class, supplier -> {
            assertTrue(supplier.get().scanRight(Integer::sum).isEmpty());
            assertEquals(asList(0), supplier.get().scanRight(0, Integer::sum));
        });
        assertEquals(333833500, IntStreamEx.rangeClosed(1, 1000).boxed().parallel().scanRight(0, Integer::sum).stream()
                .mapToLong(x -> x).sum());
    }

    @Test
    public void testPermutations() {
        assertEquals("[]", StreamEx.ofPermutations(0).map(Arrays::toString).joining(";"));
        assertEquals("[0, 1, 2];[0, 2, 1];[1, 0, 2];[1, 2, 0];[2, 0, 1];[2, 1, 0]", StreamEx.ofPermutations(3).map(
            Arrays::toString).joining(";"));
        assertEquals(720, StreamEx.ofPermutations(7).parallel().filter(i -> i[3] == 5).count());
    }

    static class TreeNode {
        final String title;

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
        final List<TreeNode> nodes = new ArrayList<>();

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

        static CompositeNode createTestData() {
            CompositeNode r = new CompositeNode("root");
            r.add(new CompositeNode("childA").add(new TreeNode("grandA1")).add(new TreeNode("grandA2")));
            r.add(new CompositeNode("childB").add(new TreeNode("grandB1")));
            r.add(new TreeNode("childC"));
            return r;
        }
    }

    @Test
    public void testOfTree() {
        String inputSimple = "bbb";
        List<Object> input = asList("aa", null, asList(asList("bbbb", "cc", null, asList()), "ddd", asList("e"), asList(
            "fff")), "ggg");
        @SuppressWarnings("unchecked")
        Function<Object, Stream<Object>> generator = o -> o instanceof List ? ((List<Object>) o).stream() : null;
        assertEquals("bbb", StreamEx.ofTree(inputSimple, generator).select(String.class).joining(","));
        StreamEx<Object> ofTree = StreamEx.ofTree(input, generator);
        assertEquals("aa,bbbb,cc,ddd,e,fff,ggg", ofTree.select(String.class).joining(","));
        assertEquals(14, StreamEx.ofTree(input, generator).select(List.class).mapToInt(List::size).sum());

        CompositeNode r = CompositeNode.createTestData();

        streamEx(r::flatStream, s -> {
            assertEquals("root,childA,grandA1,grandA2,childB,grandB1,childC", s.get().joining(","));
            assertEquals(Optional.of("grandB1"), s.get().findFirst(tn -> tn.title.contains("B1")).map(tn -> tn.title));
            assertEquals(Optional.empty(), s.get().findFirst(tn -> tn.title.contains("C1")).map(tn -> tn.title));
        });

        streamEx(() -> StreamEx.ofTree("", (String str) -> str.length() >= 3 ? null
                : Stream.of("a", "b").map(str::concat)), supplier -> {
            assertEquals(Arrays.asList("", "a", "aa", "aaa", "aab", "ab", "aba", "abb", "b", "ba", "baa", "bab",
                    "bb", "bba", "bbb"), supplier.get().toList());
            assertEquals(Arrays.asList("a", "b", "aa", "ab", "ba", "bb", "aaa", "aab", "aba", "abb", "baa",
                    "bab", "bba", "bbb"), supplier.get().sortedByInt(String::length).without("").toList());
        });

        assertEquals(1000001, StreamEx.ofTree("x", s -> s.equals("x") ? IntStreamEx.range(1000000).mapToObj(
                String::valueOf) : null).parallel().count());
    }

    @Test
    public void testOfTreeDeep() {
        List<Integer> numbers = StreamEx.ofTree(1, n -> n >= 10000 ? null : StreamEx.of(n + 1))
                .toList();
        assertEquals(IntStreamEx.rangeClosed(1, 10000).boxed().toList(), numbers);

        assertEquals(700, StreamEx.ofTree(1, n -> n >= 300 ? null : StreamEx.constant(n + 1, n % 100 == 0 ? 2 : 1))
                .count());
    }

    @Test
    public void testOfTreeClose() {
        CompositeNode r = CompositeNode.createTestData();

        r.flatStream().close(); // should not fail

        List<Consumer<StreamEx<TreeNode>>> tests = Arrays.asList(stream -> assertEquals(
                Optional.empty(), stream.findFirst(tn -> tn.title.contains("abc"))), stream -> assertEquals(Optional.of(
                "grandB1"), stream.findFirst(tn -> tn.title.contains("B1")).map(tn -> tn.title)),
            stream -> assertEquals(7, stream.count()));
        for (Consumer<StreamEx<TreeNode>> test : tests) {
            Set<String> set = new HashSet<>();
            try (StreamEx<TreeNode> closableTree = StreamEx.ofTree(r, CompositeNode.class, cn -> cn.elements().onClose(
                () -> set.add(cn.title)))) {
                test.accept(closableTree);
            }
            assertEquals(set, StreamEx.of("root", "childA", "childB").toSet());
            boolean catched = false;
            try (StreamEx<TreeNode> closableTree = StreamEx.ofTree(r, CompositeNode.class, cn -> cn.elements().onClose(
                () -> {
                    if (!cn.title.equals("childA"))
                        throw new IllegalArgumentException(cn.title);
                }))) {
                test.accept(closableTree);
            } catch (IllegalArgumentException ex) {
                catched = true;
                assertEquals("childB", ex.getMessage());
                assertEquals(1, ex.getSuppressed().length);
                assertTrue(ex.getSuppressed()[0] instanceof IllegalArgumentException);
                assertEquals("root", ex.getSuppressed()[0].getMessage());
            }
            assertTrue(catched);

            catched = false;
            try (StreamEx<TreeNode> closableTree = StreamEx.ofTree(r, CompositeNode.class, cn -> cn.elements().onClose(
                () -> {
                    if (!cn.title.equals("childA"))
                        throw new InternalError(cn.title);
                }))) {
                test.accept(closableTree);
            } catch (InternalError ex) {
                catched = true;
                assertEquals("childB", ex.getMessage());
                assertEquals(1, ex.getSuppressed().length);
                assertTrue(ex.getSuppressed()[0] instanceof InternalError);
                assertEquals("root", ex.getSuppressed()[0].getMessage());
            }
            assertTrue(catched);

            catched = false;
            try (StreamEx<TreeNode> closableTree = StreamEx.ofTree(r, CompositeNode.class, cn -> cn.elements().onClose(
                () -> {
                    if (!cn.title.equals("childA"))
                        throw new InternalError(cn.title);
                }))) {
                test.accept(closableTree.parallel());
            } catch (InternalError ex) {
                catched = true;
                assertEquals(1, ex.getSuppressed().length);
                assertTrue(ex.getSuppressed()[0] instanceof InternalError);
                Set<String> msgSet = StreamEx.of(ex.getMessage(), ex.getSuppressed()[0].getMessage()).toSet();
                assertTrue(msgSet.contains("root"));
                assertTrue(msgSet.contains("childB") || msgSet.contains("java.lang.InternalError: childB"));
            }
            assertTrue(catched);
        }
    }

    @Test
    public void testCross() {
        assertEquals("a-1, a-2, a-3, b-1, b-2, b-3, c-1, c-2, c-3", StreamEx.of("a", "b", "c").cross(1, 2, 3).join("-")
                .joining(", "));
        assertEquals("a-1, b-1, c-1", StreamEx.of("a", "b", "c").cross(1).join("-").joining(", "));
        assertEquals("", StreamEx.of("a", "b", "c").cross().join("-").joining(", "));
        List<String> inputs = asList("i", "j", "k");
        List<String> outputs = asList("x", "y", "z");
        assertEquals("i->x, i->y, i->z, j->x, j->y, j->z, k->x, k->y, k->z", StreamEx.of(inputs).cross(outputs)
                .mapKeyValue((input, output) -> input + "->" + output).joining(", "));
        assertEquals("", StreamEx.of(inputs).cross(Collections.emptyList()).join("->").joining(", "));
        assertEquals("i-i, j-j, k-k", StreamEx.of(inputs).cross(Stream::of).join("-").joining(", "));
        assertEquals("j-j, k-k", StreamEx.of(inputs).cross(x -> x.equals("i") ? null : Stream.of(x)).join("-").joining(
            ", "));
    }

    @Test
    public void testCollapse() {
        streamEx(() -> StreamEx.constant(1, 1000), supplier -> assertEquals(Collections.singletonList(1), supplier.get()
                .collapse(Objects::equals).toList()));
    }

    @Test
    public void testCollapseEmptyLines() {
        withRandom(r -> repeat(100, i -> {
            List<String> input = IntStreamEx.range(r.nextInt(i + 1)).mapToObj(n -> r.nextBoolean() ? ""
                    : String.valueOf(n)).toList();
            List<String> resultSpliterator = StreamEx.of(input).collapse((str1, str2) -> str1.isEmpty() && str2
                    .isEmpty()).toList();
            List<String> resultSpliteratorParallel = StreamEx.of(input).parallel().collapse((str1, str2) -> str1
                    .isEmpty() && str2.isEmpty()).toList();
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
            assertEquals(expected, resultSpliterator);
            assertEquals(expected, resultSpliteratorParallel);
        }));
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
        withRandom(r -> repeat(100, i -> {
            int size = r.nextInt(i * 5 + 1);
            int[] input = IntStreamEx.of(r, size, 0, size * 3 / 2 + 2).toArray();
            String result = IntStreamEx.of(input).sorted().boxed().distinct().map(Interval::new).collapse(
                Interval::adjacent, Interval::merge).joining(" & ");
            String resultIntervalMap = IntStreamEx.of(input).sorted().boxed().distinct().intervalMap((a, b) -> b
                - a == 1, Interval::new).joining(" & ");
            String resultIntervalMapParallel = IntStreamEx.of(input).sorted().boxed().distinct().intervalMap((a,
                    b) -> b - a == 1, Interval::new).parallel().joining(" & ");
            String resultParallel = IntStreamEx.of(input).parallel().sorted().boxed().distinct().map(Interval::new)
                    .collapse(Interval::adjacent, Interval::merge).joining(" & ");
            String resultParallel2 = IntStreamEx.of(input).sorted().boxed().distinct().map(Interval::new).collapse(
                Interval::adjacent, Interval::merge).parallel().joining(" & ");
            String resultCollector = IntStreamEx.of(input).sorted().boxed().distinct().map(Interval::new).collapse(
                Interval::adjacent, Collectors.reducing(Interval::merge)).map(Optional::get).parallel().joining(
                    " & ");
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
            assertEquals(expectedStr, result);
            assertEquals(expectedStr, resultParallel);
            assertEquals(expectedStr, resultParallel2);
            assertEquals(expectedStr, resultIntervalMap);
            assertEquals(expectedStr, resultIntervalMapParallel);
            assertEquals(expectedStr, resultCollector);
        }));
    }

    @Test
    public void testCollapseDistinct() {
        withRandom(r -> repeat(100, i -> {
            int size = r.nextInt(i * 5 - 4);
            List<Integer> input = IntStreamEx.of(r, size, 0, size * 3 / 2 + 2).boxed().sorted().toList();
            List<Integer> distinct = StreamEx.of(input).collapse(Integer::equals).toList();
            List<Integer> distinctCollector = StreamEx.of(input).collapse(Integer::equals, MoreCollectors.first())
                    .map(Optional::get).toList();
            List<Integer> distinctParallel = StreamEx.of(input).parallel().collapse(Integer::equals).toList();
            List<Integer> distinctCollectorParallel = StreamEx.of(input).parallel().collapse(Integer::equals,
                MoreCollectors.first()).map(Optional::get).toList();
            List<Integer> expected = input.stream().distinct().collect(Collectors.toList());
            assertEquals(expected, distinct);
            assertEquals(expected, distinctParallel);
            assertEquals(expected, distinctCollector);
            assertEquals(expected, distinctCollectorParallel);
        }));
    }

    @Test
    public void testCollapsePairMap() {
        int[] input = { 0, 0, 1, 1, 1, 1, 4, 6, 6, 3, 3, 10 };
        List<Integer> expected = IntStreamEx.of(input).pairMap((a, b) -> b - a).without(0).boxed().toList();
        streamEx(() -> IntStreamEx.of(input).boxed(), supplier -> assertEquals(expected, supplier.get().collapse(
            Integer::equals).pairMap((a, b) -> b - a).toList()));
    }

    static StreamEx<String> sentences(StreamEx<String> source) {
        return source.flatMap(Pattern.compile("(?<=\\.)")::splitAsStream).collapse((a, b) -> !a.endsWith("."), (a,
                b) -> a + ' ' + b).map(String::trim);
    }

    @Test
    public void testStreamOfSentences() {
        List<String> lines = asList("This is the", "first sentence.  This is the",
            "second sentence. Third sentence. Fourth", "sentence. Fifth sentence.", "The last");
        streamEx(lines::stream, supplier -> assertEquals(asList("This is the first sentence.",
            "This is the second sentence.", "Third sentence.", "Fourth sentence.", "Fifth sentence.", "The last"),
            sentences(supplier.get()).toList()));
    }

    @Test
    public void testCollapseCollector() {
        List<String> input = asList("aaa", "bb", "baz", "bar", "foo", "fee", "abc");
        streamEx(input::stream, supplier -> assertEquals(asList("aaa", "bb:baz:bar", "foo:fee", "abc"), supplier.get()
                .collapse((a, b) -> a.charAt(0) == b.charAt(0), Collectors.joining(":")).toList()));
    }

    @Test
    public void testLongestSeries() {
        List<Integer> input = asList(1, 2, 2, 3, 3, 2, 2, 2, 3, 4, 4, 4, 4, 4, 2);
        streamEx(input::stream, supplier -> assertEquals(5L, (long) supplier.get().collapse(Object::equals, Collectors
                .counting()).maxBy(Function.identity()).get()));
    }

    @Test
    public void testGroupRuns() {
        List<String> input = asList("aaa", "bb", "baz", "bar", "foo", "fee", "abc");
        List<List<String>> expected = asList(asList("aaa"), asList("bb", "baz", "bar"), asList("foo", "fee"), asList(
            "abc"));
        streamEx(input::stream, supplier -> {
            assertEquals(expected, supplier.get().groupRuns((a, b) -> a.charAt(0) == b.charAt(0)).toList());
            assertEquals(expected, supplier.get().collapse((a, b) -> a.charAt(0) == b.charAt(0), Collectors.toList())
                    .toList());
        });
    }

    @Test
    public void testGroupRunsRandom() {
        withRandom(r -> {
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
        });
    }

    @Test
    public void testGroupRunsSeparated() {
        streamEx(asList("a", "b", null, "c", null, "d", "e")::stream, supplier -> assertEquals(asList(asList("a", "b"),
            asList("c"), asList("d", "e")), supplier.get().groupRuns((a, b) -> a != null && b != null).remove(
                list -> list.get(0) == null).toList()));
    }

    @Test
    public void testGroupRunsByStart() {
        List<String> input = asList("str1", "str2", "START: str3", "str4", "START: str5", "START: str6", "START: str7",
            "str8", "str9");
        Pattern start = Pattern.compile("^START:");
        streamEx(input::stream, supplier -> assertEquals(asList(asList("str1", "str2"), asList("START: str3", "str4"),
            asList("START: str5"), asList("START: str6"), asList("START: str7", "str8", "str9")), supplier.get()
                    .groupRuns((a, b) -> !start.matcher(b).find()).toList()));
    }

    private static String format(StreamEx<Integer> ints) {
        return ints.distinct().sorted().intervalMap((i, j) -> j == i + 1, (i, j) -> j.equals(i) ? i.toString()
                : j == i + 1 ? i + "," + j : i + ".." + j).joining(",");
    }

    private static String formatNaive(int[] input) {
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
        streamEx(() -> StreamEx.of(input), s -> {
            assertEquals("1: 1, 2: 2, 4: 1, 2: 1, null: 2, 1: 3, null: 2", s.get().runLengths().join(": ").joining(
                ", "));
            assertEquals("1=1, 2=2, 4=1, 2=1, null=2, 1=3", s.get().runLengths().distinct().map(String::valueOf)
                    .joining(", "));
        });
        Entry<Integer, Long> entry = StreamEx.of(input).runLengths().findFirst().get();
        // Test equals contract for custom entry
        assertNotEquals(entry, new Object());
        assertNotEquals(new Object(), entry);
        assertEquals(entry, new AbstractMap.SimpleImmutableEntry<>(1, 1L));

        assertThrows(UnsupportedOperationException.class,
                     () -> StreamEx.of("1", "1", "1").runLengths().forEach(e -> e.setValue(5L)));
    }
    
    @SuppressWarnings("SimplifiableAssertion")
    @Test
    public void testRunLengthsEntries() {
        // runLengths produces custom entries (ObjLongBox), so let's test their equals/hashCode contract
        List<Entry<String, Long>> runLengths = StreamEx.of("a", "a", "b", "b", "a", "a", "a").runLengths().toList();
        assertTrue(runLengths.get(0).equals(new AbstractMap.SimpleImmutableEntry<>("a", 2L)));
        assertTrue(runLengths.get(1).equals(new AbstractMap.SimpleImmutableEntry<>("b", 2L)));
        assertTrue(runLengths.get(2).equals(new AbstractMap.SimpleImmutableEntry<>("a", 3L)));
        assertEquals(runLengths.get(0).hashCode(), new AbstractMap.SimpleImmutableEntry<>("a", 2L).hashCode());
        assertEquals(runLengths.get(1).hashCode(), new AbstractMap.SimpleImmutableEntry<>("b", 2L).hashCode());
        assertEquals(runLengths.get(2).hashCode(), new AbstractMap.SimpleImmutableEntry<>("a", 3L).hashCode());
        assertTrue(new AbstractMap.SimpleImmutableEntry<>("a", 2L).equals(runLengths.get(0)));
        assertTrue(new AbstractMap.SimpleImmutableEntry<>("b", 2L).equals(runLengths.get(1)));
        assertTrue(new AbstractMap.SimpleImmutableEntry<>("a", 3L).equals(runLengths.get(2)));
        assertNotEquals(runLengths.get(0), runLengths.get(1));
        assertNotEquals(runLengths.get(0), runLengths.get(2));
    }

    @Test
    public void testRunLengthsSorted() {
        withRandom(r -> {
            int[] input = IntStreamEx.of(r, 1000, 1, 20).sorted().toArray();
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
        });
    }

    /*
     * Returns the longest input stream segment for which the predicate holds (like
     * the corresponding Scala method)
     */
    private static long segmentLength(IntStreamEx source, IntPredicate predicate) {
        return source.mapToObj(predicate::test).runLengths().removeKeys(Boolean.FALSE::equals).mapToLong(
            Entry::getValue).max().orElse(0);
    }

    @Test
    public void testSegmentLength() {
        Consumer<int[]> test = input -> {
            // get maximal count of consecutive positive numbers
            long res = segmentLength(IntStreamEx.of(input), x -> x > 0);
            long resParallel = segmentLength(IntStreamEx.of(input).parallel(), x -> x > 0);
            long expected = 0;
            long cur = input[0] > 0 ? 1 : 0;
            for (int i = 0; i < input.length - 1; i++) {
                if (input[i] > 0 && input[i + 1] > 0)
                    cur++;
                else {
                    if (cur > expected)
                        expected = cur;
                    cur = 1;
                }
            }
            if (cur > expected)
                expected = cur;
            assertEquals(expected, res);
            assertEquals(expected, resParallel);
        };
        withRandom(r -> repeat(100, n -> test.accept(IntStreamEx.of(r, 1000, -10, 100).toArray())));
        test.accept(new int[] { 1, 2, 3, -1 });
        test.accept(new int[] { -1, 1, 2, -1, 1, 2, 3 });
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
        assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]", StreamEx.ofSubLists(input, Integer.MAX_VALUE).joining(
            "-"));
        assertEquals("", StreamEx.ofSubLists(Collections.emptyList(), 1).joining("-"));
        assertEquals("", StreamEx.ofSubLists(Collections.emptyList(), Integer.MAX_VALUE).joining("-"));

        List<Integer> myList = new SeqList(Integer.MAX_VALUE - 2);
        assertEquals(1, StreamEx.ofSubLists(myList, Integer.MAX_VALUE - 1).count());
        assertEquals(Integer.MAX_VALUE - 2, StreamEx.ofSubLists(myList, Integer.MAX_VALUE - 1).findFirst().get()
                .size());
        assertEquals(1, StreamEx.ofSubLists(myList, Integer.MAX_VALUE - 2).count());
        assertEquals(1, StreamEx.ofSubLists(myList, Integer.MAX_VALUE - 3).skip(1).findFirst().get().size());
    }

    @Test
    public void testSubListsStep() {
        List<Integer> input = IntStreamEx.range(12).boxed().toList();
        assertThrows(IllegalArgumentException.class, () -> StreamEx.ofSubLists(input, 0, Integer.MAX_VALUE));
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

        assertThrows(IllegalArgumentException.class, () -> StreamEx.ofSubLists(Collections.emptyList(), 0));
        assertThrows(IllegalArgumentException.class, () -> StreamEx.ofSubLists(Collections.emptyList(), 1, 0));
    }

    @Test
    public void testTakeWhile() {
        streamEx(asList("aaa", "b", "cccc")::stream, s -> {
            assertEquals(asList("aaa"), s.get().takeWhile(x -> x.length() > 1).toList());
            assertEquals(asList("aaa"), s.get().sorted().takeWhile(x -> x.length() > 1).toList());
            assertEquals(asList("aaa", "b", "cccc"), s.get().takeWhile(x -> x.length() > 0).toList());
            assertEquals(Collections.emptyList(), s.get().takeWhile(x -> x.length() > 5).toList());
        });
    }

    @Test
    public void testTakeWhileInclusive() {
        streamEx(asList("aaa", "b", "cccc")::stream, s -> {
            assertEquals(asList("aaa", "b"), s.get().takeWhileInclusive(x -> x.length() > 1).toList());
            assertEquals(asList("aaa", "b", "cccc"), s.get().takeWhileInclusive(x -> x.length() > 0).toList());
            assertEquals(asList("aaa"), s.get().takeWhileInclusive(x -> x.length() > 5).toList());
        });
    }

    @Test
    public void testDropWhile() {
        streamEx(asList("aaa", "b", "cccc")::stream, s -> {
            assertEquals(asList("b", "cccc"), s.get().dropWhile(x -> x.length() > 1).toList());
            assertEquals(asList(), s.get().dropWhile(x -> x.length() > 0).toList());
            assertEquals(asList("aaa", "b", "cccc"), s.get().dropWhile(x -> x.length() > 5).toList());
            // Saving to Optional is necessary as javac <8u40 fails to compile
            // this without an intermediate variable
            Optional<String> opt1 = s.get().dropWhile(x -> x.length() > 1).findFirst();
            assertEquals(Optional.of("b"), opt1);
            Optional<String> opt0 = s.get().dropWhile(x -> x.length() > 0).findFirst();
            assertEquals(Optional.empty(), opt0);
            Optional<String> opt5 = s.get().dropWhile(x -> x.length() > 5).findFirst();
            assertEquals(Optional.of("aaa"), opt5);
        });
    }

    @Test
    public void testTakeDropUnordered() {
        repeat(10, n -> withRandom(rnd -> {
            List<Boolean> data = IntStreamEx.of(rnd, n * 100L, 0, rnd.nextInt(10) + 2).mapToObj(x -> x != 0).toList();
            List<Boolean> sorted = StreamEx.of(data).sorted().toList();
            streamEx(() -> data.stream().unordered(), s -> {
                assertFalse(StreamEx.of(s.get().takeWhile(b -> b).toList()).has(false));
                assertEquals(1L, StreamEx.of(s.get().takeWhileInclusive(b -> b).toList()).without(true).count());
                assertEquals(0L, s.get().dropWhile(b -> true).count());
                assertEquals(0L, s.get().takeWhile(b -> false).count());
                assertEquals(1L, s.get().takeWhileInclusive(b -> false).count());
                List<Boolean> dropNone = s.get().dropWhile(b -> false).sorted().toList();
                assertEquals(sorted, dropNone);
                List<Boolean> takeAll = s.get().takeWhileInclusive(b -> true).sorted().toList();
                assertEquals(sorted, takeAll);
            });
        }));
    }

    @Test
    public void testOfPairs() {
        withRandom(r -> {
            Point[] pts = StreamEx.generate(() -> new Point(r.nextDouble(), r.nextDouble())).limit(100).toArray(
                Point[]::new);
            double expected = StreamEx.of(pts).cross(pts).mapKeyValue(Point::distance).mapToDouble(Double::doubleValue)
                    .max().getAsDouble();
            double[] allDist = IntStreamEx.ofIndices(pts).flatMapToDouble(i1 -> StreamEx.of(pts, i1 + 1, pts.length)
                    .mapToDouble(pt -> pt.distance(pts[i1]))).toArray();
            streamEx(() -> StreamEx.ofPairs(pts, Point::distance), supplier -> {
                assertEquals(expected, supplier.get().mapToDouble(Double::doubleValue).max().getAsDouble(), 0.0);
                assertArrayEquals(allDist, supplier.get().mapToDouble(Double::doubleValue).toArray(), 0.0);
            });
        });
    }

    @Test
    public void testToFlatCollection() {
        List<List<String>> strings = IntStreamEx.range(100).mapToObj(String::valueOf).groupRuns((a, b) -> a.charAt(
            0) == b.charAt(0)).toList();
        Set<String> expected = IntStreamEx.range(100).mapToObj(String::valueOf).toSet();
        List<String> expectedList = IntStreamEx.range(100).mapToObj(String::valueOf).toList();
        streamEx(strings::stream, supplier -> {
            assertEquals(expected, supplier.get().toFlatCollection(x -> x, HashSet::new));
            assertEquals(expectedList, supplier.get().toFlatList(x -> x));
        });
    }

    @Test
    public void testCartesian() {
        List<List<Integer>> expected = IntStreamEx.range(32).mapToObj(i -> IntStreamEx.range(5).mapToObj(n -> (i >> (4
            - n)) & 1).toList()).toList();
        streamEx(() -> StreamEx.cartesianPower(5, asList(0, 1)), supplier -> assertEquals(expected, supplier.get()
                .toList()));

        List<List<Integer>> input2 = asList(asList(1, 2, 3), asList(), asList(4, 5, 6));
        streamEx(() -> StreamEx.cartesianProduct(input2), supplier -> assertFalse(supplier.get().findAny()
                .isPresent()));

        List<List<Integer>> input3 = asList(asList(1, 2), asList(3), asList(4, 5));
        streamEx(() -> StreamEx.cartesianProduct(input3), supplier -> assertEquals(
            "[1, 3, 4],[1, 3, 5],[2, 3, 4],[2, 3, 5]", supplier.get().joining(",")));

        Set<Integer> input4 = IntStreamEx.range(10).boxed().toCollection(TreeSet::new);
        streamEx(() -> StreamEx.cartesianPower(3, input4), supplier -> assertEquals(IntStreamEx.range(1000).boxed()
                .toList(), supplier.get().map(list -> list.get(0) * 100 + list.get(1) * 10 + list.get(2)).toList()));

        assertEquals(asList(Collections.emptyList()), StreamEx.cartesianProduct(Collections.emptyList()).toList());
        assertEquals(asList(Collections.emptyList()), StreamEx.cartesianPower(0, asList(1, 2, 3)).toList());
        streamEx(() -> StreamEx.cartesianPower(3, new NoSplittingList()),
            s -> assertEquals(125, s.get().filter(x -> true).count()));
    }
    
    private static class NoSplittingList extends AbstractList<String> {
        @Override
        public String get(int index) {
            return "foo";
        }

        @Override
        public Spliterator<String> spliterator() {
            return new Spliterator<String>() {
                int pos = size();

                @Override
                public boolean tryAdvance(Consumer<? super String> action) {
                    if (pos <= 0) return false;
                    pos--;
                    action.accept("foo");
                    return true;
                }

                @Override
                public Spliterator<String> trySplit() {
                    return null;
                }

                @Override
                public long estimateSize() {
                    return size();
                }

                @Override
                public int characteristics() {
                    return SIZED;
                }
            };
        }

        @Override
        public int size() {
            return 5;
        }
    }

    @Test
    public void testCartesianReduce() {
        List<String> expected = IntStreamEx.range(32).mapToObj(i -> IntStreamEx.range(5).mapToObj(n -> (i >> (4 - n))
            & 1).joining()).toList();
        streamEx(() -> StreamEx.cartesianPower(5, asList(0, 1), "", (a, b) -> a + b), supplier -> assertEquals(expected,
            supplier.get().toList()));

        List<List<Integer>> input2 = asList(asList(1, 2, 3), asList(), asList(4, 5, 6));
        streamEx(() -> StreamEx.cartesianProduct(input2, "", (a, b) -> a + b), supplier -> assertFalse(supplier.get()
                .findAny().isPresent()));

        List<List<Integer>> input3 = asList(asList(1, 2), asList(3), asList(4, 5));
        streamEx(() -> StreamEx.cartesianProduct(input3, "", (a, b) -> a + b), supplier -> assertEquals(
            "134,135,234,235", supplier.get().joining(",")));

        assertEquals(asList(""), StreamEx.cartesianProduct(Collections.<List<String>>emptyList(), "", String::concat)
                .toList());
        assertEquals(asList(""), StreamEx.cartesianPower(0, asList(1, 2, 3), "", (a, b) -> a + b).toList());
    }

    @Test
    public void testDistinct() {
        List<String> input = asList("str", "a", "foo", "", "bbbb", null, "abcd", "s");
        streamEx(input::stream, supplier -> {
            assertEquals(input, supplier.get().distinct(x -> x).toList());
            assertEquals(asList("str", "a", "", "bbbb"), supplier.get().distinct(x -> x == null ? 0 : x.length())
                    .toList());
        });
    }

    @Test
    public void testIndexOf() {
        List<Integer> input = IntStreamEx.range(100).append(IntStreamEx.range(100)).boxed().toList();
        AtomicInteger counter = new AtomicInteger();
        assertEquals(10, StreamEx.of(input).peek(t -> counter.incrementAndGet()).indexOf(10).getAsLong());
        assertEquals(11, counter.get());
        for (int i = 0; i < 100; i++) {
            assertEquals(99, StreamEx.of(input).parallel().prepend().peek(t -> counter.incrementAndGet()).indexOf(99)
                    .getAsLong());
        }
        streamEx(input::stream, supplier -> {
            for (int i : new int[] { 0, 1, 10, 50, 78, 99 }) {
                assertEquals("#" + i, i, supplier.get().peek(t -> counter.incrementAndGet()).indexOf(x -> x == i)
                        .getAsLong());
            }
            assertFalse(supplier.get().indexOf(x -> false).isPresent());
        });
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
        streamEx(() -> StreamEx.of(0, 343, 999), s -> assertEquals(asList(2, 343, 997), s.get().mapFirst(x -> x + 2)
                .mapLast(x -> x - 2).toList()));
        streamEx(() -> IntStreamEx.range(1000).boxed(), s -> {
            assertEquals(asList(2, 343, 997), s.get().filter(x -> x == 0 || x == 343 || x == 999).mapFirst(x -> x + 2)
                    .mapLast(x -> x - 2).toList());
            assertEquals(asList(4, 343, 997), s.get().filter(x -> x == 0 || x == 343 || x == 999).mapFirst(x -> x + 2)
                    .mapFirst(x -> x + 2).mapLast(x -> x - 2).toList());
        });
        Supplier<Stream<Integer>> base = () -> IntStreamEx.rangeClosed(49, 0, -1).boxed().foldLeft(StreamEx.of(0), (
                stream, i) -> stream.prepend(i).mapFirst(x -> x + 2));
        streamEx(base, s -> assertEquals(IntStreamEx.range(2, 52).boxed().append(0).toList(), s.get().toList()));
        base = () -> IntStreamEx.range(50).boxed().foldLeft(StreamEx.of(0), (stream, i) -> stream.append(i).mapLast(
            x -> x + 2));
        streamEx(base, s -> assertEquals(IntStreamEx.range(2, 52).boxed().prepend(0).toList(), s.get().toList()));
        streamEx(asList("red", "green", "blue", "orange")::stream, s -> assertEquals("red, green, blue, or orange", s
                .get().mapLast("or "::concat).joining(", ")));
    }

    @Test
    public void testMapFirstOrElse() {
        streamEx(() -> StreamEx.split("testString", ""), s -> assertEquals("Teststring", s.get().mapFirstOrElse(
            String::toUpperCase, String::toLowerCase).joining()));
    }

    @Test
    public void testMapLastOrElse() {
        streamEx(asList("red", "green", "blue", "orange")::stream, s -> assertEquals("red, green, blue, or orange!", s
                .get().mapLastOrElse(str -> str + ", ", str -> "or " + str + "!").joining()));
        streamEx(asList("red", "green", "blue", "orange")::stream, s -> assertEquals(
            "|- red\n|- green\n|- blue\n\\- orange", s.get().mapLastOrElse("|- "::concat, "\\- "::concat).joining(
                "\n")));
    }

    @Test
    public void testPeekFirst() {
        List<String> input = asList("A", "B", "C", "D");
        streamEx(input::stream, s -> {
            AtomicReference<String> firstElement = new AtomicReference<>();
            assertEquals(asList("B", "C", "D"), s.get().peekFirst(firstElement::set).skip(1).toList());
            assertEquals("A", firstElement.get());

            assertEquals(asList("B", "C", "D"), s.get().skip(1).peekFirst(firstElement::set).toList());
            assertEquals("B", firstElement.get());

            firstElement.set(null);
            assertEquals(asList(), s.get().skip(4).peekFirst(firstElement::set).toList());
            assertNull(firstElement.get());
        });
    }

    @Test
    public void testPeekLast() {
        List<String> input = asList("A", "B", "C", "D");
        AtomicReference<String> lastElement = new AtomicReference<>();
        assertEquals(asList("A", "B", "C"), StreamEx.of(input).peekLast(lastElement::set).limit(3).toList());
        assertNull(lastElement.get());

        assertEquals(input, StreamEx.of(input).peekLast(lastElement::set).limit(4).toList());
        assertEquals("D", lastElement.get());

        assertEquals(asList("A", "B", "C"), StreamEx.of(input).limit(3).peekLast(lastElement::set).toList());
        assertEquals("C", lastElement.get());

        lastElement.set(null);
        assertEquals(2L, StreamEx.of(input).peekLast(lastElement::set).indexOf("C").getAsLong());
        assertNull(lastElement.get());
        assertEquals(3L, StreamEx.of(input).peekLast(lastElement::set).indexOf("D").getAsLong());
        assertEquals("D", lastElement.get());
    }

    @Test
    public void testSplit() {
        streamEx(() -> StreamEx.split("", "abcd"), s -> assertEquals(1, s.get().count()));
        streamEx(() -> StreamEx.split("", Pattern.compile("abcd")), s -> assertEquals(1, s.get().count()));
        streamEx(() -> StreamEx.split("ab.cd...", '.'), s -> assertEquals("ab|cd", s.get().joining("|")));
        streamEx(() -> StreamEx.split("ab.cd...", "."), s -> assertEquals(0, s.get().count()));
        streamEx(() -> StreamEx.split("ab.cd...", "\\."), s -> assertEquals("ab|cd", s.get().joining("|")));
        streamEx(() -> StreamEx.split("ab.cd...", "cd"), s -> assertEquals("ab.|...", s.get().joining("|")));
        streamEx(() -> StreamEx.split("ab.cd...", "\\w"), s -> assertEquals("||.||...", s.get().joining("|")));
        streamEx(() -> StreamEx.split("ab.cd...", "\\W"), s -> assertEquals("ab|cd", s.get().joining("|")));
        streamEx(() -> StreamEx.split("ab|cd|e", "\\|"), s -> assertEquals("ab,cd,e", s.get().joining(",")));

        assertThrows(PatternSyntaxException.class, () -> StreamEx.split("a", "\\0"));
        asList('9', 'A', 'Z', 'z').forEach(ch ->
                assertEquals(asList("a" + ch + "a"), StreamEx.split("a" + ch + "a", "\\" + ch).toList()));
        assertEquals(asList("aaa"), StreamEx.split("aaa", "\\a").toList());
        asList(Character.MIN_HIGH_SURROGATE, Character.MAX_HIGH_SURROGATE).forEach(ch ->
                assertEquals(asList("a", "a"), StreamEx.split("a" + ch + "a", "\\" + ch).toList()));
    }

    @Test
    public void testSplitChar() {
        streamEx(() -> StreamEx.split("abcd,e,f,gh,,,i,j,kl,,,,,,", ','), s -> assertEquals("abcd|e|f|gh|||i|j|kl", s
                .get().joining("|")));
        streamEx(() -> StreamEx.split("abcd,e,f,gh,,,i,j,kl,,,,,,x", ','), s -> assertEquals(
            "abcd|e|f|gh|||i|j|kl||||||x", s.get().joining("|")));
        streamEx(() -> StreamEx.split("abcd", ','), s -> assertEquals("abcd", s.get().joining("|")));
        streamEx(() -> StreamEx.split("", ','), s -> assertEquals(asList(""), s.get().toList()));
        streamEx(() -> StreamEx.split(",,,,,,,,,", ','), s -> assertEquals(0, s.get().count()));

        withRandom(r -> repeat(10, iter -> {
            StringBuilder source = new StringBuilder(IntStreamEx.of(r, 0, 3).limit(r.nextInt(10000)).elements(
                new int[] { ',', 'a', 'b' }).charsToString());
            String[] expected = source.toString().split(",");
            String[] expectedFull = source.toString().split(",", -1);
            streamEx(() -> StreamEx.split(source, ','), s -> assertArrayEquals(expected, s.get().toArray(
                String[]::new)));
            streamEx(() -> StreamEx.split(source, ',', false), s -> assertArrayEquals(expectedFull, s.get().toArray(
                String[]::new)));
        }));
    }

    @Test
    public void testWithFirst() {
        repeat(10, i -> {
            streamEx(() -> StreamEx.of(0, 2, 4), s -> assertEquals(asList("0|0", "0|1", "0|2", "0|3", "0|4", "0|5"), s
                    .get().flatMap(x -> Stream.of(x, x + 1)).withFirst().mapKeyValue((a, b) -> a + "|" + b).toList()));

            // Check exception-friendliness: short-circuiting collectors use Exceptions for control flow
            streamEx(() -> IntStreamEx.range(100).boxed(), s -> {
                assertEquals("0|0, 0|1, 0|2, ...", s.get().withFirst((a, b) -> a + "|" + b).collect(Joining.with(", ")
                        .maxChars(18)));
                assertEquals(asList(), s.get().withFirst((a, b) -> a + "|" + b).collect(MoreCollectors.head(0)));
            });

            streamEx(() -> StreamEx.of("a", "b", "c", "d"), s -> assertEquals(Collections.singletonMap("a", asList("a",
                "b", "c", "d")), s.get().withFirst().grouping()));

            streamEx(() -> StreamEx.of("a", "b", "c", "d"), s -> assertEquals(asList("aa", "ab", "ac", "ad"), s.get()
                    .withFirst(String::concat).toList()));

            // Header mapping
            String input = "name,type,value\nID,int,5\nSurname,string,Smith\nGiven name,string,John";
            List<Map<String, String>> expected = asList(EntryStream.of("name", "ID", "type", "int", "value", "5")
                    .toMap(), EntryStream.of("name", "Surname", "type", "string", "value", "Smith").toMap(), EntryStream
                            .of("name", "Given name", "type", "string", "value", "John").toMap());
            streamEx(() -> StreamEx.ofLines(new StringReader(input)), s -> {
                assertEquals(expected, s.get().map(str -> str.split(",")).withFirst().skip(1).mapKeyValue((header,
                        row) -> EntryStream.zip(header, row).toMap()).toList());
                assertEquals(expected, s.get().map(str -> str.split(",")).withFirst((header, row) -> EntryStream.zip(
                    header, row).toMap()).skip(1).toList());
            });
        });
        Map<Integer, List<Integer>> expected = Collections.singletonMap(0, IntStreamEx.range(0, 10000).boxed()
                .toList());
        streamEx(() -> IntStreamEx.range(10000).boxed(), s -> assertEquals(expected, s.get().withFirst().grouping()));

        streamEx(() -> StreamEx.of(5, 10, 13, 12, 11), s -> assertEquals(asList("5+0", "5+5", "5+8", "5+7", "5+6"), s
                .get().withFirst((a, b) -> a + "+" + (b - a)).toList()));
    }

    @Test
    public void testZipWith() {
        List<String> input = asList("John", "Mary", "Jane", "Jimmy");
        Spliterator<String> spliterator = StreamEx.of(input).zipWith(IntStreamEx.range(1, Integer.MAX_VALUE).boxed(), (
                name, idx) -> idx + ". " + name).spliterator();
        assertEquals(4, spliterator.getExactSizeIfKnown());
        List<String> expected = asList("1. John", "2. Mary", "3. Jane", "4. Jimmy");
        streamEx(input::stream, s -> assertEquals(expected, s.get().zipWith(IntStream.range(1, Integer.MAX_VALUE)
                .boxed(), (name, idx) -> idx + ". " + name).toList()));
        streamEx(input::stream, s -> assertEquals(expected, s.get().zipWith(IntStream.range(1, Integer.MAX_VALUE)
                .boxed()).mapKeyValue((name, idx) -> idx + ". " + name).toList()));
        streamEx(() -> IntStream.range(1, Integer.MAX_VALUE).boxed(), s -> assertEquals(expected, s.get().zipWith(input
                .stream(), (idx, name) -> idx + ". " + name).toList()));
        streamEx(input::stream, s -> assertEquals(expected, s.get().zipWith(IntStreamEx.ints())
                .mapKeyValue((name, idx) -> (idx + 1) + ". " + name).toList()));
        streamEx(input::stream, s -> assertEquals(expected, s.get()
                .zipWith(IntStreamEx.ints(), (name, idx) -> (idx + 1) + ". " + name).toList()));
    }

    // Like Stream.generate(supplier)
    public static <T> StreamEx<T> generate(Supplier<T> supplier) {
        return StreamEx.produce(action -> {
            action.accept(supplier.get());
            return true;
        });
    }

    // Adapt spliterator to produce
    public static <T> StreamEx<T> fromSpliterator(Spliterator<T> spltr) {
        return StreamEx.produce((Predicate<Consumer<? super T>>) spltr::tryAdvance);
    }

    // Adapt iterator to produce
    public static <T> StreamEx<T> fromIterator(Iterator<T> iter) {
        return StreamEx.produce(action -> {
            if (!iter.hasNext())
                return false;
            action.accept(iter.next());
            return true;
        });
    }

    // Stream of all matches of given matcher
    public static StreamEx<String> matches(Matcher m) {
        return StreamEx.produce(action -> {
            if (!m.find())
                return false;
            action.accept(m.group());
            return true;
        });
    }

    public static <T> StreamEx<T> fromQueue(Queue<T> queue, T sentinel) {
        return StreamEx.produce(action -> {
            T next = queue.poll();
            if (next == null || next.equals(sentinel))
                return false;
            action.accept(next);
            return true;
        });
    }

    @Test
    public void testProduce() {
        assertEquals(asList(4, 4, 4, 4, 4), generate(() -> 4).limit(5).toList());
        assertEquals(asList("foo", "bar", "baz"), fromSpliterator(asList("foo", "bar", "baz").spliterator()).toList());
        assertEquals(asList("foo", "bar", "baz"), fromIterator(asList("foo", "bar", "baz").iterator()).toList());

        assertEquals(asList("123", "543", "111", "5432"), matches(Pattern.compile("\\d+").matcher("123 543,111:5432"))
                .toList());

        Queue<String> queue = new ArrayDeque<>(asList("one", "two", "STOP", "three", "four", "five", "STOP", "STOP",
            "six"));
        assertEquals(asList("one", "two"), fromQueue(queue, "STOP").toList());
        assertEquals(asList("three", "four", "five"), fromQueue(queue, "STOP").toList());
        assertEquals(asList(), fromQueue(queue, "STOP").toList());
        assertEquals(asList("six"), fromQueue(queue, "STOP").toList());
        assertEquals(asList(), fromQueue(queue, "STOP").toList());
    }

    @Test
    public void testPrefix() {
        List<String> input = asList("a", "b", "c", "d", "e");
        streamEx(input::stream, s -> assertEquals(asList("a", "ab", "abc", "abcd", "abcde"), s.get().prefix(
            String::concat).toList()));
        streamEx(input::stream, s -> assertEquals(Optional.of("abcd"), s.get().prefix(String::concat).findFirst(
            str -> str.length() > 3)));

        streamEx(() -> StreamEx.constant("a", 5), s -> assertEquals(new HashSet<>(asList("a", "aa", "aaa", "aaaa",
            "aaaaa")), s.get().prefix(String::concat).toSet()));
        streamEx(() -> StreamEx.constant("a", 5), s -> assertEquals(Optional.of("aaaaa"), s.get().prefix(String::concat)
                .findFirst(str -> str.length() > 4)));
        streamEx(() -> StreamEx.constant("a", 5), s -> assertEquals(Optional.empty(), s.get().prefix(String::concat)
                .findFirst(str -> str.length() > 6)));

        streamEx(() -> StreamEx.constant(100L, 10000), s -> assertEquals(5000500000L, (long) s.get().prefix(Long::sum)
                .reduce(0L, Long::sum)));

        streamEx(() -> IntStreamEx.range(10000).boxed().unordered(), s -> assertEquals(49995000, s.get().prefix(
            Integer::sum).mapToInt(Integer::intValue).max().getAsInt()));
    }

    /**
     * Returns maximal stream value short-circuiting when stopValue is reached
     *
     * @param stream stream to process
     * @param comparator comparator to compare stream values
     * @param stopValue value to short-circuit at
     * @return optional describing maximal value or empty optional if the input
     *         stream is empty
     */
    static <T> Optional<T> maxWithStop(StreamEx<T> stream, Comparator<T> comparator, T stopValue) {
        return stream.prefix(BinaryOperator.maxBy(comparator)).takeWhileInclusive(Predicate.isEqual(stopValue).negate())
                .collect(MoreCollectors.last());
    }

    @Test
    public void testMaxWithStop() {
        // Infinite stream, stop is reached
        assertEquals(Optional.of(1000), maxWithStop(IntStreamEx.of(new Random(1), 0, 1001).boxed(), Comparator
                .naturalOrder(), 1000));
        // Finite stream, stop is not reached
        assertEquals(Optional.of(999), maxWithStop(IntStreamEx.of(new Random(1), 10000, 0, 1000).boxed(), Comparator
                .naturalOrder(), 1001));
    }

    @Test
    public void testToImmutableList() {
        List<Integer> expected = asList(1, 2, 3);
        repeat(4, n -> streamEx(() -> IntStreamEx.range(4).atLeast(n).boxed(), s -> {
            List<Integer> list = s.get().toImmutableList();
            assertEquals(expected.subList(n - 1, expected.size()), list);
            assertThrows(UnsupportedOperationException.class, () -> list.add(0));
            assertThrows(UnsupportedOperationException.class, () -> list.set(0, 0));
        }));
    }

    @Test
    public void testToImmutableSet() {
        List<Integer> expected = asList(1, 2, 3);
        repeat(4, n -> streamEx(() -> IntStreamEx.range(4).atLeast(n).boxed(), s -> {
            Set<Integer> set = s.get().toImmutableSet();
            assertEquals(new HashSet<>(expected.subList(n - 1, expected.size())), set);
            assertThrows(UnsupportedOperationException.class, () -> set.add(-1));
        }));
    }

    @Test
    public void testInto() {
        for (List<Number> list : asList(new ArrayList<Number>(), new LinkedList<Number>())) {
            List<Number> res = StreamEx.of(1, 2, 3, 4).filter(x -> x < 4).into(list);
            assertSame(list, res);
            assertEquals(asList(1, 2, 3), list);
            assertSame(list, StreamEx.of(4, 5, 6).into(list));
            assertEquals(asList(1, 2, 3, 4, 5, 6), list);
            assertSame(list, StreamEx.of(7, 8, 9, 10).parallel().into(list));
            assertEquals(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), list);
        }
        // a kind of mock object
        Collection<String> c = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;
            int size = Integer.MAX_VALUE - 10;

            @Override
            public boolean add(String e) {
                assertEquals("a", e);
                size++;
                return true;
            }

            @Override
            public int size() {
                return size;
            }
        };
        assertSame(c, StreamEx.constant("a", 20).into(c));
        //noinspection NumericOverflow
        assertEquals(Integer.MAX_VALUE + 10, c.size());
    }

    @Test
    public void testIntoArrayListEnsureCapacityOptimization() {
        CapacityEnsuredAssertingArrayList<Integer> list = new CapacityEnsuredAssertingArrayList<>();
        Collection<Integer> collection = list;
        StreamEx.<Integer>of().into(collection);
        StreamEx.of(asList(1)).into(collection);
        int maxAvailableSize = Integer.MAX_VALUE - collection.size();
        assertThrows(IllegalArgumentException.class,
                     () -> StreamEx.<Integer>of(emptySpliteratorWithExactSize(-2)).into(collection));
        assertThrows(OutOfMemoryError.class,
                     () -> StreamEx.<Integer>of(emptySpliteratorWithExactSize(Long.MAX_VALUE)).into(collection));
        StreamEx.<Integer>of(emptySpliteratorWithExactSize(maxAvailableSize + 1)).into(collection);
        StreamEx.<Integer>of(emptySpliteratorWithExactSize(maxAvailableSize)).into(collection);
        assertEquals(Integer.MAX_VALUE, list.ensuredCapacity);
    }

    private static class CapacityEnsuredAssertingArrayList<E> extends ArrayList<E> {
        private int ensuredCapacity = 0;

        @Override
        public boolean add(E element) {
            if (size() >= ensuredCapacity)
                fail("ArrayList capacity was not ensured");
            return super.add(element);
        }

        @Override
        public void ensureCapacity(int minCapacity) {
            if (minCapacity <= ensuredCapacity)
                fail("Not required ensureCapacity call");
            ensuredCapacity = minCapacity;
        }
    }

    @Test
    public void testFilterBy() {
        assertEquals(3, StreamEx.of("a", "bb", "c", "e", "ddd").filterBy(String::length, 1).count());
        assertEquals(2, StreamEx.of("a", "bb", "c", "e", "ddd").filterBy(x -> x.length() > 1 ? null : x, null).count());
    }

    @Test
    public void testRemoveBy() {
        assertEquals(2, StreamEx.of("a", "bb", "c", "e", "ddd").removeBy(String::length, 1).count());
        assertEquals(3, StreamEx.of("a", "bb", "c", "e", "ddd").removeBy(x -> x.length() > 1 ? null : x, null).count());
    }

    @Test
    public void testIntersperse() {
        List<String> expected = asList("a", "--", "b", "--", "c", "--", "d", "--", "e");
        streamEx(asList("a", "b", "c", "d", "e")::stream, s -> assertEquals(expected, s.get().intersperse("--").toList()));
        assertEquals(Collections.emptyList(), StreamEx.empty().intersperse("xyz").toList());
    }

    @Test
    public void testIfEmpty() {
        repeat(10, n -> streamEx(asList(1, 2, 3, 4, 5, 6)::stream, s -> {
            assertEquals("123456", s.get().ifEmpty(7, 8, 9).joining());
            assertEquals("123456", s.get().filter(x -> x > 0).ifEmpty(7, 8, 9).joining());
            assertEquals("6", s.get().filter(x -> x > 5).ifEmpty(7, 8, 9).joining());
            assertEquals("789", s.get().filter(x -> x < 0).ifEmpty(7, 8, 9).joining());
            assertEquals("123456", s.get().ifEmpty(s.get()).joining());
            assertEquals("123456", s.get().filter(x -> x > 0).ifEmpty(s.get().filter(x -> x % 2 == 1)).joining());
            assertEquals("135", s.get().filter(x -> x < 0).ifEmpty(s.get().filter(x -> x % 2 == 1)).joining());
            assertEquals("", s.get().filter(x -> x < 0).ifEmpty(s.get().filter(x -> x < 0)).joining());

            assertEquals(Optional.of(1), s.get().ifEmpty(7, 8, 9).findFirst());
            assertEquals(Optional.of(1), s.get().filter(x -> x > 0).ifEmpty(7, 8, 9).findFirst());
            assertEquals(Optional.of(6), s.get().filter(x -> x > 5).ifEmpty(7, 8, 9).findFirst());
            assertEquals(Optional.of(7), s.get().filter(x -> x < 0).ifEmpty(7, 8, 9).findFirst());
            assertEquals(Optional.of(1), s.get().ifEmpty(s.get()).findFirst());
            assertEquals(Optional.of(1), s.get().filter(x -> x > 0).ifEmpty(s.get().filter(x -> x % 2 == 1)).findFirst());
            assertEquals(Optional.of(1), s.get().filter(x -> x < 0).ifEmpty(s.get().filter(x -> x % 2 == 1)).findFirst());
            assertEquals(Optional.empty(), s.get().filter(x -> x < 0).ifEmpty(s.get().filter(x -> x < 0)).findFirst());

            assertEquals(Optional.of(1), s.get().ifEmpty().findFirst());
            assertEquals(Optional.of(1), StreamEx.empty().ifEmpty(s.get()).findFirst());
        }));
    }

    @Test
    public void testOfCombinations() {
        List<String> expectedN5K3 = asList("[0, 1, 2]", "[0, 1, 3]", "[0, 1, 4]", "[0, 2, 3]", "[0, 2, 4]", "[0, 3, 4]", "[1, 2, 3]", "[1, 2, 4]", "[1, 3, 4]", "[2, 3, 4]");
        List<String> expectedN5K2 = asList("[0, 1]", "[0, 2]", "[0, 3]", "[0, 4]", "[1, 2]", "[1, 3]", "[1, 4]", "[2, 3]", "[2, 4]", "[3, 4]");

        streamEx(() -> StreamEx.ofCombinations(5, 3), s ->
                assertEquals(expectedN5K3, s.get().map(Arrays::toString).collect(Collectors.toList())));
        streamEx(() -> StreamEx.ofCombinations(20, 7), s ->
                assertEquals(Optional.empty(), s.get().map(Arrays::toString).distinct(2).findFirst()));
        streamEx(() -> StreamEx.ofCombinations(7, 20), s ->
                assertEquals(Optional.empty(), s.get().map(Arrays::toString).findFirst()));
        streamEx(() -> StreamEx.ofCombinations(5, 2), s ->
                assertEquals(expectedN5K2, s.get().map(Arrays::toString).collect(Collectors.toList())));
        streamEx(() -> StreamEx.ofCombinations(5, 0), s ->
                assertEquals(asList("[]"), s.get().map(Arrays::toString).collect(Collectors.toList())));
        streamEx(() -> StreamEx.ofCombinations(5, 5), s ->
                assertEquals(asList("[0, 1, 2, 3, 4]"), s.get().map(Arrays::toString).collect(Collectors.toList())));

        assertThrows(IllegalArgumentException.class, () -> StreamEx.ofCombinations(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> StreamEx.ofCombinations(0, -1));
        assertThrows(UnsupportedOperationException.class, () -> StreamEx.ofCombinations(1000, 500));
    }

    @Test
    public void testMapToEntry() {
        assertEquals(Collections.singletonMap(1, "a"),
                StreamEx.of("a").mapToEntry(String::length, Function.identity()).toMap());
    }

    @Test
    public void testFlatMapToEntry() {
        assertEquals(Collections.singletonMap(1, "a"),
                StreamEx.of("a").flatMapToEntry(s -> Collections.singletonMap(s.length(), s)).toMap());
    }

    @Test
    public void testMapToEntryPartial() {
        Function<Integer, Optional<String>> literalOf = num -> num == 1
                ? Optional.of("one")
                : Optional.empty();

        List<Integer> original = asList(1, 2, 3, 4);
        Map<Integer, String> expected = Collections.singletonMap(1, "one");
        streamEx(original::stream, s -> assertEquals(expected, s.get().mapToEntryPartial(literalOf).toMap()));
    }

    @Test
    public void testConcurrentGroupingBy() {
        StreamEx.of("a", "b").parallel().groupingBy(String::length, secondConcurrentAddAssertingCollector("a", "b"));
        StreamEx.of("x", "y").parallel()
                .groupingBy(String::length, ConcurrentHashMap::new, secondConcurrentAddAssertingCollector("x", "y"));
    }

    static <T> Collector<T, ?, ?> secondConcurrentAddAssertingCollector(T first, T second) {
        return Collector.<T, Exchanger<T>>of(
                Exchanger::new,
                (exchanger, t) -> {
                    T t1;
                    try {
                        t1 = exchanger.exchange(t, 1, TimeUnit.SECONDS);
                    } catch (InterruptedException | TimeoutException e) {
                        throw new AssertionError("Unexpected exception: ", e);
                    }
                    assertTrue((t1.equals(first) && t.equals(second) || t1.equals(second) && t.equals(first)));
                },
                (a, b) -> {
                    throw new AssertionError(
                            "Combining is not expected within secondConcurrentAddAssertingCollector");
                },
                Collector.Characteristics.CONCURRENT,
                Collector.Characteristics.UNORDERED,
                Collector.Characteristics.IDENTITY_FINISH);
    }

    @Test
    public void testReduceWithZero() {
        streamEx(() -> IntStreamEx.range(1, Integer.MAX_VALUE).boxed(), s -> {
            assertEquals(Optional.of(0), s.get().reduceWithZero(0, (a, b) -> a * b));
            assertEquals((Integer) 0, s.get().reduceWithZero(0, 1, (a, b) -> a * b));
        });
    }

    @Test
    public void testCount() {
        assertEquals(3L, StreamEx.of("x", "", "y", "", "z").count(x -> !x.isEmpty()));
    }
    
    @Test
    public void testIterateUntilNull() {
        StreamEx<@NonNull Integer> integers = StreamEx.iterateUntilNull(1, x -> x < 6 ? x + 1 : null);
        assertEquals(asList(1, 2, 3, 4, 5, 6), integers.toList());
        assertEquals(asList(), 
                StreamEx.iterateUntilNull((Integer)null, x -> x < 6 ? x + 1 : null).map(Object::toString).toList());
    }
    
    @Test
    public void testTakeUntilNull() {
        List<String> list = StreamEx.of(1, 2, 3, null, 4, 5, 6, null)
                .takeUntilNull()
                .map(Object::toString)
                .toList();
        assertEquals(asList("1", "2", "3"), list);
    }
}
