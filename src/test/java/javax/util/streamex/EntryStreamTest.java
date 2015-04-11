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
	
	private Map<String, Integer> createMap() {
		Map<String, Integer> data = new LinkedHashMap<>();
		data.put("a", 1);
		data.put("bb", 22);
		data.put("ccc", 33);
		return data;
	}
}
