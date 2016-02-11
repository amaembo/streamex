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
import java.util.function.Supplier;

import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class ZipSpliteratorTest {
    @Test
    public void testEven() {
        Supplier<Spliterator<String>> s = () -> new ZipSpliterator<>(IntStreamEx.range(1000).spliterator(), 
                IntStreamEx.range(1, 1001).spliterator(), (x, y) -> x+":"+y, true);
        List<String> expected = IntStreamEx.range(1000).mapToObj(x -> x+":"+(x+1)).toList();
        checkSpliterator("even", expected, s);
        s = () -> new ZipSpliterator<>(IntStreamEx.range(1000).spliterator(), 
                IntStreamEx.range(2, 1002).map(x -> x - 1).spliterator(), (x, y) -> x+":"+y, true);
        checkSpliterator("evenMap", expected, s);
        int[] nums = IntStreamEx.range(1000).toArray();
        s = () -> new ZipSpliterator<>(IntStreamEx.of(nums).spliterator(), 
                IntStreamEx.range(2, 1002).map(x -> x - 1).spliterator(), (x, y) -> x+":"+y, true);
        checkSpliterator("evenArray", expected, s);
    }
}
