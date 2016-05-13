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

import static one.util.streamex.StreamExInternals.*;

import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;

import one.util.streamex.StreamExInternals.CloneableSpliterator;

/**
 * @author Tagir Valeev
 */
/* package */ abstract class PrefixOps<T, S extends Spliterator<T>> extends CloneableSpliterator<T, PrefixOps<T, S>>{
    S source;
    AtomicReference<T> accRef;
    T acc = none();
    
    PrefixOps(S source) {
        this.source = source;
    }
    
    @Override
    public Spliterator<T> trySplit() {
        if(acc != NONE) {
            return null;
        }
        @SuppressWarnings("unchecked")
        S prefix = (S) source.trySplit();
        if(prefix == null) {
            return null;
        }
        if(accRef == null) {
            accRef = new AtomicReference<>(none());
        }
        PrefixOps<T, S> pref = doClone();
        pref.source = prefix;
        return pref;
    }

    @Override
    public long estimateSize() {
        return source.estimateSize();
    }

    @Override
    public int characteristics() {
        return source.characteristics() & (ORDERED | IMMUTABLE | CONCURRENT | SIZED | SUBSIZED);
    }

    static final class OfRef<T> extends AbstractSpliterator<T> implements Consumer<T> {
        private final BinaryOperator<T> op;
        private final Spliterator<T> source;
        private T acc = none();

        OfRef(Spliterator<T> source, BinaryOperator<T> op) {
            super(source.estimateSize(), source.characteristics() & (ORDERED | IMMUTABLE | CONCURRENT | SIZED));
            this.source = source;
            this.op = op;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if(!source.tryAdvance(this))
                return false;
            action.accept(acc);
            return true;
        }
        
        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            source.forEachRemaining(next -> {
                this.accept(next);
                action.accept(acc);
            });
        }

        @Override
        public void accept(T next) {
            acc = acc == NONE ? next : op.apply(acc, next);
        }
    }
    
    static final class OfUnordRef<T> extends PrefixOps<T, Spliterator<T>> {
        private final BinaryOperator<T> op;

        OfUnordRef(Spliterator<T> source, BinaryOperator<T> op) {
            super(source);
            this.op = (a, b) -> a == NONE ? b : op.apply(a, b);
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            Box<T> box = new Box<>();
            if(!source.tryAdvance(box)) {
                return false;
            }
            if(accRef == null) {
                action.accept(acc = op.apply(acc, box.a));
            } else {
                action.accept(accRef.accumulateAndGet(box.a, op));
            }
            return true;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            if(accRef == null) {
                source.forEachRemaining(next -> action.accept(acc = op.apply(acc, next)));
            } else {
                source.forEachRemaining(next -> action.accept(accRef.accumulateAndGet(next, op)));
            }
        }
    }
    
    static final class OfUnordRef3<T> extends PrefixOps<T, Spliterator<T>> {
        private static final int BUF_SIZE = 128;
        
        private final BinaryOperator<T> op;
        private final BinaryOperator<T> localOp;
        private final T[] buf;
        private int idx = 0;
        
        @SuppressWarnings("unchecked")
        OfUnordRef3(Spliterator<T> source, BinaryOperator<T> op) {
            super(source);
            this.buf = (T[]) new Object[BUF_SIZE];
            this.op = (a, b) -> a == NONE ? b : op.apply(a, b);
            this.localOp = op;
        }
        
        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            Box<T> box = new Box<>();
            if(!source.tryAdvance(box)) {
                return false;
            }
            if(accRef == null) {
                action.accept(acc = op.apply(acc, box.a));
            } else {
                action.accept(accRef.accumulateAndGet(box.a, op));
            }
            return true;
        }
        
        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            if(accRef == null) {
                source.forEachRemaining(next -> action.accept(acc = op.apply(acc, next)));
            } else {
                source.forEachRemaining(next -> {
                    if(idx == 0) {
                        buf[idx++] = next;
                    } else {
                        T prev = buf[idx-1];
                        buf[idx++] = localOp.apply(prev, next);
                        if(idx == buf.length) {
                            drain(action);
                            idx = 0;
                        }
                    }
                });
                if(idx > 0)
                    drain(action);
            }
        }

        private void drain(Consumer<? super T> action) {
            T last = buf[idx-1];
            T acc = accRef.getAndAccumulate(last, op);
            if(acc != NONE) {
                for(int i=0; i<idx; i++) {
                    action.accept(localOp.apply(buf[i], acc));
                }
            } else {
                for(int i=0; i<idx; i++) {
                    action.accept(buf[i]);
                }
            }
        }
    }
}
