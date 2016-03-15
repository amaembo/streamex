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
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;

import org.junit.Test;

import static one.util.streamex.TestHelpers.*;
import static org.junit.Assert.*;

/**
 * @author Tagir Valeev
 */
public class WithFirstSpliteratorTest {
    @Test
    public void testSpliterator() {
        checkSpliterator("withFirst", EntryStream.of(0, 0, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5).toList(),
            () -> new WithFirstSpliterator<>(Stream.of(0, 1, 2, 3, 4, 5).spliterator(),
                    AbstractMap.SimpleImmutableEntry<Integer, Integer>::new));
        checkSpliterator("withFirstFlatMap", EntryStream.of(0, 0, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5).toList(),
            () -> new WithFirstSpliterator<>(Stream.of(0, 2, 4).flatMap(x -> Stream.of(x, x + 1)).parallel()
                    .spliterator(), AbstractMap.SimpleImmutableEntry<Integer, Integer>::new));
    }
    
    @Test
    public void testCharacteristics() {
        WithFirstSpliterator<Integer, Integer> spltr = new WithFirstSpliterator<>(Stream.of(6, 1, 2, 3, 4, 5)
                .spliterator(), (a, b) -> a + b);
        assertTrue(spltr.hasCharacteristics(Spliterator.SIZED));
        assertEquals(6, spltr.getExactSizeIfKnown());
        assertTrue(spltr.tryAdvance(x -> assertEquals(12, (int)x)));
        assertEquals(5, spltr.getExactSizeIfKnown());
        assertTrue(spltr.tryAdvance(x -> assertEquals(7, (int)x)));
        assertEquals(4, spltr.getExactSizeIfKnown());
        
        spltr = new WithFirstSpliterator<>(Spliterators.emptySpliterator(), (a, b) -> a + b);
        assertTrue(spltr.hasCharacteristics(Spliterator.SIZED));
        assertEquals(0, spltr.getExactSizeIfKnown());
        assertFalse(spltr.tryAdvance(x -> fail("Should not happen")));
        assertEquals(0, spltr.getExactSizeIfKnown());
        
        WithFirstSpliterator<Long, Long> longSpltr = new WithFirstSpliterator<>(LongStreamEx.range(Long.MAX_VALUE)
                .spliterator(), Long::sum);
        assertTrue(longSpltr.hasCharacteristics(Spliterator.SIZED));
        assertEquals(Long.MAX_VALUE, longSpltr.getExactSizeIfKnown());
        assertTrue(longSpltr.tryAdvance(x -> assertEquals(0, (long)x)));
        assertEquals(Long.MAX_VALUE-1, longSpltr.getExactSizeIfKnown());
        
        longSpltr = new WithFirstSpliterator<>(LongStreamEx.range(-1, Long.MAX_VALUE)
                .spliterator(), Long::sum);
        assertFalse(longSpltr.hasCharacteristics(Spliterator.SIZED));
        assertEquals(Long.MAX_VALUE, longSpltr.estimateSize());
    }
}
