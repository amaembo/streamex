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
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import one.util.streamex.StreamExInternals.CloneableSpliterator;

/**
 * @author Tagir Valeev
 *
 */
/* package */abstract class ConstSpliterator<T, S extends ConstSpliterator<T, ?>> extends CloneableSpliterator<T, S> {
    long remaining;
    private final boolean ordered;

    public ConstSpliterator(long remaining, boolean ordered) {
        this.remaining = remaining;
        this.ordered = ordered;
    }

    @Override
    public S trySplit() {
        long remaining = this.remaining;
        if (remaining >= 2) {
            S clone = doClone();
            remaining >>= 1;
            clone.remaining = remaining;
            this.remaining -= remaining;
            return clone;
        }
        return null;
    }

    @Override
    public long estimateSize() {
        return remaining;
    }

    @Override
    public int characteristics() {
        return SIZED | SUBSIZED | IMMUTABLE | (ordered ? ORDERED : 0);
    }

    static final class OfRef<T> extends ConstSpliterator<T, OfRef<T>> {
        private final T value;

        OfRef(T value, long count, boolean ordered) {
            super(count, ordered);
            this.value = value;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (remaining <= 0)
                return false;
            action.accept(value);
            remaining--;
            return true;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            for (long r = remaining; r > 0; r--) {
                action.accept(value);
            }
            remaining = 0;
        }
    }

    static final class OfInt extends ConstSpliterator<Integer, OfInt> implements Spliterator.OfInt {
        private final int value;

        OfInt(int value, long count, boolean ordered) {
            super(count, ordered);
            this.value = value;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (remaining <= 0)
                return false;
            action.accept(value);
            remaining--;
            return true;
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            for (long r = remaining; r > 0; r--) {
                action.accept(value);
            }
            remaining = 0;
        }
    }

    static final class OfLong extends ConstSpliterator<Long, OfLong> implements Spliterator.OfLong {
        private final long value;

        OfLong(long value, long count, boolean ordered) {
            super(count, ordered);
            this.value = value;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            if (remaining <= 0)
                return false;
            action.accept(value);
            remaining--;
            return true;
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            for (long r = remaining; r > 0; r--) {
                action.accept(value);
            }
            remaining = 0;
        }
    }

    static final class OfDouble extends ConstSpliterator<Double, OfDouble> implements Spliterator.OfDouble {
        private final double value;

        OfDouble(double value, long count, boolean ordered) {
            super(count, ordered);
            this.value = value;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if (remaining <= 0)
                return false;
            action.accept(value);
            remaining--;
            return true;
        }

        @Override
        public void forEachRemaining(DoubleConsumer action) {
            for (long r = remaining; r > 0; r--) {
                action.accept(value);
            }
            remaining = 0;
        }
    }
}
