/*
 * Copyright 2015, 2019 StreamEx contributors
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
package one.util.streamex.api;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Test;

import one.util.streamex.StreamEx;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Tagir Valeev
 */
public class StreamExApiTest {
    @Test
    public void testMap() {
        BiFunction<StreamEx<Integer>, Function<Integer, Integer>, StreamEx<Integer>> streamMapper = StreamEx::map;
        assertEquals(asList(2, 4, 10), streamMapper.apply(StreamEx.of(1, 2, 5), x -> x * 2).toList());
    }
    
    @Test
    public void testAppend() {
        List<String> input = asList("a", "b", "c");
        assertEquals(input, input.stream().map(StreamEx::of).reduce(StreamEx::append).get().toList());
    }
    
    @Test
    public void testPrepend() {
        List<String> input = asList("a", "b", "c");
        List<String> expected = asList("c", "b", "a");
        assertEquals(expected, input.stream().map(StreamEx::of).reduce(StreamEx::prepend).get().toList());
    }
    
    @Test
    public void testMapMulti() {
        List<String> result = StreamEx.of("abc", "def", "gh")
                .<String>mapMulti((s, cons) -> s.chars()
                        .mapToObj(ch -> ""+(char)ch).forEach(cons))
                .toList();
        assertEquals(asList("a", "b", "c", "d", "e", "f", "g", "h"), result);
    }
    
    @Test
    public void testMapMultiToInt() {
        int[] result = StreamEx.of("abc", "def", "gh")
                .mapMultiToInt((s, cons) -> s.chars().forEach(cons))
                .toArray();
        assertArrayEquals(new int[] {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'}, result);
    }
    
    @Test
    public void testMapMultiToLong() {
        long[] result = StreamEx.of("abc", "def", "gh")
                .mapMultiToLong((s, cons) -> s.chars().asLongStream().forEach(cons))
                .toArray();
        assertArrayEquals(new long[] {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'}, result);
    }
    
    @Test
    public void testMapMultiToDouble() {
        double[] result = StreamEx.of("abc", "def", "gh")
                .mapMultiToDouble((s, cons) -> s.chars().asDoubleStream().forEach(cons))
                .toArray();
        assertArrayEquals(new double[] {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'}, result, 0.0);
    }
}
