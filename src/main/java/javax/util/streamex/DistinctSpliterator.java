package javax.util.streamex;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

final class DistinctSpliterator<T> implements Spliterator<T> {
    final Spliterator<T> source;
    AtomicLong nullCounter;
    Map<T, Long> counts;
    final long atLeast;
    T cur;

    DistinctSpliterator(Spliterator<T> source, long atLeast, AtomicLong nullCounter, Map<T, Long> counts) {
        this.source = source;
        this.atLeast = atLeast;
        this.nullCounter = nullCounter;
        this.counts = counts;
    }

    DistinctSpliterator(Spliterator<T> source, long atLeast) {
        this(source, atLeast, null, new HashMap<>());
    }

    void setCur(T t) {
        cur = t;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (nullCounter == null) {
            while (source.tryAdvance(this::setCur)) {
                if (counts.merge(cur, 1L, Long::sum) == atLeast) {
                    action.accept(cur);
                    return true;
                }
            }
        } else {
            while (source.tryAdvance(this::setCur)) {
                long count = cur == null ? nullCounter.incrementAndGet() : counts.merge(cur, 1L, Long::sum);
                if (count == atLeast) {
                    action.accept(cur);
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
            if(!source.hasCharacteristics(NONNULL)) {
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
