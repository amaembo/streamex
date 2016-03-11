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

import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import one.util.streamex.StreamEx.Emitter;

import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class StreamExEmitterTest {
    public static <T> Emitter<T> generate(Supplier<T> supplier) {
        return new Emitter<T>() {
            @Override
            public Emitter<T> next(Consumer<? super T> action) {
                action.accept(supplier.get());
                return this;
            }
        };
    }

    public static <T> Emitter<T> iterate(T seed, Predicate<? super T> test, UnaryOperator<T> op) {
        return test.test(seed) ? action -> {
            action.accept(seed);
            return iterate(op.apply(seed), test, op);
        } : null;
    }

    public static Emitter<Integer> collatz(int start) {
        return action -> {
            action.accept(start);
            return start == 1 ? null : collatz(start % 2 == 0 ? start / 2 : start * 3 + 1);
        };
    }
    
    public static Emitter<Integer> scannerInts(Scanner sc) {
        return sc.hasNextInt() ? action -> {
            action.accept(sc.nextInt());
            return scannerInts(sc);
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
    }
}
