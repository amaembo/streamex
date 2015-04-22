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

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import static org.junit.Assert.*;

public class StreamExTest {
    @Test
    public void testCreate() {
        assertEquals(Arrays.asList(), StreamEx.empty().toList());
        assertEquals(Arrays.asList(), StreamEx.empty().toList()); // double check is intended
        assertEquals(Arrays.asList("a"), StreamEx.of("a").toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.of("a", "b").toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.of(Arrays.asList("a", "b")).toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.of(Arrays.asList("a", "b").stream()).toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.split("a,b", ",").toList());
        assertEquals(Arrays.asList("a", "c", "d"),
                StreamEx.split("abcBd", Pattern.compile("b", Pattern.CASE_INSENSITIVE)).toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.ofLines(new StringReader("a\nb")).toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.ofLines(new BufferedReader(new StringReader("a\nb"))).toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.ofLines(getReader()).toList());
        assertEquals(Arrays.asList("a", "aa", "aaa", "aaaa"), StreamEx.iterate("a", x -> x+"a").limit(4).toList());
        assertEquals(Arrays.asList("a", "a", "a", "a"), StreamEx.generate(() -> "a").limit(4).toList());
        
        StreamEx<String> stream = StreamEx.of("foo", "bar");
        assertSame(stream, StreamEx.of(stream));
    }

    private Reader getReader() {
        return new BufferedReader(new StringReader("a\nb"));
    }

    @Test
    public void testBasics() {
        assertFalse(StreamEx.of("a").isParallel());
        assertTrue(StreamEx.of("a").parallel().isParallel());
        assertFalse(StreamEx.of("a").parallel().sequential().isParallel());
        AtomicInteger i = new AtomicInteger();
        try(Stream<String> s = StreamEx.of("a").onClose(() -> i.incrementAndGet())) {
            assertEquals(1, s.count());
        }
        assertEquals(1, i.get());
        assertEquals(Arrays.asList(1, 2), StreamEx.of("a", "bb").map(String::length).toList());
    }
    
    @Test
    public void testToMap() {
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("bb", 2);
        expected.put("ccc", 3);
        Map<String, Integer> seqMap = StreamEx.of("a", "bb", "ccc").toMap(String::length);
        Map<String, Integer> parallelMap = StreamEx.of("a", "bb", "ccc").parallel().toMap(String::length);
        assertEquals(expected, seqMap);
        assertEquals(expected, parallelMap);
        assertFalse(seqMap instanceof ConcurrentMap);
        assertTrue(parallelMap instanceof ConcurrentMap);
        
        Map<Integer, String> expected2 = new HashMap<>();
        expected2.put(1, "a");
        expected2.put(2, "bb");
        expected2.put(3, "ccc");
        Map<Integer, String> seqMap2 = StreamEx.of("a", "bb", "ccc").toMap(String::length, Function.identity());
        Map<Integer, String> parallelMap2 = StreamEx.of("a", "bb", "ccc").parallel().toMap(String::length, Function.identity());
        assertEquals(expected2, seqMap2);
        assertEquals(expected2, parallelMap2);
        assertFalse(seqMap2 instanceof ConcurrentMap);
        assertTrue(parallelMap2 instanceof ConcurrentMap);
        
        Map<Integer, String> expected3 = new HashMap<>();
        expected3.put(1, "a");
        expected3.put(2, "bbbb");
        expected3.put(3, "ccc");
        Map<Integer, String> seqMap3 = StreamEx.of("a", "bb", "ccc", "bb").toMap(String::length, Function.identity(), String::concat);
        Map<Integer, String> parallelMap3 = StreamEx.of("a", "bb", "ccc", "bb").parallel().toMap(String::length, Function.identity(), String::concat);
        assertEquals(expected3, seqMap3);
        assertEquals(expected3, parallelMap3);
        assertFalse(seqMap3 instanceof ConcurrentMap);
        assertTrue(parallelMap3 instanceof ConcurrentMap);
    }
    
