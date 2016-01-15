package one.util.streamex;

import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static one.util.streamex.StreamExInternals.*;

/* package */abstract class UnknownSizeSpliterator<T, S extends UnknownSizeSpliterator<? extends T, S, I>, I extends Iterator<? extends T>>
        implements Spliterator<T> {
    static final int BATCH_UNIT = 1 << 10; // batch array size increment
    static final int MAX_BATCH = 1 << 25; // max batch array size;

    /**
     * Optimize the stream created on IteratorSpliterator replacing it with
     * UnknownSizeSpliterator.
     * 
     * @param stream original stream
     * @return either original or optimized stream
     */
    @SuppressWarnings("unchecked")
    static <T> Stream<T> optimize(Stream<T> stream) {
        if (SOURCE_SPLITERATOR == null || SPLITERATOR_ITERATOR == null)
            return stream;
        Iterator<T> it = null;
        try {
            Spliterator<T> spliterator = (Spliterator<T>) SOURCE_SPLITERATOR.get(stream);
            if (spliterator != null && !spliterator.hasCharacteristics(SIZED)
                && spliterator.getClass().getName().equals("java.util.Spliterators$IteratorSpliterator")) {
                it = (Iterator<T>) SPLITERATOR_ITERATOR.get(spliterator);
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // ignore
        }
        if (it == null)
            return stream;
        stream.spliterator(); // consume stream
        return StreamSupport.stream(new USOfRef<>(it), stream.isParallel()).onClose(stream::close);
    }

    I it;
    int index, fence;
    long est = Long.MAX_VALUE;

    UnknownSizeSpliterator(I iterator) {
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

    S correctSize(S prefix) {
        if (this.it != null)
            prefix.est = Long.MAX_VALUE - 1;
        else {
            prefix.est = this.est / 2;
            this.est -= prefix.est;
        }
        return prefix;
    }

    @Override
    public long estimateSize() {
        return est;
    }

    @Override
    public int characteristics() {
        return ORDERED;
    }

    static class USOfRef<T> extends UnknownSizeSpliterator<T, USOfRef<T>, Iterator<? extends T>> {
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
                while (i.hasNext() && j < n) {
                    a[j++] = i.next();
                }
                fence = j;
                if (i.hasNext()) {
                    return correctSize(new USOfRef<>(a, 0, j));
                }
                it = null;
                array = a;
            }
            int lo = index, mid = (lo + fence) >>> 1;
            return (lo >= mid) ? null : correctSize(new USOfRef<>(array, lo, index = mid));
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            if (it != null)
                it.forEachRemaining(action);
            else {
                Object[] a = array;
                int i = index, hi = fence;
                while (i < hi) {
                    @SuppressWarnings("unchecked")
                    T t = (T) a[i++];
                    action.accept(t);
                }
            }
            index = fence;
            est = 0;
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
            est = 0;
            return false;
        }
    }

    static class USOfInt extends UnknownSizeSpliterator<Integer, USOfInt, PrimitiveIterator.OfInt> implements
            Spliterator.OfInt {
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
                while (i.hasNext() && j < n) {
                    a[j++] = i.next();
                }
                fence = j;
                if (i.hasNext()) {
                    return correctSize(new USOfInt(a, 0, j));
                }
                it = null;
                array = a;
            }
            int lo = index, mid = (lo + fence) >>> 1;
            return (lo >= mid) ? null : correctSize(new USOfInt(array, lo, index = mid));
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            if (it != null)
                it.forEachRemaining(action);
            else {
                int[] a = array;
                int i = index, hi = fence;
                while (i < hi) {
                    action.accept(a[i++]);
                }
            }
            index = fence;
            est = 0;
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
            est = 0;
            return false;
        }
    }

    static class USOfLong extends UnknownSizeSpliterator<Long, USOfLong, PrimitiveIterator.OfLong> implements
            Spliterator.OfLong {
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
                while (i.hasNext() && j < n) {
                    a[j++] = i.next();
                }
                fence = j;
                if (i.hasNext()) {
                    return correctSize(new USOfLong(a, 0, j));
                }
                it = null;
                array = a;
            }
            int lo = index, mid = (lo + fence) >>> 1;
            return (lo >= mid) ? null : correctSize(new USOfLong(array, lo, index = mid));
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            if (it != null)
                it.forEachRemaining(action);
            else {
                long[] a = array;
                int i = index, hi = fence;
                while (i < hi) {
                    action.accept(a[i++]);
                }
            }
            index = fence;
            est = 0;
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
            est = 0;
            return false;
        }
    }

    static class USOfDouble extends UnknownSizeSpliterator<Double, USOfDouble, PrimitiveIterator.OfDouble> implements
            Spliterator.OfDouble {
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
                while (i.hasNext() && j < n) {
                    a[j++] = i.next();
                }
                fence = j;
                if (i.hasNext()) {
                    return correctSize(new USOfDouble(a, 0, j));
                }
                it = null;
                array = a;
            }
            int lo = index, mid = (lo + fence) >>> 1;
            return (lo >= mid) ? null : correctSize(new USOfDouble(array, lo, index = mid));
        }

        @Override
        public void forEachRemaining(DoubleConsumer action) {
            if (it != null)
                it.forEachRemaining(action);
            else {
                double[] a = array;
                int i = index, hi = fence;
                while (i < hi) {
                    action.accept(a[i++]);
                }
            }
            index = fence;
            est = 0;
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
            est = 0;
            return false;
        }
    }
}
