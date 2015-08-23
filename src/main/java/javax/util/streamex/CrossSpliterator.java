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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author Tagir Valeev
 */
/* package */class CrossSpliterator<T, A, R> implements Spliterator<R> {
    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final Function<A, R> finisher;
    private long est;
    private List<Entry<T>> entries;
    private final int splitPos;

    private static class Entry<T> implements Consumer<T> {
        T cur;
        Spliterator<T> spliterator;
        final Collection<T> collection;
        Entry<T> prev;

        Entry(Entry<T> prev, Collection<T> collection, Spliterator<T> spliterator) {
            this.prev = prev;
            this.collection = collection;
            this.spliterator = spliterator;
        }

        @Override
        public void accept(T t) {
            this.cur = t;
        }

        boolean advance() {
            if (spliterator == null) {
                if (prev != null && !prev.advance())
                    return false;
                spliterator = collection.spliterator();
            }
            if (!spliterator.tryAdvance(this)) {
                if (prev == null || !prev.advance())
                    return false;
                spliterator = collection.spliterator();
                if (!spliterator.tryAdvance(this))
                    return false;
            }
            return true;
        }
    }

    public CrossSpliterator(Collection<? extends Collection<T>> source, Collector<T, A, R> collector) {
        this.supplier = collector.supplier();
        this.accumulator = collector.accumulator();
        this.finisher = collector.finisher();
        this.splitPos = 0;
        long est = 1;
        try {
            for (Collection<T> c : source) {
                est = StrictMath.multiplyExact(est, c.size());
            }
        } catch (ArithmeticException e) {
            est = Long.MAX_VALUE;
        }
        this.est = est;
        this.entries = new ArrayList<>(source.size());
        Entry<T> prev = null;
        for (Collection<T> c : source) {
            prev = new Entry<>(prev, c, null);
            this.entries.add(prev);
        }
    }

    CrossSpliterator(Supplier<A> supplier, BiConsumer<A, T> accumulator, Function<A, R> finisher, long est,
            int splitPos, List<Entry<T>> entries) {
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.finisher = finisher;
        this.est = est;
        this.splitPos = splitPos;
        this.entries = entries;
    }

    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        if (entries == null)
            return false;
        if (est < Long.MAX_VALUE && est > 0)
            est--;
        if (entries.get(entries.size() - 1).advance()) {
            action.accept(collect());
            return true;
        }
        entries = null;
        est = 0;
        return false;
    }

    private R collect() {
        A res = supplier.get();
        for (Entry<T> e : entries) {
            accumulator.accept(res, e.cur);
        }
        return finisher.apply(res);
    }

    @Override
    public Spliterator<R> trySplit() {
        Entry<T> entry = entries.get(splitPos);
        if (entry.spliterator == null)
            entry.spliterator = entry.collection.spliterator();
        Spliterator<T> res = entry.spliterator.trySplit();
        if (res == null) {
            // TODO: support further split
            return null;
        }
        long prefixEst = Long.MAX_VALUE;
        long newEst = Long.MAX_VALUE;
        if (est < Long.MAX_VALUE) {
            newEst = entry.spliterator.getExactSizeIfKnown();
            try {
                for (int i = splitPos + 1; i < entries.size(); i++) {
                    newEst = StrictMath.multiplyExact(newEst, entries.get(i).collection.size());
                }
                prefixEst = est - newEst;
            } catch (ArithmeticException e) {
                newEst = Long.MAX_VALUE;
            }
        }
        List<Entry<T>> prefixEntries = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            entry = entries.get(i);
            if (i < splitPos) {
                prefixEntries.add(entry);
            } else if (i == splitPos) {
                prefixEntries.add(new Entry<>(null, entry.collection, res));
                entry.prev = null;
            } else {
                prefixEntries.add(entry);
                entry.prev = prefixEntries.get(i - 1);
                entries.set(i, new Entry<>(entries.get(i - 1), entry.collection, null));
            }
        }
        this.est = newEst;
        return new CrossSpliterator<>(supplier, accumulator, finisher, prefixEst, splitPos, prefixEntries);
    }

    @Override
    public long estimateSize() {
        return est;
    }

    @Override
    public int characteristics() {
        int sized = est < Long.MAX_VALUE ? SIZED : 0;
        return ORDERED | sized;
    }
}
