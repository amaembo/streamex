/*
 * Copyright 2015, 2023 StreamEx contributors
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

import java.io.IOException;
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
    public static class DomainException extends Exception {}

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

    /* ************************************************************* */
    /* Here some unit tests to explain interests of my modifications */
    /* ************************************************************* */

    /*
        We can see in this example, when i use conventional map method, it is impossible to use
        functions that can throws throwable exception.
        It is necessary either :
         - to prevent any exception to throws
         - code a try-catch block in lambda like in the example to wrap exceptions in a RuntimeException
     */
    @Test
    public void testMap() {
        List<String> result = StreamEx.of("Luke", "Leïa", "Anakin")
            .map(firstname -> {
                try {
                    if (firstname.equals("Error"))
                        throw new DomainException();
                    return firstname + " Skywalker";
                } catch (DomainException e) {
                    throw new RuntimeException(e);
                }
            }).toList();
        assertEquals("map throwing", asList("Luke Skywalker", "Leïa Skywalker", "Anakin Skywalker"), result);
    }

    /*

     */
    @Test
    public void testMapThrowing() throws DomainException {
        List<String> result = StreamEx.of("Luke", "Leïa", "Anakin")
            .mapThrowing(firstname -> {
                if (firstname.equals("Error"))
                    throw new DomainException();
                return firstname + " Skywalker";
            }).toList();
        assertEquals("map throwing", asList("Luke Skywalker", "Leïa Skywalker", "Anakin Skywalker"), result);
    }

    @Test
    public void testTryMap() {
        List<String> result = StreamEx.of("Luke", "Leïa", "Anakin")
            .tryMap(firstname -> {
                if (firstname.equals("Error"))
                    throw new DomainException();
                return firstname + " Skywalker";
            }).toList();
        assertEquals("map throwing", asList("Luke Skywalker", "Leïa Skywalker", "Anakin Skywalker"), result);
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

    @Test
    public void testMapMultiThrowing() throws DomainException {
        boolean hasMapMulti = VerSpec.VER_SPEC.getClass().getSimpleName().equals("Java16Specific");
        List<String> data = new ArrayList<>();
        StreamEx.of(10, 20, 30).<DomainException, String>mapMultiThrowing((e, cons) -> {
            if(e > 50)
                throw new DomainException();
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

    @Test
    public void testTryMapMulti() {
        boolean hasMapMulti = VerSpec.VER_SPEC.getClass().getSimpleName().equals("Java16Specific");
        List<String> data = new ArrayList<>();
        StreamEx.of(10, 20, 30).<String>tryMapMulti((e, cons) -> {
            if(e > 50)
                throw new DomainException();
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
