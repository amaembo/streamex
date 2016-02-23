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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collector;

/**
 * A container to keep limit least elements according to given comparator
 * 
 * First stage (less than limit elements): fill the pq sequentially until limit
 * elements is created.
 * 
 * Second stage: bootstrap. First fill series as a circular buffer until new
 * element cannot be appended or prepended to it, then drain circular buffer
 * elements to the pq (replacing greatest current element with the new one) and
 * start circular buffer again.
 * 
 * Finally, sort the pq array respecting the insertion order (which is tracked
 * in order array).
 * 
 * @author Tagir Valeev
 *
 * @param <T>
 */
/* package */class Limiter<T> extends AbstractCollection<T> {
    private final T[] pq;
    private final T[] series;
    private final int[] order;
    private final Comparator<? super T> comparator;
    private int size, maxOrder, head, tail;

    @SuppressWarnings("unchecked")
    public Limiter(int limit, Comparator<? super T> comparator) {
        // limit >= 2
        pq = (T[]) new Object[limit];
        series = (T[]) new Object[limit];
        order = new int[limit];
        this.comparator = comparator;
    }

    public Limiter<T> putAll(Limiter<T> other) {
        other.sort();
        for (int i = 0; i < other.size; i++) {
            if (!put(other.pq[i]))
                break;
        }
        return this;
    }

    public boolean put(T t) {
        int limit = pq.length;
        if (size < limit) {
            pq[size] = t;
            if (++size == limit) {
                Arrays.sort(pq, comparator);
                Collections.reverse(Arrays.asList(pq));
                for (int j = 0; j < limit; j++) {
                    order[j] = limit - j - 1;
                }
            }
            return true;
        }
        if (comparator.compare(t, pq[0]) >= 0)
            return false;
        if (maxOrder == 0) {
            maxOrder = limit;
            series[0] = t;
            return true;
        }
        if (head == tail) { // one element in the series
            if (comparator.compare(t, series[head]) >= 0) {
                if (++tail == limit)
                    tail = 0;
                series[tail] = t;
            } else {
                if (--head == -1)
                    head = limit - 1;
                series[head] = t;
            }
        } else {
            if (comparator.compare(t, series[tail]) >= 0) {
                // append to the series or ignore new element
                if (tail + 1 == head || head == 0 && tail == limit - 1)
                    return false;
                if (++tail == limit)
                    tail = 0;
                series[tail] = t;
            } else if (comparator.compare(t, series[head]) < 0) {
                // prepend to the series possibly removing the biggest element
                if (--head == -1)
                    head = limit - 1;
                series[head] = t;
                if (tail == head && --tail == -1) {
                    tail = limit - 1;
                }
            } else {
                drain();
                series[head = tail = 0] = t;
            }
        }
        return true;
    }

    private void drain() {
        int i = head;
        while (true) {
            if (!putPQ(series[i]) || i == tail)
                break;
            if (++i == series.length)
                i = 0;
        }
    }

    private boolean putPQ(T t) {
        // size == limit == queue.length here
        int limit = size;
        if (comparator.compare(pq[0], t) <= 0)
            return false;
        // sift-down
        int mid = limit >>> 1, cmp = 0, k = 0;
        while (k < mid) {
            int child = (k << 1) + 1;
            T c = pq[child];
            int oc = order[child];
            int right = child + 1;
            if (right < limit && ((cmp = comparator.compare(c, pq[right])) < 0 || cmp == 0 && oc < order[right])) {
                c = pq[child = right];
                oc = order[child];
            }
            if (comparator.compare(t, c) >= 0)
                break;
            pq[k] = c;
            order[k] = oc;
            k = child;
        }
        pq[k] = t;
        order[k] = maxOrder++;
        return true;
    }

    public void sort() {
        int limit = pq.length;
        if (size < limit)
            Arrays.sort(pq, 0, size, comparator);
        else {
            if (maxOrder == 0) {
                Collections.reverse(Arrays.asList(pq));
                return;
            }
            drain();
            // Respect order also
            quickSort(0, limit - 1);
        }
    }

    private void quickSort(int lowerIndex, int higherIndex) {
        int i = lowerIndex;
        int j = higherIndex;
        int mid = lowerIndex + (higherIndex - lowerIndex) / 2;
        T pivot = pq[mid];
        int pivotOrder = order[mid];
        int cmp;
        while (i <= j) {
            while ((cmp = comparator.compare(pq[i], pivot)) < 0 || cmp == 0 && order[i] < pivotOrder) {
                i++;
            }
            while ((cmp = comparator.compare(pq[j], pivot)) > 0 || cmp == 0 && order[j] > pivotOrder) {
                j--;
            }
            if (i <= j) {
                exchange(i, j);
                i++;
                j--;
            }
        }
        if (lowerIndex < j)
            quickSort(lowerIndex, j);
        if (i < higherIndex)
            quickSort(i, higherIndex);
    }

    private void exchange(int i, int j) {
        int temp = order[i];
        order[i] = order[j];
        order[j] = temp;
        T t = pq[i];
        pq[i] = pq[j];
        pq[j] = t;
    }

    public static <T> Collector<T, ?, List<T>> least(int limit, Comparator<? super T> comp) {
        return Collector.<T, Limiter<T>, List<T>> of(() -> new Limiter<>(limit, comp), Limiter::add, (pq1, pq2) -> {
            pq1.addAll(pq2);
            return pq1;
        }, pq -> {
            pq.sort();
            return pq.size == limit ? Arrays.asList(pq.pq) : Arrays.asList(pq.pq).subList(0, pq.size);
        });
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.asList(pq).subList(0, size).iterator();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Object[] toArray() {
        if (size == pq.length)
            return pq;
        return Arrays.copyOfRange(pq, 0, size);
    }
}
