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
package javax.util.streamex;

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

import static javax.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
/* package */ abstract class PairSpliterator<T, S extends Spliterator<T>, R, SS extends PairSpliterator<T, S, R, SS>> implements
        Spliterator<R>, Cloneable {
    private static Sink<?> EMPTY = new Sink<>(null, true);
    // Common lock for all the derived spliterators
    private final Object lock = new Object();
    S source;
    @SuppressWarnings("unchecked")
    Sink<T> left = (Sink<T>) EMPTY;
    @SuppressWarnings("unchecked")
    Sink<T> right = (Sink<T>) EMPTY;

    private static final class Sink<T> {
        final boolean isLeft;
        Sink<T> other;
        private T payload = none();
        private final Object lock;

        Sink(Object lock, boolean isLeft) {
            this.lock = lock;
            this.isLeft = isLeft;
        }

        boolean push(T payload, BiConsumer<T, T> fn) {
            if(lock == null)
                return false;
            synchronized(lock) {
                Sink<T> that = other;
                if (that == null)
                    return false;
                this.payload = payload;
                return isLeft ? operate(this, that, fn) : operate(that, this, fn);
            }
        }

        static <T> boolean operate(Sink<T> left, Sink<T> right, BiConsumer<T, T> fn) {
            if (left.payload == NONE || right.payload == NONE)
                return false;
            fn.accept(left.payload, right.payload);
            left.clear();
            right.clear();
            return true;
        }

        boolean connect(Sink<T> right, BiConsumer<T, T> fn) {
            if(lock == null)
                return false;
            synchronized(lock) {
                Sink<T> leftLeft = this.other; 
                Sink<T> rightRight = right.other;
                if(leftLeft == null) {
                    if(rightRight != null) {
                        rightRight.clear();
                    }
                    return false;
                }
                if(rightRight == null) {
                    leftLeft.clear();
                    return false;
                }
                rightRight.other = leftLeft;
                leftLeft.other = rightRight;
                return operate(leftLeft, rightRight, fn);
            }
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
            Sink<T> left = new Sink<>(lock, true);
            Sink<T> right = new Sink<>(lock, false);
            clone.source = prefixSource;
            clone.right = right.other = left;
            this.left = left.other = right;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    static final class PSOfRef<T, R> extends PairSpliterator<T, Spliterator<T>, R, PSOfRef<T, R>> {
        private final BiFunction<? super T, ? super T, ? extends R> mapper;
        private T cur;

        public PSOfRef(BiFunction<? super T, ? super T, ? extends R> mapper, Spliterator<T> source) {
            super(source);
            this.mapper = mapper;
        }

        void setCur(T t) {
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
                if (!source.tryAdvance(this::setCur)) {
                    right = null;
                    return l.connect(r, fn(action));
                }
                if (l.push(cur, fn(action)))
                    return true;
            }
            T prev = cur;
            if (!source.tryAdvance(this::setCur)) {
                right = null;
                return r != null && r.push(prev, fn(action));
            }
            action.accept(mapper.apply(prev, cur));
            return true;
        }

        @Override
        public void forEachRemaining(Consumer<? super R> action) {
            Sink<T> l = left, r = right;
            left = right = null;
            if (l != null) {
                if (!source.tryAdvance(this::setCur)) {
                    l.connect(r, fn(action));
                    return;
                }
                l.push(cur, fn(action));
            }
            source.forEachRemaining(next -> action.accept(mapper.apply(cur, cur = next)));
            if (r != null) {
                r.push(cur, fn(action));
            }
        }
    }

    static final class PSOfInt extends PairSpliterator<Integer, Spliterator.OfInt, Integer, PSOfInt> implements
            Spliterator.OfInt {
        private final IntBinaryOperator mapper;
        private int cur;

        public PSOfInt(IntBinaryOperator mapper, Spliterator.OfInt source) {
            super(source);
            this.mapper = mapper;
        }

        void setCur(int t) {
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
                if (!source.tryAdvance((IntConsumer) this::setCur)) {
                    right = null;
                    return l.connect(r, fn(action));
                }
                if (l.push(cur, fn(action)))
                    return true;
            }
            int prev = cur;
            if (!source.tryAdvance((IntConsumer) this::setCur)) {
                right = null;
                return r != null && r.push(prev, fn(action));
            }
            action.accept(mapper.applyAsInt(prev, cur));
            return true;
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            Sink<Integer> l = left, r = right;
            left = right = null;
            if (l != null) {
                if (!source.tryAdvance((IntConsumer) this::setCur)) {
                    l.connect(r, fn(action));
                    return;
                }
                l.push(cur, fn(action));
            }
            source.forEachRemaining((IntConsumer) next -> action.accept(mapper.applyAsInt(cur, cur = next)));
            if (r != null) {
                r.push(cur, fn(action));
            }
        }
    }

    static final class PSOfLong extends PairSpliterator<Long, Spliterator.OfLong, Long, PSOfLong> implements
            Spliterator.OfLong {
        private final LongBinaryOperator mapper;
        private long cur;

        public PSOfLong(LongBinaryOperator mapper, Spliterator.OfLong source) {
            super(source);
            this.mapper = mapper;
        }

        void setCur(long t) {
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
                if (!source.tryAdvance((LongConsumer) this::setCur)) {
                    right = null;
                    return l.connect(r, fn(action));
                }
                if (l.push(cur, fn(action)))
                    return true;
            }
            long prev = cur;
            if (!source.tryAdvance((LongConsumer) this::setCur)) {
                right = null;
                return r != null && r.push(prev, fn(action));
            }
            action.accept(mapper.applyAsLong(prev, cur));
            return true;
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            Sink<Long> l = left, r = right;
            left = right = null;
            if (l != null) {
                if (!source.tryAdvance((LongConsumer) this::setCur)) {
                    l.connect(r, fn(action));
                    return;
                }
                l.push(cur, fn(action));
            }
            source.forEachRemaining((LongConsumer) next -> action.accept(mapper.applyAsLong(cur, cur = next)));
            if (r != null) {
                r.push(cur, fn(action));
            }
        }
    }

    static final class PSOfDouble extends PairSpliterator<Double, Spliterator.OfDouble, Double, PSOfDouble> implements
            Spliterator.OfDouble {
        private final DoubleBinaryOperator mapper;
        private double cur;

        public PSOfDouble(DoubleBinaryOperator mapper, Spliterator.OfDouble source) {
            super(source);
            this.mapper = mapper;
        }

        void setCur(double t) {
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
                if (!source.tryAdvance((DoubleConsumer) this::setCur)) {
                    right = null;
                    return l.connect(r, fn(action));
                }
                if (l.push(cur, fn(action)))
                    return true;
            }
            double prev = cur;
            if (!source.tryAdvance((DoubleConsumer) this::setCur)) {
                right = null;
                return r != null && r.push(prev, fn(action));
            }
            action.accept(mapper.applyAsDouble(prev, cur));
            return true;
        }

        @Override
        public void forEachRemaining(DoubleConsumer action) {
            Sink<Double> l = left, r = right;
            left = right = null;
            if (l != null) {
                if (!source.tryAdvance((DoubleConsumer) this::setCur)) {
                    l.connect(r, fn(action));
                    return;
                }
                l.push(cur, fn(action));
            }
            source.forEachRemaining((DoubleConsumer) next -> action.accept(mapper.applyAsDouble(cur, cur = next)));
            if (r != null) {
                r.push(cur, fn(action));
            }
        }
    }
}
