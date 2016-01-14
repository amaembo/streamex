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

import java.util.List;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;

import static one.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
/* package */abstract class RangeBasedSpliterator<T, S extends RangeBasedSpliterator<T, ?>> extends CloneableSpliterator<T, S> {
    int cur;
    int limit;

    public RangeBasedSpliterator(int fromInclusive, int toExclusive) {
        this.cur = fromInclusive;
        this.limit = toExclusive;
    }

    @Override
    public long estimateSize() {
        return limit - cur;
    }

    @Override
    public int characteristics() {
        return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
    }

    @Override
    public S trySplit() {
        int size = limit - cur;
        if (size >= 2) {
            S clone = doClone();
            clone.limit = this.cur = this.cur + size / 2;
            return clone;
        }
        return null;
    }

    static final class AsEntry<T> extends RangeBasedSpliterator<Entry<Integer, T>, AsEntry<T>> {
        private final List<T> list;

        public AsEntry(List<T> list) {
            super(0, list.size());
            this.list = list;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Entry<Integer, T>> action) {
            if (cur < limit) {
                action.accept(new ObjIntBox<>(list.get(cur), cur));
                cur++;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super Entry<Integer, T>> action) {
            int l = limit, c = cur;
            List<T> list = this.list;
            while (c < l) {
                action.accept(new ObjIntBox<>(list.get(c), c));
                c++;
            }
            cur = limit;
        }
    }

    static final class OfSubLists<T> extends RangeBasedSpliterator<List<T>, OfSubLists<T>> {
        private final List<T> source;
        private final int length;
        private final int shift;
        private final int listSize;

        public OfSubLists(List<T> source, int length, int shift) {
            super(0, Math.max(0, source.size() - Math.max(length - shift, 0) - 1) / shift + 1);
            this.source = source;
            this.listSize = source.size();
            this.shift = shift;
            this.length = length;
        }

        @Override
        public boolean tryAdvance(Consumer<? super List<T>> action) {
            if (cur < limit) {
                int start = cur * shift;
                int stop = listSize - length > start ? start + length : listSize;
                action.accept(source.subList(start, stop));
                cur++;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super List<T>> action) {
            int l = limit, c = cur, ll = length, sf = shift, ls = listSize;
            int start = cur * sf;
            while (c < l) {
                int stop = ls - ll > start ? start + ll : ls;
                action.accept(source.subList(start, stop));
                start += sf;
                c++;
            }
            cur = limit;
        }
    }

    static final class ZipRef<U, V, T> extends RangeBasedSpliterator<T, ZipRef<U, V, T>> {
        private final List<U> l1;
        private final List<V> l2;
        private final BiFunction<? super U, ? super V, ? extends T> mapper;

        public ZipRef(int fromInclusive, int toExclusive, BiFunction<? super U, ? super V, ? extends T> mapper,
                List<U> l1, List<V> l2) {
            super(fromInclusive, toExclusive);
            this.l1 = l1;
            this.l2 = l2;
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (cur < limit) {
                action.accept(mapper.apply(l1.get(cur), l2.get(cur)));
                cur++;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            int l = limit, c = cur;
            while (c < l) {
                action.accept(mapper.apply(l1.get(c), l2.get(c)));
                c++;
            }
            cur = limit;
        }
    }

    static final class ZipInt extends RangeBasedSpliterator<Integer, ZipInt> implements Spliterator.OfInt {
        private final IntBinaryOperator mapper;
        private final int[] arr1, arr2;

        public ZipInt(int fromInclusive, int toExclusive, IntBinaryOperator mapper, int[] arr1, int[] arr2) {
            super(fromInclusive, toExclusive);
            this.mapper = mapper;
            this.arr1 = arr1;
            this.arr2 = arr2;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (cur < limit) {
                action.accept(mapper.applyAsInt(arr1[cur], arr2[cur]));
                cur++;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            int l = limit, c = cur;
            while (c < l) {
                action.accept(mapper.applyAsInt(arr1[c], arr2[c]));
                c++;
            }
            cur = limit;
        }
    }

    static final class OfByte extends RangeBasedSpliterator<Integer, OfByte> implements Spliterator.OfInt {
        private final byte[] array;

        public OfByte(int fromInclusive, int toExclusive, byte[] array) {
            super(fromInclusive, toExclusive);
            this.array = array;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (cur < limit) {
                action.accept(array[cur]);
                cur++;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            int l = limit, c = cur;
            while (c < l) {
                action.accept(array[c++]);
            }
            cur = limit;
        }
    }

    static final class OfChar extends RangeBasedSpliterator<Integer, OfChar> implements Spliterator.OfInt {
        private final char[] array;

        public OfChar(int fromInclusive, int toExclusive, char[] array) {
            super(fromInclusive, toExclusive);
            this.array = array;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (cur < limit) {
                action.accept(array[cur]);
                cur++;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            int l = limit, c = cur;
            while (c < l) {
                action.accept(array[c++]);
            }
            cur = limit;
        }
    }

    static final class OfShort extends RangeBasedSpliterator<Integer, OfShort> implements Spliterator.OfInt {
        private final short[] array;

        public OfShort(int fromInclusive, int toExclusive, short[] array) {
            super(fromInclusive, toExclusive);
            this.array = array;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (cur < limit) {
                action.accept(array[cur]);
                cur++;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            int l = limit, c = cur;
            while (c < l) {
                action.accept(array[c++]);
            }
            cur = limit;
        }
    }

    static final class ZipLong extends RangeBasedSpliterator<Long, ZipLong> implements Spliterator.OfLong {
        private final LongBinaryOperator mapper;
        private final long[] arr1, arr2;

        public ZipLong(int fromInclusive, int toExclusive, LongBinaryOperator mapper, long[] arr1, long[] arr2) {
            super(fromInclusive, toExclusive);
            this.mapper = mapper;
            this.arr1 = arr1;
            this.arr2 = arr2;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            if (cur < limit) {
                action.accept(mapper.applyAsLong(arr1[cur], arr2[cur]));
                cur++;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            int l = limit, c = cur;
            while (c < l) {
                action.accept(mapper.applyAsLong(arr1[c], arr2[c]));
                c++;
            }
            cur = limit;
        }
    }

    static final class OfFloat extends RangeBasedSpliterator<Double, OfFloat> implements Spliterator.OfDouble {
        private final float[] array;

        public OfFloat(int fromInclusive, int toExclusive, float[] array) {
            super(fromInclusive, toExclusive);
            this.array = array;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if (cur < limit) {
                action.accept(array[cur]);
                cur++;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(DoubleConsumer action) {
            int l = limit, c = cur;
            while (c < l) {
                action.accept(array[c++]);
            }
            cur = limit;
        }
    }

    static final class ZipDouble extends RangeBasedSpliterator<Double, ZipDouble> implements Spliterator.OfDouble {
        private final DoubleBinaryOperator mapper;
        private final double[] arr1, arr2;

        public ZipDouble(int fromInclusive, int toExclusive, DoubleBinaryOperator mapper, double[] arr1, double[] arr2) {
            super(fromInclusive, toExclusive);
            this.mapper = mapper;
            this.arr1 = arr1;
            this.arr2 = arr2;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if (cur < limit) {
                action.accept(mapper.applyAsDouble(arr1[cur], arr2[cur]));
                cur++;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(DoubleConsumer action) {
            int l = limit, c = cur;
            while (c < l) {
                action.accept(mapper.applyAsDouble(arr1[c], arr2[c]));
                c++;
            }
            cur = limit;
        }
    }
}
