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

import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentSkipListMap;
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
        System.out.println(key+": start");
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
            System.out.println(key+": cancelled");
            if (localCancelled) {
                return false;
            }
        }
        this.source = null;
        A result = acc;
        boolean changed = true;
        Entry<Key, A> lowerEntry, higherEntry;
        System.out.println(key+": combining");
        do {
            changed = false;
            while(true) {
                if(localCancelled)
                    return false;
                lowerEntry = map.lowerEntry(key);
                if(lowerEntry == null || lowerEntry.getValue() == NONE)
                    break;
                A prev = map.remove(lowerEntry.getKey());
                if(prev == null) // other party removed this key during suffix traversal
                    break;
                System.out.println(key+": combine with prefix "+lowerEntry.getKey());
                result = combiner.apply(prev, result);
                if(cancelPredicate.test(result)) {
                    cancelSuffix();
                }
                changed = true;
            }
            while(true) {
                if(localCancelled)
                    return false;
                higherEntry = map.higherEntry(key);
                if(higherEntry == null || higherEntry.getValue() == NONE)
                    break;
                A next = map.remove(higherEntry.getKey());
                if(next == null) // other party removed this key during prefix traversal
                    break;
                System.out.println(key+": combine with suffix "+higherEntry.getKey());
                result = combiner.apply(result, next);
                if(cancelPredicate.test(result)) {
                    cancelSuffix();
                }
                changed = true;
            }
        } while(changed);
        if(lowerEntry == null && (higherEntry == null || suffix == null)) {
            System.out.println(key+": consume!");
            action.accept(result);
            return true;
        }
        System.out.println(key+": offer");
        A old = map.put(key, result);
        assert old == NONE;
        return false;
    }

    private void cancelSuffix() {
        if (this.suffix == null)
            return;
        OrderedCancellableSpliterator3<T, A> suffix = this.suffix;
        while (suffix != null && !suffix.localCancelled) {
            suffix.localCancelled = true;
            suffix = suffix.suffix;
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
                prefixPrefix.suffix = result;
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
