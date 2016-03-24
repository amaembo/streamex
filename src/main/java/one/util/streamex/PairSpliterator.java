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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongUnaryOperator;

import one.util.streamex.StreamExInternals.TailSpliterator;
import static one.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
/* package */abstract class PairSpliterator<T, S extends Spliterator<T>, R, SS extends PairSpliterator<T, S, R, SS>>
        extends CloneableSpliterator<R, SS> {
    static final int MODE_PAIRS = 0;
    static final int MODE_MAP_FIRST = 1;
    static final int MODE_MAP_LAST = 2;
    static final int MODE_MAP_FIRST_OR_ELSE = 3;
    static final int MODE_MAP_LAST_OR_ELSE = 4;
    
    static Sink<?> EMPTY = new Sink<>(null);
    // Common lock for all the derived spliterators
    final Object lock = new Object();
    final int mode;
    S source;
    @SuppressWarnings("unchecked")
    Sink<T> left = (Sink<T>) EMPTY;
    @SuppressWarnings("unchecked")
    Sink<T> right = (Sink<T>) EMPTY;

    static final class Sink<T> {
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

    PairSpliterator(S source, int mode, T headTail) {
        this.source = source;
        this.mode = mode;
        if(mode != MODE_PAIRS) {
            Sink<T> sink = new Sink<>(this.lock);
            Sink<T> other = new Sink<>(this.lock);
            sink.other = other;
            other.other = sink;
            other.push(headTail, null, true);
            if(mode == MODE_MAP_FIRST || mode == MODE_MAP_FIRST_OR_ELSE)
                this.left = sink;
            else
                this.right = sink;
        }
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
        SS clone = doClone();
        Sink<T> left = new Sink<>(lock);
        Sink<T> right = new Sink<>(lock);
        clone.source = prefixSource;
        clone.right = right.other = left;
        this.left = left.other = right;
        return clone;
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

    static class PSOfRef<T, R> extends PairSpliterator<T, Spliterator<T>, R, PSOfRef<T, R>> implements
            Consumer<T>, TailSpliterator<R> {
        private static final Object HEAD_TAIL = new Object();

        private final BiFunction<? super T, ? super T, ? extends R> mapper;
        private T cur;

        PSOfRef(BiFunction<? super T, ? super T, ? extends R> mapper, Spliterator<T> source) {
            super(source, MODE_PAIRS, null);
            this.mapper = mapper;
        }

        // Must be called only if T == R
        @SuppressWarnings("unchecked")
        PSOfRef(Function<? super T, ? extends R> mapper, Spliterator<T> source, boolean first) {
            super(source, first ? MODE_MAP_FIRST : MODE_MAP_LAST, (T)HEAD_TAIL);
            BiFunction<? super T, ? super T, ?> m = first ? 
                    ((a, b) -> a == HEAD_TAIL ? mapper.apply(b) : (T)b) :
                    ((a, b) -> b == HEAD_TAIL ? mapper.apply(a) : (T)a);
            this.mapper = (BiFunction<? super T, ? super T, ? extends R>) m;
        }

        @SuppressWarnings("unchecked")
        PSOfRef(Function<? super T, ? extends R> boundMapper, Function<? super T, ? extends R> elseMapper, Spliterator<T> source, boolean first) {
            super(source, first ? MODE_MAP_FIRST_OR_ELSE : MODE_MAP_LAST_OR_ELSE, (T)HEAD_TAIL);
            this.mapper = first ? 
                ((a, b) -> a == HEAD_TAIL ? boundMapper.apply(b) : elseMapper.apply(b)) :
                ((a, b) -> b == HEAD_TAIL ? boundMapper.apply(a) : elseMapper.apply(a));
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

        @Override
        public Spliterator<R> tryAdvanceOrTail(Consumer<? super R> action) {
            if (mode != MODE_MAP_FIRST || right != EMPTY) {
                return tryAdvance(action) ? this : null;
            }
            if (left != null) {
                Sink<T> l = left;
                left = null;
                source = TailSpliterator.tryAdvanceWithTail(source, this);
                if (source == null) {
                    right = null;
                    return null;
                }
                if (l.push(cur, fn(action), false))
                    return this;
            }
            @SuppressWarnings("unchecked")
            Spliterator<R> s = (Spliterator<R>)source;
            source = null;
            return s;
        }
        
        @Override
        public Spliterator<R> forEachOrTail(Consumer<? super R> action) {
            if(mode != MODE_MAP_FIRST || right != EMPTY) {
                forEachRemaining(action);
                return null;
            }
            while (true) {
                Spliterator<R> tail = tryAdvanceOrTail(action);
                if (tail != this)
                    return tail;
            }
        }
    }

    static final class PSOfInt extends PairSpliterator<Integer, Spliterator.OfInt, Integer, PSOfInt> implements
            Spliterator.OfInt, IntConsumer {
        private final IntBinaryOperator mapper;
        private final IntUnaryOperator unaryMapper;
        private int cur;

        PSOfInt(IntBinaryOperator mapper, IntUnaryOperator unaryMapper, Spliterator.OfInt source, int mode) {
            super(source, mode, null);
            this.mapper = mapper;
            this.unaryMapper = unaryMapper;
        }
        
        @Override
        public void accept(int t) {
            cur = t;
        }

        private BiConsumer<Integer, Integer> fn(IntConsumer action) {
            switch(mode) {
            case MODE_MAP_FIRST:
                return (a, b) -> action.accept(a == null ? unaryMapper.applyAsInt(b) : b);
            case MODE_MAP_LAST:
                return (a, b) -> action.accept(b == null ? unaryMapper.applyAsInt(a) : a);
            default:
                return (a, b) -> action.accept(mapper.applyAsInt(a, b));
            }
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
        private final LongUnaryOperator unaryMapper;
        private long cur;

        PSOfLong(LongBinaryOperator mapper, LongUnaryOperator unaryMapper, Spliterator.OfLong source, int mode) {
            super(source, mode, null);
            this.mapper = mapper;
            this.unaryMapper = unaryMapper;
        }

        @Override
        public void accept(long t) {
            cur = t;
        }

        private BiConsumer<Long, Long> fn(LongConsumer action) {
            switch(mode) {
            case MODE_MAP_FIRST:
                return (a, b) -> action.accept(a == null ? unaryMapper.applyAsLong(b) : b);
            case MODE_MAP_LAST:
                return (a, b) -> action.accept(b == null ? unaryMapper.applyAsLong(a) : a);
            default:
                return (a, b) -> action.accept(mapper.applyAsLong(a, b));
            }
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
        private final DoubleUnaryOperator unaryMapper;
        private double cur;

        PSOfDouble(DoubleBinaryOperator mapper, DoubleUnaryOperator unaryMapper, Spliterator.OfDouble source, int mode) {
            super(source, mode, null);
            this.mapper = mapper;
            this.unaryMapper = unaryMapper;
        }

        @Override
        public void accept(double t) {
            cur = t;
        }

        private BiConsumer<Double, Double> fn(DoubleConsumer action) {
            switch(mode) {
            case MODE_MAP_FIRST:
                return (a, b) -> action.accept(a == null ? unaryMapper.applyAsDouble(b) : b);
            case MODE_MAP_LAST:
                return (a, b) -> action.accept(b == null ? unaryMapper.applyAsDouble(a) : a);
            default:
                return (a, b) -> action.accept(mapper.applyAsDouble(a, b));
            }
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
