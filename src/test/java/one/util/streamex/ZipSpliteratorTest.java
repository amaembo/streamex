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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.function.Supplier;

import static one.util.streamex.TestHelpers.checkSpliterator;
import static one.util.streamex.TestHelpers.consumeElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Tagir Valeev
 */
public class ZipSpliteratorTest {
    @Test
    public void testEven() {
        List<String> expected = IntStreamEx.range(200).mapToObj(x -> x + ":" + (x + 1)).toList();
        int[] nums = IntStreamEx.range(200).toArray();
        Supplier<Spliterator<String>> s = () -> new ZipSpliterator<>(IntStreamEx.range(200).spliterator(),
                IntStreamEx.range(1, 201).spliterator(), (x, y) -> x + ":" + y, true);
        checkSpliterator("even", expected, s);
        s = () -> new ZipSpliterator<>(IntStreamEx.range(200).spliterator(),
                IntStreamEx.range(2, 202).parallel().map(x -> x - 1).spliterator(), (x, y) -> x + ":" + y, true);
        checkSpliterator("evenMap", expected, s);
        s = () -> new ZipSpliterator<>(IntStreamEx.of(nums).spliterator(),
                IntStreamEx.range(2, 202).parallel().map(x -> x - 1).spliterator(), (x, y) -> x + ":" + y, true);
        checkSpliterator("evenArray", expected, s);
    }

    @Test
    public void testUnEven() {
        List<String> expected = IntStreamEx.range(200).mapToObj(x -> x + ":" + (x + 1)).toList();
        Supplier<Spliterator<String>> s = () -> new ZipSpliterator<>(IntStreamEx.range(200).spliterator(), 
                IntStreamEx.range(90).append(IntStreamEx.range(90, 200)).spliterator(), (x, y) -> x + ":" + (y + 1), true);
        checkSpliterator("unevenRight", expected, s);
        s = () -> new ZipSpliterator<>(
                IntStreamEx.range(90).append(IntStreamEx.range(90, 200)).spliterator(),
                IntStreamEx.range(200).spliterator(), (x, y) -> x + ":" + (y + 1), true);
        checkSpliterator("unevenLeft", expected, s);
    }
    
    @Test
    public void testUnknownSize() {
        List<String> expected = IntStreamEx.range(200).mapToObj(x -> x + ":" + (x + 1)).toList();
        Supplier<Spliterator<String>> s = () -> new ZipSpliterator<>(Spliterators.spliteratorUnknownSize(IntStreamEx
                .range(200).iterator(), Spliterator.ORDERED), Spliterators.spliteratorUnknownSize(IntStreamEx.range(1,
            201).iterator(), Spliterator.ORDERED), (x, y) -> x + ":" + y, true);
        checkSpliterator("unknownSize", expected, s);
    }
    
    @Test
    public void testNotSubsized() {
        Supplier<Spliterator<String>> s = () -> new ZipSpliterator<>(new TreeSet<>(Arrays
            .asList(1, 2, 3, 4))
            .spliterator(), new TreeSet<>(Arrays.asList("a", "b", "c", "d")).spliterator(),
            (a, b) -> a + b, true);
        checkSpliterator("not-subsized", Arrays.asList("1a", "2b", "3c", "4d"), s);
    }
    
    @Test
    public void testTrySplit() {
        ZipSpliterator<Integer, String, String> spliterator = new ZipSpliterator<>(Arrays.asList(1, 2, 3, 4)
            .spliterator(), Arrays.asList("a", "b", "c", "d").spliterator(),
            (a, b) -> a + b, true);
        Spliterator<String> prefix = spliterator.trySplit();
        assertTrue(prefix instanceof ZipSpliterator);
        assertEquals(2, prefix.getExactSizeIfKnown());
        assertEquals(2, spliterator.getExactSizeIfKnown());

        // not SUBSIZED
        spliterator = new ZipSpliterator<>(new TreeSet<>(Arrays.asList(1, 2, 3, 4))
            .spliterator(), new TreeSet<>(Arrays.asList("a", "b", "c", "d")).spliterator(),
            (a, b) -> a + b, true);
        prefix = spliterator.trySplit();
        assertTrue(prefix instanceof UnknownSizeSpliterator.USOfRef);
        assertEquals(4, prefix.estimateSize());
        consumeElement(prefix, "1a");
        consumeElement(prefix, "2b");
        consumeElement(prefix, "3c");
        consumeElement(prefix, "4d");
        assertFalse(spliterator.tryAdvance(Assert::fail));

        spliterator = new ZipSpliterator<>(new TreeSet<>(Arrays.asList(1))
            .spliterator(), new TreeSet<>(Arrays.asList("a")).spliterator(),
            (a, b) -> a + b, true);
        assertNull(spliterator.trySplit());

        spliterator = new ZipSpliterator<>(new ConstSpliterator.OfRef<>(1, 1000, true),
            new ConstSpliterator.OfRef<>("a", 4000, true), (a, b) -> a + b, true);
        assertEquals(1000, spliterator.getExactSizeIfKnown());
        prefix = spliterator.trySplit();
        assertTrue(prefix instanceof UnknownSizeSpliterator.USOfRef);
        assertEquals(1000, prefix.estimateSize());
        assertEquals(0, spliterator.estimateSize());

        spliterator = new ZipSpliterator<>(new ConstSpliterator.OfRef<>(1, 1000, true),
            new ConstSpliterator.OfRef<>("a", 1100, true), (a, b) -> a + b, true);
        assertEquals(1000, spliterator.getExactSizeIfKnown());
        prefix = spliterator.trySplit();
        assertTrue(prefix instanceof ZipSpliterator);
        assertEquals(550, prefix.getExactSizeIfKnown());
        assertEquals(450, spliterator.getExactSizeIfKnown());

        spliterator = new ZipSpliterator<>(new ConstSpliterator.OfRef<>(1, 1100, true),
            new ConstSpliterator.OfRef<>("a", 1000, true), (a, b) -> a + b, true);
        assertEquals(1000, spliterator.getExactSizeIfKnown());
        prefix = spliterator.trySplit();
        assertTrue(prefix instanceof ZipSpliterator);
        assertEquals(550, prefix.getExactSizeIfKnown());
        assertEquals(450, spliterator.getExactSizeIfKnown());
        prefix = spliterator.trySplit();
        assertTrue(prefix instanceof UnknownSizeSpliterator.USOfRef);
        assertEquals(450, prefix.estimateSize());
        assertEquals(0, spliterator.estimateSize());
    }
}
