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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static one.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
/* package */class UnorderedCancellableSpliterator<T, A> extends CloneableSpliterator<A, UnorderedCancellableSpliterator<T, A>> {
    private volatile Spliterator<T> source;
    private final BiConsumer<A, ? super T> accumulator;
    private final Predicate<A> cancelPredicate;
    private final Supplier<A> supplier;
    private final ConcurrentLinkedQueue<A> partialResults = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicInteger nPeers = new AtomicInteger(1);
    private final BinaryOperator<A> combiner;
    private boolean flag;

    UnorderedCancellableSpliterator(Spliterator<T> source, Supplier<A> supplier, BiConsumer<A, ? super T> accumulator,
            BinaryOperator<A> combiner, Predicate<A> cancelPredicate) {
        this.source = source;
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.combiner = combiner;
        this.cancelPredicate = cancelPredicate;
    }

    private boolean checkCancel(A acc) {
        if (cancelPredicate.test(acc)) {
            if (cancelled.compareAndSet(false, true)) {
                flag = true;
                return true;
            }
        }
        if (cancelled.get()) {
            flag = false;
            return true;
        }
        return false;
    }

    private boolean handleCancel(Consumer<? super A> action, A acc) {
        source = null;
        if (flag) {
            action.accept(acc);
            return true;
        }
        return false;
    }

    @Override
    public boolean tryAdvance(Consumer<? super A> action) {
        Spliterator<T> source = this.source;
        if (source == null || cancelled.get()) {
            this.source = null;
            return false;
        }
        A acc = supplier.get();
        if (checkCancel(acc))
            return handleCancel(action, acc);
        try {
            source.forEachRemaining(t -> {
                accumulator.accept(acc, t);
                if (checkCancel(acc))
                    throw new CancelException();
            });
        } catch (CancelException ex) {
            return handleCancel(action, acc);
        }
        A result = acc;
        while (true) {
            A acc2 = partialResults.poll();
            if (acc2 == null)
                break;
            result = combiner.apply(result, acc2);
            if (checkCancel(result))
                return handleCancel(action, result);
        }
        partialResults.offer(result);
        this.source = null;
        if (nPeers.decrementAndGet() == 0) {
            result = partialResults.poll();
            // non-cancelled finish
            while (true) {
                A acc2 = partialResults.poll();
                if (acc2 == null)
                    break;
                result = combiner.apply(result, acc2);
                if (cancelPredicate.test(result))
                    break;
            }
            this.source = null;
            action.accept(result);
            return true;
        }
        return false;
    }

    @Override
    public void forEachRemaining(Consumer<? super A> action) {
        tryAdvance(action);
    }

    @Override
    public Spliterator<A> trySplit() {
        if (source == null || cancelled.get()) {
            source = null;
            return null;
        }
        Spliterator<T> prefix = source.trySplit();
        if (prefix == null) {
            return null;
        }
        UnorderedCancellableSpliterator<T, A> result = doClone();
        result.source = prefix;
        nPeers.incrementAndGet();
        return result;
    }

    @Override
    public long estimateSize() {
        return source == null ? 0 : source.estimateSize();
    }

    @Override
    public int characteristics() {
        return source == null ? SIZED : 0;
    }
}
