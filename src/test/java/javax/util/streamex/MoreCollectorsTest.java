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

import static org.junit.Assert.*;
import static javax.util.streamex.TestHelpers.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.Collector.Characteristics;

import javax.util.streamex.StreamExInternals.BooleanMap;

import org.junit.Test;

public class MoreCollectorsTest {
    @Test(expected = UnsupportedOperationException.class)
    public void testInstantiate() throws Throwable {
        Constructor<MoreCollectors> constructor = MoreCollectors.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testToArray() {
        List<String> input = Arrays.asList("a", "bb", "c", "", "cc", "eee", "bb", "ddd");
        for (StreamSupplier<String, StreamEx<String>> supplier : suppliers(() -> StreamEx.of(input))) {
            Map<Integer, String[]> result = supplier.get().groupingBy(String::length, HashMap::new,
                MoreCollectors.toArray(String[]::new));
            assertArrayEquals(supplier.toString(), new String[] { "" }, result.get(0));
            assertArrayEquals(supplier.toString(), new String[] { "a", "c" }, result.get(1));
            assertArrayEquals(supplier.toString(), new String[] { "bb", "cc", "bb" }, result.get(2));
            assertArrayEquals(supplier.toString(), new String[] { "eee", "ddd" }, result.get(3));
        }
    }

    @Test
    public void testDistinctCount() {
        List<String> input = Arrays.asList("a", "bb", "c", "cc", "eee", "bb", "bc", "ddd");
        for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
            Map<String, Integer> result = supplier.get().groupingBy(s -> s.substring(0, 1), HashMap::new,
                MoreCollectors.distinctCount(String::length));
            assertEquals(1, (int) result.get("a"));
            assertEquals(1, (int) result.get("b"));
            assertEquals(2, (int) result.get("c"));
            assertEquals(1, (int) result.get("d"));
            assertEquals(1, (int) result.get("e"));
        }
    }

