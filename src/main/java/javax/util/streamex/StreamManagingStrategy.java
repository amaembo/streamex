package javax.util.streamex;

import java.util.Map.Entry;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator.OfDouble;
import java.util.PrimitiveIterator.OfInt;
import java.util.PrimitiveIterator.OfLong;
import java.util.Spliterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/* package */abstract class StreamManagingStrategy {
    private static final class DefaultStreamManagingStrategy extends StreamManagingStrategy {
        @Override
        public <T> StreamEx<T> newStreamEx(Stream<T> src) {
            return new StreamEx<>(src);
        }

        @Override
        public <K, V> EntryStream<K, V> newEntryStream(Stream<Entry<K, V>> src) {
            return new EntryStream<>(src);
        }

        @Override
        public LongStreamEx newLongStreamEx(LongStream src) {
            return new LongStreamEx(src);
        }

        @Override
        public IntStreamEx newIntStreamEx(IntStream src) {
            return new IntStreamEx(src);
        }

        @Override
        public DoubleStreamEx newDoubleStreamEx(DoubleStream src) {
            return new DoubleStreamEx(src);
        }

        @Override
        public <T> T terminate(Supplier<T> terminalOperation) {
            return terminalOperation.get();
        }
    }

    private static class CustomPoolStreamManagingStrategy extends StreamManagingStrategy {
        private final ForkJoinPool fjp;

        public CustomPoolStreamManagingStrategy(ForkJoinPool fjp) {
            this.fjp = fjp;
        }

        @Override
        public <T> StreamEx<T> newStreamEx(Stream<T> src) {
            return new StreamEx<T>(src) {
                @Override
                StreamManagingStrategy strategy() {
                    return CustomPoolStreamManagingStrategy.this;
                }
            };
        }

        @Override
        public <K, V> EntryStream<K, V> newEntryStream(Stream<Entry<K, V>> src) {
            return new EntryStream<K, V>(src) {
                @Override
                StreamManagingStrategy strategy() {
                    return CustomPoolStreamManagingStrategy.this;
                }
            };
        }

        @Override
        public LongStreamEx newLongStreamEx(LongStream src) {
            return new CustomLongStreamEx(src, this);
        }

        @Override
        public IntStreamEx newIntStreamEx(IntStream src) {
            return new CustomIntStreamEx(src, this);
        }

        @Override
        public DoubleStreamEx newDoubleStreamEx(DoubleStream src) {
            return new CustomDoubleStreamEx(src, this);
        }

        @Override
        public <T> T terminate(Supplier<T> terminalOperation) {
            ForkJoinTask<T> task = fjp.submit(terminalOperation::get);
            return task.join();
        }
    }

    static class CustomIntStreamEx extends IntStreamEx {
        private final StreamManagingStrategy strategy;
    
        CustomIntStreamEx(IntStream stream, StreamManagingStrategy strategy) {
            super(stream);
            this.strategy = strategy;
        }
    
        @Override
        StreamManagingStrategy strategy() {
            return strategy;
        }
    
        @Override
        public void forEach(IntConsumer action) {
            strategy().terminate(() -> {stream.forEach(action); return null;});
        }
    
        @Override
        public void forEachOrdered(IntConsumer action) {
            strategy().terminate(() -> {stream.forEachOrdered(action); return null;});
        }
    
        @Override
        public int[] toArray() {
            return strategy().terminate(stream::toArray);
        }
    
        @Override
        public int reduce(int identity, IntBinaryOperator op) {
            return strategy().terminate(() -> stream.reduce(identity, op));
        }
    
        @Override
        public OptionalInt reduce(IntBinaryOperator op) {
            return strategy().terminate(() -> stream.reduce(op));
        }
    
        @Override
        public long count() {
            return strategy().terminate(stream::count);
        }
    
        @Override
        public OptionalDouble average() {
            return strategy().terminate(stream::average);
        }
    
        @Override
        public boolean anyMatch(IntPredicate predicate) {
            return strategy().terminate(() -> stream.anyMatch(predicate));
        }
    
        @Override
        public boolean allMatch(IntPredicate predicate) {
            return strategy().terminate(() -> stream.allMatch(predicate));
        }
    
        @Override
        public OptionalInt findFirst() {
            return strategy().terminate(stream::findFirst);
        }
    
        @Override
        public OptionalInt findAny() {
            return strategy().terminate(stream::findAny);
        }
    
        @Override
        public OfInt iterator() {
            return strategy().terminate(stream::iterator);
        }
    
        @Override
        public Spliterator.OfInt spliterator() {
            return strategy().terminate(stream::spliterator);
        }
    }

    static class CustomLongStreamEx extends LongStreamEx {
        private final StreamManagingStrategy strategy;
    
        CustomLongStreamEx(LongStream stream, StreamManagingStrategy strategy) {
            super(stream);
            this.strategy = strategy;
        }
    
        @Override
        StreamManagingStrategy strategy() {
            return strategy;
        }
    
        @Override
        public void forEach(LongConsumer action) {
            strategy().terminate(() -> {stream.forEach(action); return null;});
        }
    
        @Override
        public void forEachOrdered(LongConsumer action) {
            strategy().terminate(() -> {stream.forEachOrdered(action); return null;});
        }
    
        @Override
        public long[] toArray() {
            return strategy().terminate(stream::toArray);
        }
    
        @Override
        public long reduce(long identity, LongBinaryOperator op) {
            return strategy().terminate(() -> stream.reduce(identity, op));
        }
    
        @Override
        public OptionalLong reduce(LongBinaryOperator op) {
            return strategy().terminate(() -> stream.reduce(op));
        }
    
        @Override
        public long count() {
            return strategy().terminate(stream::count);
        }
    
        @Override
        public OptionalDouble average() {
            return strategy().terminate(stream::average);
        }
    
        @Override
        public boolean anyMatch(LongPredicate predicate) {
            return strategy().terminate(() -> stream.anyMatch(predicate));
        }
    
        @Override
        public boolean allMatch(LongPredicate predicate) {
            return strategy().terminate(() -> stream.allMatch(predicate));
        }
    
        @Override
        public OptionalLong findFirst() {
            return strategy().terminate(stream::findFirst);
        }
    
        @Override
        public OptionalLong findAny() {
            return strategy().terminate(stream::findAny);
        }
    
        @Override
        public OfLong iterator() {
            return strategy().terminate(stream::iterator);
        }
    
        @Override
        public Spliterator.OfLong spliterator() {
            return strategy().terminate(stream::spliterator);
        }
    }

    static class CustomDoubleStreamEx extends DoubleStreamEx {
    
        private final StreamManagingStrategy strategy;
    
        CustomDoubleStreamEx(DoubleStream stream, StreamManagingStrategy strategy) {
            super(stream);
            this.strategy = strategy;
        }
    
        @Override
        StreamManagingStrategy strategy() {
            return strategy;
        }
    
        @Override
        public void forEach(DoubleConsumer action) {
            strategy().terminate(() -> {stream.forEach(action); return null;});
        }
    
        @Override
        public void forEachOrdered(DoubleConsumer action) {
            strategy().terminate(() -> {stream.forEachOrdered(action); return null;});
        }
    
        @Override
        public double[] toArray() {
            return strategy().terminate(stream::toArray);
        }
    
        @Override
        public double reduce(double identity, DoubleBinaryOperator op) {
            return strategy().terminate(() -> stream.reduce(identity, op));
        }
    
        @Override
        public OptionalDouble reduce(DoubleBinaryOperator op) {
            return strategy().terminate(() -> stream.reduce(op));
        }
    
        @Override
        public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
            return strategy().terminate(() -> stream.collect(supplier, accumulator, combiner));
        }
    
        @Override
        public double sum() {
            return strategy().terminate(stream::sum);
        }
    
        @Override
        public long count() {
            return strategy().terminate(stream::count);
        }
    
        @Override
        public OptionalDouble average() {
            return strategy().terminate(stream::average);
        }
    
        @Override
        public boolean anyMatch(DoublePredicate predicate) {
            return strategy().terminate(() -> stream.anyMatch(predicate));
        }
    
        @Override
        public boolean allMatch(DoublePredicate predicate) {
            return strategy().terminate(() -> stream.allMatch(predicate));
        }
    
        @Override
        public OptionalDouble findFirst() {
            return strategy().terminate(() -> stream.findFirst());
        }
    
        @Override
        public OptionalDouble findAny() {
            return strategy().terminate(() -> stream.findAny());
        }
    
        @Override
        public OfDouble iterator() {
            return strategy().terminate(stream::iterator);
        }
    
        @Override
        public Spliterator.OfDouble spliterator() {
            return strategy().terminate(stream::spliterator);
        }
    }

    abstract <T> StreamEx<T> newStreamEx(Stream<T> src);
    abstract <K, V> EntryStream<K, V> newEntryStream(Stream<Entry<K, V>> src);
    abstract IntStreamEx newIntStreamEx(IntStream src);
    abstract LongStreamEx newLongStreamEx(LongStream src);
    abstract DoubleStreamEx newDoubleStreamEx(DoubleStream src);
    abstract <T> T terminate(Supplier<T> terminalOperation);
    
    static final StreamManagingStrategy DEFAULT = new DefaultStreamManagingStrategy();
    
    static StreamManagingStrategy forCustomPool(ForkJoinPool fjp) {
        return new CustomPoolStreamManagingStrategy(fjp);
    }
}
