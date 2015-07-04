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

import static javax.util.streamex.TestHelpers.*;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class PairSpliteratorTest {
    @Test
    public void testSpliterator() {
        int[] ints = IntStreamEx.of(new Random(1), 100).toArray();
        checkSpliterator("pair", () -> new PairSpliterator.PSOfRef<>((a, b) -> (a-b), Arrays.spliterator(ints)));
        checkSpliterator("pair", () -> new PairSpliterator.PSOfInt((a, b) -> (a-b), Arrays.spliterator(ints)));
        long[] longs = LongStreamEx.of(new Random(1), 100).toArray();
        checkSpliterator("pair", () -> new PairSpliterator.PSOfLong((a, b) -> (a-b), Arrays.spliterator(longs)));
        double[] doubles = DoubleStreamEx.of(new Random(1), 100).toArray();
        checkSpliterator("pair", () -> new PairSpliterator.PSOfDouble((a, b) -> (a-b), Arrays.spliterator(doubles)));
    }
}
