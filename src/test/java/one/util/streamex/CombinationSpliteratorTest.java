/*
 * Copyright 2015, 2023 StreamEx contributors
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

import java.util.Spliterator;

import org.junit.Test;

import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static java.util.Spliterator.SUBSIZED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CombinationSpliteratorTest {
    @Test
    public void testStepJump() {
        int[][] nk = {
                {1, 1},
                {1, 10},
                {9, 10},
                {5, 10},
                {2, 5},
                {3, 5},
                {8, 16},
                {7, 20},
                {15, 20},
                {20, 20}
        };
        for (int[] ints : nk) {
            int k = ints[0];
            int n = ints[1];
            int[] values = IntStreamEx.range(k).toArray();
            long size = CombinationSpliterator.cnk(n, k);
            assertArrayEquals("n=" + n + ", k=" + k, values, CombinationSpliterator.jump(size - 1, k, n));
            for (long cur = 1; cur < size; cur++) {
                CombinationSpliterator.step(values, n);
                assertArrayEquals("n=" + n + ", k=" + k + ", cur = " + cur, values, CombinationSpliterator.jump(size - 1 - cur, k, n));
            }
        }
    }

    @Test
    public void testCharacteristics() {
        Spliterator<int[]> spliterator = StreamEx.ofCombinations(10, 5).spliterator();
        assertEquals(DISTINCT | IMMUTABLE | NONNULL | ORDERED | SIZED | SUBSIZED, spliterator.characteristics());
        assertEquals(252, spliterator.estimateSize());
    }

    @Test
    public void testTrySplit() {
        Spliterator<int[]> spliterator = StreamEx.ofCombinations(5, 5).spliterator();
        assertEquals(1, spliterator.estimateSize());
        assertNull(spliterator.trySplit());

        spliterator = StreamEx.ofCombinations(2, 1).spliterator();
        assertEquals(2, spliterator.estimateSize());
        assertNotNull(spliterator.trySplit());

        spliterator = StreamEx.ofCombinations(2, 1).spliterator();
        assertEquals(2, spliterator.estimateSize());
        boolean[] readZero = {false};
        assertTrue(spliterator.tryAdvance(x -> readZero[0] = x[0] == 0));
        assertTrue(readZero[0]);
        assertNull(spliterator.trySplit());

        spliterator = StreamEx.ofCombinations(4, 1).spliterator();
        assertEquals(4, spliterator.estimateSize());
        assertNotNull(spliterator.trySplit());
        assertEquals(2, spliterator.estimateSize());
        assertNotNull(spliterator.trySplit());
        assertEquals(1, spliterator.estimateSize());
        assertNull(spliterator.trySplit());
    }
}
