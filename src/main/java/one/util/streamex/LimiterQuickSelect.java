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
/* package */ class LimiterQuickSelect<T> extends AbstractCollection<T> {
    private final T[] data;
    private final int limit;
    private final Comparator<? super T> comparator;
    private int size;
    private boolean initial = true;
    
    @SuppressWarnings("unchecked")
    public LimiterQuickSelect(int limit, Comparator<? super T> comparator) {
        this.limit = limit;
        this.comparator = comparator;
        this.data = (T[]) new Object[limit*2];
    }
    
    public void put(T t) {
        if(initial || comparator.compare(t, data[limit-1]) < 0) {
            if(size == data.length) {
                Arrays.sort(data, comparator);
                initial = false;
                size = limit;
                if(comparator.compare(t, data[limit-1]) < 0) {
                    data[size++] = t;
                }
            } else {
                data[size++] = t;
            }
        }
    }
    
    public LimiterQuickSelect<T> putAll(LimiterQuickSelect<T> lqs) {
        for(int i=0; i<lqs.size; i++) {
            put(lqs.data[i]);
        }
        return this;
    }
    
    public void sort() {
        if(initial || size > limit)
            Arrays.sort(data, 0, size, comparator);
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
