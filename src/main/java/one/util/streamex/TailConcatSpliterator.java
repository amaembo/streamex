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

import one.util.streamex.StreamExInternals.TailSpliterator;

/**
 * @author Tagir Valeev
 *
 * @param <T> type of the elements
 */
/* package */class TailConcatSpliterator<T> implements TailSpliterator<T> {
    private Spliterator<T> left, right;
    private int characteristics;
    private long size;

    @SuppressWarnings("unchecked")
    public TailConcatSpliterator(Spliterator<? extends T> left, Spliterator<? extends T> right) {
        this.left = (Spliterator<T>) left;
        this.right = (Spliterator<T>) right;
        this.characteristics = left.characteristics() & right.characteristics() & (ORDERED | SIZED | SUBSIZED);
        this.size = left.estimateSize() + right.estimateSize();
        if (this.size < 0) {
            this.size = Long.MAX_VALUE;
            this.characteristics &= (~SIZED) & (~SUBSIZED);
        }
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (left != null) {
            if (left.tryAdvance(action)) {
                if (size > 0 && size != Long.MAX_VALUE)
                    size--;
                return true;
            }
            left = null;
        }
        if(right != null)
            right = TailSpliterator.tryAdvanceWithTail(right, action);
        return right != null;
    }

    @Override
    public Spliterator<T> tryAdvanceOrTail(Consumer<? super T> action) {
        if (left == null || !left.tryAdvance(action)) {
            Spliterator<T> s = right;
            right = null;
            return s;
        }
        if (size > 0 && size != Long.MAX_VALUE)
            size--;
        return this;
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        if (left != null)
            left.forEachRemaining(action);
        if(right != null)
            TailSpliterator.forEachWithTail(right, action);
    }

    @Override
    public Spliterator<T> forEachOrTail(Consumer<? super T> action) {
        if (left != null)
            left.forEachRemaining(action);
        Spliterator<T> s = right;
        right = null;
        return s;
    }

    @Override
    public Spliterator<T> trySplit() {
        if (left == null)
            return right.trySplit();
        Spliterator<T> s = left;
        left = null;
        return s;
    }

    @Override
    public long estimateSize() {
        if (left == null)
            return right == null ? 0 : right.estimateSize();
        return size;
    }

    @Override
    public int characteristics() {
        return characteristics;
    }
}