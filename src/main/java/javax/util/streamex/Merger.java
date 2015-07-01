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

import java.util.function.BinaryOperator;

import static javax.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 *
 */
class Merger<T> {
    private final Sink<T> left;
    private Sink<T> right;
    @SuppressWarnings({ "rawtypes" })
    private static Merger.Sink EMPTY = new Sink<>();
    
    private Merger(Sink<T> left, Sink<T> right) {
        this.left = left;
        this.right = right;
    }
    
    @SuppressWarnings("unchecked")
    static <T> Sink<T> empty() {
        return EMPTY;
    }
    
    static <T> Merger<T> normal() {
        Sink<T> left = new Sink<>();
        Sink<T> right = new Sink<>();
        Merger<T> merger = new Merger<>(left, right);
        left.m = merger;
        right.m = merger;
        return merger;
    }
    
    static class Sink<T> {
        Merger<T> m;
        private T payload = none();
        
        T push(T payload, BinaryOperator<T> fn) {
            assert this.payload == NONE;
            if(m == null)
                return none();
            synchronized(m) {
                this.payload = payload;
                return m.operate(fn);
            }
        }
        
        T connect(Sink<T> right, BinaryOperator<T> fn) {
            assert payload == NONE;
            if(right == null)
                return none();
            if(m == null) {
                if(right.m != null) {
                    right.m.clear();
                }
                return none();
            }
            assert m.right == this;
            if(right.m == null) {
                m.clear();
                return none();
            }
            assert right.m.left == right;
            synchronized (m) {
                synchronized (right.m) {
                    m.right = right.m.right;
                    m.right.m = m;
                    return m.operate(fn);
                }
            }
        }

        void clear() {
            m = null;
            payload = none();
        }
    }
    
    T operate(BinaryOperator<T> fn) {
        if(left.payload != NONE && right.payload != NONE) {
            T result = fn.apply(left.payload, right.payload);
            clear();
            return result;
        }
        return none();
    }

    void clear() {
        synchronized (this) {
            left.clear();
            right.clear();
        }
    }
    
    public Sink<T> getLeft() {
        return left;
    }
    
    public Sink<T> getRight() {
        return right;
    }
}
