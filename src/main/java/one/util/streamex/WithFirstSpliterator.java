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

import java.util.Spliterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import one.util.streamex.StreamExInternals.CloneableSpliterator;

/**
 * @author Tagir Valeev
 */
/* package */final class WithFirstSpliterator<T, R> extends CloneableSpliterator<R, WithFirstSpliterator<T, R>> implements Consumer<T> {
    private static final int STATE_NONE = 0;
    private static final int STATE_FIRST_READ = 1;
    private static final int STATE_INIT = 2;
    private static final int STATE_EMPTY = 3;

    private ReentrantLock lock;
    private Spliterator<T> source;
    private WithFirstSpliterator<T, R> prefix;
    private volatile T first;
    private volatile int state = STATE_NONE;
    private final BiFunction<? super T, ? super T, ? extends R> mapper;
    private Consumer<? super R> action;
    
    WithFirstSpliterator(Spliterator<T> source, BiFunction<? super T, ? super T, ? extends R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }
    
    private void acquire() {
        if(lock != null && state == STATE_NONE) {
            lock.lock();
        }
    }
    
    private void release() {
        if(lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        if (state == STATE_NONE) {
            acquire();
            try {
                doInit();
            }
            finally {
                release();
            }
        }
        if (state == STATE_FIRST_READ) {
            state = STATE_INIT;
            action.accept(mapper.apply(first, first));
            return true;
        }
        if (state != STATE_INIT)
            return false;
        this.action = action;
        boolean hasNext = source.tryAdvance(this);
        this.action = null;
        return hasNext;
    }

    private void doInit() {
        int prefixState = state;
        if (prefixState != STATE_NONE)
            return;
        if (prefix != null) {
            prefix.doInit();
            prefixState = prefix.state;
        }
        if (prefixState == STATE_FIRST_READ || prefixState == STATE_INIT) {
            first = prefix.first;
            state = STATE_INIT;
            return;
        }
        state = source.tryAdvance(x -> first = x) ? STATE_FIRST_READ : STATE_EMPTY;
    }

    @Override
    public void forEachRemaining(Consumer<? super R> action) {
        acquire();
        int myState = state;
        this.action = action;
        if(myState == STATE_FIRST_READ || myState == STATE_INIT) {
            release();
            if(myState == STATE_FIRST_READ) {
                state = STATE_INIT;
                accept(first);
            }
            source.forEachRemaining(this);
            this.action = null;
            return;
        }
        try {
            Consumer<T> init = x -> {
                if (state == STATE_NONE) {
                    if (prefix != null) {
                        prefix.doInit();
                    }
                    this.first = (prefix == null || prefix.state == STATE_EMPTY) ? x : prefix.first;
                    state = STATE_INIT;
                }
                release();
            };
            source.forEachRemaining(init.andThen(this));
            this.action = null;
        }
        finally {
            release();
        }
    }

    @Override
    public Spliterator<R> trySplit() {
        if(state != STATE_NONE)
            return null;
        Spliterator<T> prefix;
        if(lock == null)
            lock = new ReentrantLock();
        acquire();
        try {
            if(state != STATE_NONE)
                return null;
            prefix = source.trySplit();
            if (prefix == null)
                return null;
            WithFirstSpliterator<T, R> result = doClone();
            result.source = prefix;
            return this.prefix = result;
        } finally {
            release();
        }
    }

    @Override
    public long estimateSize() {
        return source.estimateSize();
    }

    @Override
    public int characteristics() {
        return NONNULL
            | (source.characteristics() & (DISTINCT | IMMUTABLE | CONCURRENT | ORDERED | (lock == null ? SIZED : 0)));
    }

    @Override
    public void accept(T x) {
        action.accept(mapper.apply(first, x));
    }
}
