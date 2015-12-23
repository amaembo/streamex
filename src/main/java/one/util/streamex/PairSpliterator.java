/*
 * Copyright 2015 Tagir Valeev
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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;

import static one.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
/* package */abstract class PairSpliterator<T, S extends Spliterator<T>, R, SS extends PairSpliterator<T, S, R, SS>>
        implements Spliterator<R>, Cloneable {
    private static Sink<?> EMPTY = new Sink<>(null);
    // Common lock for all the derived spliterators
    private final Object lock = new Object();
    S source;
    @SuppressWarnings("unchecked")
    Sink<T> left = (Sink<T>) EMPTY;
    @SuppressWarnings("unchecked")
    Sink<T> right = (Sink<T>) EMPTY;

    private static final class Sink<T> {
        Sink<T> other;
        private T payload = none();
        private final Object lock;

        Sink(Object lock) {
            this.lock = lock;
        }

        boolean push(T payload, BiConsumer<T, T> fn, boolean isLeft) {
            if (lock == null)
                return false;
            T otherPayload;
            synchronized (lock) {
                Sink<T> that = other;
                if (that == null)
                    return false;
                otherPayload = that.payload;
                if (otherPayload == NONE) {
                    this.payload = payload;
                    return false;
                }
                other = null;
                that.clear();
            }
            if (isLeft)
                fn.accept(payload, otherPayload);
            else
                fn.accept(otherPayload, payload);
            return true;
        }

        boolean connect(Sink<T> right, BiConsumer<T, T> fn) {
            if (lock == null)
                return false;
            T a, b;
            synchronized (lock) {
                Sink<T> leftLeft = this.other;
                Sink<T> rightRight = right.other;
                if (leftLeft == null || rightRight == null) {
                    if (rightRight != null)
                        rightRight.clear();
                    if (leftLeft != null)
                        leftLeft.clear();
                    return false;
                }
                rightRight.other = leftLeft;
                leftLeft.other = rightRight;
                if (leftLeft.payload == NONE || rightRight.payload == NONE)
                    return false;
                a = leftLeft.payload;
                b = rightRight.payload;
                leftLeft.clear();
                rightRight.clear();
            }
            fn.accept(a, b);
            return true;
        }

        void clear() {
            other = null;
            payload = none();
        }
    }

    PairSpliterator(S source) {
        this.source = source;
    }

    @Override
    public long estimateSize() {
        long size = source.estimateSize();
        if (size == Long.MAX_VALUE || size == 0)
            return size;
        return size - 1;
    }

    @Override
    public int characteristics() {
        return source.characteristics()
            & ((left == EMPTY && right == EMPTY ? SIZED : 0) | CONCURRENT | IMMUTABLE | ORDERED);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SS trySplit() {
        S prefixSource = (S) source.trySplit();
        if (prefixSource == null)
            return null;
        try {
            SS clone = (SS) clone();
            Sink<T> left = new Sink<>(lock);
            Sink<T> right = new Sink<>(lock);
            clone.source = prefixSource;
            clone.right = right.other = left;
            this.left = left.other = right;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    void finish(BiConsumer<T, T> fn, T cur) {
        Sink<T> r = right, l = left;
        right = left = null;
        if (l != null) {
            l.connect(r, fn);
        } else if (r != null) {
            r.push(cur, fn, true);
        }
    }

    static final class PSOfRef<T, R> extends PairSpliterator<T, Spliterator<T>, R, PSOfRef<T, R>> implements
            Consumer<T> {
        private final BiFunction<? super T, ? super T, ? extends R> mapper;
        private T cur;

        public PSOfRef(BiFunction<? super T, ? super T, ? extends R> mapper, Spliterator<T> source) {
            super(source);
            this.mapper = mapper;
        }

        @Override
        public void accept(T t) {
            cur = t;
        }

        private BiConsumer<T, T> fn(Consumer<? super R> action) {
            return (a, b) -> action.accept(mapper.apply(a, b));
        }

        @Override
        public boolean tryAdvance(Consumer<? super R> action) {
            Sink<T> l = left, r = right;
            if (l != null) {
                left = null;
                if (!source.tryAdvance(this)) {
                    right = null;
                    return l.connect(r, fn(action));
                }
                if (l.push(cur, fn(action), false))
                    return true;
            }
            T prev = cur;
            if (!source.tryAdvance(this)) {
                right = null;
                return r != null && r.push(prev, fn(action), true);
            }
            action.accept(mapper.apply(prev, cur));
            return true;
        }

        @Override
        public void forEachRemaining(Consumer<? super R> action) {
            BiConsumer<T, T> fn = fn(action);
            source.forEachRemaining(next -> {
                if (left != null) {
                    left.push(cur = next, fn, false);
                    left = null;
                } else {
                    action.accept(mapper.apply(cur, cur = next));
                }
            });
            finish(fn, cur);
        }
    }

    static final class PSOfInt extends PairSpliterator<Integer, Spliterator.OfInt, Integer, PSOfInt> implements
            Spliterator.OfInt, IntConsumer {
        private final IntBinaryOperator mapper;
        private int cur;

        public PSOfInt(IntBinaryOperator mapper, Spliterator.OfInt source) {
            super(source);
            this.mapper = mapper;
        }

        @Override
        public void accept(int t) {
            cur = t;
        }

        private BiConsumer<Integer, Integer> fn(IntConsumer action) {
            return (a, b) -> action.accept(mapper.applyAsInt(a, b));
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Sink<Integer> l = left, r = right;
            if (l != null) {
                left = null;
                if (!source.tryAdvance(this)) {
                    right = null;
                    return l.connect(r, fn(action));
                }
                if (l.push(cur, fn(action), false))
                    return true;
            }
            int prev = cur;
            if (!source.tryAdvance(this)) {
                right = null;
                return r != null && r.push(prev, fn(action), true);
            }
            action.accept(mapper.applyAsInt(prev, cur));
            return true;
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            BiConsumer<Integer, Integer> fn = fn(action);
            source.forEachRemaining((int next) -> {
                if (left != null) {
                    left.push(cur = next, fn, false);
                    left = null;
                } else {
                    action.accept(mapper.applyAsInt(cur, cur = next));
                }
            });
            finish(fn, cur);
        }
    }

    static final class PSOfLong extends PairSpliterator<Long, Spliterator.OfLong, Long, PSOfLong> implements
            Spliterator.OfLong, LongConsumer {
        private final LongBinaryOperator mapper;
        private long cur;

        public PSOfLong(LongBinaryOperator mapper, Spliterator.OfLong source) {
            super(source);
            this.mapper = mapper;
        }

        @Override
        public void accept(long t) {
            cur = t;
        }

        private BiConsumer<Long, Long> fn(LongConsumer action) {
            return (a, b) -> action.accept(mapper.applyAsLong(a, b));
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            Sink<Long> l = left, r = right;
            if (l != null) {
                left = null;
                if (!source.tryAdvance(this)) {
                    right = null;
                    return l.connect(r, fn(action));
                }
                if (l.push(cur, fn(action), false))
                    return true;
            }
            long prev = cur;
            if (!source.tryAdvance(this)) {
                right = null;
                return r != null && r.push(prev, fn(action), true);
            }
            action.accept(mapper.applyAsLong(prev, cur));
            return true;
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            BiConsumer<Long, Long> fn = fn(action);
            source.forEachRemaining((long next) -> {
                if (left != null) {
                    left.push(cur = next, fn, false);
                    left = null;
                } else {
                    action.accept(mapper.applyAsLong(cur, cur = next));
                }
            });
            finish(fn, cur);
        }
    }

    static final class PSOfDouble extends PairSpliterator<Double, Spliterator.OfDouble, Double, PSOfDouble> implements
            Spliterator.OfDouble, DoubleConsumer {
        private final DoubleBinaryOperator mapper;
        private double cur;

        public PSOfDouble(DoubleBinaryOperator mapper, Spliterator.OfDouble source) {
            super(source);
            this.mapper = mapper;
        }

        @Override
        public void accept(double t) {
            cur = t;
        }

        private BiConsumer<Double, Double> fn(DoubleConsumer action) {
            return (a, b) -> action.accept(mapper.applyAsDouble(a, b));
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            Sink<Double> l = left, r = right;
            if (l != null) {
                left = null;
                if (!source.tryAdvance(this)) {
                    right = null;
                    return l.connect(r, fn(action));
                }
                if (l.push(cur, fn(action), false))
                    return true;
            }
            double prev = cur;
            if (!source.tryAdvance(this)) {
                right = null;
                return r != null && r.push(prev, fn(action), true);
            }
            action.accept(mapper.applyAsDouble(prev, cur));
            return true;
        }

        @Override
        public void forEachRemaining(DoubleConsumer action) {
            BiConsumer<Double, Double> fn = fn(action);
            source.forEachRemaining((double next) -> {
                if (left != null) {
                    left.push(cur = next, fn, false);
                    left = null;
                } else {
                    action.accept(mapper.applyAsDouble(cur, cur = next));
                }
            });
            finish(fn, cur);
        }
    }
}
