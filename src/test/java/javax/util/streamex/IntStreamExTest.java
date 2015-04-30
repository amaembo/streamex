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
import java.util.Collections;
import java.util.OptionalInt;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import static org.junit.Assert.*;

public class IntStreamExTest {
    @Test
    public void testCreate() {
        assertArrayEquals(new int[] {}, IntStreamEx.empty().toArray());
        assertArrayEquals(new int[] {}, IntStreamEx.empty().toArray()); // double check is intended
        assertArrayEquals(new int[] { 1 }, IntStreamEx.of(1).toArray());
        assertArrayEquals(new int[] { 1 }, IntStreamEx.of(OptionalInt.of(1)).toArray());
        assertArrayEquals(new int[] {}, IntStreamEx.of(OptionalInt.empty()).toArray());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStreamEx.of(1, 2, 3).toArray());
        assertArrayEquals(new int[] { 4, 6 }, IntStreamEx.of(new int[] {2, 4, 6, 8, 10}, 1, 3).toArray());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStreamEx.of(new byte[] {1,2,3}).toArray());
        assertArrayEquals(new int[] { 4, 6 }, IntStreamEx.of(new byte[] {2, 4, 6, 8, 10}, 1, 3).toArray());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStreamEx.of(new short[] {1,2,3}).toArray());
        assertArrayEquals(new int[] { 4, 6 }, IntStreamEx.of(new short[] {2, 4, 6, 8, 10}, 1, 3).toArray());
        assertArrayEquals(new int[] { 'a', 'b', 'c' }, IntStreamEx.of('a', 'b', 'c').toArray());
        assertArrayEquals(new int[] { '1', 'b' }, IntStreamEx.of(new char[] {'a', '1', 'b', '2', 'c', '3'}, 1, 3).toArray());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStreamEx.of(IntStream.of(1, 2, 3)).toArray());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStreamEx.of(Arrays.asList(1, 2, 3)).toArray());
        assertArrayEquals(new int[] { 0, 1, 2 }, IntStreamEx.range(3).toArray());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStreamEx.range(1, 4).toArray());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStreamEx.rangeClosed(1, 3).toArray());
        assertArrayEquals(new int[] { 1, 2, 4, 8, 16 }, IntStreamEx.iterate(1, x -> x*2).limit(5).toArray());
        assertArrayEquals(new int[] { 1, 1, 1, 1 }, IntStreamEx.generate(() -> 1).limit(4).toArray());
        assertArrayEquals(new int[] { 1, 1, 1, 1 }, IntStreamEx.constant(1, 4).toArray());
        assertArrayEquals(new int[] { 'a', 'b', 'c' }, IntStreamEx.ofChars("abc").toArray());
        assertEquals(10, IntStreamEx.of(new Random(), 10).count());
        assertTrue(IntStreamEx.of(new Random(), 100, 1, 10).allMatch(x -> x >= 1 && x < 10));
        assertArrayEquals(IntStreamEx.of(new Random(1), 100, 1, 10).toArray(), IntStreamEx.of(new Random(1), 1, 10).limit(100).toArray());
        
        IntStream stream = IntStreamEx.of(1, 2, 3);
        assertSame(stream, IntStreamEx.of(stream));

        assertArrayEquals(new int[] { 4, 2, 0, -2, -4 },
                IntStreamEx.zip(new int[] { 5, 4, 3, 2, 1 }, new int[] { 1, 2, 3, 4, 5 }, (a, b) -> a - b)
                        .toArray());
    }
    
    @Test(expected=ArrayIndexOutOfBoundsException.class)
    public void testArrayOffsetUnderflow() {
        IntStreamEx.of(new byte[] {2, 4, 6, 8, 10}, -1, 3).findAny();
    }
    
    @Test(expected=ArrayIndexOutOfBoundsException.class)
    public void testArrayOffsetWrong() {
        IntStreamEx.of(new byte[] {2, 4, 6, 8, 10}, 3, 1).findAny();
    }
    
    @Test(expected=ArrayIndexOutOfBoundsException.class)
    public void testArrayLengthOverflow() {
        IntStreamEx.of(new byte[] {2, 4, 6, 8, 10}, 3, 6).findAny();
    }
    
    @Test
    public void testArrayLengthOk() {
        assertEquals(10, IntStreamEx.of(new byte[] {2, 4, 6, 8, 10}, 3, 5).skip(1).findFirst().getAsInt());
    }
    
    @Test
    public void testOfIndices() {
        assertArrayEquals(new int[] {}, IntStreamEx.ofIndices(new int[0]).toArray());
        assertArrayEquals(new int[] {0,1,2}, IntStreamEx.ofIndices(new int[] {5,-100,1}).toArray());
        assertArrayEquals(new int[] {0,2}, IntStreamEx.ofIndices(new int[] {5,-100,1}, i -> i > 0).toArray());
        assertArrayEquals(new int[] {0,1,2}, IntStreamEx.ofIndices(new long[] {5,-100,1}).toArray());
        assertArrayEquals(new int[] {0,2}, IntStreamEx.ofIndices(new long[] {5,-100,1}, i -> i > 0).toArray());
        assertArrayEquals(new int[] {0,1,2}, IntStreamEx.ofIndices(new double[] {5,-100,1}).toArray());
        assertArrayEquals(new int[] {0,2}, IntStreamEx.ofIndices(new double[] {5,-100,1}, i -> i > 0).toArray());
        assertArrayEquals(new int[] {0,1,2}, IntStreamEx.ofIndices(new String[] {"a", "b", "c"}).toArray());
        assertArrayEquals(new int[] {1}, IntStreamEx.ofIndices(new String[] {"a", "", "c"}, String::isEmpty).toArray());
        assertArrayEquals(new int[] {0,1,2}, IntStreamEx.ofIndices(Arrays.asList("a", "b", "c")).toArray());
        assertArrayEquals(new int[] {1}, IntStreamEx.ofIndices(Arrays.asList("a", "", "c"), String::isEmpty).toArray());
    }

    @Test
    public void testBasics() {
        assertFalse(IntStreamEx.of(1).isParallel());
        assertTrue(IntStreamEx.of(1).parallel().isParallel());
        assertFalse(IntStreamEx.of(1).parallel().sequential().isParallel());
        AtomicInteger i = new AtomicInteger();
        try(IntStreamEx s = IntStreamEx.of(1).onClose(() -> i.incrementAndGet())) {
            assertEquals(1, s.count());
        }
        assertEquals(1, i.get());
        assertEquals(6, IntStreamEx.range(0, 4).sum());
        assertEquals(3, IntStreamEx.range(0, 4).max().getAsInt());
        assertEquals(0, IntStreamEx.range(0, 4).min().getAsInt());
        assertEquals(1.5, IntStreamEx.range(0, 4).average().getAsDouble(), 0.000001);
        assertEquals(4, IntStreamEx.range(0, 4).summaryStatistics().getCount());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStreamEx.range(0, 5).skip(1).limit(3).toArray());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStreamEx.of(3,1,2).sorted().toArray());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStreamEx.of(1, 2, 1, 3, 2).distinct().toArray());
        assertArrayEquals(new int[] { 2, 4, 6 }, IntStreamEx.range(1, 4).map(x -> x*2).toArray());
        assertArrayEquals(new long[] { 2, 4, 6 }, IntStreamEx.range(1, 4).mapToLong(x -> x*2).toArray());
        assertArrayEquals(new double[] { 2, 4, 6 }, IntStreamEx.range(1, 4).mapToDouble(x -> x*2).toArray(), 0.0);
        assertArrayEquals(new int[] { 1, 3 }, IntStreamEx.range(0, 5).filter(x -> x % 2 == 1).toArray());
        assertEquals(6, IntStreamEx.of(1, 2, 3).reduce(Integer::sum).getAsInt());
        assertEquals(Integer.MAX_VALUE, IntStreamEx.rangeClosed(1, Integer.MAX_VALUE).spliterator().getExactSizeIfKnown());
    }
    
    @Test
    public void testElements() {
        assertEquals(Arrays.asList("f", "d", "b"), IntStreamEx.of(5,3,1).elements("abcdef".split("")).toList());
        assertEquals(Arrays.asList("f", "d", "b"), IntStreamEx.of(5,3,1).elements(Arrays.asList("a", "b", "c", "d", "e", "f")).toList());
        assertArrayEquals(new int[] {10,6,2}, IntStreamEx.of(5,3,1).elements(new int[] {0,2,4,6,8,10}).toArray());
        assertArrayEquals(new long[] {10,6,2}, IntStreamEx.of(5,3,1).elements(new long[] {0,2,4,6,8,10}).toArray());
        assertArrayEquals(new double[] {10,6,2}, IntStreamEx.of(5,3,1).elements(new double[] {0,2,4,6,8,10}).toArray(), 0.0);
    }

    @Test
    public void testPrepend() {
        assertArrayEquals(new int[] { -1, 0, 1, 2, 3 }, IntStreamEx.of(1, 2, 3).prepend(-1, 0).toArray());
    }

    @Test
    public void testAppend() {
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStreamEx.of(1, 2, 3).append(4, 5).toArray());
    }

    @Test
    public void testHas() {
        assertTrue(IntStreamEx.range(1, 4).has(3));
        assertFalse(IntStreamEx.range(1, 4).has(4));
    }
    
    @Test
    public void testToBitSet() {
        assertEquals("{0, 1, 2, 3, 4}", IntStreamEx.range(5).toBitSet().toString());
        assertEquals("{0, 2, 3, 4, 10}", IntStreamEx.of(0, 2, 0, 3, 0, 4, 0, 10).parallel().toBitSet().toString());
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
    public void testRemove() {
        assertArrayEquals(new int[] { 1, 2 }, IntStreamEx.of(1, 2, 3).remove(x -> x > 2).toArray());
    }

    @Test
    public void testSort() {
        assertArrayEquals(new int[] { 0, 3, 6, 1, 4, 7, 2, 5, 8 },
                IntStreamEx.range(0, 9).sortedByInt(i -> i % 3 * 3 + i / 3).toArray());
        assertArrayEquals(new int[] { 0, 3, 6, 1, 4, 7, 2, 5, 8 },
                IntStreamEx.range(0, 9).sortedByLong(i -> (long) i % 3 * Integer.MAX_VALUE + i / 3).toArray());
        assertArrayEquals(new int[] { 8, 7, 6, 5, 4, 3, 2, 1 }, IntStreamEx.range(1, 9).sortedByDouble(i -> 1.0 / i)
                .toArray());
        assertArrayEquals(new int[] { 10, 11, 5, 6, 7, 8, 9 }, IntStreamEx.range(5, 12).sortedBy(String::valueOf)
                .toArray());
        assertArrayEquals(new int[] { Integer.MAX_VALUE, 1000, 1, 0, -10, Integer.MIN_VALUE },
                IntStreamEx.of(0, 1, 1000, -10, Integer.MIN_VALUE, Integer.MAX_VALUE).reverseSorted().toArray());
    }
    
    @Test
    public void testToString() {
        assertEquals("LOWERCASE", IntStreamEx.ofChars("lowercase").map(c -> Character.toUpperCase((char)c)).charsToString());
        assertEquals("LOWERCASE", IntStreamEx.ofCodePoints("lowercase").map(Character::toUpperCase).codePointsToString());
    }

    @Test
    public void testMinMax() {
        assertEquals(9, IntStreamEx.range(5, 12).max((a, b) -> String.valueOf(a).compareTo(String.valueOf(b))).getAsInt());
        assertEquals(10, IntStreamEx.range(5, 12).min((a, b) -> String.valueOf(a).compareTo(String.valueOf(b))).getAsInt());
        assertEquals(9, IntStreamEx.range(5, 12).maxBy(String::valueOf).getAsInt());
        assertEquals(10, IntStreamEx.range(5, 12).minBy(String::valueOf).getAsInt());
        assertEquals(5, IntStreamEx.range(5, 12).maxByDouble(x -> 1.0/x).getAsInt());
        assertEquals(11, IntStreamEx.range(5, 12).minByDouble(x -> 1.0/x).getAsInt());
        assertEquals(29, IntStreamEx.of(15, 8, 31, 47, 19, 29).maxByInt(x -> x % 10 * 10 + x / 10).getAsInt());
        assertEquals(31, IntStreamEx.of(15, 8, 31, 47, 19, 29).minByInt(x -> x % 10 * 10 + x / 10).getAsInt());
        assertEquals(29, IntStreamEx.of(15, 8, 31, 47, 19, 29).maxByLong(x -> Long.MIN_VALUE + x % 10 * 10 + x / 10).getAsInt());
        assertEquals(31, IntStreamEx.of(15, 8, 31, 47, 19, 29).minByLong(x -> Long.MIN_VALUE + x % 10 * 10 + x / 10).getAsInt());
    }
    
    private IntStreamEx dropLast(IntStreamEx s) {
        return s.pairMap((a, b) -> a);
    }
    
    @Test
    public void testPairMap() {
        assertEquals(0, IntStreamEx.range(0).pairMap(Integer::sum).count());
        assertEquals(0, IntStreamEx.range(1).pairMap(Integer::sum).count());
        assertEquals(Collections.singletonMap(1, 9999L),
                IntStreamEx.range(10000).pairMap((a, b) -> b - a).boxed().groupingBy(Function.identity(), Collectors.counting()));
        assertEquals(Collections.singletonMap(1, 9999L),
                IntStreamEx.range(10000).parallel().pairMap((a, b) -> b - a).boxed().groupingBy(Function.identity(), Collectors.counting()));
        assertEquals("Test Capitalization Stream",
                IntStreamEx.ofChars("test caPiTaliZation streaM").parallel().prepend(0)
                        .pairMap((c1, c2) -> !Character.isLetter(c1) && Character.isLetter(c2) ? 
                                Character.toTitleCase(c2) : Character.toLowerCase(c2)).charsToString());
        assertArrayEquals(IntStreamEx.range(9999).toArray(), dropLast(IntStreamEx.range(10000)).toArray());
        
        int data[] = new Random(1).ints(1000, 1, 1000).toArray();
        int[] expected = new int[data.length-1];
        int lastSquare = data[0]*data[0];
        for(int i=0; i<expected.length; i++) {
          int newSquare = data[i+1]*data[i+1];
          expected[i] = newSquare - lastSquare;
          lastSquare = newSquare;
        }
        int[] result = IntStreamEx.of(data).map(x -> x*x).pairMap((a, b) -> b - a).toArray();
        assertArrayEquals(expected, result);
    }
}
