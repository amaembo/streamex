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

import static javax.util.streamex.StreamExInternals.*;

abstract class PairSpliterator2<T, S extends Spliterator<T>, R, SS extends PairSpliterator2<T, S, R, SS>> implements
        Spliterator<R>, Cloneable {
    private static Sink<?> EMPTY = new Sink<>(null);
    S source;
    @SuppressWarnings("unchecked")
    Sink<T> left = (Sink<T>) EMPTY;
    @SuppressWarnings("unchecked")
    Sink<T> right = (Sink<T>) EMPTY;

    private static final class Sink<T> {
        Merger<T> m;
        private T payload = none();

        Sink(Merger<T> m) {
            this.m = m;
        }

        boolean push(T payload, BiConsumer<T, T> fn) {
            assert this.payload == NONE;
            if (m == null)
                return false;
            synchronized (m) {
                this.payload = payload;
                return m.operate(fn);
            }
        }

        boolean connect(Sink<T> right, BiConsumer<T, T> fn) {
            assert payload == NONE;
            if (m == null) {
                if (right.m != null) {
                    right.m.clear();
                }
                return false;
            }
            assert m.right == this;
            if (right.m == null) {
                m.clear();
                return false;
            }
            assert right.m.left == right;
            synchronized (m) {
                synchronized (right.m) {
                    m.right = right.m.right;
                    m.right.m = m;
                    return m.operate(fn);
                }
            }
        }

        void clear() {
            m = null;
            payload = none();
        }
    }

    private static final class Merger<T> {
        final Sink<T> left;
        Sink<T> right;

        Merger() {
            this.left = new Sink<>(this);
            this.right = new Sink<>(this);
        }

        boolean operate(BiConsumer<T, T> fn) {
            if (left.payload == NONE || right.payload == NONE)
                return false;
            fn.accept(left.payload, right.payload);
            clear();
            return true;
        }

        void clear() {
            synchronized (this) {
                left.clear();
                right.clear();
            }
        }
    }

    PairSpliterator2(S source) {
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
        SS clone;
        try {
            clone = (SS) clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        Merger<T> merger = new Merger<>();
        clone.source = prefixSource;
        clone.right = merger.left;
        this.left = merger.right;
        return clone;
    }

    static final class PSOfRef<T, R> extends PairSpliterator2<T, Spliterator<T>, R, PSOfRef<T, R>> {
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
            source.forEachRemaining(next -> {
                action.accept(mapper.apply(cur, next));
                this.cur = next;
            });
            if (r != null) {
                r.push(cur, fn(action));
            }
        }
    }
}
