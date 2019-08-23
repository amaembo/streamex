/*
 * Copyright 2015, 2019 StreamEx contributors
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

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

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
}