    @Test
    public void testDistinctBy() {
        List<String> input = Arrays.asList("a", "bb", "c", "cc", "eee", "bb", "bc", "ddd", "ca", "ce", "cf", "ded",
            "dump");
        for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
            Map<String, List<String>> result = supplier.get().groupingBy(s -> s.substring(0, 1), HashMap::new,
                MoreCollectors.distinctBy(String::length));
            assertEquals(supplier.toString(), Arrays.asList("a"), result.get("a"));
            assertEquals(supplier.toString(), Arrays.asList("bb"), result.get("b"));
            assertEquals(supplier.toString(), Arrays.asList("c", "cc"), result.get("c"));
            assertEquals(supplier.toString(), Arrays.asList("ddd", "dump"), result.get("d"));
            assertEquals(supplier.toString(), Arrays.asList("eee"), result.get("e"));
        }
    }

    @Test
    public void testMaxAll() {
        List<String> input = Arrays.asList("a", "bb", "c", "", "cc", "eee", "bb", "ddd");
        checkCollector("maxAll", Arrays.asList("eee", "ddd"), input::stream,
            MoreCollectors.maxAll(Comparator.comparingInt(String::length)));
        Collector<String, ?, String> maxAllJoin = MoreCollectors.maxAll(Comparator.comparingInt(String::length),
            Collectors.joining(","));
        checkCollector("maxAllJoin", "eee,ddd", input::stream, maxAllJoin);
        checkCollector("minAll", 1L, input::stream,
            MoreCollectors.minAll(Comparator.comparingInt(String::length), Collectors.counting()));
        checkCollector("minAllEmpty", Arrays.asList(""), input::stream,
            MoreCollectors.minAll(Comparator.comparingInt(String::length)));
        checkCollectorEmpty("maxAll", Collections.emptyList(),
            MoreCollectors.maxAll(Comparator.comparingInt(String::length)));
        checkCollectorEmpty("maxAllJoin", "", maxAllJoin);

        List<Integer> ints = IntStreamEx.of(new Random(1), 10000, 1, 1000).boxed().toList();
        List<Integer> expectedMax = getMaxAll(ints, Comparator.naturalOrder());
        List<Integer> expectedMin = getMaxAll(ints, Comparator.reverseOrder());
        Collector<Integer, ?, SimpleEntry<Integer, Long>> downstream = MoreCollectors.pairing(MoreCollectors.first(),
            Collectors.counting(), (opt, cnt) -> new AbstractMap.SimpleEntry<>(opt.get(), cnt));

        checkCollector("maxAll", expectedMax, ints::stream, MoreCollectors.maxAll(Integer::compare));
        checkCollector("minAll", expectedMin, ints::stream, MoreCollectors.minAll());
        checkCollector("entry", new SimpleEntry<>(expectedMax.get(0), (long) expectedMax.size()), ints::stream,
            MoreCollectors.maxAll(downstream));
        checkCollector("entry", new SimpleEntry<>(expectedMin.get(0), (long) expectedMin.size()), ints::stream,
            MoreCollectors.minAll(downstream));

        Integer a = new Integer(1), b = new Integer(1), c = new Integer(1000), d = new Integer(1000);
        ints = IntStreamEx.range(10, 100).boxed().append(a, c).prepend(b, d).toList();
        for (StreamExSupplier<Integer> supplier : streamEx(ints::stream)) {
            List<Integer> list = supplier.get().collect(MoreCollectors.maxAll());
            assertEquals(2, list.size());
            assertSame(d, list.get(0));
            assertSame(c, list.get(1));

            list = supplier.get().collect(MoreCollectors.minAll());
            assertEquals(2, list.size());
            assertSame(b, list.get(0));
            assertSame(a, list.get(1));
        }
    }

    private List<Integer> getMaxAll(List<Integer> ints, Comparator<Integer> c) {
        List<Integer> expectedMax = null;
        for (Integer i : ints) {
            if (expectedMax == null || c.compare(i, expectedMax.get(0)) > 0) {
                expectedMax = new ArrayList<>();
                expectedMax.add(i);
            } else if (i.equals(expectedMax.get(0))) {
                expectedMax.add(i);
            }
        }
        return expectedMax;
    }

    @Test
    public void testFirstLast() {
        Supplier<Stream<Integer>> s = () -> IntStreamEx.range(1000).boxed();
        checkShortCircuitCollector("first", Optional.of(0), 1, s, MoreCollectors.first());
        checkShortCircuitCollector("firstLong", Optional.of(0), 1,
            () -> Stream.of(1).flatMap(x -> IntStream.range(0, 1000000000).boxed()), MoreCollectors.first(), true);
        checkShortCircuitCollector("first", Optional.of(1), 1, () -> Stream.iterate(1, x -> x + 1),
            MoreCollectors.first(), true);
        assertEquals(1, (int) StreamEx.iterate(1, x -> x + 1).parallel().collect(MoreCollectors.first()).get());

        checkCollector("last", Optional.of(999), s, MoreCollectors.last());
        checkCollectorEmpty("first", Optional.empty(), MoreCollectors.first());
        checkCollectorEmpty("last", Optional.empty(), MoreCollectors.last());
    }

    @Test
    public void testHeadParallel() {
        List<Integer> expected = IntStreamEx.range(0, 2000, 2).boxed().toList();
        List<Integer> expectedShort = Arrays.asList(0, 1);
        for (int i = 0; i < 1000; i++) {
            assertEquals("#" + i, expectedShort,
                IntStreamEx.range(1000).boxed().parallel().collect(MoreCollectors.head(2)));
            assertEquals("#" + i, expected, IntStreamEx.range(10000).boxed().parallel().filter(x -> x % 2 == 0)
                    .collect(MoreCollectors.head(1000)));
        }
        assertEquals(expectedShort, StreamEx.iterate(0, x -> x + 1).parallel().collect(MoreCollectors.head(2)));
    }

    @Test
    public void testHeadTail() {
        List<Integer> ints = IntStreamEx.range(1000).boxed().toList();
        checkShortCircuitCollector("tail(0)", Arrays.asList(), 0, ints::stream, MoreCollectors.tail(0));
        checkCollector("tail(1)", Arrays.asList(999), ints::stream, MoreCollectors.tail(1));
        checkCollector("tail(2)", Arrays.asList(998, 999), ints::stream, MoreCollectors.tail(2));
        checkCollector("tail(500)", ints.subList(500, 1000), ints::stream, MoreCollectors.tail(500));
        checkCollector("tail(999)", ints.subList(1, 1000), ints::stream, MoreCollectors.tail(999));
        checkCollector("tail(1000)", ints, ints::stream, MoreCollectors.tail(1000));
        checkCollector("tail(MAX)", ints, ints::stream, MoreCollectors.tail(Integer.MAX_VALUE));

        checkShortCircuitCollector("head(0)", Arrays.asList(), 0, ints::stream, MoreCollectors.head(0));
        checkShortCircuitCollector("head(1)", Arrays.asList(0), 1, ints::stream, MoreCollectors.head(1));
        checkShortCircuitCollector("head(2)", Arrays.asList(0, 1), 2, ints::stream, MoreCollectors.head(2));
        checkShortCircuitCollector("head(500)", ints.subList(0, 500), 500, ints::stream, MoreCollectors.head(500));
        checkShortCircuitCollector("head(999)", ints.subList(0, 999), 999, ints::stream, MoreCollectors.head(999));
        checkShortCircuitCollector("head(1000)", ints, 1000, ints::stream, MoreCollectors.head(1000));
        checkShortCircuitCollector("head(MAX)", ints, 1000, ints::stream, MoreCollectors.head(Integer.MAX_VALUE));

        checkShortCircuitCollector("head(10000)", IntStreamEx.rangeClosed(1, 10000).boxed().toList(), 10000,
            () -> Stream.iterate(1, x -> x + 1), MoreCollectors.head(10000), true);
    }

    @Test
    public void testGreatest() {
        List<Integer> ints = IntStreamEx.of(new Random(1), 1000, 1, 1000).boxed().toList();
        List<Integer> sorted = StreamEx.of(ints).sorted().toList();
        List<Integer> revSorted = StreamEx.of(ints).reverseSorted().toList();
        Comparator<Integer> byString = Comparator.comparing(String::valueOf);
        checkShortCircuitCollector("least(0)", Collections.emptyList(), 0, ints::stream, MoreCollectors.least(0));
        checkCollector("least(5)", sorted.subList(0, 5), ints::stream, MoreCollectors.least(5));
        checkCollector("least(20)", sorted.subList(0, 20), ints::stream, MoreCollectors.least(20));
        checkCollector("least(MAX)", sorted, ints::stream, MoreCollectors.least(Integer.MAX_VALUE));
        checkCollector("least(byString, 20)", StreamEx.of(ints).sorted(byString).limit(20).toList(), ints::stream,
            MoreCollectors.least(byString, 20));

        checkShortCircuitCollector("greatest(0)", Collections.emptyList(), 0, ints::stream, MoreCollectors.greatest(0));
        checkCollector("greatest(5)", revSorted.subList(0, 5), ints::stream, MoreCollectors.greatest(5));
        checkCollector("greatest(20)", revSorted.subList(0, 20), ints::stream, MoreCollectors.greatest(20));
        checkCollector("greatest(MAX)", revSorted, ints::stream, MoreCollectors.greatest(Integer.MAX_VALUE));
        checkCollector("greatest(byString, 20)", StreamEx.of(ints).reverseSorted(byString).limit(20).toList(),
            ints::stream, MoreCollectors.greatest(byString, 20));

        Supplier<Stream<Integer>> s = () -> IntStreamEx.range(100).boxed();
        checkCollector("1", IntStreamEx.range(1).boxed().toList(), s, MoreCollectors.least(1));
        checkCollector("2", IntStreamEx.range(2).boxed().toList(), s, MoreCollectors.least(2));
        checkCollector("10", IntStreamEx.range(10).boxed().toList(), s, MoreCollectors.least(10));
        checkCollector("100", IntStreamEx.range(100).boxed().toList(), s, MoreCollectors.least(100));
        checkCollector("200", IntStreamEx.range(100).boxed().toList(), s, MoreCollectors.least(200));
    }

    @Test
    public void testCountingInt() {
        checkCollector("counting", 1000, () -> IntStreamEx.range(1000).boxed(), MoreCollectors.countingInt());
        checkCollectorEmpty("counting", 0, MoreCollectors.countingInt());
    }

    @Test
    public void testMinIndex() {
        List<Integer> ints = IntStreamEx.of(new Random(1), 1000, 5, 47).boxed().toList();
        long expectedMin = IntStreamEx.ofIndices(ints).minBy(ints::get).getAsInt();
        long expectedMax = IntStreamEx.ofIndices(ints).maxBy(ints::get).getAsInt();
        long expectedMinString = IntStreamEx.ofIndices(ints).minBy(i -> String.valueOf(ints.get(i))).getAsInt();
        long expectedMaxString = IntStreamEx.ofIndices(ints).maxBy(i -> String.valueOf(ints.get(i))).getAsInt();
        Comparator<Integer> cmp = Comparator.comparing(String::valueOf);
        checkCollector("minIndex", OptionalLong.of(expectedMin), ints::stream, MoreCollectors.minIndex());
        checkCollector("maxIndex", OptionalLong.of(expectedMax), ints::stream, MoreCollectors.maxIndex());
        checkCollector("minIndex", OptionalLong.of(expectedMinString), ints::stream, MoreCollectors.minIndex(cmp));
        checkCollector("maxIndex", OptionalLong.of(expectedMaxString), ints::stream, MoreCollectors.maxIndex(cmp));
        Supplier<Stream<String>> supplier = () -> ints.stream().map(Object::toString);
        checkCollector("minIndex", OptionalLong.of(expectedMinString), supplier, MoreCollectors.minIndex());
        checkCollector("maxIndex", OptionalLong.of(expectedMaxString), supplier, MoreCollectors.maxIndex());
        checkCollectorEmpty("minIndex", OptionalLong.empty(), MoreCollectors.<String> minIndex());
        checkCollectorEmpty("maxIndex", OptionalLong.empty(), MoreCollectors.<String> maxIndex());
    }

    @Test
    public void testGroupingByEnum() {
        EnumMap<TimeUnit, Long> expected = new EnumMap<>(TimeUnit.class);
        EnumSet.allOf(TimeUnit.class).forEach(tu -> expected.put(tu, 0L));
        expected.put(TimeUnit.SECONDS, 1L);
        expected.put(TimeUnit.DAYS, 2L);
        expected.put(TimeUnit.NANOSECONDS, 1L);
        checkCollector("groupingByEnum", expected,
            () -> Stream.of(TimeUnit.SECONDS, TimeUnit.DAYS, TimeUnit.DAYS, TimeUnit.NANOSECONDS),
            MoreCollectors.groupingByEnum(TimeUnit.class, Function.identity(), Collectors.counting()));
    }

    @Test(expected = IllegalStateException.class)
    public void testGroupingByWithDomainException() {
        List<Integer> list = Arrays.asList(1, 2, 20, 3, 31, 4);
        Collector<Integer, ?, Map<Integer, List<Integer>>> c = MoreCollectors.groupingBy(i -> i % 10,
            StreamEx.of(0, 1, 2, 3).toSet(), Collectors.toList());
        Map<Integer, List<Integer>> map = list.stream().collect(c);
        System.out.println(map);
    }

    @Test
    public void testGroupingByWithDomain() {
        List<String> data = Arrays.asList("a", "foo", "test", "ququq", "bar", "blahblah");
        Collector<String, ?, String> collector = MoreCollectors.collectingAndThen(MoreCollectors.groupingBy(
            String::length, IntStreamEx.range(10).boxed().toSet(), TreeMap::new, MoreCollectors.first()),
            Object::toString);
        checkShortCircuitCollector("groupingWithDomain",
            "{0=Optional.empty, 1=Optional[a], 2=Optional.empty, 3=Optional[foo], 4=Optional[test], 5=Optional[ququq], "
                + "6=Optional.empty, 7=Optional.empty, 8=Optional[blahblah], 9=Optional.empty}", data.size(),
            data::stream, collector);

        Map<String, String> name2sex = new LinkedHashMap<>();
        name2sex.put("Mary", "Girl");
        name2sex.put("John", "Boy");
        name2sex.put("James", "Boy");
        name2sex.put("Lucie", "Girl");
        name2sex.put("Fred", "Boy");
        name2sex.put("Thomas", "Boy");
        name2sex.put("Jane", "Girl");
        name2sex.put("Ruth", "Girl");
        name2sex.put("Melanie", "Girl");
        Collector<Entry<String, String>, ?, Map<String, List<String>>> groupingBy = MoreCollectors.groupingBy(
            Entry::getValue, StreamEx.of("Girl", "Boy").toSet(),
            MoreCollectors.mapping(Entry::getKey, MoreCollectors.head(2)));
        AtomicInteger counter = new AtomicInteger();
        Map<String, List<String>> map = EntryStream.of(name2sex).peek(c -> counter.incrementAndGet())
                .collect(groupingBy);
        assertEquals(Arrays.asList("Mary", "Lucie"), map.get("Girl"));
        assertEquals(Arrays.asList("John", "James"), map.get("Boy"));
        assertEquals(4, counter.get());
    }

    @Test
    public void testToBooleanArray() {
        List<Integer> input = IntStreamEx.of(new Random(1), 1000, 1, 100).boxed().toList();
        boolean[] expected = new boolean[input.size()];
        for (int i = 0; i < expected.length; i++)
            expected[i] = input.get(i) > 50;
        for (StreamExSupplier<Integer> supplier : streamEx(input::stream)) {
            assertArrayEquals(supplier.toString(), expected,
                supplier.get().collect(MoreCollectors.toBooleanArray(x -> x > 50)));
        }
    }

    @Test
    public void testPartitioningBy() {
        Collector<Integer, ?, Map<Boolean, Optional<Integer>>> by20 = MoreCollectors.partitioningBy(x -> x % 20 == 0,
            MoreCollectors.first());
        Collector<Integer, ?, Map<Boolean, Optional<Integer>>> by200 = MoreCollectors.partitioningBy(x -> x % 200 == 0,
            MoreCollectors.first());
        Supplier<Stream<Integer>> supplier = () -> IntStreamEx.range(1, 100).boxed();
        checkShortCircuitCollector("by20", new BooleanMap<>(Optional.of(20), Optional.of(1)), 20, supplier, by20);
        checkShortCircuitCollector("by200", new BooleanMap<>(Optional.empty(), Optional.of(1)), 99, supplier, by200);
    }

    @Test
    public void testMapping() {
        List<String> input = Arrays.asList("Capital", "lower", "Foo", "bar");
        Collector<String, ?, Map<Boolean, Optional<Integer>>> collector = MoreCollectors
                .partitioningBy(str -> Character.isUpperCase(str.charAt(0)),
                    MoreCollectors.mapping(String::length, MoreCollectors.first()));
        checkShortCircuitCollector("mapping", new BooleanMap<>(Optional.of(7), Optional.of(5)), 2, input::stream,
            collector);
        Collector<String, ?, Map<Boolean, Optional<Integer>>> collectorLast = MoreCollectors.partitioningBy(
            str -> Character.isUpperCase(str.charAt(0)), MoreCollectors.mapping(String::length, MoreCollectors.last()));
        checkCollector("last", new BooleanMap<>(Optional.of(3), Optional.of(3)), input::stream, collectorLast);

        input = Arrays.asList("Abc", "Bac", "Aac", "Abv", "Bbc", "Bgd", "Atc", "Bpv");
        Map<Character, List<String>> expected = EntryStream.of('A', Arrays.asList("Abc", "Aac"), 'B',
            Arrays.asList("Bac", "Bbc")).toMap();
        AtomicInteger cnt = new AtomicInteger();
        Collector<String, ?, Map<Character, List<String>>> groupMap = Collectors.groupingBy(s -> s.charAt(0),
            MoreCollectors.mapping(x -> {
                cnt.incrementAndGet();
                return x;
            }, MoreCollectors.head(2)));
        checkCollector("groupMap", expected, input::stream, groupMap);
        cnt.set(0);
        assertEquals(expected, input.stream().collect(groupMap));
        assertEquals(4, cnt.get());
    }

    @Test
    public void testIntersecting() {
        for (int i = 0; i < 5; i++) {
            List<List<String>> input = Arrays.asList(Arrays.asList("aa", "bb", "cc"), Arrays.asList("cc", "bb", "dd"),
                Arrays.asList("ee", "dd"), Arrays.asList("aa", "bb", "dd"));
            checkShortCircuitCollector("#" + i, Collections.emptySet(), 3, input::stream, MoreCollectors.intersecting());
            List<List<Integer>> copies = new ArrayList<>(Collections.nCopies(100, Arrays.asList(1, 2)));
            checkShortCircuitCollector("#" + i, StreamEx.of(1, 2).toSet(), 100, copies::stream,
                MoreCollectors.intersecting());
            copies.addAll(Collections.nCopies(100, Arrays.asList(3)));
            checkShortCircuitCollector("#" + i, Collections.emptySet(), 101, copies::stream,
                MoreCollectors.intersecting());
            checkCollectorEmpty("#" + i, Collections.emptySet(), MoreCollectors.intersecting());
        }
    }

    @Test
    public void testAndInt() {
        List<Integer> ints = Arrays.asList(0b1100, 0b0110, 0b101110, 0b11110011);
        Collector<Integer, ?, OptionalInt> collector = MoreCollectors.andingInt(Integer::intValue);
        checkShortCircuitCollector("andInt", OptionalInt.of(0), 4, ints::stream, collector);
        checkCollectorEmpty("andIntEmpty", OptionalInt.empty(), collector);
        assertEquals(OptionalInt.of(0), IntStreamEx.iterate(16384, i -> i + 1).parallel().boxed().collect(collector));
        assertEquals(OptionalInt.of(16384), IntStreamEx.iterate(16384, i -> i + 1).parallel().limit(16383).boxed()
                .collect(collector));
        Collector<Integer, ?, Integer> unwrapped = MoreCollectors.collectingAndThen(
            MoreCollectors.andingInt(Integer::intValue), OptionalInt::getAsInt);
        assertTrue(unwrapped.characteristics().contains(Characteristics.UNORDERED));
        checkShortCircuitCollector("andIntUnwrapped", 0, 4, ints::stream, unwrapped);
        checkShortCircuitCollector("andIntUnwrapped", 0, 2, Arrays.asList(0x1, 0x10, 0x100)::stream, unwrapped);
    }

    @Test
    public void testAndLong() {
        List<Long> longs = Arrays.asList(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFF00000000L, 0xFFFFFFFF0000L);
        checkShortCircuitCollector("andLong", OptionalLong.of(0xFFFF00000000L), 3, longs::stream,
            MoreCollectors.andingLong(Long::longValue));
        longs = Arrays.asList(1L, 2L, 3L, 4L);
        checkShortCircuitCollector("andLong", OptionalLong.of(0), 2, longs::stream,
            MoreCollectors.andingLong(Long::longValue));
        checkCollectorEmpty("andLongEmpty", OptionalLong.empty(), MoreCollectors.andingLong(Long::longValue));
    }

    @Test
    public void testAndLongFlatMap() {
        checkShortCircuitCollector("andLongFlat", OptionalLong.of(0), 2,
            () -> LongStreamEx.of(0).flatMap(x -> LongStream.range(1, 100000000)).boxed(),
            MoreCollectors.andingLong(Long::longValue), true);
    }

    @Test
    public void testFiltering() {
        Collector<Integer, ?, Optional<Integer>> firstEven = MoreCollectors.filtering(x -> x % 2 == 0,
            MoreCollectors.first());
        Collector<Integer, ?, Optional<Integer>> firstOdd = MoreCollectors.filtering(x -> x % 2 != 0,
            MoreCollectors.first());
        Collector<Integer, ?, Integer> sumOddEven = MoreCollectors.pairing(firstEven, firstOdd,
            (e, o) -> e.get() + o.get());
        List<Integer> ints = Arrays.asList(1, 3, 5, 7, 9, 10, 8, 6, 4, 2, 3, 7, 11);
        checkShortCircuitCollector("sumOddEven", 11, 6, ints::stream, sumOddEven);
        Collector<Integer, ?, Long> countEven = MoreCollectors.filtering(x -> x % 2 == 0, Collectors.counting());
        checkCollector("filtering", 5L, ints::stream, countEven);
    }

    @Test
    public void testOnlyOne() {
        List<Integer> ints = IntStreamEx.rangeClosed(1, 100).boxed().toList();
        checkShortCircuitCollector("One", Optional.empty(), 2, ints::stream, MoreCollectors.onlyOne());
        checkShortCircuitCollector("FilterSeveral", Optional.empty(), 2, () -> ints.stream().filter(x -> x % 20 == 0),
            MoreCollectors.onlyOne());
        checkShortCircuitCollector("FilterSeveral2", Optional.empty(), 40, ints::stream,
            MoreCollectors.filtering(x -> x % 20 == 0, MoreCollectors.onlyOne()));
        checkShortCircuitCollector("FilterOne", Optional.of(60), 1, () -> ints.stream().filter(x -> x % 60 == 0),
            MoreCollectors.onlyOne());
        checkShortCircuitCollector("FilterNone", Optional.empty(), 0, () -> ints.stream().filter(x -> x % 110 == 0),
            MoreCollectors.onlyOne());
    }

    @Test
    public void testToEnumSet() {
        TimeUnit[] vals = TimeUnit.values();
        List<TimeUnit> enumValues = IntStreamEx.range(100).map(x -> x % vals.length).elements(vals).toList();
        checkShortCircuitCollector("toEnumSet", EnumSet.allOf(TimeUnit.class), vals.length, enumValues::stream,
            MoreCollectors.toEnumSet(TimeUnit.class));
        enumValues = IntStreamEx.range(100).map(x -> x % (vals.length - 1)).elements(vals).toList();
        EnumSet<TimeUnit> expected = EnumSet.allOf(TimeUnit.class);
        expected.remove(vals[vals.length - 1]);
        checkShortCircuitCollector("toEnumSet", expected, 100, enumValues::stream,
            MoreCollectors.toEnumSet(TimeUnit.class));
        checkCollectorEmpty("Empty", EnumSet.noneOf(TimeUnit.class), MoreCollectors.toEnumSet(TimeUnit.class));
    }

    @Test
    public void testFlatMapping() {
        {
            Map<Integer, List<Integer>> expected = IntStreamEx.rangeClosed(1, 100).boxed()
                    .toMap(x -> IntStreamEx.rangeClosed(1, x).boxed().toList());
            checkCollector(
                "flatMappingSimple",
                expected,
                () -> IntStreamEx.rangeClosed(1, 100).boxed(),
                Collectors.groupingBy(Function.identity(),
                    MoreCollectors.flatMapping(x -> IntStream.rangeClosed(1, x).boxed(), Collectors.toList())));
        }

        Function<Entry<String, List<String>>, Stream<String>> valuesStream = e -> e.getValue() == null ? null : e
                .getValue().stream();
        List<Entry<String, List<String>>> list = EntryStream
                .of("a", Arrays.asList("bb", "cc", "dd"), "b", Arrays.asList("ee", "ff"), "c", null)
                .append("c", Arrays.asList("gg"), "b", null, "a", Arrays.asList("hh")).toList();
        {
            Map<String, List<String>> expected = EntryStream.of(list.stream())
                    .flatMapValues(l -> l == null ? null : l.stream()).grouping();
            checkCollector("flatMappingCombine", expected, list::stream,
                Collectors.groupingBy(Entry::getKey, MoreCollectors.flatMapping(valuesStream, Collectors.toList())));
            AtomicInteger openClose = new AtomicInteger();
            Collector<Entry<String, List<String>>, ?, Map<String, List<String>>> groupingBy = Collectors.groupingBy(Entry::getKey, MoreCollectors.flatMapping(valuesStream.andThen(s -> {
                if(s == null) return null;
                openClose.incrementAndGet();
                return s.onClose(openClose::decrementAndGet);
            }), Collectors.toList()));
            checkCollector("flatMappingCombineClosed", expected, list::stream,
                MoreCollectors.collectingAndThen(groupingBy, res -> {
                    assertEquals(0, openClose.get());
                    return res;
                }));
            boolean catched = false;
            try {
                Collector<Entry<String, List<String>>, ?, Map<String, List<String>>> groupingByException = Collectors.groupingBy(Entry::getKey, MoreCollectors.flatMapping(valuesStream.andThen(s -> {
                    if(s == null) return null;
                    openClose.incrementAndGet();
                    return s.onClose(openClose::decrementAndGet).peek(e -> {
                        if(e.equals("gg"))
                            throw new IllegalArgumentException(e);
                    });
                }), Collectors.toList()));
                list.stream().collect(
                    MoreCollectors.collectingAndThen(groupingByException, res -> {
                        assertEquals(0, openClose.get());
                        return res;
                    }));
            } catch (IllegalArgumentException e1) {
                assertEquals("gg", e1.getMessage());
                catched = true;
            }
            assertTrue(catched);
        }
        {
            Map<String, List<String>> expected = EntryStream.of("a", Arrays.asList("bb"), "b", Arrays.asList("ee"),
                "c", Arrays.asList("gg")).toMap();
            Collector<Entry<String, List<String>>, ?, List<String>> headOne = MoreCollectors.flatMapping(valuesStream,
                MoreCollectors.head(1));
            checkCollector("flatMappingSubShort", expected, list::stream, Collectors.groupingBy(Entry::getKey, headOne));
            checkShortCircuitCollector("flatMappingShort", expected, 4, list::stream,
                MoreCollectors.groupingBy(Entry::getKey, StreamEx.of("a", "b", "c").toSet(), headOne));
            AtomicInteger cnt = new AtomicInteger();
            Collector<Entry<String, List<String>>, ?, List<String>> headPeek = MoreCollectors.flatMapping(
                valuesStream.andThen(s -> s == null ? null : s.peek(x -> cnt.incrementAndGet())), MoreCollectors.head(1));
            assertEquals(expected, StreamEx.of(list).collect(Collectors.groupingBy(Entry::getKey, headPeek)));
            assertEquals(3, cnt.get());
            cnt.set(0);
            assertEquals(
                expected,
                StreamEx.of(list).collect(
                    MoreCollectors.groupingBy(Entry::getKey, StreamEx.of("a", "b", "c").toSet(), headPeek)));
            assertEquals(3, cnt.get());
        }
    }
}
