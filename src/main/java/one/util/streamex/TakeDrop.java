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

import java.util.Comparator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Spliterators.AbstractDoubleSpliterator;
import java.util.Spliterators.AbstractIntSpliterator;
import java.util.Spliterators.AbstractLongSpliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

import one.util.streamex.Internals.CloneableSpliterator;

/**
 * @author Tagir Valeev
 */
interface TakeDrop {
    final class TDOfRef<T> extends AbstractSpliterator<T> implements Consumer<T> {
        private final Predicate<? super T> predicate;
        private final boolean drop;
        private final boolean inclusive;
        private boolean checked;
        private final Spliterator<T> source;
        private T cur;
    
        TDOfRef(Spliterator<T> source, boolean drop, boolean inclusive, Predicate<? super T> predicate) {
            super(source.estimateSize(), source.characteristics()
                & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.inclusive = inclusive;
            this.source = source;
        }
    
        @Override
        public Comparator<? super T> getComparator() {
            return source.getComparator();
        }
    
        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (drop) {
                if (checked)
                    return source.tryAdvance(action);
                while (source.tryAdvance(this)) {
                    if (!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if (!checked && source.tryAdvance(this) && (predicate.test(cur) || (checked = inclusive))) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }
    
        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            if (drop) {
                if (checked)
                    source.forEachRemaining(action);
                else {
                    source.forEachRemaining(e -> {
                        if (checked)
                            action.accept(e);
                        else {
                            if (!predicate.test(e)) {
                                checked = true;
                                action.accept(e);
                            }
                        }
                    });
                }
            } else
                super.forEachRemaining(action);
        }
    
        @Override
        public void accept(T t) {
            this.cur = t;
        }
    }
    
    final class UnorderedTDOfRef<T> extends CloneableSpliterator<T, UnorderedTDOfRef<T>> implements Consumer<T> {
        private final Predicate<? super T> predicate;
        private final boolean drop;
        private final boolean inclusive;
        private final AtomicBoolean checked = new AtomicBoolean();
        private Spliterator<T> source;
        private T cur;

        UnorderedTDOfRef(Spliterator<T> source, boolean drop, boolean inclusive, Predicate<? super T> predicate) {
            this.drop = drop;
            this.predicate = predicate;
            this.inclusive = inclusive;
            this.source = source;
        }

        @Override
        public void accept(T t) {
            this.cur = t;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (drop) {
                if (checked.get())
                    return source.tryAdvance(action);
                while (source.tryAdvance(this)) {
                    if (!predicate.test(cur)) {
                        checked.set(true);
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if (!checked.get() && source.tryAdvance(this) && 
                    (predicate.test(cur) || (checked.compareAndSet(false, true) && inclusive))) {
                action.accept(cur);
                return true;
            }
            return false;
        }

        @Override
        public Spliterator<T> trySplit() {
            Spliterator<T> prefix = source.trySplit();
            if (prefix == null) {
                return null;
            }
            if (checked.get()) {
                return drop ? prefix : Spliterators.emptySpliterator();
            }
            UnorderedTDOfRef<T> clone = doClone();
            clone.source = prefix;
            return clone;
        }

        @Override
        public long estimateSize() {
            return source.estimateSize();
        }

        @Override
        public int characteristics() {
            return source.characteristics() & (DISTINCT | NONNULL);
        }
    }
    
    final class TDOfInt extends AbstractIntSpliterator implements IntConsumer {
        private final IntPredicate predicate;
        private final boolean drop;
        private final boolean inclusive;
        private boolean checked;
        private final Spliterator.OfInt source;
        private int cur;
    
        TDOfInt(Spliterator.OfInt source, boolean drop, boolean inclusive, IntPredicate predicate) {
            super(source.estimateSize(), source.characteristics() & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL
                | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.inclusive = inclusive;
            this.source = source;
        }
    
        @Override
        public Comparator<? super Integer> getComparator() {
            return source.getComparator();
        }
    
        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (drop) {
                if (checked)
                    return source.tryAdvance(action);
                while (source.tryAdvance(this)) {
                    if (!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if (!checked && source.tryAdvance(this) && (predicate.test(cur) || (checked = inclusive))) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }
    
        @Override
        public void forEachRemaining(IntConsumer action) {
            if (drop) {
                if (checked)
                    source.forEachRemaining(action);
                else {
                    source.forEachRemaining((int e) -> {
                        if (checked)
                            action.accept(e);
                        else {
                            if (!predicate.test(e)) {
                                checked = true;
                                action.accept(e);
                            }
                        }
                    });
                }
            } else
                super.forEachRemaining(action);
        }
    
        @Override
        public void accept(int t) {
            this.cur = t;
        }
    }

    final class TDOfLong extends AbstractLongSpliterator implements LongConsumer {
        private final LongPredicate predicate;
        private final boolean drop;
        private final boolean inclusive;
        private boolean checked;
        private final Spliterator.OfLong source;
        private long cur;
    
        TDOfLong(Spliterator.OfLong source, boolean drop, boolean inclusive, LongPredicate predicate) {
            super(source.estimateSize(), source.characteristics() & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL
                | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.inclusive = inclusive;
            this.source = source;
        }
    
        @Override
        public Comparator<? super Long> getComparator() {
            return source.getComparator();
        }
    
        @Override
        public boolean tryAdvance(LongConsumer action) {
            if (drop) {
                if (checked)
                    return source.tryAdvance(action);
                while (source.tryAdvance(this)) {
                    if (!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if (!checked && source.tryAdvance(this) && (predicate.test(cur) || (checked = inclusive))) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }
    
        @Override
        public void forEachRemaining(LongConsumer action) {
            if (drop) {
                if (checked)
                    source.forEachRemaining(action);
                else {
                    source.forEachRemaining((long e) -> {
                        if (checked)
                            action.accept(e);
                        else {
                            if (!predicate.test(e)) {
                                checked = true;
                                action.accept(e);
                            }
                        }
                    });
                }
            } else
                super.forEachRemaining(action);
        }
    
        @Override
        public void accept(long t) {
            this.cur = t;
        }
    }

    final class TDOfDouble extends AbstractDoubleSpliterator implements DoubleConsumer {
        private final DoublePredicate predicate;
        private final boolean drop;
        private final boolean inclusive;
        private boolean checked;
        private final Spliterator.OfDouble source;
        private double cur;
    
        TDOfDouble(Spliterator.OfDouble source, boolean drop, boolean inclusive, DoublePredicate predicate) {
            super(source.estimateSize(), source.characteristics() & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL
                | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.inclusive = inclusive;
            this.source = source;
        }
    
        @Override
        public Comparator<? super Double> getComparator() {
            return source.getComparator();
        }
    
        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if (drop) {
                if (checked)
                    return source.tryAdvance(action);
                while (source.tryAdvance(this)) {
                    if (!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if (!checked && source.tryAdvance(this) && (predicate.test(cur) || (checked = inclusive))) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }
    
        @Override
        public void forEachRemaining(DoubleConsumer action) {
            if (drop) {
                if (checked)
                    source.forEachRemaining(action);
                else {
                    source.forEachRemaining((double e) -> {
                        if (checked)
                            action.accept(e);
                        else {
                            if (!predicate.test(e)) {
                                checked = true;
                                action.accept(e);
                            }
                        }
                    });
                }
            } else
                super.forEachRemaining(action);
        }
    
        @Override
        public void accept(double t) {
            this.cur = t;
        }
    }
}
