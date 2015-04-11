package javax.util.streamex;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

public class IntStreamExTest {
	@Test
	public void testCreate() {
		assertArrayEquals(new int[] {}, IntStreamEx.empty().toArray());
		assertArrayEquals(new int[] {1}, IntStreamEx.of(1).toArray());
		assertArrayEquals(new int[] {1, 2, 3}, IntStreamEx.of(1, 2, 3).toArray());
		assertArrayEquals(new int[] {1, 2, 3}, IntStreamEx.of(Arrays.asList(1, 2, 3)).toArray());
		assertArrayEquals(new int[] {1, 2, 3}, IntStreamEx.range(1, 4).toArray());
		assertArrayEquals(new int[] {1, 2, 3}, IntStreamEx.rangeClosed(1, 3).toArray());
		assertArrayEquals(new int[] {'a', 'b', 'c'}, IntStreamEx.ofChars("abc").toArray());
	}
	
	@Test
	public void testPrepend() {
		assertArrayEquals(new int[] { -1, 0, 1, 2, 3 }, IntStreamEx.of(1, 2, 3)
				.prepend(-1, 0).toArray());
	}
	
	@Test
	public void testAppend() {
		assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStreamEx.of(1, 2, 3)
				.append(4, 5).toArray());
	}
	
	@Test
	public void testHas() {
		assertTrue(IntStreamEx.range(1, 4).has(3));
		assertFalse(IntStreamEx.range(1, 4).has(4));
	}
}
