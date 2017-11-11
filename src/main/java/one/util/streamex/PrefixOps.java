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

import static one.util.streamex.StreamExInternals.*;

import java.util.Spliterator;
import java.util.Spliterators.AbstractDoubleSpliterator;
import java.util.Spliterators.AbstractIntSpliterator;
import java.util.Spliterators.AbstractLongSpliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;

/**
 * @author Tagir Valeev
 */
/* package */ abstract class PrefixOps<T, S extends Spliterator<T>> extends CloneableSpliterator<T, PrefixOps<T, S>>{
    private static final int BUF_SIZE = 128;
    
    S source;
    AtomicReference<T> accRef;
    T acc = none();
    int idx = 0;
    final BinaryOperator<T> op;
    
    PrefixOps(S source, BinaryOperator<T> op) {
        this.source = source;
        this.op = op;
    }
    
    @Override
    public Spliterator<T> trySplit() {
        if(acc != NONE) {
            return null;
        }
        @SuppressWarnings("unchecked")
        S prefix = (S) source.trySplit();
        if(prefix == null) {
            return null;
        }
        if(accRef == null) {
            accRef = new AtomicReference<>(none());
        }
        PrefixOps<T, S> pref = doClone();
        pref.source = prefix;
        return pref;
    }

    @Override
    public long estimateSize() {
        return source.estimateSize();
    }

    @Override
    public int characteristics() {
        return source.characteristics() & (ORDERED | IMMUTABLE | CONCURRENT | SIZED | SUBSIZED);
    }

    static final class OfRef<T> extends AbstractSpliterator<T> implements Consumer<T> {
        private final BinaryOperator<T> op;
        private final Spliterator<T> source;
        private boolean started;
        private T acc;

        OfRef(Spliterator<T> source, BinaryOperator<T> op) {
            super(source.estimateSize(), source.characteristics() & (ORDERED | IMMUTABLE | CONCURRENT | SIZED));
            this.source = source;
            this.op = op;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if(!source.tryAdvance(this))
                return false;
            action.accept(acc);
            return true;
        }
        
        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            source.forEachRemaining(next -> {
                this.accept(next);
                action.accept(acc);
            });
        }

        @Override
        public void accept(T next) {
            if(started) {
                acc = op.apply(acc, next);
            } else {
                started = true;
                acc = next;
            }
        }
    }
    
    static final class OfInt extends AbstractIntSpliterator implements IntConsumer {
        private final IntBinaryOperator op;
        private final Spliterator.OfInt source;
        private boolean started;
        private int acc;
        
        OfInt(Spliterator.OfInt source, IntBinaryOperator op) {
            super(source.estimateSize(), source.characteristics() & (ORDERED | IMMUTABLE | CONCURRENT | SIZED | NONNULL));
            this.source = source;
            this.op = op;
        }
        
        @Override
        public boolean tryAdvance(IntConsumer action) {
            if(!source.tryAdvance(this))
                return false;
            action.accept(acc);
            return true;
        }
        
        @Override
        public void forEachRemaining(IntConsumer action) {
            source.forEachRemaining((int next) -> {
                this.accept(next);
                action.accept(acc);
            });
        }
        
        @Override
        public void accept(int next) {
            if(started) {
                acc = op.applyAsInt(acc, next);
            } else {
                started = true;
                acc = next;
            }
        }
    }

    static final class OfLong extends AbstractLongSpliterator implements LongConsumer {
        private final LongBinaryOperator op;
        private final Spliterator.OfLong source;
        private boolean started;
        private long acc;
        
        OfLong(Spliterator.OfLong source, LongBinaryOperator op) {
            super(source.estimateSize(), source.characteristics() & (ORDERED | IMMUTABLE | CONCURRENT | SIZED | NONNULL));
            this.source = source;
            this.op = op;
        }
        
        @Override
        public boolean tryAdvance(LongConsumer action) {
            if(!source.tryAdvance(this))
                return false;
            action.accept(acc);
            return true;
        }
        
        @Override
        public void forEachRemaining(LongConsumer action) {
            source.forEachRemaining((long next) -> {
                this.accept(next);
                action.accept(acc);
            });
        }
        
        @Override
        public void accept(long next) {
            if(started) {
                acc = op.applyAsLong(acc, next);
            } else {
                started = true;
                acc = next;
            }
        }
    }

    static final class OfDouble extends AbstractDoubleSpliterator implements DoubleConsumer {
        private final DoubleBinaryOperator op;
        private final Spliterator.OfDouble source;
        private boolean started;
        private double acc;
        
        OfDouble(Spliterator.OfDouble source, DoubleBinaryOperator op) {
            super(source.estimateSize(), source.characteristics() & (ORDERED | IMMUTABLE | CONCURRENT | SIZED | NONNULL));
            this.source = source;
            this.op = op;
        }
        
        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if(!source.tryAdvance(this))
                return false;
            action.accept(acc);
            return true;
        }
        
        @Override
        public void forEachRemaining(DoubleConsumer action) {
            source.forEachRemaining((double next) -> {
                this.accept(next);
                action.accept(acc);
            });
        }
        
        @Override
        public void accept(double next) {
            if(started) {
                acc = op.applyAsDouble(acc, next);
            } else {
                started = true;
                acc = next;
            }
        }
    }
    
    static final class OfUnordRef<T> extends PrefixOps<T, Spliterator<T>> implements Consumer<T> {
        private final BinaryOperator<T> localOp;
        
        OfUnordRef(Spliterator<T> source, BinaryOperator<T> op) {
            super(source, (a, b) -> a == NONE ? b : op.apply(a, b));
            this.localOp = op;
        }
        
        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if(!source.tryAdvance(this)) {
                return false;
            }
            action.accept(acc);
            return true;
        }
        
        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            if(accRef == null) {
                source.forEachRemaining(next -> action.accept(acc = op.apply(acc, next)));
            } else {
                @SuppressWarnings("unchecked")
                T[] buf = (T[]) new Object[BUF_SIZE];
                source.forEachRemaining(next -> {
                    if(idx == 0) {
                        buf[idx++] = next;
                    } else {
                        T prev = buf[idx-1];
                        buf[idx++] = localOp.apply(prev, next);
                        if(idx == buf.length) {
                            drain(action, buf);
                            idx = 0;
                        }
                    }
                });
                if(idx > 0)
                    drain(action, buf);
            }
        }

        private void drain(Consumer<? super T> action, T[] buf) {
            T last = buf[idx-1];
            T acc = accRef.getAndAccumulate(last, op);
            if(acc != NONE) {
                for(int i=0; i<idx; i++) {
                    action.accept(localOp.apply(buf[i], acc));
                }
            } else {
                for(int i=0; i<idx; i++) {
                    action.accept(buf[i]);
                }
            }
        }

        @Override
        public void accept(T next) {
            if(accRef == null) {
                acc = op.apply(acc, next);
            } else {
                acc = accRef.accumulateAndGet(next, op);
            }
        }
    }
}
