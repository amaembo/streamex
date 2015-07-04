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
    private final Spliterator<T> source;
    private final CollapseSpliterator2<T, R> root; // used as lock
    private T cur = none();
    private volatile Container<T, R> left;
    private volatile Container<T, R> right;
    private final Function<T, R> mapper;
    private final BiFunction<R, T, R> accumulator;
    private final BinaryOperator<R> combiner;
    private final BiPredicate<T, T> mergeable;

    private static final class Container<T, R> {
        CollapseSpliterator2<T, R> lhs, rhs;
        T left = none(), right = none();
        R acc;
        
        Container(CollapseSpliterator2<T, R> lhs, R acc, CollapseSpliterator2<T, R> rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
            this.acc = acc;
        }
    }

    CollapseSpliterator2(BiPredicate<T, T> mergeable, Function<T, R> mapper, BiFunction<R, T, R> accumulator,
            BinaryOperator<R> combiner, Spliterator<T> source) {
        this.source = source;
        this.mergeable = mergeable;
        this.mapper = mapper;
        this.accumulator = accumulator;
        this.combiner = combiner;
        this.root = this;
    }

    private CollapseSpliterator2(CollapseSpliterator2<T, R> root, Spliterator<T> source, Container<T, R> left, Container<T, R> right) {
        this.source = source;
        this.root = root;
        this.mergeable = root.mergeable;
        this.mapper = root.mapper;
        this.accumulator = root.accumulator;
        this.combiner = root.combiner;
        this.left = left;
        this.right = right;
        if(left != null)
            left.rhs = this;
        if(right != null)
            right.lhs = this;
    }

    void setCur(T t) {
        cur = t;
    }
    
    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        if (left != null) {
            if(accept(handleLeft(), action)) {
                return true;
            }
        }
        if(cur == none()) {// start
            if(!source.tryAdvance(this::setCur)) {
                return accept(pushRight(none(), none()), action);
            }
        }
        T first = cur;
        R acc = mapper.apply(cur);
        T last = first;
        while(source.tryAdvance(this::setCur)) {
            if(!this.mergeable.test(last, cur)) {
                action.accept(acc);
                return true;
            }
            last = cur;
            acc = this.accumulator.apply(acc, last);
        }
        return accept(pushRight(acc, last), action);
    }
    
    private boolean accept(R acc, Consumer<? super R> action) {
        if(acc != NONE) {
            action.accept(acc);
            return true;
        }
        return false;
    }
    
    private R drain(Container<T, R> c) {
        if(c.lhs != null)
            c.lhs.right = null;
        if(c.rhs != null)
            c.rhs.left = null;
        return c.acc;
    }
    
    private R drainLeft(Container<T, R> l) {
        return l.left == NONE ? drain(l) : none();
    }

    private R drainRight(Container<T, R> r) {
        return r.right == NONE ? drain(r) : none();
    }

    private R handleLeft() {
        synchronized(root) {
            Container<T, R> l = left;
            if(l == null) {
                return none();
            }
            if(l.left == NONE && l.right == NONE && l.acc != NONE) {
                return drain(l);
            }
        }
        if(source.tryAdvance(this::setCur)) {
            T first = this.cur;
            T last = first;
            R acc = this.mapper.apply(first);
            while(source.tryAdvance(this::setCur)) {
                if(!this.mergeable.test(last, cur))
                    return pushLeft(first, acc);
                last = cur;
                acc = this.accumulator.apply(acc, last);
            }
            cur = none();
            return connectOne(first, acc, last);
        }
        return connectEmpty();
    }

    // l + <first|acc|?>
    private R pushLeft(T first, R acc) {
        synchronized(root) {
            Container<T, R> l = left;
            if(l == null)
                return acc;
            left = null;
            l.rhs = null;
            T laright = l.right;
            l.right = none();
            if(l.acc == NONE) {
                l.acc = acc;
                l.left = first;
                return none();
            }
            assert laright != NONE;
            if(this.mergeable.test(laright, first)) {
                l.acc = this.combiner.apply(l.acc, acc);
                return drainLeft(l);
            }
            if(l.left == NONE) {
                left = new Container<>(null, acc, this);
                return drain(l);
            }
        }
        return acc;
    }

    // <?|acc|last> + r
    private R pushRight(R acc, T last) {
        cur = none();
        if(right == null)
            return acc;
        synchronized(root) {
            Container<T, R> r = right;
            if(r == null)
                return acc;
            right = null;
            r.lhs = null;
            T raleft = r.left;
            r.left = none();
            if(r.acc == NONE) {
                if(acc == NONE) {
                    drain(r);
                } else {
                    r.acc = acc;
                    r.right = last;
                }
                return none();
            }
            if(acc == NONE) {
                return drainRight(r);
            }
            if(raleft == NONE) {
                assert raleft != NONE;
            }
            assert last != NONE;
            if(mergeable.test(last, raleft)) {
                r.acc = combiner.apply(acc, r.acc);
                return drainRight(r);
            }
            if(r.right == NONE)
                right = new Container<>(this, drain(r), null);
            return acc;
        }
    }

    // l + <first|acc|last> + r
    private R connectOne(T first, R acc, T last) {
        synchronized (root) {
            Container<T, R> l = left;
            if(l == null) {
                return pushRight(acc, last);
            }
            if(l.acc == NONE) {
                l.acc = acc;
                l.left = first;
                l.right = last;
                return connectEmpty();
            }
            T laright = l.right;
            assert laright != NONE;
            if(mergeable.test(laright, first)) {
                l.acc = combiner.apply(l.acc, acc);
                l.right = last;
                return connectEmpty();
            }
            left = null;
            l.rhs = null;
            l.right = none();
            if(l.left != NONE) {
                return pushRight(acc, last);
            }
            acc = pushRight(acc, last);
            if(acc != NONE)
                left = new Container<>(null, acc, this);
            return drain(l);
        }
    }

    // l + r
    private R connectEmpty() {
        synchronized (root) {
            Container<T, R> l = left, r = right;
            if(l == null) {
                return pushRight(none(), none());
            }
            left = right = null;
            l.rhs = null;
            T laright = l.right;
            l.right = none();
            if(l.acc == NONE) {
                if(r == null)
                    drain(l);
                else {
                    if(l.lhs != null) {
                        l.lhs.right = r;
                        r.lhs = l.lhs;
                    }
                }
                return none();
            }
            assert laright != NONE;
            if(r == null) {
                return drainLeft(l);
            }
            r.lhs = null;
            if(r.acc == NONE) {
                if(r.rhs != null) {
                    r.rhs.left = l;
                    l.rhs = r.rhs;
                    l.right = laright;
                }
                return none();
            }
            T raleft = r.left;
            r.left = none();
            assert raleft != NONE;
            if(mergeable.test(laright, raleft)) {
                R acc = combiner.apply(l.acc, r.acc);
                if(l.left == NONE && r.right == NONE) {
                    drain(l);
                    drain(r);
                    return acc;
                }
                l.acc = acc;
                l.right = r.right;
                if(r.rhs != null) {
                    r.rhs.left = l;
                    l.rhs = r.rhs;
                }
                return none();
            }
            if(l.left == NONE) {
                if(r.right == NONE)
                    right = new Container<>(this, drain(r), null);
                return drain(l);
            }
            return drainRight(r);
        }
    }

    @Override
    public Spliterator<R> trySplit() {
        Spliterator<T> prefix = source.trySplit();
        if (prefix == null)
            return null;
        Container<T, R> newBox = new Container<>(null, none(), this);
        CollapseSpliterator2<T, R> result = new CollapseSpliterator2<>(root, prefix, left, newBox);
        this.left = newBox;
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
