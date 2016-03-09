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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

/**
 * @author Tagir Valeev
 */
/* package */abstract class CrossSpliterator<T, A> implements Spliterator<A> {
    long est;
    int splitPos;
    final Spliterator<T>[] spliterators;
    final Collection<T>[] collections;

    @SuppressWarnings("unchecked")
    CrossSpliterator(Collection<? extends Collection<T>> source) {
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
        this.collections = source.toArray(new Collection[0]);
        this.spliterators = new Spliterator[collections.length];
    }

    abstract Spliterator<A> doSplit(long prefixEst, Spliterator<T>[] prefixSpliterators,
            Collection<T>[] prefixCollections);

    abstract void accumulate(int pos, T t);

    CrossSpliterator(long est, int splitPos, Spliterator<T>[] spliterators, Collection<T>[] collections) {
        this.est = est;
        this.splitPos = splitPos;
        this.spliterators = spliterators;
        this.collections = collections;
    }

    static final class Reducing<T, A> extends CrossSpliterator<T, A> {
        private A[] elements;
        private final BiFunction<A, ? super T, A> accumulator;

        @SuppressWarnings("unchecked")
        Reducing(Collection<? extends Collection<T>> source, A identity, BiFunction<A, ? super T, A> accumulator) {
            super(source);
            this.accumulator = accumulator;
            this.elements = (A[]) new Object[collections.length + 1];
            this.elements[0] = identity;
        }

        private Reducing(long est, int splitPos, BiFunction<A, ? super T, A> accumulator,
                Spliterator<T>[] spliterators, Collection<T>[] collections, A[] elements) {
            super(est, splitPos, spliterators, collections);
            this.accumulator = accumulator;
            this.elements = elements;
        }

        @Override
        public boolean tryAdvance(Consumer<? super A> action) {
            if (elements == null)
                return false;
            if (est < Long.MAX_VALUE && est > 0)
                est--;
            int l = collections.length;
            if (advance(l - 1)) {
                action.accept(elements[l]);
                return true;
            }
            elements = null;
            est = 0;
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super A> action) {
            if (elements == null)
                return;
            int l = collections.length;
            A[] e = elements;
            while (advance(l - 1)) {
                action.accept(e[l]);
            }
            elements = null;
            est = 0;
        }

        @Override
        Spliterator<A> doSplit(long prefixEst, Spliterator<T>[] prefixSpliterators, Collection<T>[] prefixCollections) {
            return new Reducing<>(prefixEst, splitPos, accumulator, prefixSpliterators, prefixCollections, elements
                    .clone());
        }

        @Override
        void accumulate(int pos, T t) {
            elements[pos + 1] = accumulator.apply(elements[pos], t);
        }
    }

    static final class ToList<T> extends CrossSpliterator<T, List<T>> {
        private List<T> elements;

        @SuppressWarnings("unchecked")
        ToList(Collection<? extends Collection<T>> source) {
            super(source);
            this.elements = (List<T>) Arrays.asList(new Object[collections.length]);
        }

        private ToList(long est, int splitPos, Spliterator<T>[] spliterators, Collection<T>[] collections,
                List<T> elements) {
            super(est, splitPos, spliterators, collections);
            this.elements = elements;
        }

        @Override
        public boolean tryAdvance(Consumer<? super List<T>> action) {
            if (elements == null)
                return false;
            if (est < Long.MAX_VALUE && est > 0)
                est--;
            if (advance(collections.length - 1)) {
                action.accept(new ArrayList<>(elements));
                return true;
            }
            elements = null;
            est = 0;
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super List<T>> action) {
            if (elements == null)
                return;
            List<T> e = elements;
            int l = collections.length - 1;
            while (advance(l)) {
                action.accept(new ArrayList<>(e));
            }
            elements = null;
            est = 0;
        }

        @Override
        Spliterator<List<T>> doSplit(long prefixEst, Spliterator<T>[] prefixSpliterators,
                Collection<T>[] prefixCollections) {
            @SuppressWarnings("unchecked")
            List<T> prefixElements = (List<T>) Arrays.asList(elements.toArray());
            return new ToList<>(prefixEst, splitPos, prefixSpliterators, prefixCollections, prefixElements);
        }

        @Override
        void accumulate(int pos, T t) {
            elements.set(pos, t);
        }
    }

    boolean advance(int i) {
        if (spliterators[i] == null) {
            if (i > 0 && collections[i - 1] != null && !advance(i - 1))
                return false;
            spliterators[i] = collections[i].spliterator();
        }
        Consumer<? super T> action = t -> accumulate(i, t);
        if (!spliterators[i].tryAdvance(action)) {
            if (i == 0 || collections[i - 1] == null || !advance(i - 1))
                return false;
            spliterators[i] = collections[i].spliterator();
            if (!spliterators[i].tryAdvance(action))
                return false;
        }
        return true;
    }

    @Override
    public Spliterator<A> trySplit() {
        if (spliterators[splitPos] == null)
            spliterators[splitPos] = collections[splitPos].spliterator();
        Spliterator<T> res = spliterators[splitPos].trySplit();
        if (res == null) {
            if (splitPos == spliterators.length - 1)
                return null;
            @SuppressWarnings("unchecked")
            T[] arr = (T[]) StreamSupport.stream(spliterators[splitPos], false).toArray();
            if (arr.length == 0)
                return null;
            if (arr.length == 1) {
                accumulate(splitPos, arr[0]);
                splitPos++;
                return trySplit();
            }
            spliterators[splitPos] = Spliterators.spliterator(arr, Spliterator.ORDERED);
            return trySplit();
        }
        long prefixEst = Long.MAX_VALUE;
        long newEst = spliterators[splitPos].getExactSizeIfKnown();
        if (newEst == -1) {
            newEst = Long.MAX_VALUE;
        } else {
            try {
                for (int i = splitPos + 1; i < collections.length; i++) {
                    newEst = StrictMath.multiplyExact(newEst, collections[i].size());
                }
                if (est != Long.MAX_VALUE)
                    prefixEst = est - newEst;
            } catch (ArithmeticException e) {
                newEst = Long.MAX_VALUE;
            }
        }
        Spliterator<T>[] prefixSpliterators = spliterators.clone();
        Collection<T>[] prefixCollections = collections.clone();
        prefixSpliterators[splitPos] = res;
        this.est = newEst;
        Arrays.fill(spliterators, splitPos + 1, spliterators.length, null);
        return doSplit(prefixEst, prefixSpliterators, prefixCollections);
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
