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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Spliterator;

import one.util.streamex.IntStreamEx;
import one.util.streamex.PermutationSpliterator;

import org.junit.Test;

import static org.junit.Assert.*;

public class PermutationSpliteratorTest {
    private static final String PERMUTATIONS_4 = "0123,0132,0213,0231,0312,0321," + "1023,1032,1203,1230,1302,1320,"
        + "2013,2031,2103,2130,2301,2310," + "3012,3021,3102,3120,3201,3210";
    private static final String PERMUTATIONS_3 = "012,021,102,120,201,210";

    private List<String> collect(Spliterator<int[]> spliterator) {
        List<String> strings = new ArrayList<>();
        spliterator.forEachRemaining(i -> strings.add(IntStreamEx.of(i).mapToObj(String::valueOf).joining()));
        return strings;
    }

    private void collectRandomSplit(Spliterator<int[]> spliterator, Random r, List<String> strings) {
        if (spliterator.estimateSize() == 0)
            return;
        int n = r.nextInt((int) spliterator.estimateSize()) + 1;
        for (int i = 0; i < n; i++) {
            if (!spliterator.tryAdvance(is -> strings.add(IntStreamEx.of(is).mapToObj(String::valueOf).joining())))
                return;
        }
        Spliterator<int[]> prefix = spliterator.trySplit();
        if (prefix != null)
            collectRandomSplit(prefix, r, strings);
        collectRandomSplit(spliterator, r, strings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOverflow() {
        new PermutationSpliterator(21);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnderflow() {
        new PermutationSpliterator(-1);
    }

    @Test
    public void testAdvance3() {
        Spliterator<int[]> spliterator = new PermutationSpliterator(3);
        assertEquals(PERMUTATIONS_3, String.join(",", collect(spliterator)));
    }

    @Test
    public void testAdvance4() {
        Spliterator<int[]> spliterator = new PermutationSpliterator(4);
        assertEquals(PERMUTATIONS_4, String.join(",", collect(spliterator)));
    }

    @Test
    public void testSplit3() {
        Spliterator<int[]> spliterator = new PermutationSpliterator(3);
        Spliterator<int[]> prefix = spliterator.trySplit();
        assertNotNull(prefix);
        assertEquals(3, spliterator.getExactSizeIfKnown());
        assertEquals(3, prefix.getExactSizeIfKnown());
        List<String> strings = collect(prefix);
        assertEquals(3, strings.size());
        strings.addAll(collect(spliterator));
        assertEquals(PERMUTATIONS_3, String.join(",", strings));
    }

    @Test
    public void testSplit3Random() {
        Random r = new Random(1);
        for (int i = 0; i < 100; i++) {
            List<String> strings = new ArrayList<>();
            collectRandomSplit(new PermutationSpliterator(3), r, strings);
            assertEquals(String.valueOf(i), PERMUTATIONS_3, String.join(",", strings));
        }
    }

    @Test
    public void testSplit4() {
        Spliterator<int[]> spliterator = new PermutationSpliterator(4);
        Spliterator<int[]> prefix = spliterator.trySplit();
        assertNotNull(prefix);
        assertEquals(12, spliterator.getExactSizeIfKnown());
        assertEquals(12, prefix.getExactSizeIfKnown());
        List<String> strings = collect(prefix);
        assertEquals(12, strings.size());
        strings.addAll(collect(spliterator));
        assertEquals(PERMUTATIONS_4, String.join(",", strings));
    }

    @Test
    public void testSplit4Random() {
        Random r = new Random(1);
        for (int i = 0; i < 100; i++) {
            List<String> strings = new ArrayList<>();
            collectRandomSplit(new PermutationSpliterator(4), r, strings);
            assertEquals(String.valueOf(i), PERMUTATIONS_4, String.join(",", strings));
        }
    }
}
