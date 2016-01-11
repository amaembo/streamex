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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * @author Tagir Valeev
 */
/*package*/ final class WithFirstSpliterator<T> implements Spliterator<Map.Entry<T, T>> {
    private final Spliterator<T> source;
    private T first;
    private boolean initialized;
    
    WithFirstSpliterator(Spliterator<T> source) {
        this.source = source;
    }
    
    private WithFirstSpliterator(Spliterator<T> source, T first) {
        this.source = source;
        this.first = first;
        this.initialized = true;
    }
    
    @Override
    public boolean tryAdvance(Consumer<? super Entry<T, T>> action) {
        if(!init())
            return false;
        return source.tryAdvance(x -> action.accept(new AbstractMap.SimpleImmutableEntry<>(first, x)));
    }

    private boolean init() {
        if(initialized)
            return true;
        initialized = true;
        return source.tryAdvance(x -> first = x);
    }

    @Override
    public void forEachRemaining(Consumer<? super Entry<T, T>> action) {
        source.forEachRemaining(x -> {
            if(!initialized) {
                first = x;
                initialized = true;
            } else {
                action.accept(new AbstractMap.SimpleImmutableEntry<>(first, x));
            }
        });
    }

    @Override
    public Spliterator<Entry<T, T>> trySplit() {
        if(!init())
            return null;
        Spliterator<T> prefix = source.trySplit();
        if(prefix == null)
            return null;
        return new WithFirstSpliterator<>(prefix, first);
    }

    @Override
    public long estimateSize() {
        long size = source.estimateSize();
        return size == Long.MAX_VALUE || size <= 0 ? size : size - 1;
    }

    @Override
    public int characteristics() {
        return NONNULL | (source.characteristics() & (DISTINCT | SIZED | SUBSIZED | IMMUTABLE | CONCURRENT | ORDERED));
    }
}
