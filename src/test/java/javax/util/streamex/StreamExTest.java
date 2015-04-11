package javax.util.streamex;

import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import static org.junit.Assert.*;

public class StreamExTest {
	@Test
	public void testCreate() {
		assertEquals(Arrays.asList(), StreamEx.empty().toList());
		assertEquals(Arrays.asList("a"), StreamEx.of("a").toList());
		assertEquals(Arrays.asList("a", "b"), StreamEx.of("a", "b").toList());
		assertEquals(Arrays.asList("a", "b"),
				StreamEx.of(Arrays.asList("a", "b")).toList());
		assertEquals(Arrays.asList("a", "b"),
				StreamEx.of(Arrays.asList("a", "b").stream()).toList());
		assertEquals(Arrays.asList("a", "b"), StreamEx.split("a,b", ",").toList());
		assertEquals(Arrays.asList("a", "b"), StreamEx.ofLines(new StringReader("a\nb")).toList());
	}

	@Test
	public void testSelect() {
		assertEquals(Arrays.asList("a", "b"),
				StreamEx.of(1, "a", 2, "b", 3, "cc").select(String.class)
						.filter(s -> s.length() == 1).toList());
	}

	@Test
	public void testFlatCollection() {
		Map<Integer, List<String>> data = new LinkedHashMap<>();
		data.put(1, Arrays.asList("a", "b"));
		data.put(2, Arrays.asList("c", "d"));
		assertEquals(Arrays.asList("a", "b", "c", "d"), StreamEx
				.ofEntries(data).flatCollection(Entry::getValue).toList());
	}

	@Test
	public void testAppend() {
		assertEquals(Arrays.asList("a", "b", "c", "d", "e"),
				StreamEx.of("a", "b", "c", "dd").remove(s -> s.length() > 1)
						.append("d", "e").toList());
		assertEquals(
				Arrays.asList("a", "b", "c", "d", "e"),
				StreamEx.of("a", "b", "c")
						.append(Arrays.asList("d", "e").stream()).toList());
	}
	
	@Test
	public void testPrepend() {
		assertEquals(Arrays.asList("d", "e", "a", "b", "c"),
				StreamEx.of("a", "b", "c", "dd").remove(s -> s.length() > 1)
				.prepend("d", "e").toList());
		assertEquals(
				Arrays.asList("d", "e", "a", "b", "c"),
				StreamEx.of("a", "b", "c")
						.prepend(Arrays.asList("d", "e").stream()).toList());
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
		assertEquals(Arrays.asList("a", "cc", "bbb"), StreamEx.of(data).sortedBy(s -> s.length()).toList());
	}
}
