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

import java.util.Comparator;
import java.util.Spliterator;
import java.util.Spliterators.AbstractDoubleSpliterator;
import java.util.Spliterators.AbstractIntSpliterator;
import java.util.Spliterators.AbstractLongSpliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 * @author Tagir Valeev
 */
/* package */ final class TakeDropSpliterators {
    static final class TDOfRef<T> extends AbstractSpliterator<T> implements Consumer<T> {
        private final Predicate<? super T> predicate;
        private final boolean drop;
        private boolean checked;
        private final Spliterator<T> source;
        private T cur;

        TDOfRef(Spliterator<T> source, boolean drop, Predicate<? super T> predicate) {
            super(source.estimateSize(), source.characteristics() & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.source = source;
        }

        @Override
        public Comparator<? super T> getComparator() {
            return source.getComparator();
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if(checked) {
                if(!drop) return false;
                return source.tryAdvance(action);
            }
            if(drop) {
                while(source.tryAdvance(this)) {
                    if(!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if(!source.tryAdvance(this))
                return false;
            if(predicate.test(cur)) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }

        @Override
        public void accept(T t) {
            this.cur = t;
        }
    }

    static final class TDOfInt extends AbstractIntSpliterator implements IntConsumer {
        private final IntPredicate predicate;
        private final boolean drop;
        private boolean checked;
        private final Spliterator.OfInt source;
        private int cur;
        
        TDOfInt(Spliterator.OfInt source, boolean drop, IntPredicate predicate) {
            super(source.estimateSize(), source.characteristics() & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.source = source;
        }
        
        @Override
        public Comparator<? super Integer> getComparator() {
            return source.getComparator();
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if(checked) {
                if(!drop) return false;
                return source.tryAdvance(action);
            }
            if(drop) {
                while(source.tryAdvance(this)) {
                    if(!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if(!source.tryAdvance(this))
                return false;
            if(predicate.test(cur)) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }
        
        @Override
        public void accept(int t) {
            this.cur = t;
        }
    }

    static final class TDOfLong extends AbstractLongSpliterator implements LongConsumer {
        private final LongPredicate predicate;
        private final boolean drop;
        private boolean checked;
        private final Spliterator.OfLong source;
        private long cur;
        
        TDOfLong(Spliterator.OfLong source, boolean drop, LongPredicate predicate) {
            super(source.estimateSize(), source.characteristics() & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.source = source;
        }
        
        @Override
        public Comparator<? super Long> getComparator() {
            return source.getComparator();
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            if(checked) {
                if(!drop) return false;
                return source.tryAdvance(action);
            }
            if(drop) {
                while(source.tryAdvance(this)) {
                    if(!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if(!source.tryAdvance(this))
                return false;
            if(predicate.test(cur)) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }
        
        @Override
        public void accept(long t) {
            this.cur = t;
        }
    }

    static final class TDOfDouble extends AbstractDoubleSpliterator implements DoubleConsumer {
        private final DoublePredicate predicate;
        private final boolean drop;
        private boolean checked;
        private final Spliterator.OfDouble source;
        private double cur;
        
        TDOfDouble(Spliterator.OfDouble source, boolean drop, DoublePredicate predicate) {
            super(source.estimateSize(), source.characteristics() & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.source = source;
        }
        
        @Override
        public Comparator<? super Double> getComparator() {
            return source.getComparator();
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if(checked) {
                if(!drop) return false;
                return source.tryAdvance(action);
            }
            if(drop) {
                while(source.tryAdvance(this)) {
                    if(!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if(!source.tryAdvance(this))
                return false;
            if(predicate.test(cur)) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }
        
        @Override
        public void accept(double t) {
            this.cur = t;
        }
    }
}
