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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.junit.Test;

/**
 * @author Tagir Valeev
 *
 */
public class TailConcatSpliteratorTest {
    @Test
    public void testCharacteristics() {
        TailConcatSpliterator<Integer> spltr = new TailConcatSpliterator<>(IntStreamEx.range(1000).spliterator(), IntStreamEx.range(1000).spliterator());
        assertTrue(spltr.hasCharacteristics(Spliterator.SIZED));
        assertTrue(spltr.hasCharacteristics(Spliterator.SUBSIZED));
        assertTrue(spltr.hasCharacteristics(Spliterator.ORDERED));
        assertEquals(2000, spltr.getExactSizeIfKnown());

        spltr = new TailConcatSpliterator<>(Spliterators.emptySpliterator(), Spliterators.emptySpliterator());
        assertTrue(spltr.hasCharacteristics(Spliterator.SIZED));
        assertEquals(0, spltr.getExactSizeIfKnown());
        
        spltr = new TailConcatSpliterator<>(IntStreamEx.range(1000).spliterator(), new HashSet<>(Arrays.asList(1,2,3)).spliterator());
        assertTrue(spltr.hasCharacteristics(Spliterator.SIZED));
        assertFalse(spltr.hasCharacteristics(Spliterator.SUBSIZED));
        assertFalse(spltr.hasCharacteristics(Spliterator.ORDERED));
        assertEquals(1003, spltr.getExactSizeIfKnown());
        spltr.tryAdvance(x -> assertEquals(0, (int)x));
        assertEquals(1002, spltr.getExactSizeIfKnown());
        
        TailConcatSpliterator<Long> longSpltr = new TailConcatSpliterator<>(LongStreamEx.range(Long.MAX_VALUE / 2 + 1)
                .spliterator(), LongStreamEx.range(Long.MAX_VALUE / 2 + 1).spliterator());
        assertFalse(longSpltr.hasCharacteristics(Spliterator.SIZED));
        assertFalse(longSpltr.hasCharacteristics(Spliterator.SUBSIZED));
        assertTrue(longSpltr.hasCharacteristics(Spliterator.ORDERED));
        assertEquals(Long.MAX_VALUE, longSpltr.estimateSize());
        longSpltr.tryAdvance(x -> assertEquals(0L, (long)x));
        assertEquals(Long.MAX_VALUE, longSpltr.estimateSize());
        
        Queue<Integer> q = new ConcurrentLinkedDeque<>();
        spltr = new TailConcatSpliterator<>(q.spliterator(), q.spliterator());
        q.add(1);
        assertFalse(longSpltr.hasCharacteristics(Spliterator.SIZED));
        assertTrue(spltr.tryAdvance(x -> assertEquals(1, (int)x)));
        assertTrue(spltr.tryAdvance(x -> assertEquals(1, (int)x)));
        assertFalse(spltr.tryAdvance(x -> fail("Should not happen")));
    }
}
