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

import java.util.ArrayDeque;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import one.util.streamex.StreamExInternals.CloneableSpliterator;
import static one.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
/* package */final class OrderedCancellableSpliterator<T, A> extends CloneableSpliterator<A, OrderedCancellableSpliterator<T, A>> {
    private Spliterator<T> source;
    private final Object lock = new Object();
    private final BiConsumer<A, ? super T> accumulator;
    private final Predicate<A> cancelPredicate;
    private final BinaryOperator<A> combiner;
    private final Supplier<A> supplier;
    private volatile boolean localCancelled;
    private OrderedCancellableSpliterator<T, A> prefix;
    private OrderedCancellableSpliterator<T, A> suffix;
    private A payload;

    OrderedCancellableSpliterator(Spliterator<T> source, Supplier<A> supplier, BiConsumer<A, ? super T> accumulator,
            BinaryOperator<A> combiner, Predicate<A> cancelPredicate) {
        this.source = source;
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.combiner = combiner;
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
        try {
            source.forEachRemaining(t -> {
                accumulator.accept(acc, t);
                if (cancelPredicate.test(acc)) {
                    cancelSuffix();
                    throw new CancelException();
                }
                if (localCancelled) {
                    throw new CancelException();
                }
            });
        } catch (CancelException ex) {
            if (localCancelled) {
                return false;
            }
        }
        this.source = null;
        A result = acc;
        while (true) {
            if (prefix == null && suffix == null) {
                action.accept(result);
                return true;
            }
            ArrayDeque<A> res = new ArrayDeque<>();
            res.offer(result);
            synchronized (lock) {
                if (localCancelled)
                    return false;
                OrderedCancellableSpliterator<T, A> s = prefix;
                while (s != null) {
                    if (s.payload == null)
                        break;
                    res.offerFirst(s.payload);
                    s = s.prefix;
                }
                prefix = s;
                if (s != null) {
                    s.suffix = this;
                }
                s = suffix;
                while (s != null) {
                    if (s.payload == null)
                        break;
                    res.offerLast(s.payload);
                    s = s.suffix;
                }
                suffix = s;
                if (s != null) {
                    s.prefix = this;
                }
                if (res.size() == 1) {
                    if (prefix == null && suffix == null) {
                        action.accept(result);
                        return true;
                    }
                    this.payload = result;
                    break;
                }
            }
            result = res.pollFirst();
            while (!res.isEmpty()) {
                result = combiner.apply(result, res.pollFirst());
                if (cancelPredicate.test(result)) {
                    cancelSuffix();
                }
            }
        }
        return false;
    }

    private void cancelSuffix() {
        if (this.suffix == null)
            return;
        synchronized (lock) {
            OrderedCancellableSpliterator<T, A> suffix = this.suffix;
            while (suffix != null && !suffix.localCancelled) {
                suffix.prefix = null;
                suffix.localCancelled = true;
                suffix = suffix.suffix;
            }
            this.suffix = null;
        }
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
        synchronized (lock) {
            OrderedCancellableSpliterator<T, A> result = doClone();
            result.source = prefix;
            this.prefix = result;
            result.suffix = this;
            OrderedCancellableSpliterator<T, A> prefixPrefix = result.prefix;
            if (prefixPrefix != null)
                prefixPrefix.suffix = result;
            return result;
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
