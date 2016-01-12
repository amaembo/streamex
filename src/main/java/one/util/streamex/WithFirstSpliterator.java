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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @author Tagir Valeev
 */
/* package */final class WithFirstSpliterator<T> implements Spliterator<Map.Entry<T, T>>, Cloneable {
    private static final int STATE_NONE = 0;
    private static final int STATE_INIT = 1;
    private static final int STATE_EMPTY = 2;

    private ReentrantLock lock;
    private Spliterator<T> source;
    private WithFirstSpliterator<T> prefix;
    private volatile T first;
    private volatile int state = STATE_NONE;
    
    WithFirstSpliterator(Spliterator<T> source) {
        this.source = source;
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
    public boolean tryAdvance(Consumer<? super Entry<T, T>> action) {
        if (state == STATE_NONE) {
            acquire();
            try {
                doInit();
            }
            finally {
                release();
            }
        }
        if (state != STATE_INIT)
            return false;
        return source.tryAdvance(x -> action.accept(new AbstractMap.SimpleImmutableEntry<>(first, x)));
    }

    private void doInit() {
        int prefixState = state;
        if (prefixState != STATE_NONE)
            return;
        if (prefix != null) {
            prefix.doInit();
            prefixState = prefix.state;
        }
        if (prefixState == STATE_INIT) {
            this.first = prefix.first;
            this.state = STATE_INIT;
            return;
        }
        state = source.tryAdvance(x -> first = x) ? STATE_INIT : STATE_EMPTY;
    }

    private boolean initForEach(T x) {
        int prefixState = state;
        if (prefixState != STATE_NONE)
            return false;
        if (prefix != null) {
            prefix.doInit();
            prefixState = prefix.state;
        }
        state = STATE_INIT;
        if (prefixState == STATE_INIT) {
            this.first = prefix.first;
            return false;
        }
        this.first = x;
        return true;
    }

    @Override
    public void forEachRemaining(Consumer<? super Entry<T, T>> action) {
        acquire();
        try {
            source.forEachRemaining(x -> {
                if (state == STATE_NONE) {
                    if (initForEach(x)) {
                        release();
                        return;
                    }
                }
                release();
                action.accept(new AbstractMap.SimpleImmutableEntry<>(first, x));
            });
        }
        finally {
            release();
        }
    }

    @Override
    public Spliterator<Entry<T, T>> trySplit() {
        Spliterator<T> prefix;
        if(lock == null)
            lock = new ReentrantLock();
        acquire();
        try {
            prefix = source.trySplit();
            if (prefix == null)
                return null;
            @SuppressWarnings("unchecked")
            WithFirstSpliterator<T> result = (WithFirstSpliterator<T>) super.clone();
            result.source = prefix;
            return this.prefix = result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        } finally {
            release();
        }
    }

    @Override
    public long estimateSize() {
        long size = source.estimateSize();
        if (size > 0 && size < Long.MAX_VALUE && lock == null && state == STATE_NONE)
            size--;
        return size;
    }

    @Override
    public int characteristics() {
        return NONNULL
            | (source.characteristics() & (DISTINCT | IMMUTABLE | CONCURRENT | ORDERED | (lock == null ? SIZED : 0)));
    }
}
