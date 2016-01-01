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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import one.util.streamex.CollapseSpliterator;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import one.util.streamex.StreamExInternals;

import org.junit.Test;

import static one.util.streamex.TestHelpers.*;
import static org.junit.Assert.*;

/**
 * @author Tagir Valeev
 *
 */
public class CollapseSpliteratorTest {
    private static <T> void splitEquals(Spliterator<T> source, BiConsumer<Spliterator<T>, Spliterator<T>> consumer) {
        Spliterator<T> right = new CollapseSpliterator<>(Objects::equals, Function.identity(), StreamExInternals
                .selectFirst(), StreamExInternals.selectFirst(), source);
        Spliterator<T> left = right.trySplit();
        assertNotNull(left);
        consumer.accept(left, right);
    }

    @Test
    public void testSimpleSplit() {
        List<Integer> input = Arrays.asList(1, 1, 1, 2, 2, 2, 2, 2);
        splitEquals(input.spliterator(), (left, right) -> {
            List<Integer> result = new ArrayList<>();
            left.forEachRemaining(result::add);
            right.forEachRemaining(result::add);
            assertEquals(Arrays.asList(1, 2), result);
        });
        splitEquals(input.spliterator(), (left, right) -> {
            List<Integer> result = new ArrayList<>();
            List<Integer> resultRight = new ArrayList<>();
            right.forEachRemaining(resultRight::add);
            left.forEachRemaining(result::add);
            result.addAll(resultRight);
            assertEquals(Arrays.asList(1, 2), result);
        });
        input = IntStreamEx.of(new Random(1), 100, 1, 10).sorted().boxed().toList();
        splitEquals(input.spliterator(), (left, right) -> {
            List<Integer> result = new ArrayList<>();
            List<Integer> resultRight = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                left.tryAdvance(result::add);
                right.tryAdvance(resultRight::add);
            }
            result.addAll(resultRight);
            assertEquals(IntStreamEx.range(1, 10).boxed().toList(), result);
        });
        input = IntStreamEx.constant(100, 100).append(2).prepend(1).boxed().toList();
        splitEquals(StreamEx.of(input).without(100).parallel().spliterator(), (left, right) -> {
            List<Integer> result = new ArrayList<>();
            left.forEachRemaining(result::add);
            right.forEachRemaining(result::add);
            assertEquals(Arrays.asList(1, 2), result);
        });
        input = Arrays.asList(0, 0, 1, 1, 1, 1, 4, 6, 6, 3, 3, 10);
        splitEquals(Stream.concat(Stream.empty(), input.parallelStream()).spliterator(), (left, right) -> {
            List<Integer> result = new ArrayList<>();
            left.forEachRemaining(result::add);
            right.forEachRemaining(result::add);
            assertEquals(Arrays.asList(0, 1, 4, 6, 3, 10), result);
        });
    }

    @Test
    public void testNonIdentity() {
        checkNonIdentity(Arrays.asList(1, 2, 5, 6, 7, 8, 10, 11, 15));
        checkNonIdentity(IntStreamEx.range(3, 100).prepend(1).boxed().toList());
    }

    private void checkNonIdentity(List<Integer> input) {
        checkSpliterator("collpase", () -> new CollapseSpliterator<Integer, Entry<Integer, Integer>>(
                (a, b) -> (b - a == 1), a -> new AbstractMap.SimpleEntry<>(a, a),
                (acc, a) -> new AbstractMap.SimpleEntry<>(acc.getKey(), a), (a, b) -> new AbstractMap.SimpleEntry<>(a
                        .getKey(), b.getValue()), input.spliterator()));
    }

    @Test
    public void testMultiSplit() {
        List<Integer> input = Arrays.asList(0, 0, 1, 1, 1, 1, 4, 6, 6, 3, 3, 10);
        multiSplit(input::spliterator);
        multiSplit(() -> Stream.concat(Stream.empty(), input.parallelStream()).spliterator());
    }

    private void multiSplit(Supplier<Spliterator<Integer>> inputSpliterator) throws AssertionError {
        Random r = new Random(1);
        for (int n = 1; n < 100; n++) {
            Spliterator<Integer> spliterator = new CollapseSpliterator<>(Objects::equals, Function.identity(),
                    StreamExInternals.selectFirst(), StreamExInternals.selectFirst(), inputSpliterator.get());
            List<Integer> result = new ArrayList<>();
            List<Spliterator<Integer>> spliterators = new ArrayList<>();
            spliterators.add(spliterator);
            for (int i = 0; i < 8; i++) {
                Spliterator<Integer> split = spliterators.get(r.nextInt(spliterators.size())).trySplit();
                if (split != null)
                    spliterators.add(split);
            }
            Collections.shuffle(spliterators, r);
            for (int i = 0; i < spliterators.size(); i++) {
                try {
                    spliterators.get(i).forEachRemaining(result::add);
                } catch (AssertionError e) {
                    throw new AssertionError("at #" + i, e);
                }
            }
            assertEquals("#" + n, 6, result.size());
        }
    }
}
