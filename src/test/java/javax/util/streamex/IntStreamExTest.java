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
	
	@Test
	public void testAs() {
		assertEquals(4, IntStreamEx.range(0, 5).asLongStream().findAny(x -> x > 3).getAsLong());
		assertEquals(4.0, IntStreamEx.range(0, 5).asDoubleStream().findAny(x -> x > 3).getAsDouble(), 0.0);
	}
	
	@Test
	public void testFind() {
		assertEquals(6, IntStreamEx.range(1, 10).findFirst(i -> i > 5).getAsInt());
		assertFalse(IntStreamEx.range(1, 10).findAny(i -> i > 10).isPresent());
	}

	@Test
	public void testSort() {
		assertArrayEquals(new int[] { 0, 3, 6, 1, 4, 7, 2, 5, 8 }, IntStreamEx
				.range(0, 9).sortedByInt(i -> i % 3 * 3 + i / 3).toArray());
		assertArrayEquals(new int[] { 0, 3, 6, 1, 4, 7, 2, 5, 8 }, IntStreamEx
				.range(0, 9).sortedByLong(i -> (long)i % 3 * Integer.MAX_VALUE + i / 3).toArray());
		assertArrayEquals(new int[] { 8, 7, 6, 5, 4, 3, 2, 1 }, IntStreamEx
				.range(1, 9).sortedByDouble(i -> 1.0/i).toArray());
		assertArrayEquals(new int[] { 10, 11, 5, 6, 7, 8, 9 }, IntStreamEx
				.range(5, 12).sortedBy(String::valueOf).toArray());
		
	}
}