    @Test
    public void testToSortedMap() {
        SortedMap<String, Integer> expected = new TreeMap<>();
        expected.put("a", 1);
        expected.put("bb", 2);
        expected.put("ccc", 3);
        SortedMap<String, Integer> seqMap = StreamEx.of("a", "bb", "ccc").toSortedMap(String::length);
        SortedMap<String, Integer> parallelMap = StreamEx.of("a", "bb", "ccc").parallel().toSortedMap(String::length);
        assertEquals(expected, seqMap);
        assertEquals(expected, parallelMap);
        assertFalse(seqMap instanceof ConcurrentMap);
        assertTrue(parallelMap instanceof ConcurrentMap);
        
        SortedMap<Integer, String> expected2 = new TreeMap<>();
        expected2.put(1, "a");
        expected2.put(2, "bb");
        expected2.put(3, "ccc");
        SortedMap<Integer, String> seqMap2 = StreamEx.of("a", "bb", "ccc").toSortedMap(String::length, Function.identity());
        SortedMap<Integer, String> parallelMap2 = StreamEx.of("a", "bb", "ccc").parallel().toSortedMap(String::length, Function.identity());
        assertEquals(expected2, seqMap2);
        assertEquals(expected2, parallelMap2);
        assertFalse(seqMap2 instanceof ConcurrentMap);
        assertTrue(parallelMap2 instanceof ConcurrentMap);
        
        SortedMap<Integer, String> expected3 = new TreeMap<>();
        expected3.put(1, "a");
        expected3.put(2, "bbbb");
        expected3.put(3, "ccc");
        SortedMap<Integer, String> seqMap3 = StreamEx.of("a", "bb", "ccc", "bb").toSortedMap(String::length, Function.identity(), String::concat);
        SortedMap<Integer, String> parallelMap3 = StreamEx.of("a", "bb", "ccc", "bb").parallel().toSortedMap(String::length, Function.identity(), String::concat);
        assertEquals(expected3, seqMap3);
        assertEquals(expected3, parallelMap3);
        assertFalse(seqMap3 instanceof ConcurrentMap);
        assertTrue(parallelMap3 instanceof ConcurrentMap);
    }
    
    @Test
    public void testGroupingBy() {
        Map<Integer, List<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("a"));
        expected.put(2, Arrays.asList("bb", "bb"));
        expected.put(3, Arrays.asList("ccc"));
        Map<Integer, List<String>> seqMap = StreamEx.of("a", "bb", "bb", "ccc").groupingBy(String::length);
        Map<Integer, List<String>> parallelMap = StreamEx.of("a", "bb", "bb", "ccc").parallel().groupingBy(String::length);
        assertEquals(expected, seqMap);
        assertEquals(expected, parallelMap);
        assertFalse(seqMap instanceof ConcurrentMap);
        assertTrue(parallelMap instanceof ConcurrentMap);

        Map<Integer, Set<String>> expectedMapSet = new HashMap<>();
        expectedMapSet.put(1, new HashSet<>(Arrays.asList("a")));
        expectedMapSet.put(2, new HashSet<>(Arrays.asList("bb", "bb")));
        expectedMapSet.put(3, new HashSet<>(Arrays.asList("ccc")));
        Map<Integer, Set<String>> seqMapSet = StreamEx.of("a", "bb", "bb", "ccc").groupingBy(String::length, Collectors.toSet());
        Map<Integer, Set<String>> parallelMapSet = StreamEx.of("a", "bb", "bb", "ccc").parallel().groupingBy(String::length, Collectors.toSet());
        assertEquals(expectedMapSet, seqMapSet);
        assertEquals(expectedMapSet, parallelMapSet);
        assertFalse(seqMapSet instanceof ConcurrentMap);
        assertTrue(parallelMapSet instanceof ConcurrentMap);

