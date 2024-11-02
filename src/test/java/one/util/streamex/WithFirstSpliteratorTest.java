/*
 * Copyright 2015, 2024 StreamEx contributors
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

import java.util.AbstractMap;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;

import static one.util.streamex.TestHelpers.checkSpliterator;
import static one.util.streamex.TestHelpers.consumeElement;
import static org.junit.Assert.*;

/**
 * @author Tagir Valeev
 */
public class WithFirstSpliteratorTest {
    @Test
    public void testSpliterator() {
        checkSpliterator("withFirst", EntryStream.of(0, 0, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5).toList(),
            () -> new WithFirstSpliterator<>(Stream.of(0, 1, 2, 3, 4, 5).spliterator(),
                    AbstractMap.SimpleImmutableEntry::new));
        checkSpliterator("withFirstFlatMap", EntryStream.of(0, 0, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5).toList(),
            () -> new WithFirstSpliterator<>(Stream.of(0, 2, 4).flatMap(x -> Stream.of(x, x + 1)).parallel()
                    .spliterator(), AbstractMap.SimpleImmutableEntry::new));
    }
    
    @Test
    public void testCharacteristics() {
        WithFirstSpliterator<Integer, Integer> spltr = new WithFirstSpliterator<>(Stream.of(6, 1, 2, 3, 4, 5)
                .spliterator(), Integer::sum);
        assertTrue(spltr.hasCharacteristics(Spliterator.SIZED));
        assertEquals(6, spltr.getExactSizeIfKnown());
        consumeElement(spltr, 12);
        assertEquals(5, spltr.getExactSizeIfKnown());
        consumeElement(spltr, 7);
        assertEquals(4, spltr.getExactSizeIfKnown());
        
        spltr = new WithFirstSpliterator<>(Spliterators.emptySpliterator(), Integer::sum);
        assertTrue(spltr.hasCharacteristics(Spliterator.SIZED));
        assertEquals(0, spltr.getExactSizeIfKnown());
        assertFalse(spltr.tryAdvance(x -> fail("Should not happen")));
        assertEquals(0, spltr.getExactSizeIfKnown());
        
        WithFirstSpliterator<Long, Long> longSpltr = new WithFirstSpliterator<>(LongStreamEx.range(Long.MAX_VALUE)
                .spliterator(), Long::sum);
        assertTrue(longSpltr.hasCharacteristics(Spliterator.SIZED));
        assertEquals(Long.MAX_VALUE, longSpltr.getExactSizeIfKnown());
        consumeElement(longSpltr, 0L);
        assertEquals(Long.MAX_VALUE - 1, longSpltr.getExactSizeIfKnown());
        
        longSpltr = new WithFirstSpliterator<>(LongStreamEx.range(-1, Long.MAX_VALUE)
                .spliterator(), Long::sum);
        assertFalse(longSpltr.hasCharacteristics(Spliterator.SIZED));
        assertEquals(Long.MAX_VALUE, longSpltr.estimateSize());
    }
}
