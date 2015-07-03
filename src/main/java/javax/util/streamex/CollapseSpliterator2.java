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
    private final CollapseSpliterator2<T, R> root;
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
    }

    void setCur(T t) {
        cur = t;
    }
    
    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        if (source == null)
            return false;
        Box<Container<T, R>> l = left, r = right;
        if (l != null) {
            if(accept(handleLeft(l, r), action)) {
                return true;
            }
        }
        return true;
    }
    
    private boolean accept(R acc, Consumer<? super R> action) {
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
    
    private R handleRight(R acc, T last, Box<Container<T, R>> r) {
        if(r == null)
            return acc;
        synchronized(root) {
            right = null;
            if(r.a == null)
                return acc;
            T raleft = r.a.left;
            r.a.left = none();
            if(r.a.acc == NONE) {
                r.a.acc = acc;
                r.a.right = last;
                return none();
            }
            if(acc == NONE) {
                return r.a.right == NONE ? drain(r) : none();
            }
            assert raleft != NONE;
            assert last != NONE;
            if(mergeable.test(last, raleft)) {
                r.a.acc = combiner.apply(acc, r.a.acc);
                return r.a.right == NONE ? drain(r) : none();
            }
            if(r.a.right == NONE)
                right = new Box<>(new Container<>(drain(r)));
            return acc;
        }
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
                if(this.mergeable.test(last, cur)) {
                    last = cur;
                    acc = this.accumulator.apply(acc, last);
                } else {
                    // push-left
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
                            return l.a.left == NONE ? drain(l) : none();
                        }
                        if(l.a.left == NONE) {
                            left = new Box<>(new Container<>(acc));
                            return drain(l);
                        }
                    }
                    return none();
                }
            }
            // connect-one (first, acc, last)
            synchronized (root) {
                left = right = null;
                if(l.a == null) {
                    return handleRight(acc, last, r);
                }
                if(l.a.acc == NONE) {
                    l.a.acc = acc;
                    l.a.left = first;
                    l.a.right = last;
                    return connectEmpty(l, r);
                }
                T laright = l.a.right;
                if(mergeable.test(laright, first)) {
                    l.a.acc = combiner.apply(l.a.acc, acc);
                    l.a.right = last;
                    return connectEmpty(l, r);
                }
                l.a.right = none();
                if(l.a.left != NONE) {
                    return handleRight(acc, last, r);
                }
                // TODO
            }
        } else {
            return connectEmpty(l, r);
        }
        return none();
    }

    private R connectEmpty(Box<Container<T, R>> l, Box<Container<T, R>> r) {
        synchronized (root) {
            left = right = null;
            if(l.a == null) {
                return handleRight(none(), none(), r);
            }
            T laright = l.a.right;
            l.a.right = none();
            if(l.a.acc == NONE) {
                l.a = r.a;
                return none();
            }
            assert laright != NONE;
            if(r == null || r.a == null) {
                return l.a.left == NONE ? drain(l) : none();
            }
            T raleft = r.a.left;
            r.a.left = none();
            if(r.a.acc == NONE) {
                r.a = l.a;
                return none();
            }
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
            return r.a.right == NONE ? drain(r) : none();
        }
    }

    @Override
    public Spliterator<R> trySplit() {
        Spliterator<T> prefix = source.trySplit();
        if (prefix == null)
            return null;
        Box<Container<T, R>> newBox = new Box<>(new Container<>(none()));
        CollapseSpliterator2<T, R> result = new CollapseSpliterator2<>(root, prefix, newBox, right);
        this.right = newBox;
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
