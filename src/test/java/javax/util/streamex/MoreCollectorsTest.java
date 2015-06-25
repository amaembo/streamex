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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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
    }

    @Test
    public void testFirstLast() {
        for (StreamExSupplier<Integer> supplier : streamEx(() -> IntStreamEx.range(1000).boxed())) {
            assertEquals(supplier.toString(), 999, (int) supplier.get().collect(MoreCollectors.last()).get());
            assertEquals(supplier.toString(), 0, (int) supplier.get().collect(MoreCollectors.first()).get());
        }
        for (StreamExSupplier<Integer> supplier : emptyStreamEx(Integer.class)) {
            assertFalse(supplier.toString(), supplier.get().collect(MoreCollectors.first()).isPresent());
            assertFalse(supplier.toString(), supplier.get().collect(MoreCollectors.last()).isPresent());
        }
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
    }

    @Test
    public void testCountingInt() {
        for (StreamExSupplier<Integer> supplier : streamEx(() -> IntStreamEx.range(1000).boxed())) {
            assertEquals(supplier.toString(), 1000, (int)supplier.get().collect(MoreCollectors.countingInt()));
        }
    }
}
