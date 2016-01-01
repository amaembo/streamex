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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;

import one.util.streamex.IntStreamEx;
import one.util.streamex.PairPermutationSpliterator;

import org.junit.Test;

import static one.util.streamex.TestHelpers.*;
import static org.junit.Assert.*;

/**
 * @author Tagir Valeev
 */
public class PairPermutationSpliteratorTest {
    @Test
    public void testSqrt() {
        for (int rev = 0; rev < 1000; rev++) {
            int row = (int) (Math.sqrt(8 * rev + 1) - 1) / 2;
            int row2 = PairPermutationSpliterator.isqrt(rev);
            assertEquals(row, row2);
        }
        for (int row : new int[] { 1_000_000_000, 2_000_000_000, Integer.MAX_VALUE - 1, Integer.MAX_VALUE }) {
            assertEquals(row, PairPermutationSpliterator.isqrt(row * (row + 1L) / 2));
            assertEquals(row - 1, PairPermutationSpliterator.isqrt(row * (row + 1L) / 2 - 1));
        }
    }

    @Test
    public void testCharacteristics() {
        PairPermutationSpliterator<Integer, Integer> spltr = new PairPermutationSpliterator<>(Arrays.asList(1, 2, 3),
                Integer::sum);
        assertTrue(spltr.hasCharacteristics(Spliterator.ORDERED));
        assertTrue(spltr.hasCharacteristics(Spliterator.SIZED));
        assertTrue(spltr.hasCharacteristics(Spliterator.SUBSIZED));
        assertEquals(3, spltr.getExactSizeIfKnown());
    }

    @Test
    public void testSpliterator() {
        for (int i : IntStreamEx.rangeClosed(2, 13).boxed()) {
            List<Integer> input = IntStreamEx.range(i).boxed().toList();
            List<Map.Entry<Integer, Integer>> expected = IntStreamEx.range(i)
                    .<Map.Entry<Integer, Integer>> flatMapToObj(
                        a -> IntStreamEx.range(a + 1, i).mapToObj(b -> new AbstractMap.SimpleEntry<>(a, b))).toList();
            checkSpliterator("#" + i, expected, () -> new PairPermutationSpliterator<>(input,
                    AbstractMap.SimpleEntry<Integer, Integer>::new));
        }
    }
}
