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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static one.util.streamex.StreamExInternals.*;

/* package */final class DistinctSpliterator<T> extends Box<T> implements Spliterator<T>, Consumer<T> {
    private final Spliterator<T> source;
    private AtomicLong nullCounter;
    private Map<T, Long> counts;
    private final long atLeast;

    DistinctSpliterator(Spliterator<T> source, long atLeast, AtomicLong nullCounter, Map<T, Long> counts) {
        this.source = source;
        this.atLeast = atLeast;
        this.nullCounter = nullCounter;
        this.counts = counts;
    }

    DistinctSpliterator(Spliterator<T> source, long atLeast) {
        this(source, atLeast, null, new HashMap<>());
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (nullCounter == null) {
            while (source.tryAdvance(this)) {
                if (counts.merge(a, 1L, Long::sum) == atLeast) {
                    action.accept(a);
                    return true;
                }
            }
        } else {
            while (source.tryAdvance(this)) {
                long count = a == null ? nullCounter.incrementAndGet() : counts.merge(a, 1L, Long::sum);
                if (count == atLeast) {
                    action.accept(a);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        if (nullCounter == null) {
            source.forEachRemaining(e -> {
                if (counts.merge(e, 1L, Long::sum) == atLeast) {
                    action.accept(e);
                }
            });
        } else {
            source.forEachRemaining(e -> {
                long count = e == null ? nullCounter.incrementAndGet() : counts.merge(e, 1L, Long::sum);
                if (count == atLeast) {
                    action.accept(e);
                }
            });
        }
    }

    @Override
    public Spliterator<T> trySplit() {
        Spliterator<T> split = source.trySplit();
        if (split == null)
            return null;
        if (counts.getClass() == HashMap.class) {
            if (!source.hasCharacteristics(NONNULL)) {
                Long current = counts.remove(null);
                nullCounter = new AtomicLong(current == null ? 0 : current);
            }
            counts = new ConcurrentHashMap<>(counts);
        }
        return new DistinctSpliterator<>(split, atLeast, nullCounter, counts);
    }

    @Override
    public long estimateSize() {
        return source.estimateSize();
    }

    @Override
    public int characteristics() {
        return DISTINCT | (source.characteristics() & (NONNULL | CONCURRENT | IMMUTABLE | ORDERED | SORTED));
    }

    @Override
    public Comparator<? super T> getComparator() {
        return source.getComparator();
    }
}
