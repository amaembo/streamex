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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.Test;

public class EntryStreamTest {
    @Test
    public void testCreate() {
        assertEquals(0, EntryStream.empty().count());
        assertEquals(0, EntryStream.empty().count());
        Map<String, Integer> data = createMap();
        assertEquals(data, EntryStream.of(data).toMap());
        assertEquals(data, EntryStream.of(data.entrySet().stream()).toMap());
        Map<String, Integer> expected = new HashMap<>();
        expected.put("aaa", 3);
        expected.put("bbb", 3);
        expected.put("c", 1);
        assertEquals(expected, StreamEx.of("aaa", "bbb", "c").mapToEntry(String::length).toMap());
        assertEquals(expected, StreamEx.of("aaa", "bbb", "c").mapToEntry(s -> s, String::length).toMap());
        assertEquals(expected, EntryStream.zip(Arrays.asList("aaa", "bbb", "c"), Arrays.asList(3, 3, 1)).toMap());
        assertEquals(expected, EntryStream.zip(new String[] {"aaa", "bbb", "c"}, new Integer[] {3, 3, 1}).toMap());
        assertEquals(Collections.singletonMap("foo", 1), EntryStream.of("foo", 1).toMap());

        assertEquals(
                expected,
                StreamEx.of(Collections.singletonMap("aaa", 3), Collections.singletonMap("bbb", 3),
                        Collections.singletonMap("c", 1), Collections.emptyMap()).flatMapToEntry(m -> m).toMap());

        Stream<Entry<String, Integer>> stream = EntryStream.of(data);
        assertSame(stream, EntryStream.of(stream));
    }

    @Test
    public void testMap() {
        assertEquals(Arrays.asList("1a", "22bb", "33ccc"),
                EntryStream.of(createMap()).map(entry -> entry.getValue() + entry.getKey()).toList());
    }

    @Test
    public void testMapKeyValue() {
        assertEquals(Arrays.asList("1a", "22bb", "33ccc"), EntryStream.of(createMap()).mapKeyValue((k, v) -> v + k)
                .toList());
    }

    @Test
    public void testFilter() {
        assertEquals(Collections.singletonMap("a", 1), EntryStream.of(createMap()).filterKeys(s -> s.length() < 2)
                .toMap());
        assertEquals(Collections.singletonMap("bb", 22), EntryStream.of(createMap()).filterValues(v -> v % 2 == 0)
                .toMap());
    }

    @Test
    public void testRemove() {
        Map<String, List<Integer>> data = new HashMap<>();
        data.put("aaa", Collections.emptyList());
        data.put("bbb", Collections.singletonList(1));
        assertEquals(Arrays.asList("bbb"), EntryStream.of(data).removeValues(List::isEmpty).keys().toList());
        assertEquals(Arrays.asList("aaa"), EntryStream.of(data).removeKeys(Pattern.compile("bbb").asPredicate()).keys()
                .toList());
    }

    @Test
    public void testLimit() {
        assertEquals(Collections.singletonMap("a", 1), EntryStream.of(createMap()).limit(1).toMap());
    }

    @Test
    public void testKeys() {
        assertEquals(new HashSet<>(Arrays.asList("a", "bb", "ccc")), EntryStream.of(createMap()).keys().toSet());
    }

    @Test
    public void testValues() {
        assertEquals(new HashSet<>(Arrays.asList(1, 22, 33)), EntryStream.of(createMap()).values().toSet());
    }

    @Test
    public void testMapKeys() {
        Map<Integer, Integer> expected = new HashMap<>();
        expected.put(1, 1);
        expected.put(2, 22);
        expected.put(3, 33);
        Map<Integer, Integer> result = EntryStream.of(createMap()).mapKeys(String::length).toMap();
        assertEquals(expected, result);
    }

    @Test
    public void testMapValues() {
        Map<String, String> expected = new HashMap<>();
        expected.put("a", "1");
        expected.put("bb", "22");
        expected.put("ccc", "33");
        Map<String, String> result = EntryStream.of(createMap()).mapValues(String::valueOf).toMap();
        assertEquals(expected, result);
    }

