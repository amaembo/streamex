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
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static one.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
/* package */class ZipSpliterator<U, V, R> implements Spliterator<R> {
    private final Spliterator<U> left;
    private final Spliterator<V> right;
    private final BiFunction<? super U, ? super V, ? extends R> mapper;

    ZipSpliterator(Spliterator<U> left, Spliterator<V> right, BiFunction<? super U, ? super V, ? extends R> mapper) {
        this.left = left;
        this.right = right;
        this.mapper = mapper;
    }

    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        PairBox<U, V> box = new PairBox<>(null, null);
        if (left.tryAdvance(box::setA) && right.tryAdvance(box::setB)) {
            action.accept(mapper.apply(box.a, box.b));
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
            Box<V> box = new Box<>(null);
            left.forEachRemaining(u -> {
                if(right.tryAdvance(box::setA)) {
                    action.accept(mapper.apply(u, box.a));
                }
            });
        } else {
            Box<U> box = new Box<>(null);
            right.forEachRemaining(v -> {
                if(left.tryAdvance(box::setA)) {
                    action.accept(mapper.apply(box.a, v));
                }
            });
        }
    }

    @Override
    public Spliterator<R> trySplit() {
        // TODO Support array-based splitting
        // TODO Support SUBSIZED splitting if parts match exactly
        // TODO Support SUBSIZED splitting if parts differ a little bit
        return null;
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
