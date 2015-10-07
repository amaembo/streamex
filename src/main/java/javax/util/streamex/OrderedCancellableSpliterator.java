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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author Tagir Valeev
 */
/* package */final class OrderedCancellableSpliterator<T, A> implements Spliterator<A>, Cloneable {
    private volatile Spliterator<T> source;
    private final BiConsumer<A, ? super T> accumulator;
    private final Predicate<A> cancelPredicate;
    private final Supplier<A> supplier;
    private volatile boolean localCancelled;
    private OrderedCancellableSpliterator<T, A> prefix;
    private volatile OrderedCancellableSpliterator<T, A> suffix;

    OrderedCancellableSpliterator(Spliterator<T> source, Supplier<A> supplier, BiConsumer<A, ? super T> accumulator,
            Predicate<A> cancelPredicate) {
        this.source = source;
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.cancelPredicate = cancelPredicate;
    }

    @Override
    public boolean tryAdvance(Consumer<? super A> action) {
        Spliterator<T> source = this.source;
        if (source == null || localCancelled) {
            this.source = null;
            return false;
        }
        A acc = supplier.get();
        do {
            if (cancelPredicate.test(acc)) {
                this.source = null;
                this.localCancelled = true;
                OrderedCancellableSpliterator<T, A> suffix = this.suffix;
                // Due to possible race with trySplit some spliterators can
                // be skipped. This is handled in trySplit
                while (suffix != null && !suffix.localCancelled) {
                    suffix.localCancelled = true;
                    suffix = suffix.suffix;
                }
                action.accept(acc);
                return true;
            }
            if (localCancelled) {
                this.source = null;
                return false;
            }
        } while (source.tryAdvance(t -> accumulator.accept(acc, t)));
        this.source = null;
        action.accept(acc);
        return true;
    }

    @Override
    public void forEachRemaining(Consumer<? super A> action) {
        tryAdvance(action);
    }

    @Override
    public Spliterator<A> trySplit() {
        if (source == null || localCancelled) {
            source = null;
            return null;
        }
        Spliterator<T> prefix = source.trySplit();
        if (prefix == null) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            OrderedCancellableSpliterator<T, A> result = (OrderedCancellableSpliterator<T, A>) this.clone();
            result.source = prefix;
            this.prefix = result;
            result.suffix = this;
            OrderedCancellableSpliterator<T, A> prefixPrefix = result.prefix;
            if (prefixPrefix != null)
                prefixPrefix.suffix = result;
            if (this.localCancelled || result.localCancelled) {
                // we can end up here due to the race with suffix updates in
                // tryAdvance
                this.localCancelled = result.localCancelled = true;
                return null;
            }
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    @Override
    public long estimateSize() {
        return source == null ? 0 : source.estimateSize();
    }

    @Override
    public int characteristics() {
        return source == null ? SIZED : ORDERED;
    }
}
