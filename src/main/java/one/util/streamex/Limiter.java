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
import java.util.List;
import java.util.stream.Collector;

/**
 * A container to keep n least elements according to given comparator
 * 
 * @author Tagir Valeev
 *
 * @param <T>
 */
/* package */ class Limiter<T> extends AbstractCollection<T> {
    private final T[] queue;
    private final T[] series;
    private final int[] order;
    private final Comparator<? super T> comparator;
    private int size, maxOrder, head, tail;

    @SuppressWarnings("unchecked")
    public Limiter(int limit, Comparator<? super T> comparator) {
        queue = (T[]) new Object[limit];
        series = (T[]) new Object[limit];
        order = new int[limit];
        this.comparator = comparator;
    }
    
    public void addAll(Limiter<T> other) {
        if(queue.length == 0)
            return;
        other.sort();
    	for(int i = 0; i<other.size; i++) {
    		if(!add(other.queue[i]))
    		    break;
    	}
    }

    @Override
    public boolean add(T e) {
        int limit = queue.length;
        if (limit < 2) {
            if (limit == 0 || size == 1 && comparator.compare(e, queue[0]) >= 0)
                return false;
            queue[0] = e;
            size = 1;
            return true;
        }
        if (maxOrder == 0) {
            maxOrder = 1;
            series[0] = e;
            return true;
        }
        if (size == limit && comparator.compare(e, queue[0]) >= 0)
            return false;
        if (head == tail) { // one element in the series
            if (comparator.compare(e, series[head]) >= 0) {
                if (++tail == limit)
                    tail = 0;
                series[tail] = e;
            } else {
                if (--head == -1)
                    head = limit - 1;
                series[head] = e;
            }
        } else {
            if (comparator.compare(e, series[tail]) >= 0) {
                if (tail + 1 == head || head == 0 && tail == limit - 1)
                    return false;
                if (++tail == limit)
                    tail = 0;
                series[tail] = e;
            } else if (comparator.compare(e, series[head]) < 0) {
                if (--head == -1)
                    head = limit - 1;
                series[head] = e;
                if (tail == head && --tail == -1) {
                    tail = limit - 1;
                }
            } else {
                drain();
                series[head = tail = 0] = e;
            }
        }
        return true;
    }

    private void drain() {
        int i = head;
        while (true) {
            if (!doAdd(series[i]) || i == tail)
                break;
            if (++i == series.length)
                i = 0;
        }
    }

    private boolean doAdd(T t) {
        int i = size;
        int limit = queue.length;
        if (i == limit) {
            if (comparator.compare(queue[0], t) <= 0)
                return false;
            // sift-down
            int mid = i >>> 1, cmp = 0, k = 0;
            while (k < mid) {
                int child = (k << 1) + 1;
                T c = queue[child];
                int oc = order[child];
                int right = child + 1;
                if (right < i && ((cmp = comparator.compare(c, queue[right])) < 0 || cmp == 0 && oc < order[right])) {
                    c = queue[child = right];
                    oc = order[child];
                }
                if (comparator.compare(t, c) >= 0)
                    break;
                queue[k] = c;
                order[k] = oc;
                k = child;
            }
            queue[k] = t;
            order[k] = maxOrder++;
        } else {
            queue[i] = t;
            if ((size = i + 1) == limit) {
                Arrays.sort(queue, comparator.reversed());
                for (int j = 0; j < limit; j++) {
                    order[j] = j + 1;
                }
                maxOrder = limit + 1;
            }
        }
        return true;
    }

    public void sort() {
        int limit = queue.length;
        if (limit < 2 || maxOrder == 0)
            return;
        drain();
        if(size < limit)
            Arrays.sort(queue, 0, size, comparator);
        else
            quickSort(0, limit - 1);
    }
    
    private void quickSort(int lowerIndex, int higherIndex) {
        int i = lowerIndex;
        int j = higherIndex;
        int mid = lowerIndex+(higherIndex-lowerIndex)/2;
        T pivot = queue[mid];
        int pivotOrder = order[mid];
        int cmp;
        while (i <= j) {
            while ((cmp = comparator.compare(queue[i], pivot)) < 0 || cmp == 0 && order[i] < pivotOrder) {
                i++;
            }
            while ((cmp = comparator.compare(queue[j], pivot)) > 0 || cmp == 0 && order[j] > pivotOrder) {
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
        T t = queue[i];
        queue[i] = queue[j];
        queue[j] = t;
    }
    
    public static <T> Collector<T, ?, List<T>> least(int limit, Comparator<? super T> comp) {
        return Collector.<T, Limiter<T>, List<T>>of(() -> new Limiter<>(limit, comp), Limiter::add, (pq1, pq2) -> {
            pq1.addAll(pq2);
            return pq1;
        }, pq -> {
            pq.sort();
            return pq.size == limit ? Arrays.asList(pq.queue) : Arrays.asList(pq.queue).subList(0, pq.size); 
        });
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.asList(queue).subList(0, size).iterator();
    }

    @Override
    public int size() {
        return size;
    }
    
    @Override
    public Object[] toArray() {
        if(size == queue.length)
            return queue;
        return Arrays.copyOfRange(queue, 0, size);
    }
}
