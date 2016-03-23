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
import static one.util.streamex.TestHelpers.*;
import static java.util.Arrays.asList;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.function.BinaryOperator;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import one.util.streamex.DoubleStreamEx.DoubleEmitter;
import one.util.streamex.IntStreamEx.IntEmitter;
import one.util.streamex.LongStreamEx.LongEmitter;
import one.util.streamex.StreamEx.Emitter;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author Tagir Valeev
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EmitterTest {
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

    public static IntEmitter collatzInt(int start) {
        return action -> {
            action.accept(start);
            return start == 1 ? null : collatzInt(start % 2 == 0 ? start / 2 : start * 3 + 1);
        };
    }
    
    public static LongEmitter collatzLong(long start) {
        return action -> {
            action.accept(start);
            return start == 1 ? null : collatzLong(start % 2 == 0 ? start / 2 : start * 3 + 1);
        };
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

    // Perform scanLeft on the iterator
    public static <T> Emitter<T> scanLeft(Iterator<T> iter, T initial, BinaryOperator<T> reducer) {
        return action -> {
            if (!iter.hasNext())
                return null;
            T sum = reducer.apply(initial, iter.next());
            action.accept(sum);
            return scanLeft(iter, sum, reducer);
        };
    }

    public static Emitter<Integer> flatTest(int start) {
        return action -> {
            for (int i = 0; i < start; i++)
                action.accept(start);
            return start == 0 ? null : flatTest(start - 1);
        };
    }

    public static IntEmitter flatTestInt(int start) {
        return action -> {
            for (int i = 0; i < start; i++)
                action.accept(start);
            return start == 0 ? null : flatTestInt(start - 1);
        };
    }
    
    public static LongEmitter flatTestLong(int start) {
        return action -> {
            for (int i = 0; i < start; i++)
                action.accept(start);
            return start == 0 ? null : flatTestLong(start - 1);
        };
    }
    
    public static DoubleEmitter flatTestDouble(int start) {
        return action -> {
            for (int i = 0; i < start; i++)
                action.accept(start);
            return start == 0 ? null : flatTestDouble(start - 1);
        };
    }
    
    public static LongStreamEx primes() {
        return ((LongEmitter)(action -> {
            action.accept(2);
            return primes(3, x -> x % 2 != 0);
        })).stream();
    }
    
    private static LongEmitter primes(long start, LongPredicate isPrime) {
        return action -> {
            long nextPrime = LongStreamEx.range(start, Long.MAX_VALUE, 2).findFirst(isPrime).getAsLong();
            action.accept(nextPrime);
            return primes(nextPrime+2, isPrime.and(x -> x % nextPrime != 0));
        };
    }
    
    @Test
    public void testEmitter() {
        assertEquals(asList(17, 52, 26, 13, 40, 20, 10, 5, 16, 8, 4, 2, 1), collatz(17).stream().toList());
        checkSpliterator("collatz", asList(17, 52, 26, 13, 40, 20, 10, 5, 16, 8, 4, 2, 1), collatz(17)::spliterator);
        assertArrayEquals(new int[] {17, 52, 26, 13, 40, 20, 10, 5, 16, 8, 4, 2, 1}, collatzInt(17).stream().toArray());
        checkSpliterator("collatzInt", asList(17, 52, 26, 13, 40, 20, 10, 5, 16, 8, 4, 2, 1), collatzInt(17)::spliterator);
        assertArrayEquals(new long[] {17, 52, 26, 13, 40, 20, 10, 5, 16, 8, 4, 2, 1}, collatzLong(17).stream().toArray());
        checkSpliterator("collatzLong", asList(17L, 52L, 26L, 13L, 40L, 20L, 10L, 5L, 16L, 8L, 4L, 2L, 1L),
            collatzLong(17)::spliterator);
        assertTrue(collatz(17).stream().has(1));
        assertTrue(collatzInt(17).stream().has(1));
        assertFalse(collatzLong(17).stream().has(0));
        assertEquals(asList(1, 2, 3, 4, 5, 6, 7, 8, 9), iterate(1, x -> x < 10, x -> x + 1).stream().toList());

        // Extracting to variables is necessary to work-around javac <8u40 bug
        String expected = "354224848179261915075";
        String actual = fibonacci().skip(99).findFirst().get().toString();
        assertEquals(expected, actual);
        assertEquals(asList(1, 1, 2, 3, 5, 8, 13, 21, 34, 55), fibonacci().map(BigInteger::intValueExact).limit(10)
                .toList());

        assertEquals(asList("aa", "aabbb", "aabbbc"), scanLeft(asList("aa", "bbb", "c").iterator(), "", String::concat)
                .stream().toList());
        
        assertEquals(asList(4, 4, 4, 4, 3, 3, 3, 2, 2, 1), flatTest(4).stream().toList());
        assertEquals(asList(4, 4, 4, 4, 3, 3, 3, 2, 2), flatTest(4).stream().limit(9).toList());
        
        checkSpliterator("flatTest", asList(4, 4, 4, 4, 3, 3, 3, 2, 2, 1), flatTest(4)::spliterator);
        checkSpliterator("flatTest", asList(4, 4, 4, 4, 3, 3, 3, 2, 2, 1), flatTestInt(4)::spliterator);
        checkSpliterator("flatTest", asList(4L, 4L, 4L, 4L, 3L, 3L, 3L, 2L, 2L, 1L), flatTestLong(4)::spliterator);
        checkSpliterator("flatTest", asList(4.0, 4.0, 4.0, 4.0, 3.0, 3.0, 3.0, 2.0, 2.0, 1.0), flatTestDouble(4)::spliterator);
        
        assertEquals(7919L, primes().skip(999).findFirst().getAsLong());
    }
}
