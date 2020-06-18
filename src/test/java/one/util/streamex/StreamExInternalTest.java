/*
 * Copyright 2015, 2020 StreamEx contributors
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

import java.util.Spliterator;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static java.util.Arrays.asList;
import static one.util.streamex.TestHelpers.streamEx;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Tagir Valeev
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StreamExInternalTest {
    @Test
    public void testCreate() {

        StreamEx<String> stream = StreamEx.of("foo", "bar");
        assertSame(stream.stream(), StreamEx.of(stream).stream());
    }

    @Test
    public void testTakeWhileInclusive() {
        streamEx(asList("aaa", "b", "cccc")::stream, s -> {
            assertEquals(asList("aaa", "b"), s.get().takeWhileInclusive(x -> x.length() > 1).toList());
            assertEquals(asList("aaa", "b", "cccc"), s.get().takeWhileInclusive(x -> x.length() > 0).toList());
            assertEquals(asList("aaa"), s.get().takeWhileInclusive(x -> x.length() > 5).toList());
        });
    }

    @Test
    public void testDropWhile() {
        // Test that in JDK9 operation is propagated to JDK dropWhile method.
        boolean hasDropWhile = VerSpec.VER_SPEC.getClass().getSimpleName().equals("Java9Specific");
        Spliterator<String> spliterator = StreamEx.of("aaa", "b", "cccc").dropWhile(x -> x.length() > 1).spliterator();
        assertEquals(hasDropWhile, !spliterator.getClass().getSimpleName().equals("TDOfRef"));
    }

    @Test
    public void testSplit() {
        assertEquals(CharSpliterator.class, StreamEx.split("a#a", "\\#").spliterator().getClass());
    }
}
