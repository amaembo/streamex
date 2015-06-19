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
    private Spliterator<T> source;
    private T cur, last;
    private R lastAcc;
    private final Function<T, R> mapper;
    private final BiFunction<R, T, R> accumulator;
    private final BinaryOperator<R> combiner;
    private final BiPredicate<T, T> mergeable;

    @SuppressWarnings("unchecked")
    CollapseSpliterator2(BiPredicate<T, T> mergeable, Function<T, R> mapper, BiFunction<R, T, R> accumulator,
            BinaryOperator<R> combiner, Spliterator<T> source) {
        this(mergeable, mapper, accumulator, combiner, source, (T) NONE, (T) NONE, (R) NONE);
    }

    CollapseSpliterator2(BiPredicate<T, T> mergeable, Function<T, R> mapper, BiFunction<R, T, R> accumulator,
            BinaryOperator<R> combiner, Spliterator<T> source, T prev, T last, R lastAcc) {
        this.source = source;
        this.cur = prev;
        this.last = last;
        this.lastAcc = lastAcc;
        this.mergeable = mergeable;
        this.mapper = mapper;
        this.accumulator = accumulator;
        this.combiner = combiner;
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
        R lastAcc = mapper.apply(cur);
        while (source.tryAdvance(this::setCur)) {
            if (mergeable.test(last, cur)) {
                lastAcc = accumulator.apply(lastAcc, cur);
                last = cur;
            } else {
                return new CollapseSpliterator2<>(mergeable, mapper, accumulator, combiner, prefix, prev, last,
                        lastAcc);
            }
        }
        if (this.last == NONE) {
            source = prefix;
            cur = prev;
            this.last = last;
            this.lastAcc = lastAcc;
            return null;
        }
        if (mergeable.test(last, this.last)) {
            source = prefix;
            cur = prev;
            this.last = last;
            this.lastAcc = combiner.apply(lastAcc, this.lastAcc);
            return null;
        }
        cur = this.last;
        Spliterator<R> result = new CollapseSpliterator2<>(mergeable, mapper, accumulator, combiner, prefix, prev,
                last, lastAcc);
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
