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
import java.util.function.LongConsumer;

/**
 * @author Tagir Valeev
 *
 */
/* package */ abstract class ConstantSpliterator<T, S extends ConstantSpliterator<T, ?>> implements Spliterator<T>, Cloneable {
    long remaining;
    
    public ConstantSpliterator(long remaining) {
        this.remaining = remaining;
    }

    @SuppressWarnings("unchecked")
    @Override
    public S trySplit() {
        long remaining = this.remaining;
        if (remaining >= 2) {
            S clone;
            try {
                clone = (S) clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError();
            }
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
        return SIZED | SUBSIZED | IMMUTABLE;
    }
    
    static final class ConstRef<T> extends ConstantSpliterator<T, ConstRef<T>> {
        private final T value;
        
        ConstRef(T value, long count) {
            super(count);
            this.value = value;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if(remaining <= 0)
                return false;
            action.accept(value);
            remaining--;
            return true;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            for(long r = remaining; r > 0; r--) {
                action.accept(value);
            }
            remaining = 0;
        }
    }
    
    static final class ConstInt extends ConstantSpliterator<Integer, ConstInt> implements Spliterator.OfInt {
        private final int value;
        
        ConstInt(int value, long count) {
            super(count);
            this.value = value;
        }
        
        @Override
        public boolean tryAdvance(IntConsumer action) {
            if(remaining <= 0)
                return false;
            action.accept(value);
            remaining--;
            return true;
        }
        
        @Override
        public void forEachRemaining(IntConsumer action) {
            for(long r = remaining; r > 0; r--) {
                action.accept(value);
            }
            remaining = 0;
        }
    }

    static final class ConstLong extends ConstantSpliterator<Long, ConstLong> implements Spliterator.OfLong {
        private final long value;
        
        ConstLong(long value, long count) {
            super(count);
            this.value = value;
        }
        
        @Override
        public boolean tryAdvance(LongConsumer action) {
            if(remaining <= 0)
                return false;
            action.accept(value);
            remaining--;
            return true;
        }
        
        @Override
        public void forEachRemaining(LongConsumer action) {
            for(long r = remaining; r > 0; r--) {
                action.accept(value);
            }
            remaining = 0;
        }
    }

    static final class ConstDouble extends ConstantSpliterator<Double, ConstDouble> implements Spliterator.OfDouble {
        private final double value;
        
        ConstDouble(double value, long count) {
            super(count);
            this.value = value;
        }
        
        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if(remaining <= 0)
                return false;
            action.accept(value);
            remaining--;
            return true;
        }
        
        @Override
        public void forEachRemaining(DoubleConsumer action) {
            for(long r = remaining; r > 0; r--) {
                action.accept(value);
            }
            remaining = 0;
        }
    }
}
