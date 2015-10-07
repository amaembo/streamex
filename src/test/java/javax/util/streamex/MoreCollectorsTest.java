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
import java.util.Locale;
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
import java.util.stream.Stream;

import org.junit.Assert;
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
        for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
            assertEquals(supplier.toString(), Arrays.asList("eee", "ddd"),
                supplier.get().collect(MoreCollectors.maxAll(Comparator.comparingInt(String::length))));
            assertEquals(
                supplier.toString(),
                1L,
                (long) supplier.get().collect(
                    MoreCollectors.minAll(Comparator.comparingInt(String::length), Collectors.counting())));
            assertEquals(
                supplier.toString(),
                "eee,ddd",
                supplier.get().collect(
                    MoreCollectors.maxAll(Comparator.comparingInt(String::length), Collectors.joining(","))));
            assertEquals(supplier.toString(), Arrays.asList(""),
                supplier.get().collect(MoreCollectors.minAll(Comparator.comparingInt(String::length))));
        }
        assertEquals(Collections.emptyList(),
            StreamEx.<String> empty().collect(MoreCollectors.maxAll(Comparator.comparingInt(String::length))));
        assertEquals(Collections.emptyList(),
            StreamEx.<String> empty().parallel()
                    .collect(MoreCollectors.maxAll(Comparator.comparingInt(String::length))));

        List<Integer> ints = IntStreamEx.of(new Random(1), 10000, 1, 1000).boxed().toList();
        List<Integer> expectedMax = null;
        List<Integer> expectedMin = null;
        for (Integer i : ints) {
            if (expectedMax == null || i > expectedMax.get(0)) {
                expectedMax = new ArrayList<>();
                expectedMax.add(i);
            } else if (i.equals(expectedMax.get(0))) {
                expectedMax.add(i);
            }
            if (expectedMin == null || i < expectedMin.get(0)) {
                expectedMin = new ArrayList<>();
                expectedMin.add(i);
            } else if (i.equals(expectedMin.get(0))) {
                expectedMin.add(i);
            }
        }
        Collector<Integer, ?, SimpleEntry<Integer, Long>> downstream = MoreCollectors.pairing(MoreCollectors.first(),
            Collectors.counting(), (opt, cnt) -> new AbstractMap.SimpleEntry<>(opt.get(), cnt));

        for (StreamExSupplier<Integer> supplier : streamEx(ints::stream)) {
            assertEquals(supplier.toString(), expectedMax,
                supplier.get().collect(MoreCollectors.maxAll(Integer::compare)));

            SimpleEntry<Integer, Long> entry = supplier.get().collect(MoreCollectors.maxAll(downstream));
            assertEquals(supplier.toString(), expectedMax.size(), (long) entry.getValue());
            assertEquals(supplier.toString(), expectedMax.get(0), entry.getKey());

            assertEquals(supplier.toString(), expectedMin,
                supplier.get().collect(MoreCollectors.minAll(Integer::compare)));

            entry = supplier.get().collect(MoreCollectors.minAll(downstream));
            assertEquals(supplier.toString(), expectedMin.size(), (long) entry.getValue());
            assertEquals(supplier.toString(), expectedMin.get(0), entry.getKey());
        }

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

    @Test
    public void testFirstLast() {
        for (StreamExSupplier<Integer> supplier : streamEx(() -> IntStreamEx.range(1000).boxed())) {
            assertEquals(supplier.toString(), 999, (int) supplier.get().collect(MoreCollectors.last()).get());
            assertEquals(supplier.toString(), 0, (int) supplier.get().collect(MoreCollectors.first()).orElse(-1));
        }
        for (StreamExSupplier<Integer> supplier : emptyStreamEx(Integer.class)) {
            assertFalse(supplier.toString(), supplier.get().collect(MoreCollectors.first()).isPresent());
            assertFalse(supplier.toString(), supplier.get().collect(MoreCollectors.last()).isPresent());
        }
        assertEquals(1, (int) StreamEx.iterate(1, x -> x + 1).parallel().collect(MoreCollectors.first()).get());
    }

    @Test
    public void testHeadTailParallel() {
        for (int i = 0; i < 100; i++) {
            assertEquals("#" + i, Arrays.asList(0, 1),
                IntStreamEx.range(1000).boxed().parallel().collect(MoreCollectors.head(2)));
        }
        assertEquals(Arrays.asList(0, 1), StreamEx.iterate(0, x -> x + 1).parallel().collect(MoreCollectors.head(2)));
    }

    @Test
    public void testHeadTail() {
        for (StreamExSupplier<Integer> supplier : streamEx(() -> IntStreamEx.range(1000).boxed())) {
            assertEquals(supplier.toString(), Arrays.asList(), supplier.get().collect(MoreCollectors.tail(0)));
            assertEquals(supplier.toString(), Arrays.asList(999), supplier.get().collect(MoreCollectors.tail(1)));
            assertEquals(supplier.toString(), Arrays.asList(998, 999), supplier.get().collect(MoreCollectors.tail(2)));
            assertEquals(supplier.toString(), supplier.get().skip(1).toList(),
                supplier.get().collect(MoreCollectors.tail(999)));
            assertEquals(supplier.toString(), supplier.get().toList(), supplier.get()
                    .collect(MoreCollectors.tail(1000)));
            assertEquals(supplier.toString(), supplier.get().toList(),
                supplier.get().collect(MoreCollectors.tail(Integer.MAX_VALUE)));

            assertEquals(supplier.toString(), Arrays.asList(), supplier.get().collect(MoreCollectors.head(0)));
            assertEquals(supplier.toString(), Arrays.asList(0), supplier.get().collect(MoreCollectors.head(1)));
            assertEquals(supplier.toString(), Arrays.asList(0, 1), supplier.get().collect(MoreCollectors.head(2)));
            assertEquals(supplier.toString(), supplier.get().limit(999).toList(),
                supplier.get().collect(MoreCollectors.head(999)));
            assertEquals(supplier.toString(), supplier.get().toList(), supplier.get()
                    .collect(MoreCollectors.head(1000)));
            assertEquals(supplier.toString(), supplier.get().toList(),
                supplier.get().collect(MoreCollectors.head(Integer.MAX_VALUE)));
        }
    }

    @Test
    public void testGreatest() {
        List<Integer> ints = IntStreamEx.of(new Random(1), 1000, 1, 1000).boxed().toList();
        Comparator<Integer> byString = Comparator.comparing(String::valueOf);
        for (StreamExSupplier<Integer> supplier : streamEx(ints::stream)) {
            assertEquals(supplier.toString(), Collections.emptyList(), supplier.get().collect(MoreCollectors.least(0)));
            assertEquals(supplier.toString(), supplier.get().sorted().limit(5).toList(),
                supplier.get().collect(MoreCollectors.least(5)));
            assertEquals(supplier.toString(), supplier.get().sorted().limit(20).toList(),
                supplier.get().collect(MoreCollectors.least(20)));
            assertEquals(supplier.toString(), supplier.get().sorted(byString).limit(20).toList(), supplier.get()
                    .collect(MoreCollectors.least(byString, 20)));
            assertEquals(supplier.toString(), supplier.get().sorted().toList(),
                supplier.get().collect(MoreCollectors.least(Integer.MAX_VALUE)));

            assertEquals(supplier.toString(), Collections.emptyList(),
                supplier.get().collect(MoreCollectors.greatest(0)));
            assertEquals(supplier.toString(), supplier.get().reverseSorted().limit(5).toList(),
                supplier.get().collect(MoreCollectors.greatest(5)));
            assertEquals(supplier.toString(), supplier.get().reverseSorted().limit(20).toList(), supplier.get()
                    .collect(MoreCollectors.greatest(20)));
            assertEquals(supplier.toString(), supplier.get().reverseSorted(byString).limit(20).toList(), supplier.get()
                    .collect(MoreCollectors.greatest(byString, 20)));
            assertEquals(supplier.toString(), supplier.get().reverseSorted().toList(),
                supplier.get().collect(MoreCollectors.greatest(Integer.MAX_VALUE)));
        }

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
        List<Integer> ints = IntStreamEx.of(new Random(1), 1000, 0, 100).boxed().toList();
        long expectedMin = IntStreamEx.ofIndices(ints).minBy(ints::get).getAsInt();
        long expectedMax = IntStreamEx.ofIndices(ints).maxBy(ints::get).getAsInt();
        long expectedMinString = IntStreamEx.ofIndices(ints).minBy(i -> String.valueOf(ints.get(i))).getAsInt();
        long expectedMaxString = IntStreamEx.ofIndices(ints).maxBy(i -> String.valueOf(ints.get(i))).getAsInt();
        for (StreamExSupplier<Integer> supplier : streamEx(ints::stream)) {
            assertEquals(supplier.toString(), expectedMin, supplier.get().collect(MoreCollectors.minIndex())
                    .getAsLong());
            assertEquals(supplier.toString(), expectedMax, supplier.get().collect(MoreCollectors.maxIndex())
                    .getAsLong());
            assertEquals(supplier.toString(), expectedMinString,
                supplier.get().collect(MoreCollectors.minIndex(Comparator.comparing(String::valueOf))).getAsLong());
            assertEquals(supplier.toString(), expectedMaxString,
                supplier.get().collect(MoreCollectors.maxIndex(Comparator.comparing(String::valueOf))).getAsLong());
            assertEquals(supplier.toString(), expectedMinString,
                supplier.get().map(String::valueOf).collect(MoreCollectors.minIndex()).getAsLong());
            assertEquals(supplier.toString(), expectedMaxString,
                supplier.get().map(String::valueOf).collect(MoreCollectors.maxIndex()).getAsLong());
        }
        for (StreamExSupplier<Integer> supplier : emptyStreamEx(Integer.class)) {
            assertFalse(supplier.toString(), supplier.get().collect(MoreCollectors.minIndex()).isPresent());
            assertFalse(supplier.toString(), supplier.get().collect(MoreCollectors.maxIndex()).isPresent());
        }
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

    @Test
    public void testGroupingByWithDomain() {
        List<String> data = Arrays.asList("a", "foo", "test", "ququq", "bar", "blahblah");
        Collector<String, ?, String> collector = 
                MoreCollectors.collectingAndThen(MoreCollectors.groupingBy(String::length,
            IntStreamEx.range(10).boxed().toSet(), TreeMap::new, MoreCollectors.first()), Object::toString);
        checkCollector("groupingWithDomain",
            "{0=Optional.empty, 1=Optional[a], 2=Optional.empty, 3=Optional[foo], 4=Optional[test], 5=Optional[ququq], "
                + "6=Optional.empty, 7=Optional.empty, 8=Optional[blahblah], 9=Optional.empty}", data::stream,
            collector);

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
        Collector<Entry<String, String>, ?, Map<String, String>> groupingBy = MoreCollectors.groupingBy(
            Entry::getValue, StreamEx.of("Girl", "Boy").toSet(),
            MoreCollectors.mapping(Entry::getKey, MoreCollectors.joining(", ", "...", 16, false)));
        AtomicInteger counter = new AtomicInteger();
        Map<String, String> map = EntryStream.of(name2sex).peek(c -> counter.incrementAndGet()).collect(groupingBy);
        assertEquals("Mary, Lucie, ...", map.get("Girl"));
        assertEquals("John, James, ...", map.get("Boy"));
        assertEquals(7, counter.get());
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
        AtomicInteger counter = new AtomicInteger();
        Map<Boolean, Optional<Integer>> map = IntStreamEx.range(1, 100).boxed().peek(x -> counter.incrementAndGet())
                .collect(MoreCollectors.partitioningBy(x -> x % 20 == 0, MoreCollectors.first()));
        assertEquals(20, (int) map.get(true).get());
        assertEquals(1, (int) map.get(false).get());
        assertEquals(20, counter.get()); // short-circuit

        counter.set(0);
        map = IntStreamEx.range(1, 100).boxed().peek(x -> counter.incrementAndGet())
                .collect(MoreCollectors.partitioningBy(x -> x % 200 == 0, MoreCollectors.first()));
        assertFalse(map.get(true).isPresent());
        assertEquals(1, (int) map.get(false).get());
        assertEquals(99, counter.get());
    }

    @Test
    public void testMapping() {
        List<String> input = Arrays.asList("Capital", "lower", "Foo", "bar");
        Collector<String, ?, Map<Boolean, Optional<Integer>>> collector = MoreCollectors
                .partitioningBy(str -> Character.isUpperCase(str.charAt(0)),
                    MoreCollectors.mapping(String::length, MoreCollectors.first()));
        AtomicInteger counter = new AtomicInteger();
        StreamEx.of(input).peek(x -> counter.incrementAndGet()).collect(collector);
        assertEquals(2, counter.get());
        for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
            Map<Boolean, Optional<Integer>> map = supplier.get().collect(collector);
            assertEquals(7, (int) map.get(true).get());
            assertEquals(5, (int) map.get(false).get());
            map = supplier.get().collect(Collectors.collectingAndThen(collector, Function.identity()));
            assertEquals(7, (int) map.get(true).get());
            assertEquals(5, (int) map.get(false).get());
        }
        Collector<String, ?, Map<Boolean, Optional<Integer>>> collectorLast = MoreCollectors.partitioningBy(
            str -> Character.isUpperCase(str.charAt(0)), MoreCollectors.mapping(String::length, MoreCollectors.last()));
        for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
            Map<Boolean, Optional<Integer>> map = supplier.get().collect(collectorLast);
            assertEquals(3, (int) map.get(true).get());
            assertEquals(3, (int) map.get(false).get());
        }
    }

    @Test
    public void testIntersecting() {
        for (int i = 0; i < 5; i++) {
            List<List<String>> input = Arrays.asList(Arrays.asList("aa", "bb", "cc"), Arrays.asList("cc", "bb", "dd"),
                Arrays.asList("ee", "dd"), Arrays.asList("aa", "bb", "dd"));
            checkShortCircuitCollector("#" + i, Collections.emptySet(), 3, StreamEx.of(input),
                MoreCollectors.intersecting());
            checkCollector("#"+i, Collections.emptySet(), input::stream, MoreCollectors.intersecting());
            List<List<Integer>> copies = new ArrayList<>(Collections.nCopies(100, Arrays.asList(1, 2)));
            checkCollector("#"+i, StreamEx.of(1, 2).toSet(), copies::stream, MoreCollectors.intersecting());
            copies.addAll(Collections.nCopies(100, Arrays.asList(3)));
            checkCollector("#"+i, Collections.emptySet(), copies::stream, MoreCollectors.intersecting());
            checkCollectorEmpty("#"+i, Collections.emptySet(), MoreCollectors.intersecting());
        }
    }

    @Test
    public void testJoining() {
        List<String> input = Arrays.asList("one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten");
        assertEquals("", StreamEx.of(input).peek(Assert::fail).collect(MoreCollectors.joining(", ", "...", 0, true)));
        assertEquals("",
            StreamEx.of(input).parallel().peek(Assert::fail).collect(MoreCollectors.joining(", ", "...", 0, true)));
        String expected = "one, two, three, four, five, six, seven, eight, nine, ten";
        for (int i = 3; i < expected.length() + 5; i++) {
            String exp = expected;
            if (exp.length() > i) {
                exp = exp.substring(0, i - 3) + "...";
            }
            String exp2 = expected;
            while (exp2.length() > i) {
                int pos = exp2.lastIndexOf(", ", exp2.endsWith(", ...") ? exp2.length() - 6 : exp2.length());
                exp2 = pos >= 0 ? exp2.substring(0, pos + 2) + "..." : "...";
            }
            for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
                assertEquals(supplier + "/#" + i, exp,
                    supplier.get().collect(MoreCollectors.joining(", ", "...", i, true)));
                assertEquals(supplier + "/#" + i, expected.substring(0, Math.min(i, expected.length())), supplier.get()
                        .collect(MoreCollectors.joining(", ", "", i, true)));
                assertEquals(supplier + "/#" + i, exp2,
                    supplier.get().collect(MoreCollectors.joining(", ", "...", i, false)));
            }
        }

        byte[] data = { (byte) 0xFF, 0x30, 0x40, 0x50, 0x70, 0x12, (byte) 0xF0 };
        assertEquals(
            "FF 30 40 50 ...",
            IntStreamEx.of(data).mapToObj(b -> String.format(Locale.ENGLISH, "%02X", b & 0xFF))
                    .collect(MoreCollectors.joining(" ", "...", 15, false)));
    }

    @Test
    public void testAndInt() {
        List<Integer> ints = Arrays.asList(0b1100, 0b0110, 0b101110, 0b11110011);
        Collector<Integer, ?, OptionalInt> collector = MoreCollectors.andInt(Integer::intValue);
        checkCollector("andInt", OptionalInt.of(0), ints::stream, collector);
        checkCollectorEmpty("andIntEmpty", OptionalInt.empty(), collector);
        assertEquals(OptionalInt.of(0), IntStreamEx.iterate(16384, i -> i + 1).parallel().boxed().collect(collector));
        assertEquals(OptionalInt.of(16384), IntStreamEx.iterate(16384, i -> i + 1).parallel().limit(16383).boxed()
                .collect(collector));
    }

    @Test
    public void testAndLong() {
        List<Long> longs = Arrays.asList(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFF00000000L, 0xFFFFFFFF0000L);
        checkCollector("andLong", OptionalLong.of(0xFFFF00000000L), longs::stream,
            MoreCollectors.andLong(Long::longValue));
        checkCollectorEmpty("andLongEmpty", OptionalLong.empty(), MoreCollectors.andLong(Long::longValue));
    }
    
    @Test
    public void testFiltering() {
        Collector<Integer, ?, Optional<Integer>> firstEven = MoreCollectors.filtering(x -> x % 2 == 0,
            MoreCollectors.first());
        Collector<Integer, ?, Optional<Integer>> firstOdd = MoreCollectors.filtering(x -> x % 2 != 0,
                MoreCollectors.first());
        Collector<Integer, ?, Integer> sumOddEven = MoreCollectors.pairing(firstEven, firstOdd, (e, o) -> e.get()+o.get());
        List<Integer> ints = Arrays.asList(1, 3, 5, 7, 9, 10, 8, 6, 4, 2, 3, 7, 11);
        checkShortCircuitCollector("sumOddEven", 11, 6, StreamEx.of(ints), sumOddEven);
        checkCollector("sumOddEven", 11, ints::stream, sumOddEven);
        Collector<Integer, ?, Long> countEven = MoreCollectors.filtering(x -> x % 2 == 0, Collectors.counting());
        checkCollector("filtering", 5L, ints::stream, countEven);
    }
}
