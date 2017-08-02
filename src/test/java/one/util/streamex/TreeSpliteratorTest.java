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
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.junit.Test;

import static one.util.streamex.TestHelpers.*;

/**
 * @author Tagir Valeev
 *
 */
public class TreeSpliteratorTest {
    @Test
    public void testPlainSpliterator() {
        List<String> expected = Arrays.asList("", "a", "aa", "aaa", "aaaa", "aaab", "aab", "aaba", "aabb", "ab", "aba",
            "abaa", "abab", "abb", "abba", "abbb", "b", "ba", "baa", "baaa", "baab", "bab", "baba", "babb", "bb", "bba",
            "bbaa", "bbab", "bbb", "bbba", "bbbb");
        checkSpliterator("tree", expected, () -> new TreeSpliterator.Plain<>("", s -> s.length() == 4 ? null
                : Stream.of("a", "b").map(s::concat)));
    }

    @Test
    public void testDepthSpliterator() {
        List<Entry<Integer, String>> expected = StreamEx.of("", "a", "aa", "ab", "ac", "b", "ba", "bb", "bc", "c", "ca",
            "cb", "cc").mapToEntry(e -> e, String::length).invert().toList();
        checkSpliterator("tree", expected, () -> new TreeSpliterator.Depth<>("", (depth, s) -> depth == 2 ? null
                : Stream.of("a", "b", "c").map(s::concat)));
    }
}
