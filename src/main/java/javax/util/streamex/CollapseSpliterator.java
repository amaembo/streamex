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
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;

/* package */ final class CollapseSpliterator<T> implements Spliterator<T> {
    private Spliterator<T> source;
    private boolean hasPrev, hasLast;
    private T cur, last;
    private final BinaryOperator<T> merger;
    private final BiPredicate<T, T> mergeable;

    CollapseSpliterator(BiPredicate<T, T> mergeable, BinaryOperator<T> merger, Spliterator<T> source, T prev,
            boolean hasPrev, T last, boolean hasLast) {
        this.source = source;
        this.hasLast = hasLast;
        this.hasPrev = hasPrev;
        this.cur = prev;
        this.last = last;
        this.mergeable = mergeable;
        this.merger = merger;
    }

    void setCur(T t) {
        cur = t;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (source == null)
            return false;
        if (!hasPrev) {
            if (!source.tryAdvance(this::setCur)) {
                return false;
            }
            hasPrev = true;
        }
        T prev = cur;
        while (source.tryAdvance(this::setCur)) {
            if (mergeable.test(prev, cur)) {
                prev = merger.apply(prev, cur);
            } else {
                action.accept(prev);
                return true;
            }
        }
        if (!hasLast) {
            source = null;
        } else {
            cur = last;
            hasLast = false;
            if (mergeable.test(prev, cur)) {
                prev = merger.apply(prev, cur);
                source = null;
            }
        }
        action.accept(prev);
        return true;
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        if (source == null)
            return;
        if (!hasPrev) {
            if (!source.tryAdvance(this::setCur)) {
                return;
            }
            hasPrev = true;
        }
        source.forEachRemaining(next -> {
            if (mergeable.test(cur, next)) {
                cur = merger.apply(cur, next);
            } else {
                action.accept(cur);
                cur = next;
            }
        });
        if (!hasLast) {
            action.accept(cur);
        } else if (mergeable.test(cur, last)) {
            action.accept(merger.apply(cur, last));
        } else {
            action.accept(cur);
            action.accept(last);
        }
        source = null;
    }

    @Override
    public Spliterator<T> trySplit() {
        Spliterator<T> prefix = source.trySplit();
        if (prefix == null)
            return null;
        T prev = cur;
        if (!source.tryAdvance(this::setCur)) {
            source = prefix;
            return null;
        }
        T last = cur;
        boolean oldHasPrev = hasPrev;
        while (source.tryAdvance(this::setCur)) {
            if (mergeable.test(last, cur)) {
                last = merger.apply(last, cur);
            } else {
                hasPrev = true;
                return new CollapseSpliterator<>(mergeable, merger, prefix, prev, oldHasPrev, last, true);
            }
        }
        if (!hasLast) {
            source = prefix;
            cur = prev;
            this.last = last;
            hasLast = true;
            return null;
        }
        if (mergeable.test(last, this.last)) {
            source = prefix;
            cur = prev;
            this.last = merger.apply(last, this.last);
            return null;
        }
        hasPrev = true;
        cur = this.last;
        hasLast = false;
        return new CollapseSpliterator<>(mergeable, merger, prefix, prev, oldHasPrev, last, true);
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