        seqMapSet = StreamEx.of("a", "bb", "bb", "ccc").groupingBy(String::length, HashMap::new, Collectors.toSet());
        assertEquals(expectedMapSet, seqMapSet);
        assertFalse(seqMapSet instanceof ConcurrentMap);
        seqMapSet = StreamEx.of("a", "bb", "bb", "ccc").parallel().groupingBy(String::length, HashMap::new, Collectors.toSet());
        assertEquals(expectedMapSet, seqMapSet);
        assertFalse(seqMapSet instanceof ConcurrentMap);
        parallelMapSet = StreamEx.of("a", "bb", "bb", "ccc").parallel().groupingBy(String::length, ConcurrentHashMap::new, Collectors.toSet());
        assertEquals(expectedMapSet, parallelMapSet);
        assertTrue(parallelMapSet instanceof ConcurrentMap);
    }
    
    @Test
    public void testIterable() {
        List<String> result = new ArrayList<>();
        for(String s : StreamEx.of("a", "b", "cc").filter(s -> s.length() < 2)) {
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
        assertEquals(Arrays.asList("a", "b", "c", "d"), StreamEx.of(data.entrySet()).flatCollection(Entry::getValue)
                .toList());
    }

    @Test
    public void testAppend() {
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"),
                StreamEx.of("a", "b", "c", "dd").remove(s -> s.length() > 1).append("d", "e").toList());
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"),
                StreamEx.of("a", "b", "c").append(Arrays.asList("d", "e").stream()).toList());
    }

    @Test
    public void testPrepend() {
        assertEquals(Arrays.asList("d", "e", "a", "b", "c"),
                StreamEx.of("a", "b", "c", "dd").remove(s -> s.length() > 1).prepend("d", "e").toList());
        assertEquals(Arrays.asList("d", "e", "a", "b", "c"),
                StreamEx.of("a", "b", "c").prepend(Arrays.asList("d", "e").stream()).toList());
    }

    @Test
    public void testNonNull() {
        List<String> data = Arrays.asList("a", null, "b");
        assertEquals(Arrays.asList("a", null, "b"), StreamEx.of(data).toList());
        assertEquals(Arrays.asList("a", "b"), StreamEx.of(data).nonNull().toList());
    }

    @Test
    public void testSortedBy() {
        List<String> data = Arrays.asList("a", "bbb", "cc");
        assertEquals(Arrays.asList("a", "cc", "bbb"), StreamEx.of(data).sortedByInt(String::length).toList());
        assertEquals(Arrays.asList("a", "cc", "bbb"), StreamEx.of(data).sortedByLong(String::length).toList());
        assertEquals(Arrays.asList("a", "cc", "bbb"), StreamEx.of(data).sortedByDouble(String::length).toList());
        assertEquals(Arrays.asList("a", "cc", "bbb"), StreamEx.of(data).sortedBy(s -> s.length()).toList());
    }

    @Test
    public void testMinBy() {
        List<String> data = Arrays.asList("a", "bbb", "cc");
        assertEquals("a", StreamEx.of(data).minByInt(String::length).get());
        assertEquals("a", StreamEx.of(data).minByLong(String::length).get());
        assertEquals("a", StreamEx.of(data).minByDouble(String::length).get());
        assertEquals("a", StreamEx.of(data).minBy(s -> s.length()).get());
    }
    
    @Test
    public void testMaxBy() {
        List<String> data = Arrays.asList("a", "bbb", "cc");
        assertEquals("bbb", StreamEx.of(data).maxByInt(String::length).get());
        assertEquals("bbb", StreamEx.of(data).maxByLong(String::length).get());
        assertEquals("bbb", StreamEx.of(data).maxByDouble(String::length).get());
        assertEquals("bbb", StreamEx.of(data).maxBy(s -> s.length()).get());
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
    public void testJoining() {
        assertEquals("abc", StreamEx.of("a", "b", "c").joining());
        assertEquals("a,b,c", StreamEx.of("a", "b", "c").joining(","));
        assertEquals("[1;2;3]", StreamEx.of(1, 2, 3).joining(";", "[", "]"));
    }
}
