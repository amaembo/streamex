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

import static org.junit.Assert.*;
import static java.util.Arrays.asList;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Spliterator;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import one.util.streamex.StreamEx.Emitter;

import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class StreamExEmitterTest {
    // Like Stream.generate(supplier)
    public static <T> Emitter<T> generate(Supplier<T> supplier) {
        return new Emitter<T>() {
            @Override
            public Emitter<T> next(Consumer<? super T> action) {
                action.accept(supplier.get());
                return this;
            }
        };
    }

    // Like Java-9 Stream.iterate(seed, test, op)
    public static <T> Emitter<T> iterate(T seed, Predicate<? super T> test, UnaryOperator<T> op) {
        return test.test(seed) ? action -> {
            action.accept(seed);
            return iterate(op.apply(seed), test, op);
        } : null;
    }

    // Collatz sequence starting from given number
    public static Emitter<Integer> collatz(int start) {
        return action -> {
            action.accept(start);
            return start == 1 ? null : collatz(start % 2 == 0 ? start / 2 : start * 3 + 1);
        };
    }
    
    // Reads numbers from scanner stopping when non-number is encountered
    public static Emitter<Integer> scannerInts(Scanner sc) {
        return sc.hasNextInt() ? action -> {
            action.accept(sc.nextInt());
            return scannerInts(sc);
        } : null;
    }

    // Stream of Fibonacci numbers
    public static StreamEx<BigInteger> fibonacci() {
        return fibonacci(BigInteger.ONE, BigInteger.ZERO).stream();
    }
    
    private static Emitter<BigInteger> fibonacci(BigInteger first, BigInteger second) {
        return action -> {
            BigInteger next = first.add(second);
            action.accept(next);
            return fibonacci(second, next);
        };
    }

    // Adapt spliterator to emitter
    public static <T> Emitter<T> fromSpliterator(Spliterator<T> spltr) {
        return action -> spltr.tryAdvance(action) ? fromSpliterator(spltr) : null;
    }

    // Adapt iterator to emitter
    public static <T> Emitter<T> fromIterator(Iterator<T> iter) {
        return action -> {
            if(!iter.hasNext()) 
                return null;
            action.accept(iter.next());
            return fromIterator(iter);
        };
    }

    // Perform scanLeft on the iterator 
    public static <T> Emitter<T> scanLeft(Iterator<T> iter, T initial, BinaryOperator<T> reducer) {
        return action -> {
            if(!iter.hasNext()) 
                return null;
            T sum = reducer.apply(initial, iter.next());
            action.accept(sum);
            return scanLeft(iter, sum, reducer);
        };
    }
    
    // Stream of all matches of given matcher
    public static Emitter<String> matches(Matcher m) {
        return m.find() ? action -> {
            action.accept(m.group());
            return matches(m);
        } : null;
    }
    
    @Test
    public void testEmitter() {
        assertEquals(asList(17, 52, 26, 13, 40, 20, 10, 5, 16, 8, 4, 2, 1), collatz(17).stream().toList());
        assertTrue(collatz(17).stream().has(1));
        assertEquals(asList(4, 4, 4, 4, 4), generate(() -> 4).stream().limit(5).toList());
        assertEquals(asList(1, 2, 3, 4, 5, 6, 7, 8, 9), iterate(1, x -> x < 10, x -> x + 1).stream().toList());
        Scanner sc = new Scanner("1 2 3 4 test");
        assertEquals(asList(1, 2, 3, 4), scannerInts(sc).stream().toList());
        assertEquals("test", sc.next());
        
        // Extracting to variables is necessary to work-around javac <8u40 bug
        String expected = "354224848179261915075";
        String actual = fibonacci().skip(99).findFirst().get().toString();
        assertEquals(expected, actual);
        assertEquals(asList(1, 1, 2, 3, 5, 8, 13, 21, 34, 55), fibonacci().map(BigInteger::intValueExact).limit(10)
                .toList());
        
        assertEquals(asList("foo", "bar", "baz"), fromSpliterator(asList("foo", "bar", "baz").spliterator()).stream().toList());
        assertEquals(asList("foo", "bar", "baz"), fromIterator(asList("foo", "bar", "baz").iterator()).stream().toList());
        
        assertEquals(asList("123", "543", "111", "5432"), matches(Pattern.compile("\\d+").matcher("123 543,111:5432"))
                .stream().toList());
        
        assertEquals(asList("aa", "aabbb", "aabbbc"), 
            scanLeft(asList("aa", "bbb", "c").iterator(), "", String::concat).stream().toList());
    }
}
