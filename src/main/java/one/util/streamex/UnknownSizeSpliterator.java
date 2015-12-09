package one.util.streamex;

import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/* package */abstract class UnknownSizeSpliterator<T, I extends Iterator<? extends T>> implements Spliterator<T> {
    static final int BATCH_UNIT = 1 << 10; // batch array size increment
    static final int MAX_BATCH = 1 << 25; // max batch array size;

    I it;
    int index, fence;

    public UnknownSizeSpliterator(I iterator) {
        this.it = iterator;
    }

    UnknownSizeSpliterator(int index, int fence) {
        this.index = index;
        this.fence = fence;
    }

    int getN() {
        int n = fence + BATCH_UNIT;
        return n > MAX_BATCH ? MAX_BATCH : n;
    }

    @Override
    public long estimateSize() {
        return it == null ? fence - index : Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return it == null ? SIZED | SUBSIZED | ORDERED : ORDERED;
    }

    static class USOfRef<T> extends UnknownSizeSpliterator<T, Iterator<? extends T>> {
        Object[] array;

        USOfRef(Iterator<? extends T> iterator) {
            super(iterator);
        }

        USOfRef(Object[] array, int index, int fence) {
            super(index, fence);
            this.array = array;
        }

        @Override
        public Spliterator<T> trySplit() {
            Iterator<? extends T> i = it;
            if (i != null) {
                int n = getN();
                Object[] a = new Object[n];
                int j = 0;
                do {
                    a[j] = i.next();
                } while (++j < n && i.hasNext());
                fence = j;
                if (i.hasNext()) {
                    return new USOfRef<>(a, 0, j);
                }
                it = null;
                array = a;
            }
            int lo = index, mid = (lo + fence) >>> 1;
            return (lo >= mid) ? null : new USOfRef<>(array, lo, index = mid);
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            if (it != null)
                it.forEachRemaining(action);
            else {
                Object[] a = array;
                int i = index, hi = fence;
                if (i < hi && a.length >= hi) {
                    do {
                        @SuppressWarnings("unchecked")
                        T t = (T) a[i];
                        action.accept(t);
                    } while (++i < hi);
                }
            }
            index = fence;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (it != null) {
                if (it.hasNext()) {
                    action.accept(it.next());
                    return true;
                }
                it = null;
                index = fence;
            } else if (index < fence) {
                @SuppressWarnings("unchecked")
                T t = (T) array[index++];
                action.accept(t);
                return true;
            }
            return false;
        }
    }

    static class USOfInt extends UnknownSizeSpliterator<Integer, PrimitiveIterator.OfInt> implements Spliterator.OfInt {
        int[] array;

        USOfInt(PrimitiveIterator.OfInt iterator) {
            super(iterator);
        }

        USOfInt(int[] array, int index, int fence) {
            super(index, fence);
            this.array = array;
        }

        @Override
        public Spliterator.OfInt trySplit() {
            PrimitiveIterator.OfInt i = it;
            if (i != null) {
                int n = getN();
                int[] a = new int[n];
                int j = 0;
                do {
                    a[j] = i.next();
                } while (++j < n && i.hasNext());
                fence = j;
                if (i.hasNext()) {
                    return new USOfInt(a, 0, j);
                }
                it = null;
                array = a;
            }
            int lo = index, mid = (lo + fence) >>> 1;
            return (lo >= mid) ? null : new USOfInt(array, lo, index = mid);
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            if (it != null)
                it.forEachRemaining(action);
            else {
                int[] a = array;
                int i = index, hi = fence;
                if (i < hi && a.length >= hi) {
                    do {
                        action.accept(a[i]);
                    } while (++i < hi);
                }
            }
            index = fence;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (it != null) {
                if (it.hasNext()) {
                    action.accept(it.nextInt());
                    return true;
                }
                it = null;
                index = fence;
            } else if (index < fence) {
                action.accept(array[index++]);
                return true;
            }
            return false;
        }
    }

    static class USOfLong extends UnknownSizeSpliterator<Long, PrimitiveIterator.OfLong> implements Spliterator.OfLong {
        long[] array;

        USOfLong(PrimitiveIterator.OfLong iterator) {
            super(iterator);
        }

        USOfLong(long[] array, int index, int fence) {
            super(index, fence);
            this.array = array;
        }

        @Override
        public Spliterator.OfLong trySplit() {
            PrimitiveIterator.OfLong i = it;
            if (i != null) {
                int n = getN();
                long[] a = new long[n];
                int j = 0;
                do {
                    a[j] = i.next();
                } while (++j < n && i.hasNext());
                fence = j;
                if (i.hasNext()) {
                    return new USOfLong(a, 0, j);
                }
                it = null;
                array = a;
            }
            int lo = index, mid = (lo + fence) >>> 1;
            return (lo >= mid) ? null : new USOfLong(array, lo, index = mid);
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            if (it != null)
                it.forEachRemaining(action);
            else {
                long[] a = array;
                int i = index, hi = fence;
                if (i < hi && a.length >= hi) {
                    do {
                        action.accept(a[i]);
                    } while (++i < hi);
                }
            }
            index = fence;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            if (it != null) {
                if (it.hasNext()) {
                    action.accept(it.nextLong());
                    return true;
                }
                it = null;
                index = fence;
            } else if (index < fence) {
                action.accept(array[index++]);
                return true;
            }
            return false;
        }
    }
    
    static class USOfDouble extends UnknownSizeSpliterator<Double, PrimitiveIterator.OfDouble> implements Spliterator.OfDouble {
        double[] array;
        
        USOfDouble(PrimitiveIterator.OfDouble iterator) {
            super(iterator);
        }
        
        USOfDouble(double[] array, int index, int fence) {
            super(index, fence);
            this.array = array;
        }
        
        @Override
        public Spliterator.OfDouble trySplit() {
            PrimitiveIterator.OfDouble i = it;
            if (i != null) {
                int n = getN();
                double[] a = new double[n];
                int j = 0;
                do {
                    a[j] = i.next();
                } while (++j < n && i.hasNext());
                fence = j;
                if (i.hasNext()) {
                    return new USOfDouble(a, 0, j);
                }
                it = null;
                array = a;
            }
            int lo = index, mid = (lo + fence) >>> 1;
                    return (lo >= mid) ? null : new USOfDouble(array, lo, index = mid);
        }
        
        @Override
        public void forEachRemaining(DoubleConsumer action) {
            if (it != null)
                it.forEachRemaining(action);
            else {
                double[] a = array;
                int i = index, hi = fence;
                if (i < hi && a.length >= hi) {
                    do {
                        action.accept(a[i]);
                    } while (++i < hi);
                }
            }
            index = fence;
        }
        
        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if (it != null) {
                if (it.hasNext()) {
                    action.accept(it.nextDouble());
                    return true;
                }
                it = null;
                index = fence;
            } else if (index < fence) {
                action.accept(array[index++]);
                return true;
            }
            return false;
        }
    }
}
