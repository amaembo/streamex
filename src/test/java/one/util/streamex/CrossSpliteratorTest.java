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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;

import one.util.streamex.CrossSpliterator;
import one.util.streamex.IntStreamEx;

import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class CrossSpliteratorTest {
    @Test
    public void testCrossToList() {
        for (int limit : new int[] { 1, 2, 4, 9 }) {
            List<List<Integer>> input = Collections.nCopies(3, IntStreamEx.range(limit).boxed().toList());
            List<List<Integer>> expected = IntStreamEx.range(limit * limit * limit).mapToObj(
                i -> Arrays.asList(i / limit / limit, i / limit % limit, i % limit)).toList();
            checkSpliterator("cross", expected, () -> new CrossSpliterator.ToList<>(input));
        }
    }

    @Test
    public void testCrossReduce() {
        for (int limit : new int[] { 1, 2, 4, 9 }) {
            List<List<Integer>> input = Collections.nCopies(3, IntStreamEx.range(limit).boxed().toList());
            List<String> expected = IntStreamEx.range(limit * limit * limit).mapToObj(
                i -> "" + (i / limit / limit) + (i / limit % limit) + (i % limit)).toList();
            checkSpliterator("cross", expected, () -> new CrossSpliterator.Reducing<>(input, "", (s, b) -> s + b));
        }
    }

    @Test
    public void testBigSize() {
        List<List<Integer>> input = new ArrayList<>();
        input.add(IntStreamEx.rangeClosed(1, 20).boxed().toList());
        input.addAll(Collections.nCopies(18, IntStreamEx.rangeClosed(1, 10).boxed().toList()));
        Spliterator<List<Integer>> spltr = new CrossSpliterator.ToList<>(input);
        assertFalse(spltr.hasCharacteristics(Spliterator.SIZED));
        assertEquals(Long.MAX_VALUE, spltr.estimateSize());
        spltr.trySplit();
        assertFalse(spltr.hasCharacteristics(Spliterator.SIZED));
        assertEquals(Long.MAX_VALUE, spltr.estimateSize());
        spltr.trySplit();
        assertTrue(spltr.hasCharacteristics(Spliterator.SIZED));
        assertEquals(5_000_000_000_000_000_000L, spltr.estimateSize());
    }
}
