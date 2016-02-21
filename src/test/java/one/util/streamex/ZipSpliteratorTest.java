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

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;

import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class ZipSpliteratorTest {
    @Test
    public void testEven() {
        List<String> expected = IntStreamEx.range(200).mapToObj(x -> x+":"+(x+1)).toList();
        int[] nums = IntStreamEx.range(200).toArray();
        Supplier<Spliterator<String>> s = () -> new ZipSpliterator<>(IntStreamEx.range(200).spliterator(), 
                IntStreamEx.range(1, 201).spliterator(), (x, y) -> x+":"+y, true);
        checkSpliterator("even", expected, s);
        s = () -> new ZipSpliterator<>(IntStreamEx.range(200).spliterator(), 
                IntStreamEx.range(2, 202).parallel().map(x -> x - 1).spliterator(), (x, y) -> x+":"+y, true);
        checkSpliterator("evenMap", expected, s);
        s = () -> new ZipSpliterator<>(IntStreamEx.of(nums).spliterator(), 
                IntStreamEx.range(2, 202).parallel().map(x -> x - 1).spliterator(), (x, y) -> x+":"+y, true);
        checkSpliterator("evenArray", expected, s);
    }

    @Test
    public void testUnEven() {
        List<String> expected = IntStreamEx.range(200).mapToObj(x -> x+":"+(x+1)).toList();
        Supplier<Spliterator<String>> s = () -> new ZipSpliterator<>(IntStreamEx.range(200).spliterator(), 
                IntStreamEx.range(90).append(IntStreamEx.range(90, 200)).spliterator(), (x, y) -> x+":"+(y+1), true);
        checkSpliterator("unevenRight", expected, s);
        s = () -> new ZipSpliterator<>( 
                IntStreamEx.range(90).append(IntStreamEx.range(90, 200)).spliterator(), 
                IntStreamEx.range(200).spliterator(), (x, y) -> x+":"+(y+1), true);
        checkSpliterator("unevenLeft", expected, s);
    }
    
    @Test
    public void testUnknownSize() {
        List<String> expected = IntStreamEx.range(200).mapToObj(x -> x+":"+(x+1)).toList();
        Supplier<Spliterator<String>> s = () -> new ZipSpliterator<>(Spliterators.spliteratorUnknownSize(IntStreamEx
                .range(200).iterator(), Spliterator.ORDERED), Spliterators.spliteratorUnknownSize(IntStreamEx.range(1,
            201).iterator(), Spliterator.ORDERED), (x, y) -> x + ":" + y, true);
        checkSpliterator("unknownSize", expected, s);
    }
}
