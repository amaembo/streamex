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

import java.util.Collections;
import java.util.Spliterator;

import org.junit.Test;

import one.util.streamex.ConstSpliterator.OfRef;

import static one.util.streamex.TestHelpers.checkSpliterator;
import static one.util.streamex.TestHelpers.consumeElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Tagir Valeev
 */
public class ConstSpliteratorTest {
    @Test
    public void testConstant() {
        checkSpliterator("ref", Collections.nCopies(100, "val"), () -> new ConstSpliterator.OfRef<>("val", 100, false));
        checkSpliterator("ref", Collections.nCopies(100, Integer.MIN_VALUE), () -> new ConstSpliterator.OfInt(
                Integer.MIN_VALUE, 100, false));
        checkSpliterator("ref", Collections.nCopies(100, Long.MIN_VALUE), () -> new ConstSpliterator.OfLong(
                Long.MIN_VALUE, 100, false));
        checkSpliterator("ref", Collections.nCopies(100, Double.MIN_VALUE), () -> new ConstSpliterator.OfDouble(
                Double.MIN_VALUE, 100, false));
    }
    
    @Test
    public void testCharacteristics() {
        OfRef<String> spltr = new ConstSpliterator.OfRef<>("val", 4, true);
        assertTrue(spltr.hasCharacteristics(Spliterator.ORDERED));
        assertTrue(spltr.hasCharacteristics(Spliterator.SIZED));
        assertTrue(spltr.hasCharacteristics(Spliterator.SUBSIZED));
        assertTrue(spltr.hasCharacteristics(Spliterator.IMMUTABLE));
        assertFalse(new ConstSpliterator.OfRef<>("val", 4, false).hasCharacteristics(Spliterator.ORDERED));
    }
    
    @Test
    public void testSplit() {
        OfRef<String> spltr = new ConstSpliterator.OfRef<>("val", 4, true);
        assertEquals(4, spltr.getExactSizeIfKnown());
        spltr = spltr.trySplit();
        assertEquals(2, spltr.getExactSizeIfKnown());
        spltr = spltr.trySplit();
        assertEquals(1, spltr.getExactSizeIfKnown());
        assertNull(spltr.trySplit());
        consumeElement(spltr, "val");
        assertEquals(0, spltr.getExactSizeIfKnown());
        assertNull(spltr.trySplit());
    }
}
