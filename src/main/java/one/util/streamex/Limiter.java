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

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Extracts least limit elements from the input sorting them according to the
 * given comparator. Works for 2 <= limit < Integer.MAX_VALUE/2. Uses
 * O(min(limit, inputSize)) additional memory.
 * 
 * @param <T> type of input elements
 * 
 * @author Tagir Valeev
 */
/* package */class Limiter<T> extends AbstractCollection<T> {
    private T[] data;
    private final int limit;
    private final Comparator<? super T> comparator;
    private int size;
    private boolean initial = true;

    @SuppressWarnings("unchecked")
    public Limiter(int limit, Comparator<? super T> comparator) {
        this.limit = limit;
        this.comparator = comparator;
        this.data = (T[]) new Object[Math.min(1000, limit) * 2];
    }

    /**
     * Accumulate new element
     * 
     * @param t element to accumulate
     * 
     * @return false if the element is definitely not included into result, so
     *         any bigger element could be skipped as well, or true if element
     *         will probably be included into result.
     */
    public boolean put(T t) {
        if (initial) {
            if (size == data.length) {
                if (size < limit * 2) {
                    @SuppressWarnings("unchecked")
                    T[] newData = (T[]) new Object[Math.min(limit, size) * 2];
                    System.arraycopy(data, 0, newData, 0, size);
                    data = newData;
                } else {
                    Arrays.sort(data, comparator);
                    initial = false;
                    size = limit;
                }
                put(t);
            } else {
                data[size++] = t;
            }
            return true;
        }
        if (size == data.length) {
            sortTail();
        }
        if (comparator.compare(t, data[limit - 1]) < 0) {
            data[size++] = t;
            return true;
        }
        return false;
    }

    /**
     * Merge other {@code Limiter} object into this (other object becomes unusable after that).
     * 
     * @param ls other object to merge
     * @return this object
     */
    public Limiter<T> putAll(Limiter<T> ls) {
        int i = 0;
        if (!ls.initial) {
            // sorted part
            for (; i < limit; i++) {
                if (!put(ls.data[i]))
                    break;
            }
            i = limit;
        }
        for (; i < ls.size; i++) {
            put(ls.data[i]);
        }
        return this;
    }

    private void sortTail() {
        // size > limit here
        T[] d = data;
        int l = limit, s = size;
        Comparator<? super T> cmp = comparator;
        Arrays.sort(d, l, s, cmp);
        if (cmp.compare(d[s - 1], d[0]) < 0) {
            // Common case: descending sequence
            // Assume size - limit <= limit here
            System.arraycopy(d, 0, d, s - l, 2 * l - s);
            System.arraycopy(d, l, d, 0, s - l);
        } else {
            // Merge presorted 0..limit-1 and limit..size-1
            @SuppressWarnings("unchecked")
            T[] buf = (T[]) new Object[l];
            int i = 0, j = l, k = 0;
            // d[l-1] is guaranteed to be the worst element, thus no need to
            // check it
            while (i < l - 1 && k < l && j < s) {
                if (cmp.compare(d[i], d[j]) <= 0) {
                    buf[k++] = d[i++];
                } else {
                    buf[k++] = d[j++];
                }
            }
            if (k < l) {
                System.arraycopy(d, i < l - 1 ? i : j, d, k, l - k);
            }
            System.arraycopy(buf, 0, d, 0, k);
        }
        size = l;
    }

    /**
     * Must be called after accumulation is finished. After calling
     * {@code sort()} this Limiter represents the resulting collection.
     */
    public void sort() {
        if (initial)
            Arrays.sort(data, 0, size, comparator);
        else if (size > limit)
            sortTail();
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOfRange(data, 0, size());
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.asList(data).subList(0, size()).iterator();
    }

    @Override
    public int size() {
        return initial && size < limit ? size : limit;
    }
}
