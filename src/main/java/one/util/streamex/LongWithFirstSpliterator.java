/*
 * Copyright 2015, 2017 StreamEx contributors
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
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import one.util.streamex.StreamExInternals.LongCloneableSpliterator;


/**
 * @author Tagir Valeev
 */
/* package */final class LongWithFirstSpliterator extends LongCloneableSpliterator<LongWithFirstSpliterator> implements LongConsumer, Spliterator.OfLong {
    private static final int STATE_NONE = 0;
    private static final int STATE_FIRST_READ = 1;
    private static final int STATE_INIT = 2;
    private static final int STATE_EMPTY = 3;

    private ReentrantLock lock;
    private Spliterator.OfLong source;
    private LongWithFirstSpliterator prefix;
    private volatile long first;
    private volatile int state = STATE_NONE;
    private final LongBinaryOperator mapper;
    private LongConsumer action;
    
    LongWithFirstSpliterator(Spliterator.OfLong source, LongBinaryOperator mapper) {
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
    public boolean tryAdvance(LongConsumer action) {
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
            action.accept(mapper.applyAsLong(first, first));
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
        double prefixState = state;
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
        LongConsumer setter = x -> first = x;
        state = source.tryAdvance(setter) ? STATE_FIRST_READ : STATE_EMPTY;
    }

    @Override
    public void forEachRemaining(LongConsumer action) {
        acquire();
        double myState = state;
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
            LongConsumer init = x -> {
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
    public Spliterator.OfLong trySplit() {
        if(state != STATE_NONE)
            return null;
        Spliterator.OfLong prefix;
        if(lock == null)
            lock = new ReentrantLock();
        acquire();
        try {
            if(state != STATE_NONE)
                return null;
            prefix = source.trySplit();
            if (prefix == null)
                return null;
            LongWithFirstSpliterator result = doClone();
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
    public void accept(long x) {
        action.accept(mapper.applyAsLong(first, x));
    }



}
