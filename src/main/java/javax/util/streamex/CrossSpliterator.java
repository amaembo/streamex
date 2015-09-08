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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Tagir Valeev
 */
/* package */class CrossSpliterator<T, R> implements Spliterator<R> {
    private final Function<List<T>, R> mapper;
    private long est;
    private final int splitPos;
    private final Spliterator<T>[] spliterators;
    private final Collection<T>[] collections;
    private List<T> elements;

    boolean advance(int i) {
        if (spliterators[i] == null) {
            if (i > 0 && collections[i-1] != null && !advance(i-1))
                return false;
            spliterators[i] = collections[i].spliterator();
        }
        Consumer<? super T> action = t -> elements.set(i, t);
        if (!spliterators[i].tryAdvance(action)) {
            if (i == 0 || collections[i-1] == null || !advance(i-1))
                return false;
            spliterators[i] = collections[i].spliterator();
            if (!spliterators[i].tryAdvance(action))
                return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    CrossSpliterator(Collection<? extends Collection<T>> source, Function<List<T>, R> mapper) {
        this.mapper = mapper;
        this.splitPos = 0;
        long est = 1;
        try {
            for (Collection<T> c : source) {
                est = StrictMath.multiplyExact(est, c.size());
            }
        } catch (ArithmeticException e) {
            est = Long.MAX_VALUE;
        }
        this.est = est;
        this.collections = source.toArray(new Collection[source.size()]);
        this.spliterators = new Spliterator[collections.length];
        this.elements = (List<T>) Arrays.asList(new Object[collections.length]);
    }

    private CrossSpliterator(Function<List<T>, R> mapper, long est,
            int splitPos, Spliterator<T>[] spliterators, Collection<T>[] collections, List<T> elements) {
        this.mapper = mapper;
        this.est = est;
        this.splitPos = splitPos;
        this.spliterators = spliterators;
        this.collections = collections;
        this.elements = elements;
    }

    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        if (elements == null)
            return false;
        if (est < Long.MAX_VALUE && est > 0)
            est--;
        if (advance(collections.length-1)) {
            action.accept(mapper.apply(elements));
            return true;
        }
        elements = null;
        est = 0;
        return false;
    }

    @Override
    public Spliterator<R> trySplit() {
        //Entry<T> entry = entries.get(splitPos);
        if (spliterators[splitPos] == null)
            spliterators[splitPos] = collections[splitPos].spliterator();
        Spliterator<T> res = spliterators[splitPos].trySplit();
        if (res == null) {
            // TODO: support further split
            return null;
        }
        long prefixEst = Long.MAX_VALUE;
        long newEst = Long.MAX_VALUE;
        if (est < Long.MAX_VALUE) {
            newEst = spliterators[splitPos].getExactSizeIfKnown();
            try {
                for (int i = splitPos + 1; i < collections.length; i++) {
                    newEst = StrictMath.multiplyExact(newEst, collections[i].size());
                }
                prefixEst = est - newEst;
            } catch (ArithmeticException e) {
                newEst = Long.MAX_VALUE;
            }
        }
        Spliterator<T>[] prefixSpliterators = spliterators.clone(); 
        Collection<T>[] prefixCollections = collections.clone();
        @SuppressWarnings("unchecked")
        List<T> prefixElements = (List<T>) Arrays.asList(elements.toArray());
        prefixSpliterators[splitPos] = res;
        Arrays.fill(spliterators, splitPos+1, spliterators.length, null);
        this.est = newEst;
        return new CrossSpliterator<>(mapper, prefixEst, splitPos, prefixSpliterators, prefixCollections, prefixElements);
    }

    @Override
    public long estimateSize() {
        return est;
    }

    @Override
    public int characteristics() {
        int sized = est < Long.MAX_VALUE ? SIZED : 0;
        return ORDERED | sized;
    }
}
