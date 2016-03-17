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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Tagir Valeev
 */
public class CollectorBuilderTest {
    @Test
    public void testBasics() {
        Map<Integer, List<String>> res = IntStreamEx.range(22).boxed().into(
            c -> c.groupingBy(x -> x / 10, 
                c1 -> c1.flatMap(x -> StreamEx.of(x, x*2)).map(x -> "[" + x + "]").limit(5).toList()));
        Map<Integer, List<String>> expected = EntryStream.of(
            0, Arrays.asList("[0]", "[0]", "[1]", "[2]", "[2]"),
            1, Arrays.asList("[10]", "[20]", "[11]", "[22]", "[12]"),
            2, Arrays.asList("[20]", "[40]", "[21]", "[42]")).toMap();
        assertEquals(expected, res);
        
        Map<Integer, String> res2 = StreamEx.of("test", "best", "foo", "bar").into(
            c -> c.groupingBy(String::length, 
                c1 -> c1.max(Comparator.naturalOrder()).get()));
        Map<Integer, String> expected2 = EntryStream.of(4, "test", 3, "foo").toMap();
        assertEquals(expected2, res2);
        
        AtomicInteger cnt = new AtomicInteger();
        Map<Boolean, List<Integer>> res3 = IntStreamEx.range(100).peek(x -> cnt.incrementAndGet()).boxed().into(c -> c
                .partitioningBy(x -> x % 2 == 0, c1 -> c1.limit(2).toList()));
        Map<Boolean, List<Integer>> expected3 = EntryStream.of(true, Arrays.asList(0, 2), false, Arrays.asList(1, 3)).toMap();
        assertEquals(expected3, res3);
        assertEquals(4, cnt.get());
    }
}
