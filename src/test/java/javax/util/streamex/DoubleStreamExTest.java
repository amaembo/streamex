package javax.util.streamex;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

public class DoubleStreamExTest {
	@Test
	public void testCreate() {
		assertArrayEquals(new double[] {}, DoubleStreamEx.empty().toArray(), 0.0);
		assertArrayEquals(new double[] {1}, DoubleStreamEx.of(1).toArray(), 0.0);
		assertArrayEquals(new double[] {1, 2, 3}, DoubleStreamEx.of(1, 2, 3).toArray(), 0.0);
		assertArrayEquals(new double[] {1, 2, 3}, DoubleStreamEx.of(Arrays.asList(1.0, 2.0, 3.0)).toArray(), 0.0);
	}
	
	@Test
	public void testPrepend() {
		assertArrayEquals(new double[] { -1, 0, 1, 2, 3 }, DoubleStreamEx.of(1, 2, 3)
				.prepend(-1, 0).toArray(), 0.0);
	}
	
	@Test
	public void testAppend() {
		assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStreamEx.of(1, 2, 3)
				.append(4, 5).toArray(), 0.0);
	}
}
