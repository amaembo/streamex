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
}
