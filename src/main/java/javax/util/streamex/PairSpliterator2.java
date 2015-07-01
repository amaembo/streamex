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
import java.util.function.BinaryOperator;
import java.util.function.Consumer;

import static javax.util.streamex.StreamExInternals.*;

abstract class PairSpliterator2<T, S extends Spliterator<T>, R, SS extends PairSpliterator2<T,S,R,SS>> implements Spliterator<R>, Cloneable {
    S source;
    Merger.Sink<T> left = Merger.empty();
    Merger.Sink<T> right = Merger.empty();

    public PairSpliterator2(S source) {
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
            & ((left == Merger.empty() && right == Merger.empty() ? SIZED : 0) | CONCURRENT | IMMUTABLE | ORDERED);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SS trySplit() {
        S prefixSource = (S) source.trySplit();
        if (prefixSource == null)
            return null;
        SS clone;
        try {
            clone = (SS)clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        Merger<T> merger = new Merger<>();
        clone.source = prefixSource;
        clone.right = merger.getLeft();
        this.left = merger.getRight();
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

        private BinaryOperator<T> fn(Consumer<? super R> action) {
            return (a, b) -> {
                action.accept(mapper.apply(a, b));
                return a;
            };
        }

        @Override
        public boolean tryAdvance(Consumer<? super R> action) {
            if (left != null) {
                boolean result;
                if (!source.tryAdvance(this::setCur)) {
                    result = left.connect(right, fn(action)) != NONE;
                    left = right = null;
                    return result;
                }
                result = left.push(cur, fn(action)) != NONE;
                left = null;
                if (result)
                    return result;
            }
            T prev = cur;
            if (!source.tryAdvance(this::setCur)) {
                if (right == null)
                    return false;
                boolean result = right.push(prev, fn(action)) != NONE;
                right = null;
                return result;
            }
            action.accept(mapper.apply(prev, cur));
            return true;
        }

        @Override
        public void forEachRemaining(Consumer<? super R> action) {
            if (left != null) {
                if (!source.tryAdvance(this::setCur)) {
                    left.connect(right, fn(action));
                    left = right = null;
                    return;
                }
                left.push(cur, fn(action));
                left = null;
            }
            source.forEachRemaining(next -> {
                action.accept(mapper.apply(cur, next));
                this.cur = next;
            });
            if(right != null) {
                right.push(cur, fn(action));
            }
        }
    }
}
