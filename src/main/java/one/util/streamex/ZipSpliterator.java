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
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static one.util.streamex.UnknownSizeSpliterator.*;
import static one.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
/* package */class ZipSpliterator<U, V, R> implements Spliterator<R> {
    private Spliterator<U> left;
    private Spliterator<V> right;
    private final BiFunction<? super U, ? super V, ? extends R> mapper;
    private boolean trySplit;
    private int batch = 0;
    private final Box<U> l = new Box<>();
    private final Box<V> r = new Box<>();

    ZipSpliterator(Spliterator<U> left, Spliterator<V> right, BiFunction<? super U, ? super V, ? extends R> mapper, boolean trySplit) {
        this.left = left;
        this.right = right;
        this.mapper = mapper;
        this.trySplit = trySplit;
    }

    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        if (left.tryAdvance(l) && right.tryAdvance(r)) {
            action.accept(mapper.apply(l.a, r.a));
            return true;
        }
        return false;
    }

    @Override
    public void forEachRemaining(Consumer<? super R> action) {
        if(!hasCharacteristics(SIZED)) {
            Spliterator.super.forEachRemaining(action);
            return;
        }
        long leftSize = left.getExactSizeIfKnown();
        long rightSize = right.getExactSizeIfKnown();
        if(leftSize <= rightSize) {
            left.forEachRemaining(u -> {
                if(right.tryAdvance(r)) {
                    action.accept(mapper.apply(u, r.a));
                }
            });
        } else {
            right.forEachRemaining(v -> {
                if(left.tryAdvance(l)) {
                    action.accept(mapper.apply(l.a, v));
                }
            });
        }
    }

    @Override
    public Spliterator<R> trySplit() {
        if(trySplit && hasCharacteristics(SIZED | SUBSIZED))
        {
            Spliterator<U> leftPrefix = left.trySplit();
            if(leftPrefix == null)
                return arraySplit();
            Spliterator<V> rightPrefix = right.trySplit();
            if(rightPrefix == null)
            {
                left = new TailConcatSpliterator<>(leftPrefix, left);
                return arraySplit();
            }
            long leftSize = leftPrefix.getExactSizeIfKnown();
            long rightSize = rightPrefix.getExactSizeIfKnown();
            if(leftSize >= 0 && rightSize >= 0)
            {
                if(leftSize == rightSize)
                {
                    return new ZipSpliterator<>(leftPrefix, rightPrefix, mapper, true);
                }
                if(Math.abs(leftSize-rightSize) < Math.min(BATCH_UNIT, Math.max(leftSize, rightSize)/8))
                {
                    if(leftSize < rightSize)
                    {
                        @SuppressWarnings("unchecked")
                        U[] array = (U[]) new Object[(int) (rightSize-leftSize)];
                        leftSize += drainTo(array, left);
                        leftPrefix = new TailConcatSpliterator<>(leftPrefix, Spliterators.spliterator(array, characteristics()));
                    } else
                    {
                        @SuppressWarnings("unchecked")
                        V[] array = (V[]) new Object[(int) (leftSize-rightSize)];
                        rightSize += drainTo(array, right);
                        rightPrefix = new TailConcatSpliterator<>(rightPrefix, Spliterators.spliterator(array, characteristics()));
                    }
                    this.trySplit = false;
                    return new ZipSpliterator<>(leftPrefix, rightPrefix, mapper, false);
                }
            }
            left = new TailConcatSpliterator<>(leftPrefix, left);
            right = new TailConcatSpliterator<>(rightPrefix, right);
        }
        return arraySplit();
    }
    
    private Spliterator<R> arraySplit() {
        long s = estimateSize();
        if (s <= 1) return null;
        int n = batch + BATCH_UNIT;
        if (n > s)
            n = (int) s;
        if (n > MAX_BATCH)
            n = MAX_BATCH;
        @SuppressWarnings("unchecked")
        R[] array = (R[]) new Object[n];
        int index = drainTo(array, this);
        if((batch = index) == 0)
            return null;
        long s2 = estimateSize();
        USOfRef<R> prefix = new UnknownSizeSpliterator.USOfRef<>(array, 0, index);
        if(hasCharacteristics(SUBSIZED))
            prefix.est = index;
        else if(s == s2)
            prefix.est = Math.max(index, s/2);
        else
            prefix.est = Math.max(index, s2 - s);
        return prefix;
    }

    @Override
    public long estimateSize() {
        return Math.min(left.estimateSize(), right.estimateSize());
    }

    @Override
    public int characteristics() {
        // Remove SORTED, NONNULL, DISTINCT
        return left.characteristics() & right.characteristics() & (SIZED | SUBSIZED | ORDERED | IMMUTABLE | CONCURRENT);
    }
}
