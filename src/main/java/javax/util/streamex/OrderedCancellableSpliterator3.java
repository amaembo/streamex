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

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static javax.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
/* package */final class OrderedCancellableSpliterator3<T, A> implements Spliterator<A>, Cloneable {
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<OrderedCancellableSpliterator3, OrderedCancellableSpliterator3> updater
         = AtomicReferenceFieldUpdater.newUpdater(OrderedCancellableSpliterator3.class, OrderedCancellableSpliterator3.class, "suffix");
    private Spliterator<T> source;
    private final ConcurrentSkipListMap<Key, A> map = new ConcurrentSkipListMap<>();
    private final BiConsumer<A, ? super T> accumulator;
    private final Predicate<A> cancelPredicate;
    private final BinaryOperator<A> combiner;
    private final Supplier<A> supplier;
    private Key key = Key.ROOT;
    private volatile boolean localCancelled;
    private OrderedCancellableSpliterator3<T, A> prefix;
    private volatile OrderedCancellableSpliterator3<T, A> suffix;
    
    private static class Key implements Comparable<Key> {
        final int length;
        final long bits1, bits2, bits3;
        
        static final int MAX_LENGTH = Long.SIZE*3;
        static final Key ROOT = new Key(null, false);
        
        private Key(Key parent, boolean left) {
            long b1 = 0, b2 = 0, b3 = 0;
            int l = 0;
            if(parent != null) {
                l = parent.length+1;
                b1 = parent.bits1;
                b2 = parent.bits2;
                b3 = parent.bits3;
                if(!left) {
                    if(l < Long.SIZE) {
                        b1 |= (1L << (Long.SIZE-l));
                    } else if(l < 2*Long.SIZE) {
                        b2 |= (1L << (2*Long.SIZE-l));
                    } else {
                        b3 |= (1L << (MAX_LENGTH-l));
                    }
                }
            }
            this.length = l;
            this.bits1 = b1;
            this.bits2 = b2;
            this.bits3 = b3;
        }
        
        Key left() {
            return new Key(this, true);
        }
        
        Key right() {
            return new Key(this, false);
        }
        
        @Override
        public int compareTo(Key o) {
            int res = Long.compareUnsigned(bits1, o.bits1);
            if(res == 0)
                res = Long.compareUnsigned(bits2, o.bits2);
            if(res == 0)
                res = Long.compareUnsigned(bits3, o.bits3);
            if(res == 0)
                res = Integer.compare(o.length, length);
            return res;
        }
        
        @Override
        public String toString() {
            return String.format("%64s%64s%64s", Long.toBinaryString(bits1), Long.toBinaryString(bits2), Long.toBinaryString(bits3)).substring(0, length).replace(' ', '0');
        }
    }
    
    static Collection<String> log = new ConcurrentLinkedQueue<>();

    OrderedCancellableSpliterator3(Spliterator<T> source, Supplier<A> supplier, BiConsumer<A, ? super T> accumulator,
            BinaryOperator<A> combiner, Predicate<A> cancelPredicate) {
        this.source = source;
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.combiner = combiner;
        this.cancelPredicate = cancelPredicate;
        this.map.put(key, none());
    }

    @Override
    public boolean tryAdvance(Consumer<? super A> action) {
        Spliterator<T> source = this.source;
        if (source == null || localCancelled) {
            this.source = null;
            return false;
        }
        //log.add(key+": start");
        A acc = supplier.get();
        try {
            source.forEachRemaining(t -> {
                accumulator.accept(acc, t);
                if (cancelPredicate.test(acc)) {
                    cancelSuffix();
                    throw new CancelException();
                }
                if (localCancelled) {
                    throw new CancelException();
                }
            });
        } catch (CancelException ex) {
            //log.add(key+"/"+acc+": cancelled");
            if (localCancelled) {
                return false;
            }
        }
        this.source = null;
        A result = acc;
        //log.add(key+"/"+acc+": combining start");
        Entry<Key, A> lowerEntry, higherEntry = null;
        while(true) {
            while(true) {
                lowerEntry = map.lowerEntry(key);
                if(lowerEntry == null || lowerEntry.getValue() == NONE)
                    break;
                if(!map.remove(lowerEntry.getKey(), lowerEntry.getValue()))
                    continue;
                //log.add(key+"/"+result+" with prefix "+lowerEntry);
                result = combiner.apply(lowerEntry.getValue(), result);
                if(cancelPredicate.test(result)) {
                    //log.add(key+"/"+result+": cancelsuffix1");
                    cancelSuffix();
                }
            }
            while(suffix != null) {
                higherEntry = map.higherEntry(key);
                if(higherEntry == null || higherEntry.getValue() == NONE)
                    break;
                if(!map.remove(higherEntry.getKey(), higherEntry.getValue()))
                    continue;
                //log.add(key+"/"+result+" with suffix "+higherEntry);
                result = combiner.apply(result, higherEntry.getValue());
                if(cancelPredicate.test(result)) {
                    //log.add(key+"/"+result+": cancelsuffix2");
                    cancelSuffix();
                }
            }
            if(lowerEntry == null && (higherEntry == null || suffix == null)) {
                //log.add(key+"/"+result+": accept!!!");
                //log.add(map.toString());
                action.accept(result);
                return true;
            }
//            if(lowerEntry == null)
//            {
//                log.add(key+"/"+result+": offer: "+suffix);
//                //log.add(map.keySet().toString());
//            }
            //log.add(key+"/"+result+": offer");
            map.put(key, result);
            if(lowerEntry != null) {
                lowerEntry = map.lowerEntry(key);
                if(lowerEntry != null && lowerEntry.getValue() != NONE) {
                    //log.add(key+": race on lower "+lowerEntry.getKey());
                    if(!map.replace(key, result, none())) {
                        //log.add(key+": other party took the responsibility; exiting");
                        return false;
                    }
                    //log.add(key+"/"+result+": continue");
                    continue;
                }
            }
            if(higherEntry != null && suffix != null) {
                higherEntry = map.higherEntry(key);
                if(higherEntry != null && higherEntry.getValue() != NONE) {
                    //log.add(key+": race on higher "+higherEntry.getKey());
                    if(!map.replace(key, result, none())) {
                        //log.add(key+": other party took the responsibility; exiting");
                        return false;
                    }
                    //log.add(key+"/"+result+": continue");
                    continue;
                }
            }
            return false;
        }
    }

    private void cancelSuffix() {
        //log.add(key+": cancelling suffix");
        if (this.suffix == null)
            return;
        OrderedCancellableSpliterator3<T, A> suffix = this.suffix;
        while (suffix != null && !suffix.localCancelled) {
            suffix.localCancelled = true;
            OrderedCancellableSpliterator3<T, A> next = suffix.suffix;
            suffix.suffix = null;
            suffix = next;
        }
        this.suffix = null;
    }

    @Override
    public void forEachRemaining(Consumer<? super A> action) {
        tryAdvance(action);
    }

    @Override
    public Spliterator<A> trySplit() {
        if (localCancelled) {
            source = null;
            return null;
        }
        if(key.length == Key.MAX_LENGTH || source == null) {
            return null;
        }
        Spliterator<T> prefix = source.trySplit();
        if (prefix == null) {
            return null;
        }
        try {
            Key left = key.left();
            Key right = key.right();
            @SuppressWarnings("unchecked")
            OrderedCancellableSpliterator3<T, A> result = (OrderedCancellableSpliterator3<T, A>) this.clone();
            result.source = prefix;
            this.prefix = result;
            result.suffix = this;
            OrderedCancellableSpliterator3<T, A> prefixPrefix = result.prefix;
            if (prefixPrefix != null)
                updater.compareAndSet(prefixPrefix, this, result);
            if (this.localCancelled || result.localCancelled) {
                // we can end up here due to the race with suffix updates in
                // tryAdvance
                this.localCancelled = result.localCancelled = true;
                return null;
            }
            map.put(left, none());
            map.put(right, none());
            map.remove(this.key);
            this.key = right;
            result.key = left;
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    @Override
    public long estimateSize() {
        return source == null ? 0 : source.estimateSize();
    }

    @Override
    public int characteristics() {
        return source == null ? SIZED : ORDERED;
    }
}
