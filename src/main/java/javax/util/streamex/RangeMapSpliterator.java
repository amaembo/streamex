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
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongConsumer;

/**
 * @author Tagir Valeev
 */
/* package */abstract class RangeMapSpliterator<T> implements Spliterator<T> {
    int cur;
    int limit;

    public RangeMapSpliterator(int fromInclusive, int toExclusive) {
        this.cur = fromInclusive;
        this.limit = toExclusive;
    }

    @Override
    public long estimateSize() {
        return limit - cur;
    }

    @Override
    public int characteristics() {
        return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
    }

    static class RMOfRef<T> extends RangeMapSpliterator<T> {
        private final IntFunction<T> mapper;

        public RMOfRef(int fromInclusive, int toExclusive, IntFunction<T> mapper) {
            super(fromInclusive, toExclusive);
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            int c = cur;
            if (c < limit) {
                action.accept(mapper.apply(c));
                cur++;
                return true;
            }
            return false;
        }

        @Override
        public Spliterator<T> trySplit() {
            int size = limit - cur;
            if (size >= 2)
                return new RMOfRef<>(cur, cur = cur + size / 2, mapper);
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            int l = limit, c = cur;
            while (c < l) {
                action.accept(mapper.apply(c++));
            }
            cur = limit;
        }
    }
    
    static class RMOfInt extends RangeMapSpliterator<Integer> implements Spliterator.OfInt {
        private final IntUnaryOperator mapper;
        
        public RMOfInt(int fromInclusive, int toExclusive, IntUnaryOperator mapper) {
            super(fromInclusive, toExclusive);
            this.mapper = mapper;
        }
        
        @Override
        public boolean tryAdvance(IntConsumer action) {
            int c = cur;
            if (c < limit) {
                action.accept(mapper.applyAsInt(c));
                cur++;
                return true;
            }
            return false;
        }
        
        @Override
        public Spliterator.OfInt trySplit() {
            int size = limit - cur;
            if (size >= 2)
                return new RMOfInt(cur, cur = cur + size / 2, mapper);
            return null;
        }
        
        @Override
        public void forEachRemaining(IntConsumer action) {
            int l = limit, c = cur;
            while (c < l) {
                action.accept(mapper.applyAsInt(c++));
            }
            cur = limit;
        }
    }
    
    static class RMOfLong extends RangeMapSpliterator<Long> implements Spliterator.OfLong {
        private final IntToLongFunction mapper;
        
        public RMOfLong(int fromInclusive, int toExclusive, IntToLongFunction mapper) {
            super(fromInclusive, toExclusive);
            this.mapper = mapper;
        }
        
        @Override
        public boolean tryAdvance(LongConsumer action) {
            int c = cur;
            if (c < limit) {
                action.accept(mapper.applyAsLong(c));
                cur++;
                return true;
            }
            return false;
        }
        
        @Override
        public Spliterator.OfLong trySplit() {
            int size = limit - cur;
            if (size >= 2)
                return new RMOfLong(cur, cur = cur + size / 2, mapper);
            return null;
        }
        
        @Override
        public void forEachRemaining(LongConsumer action) {
            int l = limit, c = cur;
            while (c < l) {
                action.accept(mapper.applyAsLong(c++));
            }
            cur = limit;
        }
    }
    
    static class RMOfDouble extends RangeMapSpliterator<Double> implements Spliterator.OfDouble {
        private final IntToDoubleFunction mapper;
        
        public RMOfDouble(int fromInclusive, int toExclusive, IntToDoubleFunction mapper) {
            super(fromInclusive, toExclusive);
            this.mapper = mapper;
        }
        
        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            int c = cur;
            if (c < limit) {
                action.accept(mapper.applyAsDouble(c));
                cur++;
                return true;
            }
            return false;
        }
        
        @Override
        public Spliterator.OfDouble trySplit() {
            int size = limit - cur;
            if (size >= 2)
                return new RMOfDouble(cur, cur = cur + size / 2, mapper);
            return null;
        }
        
        @Override
        public void forEachRemaining(DoubleConsumer action) {
            int l = limit, c = cur;
            while (c < l) {
                action.accept(mapper.applyAsDouble(c++));
            }
            cur = limit;
        }
    }
}
