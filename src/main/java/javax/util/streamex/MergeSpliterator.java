package javax.util.streamex;

import java.util.Spliterator;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;

public class MergeSpliterator<T> implements Spliterator<T> {
    private Spliterator<T> source;
    private boolean hasLast;
    private boolean hasPrev;
    private T cur;
    private T last;
    private final BinaryOperator<T> merger;
    private final BiPredicate<T, T> mergeable;

    public MergeSpliterator(BiPredicate<T, T> mergeable, BinaryOperator<T> merger, Spliterator<T> source, T prev, boolean hasPrev, T last,
            boolean hasLast) {
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
        while(source.tryAdvance(this::setCur)) {
            if(mergeable.test(prev, cur)) {
                prev = merger.apply(prev, cur);
            } else {
                action.accept(prev);
                return true;
            }
        }
        if (!hasLast) {
            action.accept(prev);
            source = null;
            return true;
        }
        cur = last;
        hasLast = false;
        if(mergeable.test(prev, cur)) {
            prev = merger.apply(prev, cur);
            source = null;
        }
        action.accept(prev);
        return true;
    }

    @Override
    public Spliterator<T> trySplit() {
        Spliterator<T> prefix = source.trySplit();
        if(prefix == null)
            return null;
        T prev = cur;
        if(!source.tryAdvance(this::setCur)) {
            source = prefix;
            return null;
        }
        T last = cur;
        boolean oldHasPrev = hasPrev;
        while(source.tryAdvance(this::setCur)) {
            if(mergeable.test(last, cur)) {
                last = merger.apply(last, cur);
            } else {
                hasPrev = true;
                return new MergeSpliterator<>(mergeable, merger, prefix, prev, oldHasPrev, last, true);
            }
        }
        if(!hasLast) {
            source = prefix;
            cur = prev;
            this.last = last;
            hasLast = true;
            return null;
        }
        if(mergeable.test(last, this.last)) {
            source = prefix;
            cur = prev;
            this.last = merger.apply(last, this.last);
            return null;
        }
        hasPrev = true;
        cur = this.last;
        hasLast = false;
        return new MergeSpliterator<>(mergeable, merger, prefix, prev, oldHasPrev, last, true);
    }

    @Override
    public long estimateSize() {
        return source.estimateSize();
    }

    @Override
    public int characteristics() {
        return source.characteristics() & (CONCURRENT | IMMUTABLE | ORDERED);
    }

}
