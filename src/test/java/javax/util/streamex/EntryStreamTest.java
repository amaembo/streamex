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
import java.util.TreeMap;

import org.junit.Test;

public class EntryStreamTest {
	@Test
	public void testCreate() {
		Map<String, Integer> data = createMap();
		assertEquals(data, EntryStream.of(data).toMap());
		Map<String, Integer> expected = new HashMap<>();
		expected.put("aaa", 3);
		expected.put("bbb", 3);
		expected.put("c", 1);
		assertEquals(expected, StreamEx.of("aaa", "bbb", "c").mapToEntry(String::length).toMap());
		assertEquals(expected, StreamEx.of("aaa", "bbb", "c").mapToEntry(s -> s, String::length).toMap());
		assertEquals(Collections.singletonMap("foo", 1), EntryStream.of("foo", 1).toMap());
	}
	
	@Test
	public void testFilter() {
		assertEquals(Collections.singletonMap("a", 1), EntryStream.of(createMap()).filterKeys(s -> s.length() < 2).toMap());
		assertEquals(Collections.singletonMap("bb", 22), EntryStream.of(createMap()).filterValues(v -> v % 2 == 0).toMap());
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
		Map<String, Integer> result = EntryStream.of(createMap()).mapEntryValues(e -> e.getKey().length()+e.getValue()).toMap();
		assertEquals(expected, result);
	}
	
	@Test
	public void testAppend() {
		assertEquals(Arrays.asList(22, 33, 5), EntryStream.of(createMap())
				.append("dddd", 5).filterKeys(k -> k.length() > 1).values()
				.toList());
	}
	
	@Test
	public void testPrepend() {
		assertEquals(Arrays.asList(5, 22, 33), EntryStream.of(createMap())
				.prepend("dddd", 5).filterKeys(k -> k.length() > 1).values()
				.toList());
	}
	
	@Test
	public void testToMap() {
		TreeMap<String, Integer> result = EntryStream.of(createMap()).toMap(TreeMap::new);
		assertEquals(createMap(), result);
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
		TreeMap<String, List<Integer>> resultTree = EntryStream.of(data).mapKeys(k -> k.substring(0, 1)).grouping(TreeMap::new);
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
		Map<String, Set<Integer>> result = EntryStream.of(data).mapKeys(k -> k.substring(0, 1)).groupingTo(HashSet::new);
		assertEquals(expected, result);
		TreeMap<String, Set<Integer>> resultTree = EntryStream.of(data).mapKeys(k -> k.substring(0, 1)).groupingTo(TreeMap::new, HashSet::new);
		assertEquals(expected, resultTree);
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
		assertEquals(expectedDistinct, StreamEx.of("aaa", "bbb", "bbb", "cc").mapToEntry(String::length).distinct().grouping());
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
	
	private Map<String, Integer> createMap() {
		Map<String, Integer> data = new LinkedHashMap<>();
		data.put("a", 1);
		data.put("bb", 22);
		data.put("ccc", 33);
		return data;
	}
}