    @Test
    public void testMapEntryValues() {
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 2);
        expected.put("bb", 24);
        expected.put("ccc", 36);
        Map<String, Integer> result = EntryStream.of(createMap())
                .mapEntryValues(e -> e.getKey().length() + e.getValue()).toMap();
        assertEquals(expected, result);
    }

    @Test
    public void testAppend() {
        assertEquals(Arrays.asList(22, 33, 5, 22, 33), EntryStream.of(createMap()).append("dddd", 5)
                .append(createMap()).filterKeys(k -> k.length() > 1).values().toList());
    }

    @Test
    public void testPrepend() {
        assertEquals(Arrays.asList(5, 22, 33, 22, 33),
                EntryStream.of(createMap()).prepend(createMap()).prepend("dddd", 5).filterKeys(k -> k.length() > 1)
                        .values().toList());
    }

    @Test
    public void testToMap() {
        TreeMap<String, Integer> result = EntryStream.of(createMap()).toCustomMap(TreeMap::new);
        assertEquals(createMap(), result);

        Map<String, Integer> expected = new HashMap<>();
        expected.put("aaa", 3);
        expected.put("bb", 4);
        assertEquals(expected,
                StreamEx.of("aaa", "bb", "bb").mapToEntry(String::length).toCustomMap(Integer::sum, HashMap::new));
        Map<String, Integer> map = StreamEx.of("aaa", "bb", "bb").mapToEntry(String::length).toMap(Integer::sum);
        assertEquals(expected, map);
        assertFalse(map instanceof ConcurrentMap);
        map = StreamEx.of("aaa", "bb", "bb").mapToEntry(String::length).parallel().toMap(Integer::sum);
        assertTrue(map instanceof ConcurrentMap);
        assertEquals(expected, map);
        SortedMap<String, Integer> sortedMap = StreamEx.of("aaa", "bb", "bb").mapToEntry(String::length)
                .toSortedMap(Integer::sum);
        assertEquals(expected, sortedMap);
        assertFalse(sortedMap instanceof ConcurrentMap);
        sortedMap = StreamEx.of("aaa", "bb", "bb").mapToEntry(String::length).parallel().toSortedMap(Integer::sum);
        assertEquals(expected, sortedMap);
        assertTrue(sortedMap instanceof ConcurrentMap);

        assertEquals(createMap(), EntryStream.of(createMap()).parallel().toMap());
        assertTrue(EntryStream.of(createMap()).parallel().toMap() instanceof ConcurrentMap);
        sortedMap = EntryStream.of(createMap()).toSortedMap();
        assertEquals(createMap(), sortedMap);
        assertFalse(sortedMap instanceof ConcurrentMap);
        sortedMap = EntryStream.of(createMap()).parallel().toSortedMap();
        assertEquals(createMap(), sortedMap);
        assertTrue(sortedMap instanceof ConcurrentMap);
    }
    
    @Test
    public void testFlatMap() {
        assertEquals(Arrays.asList((int)'a', (int)'b', (int)'b', (int)'c', (int)'c', (int)'c'),
                EntryStream.of(createMap()).flatMap(entry -> entry.getKey().chars().boxed()).toList());
        assertEquals(Arrays.asList("a", "b", "b", "c", "c", "c"),
                EntryStream.of(createMap()).flatCollection(entry -> Arrays.asList(entry.getKey().split(""))).toList());
    }

    @Test
    public void testFlatMapValues() {
        Map<String, List<Integer>> data1 = new HashMap<>();
        data1.put("aaa", Arrays.asList(1, 2, 3));
        data1.put("bb", Arrays.asList(4, 5, 6));
        Map<String, List<Integer>> data2 = new HashMap<>();
        data2.put("aaa", Arrays.asList(10));
        data2.put("bb", Arrays.asList(20));
        Map<String, List<Integer>> result = StreamEx.of(data1, data2).flatMapToEntry(m -> m)
                .flatMapValues(List::stream).grouping();
        Map<String, List<Integer>> expected = new HashMap<>();
        expected.put("aaa", Arrays.asList(1, 2, 3, 10));
        expected.put("bb", Arrays.asList(4, 5, 6, 20));
        assertEquals(expected, result);
    }

    @Test
    public void testSetValue() {
        assertEquals(Collections.singletonMap("aaa", 6), EntryStream.of("aaa", 5).peek(e -> e.setValue(6)).toMap());
    }

    @Test
    public void testGrouping() {
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("ab", 1);
        data.put("ac", 2);
        data.put("ba", 3);
        data.put("bc", 4);
        Map<String, List<Integer>> expected = new LinkedHashMap<>();
        expected.put("a", Arrays.asList(1, 2));
        expected.put("b", Arrays.asList(3, 4));
        Map<String, List<Integer>> result = EntryStream.of(data).mapKeys(k -> k.substring(0, 1)).grouping();
        assertEquals(expected, result);
        TreeMap<String, List<Integer>> resultTree = EntryStream.of(data).mapKeys(k -> k.substring(0, 1))
                .grouping(TreeMap::new);
        assertEquals(expected, resultTree);
    }

    @Test
    public void testGroupingTo() {
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("ab", 1);
        data.put("ac", 2);
        data.put("ba", 3);
        data.put("bc", 3);
        Map<String, Set<Integer>> expected = new LinkedHashMap<>();
        expected.put("a", new HashSet<>(Arrays.asList(1, 2)));
        expected.put("b", Collections.singleton(3));
        Map<String, Set<Integer>> result = EntryStream.of(data).mapKeys(k -> k.substring(0, 1))
                .groupingTo(HashSet::new);
        assertEquals(expected, result);
        assertFalse(result instanceof ConcurrentMap);
        result = EntryStream.of(data).mapKeys(k -> k.substring(0, 1)).parallel().groupingTo(HashSet::new);
        assertEquals(expected, result);
        assertTrue(result instanceof ConcurrentMap);
        SortedMap<String, Set<Integer>> resultTree = EntryStream.of(data).mapKeys(k -> k.substring(0, 1))
                .groupingTo(TreeMap::new, HashSet::new);
        assertEquals(expected, resultTree);
        assertFalse(resultTree instanceof ConcurrentMap);
        resultTree = EntryStream.of(data).mapKeys(k -> k.substring(0, 1)).parallel()
                .groupingTo(TreeMap::new, HashSet::new);
        assertEquals(expected, resultTree);
        assertFalse(resultTree instanceof ConcurrentMap);
        resultTree = EntryStream.of(data).mapKeys(k -> k.substring(0, 1)).parallel()
                .groupingTo(ConcurrentSkipListMap::new, HashSet::new);
        assertEquals(expected, resultTree);
        assertTrue(resultTree instanceof ConcurrentMap);
    }

    @Test
    public void testDistinct() {
        Map<String, List<Integer>> expected = new LinkedHashMap<>();
        expected.put("aaa", Arrays.asList(3));
        expected.put("bbb", Arrays.asList(3, 3));
        expected.put("cc", Arrays.asList(2));
        assertEquals(expected, StreamEx.of("aaa", "bbb", "bbb", "cc").mapToEntry(String::length).grouping());

        Map<String, List<Integer>> expectedDistinct = new LinkedHashMap<>();
        expectedDistinct.put("aaa", Arrays.asList(3));
        expectedDistinct.put("bbb", Arrays.asList(3));
        expectedDistinct.put("cc", Arrays.asList(2));
        assertEquals(expectedDistinct, StreamEx.of("aaa", "bbb", "bbb", "cc").mapToEntry(String::length).distinct()
                .grouping());
    }

    @Test
    public void testNonNull() {
        Map<String, String> input = new LinkedHashMap<>();
        input.put("a", "b");
        input.put("b", null);
        input.put(null, "c");
        assertEquals(Arrays.asList("b", null), EntryStream.of(input).nonNullKeys().values().toList());
        assertEquals(Arrays.asList("a", null), EntryStream.of(input).nonNullValues().keys().toList());
        assertEquals(Collections.singletonMap("a", "b"), EntryStream.of(input).nonNullValues().nonNullKeys().toMap());
    }

    @Test
    public void testSelect() {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", "2");
        map.put(3, "c");
        assertEquals(Collections.singletonMap("a", 1), EntryStream.of(map).selectValues(Integer.class).toMap());
        assertEquals(Collections.singletonMap(3, "c"), EntryStream.of(map).selectKeys(Integer.class).toMap());
    }

    @Test
    public void testInvert() {
        Map<Integer, String> result = EntryStream.of(createMap()).invert().toMap();
        Map<Integer, String> expected = new LinkedHashMap<>();
        expected.put(1, "a");
        expected.put(22, "bb");
        expected.put(33, "ccc");
        assertEquals(expected, result);
    }

    @Test(expected = IllegalStateException.class)
    public void testCollision() {
        StreamEx.of("aa", "aa").mapToEntry(String::length).toCustomMap(LinkedHashMap::new);
    }

    @Test
    public void testForKeyValue() {
        Map<String, Integer> output = new HashMap<>();
        EntryStream.of(createMap()).forKeyValue(output::put);
        assertEquals(output, createMap());
    }

    private Map<String, Integer> createMap() {
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("a", 1);
        data.put("bb", 22);
        data.put("ccc", 33);
        return data;
    }
}
