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

import javax.util.streamex.StreamExInternals.Box;

import static javax.util.streamex.StreamExInternals.*;

/* package */final class CollapseSpliterator2<T, R> implements Spliterator<R> {
    private final Spliterator<T> source;
    private final CollapseSpliterator2<T, R> root; // used as lock
    private T cur = none();
    private Box<Container<T, R>> left;
    private Box<Container<T, R>> right;
    private final Function<T, R> mapper;
    private final BiFunction<R, T, R> accumulator;
    private final BinaryOperator<R> combiner;
    private final BiPredicate<T, T> mergeable;

    private static final class Container<T, R> {
        T left = none(), right = none();
        R acc;
        
        Container(R acc) {
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

    private CollapseSpliterator2(CollapseSpliterator2<T, R> root, Spliterator<T> source, Box<Container<T, R>> left, Box<Container<T, R>> right) {
        this.source = source;
        this.root = root;
        this.mergeable = root.mergeable;
        this.mapper = root.mapper;
        this.accumulator = root.accumulator;
        this.combiner = root.combiner;
        this.left = left;
        this.right = right;
    }

    void setCur(T t) {
        cur = t;
    }
    
    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        if (left != null) {
            if(accept(handleLeft(left, right), action)) {
                return true;
            }
        }
        if(cur == none()) {// start
            if(!source.tryAdvance(this::setCur)) {
                return accept(pushRight(none(), none(), right), action);
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
        return accept(pushRight(acc, last, right), action);
    }
    
    private boolean accept(R acc, Consumer<? super R> action) {
        synchronized (root) {
            if (left != null && left.a != null && left.a.acc != NONE) {
                assert left.a.left == NONE || left.a.right != NONE;
            }
            if (right != null && right.a != null && right.a.acc != NONE) {
                assert right.a.right == NONE || right.a.left != NONE;
            }
            
        }
        if(acc != NONE) {
            action.accept(acc);
            return true;
        }
        return false;
    }
    
    private R drain(Box<Container<T, R>> box) {
        R acc = box.a.acc;
        box.a = null;
        return acc;
    }
    
    private R drainLeft(Box<Container<T, R>> l) {
        return l.a.left == NONE ? drain(l) : none();
    }

    private R drainRight(Box<Container<T, R>> r) {
        return r.a.right == NONE ? drain(r) : none();
    }

    private R handleLeft(Box<Container<T, R>> l, Box<Container<T, R>> r) {
        synchronized(root) {
            if(l.a == null) {
                left = null;
                return none();
            }
            if(l.a.left == NONE && l.a.right == NONE && l.a.acc != NONE) {
                left = null;
                return drain(l);
            }
        }
        if(source.tryAdvance(this::setCur)) {
            T first = this.cur;
            T last = first;
            R acc = this.mapper.apply(first);
            while(source.tryAdvance(this::setCur)) {
                if(!this.mergeable.test(last, cur))
                    return pushLeft(l, first, acc);
                last = cur;
                acc = this.accumulator.apply(acc, last);
            }
            cur = none();
            return connectOne(l, first, acc, last, r);
        }
        return connectEmpty(l, r);
    }

    // l + <first|acc|?>
    private R pushLeft(Box<Container<T, R>> l, T first, R acc) {
        synchronized(root) {
            if(l.a == null)
                return acc;
            left = null;
            T laright = l.a.right;
            l.a.right = none();
            if(l.a.acc == NONE) {
                l.a.acc = acc;
                l.a.left = first;
                return none();
            }
            assert laright != NONE;
            if(this.mergeable.test(laright, first)) {
                l.a.acc = this.combiner.apply(l.a.acc, acc);
                return drainLeft(l);
            }
            if(l.a.left == NONE) {
                left = new Box<>(new Container<>(acc));
                return drain(l);
            }
        }
        return acc;
    }

    // <?|acc|last> + r
    private R pushRight(R acc, T last, Box<Container<T, R>> r) {
        cur = none();
        if(r == null)
            return acc;
        synchronized(root) {
            right = null;
            if(r.a == null)
                return acc;
            T raleft = r.a.left;
            r.a.left = none();
            if(r.a.acc == NONE) {
                if(acc == NONE) {
                    r.a = null;
                } else {
                    r.a.acc = acc;
                    r.a.right = last;
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
                r.a.acc = combiner.apply(acc, r.a.acc);
                return drainRight(r);
            }
            if(r.a.right == NONE)
                right = new Box<>(new Container<>(drain(r)));
            return acc;
        }
    }

    // l + <first|acc|last> + r
    private R connectOne(Box<Container<T, R>> l, T first, R acc, T last, Box<Container<T, R>> r) {
        synchronized (root) {
            left = right = null;
            if(l.a == null) {
                return pushRight(acc, last, r);
            }
            if(l.a.acc == NONE) {
                l.a.acc = acc;
                l.a.left = first;
                l.a.right = last;
                return connectEmpty(l, r);
            }
            T laright = l.a.right;
            assert laright != NONE;
            if(mergeable.test(laright, first)) {
                l.a.acc = combiner.apply(l.a.acc, acc);
                l.a.right = last;
                return connectEmpty(l, r);
            }
            l.a.right = none();
            if(l.a.left != NONE) {
                return pushRight(acc, last, r);
            }
            acc = pushRight(acc, last, r);
            if(acc != NONE)
                left = new Box<>(new Container<>(acc));
            return drain(l);
        }
    }

    // l + r
    private R connectEmpty(Box<Container<T, R>> l, Box<Container<T, R>> r) {
        synchronized (root) {
            left = right = null;
            if(l.a == null) {
                return pushRight(none(), none(), r);
            }
            T laright = l.a.right;
            l.a.right = none();
            if(l.a.acc == NONE) {
                l.a = r == null ? null : r.a;
                return none();
            }
            assert laright != NONE;
            if(r == null || r.a == null) {
                return drainLeft(l);
            }
            if(r.a.acc == NONE) {
                r.a = l.a;
                l.a.right = laright;
                return none();
            }
            T raleft = r.a.left;
            r.a.left = none();
            assert raleft != NONE;
            if(mergeable.test(laright, raleft)) {
                R acc = combiner.apply(l.a.acc, r.a.acc);
                if(l.a.left == NONE && r.a.right == NONE) {
                    l.a = r.a = null;
                    return acc;
                }
                l.a.acc = acc;
                l.a.right = r.a.right;
                r.a = l.a;
                return none();
            }
            if(l.a.left == NONE) {
                if(r.a.right == NONE)
                    right = new Box<>(new Container<>(drain(r)));
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
        Box<Container<T, R>> newBox = new Box<>(new Container<>(none()));
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
