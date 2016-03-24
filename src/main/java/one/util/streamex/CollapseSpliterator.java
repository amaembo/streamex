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
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

import static one.util.streamex.StreamExInternals.*;

/* package */final class CollapseSpliterator<T, R> extends Box<T> implements Spliterator<R>, Consumer<T> {
    private final Spliterator<T> source;
    private final CollapseSpliterator<T, R> root; // used as lock
    private R acc;
    volatile Connector<T, R> left;
    volatile Connector<T, R> right;
    private final Function<T, R> mapper;
    private final BiFunction<R, T, R> accumulator;
    private final BinaryOperator<R> combiner;
    private final BiPredicate<? super T, ? super T> mergeable;

    private static final class Connector<T, R> {
        CollapseSpliterator<T, R> lhs, rhs;
        T left = none(), right = none();
        R acc;

        Connector(CollapseSpliterator<T, R> lhs, R acc, CollapseSpliterator<T, R> rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
            this.acc = acc;
        }

        R drain() {
            if (lhs != null)
                lhs.right = null;
            if (rhs != null)
                rhs.left = null;
            return acc;
        }

        R drainLeft() {
            return left == NONE ? drain() : none();
        }

        R drainRight() {
            return right == NONE ? drain() : none();
        }
    }

    CollapseSpliterator(BiPredicate<? super T, ? super T> mergeable, Function<T, R> mapper,
            BiFunction<R, T, R> accumulator, BinaryOperator<R> combiner, Spliterator<T> source) {
        super(none());
        this.source = source;
        this.mergeable = mergeable;
        this.mapper = mapper;
        this.accumulator = accumulator;
        this.combiner = combiner;
        this.root = this;
    }

    private CollapseSpliterator(CollapseSpliterator<T, R> root, Spliterator<T> source, Connector<T, R> left,
            Connector<T, R> right) {
        super(none());
        this.source = source;
        this.root = root;
        this.mergeable = root.mergeable;
        this.mapper = root.mapper;
        this.accumulator = root.accumulator;
        this.combiner = root.combiner;
        this.left = left;
        this.right = right;
        if (left != null)
            left.rhs = this;
        right.lhs = this;
    }

    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        if (left != null) {
            if (accept(handleLeft(), action)) {
                return true;
            }
        }
        if (a == NONE) {// start
            if (!source.tryAdvance(this)) {
                return accept(pushRight(none(), none()), action);
            }
        }
        T first = a;
        R acc = mapper.apply(a);
        T last = first;
        while (source.tryAdvance(this)) {
            if (!this.mergeable.test(last, a)) {
                action.accept(acc);
                return true;
            }
            last = a;
            acc = this.accumulator.apply(acc, last);
        }
        return accept(pushRight(acc, last), action);
    }

    @Override
    public void forEachRemaining(Consumer<? super R> action) {
        while (left != null) {
            accept(handleLeft(), action);
        }
        if (a != NONE) {
            acc = mapper.apply(a);
        }
        source.forEachRemaining(next -> {
            if (a == NONE) {
                acc = mapper.apply(next);
            } else if (!this.mergeable.test(a, next)) {
                action.accept(acc);
                acc = mapper.apply(next);
            } else {
                acc = accumulator.apply(acc, next);
            }
            a = next;
        });
        if (a == NONE) {
            accept(pushRight(none(), none()), action);
        } else if (accept(pushRight(acc, a), action)) {
            if (right != null) {
                action.accept(right.acc);
                right = null;
            }
        }
    }

    private boolean accept(R acc, Consumer<? super R> action) {
        if (acc != NONE) {
            action.accept(acc);
            return true;
        }
        return false;
    }

    private R handleLeft() {
        synchronized (root) {
            Connector<T, R> l = left;
            if (l == null) {
                return none();
            }
            if (l.left == NONE && l.right == NONE && l.acc != NONE) {
                return l.drain();
            }
        }
        if (source.tryAdvance(this)) {
            T first = this.a;
            T last = first;
            R acc = this.mapper.apply(first);
            while (source.tryAdvance(this)) {
                if (!this.mergeable.test(last, a))
                    return pushLeft(first, acc);
                last = a;
                acc = this.accumulator.apply(acc, last);
            }
            a = none();
            return connectOne(first, acc, last);
        }
        return connectEmpty();
    }

    // l + <first|acc|?>
    private R pushLeft(T first, R acc) {
        synchronized (root) {
            Connector<T, R> l = left;
            if (l == null)
                return acc;
            left = null;
            l.rhs = null;
            T laright = l.right;
            l.right = none();
            if (l.acc == NONE) {
                l.acc = acc;
                l.left = first;
                return none();
            }
            if (this.mergeable.test(laright, first)) {
                l.acc = this.combiner.apply(l.acc, acc);
                return l.drainLeft();
            }
            if (l.left == NONE) {
                left = new Connector<>(null, acc, this);
                return l.drain();
            }
        }
        return acc;
    }

    // <?|acc|last> + r
    private R pushRight(R acc, T last) {
        a = none();
        if (right == null)
            return acc;
        synchronized (root) {
            Connector<T, R> r = right;
            if (r == null)
                return acc;
            right = null;
            r.lhs = null;
            T raleft = r.left;
            r.left = none();
            if (r.acc == NONE) {
                if (acc == NONE) {
                    r.drain();
                } else {
                    r.acc = acc;
                    r.right = last;
                }
                return none();
            }
            if (acc == NONE) {
                return r.drainRight();
            }
            if (mergeable.test(last, raleft)) {
                r.acc = combiner.apply(acc, r.acc);
                return r.drainRight();
            }
            if (r.right == NONE)
                right = new Connector<>(this, r.drain(), null);
            return acc;
        }
    }

    // l + <first|acc|last> + r
    private R connectOne(T first, R acc, T last) {
        synchronized (root) {
            Connector<T, R> l = left;
            if (l == null) {
                return pushRight(acc, last);
            }
            if (l.acc == NONE) {
                l.acc = acc;
                l.left = first;
                l.right = last;
                return connectEmpty();
            }
            T laright = l.right;
            if (mergeable.test(laright, first)) {
                l.acc = combiner.apply(l.acc, acc);
                l.right = last;
                return connectEmpty();
            }
            left = null;
            l.rhs = null;
            l.right = none();
            if (l.left != NONE) {
                return pushRight(acc, last);
            }
            acc = pushRight(acc, last);
            if (acc != NONE)
                left = new Connector<>(null, acc, this);
            return l.drain();
        }
    }

    // l + r
    private R connectEmpty() {
        synchronized (root) {
            Connector<T, R> l = left, r = right;
            if (l == null) {
                return pushRight(none(), none());
            }
            left = right = null;
            l.rhs = null;
            T laright = l.right;
            l.right = none();
            if (l.acc == NONE) {
                if (r == null)
                    l.drain();
                else {
                    if (l.lhs != null) {
                        l.lhs.right = r;
                        r.lhs = l.lhs;
                    }
                }
                return none();
            }
            if (r == null) {
                return l.drainLeft();
            }
            r.lhs = null;
            if (r.acc == NONE) {
                if (r.rhs != null) {
                    r.rhs.left = l;
                    l.rhs = r.rhs;
                    l.right = laright;
                }
                return none();
            }
            T raleft = r.left;
            r.left = none();
            if (mergeable.test(laright, raleft)) {
                R acc = combiner.apply(l.acc, r.acc);
                if (l.left == NONE && r.right == NONE) {
                    l.drain();
                    r.drain();
                    return acc;
                }
                l.acc = acc;
                l.right = r.right;
                if (r.rhs != null) {
                    r.rhs.left = l;
                    l.rhs = r.rhs;
                }
                return none();
            }
            if (l.left == NONE) {
                if (r.right == NONE)
                    right = new Connector<>(this, r.drain(), null);
                return l.drain();
            }
            return r.drainRight();
        }
    }

    @Override
    public Spliterator<R> trySplit() {
        Spliterator<T> prefix = source.trySplit();
        if (prefix == null)
            return null;
        Connector<T, R> newBox = new Connector<>(null, none(), this);
        synchronized (root) {
            CollapseSpliterator<T, R> result = new CollapseSpliterator<>(root, prefix, left, newBox);
            this.left = newBox;
            return result;
        }
    }

    @Override
    public long estimateSize() {
        return source.estimateSize();
    }

    @Override
    public int characteristics() {
        return source.characteristics() & (CONCURRENT | IMMUTABLE | ORDERED);
    }
}
