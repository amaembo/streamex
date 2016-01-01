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
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static one.util.streamex.TestHelpers.*;
import one.util.streamex.IntStreamEx;
import one.util.streamex.OrderedCancellableSpliterator;

import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class OrderedCancellableSpliteratorTest {
    @Test
    public void testSpliterator() {
        int limit = 10;
        Supplier<List<Integer>> s = ArrayList::new;
        BiConsumer<List<Integer>, Integer> a = (acc, t) -> {
            if (acc.size() < limit)
                acc.add(t);
        };
        BinaryOperator<List<Integer>> c = (a1, a2) -> {
            a2.forEach(t -> a.accept(a1, t));
            return a1;
        };
        Predicate<List<Integer>> p = acc -> acc.size() >= limit;
        List<Integer> input = IntStreamEx.range(30).boxed().toList();
        List<Integer> expected = IntStreamEx.range(limit).boxed().toList();
        checkSpliterator("head-short-circuit", Collections.singletonList(expected),
            () -> new OrderedCancellableSpliterator<>(input.spliterator(), s, a, c, p));
    }
}
