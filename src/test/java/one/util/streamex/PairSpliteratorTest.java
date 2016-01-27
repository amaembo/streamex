/*
 * Copyright 2015, 2016 Tagir Valeev
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
package one.util.streamex;

import static one.util.streamex.TestHelpers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Random;
import java.util.Spliterator;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;
import one.util.streamex.LongStreamEx;
import one.util.streamex.PairSpliterator;
import one.util.streamex.PairSpliterator.PSOfRef;

import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class PairSpliteratorTest {
    @Test
    public void testSpliterator() {
        int[] ints = IntStreamEx.of(new Random(1), 100).toArray();
        long[] longs = LongStreamEx.of(new Random(1), 100).toArray();
        double[] doubles = DoubleStreamEx.of(new Random(1), 100).toArray();

        checkSpliterator("ref", () -> new PairSpliterator.PSOfRef<>((a, b) -> (a - b), Arrays.spliterator(ints)));
        checkSpliterator("int", () -> new PairSpliterator.PSOfInt((a, b) -> (a - b), null, Arrays.spliterator(ints), PairSpliterator.MODE_PAIRS));
        checkSpliterator("long", () -> new PairSpliterator.PSOfLong((a, b) -> (a - b), null, Arrays.spliterator(longs), PairSpliterator.MODE_PAIRS));
        checkSpliterator("double", () -> new PairSpliterator.PSOfDouble((a, b) -> (a - b), null, Arrays.spliterator(doubles), PairSpliterator.MODE_PAIRS));
        
        // mapFirst
        checkSpliterator("ref", IntStreamEx.of(ints, 1, ints.length).boxed().prepend(ints[0] + 2).toList(),
            () -> new PairSpliterator.PSOfRef<>(a -> a + 2, Arrays.spliterator(ints), true));
        checkSpliterator("int", IntStreamEx.of(ints, 1, ints.length).boxed().prepend(ints[0] + 2).toList(),
            () -> new PairSpliterator.PSOfInt((a, b) -> b, a -> a+2, Arrays.spliterator(ints), PairSpliterator.MODE_MAP_FIRST));
        checkSpliterator("long", LongStreamEx.of(longs, 1, longs.length).boxed().prepend(longs[0] + 2).toList(),
            () -> new PairSpliterator.PSOfLong((a, b) -> b, a -> a+2, Arrays.spliterator(longs), PairSpliterator.MODE_MAP_FIRST));
        checkSpliterator("double", DoubleStreamEx.of(doubles, 1, doubles.length).boxed().prepend(doubles[0] + 2).toList(), 
            () -> new PairSpliterator.PSOfDouble((a, b) -> b, a -> a+2, Arrays.spliterator(doubles), PairSpliterator.MODE_MAP_FIRST));
        
        // mapLast
        checkSpliterator("ref", IntStreamEx.of(ints, 0, ints.length-1).boxed().append(ints[ints.length-1] + 2).toList(),
            () -> new PairSpliterator.PSOfRef<>(a -> a + 2, Arrays.spliterator(ints), false));
        checkSpliterator("int", IntStreamEx.of(ints, 0, ints.length-1).boxed().append(ints[ints.length-1] + 2).toList(),
            () -> new PairSpliterator.PSOfInt((a, b) -> a, a -> a+2, Arrays.spliterator(ints), PairSpliterator.MODE_MAP_LAST));
        checkSpliterator("long", LongStreamEx.of(longs, 0, longs.length-1).boxed().append(longs[longs.length-1] + 2).toList(),
            () -> new PairSpliterator.PSOfLong((a, b) -> a, a -> a+2, Arrays.spliterator(longs), PairSpliterator.MODE_MAP_LAST));
        checkSpliterator("double", DoubleStreamEx.of(doubles, 0, doubles.length-1).boxed().append(doubles[doubles.length-1] + 2).toList(),
            () -> new PairSpliterator.PSOfDouble((a, b) -> a, a -> a+2, Arrays.spliterator(doubles), PairSpliterator.MODE_MAP_LAST));
    }
    
    @Test
    public void testCharacteristics() {
        PSOfRef<Integer, Integer> ps = new PairSpliterator.PSOfRef<>((a, b) -> (a - b), IntStreamEx.range(100).spliterator());
        assertTrue(ps.hasCharacteristics(Spliterator.SIZED));
        assertTrue(ps.hasCharacteristics(Spliterator.ORDERED));
        assertTrue(ps.hasCharacteristics(Spliterator.IMMUTABLE));
        assertEquals(99, ps.getExactSizeIfKnown());
    }
}
