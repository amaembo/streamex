package javax.util.streamex;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

public class LongStreamExTest {
	@Test
	public void testCreate() {
		assertArrayEquals(new long[] {}, LongStreamEx.empty().toArray());
		assertArrayEquals(new long[] {1}, LongStreamEx.of(1).toArray());
		assertArrayEquals(new long[] {1, 2, 3}, LongStreamEx.of(1, 2, 3).toArray());
		assertArrayEquals(new long[] {1, 2, 3}, LongStreamEx.of(Arrays.asList(1L, 2L, 3L)).toArray());
		assertArrayEquals(new long[] {1, 2, 3}, LongStreamEx.range(1, 4).toArray());
		assertArrayEquals(new long[] {1, 2, 3}, LongStreamEx.rangeClosed(1, 3).toArray());
	}
	
	@Test
	public void testPrepend() {
		assertArrayEquals(new long[] { -1, 0, 1, 2, 3 }, LongStreamEx.of(1, 2, 3)
				.prepend(-1, 0).toArray());
	}
	
	@Test
	public void testAppend() {
		assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStreamEx.of(1, 2, 3)
				.append(4, 5).toArray());
	}
	
	@Test
	public void testHas() {
		assertTrue(LongStreamEx.range(1, 4).has(3));
		assertFalse(LongStreamEx.range(1, 4).has(4));
	}

	@Test
	public void testFind() {
		assertEquals(6, LongStreamEx.range(1, 10).findFirst(i -> i > 5).getAsLong());
		assertFalse(LongStreamEx.range(1, 10).findAny(i -> i > 10).isPresent());
	}

	@Test
	public void testSort() {
		assertArrayEquals(new long[] { 0, 3, 6, 1, 4, 7, 2, 5, 8 }, LongStreamEx
				.range(0, 9).sortedByLong(i -> i % 3 * 3 + i / 3).toArray());
		assertArrayEquals(new long[] { 10, 11, 5, 6, 7, 8, 9 }, LongStreamEx
				.range(5, 12).sortedBy(String::valueOf).toArray());
	}
}
