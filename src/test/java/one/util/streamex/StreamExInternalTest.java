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

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;

import static java.util.Arrays.asList;
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
    public void testDropWhile() {
        // Test that in JDK9 operation is propagated to JDK dropWhile method.
        String simpleName = VerSpec.VER_SPEC.getClass().getSimpleName();
        boolean hasDropWhile = simpleName.equals("Java9Specific") || simpleName.equals("Java16Specific");
        Spliterator<String> spliterator = StreamEx.of("aaa", "b", "cccc").dropWhile(x -> x.length() > 1).spliterator();
        assertEquals(hasDropWhile, !spliterator.getClass().getSimpleName().equals("TDOfRef"));
    }

    @Test
    public void testSplit() {
        assertEquals(CharSpliterator.class, StreamEx.split("a#a", "\\#").spliterator().getClass());
    }
    
    @Test
    public void testMapMulti() {
        boolean hasMapMulti = VerSpec.VER_SPEC.getClass().getSimpleName().equals("Java16Specific");
        List<String> data = new ArrayList<>(); 
        StreamEx.of(10, 20, 30).<String>mapMulti((e, cons) -> {
            data.add("MM: "+e);
            cons.accept("T: " + e);
            data.add("MM: " +(e + 1));
            cons.accept("T: " + (e + 1));
        }).into(data);
        // When polyfill is used, results of mapMulti are buffered, so the order of execution is different
        List<String> expected = hasMapMulti ? 
                asList("MM: 10", "T: 10", "MM: 11", "T: 11", "MM: 20", "T: 20", "MM: 21", "T: 21", "MM: 30", "T: 30", "MM: 31", "T: 31") : 
                asList("MM: 10", "MM: 11", "T: 10", "T: 11", "MM: 20", "MM: 21", "T: 20", "T: 21", "MM: 30", "MM: 31", "T: 30", "T: 31");
        assertEquals(expected, data);
    }
}
