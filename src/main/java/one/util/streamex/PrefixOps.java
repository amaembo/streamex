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
import java.util.Spliterators.AbstractDoubleSpliterator;
import java.util.Spliterators.AbstractIntSpliterator;
import java.util.Spliterators.AbstractLongSpliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;

import static one.util.streamex.Internals.CloneableSpliterator;
import static one.util.streamex.Internals.NONE;
import static one.util.streamex.Internals.none;

/**
 * @author Tagir Valeev
 */
/* package */ abstract class PrefixOps<T, S extends Spliterator<T>> extends CloneableSpliterator<T, PrefixOps<T, S>> {
    private static final int BUF_SIZE = 128;
    
    protected S source;
    protected int idx = 0;
    
    PrefixOps(S source) {
        this.source = source;
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
            if (!source.tryAdvance(this))
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
            if (started) {
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
            if (!source.tryAdvance(this))
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
            if (started) {
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
            if (!source.tryAdvance(this))
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
            if (started) {
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
            if (!source.tryAdvance(this))
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
            if (started) {
                acc = op.applyAsDouble(acc, next);
            } else {
                started = true;
                acc = next;
            }
        }
    }
    
    static final class OfUnordRef<T> extends PrefixOps<T, Spliterator<T>> implements Consumer<T> {
        private final BinaryOperator<T> localOp;
        private AtomicReference<T> accRef;
        private T acc = none();
        private final BinaryOperator<T> op;
        
        OfUnordRef(Spliterator<T> source, BinaryOperator<T> op) {
            super(source);
            this.localOp = op;
            this.op = (a, b) -> a == NONE ? b : op.apply(a, b);
        }
        
        @Override
        public Spliterator<T> trySplit() {
            if (acc != NONE) {
                return null;
            }
            Spliterator<T> prefix = source.trySplit();
            if (prefix == null) {
                return null;
            }
            if (accRef == null) {
                accRef = new AtomicReference<>(none());
            }
            OfUnordRef<T> pref = (OfUnordRef<T>) doClone();
            pref.source = prefix;
            return pref;
        }
        
        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (!source.tryAdvance(this)) {
                return false;
            }
            action.accept(acc);
            return true;
        }
        
        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            if (accRef == null) {
                source.forEachRemaining(next -> action.accept(acc = op.apply(acc, next)));
            } else {
                @SuppressWarnings("unchecked")
                T[] buf = (T[]) new Object[BUF_SIZE];
                source.forEachRemaining(next -> {
                    if (idx == 0) {
                        buf[idx++] = next;
                    } else {
                        T prev = buf[idx - 1];
                        buf[idx++] = localOp.apply(prev, next);
                        if (idx == buf.length) {
                            drain(action, buf);
                            idx = 0;
                        }
                    }
                });
                if (idx > 0)
                    drain(action, buf);
            }
        }

        private void drain(Consumer<? super T> action, T[] buf) {
            T last = buf[idx - 1];
            T acc = accRef.getAndAccumulate(last, op);
            if (acc != NONE) {
                for (int i = 0; i < idx; i++) {
                    action.accept(localOp.apply(buf[i], acc));
                }
            } else {
                for (int i = 0; i < idx; i++) {
                    action.accept(buf[i]);
                }
            }
        }

        @Override
        public void accept(T next) {
            if (accRef == null) {
                acc = op.apply(acc, next);
            } else {
                acc = accRef.accumulateAndGet(next, op);
            }
        }
    }
    
    static final class OfUnordInt extends PrefixOps<Integer, Spliterator.OfInt> implements IntConsumer, Spliterator.OfInt {
        private final IntBinaryOperator op;
        private boolean started;
        private MyAtomicInteger accRef;
        private int acc;
        
        OfUnordInt(Spliterator.OfInt source, IntBinaryOperator op) {
            super(source);
            this.op = op;
        }
        
        private static final class MyAtomicInteger extends AtomicInteger {
            private boolean init;
    
            /**
             * On the very first call sets the value to {@code x}
             * @param x the initial value
             * @return {@code true} if it was the very first call
             */
            public synchronized boolean initialize(int x) {
                if (!init) {
                    init = true;
                    set(x);
                    return true;
                }
                return false;
            }
        }
        
        @Override
        public Spliterator.OfInt trySplit() {
            if (started) {
                return null;
            }
            Spliterator.OfInt prefix = source.trySplit();
            if (prefix == null) {
                return null;
            }
            if (accRef == null) {
                accRef = new MyAtomicInteger();
            }
            OfUnordInt pref = (OfUnordInt) doClone();
            pref.source = prefix;
            return pref;
        }
        
        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (!source.tryAdvance(this)) {
                return false;
            }
            action.accept(acc);
            return true;
        }
        
        @Override
        public void forEachRemaining(IntConsumer action) {
            if (accRef == null) {
                source.forEachRemaining((IntConsumer) next -> {
                    if (started) {
                        acc = op.applyAsInt(acc, next);
                    } else {
                        acc = next;
                        started = true;
                    }
                    action.accept(acc);
                });
            } else {
                int[] buf = new int[BUF_SIZE];
                source.forEachRemaining((IntConsumer) next -> {
                    if (idx == 0) {
                        buf[idx++] = next;
                    } else {
                        int prev = buf[idx - 1];
                        buf[idx++] = op.applyAsInt(prev, next);
                        if (idx == buf.length) {
                            drain(action, buf);
                            idx = 0;
                        }
                    }
                });
                if (idx > 0)
                    drain(action, buf);
            }
        }
        
        private void drain(IntConsumer action, int[] buf) {
            int last = buf[idx - 1];
            boolean accRefJustInitialized = accRef.initialize(last);
            if (accRefJustInitialized) {
                for (int i = 0; i < idx; i++) {
                    action.accept(buf[i]);
                }
            } else {
                int acc = accRef.getAndAccumulate(last, op);
                for (int i = 0; i < idx; i++) {
                    action.accept(op.applyAsInt(buf[i], acc));
                }
            }
        }
        
        @Override
        public void accept(int next) {
            if (accRef == null) {
                if (started) {
                    acc = op.applyAsInt(acc, next);
                } else {
                    started = true;
                    acc = next;
                }
            } else {
                boolean accRefJustInitialized = accRef.initialize(next);
                if (!accRefJustInitialized) {
                    acc = accRef.accumulateAndGet(next, op);
                } else {
                    acc = next;
                }
            }
        }
    }
    
    static final class OfUnordLong extends PrefixOps<Long, Spliterator.OfLong> implements LongConsumer, Spliterator.OfLong {
        private final LongBinaryOperator op;
        private boolean started;
        private MyAtomicLong accRef;
        private long acc;
        
        OfUnordLong(Spliterator.OfLong source, LongBinaryOperator op) {
            super(source);
            this.op = op;
        }
        
        private static final class MyAtomicLong extends AtomicLong {
            private boolean init;
    
            /**
             * On the very first call sets the value to {@code x}
             * @param x the initial value
             * @return {@code true} if it was the very first call
             */
            public synchronized boolean initialize(long x) {
                if (!init) {
                    init = true;
                    set(x);
                    return true;
                }
                return false;
            }
        }
        
        @Override
        public Spliterator.OfLong trySplit() {
            if (started) {
                return null;
            }
            Spliterator.OfLong prefix = source.trySplit();
            if (prefix == null) {
                return null;
            }
            if (accRef == null) {
                accRef = new MyAtomicLong();
            }
            OfUnordLong pref = (OfUnordLong) doClone();
            pref.source = prefix;
            return pref;
        }
        
        @Override
        public boolean tryAdvance(LongConsumer action) {
            if (!source.tryAdvance(this)) {
                return false;
            }
            action.accept(acc);
            return true;
        }
        
        @Override
        public void forEachRemaining(LongConsumer action) {
            if (accRef == null) {
                source.forEachRemaining((LongConsumer) next -> {
                    if (started) {
                        acc = op.applyAsLong(acc, next);
                    } else {
                        acc = next;
                        started = true;
                    }
                    action.accept(acc);
                });
            } else {
                long[] buf = new long[BUF_SIZE];
                source.forEachRemaining((LongConsumer) next -> {
                    if (idx == 0) {
                        buf[idx++] = next;
                    } else {
                        long prev = buf[idx - 1];
                        buf[idx++] = op.applyAsLong(prev, next);
                        if (idx == buf.length) {
                            drain(action, buf);
                            idx = 0;
                        }
                    }
                });
                if (idx > 0)
                    drain(action, buf);
            }
        }
        
        private void drain(LongConsumer action, long[] buf) {
            long last = buf[idx - 1];
            boolean accRefJustInitialized = accRef.initialize(last);
            if (accRefJustInitialized) {
                for (int i = 0; i < idx; i++) {
                    action.accept(buf[i]);
                }
            } else {
                long acc = accRef.getAndAccumulate(last, op);
                for (int i = 0; i < idx; i++) {
                    action.accept(op.applyAsLong(buf[i], acc));
                }
            }
        }
        
        @Override
        public void accept(long next) {
            if (accRef == null) {
                if (started) {
                    acc = op.applyAsLong(acc, next);
                } else {
                    started = true;
                    acc = next;
                }
            } else {
                boolean accRefJustInitialized = accRef.initialize(next);
                if (!accRefJustInitialized) {
                    acc = accRef.accumulateAndGet(next, op);
                } else {
                    acc = next;
                }
            }
        }
    }
}
