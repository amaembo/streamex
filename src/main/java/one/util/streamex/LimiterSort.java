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
 * @author Tagir Valeev
 */
/* package */ class LimiterSort<T> extends AbstractCollection<T> {
    private T[] data;
    private final int limit;
    private final Comparator<? super T> comparator;
    private int size;
    private boolean initial = true;
    
    @SuppressWarnings("unchecked")
    public LimiterSort(int limit, Comparator<? super T> comparator) {
        this.limit = limit;
        this.comparator = comparator;
        this.data = (T[]) new Object[Math.min(1000, limit)*2];
    }
    
    public boolean put(T t) {
        if(initial) {
            if(size == data.length) {
                if(size < limit * 2) {
                    @SuppressWarnings("unchecked")
                    T[] newData = (T[]) new Object[Math.min(limit, size)*2];
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
        if(size == data.length) {
            sortTail();
        }
        if(comparator.compare(t, data[limit-1]) < 0) {
            data[size++] = t;
            return true;
        }
        return false;
    }
    
    public LimiterSort<T> putAll(LimiterSort<T> lqs) {
        if(!lqs.initial) {
            for(int i=0; i<limit; i++) {
                if(!put(lqs.data[i])) break;
            }
            for(int i=limit; i<lqs.size; i++) {
                put(lqs.data[i]);
            }
        } else {
            for(int i=0; i<lqs.size; i++) {
                put(lqs.data[i]);
            }
        }
        return this;
    }
    
    private void sortTail() {
        // size > limit here
        T[] d = data;
        int l = limit, s = size;
        Comparator<? super T> cmp = comparator;
        Arrays.sort(d, l, s, cmp);
        if(cmp.compare(d[s-1], d[0]) < 0) {
            // Common case: descending sequence
            // Assume size - limit <= limit here
            System.arraycopy(d, 0, d, s - l, 2 * l - s);
            System.arraycopy(d, l, d, 0, s - l);
        } else {
            // Merge presorted 0..limit-1 and limit..size-1
            @SuppressWarnings("unchecked")
            T[] buf = (T[]) new Object[l];
            int i = 0, j = l, k = 0;
            // d[l-1] is guaranteed to be the worst element, thus no need to check it
            while(i < l - 1 && k < l && j < s) {
                if(cmp.compare(d[i], d[j]) <= 0) {
                    buf[k++] = d[i++];
                } else {
                    buf[k++] = d[j++];
                }
            }
            if(k < l) {
                if(i < l - 1) {
                    System.arraycopy(d, i, d, k, l-k);
                } else { // j < s
                    System.arraycopy(d, j, d, k, l-k);
                }
            }
            System.arraycopy(buf, 0, d, 0, k);
        }
        size = l;
    }
    
    public void sort() {
        if(initial)
            Arrays.sort(data, 0, size, comparator);
        else if(size > limit)
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
