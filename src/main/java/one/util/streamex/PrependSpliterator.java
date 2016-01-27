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
/* package */class PrependSpliterator<T> implements TailSpliterator<T> {
    private Spliterator<T> source;
    private T element;
    private int mode;

    public PrependSpliterator(Spliterator<T> source, T element) {
        this.source = source;
        this.element = element;
        this.mode = source.estimateSize() < Long.MAX_VALUE-1 ? 1 : 2;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (mode == 0)
            return source.tryAdvance(action);
        action.accept(element);
        element = null;
        mode = 0;
        return true;
    }

    @Override
    public Spliterator<T> tryAdvanceOrTail(Consumer<? super T> action) {
        if (mode == 0) {
            Spliterator<T> s = source;
            source = null;
            return s;
        }
        action.accept(element);
        element = null;
        mode = 0;
        return this;
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        if (mode != 0)
            action.accept(element);
        element = null;
        mode = 0;
        source.forEachRemaining(action);
    }

    @Override
    public Spliterator<T> forEachOrTail(Consumer<? super T> action) {
        if (mode != 0) {
            action.accept(element);
        }
        Spliterator<T> s = source;
        element = null;
        mode = 0;
        source = null;
        return s;
    }

    @Override
    public Spliterator<T> trySplit() {
        if (mode == 0)
            return source.trySplit();
        mode = 0;
        return new ConstSpliterator.OfRef<>(element, 1, true);
    }

    @Override
    public long estimateSize() {
        long size = source.estimateSize();
        return mode == 0 || size == Long.MAX_VALUE ? size : size + 1;
    }

    @Override
    public int characteristics() {
        switch(mode) {
        case 1:
            return source.characteristics() & (ORDERED | SIZED | SUBSIZED);
        case 2:
            return source.characteristics() & ORDERED;
        default:
            return source.characteristics();
        }
    }
}