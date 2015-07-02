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
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

import static javax.util.streamex.StreamExInternals.*;

/* package */final class CollapseSpliterator2<T, R> implements Spliterator<R> {
    private static final int NO_OP = 0;
    private static final int LEFT_EMPTY = 1;
    private static final int LEFT_FULL = 2;
    
    private Spliterator<T> source;
    private final CollapseSpliterator2<T, R> root;
    private T cur, last;
    private R lastAcc;
    Sink<T, R> left = new Sink<>(this);
    Sink<T, R> right = new Sink<>(this);
    private final Function<T, R> mapper;
    private final BiFunction<R, T, R> accumulator;
    private final BinaryOperator<R> combiner;
    private final BiPredicate<T, T> mergeable;

    private static final class Sink<T, R> {
        Sink<T, R> other;
        private T left = none();
        private R accumulator = none();
        private T right = none();
        private final CollapseSpliterator2<T, R> base;

        Sink(CollapseSpliterator2<T, R> base) {
            this.base = base;
        }
        
        int pushLeft(R accumulator, T right, Consumer<? super R> action) {
            synchronized(base) {
                Sink<T, R> that = other;
                if(this.left != NONE) {
                    if(base.mergeable.test(right, this.left)) {
                        action.accept(base.combiner.apply(accumulator, this.accumulator));
                        clear();
                        return LEFT_EMPTY;
                    } else {
                        this.left = this.right = none();
                        action.accept(accumulator);
                        return LEFT_FULL;
                    }
                }
                if (that == null) {
                    return NO_OP;
                }
                this.accumulator = accumulator;
                this.right = right;
                if(that.left != NONE) {
                    if(base.mergeable.test(right, that.left)) {
                        action.accept(base.combiner.apply(accumulator, this.accumulator));
                        clear();
                        return LEFT_EMPTY;
                    } else {
                        this.left = this.right = none();
                        action.accept(accumulator);
                        return LEFT_FULL;
                    }
                }
            }
        }

        boolean connect(Sink<T, R> right, Consumer<? super R> action) {
            if(base == null)
                return false;
            synchronized(base) {
                Sink<T, R> leftLeft = this.other; 
                Sink<T, R> rightRight = right.other;
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
                return operate(leftLeft, rightRight, base, action);
            }
        }

        void clear() {
            other = null;
            left = right = none();
            accumulator = none();
        }
    }

    CollapseSpliterator2(BiPredicate<T, T> mergeable, Function<T, R> mapper, BiFunction<R, T, R> accumulator,
            BinaryOperator<R> combiner, Spliterator<T> source) {
        this(mergeable, mapper, accumulator, combiner, source, none(), none(), none(), null);
    }

    CollapseSpliterator2(BiPredicate<T, T> mergeable, Function<T, R> mapper, BiFunction<R, T, R> accumulator,
            BinaryOperator<R> combiner, Spliterator<T> source, T prev, T last, R lastAcc, CollapseSpliterator2<T, R> root) {
        this.source = source;
        this.cur = prev;
        this.last = last;
        this.lastAcc = lastAcc;
        this.mergeable = mergeable;
        this.mapper = mapper;
        this.accumulator = accumulator;
        this.combiner = combiner;
        this.root = root == null ? this : root;
    }

    void setCur(T t) {
        cur = t;
    }

    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        if (source == null)
            return false;
        if (cur == NONE) {
            if (!source.tryAdvance(this::setCur)) {
                source = null;
                if(last != NONE) {
                    action.accept(lastAcc);
                    last = none();
                    lastAcc = none();
                    return true;
                }
                return false;
            }
        }
        R acc = mapper.apply(cur);
        T prev = cur;
        while (source.tryAdvance(this::setCur)) {
            if (mergeable.test(prev, cur)) {
                acc = accumulator.apply(acc, cur);
                prev = cur;
            } else {
                action.accept(acc);
                return true;
            }
        }
        if (last == NONE) {
            source = null;
        } else {
            cur = last;
            last = none();
            if (mergeable.test(prev, cur)) {
                acc = combiner.apply(acc, lastAcc);
                lastAcc = none();
                source = null;
            } else {
                action.accept(acc);
                return true;
            }
        }
        action.accept(lastAcc == NONE ? acc : lastAcc);
        return true;
    }

    @Override
    public void forEachRemaining(Consumer<? super R> action) {
        if (source == null)
            return;
        if (cur == NONE) {
            if (!source.tryAdvance(this::setCur)) {
                if(last != NONE)
                    action.accept(lastAcc);
                return;
            }
        }
        Box<R> accBox = new Box<>(mapper.apply(cur));
        source.forEachRemaining(next -> {
            if (mergeable.test(cur, next)) {
                accBox.a = accumulator.apply(accBox.a, next);
            } else {
                action.accept(accBox.a);
                accBox.a = mapper.apply(next);
            }
            cur = next;
        });
        if (last == NONE) {
            action.accept(accBox.a);
        } else if (mergeable.test(cur, last)) {
            action.accept(combiner.apply(accBox.a, lastAcc));
        } else {
            action.accept(accBox.a);
            action.accept(lastAcc);
        }
        source = null;
    }

    @Override
    public Spliterator<R> trySplit() {
        Spliterator<T> prefix = source.trySplit();
        if (prefix == null)
            return null;
        T prev = cur;
        if (!source.tryAdvance(this::setCur)) {
            source = prefix;
            return null;
        }
        T last = cur;
        T lastNext = last;
        R lastAcc = mapper.apply(cur);
        while (source.tryAdvance(this::setCur)) {
            if (mergeable.test(lastNext, cur)) {
                lastAcc = accumulator.apply(lastAcc, cur);
                lastNext = cur;
            } else {
                return new CollapseSpliterator2<>(mergeable, mapper, accumulator, combiner, prefix, prev, last,
                        lastAcc, root);
            }
        }
        if (this.last == NONE) {
            source = prefix;
            cur = prev;
            this.last = last;
            this.lastAcc = lastAcc;
            return null;
        }
        if (mergeable.test(lastNext, this.last)) {
            source = prefix;
            cur = prev;
            this.last = last;
            this.lastAcc = combiner.apply(lastAcc, this.lastAcc);
            return null;
        }
        cur = this.last;
        Spliterator<R> result = new CollapseSpliterator2<>(mergeable, mapper, accumulator, combiner, prefix, prev,
                last, lastAcc, root);
        this.last = none();
        this.lastAcc = none();
        return result;
    }

    @Override
    public long estimateSize() {
        return source == null ? 0 : source.estimateSize();
    }

    @Override
    public int characteristics() {
        return source == null ? (SIZED | DISTINCT) : source.characteristics() & (CONCURRENT | IMMUTABLE | ORDERED);
    }
}
