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
package one.util.streamex;

import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import one.util.streamex.Internals.CloneableSpliterator;

import static one.util.streamex.Internals.NONE;

/**
 * @author Tagir Valeev
 */
/* package */final class WithFirstSpliterator<T, R> extends CloneableSpliterator<R, WithFirstSpliterator<T, R>> {
    private static final Object lock = new Object();
    private Spliterator<T> source;
    private WithFirstSpliterator<T, R> prefix;
    private T first = none();
    private T[] firstRefHolder;
    boolean firstToBeConsumed;
    private final BiFunction<? super T, ? super T, ? extends R> mapper;
    
    WithFirstSpliterator(Spliterator<T> source, BiFunction<? super T, ? super T, ? extends R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }
    
    @SuppressWarnings("unchecked")
    static <T> T[] arrayOfNone() {
        return (T[]) new Object[]{NONE};
    }
    
    @SuppressWarnings("unchecked")
    static <T> T none() {
        return (T) NONE;
    }
    
    @Override
    public Spliterator<R> trySplit() {
        boolean isInit = first != NONE || (firstRefHolder != null && (first = firstRefHolder[0]) != NONE);
        if (isInit) {
            return null;
        }
        if (firstRefHolder == null) {
            firstRefHolder = arrayOfNone();
        }
        synchronized (lock) {
            if (isInit()) {
                return null;
            }
            Spliterator<T> prefix = source.trySplit();
            if (prefix == null) {
                return null;
            }
            WithFirstSpliterator<T, R> result = doClone();
            result.source = prefix;
            return this.prefix = result;
        }
    }
    
    private boolean isInit() {
        return first != NONE || (first = firstRefHolder[0]) != NONE;
    }
    
    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        boolean isNotInit = first == NONE && (firstRefHolder == null || (first = firstRefHolder[0]) == NONE);
        if (isNotInit) {
            if (firstRefHolder == null) {
                return source.tryAdvance(x -> {
                    first = x;
                    action.accept(mapper.apply(first, x));
                });
            } else {
                doInit();
                if (first == NONE) {
                    return false;
                }
            }
        }
        if (firstToBeConsumed) {
            firstToBeConsumed = false;
            action.accept(mapper.apply(first, first));
            return true;
        }
        return source.tryAdvance(x -> action.accept(mapper.apply(first, x)));
    }
    
    private void doInit() {
        synchronized (lock) {
            if (prefix != null) {
                prefix.doInit();
            }
            if (!isInit()) {
                source.tryAdvance(x -> {
                    firstToBeConsumed = true;
                    first = x;
                    firstRefHolder[0] = x;
                });
            }
        }
    }
    
    @Override
    public void forEachRemaining(Consumer<? super R> action) {
        if (firstRefHolder == null) {
            Consumer<T> init = x -> {
                if (first == NONE) {
                    first = x;
                }
            };
            source.forEachRemaining(init.andThen(x -> action.accept(mapper.apply(first, x))));
        } else {
            init();
            if (firstToBeConsumed) {
                firstToBeConsumed = false;
                action.accept(mapper.apply(first, first));
            }
            source.forEachRemaining(x -> action.accept(mapper.apply(first, x)));
        }
    }
    
    private void init() {
        if (!isInit()) {
            doInit();
        }
    }
    
    @Override
    public long estimateSize() {
        return source.estimateSize();
    }
    
    @Override
    public int characteristics() {
        return NONNULL
                | (source.characteristics() & (DISTINCT | IMMUTABLE | CONCURRENT | ORDERED | (firstRefHolder == null ? SIZED : 0)));
    }
}
