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
package one.util.streamex.issues;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;

import org.junit.Test;

/**
 * https://github.com/amaembo/streamex/issues/29
 * @author Tagir Valeev
 */
public class Issue0029 {
    static class T implements Comparable<T> {
        private final String name;
        private final T parent;

        public T(String name, T parent) {
            this.name = name;
            this.parent = parent;
        }

        public boolean isParent(T parent) {
            return this.parent == parent;
        }

        @Override
        public String toString() {
            return "T["+name+"]";
        }

        @Override
        public int compareTo(T o) {
            return name.compareTo(o.name);
        }
    }
    
    static class C {
        public static final boolean isNested(T a, T b) {
            return a.isParent(b) || b.isParent(a);
        }

        public static final T merge(T a, T b) {
            return a.isParent(b) ? a : b;
        }
    }
    
    private List<T> getTestData() {
        T t1 = new T("t1.1", null);
        T t11 = new T("t1", t1);
        T t2 = new T("t2.1", null);
        T t21 = new T("t2", t2);
        return Arrays.asList(t1, t2, t21, t11);
    }
    
    @Test
    public void testCollapse() {
        List<T> result = StreamEx.of(getTestData()).sorted().collapse(C::isNested, C::merge).toList();
        assertEquals("[T[t1], T[t2]]", result.toString());
    }
    
    @Test
    public void testPlain() {
        List<T> tmp = getTestData().stream().sorted().collect(Collectors.toList());
        Iterator<T> it = tmp.iterator();
        T curr, last;
        curr = last = null;
        while (it.hasNext()) {
            T oldLast = last;
            last = curr;
            curr = it.next();
            if (last != null && last.isParent(curr)) {
                it.remove();
                curr = last;
                last = oldLast;
            }
        }
        List<T> result = tmp.stream().collect(Collectors.toList());
        assertEquals("[T[t1], T[t2]]", result.toString());
    }
}
