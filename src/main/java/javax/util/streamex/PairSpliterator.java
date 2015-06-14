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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;

abstract class PairSpliterator<T, S extends Spliterator<T>, R> implements Spliterator<R> {
    S source;
    boolean hasLast, hasPrev;

    public PairSpliterator(S source, boolean hasPrev, boolean hasLast) {
        this.source = source;
        this.hasLast = hasLast;
        this.hasPrev = hasPrev;
    }

    @Override
    public long estimateSize() {
        long size = source.estimateSize();
        if (size == Long.MAX_VALUE)
            return size;
        if (hasLast)
            size++;
        if (!hasPrev && size > 0)
            size--;
        return size;
    }

    @Override
    public int characteristics() {
        return source.characteristics() & (SIZED | SUBSIZED | CONCURRENT | IMMUTABLE | ORDERED);
    }

    static final class PSOfRef<T, R> extends PairSpliterator<T, Spliterator<T>, R> {
        private T cur;
        private final T last;
        private final BiFunction<? super T, ? super T, ? extends R> mapper;

        public PSOfRef(BiFunction<? super T, ? super T, ? extends R> mapper, Spliterator<T> source, T prev,
                boolean hasPrev, T last, boolean hasLast) {
            super(source, hasPrev, hasLast);
            this.cur = prev;
            this.last = last;
            this.mapper = mapper;
        }

        void setCur(T t) {
            cur = t;
        }

        @Override
        public boolean tryAdvance(Consumer<? super R> action) {
            if (!hasPrev) {
                if (!source.tryAdvance(this::setCur)) {
                    return false;
                }
                hasPrev = true;
            }
            T prev = cur;
            if (!source.tryAdvance(this::setCur)) {
                if (!hasLast)
                    return false;
                hasLast = false;
                cur = last;
            }
            action.accept(mapper.apply(prev, cur));
            return true;
        }

        @Override
        public Spliterator<R> trySplit() {
            Spliterator<T> prefixSource = source.trySplit();
            if (prefixSource == null)
                return null;
            T prev = cur;
            if (!source.tryAdvance(this::setCur)) {
                source = prefixSource;
                return null;
            }
            boolean oldHasPrev = hasPrev;
            hasPrev = true;
            return new PSOfRef<>(mapper, prefixSource, prev, oldHasPrev, cur, true);
        }
    }

    static final class PSOfInt extends PairSpliterator<Integer, Spliterator.OfInt, Integer> implements
            Spliterator.OfInt {
        private int cur;
        private final int last;
        private final IntBinaryOperator mapper;

        PSOfInt(IntBinaryOperator mapper, Spliterator.OfInt source, int prev, boolean hasPrev, int last, boolean hasLast) {
            super(source, hasPrev, hasLast);
            this.cur = prev;
            this.last = last;
            this.mapper = mapper;
        }

        void setCur(int t) {
            cur = t;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (!hasPrev) {
                if (!source.tryAdvance((IntConsumer) this::setCur)) {
                    return false;
                }
                hasPrev = true;
            }
            int prev = cur;
            if (!source.tryAdvance((IntConsumer) this::setCur)) {
                if (!hasLast)
                    return false;
                hasLast = false;
                cur = last;
            }
            action.accept(mapper.applyAsInt(prev, cur));
            return true;
        }

        @Override
        public Spliterator.OfInt trySplit() {
            Spliterator.OfInt prefixSource = source.trySplit();
            if (prefixSource == null)
                return null;
            int prev = cur;
            if (!source.tryAdvance((IntConsumer) this::setCur)) {
                source = prefixSource;
                return null;
            }
            boolean oldHasPrev = hasPrev;
            hasPrev = true;
            return new PSOfInt(mapper, prefixSource, prev, oldHasPrev, cur, true);
        }
    }

    static final class PSOfLong extends PairSpliterator<Long, Spliterator.OfLong, Long> implements Spliterator.OfLong {
        private long cur;
        private final long last;
        private final LongBinaryOperator mapper;

        PSOfLong(LongBinaryOperator mapper, Spliterator.OfLong source, long prev, boolean hasPrev, long last,
                boolean hasLast) {
            super(source, hasPrev, hasLast);
            this.cur = prev;
            this.last = last;
            this.mapper = mapper;
        }

        void setCur(long t) {
            cur = t;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            if (!hasPrev) {
                if (!source.tryAdvance((LongConsumer) this::setCur)) {
                    return false;
                }
                hasPrev = true;
            }
            long prev = cur;
            if (!source.tryAdvance((LongConsumer) this::setCur)) {
                if (!hasLast)
                    return false;
                hasLast = false;
                cur = last;
            }
            action.accept(mapper.applyAsLong(prev, cur));
            return true;
        }

        @Override
        public Spliterator.OfLong trySplit() {
            Spliterator.OfLong prefixSource = source.trySplit();
            if (prefixSource == null)
                return null;
            long prev = cur;
            if (!source.tryAdvance((LongConsumer) this::setCur)) {
                source = prefixSource;
                return null;
            }
            boolean oldHasPrev = hasPrev;
            hasPrev = true;
            return new PSOfLong(mapper, prefixSource, prev, oldHasPrev, cur, true);
        }
    }

    static final class PSOfDouble extends PairSpliterator<Double, Spliterator.OfDouble, Double> implements
            Spliterator.OfDouble {
        private double cur;
        private final double last;
        private final DoubleBinaryOperator mapper;

        PSOfDouble(DoubleBinaryOperator mapper, Spliterator.OfDouble source, double prev, boolean hasPrev, double last,
                boolean hasLast) {
            super(source, hasPrev, hasLast);
            this.cur = prev;
            this.last = last;
            this.mapper = mapper;
        }

        void setCur(double t) {
            cur = t;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if (!hasPrev) {
                if (!source.tryAdvance((DoubleConsumer) this::setCur)) {
                    return false;
                }
                hasPrev = true;
            }
            double prev = cur;
            if (!source.tryAdvance((DoubleConsumer) this::setCur)) {
                if (!hasLast)
                    return false;
                hasLast = false;
                cur = last;
            }
            action.accept(mapper.applyAsDouble(prev, cur));
            return true;
        }

        @Override
        public Spliterator.OfDouble trySplit() {
            Spliterator.OfDouble prefixSource = source.trySplit();
            if (prefixSource == null)
                return null;
            double prev = cur;
            if (!source.tryAdvance((DoubleConsumer) this::setCur)) {
                source = prefixSource;
                return null;
            }
            boolean oldHasPrev = hasPrev;
            hasPrev = true;
            return new PSOfDouble(mapper, prefixSource, prev, oldHasPrev, cur, true);
        }
    }
}
