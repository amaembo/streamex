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
package one.util.streamex;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/* package */ class StreamFactory {

    static final class CustomPoolStreamFactory extends StreamFactory {
        private final ForkJoinPool fjp;

        public CustomPoolStreamFactory(ForkJoinPool fjp) {
            this.fjp = fjp;
        }

        @Override
        public <T> StreamEx<T> newStreamEx(Stream<T> src) {
            return new CustomStreamEx<>(src, this);
        }

        @Override
        public <K, V> EntryStream<K, V> newEntryStream(Stream<Entry<K, V>> src) {
            return new CustomEntryStream<>(src, this);
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

        public <T> T terminate(Supplier<T> terminalOperation) {
            return fjp.submit(terminalOperation::get).join();
        }

        public <T, U> T terminate(U value, Function<U, T> terminalOperation) {
            return fjp.submit(() -> terminalOperation.apply(value)).join();
        }
    }

    static final class CustomEntryStream<K, V> extends EntryStream<K, V> {
        private final CustomPoolStreamFactory strategy;

        CustomEntryStream(Stream<Entry<K, V>> stream, CustomPoolStreamFactory strategy) {
            super(stream);
            this.strategy = strategy;
        }

        @Override
        StreamFactory strategy() {
            return strategy;
        }

        @Override
        public void forEach(Consumer<? super Entry<K, V>> action) {
            strategy.terminate(() -> {
                stream.forEach(action);
                return null;
            });
        }

        @Override
        public void forEachOrdered(Consumer<? super Entry<K, V>> action) {
            strategy.terminate(() -> {
                stream.forEachOrdered(action);
                return null;
            });
        }

        @Override
        public <A> A[] toArray(IntFunction<A[]> generator) {
            return strategy.terminate(generator, stream::toArray);
        }
        
        @Override
        public <R> R toListAndThen(Function<List<Entry<K, V>>, R> finisher) {
            return strategy.terminate(finisher, super::toListAndThen);
        }

        @Override
        public Entry<K, V> reduce(Entry<K, V> identity, BinaryOperator<Entry<K, V>> accumulator) {
            return strategy.terminate(() -> stream.reduce(identity, accumulator));
        }

        @Override
        public Optional<Entry<K, V>> reduce(BinaryOperator<Entry<K, V>> accumulator) {
            return strategy.terminate(accumulator, stream::reduce);
        }

        @Override
        public <U> U reduce(U identity, BiFunction<U, ? super Entry<K, V>, U> accumulator, BinaryOperator<U> combiner) {
            return strategy.terminate(() -> stream.reduce(identity, accumulator, combiner));
        }

        @Override
        public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super Entry<K, V>> accumulator,
                BiConsumer<R, R> combiner) {
            return strategy.terminate(() -> stream.collect(supplier, accumulator, combiner));
        }

        @Override
        <R, A> R rawCollect(Collector<? super Entry<K, V>, A, R> collector) {
            return strategy.terminate(collector, stream::collect);
        }

        @Override
        public long count() {
            return strategy.terminate(stream::count);
        }

        @Override
        public boolean anyMatch(Predicate<? super Entry<K, V>> predicate) {
            return strategy.terminate(predicate, stream::anyMatch);
        }

        @Override
        public boolean allMatch(Predicate<? super Entry<K, V>> predicate) {
            return strategy.terminate(predicate, stream::allMatch);
        }

        @Override
        public Optional<Entry<K, V>> findFirst() {
            return strategy.terminate(stream::findFirst);
        }

        @Override
        public Optional<Entry<K, V>> findAny() {
            return strategy.terminate(stream::findAny);
        }
    }

    static final class CustomStreamEx<T> extends StreamEx<T> {
        private final CustomPoolStreamFactory strategy;

        CustomStreamEx(Stream<T> stream, CustomPoolStreamFactory strategy) {
            super(stream);
            this.strategy = strategy;
        }

        @Override
        StreamFactory strategy() {
            return strategy;
        }

        @Override
        public void forEach(Consumer<? super T> action) {
            strategy.terminate(() -> {
                stream.forEach(action);
                return null;
            });
        }

        @Override
        public void forEachOrdered(Consumer<? super T> action) {
            strategy.terminate(() -> {
                stream.forEachOrdered(action);
                return null;
            });
        }

        @Override
        public <A> A[] toArray(IntFunction<A[]> generator) {
            return strategy.terminate(generator, stream::toArray);
        }

        @Override
        public <R> R toListAndThen(Function<List<T>, R> finisher) {
            return strategy.terminate(finisher, super::toListAndThen);
        }

        @Override
        public T reduce(T identity, BinaryOperator<T> accumulator) {
            return strategy.terminate(() -> stream.reduce(identity, accumulator));
        }

        @Override
        public Optional<T> reduce(BinaryOperator<T> accumulator) {
            return strategy.terminate(accumulator, stream::reduce);
        }

        @Override
        public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
            return strategy.terminate(() -> stream.reduce(identity, accumulator, combiner));
        }

        @Override
        public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
            return strategy.terminate(() -> stream.collect(supplier, accumulator, combiner));
        }

        @Override
        <R, A> R rawCollect(Collector<? super T, A, R> collector) {
            return strategy.terminate(collector, stream::collect);
        }

        @Override
        public long count() {
            return strategy.terminate(stream::count);
        }

        @Override
        public boolean anyMatch(Predicate<? super T> predicate) {
            return strategy.terminate(predicate, stream::anyMatch);
        }

        @Override
        public boolean allMatch(Predicate<? super T> predicate) {
            return strategy.terminate(predicate, stream::allMatch);
        }

        @Override
        public Optional<T> findFirst() {
            return strategy.terminate(stream::findFirst);
        }

        @Override
        public Optional<T> findAny() {
            return strategy.terminate(stream::findAny);
        }
    }

    static final class CustomIntStreamEx extends IntStreamEx {
        private final CustomPoolStreamFactory strategy;

        CustomIntStreamEx(IntStream stream, CustomPoolStreamFactory strategy) {
            super(stream);
            this.strategy = strategy;
        }

        @Override
        StreamFactory strategy() {
            return strategy;
        }

        @Override
        public void forEach(IntConsumer action) {
            strategy.terminate(() -> {
                stream.forEach(action);
                return null;
            });
        }

        @Override
        public void forEachOrdered(IntConsumer action) {
            strategy.terminate(() -> {
                stream.forEachOrdered(action);
                return null;
            });
        }

        @Override
        public int[] toArray() {
            return strategy.terminate(stream::toArray);
        }

        @Override
        public int reduce(int identity, IntBinaryOperator op) {
            return strategy.terminate(() -> stream.reduce(identity, op));
        }

        @Override
        public OptionalInt reduce(IntBinaryOperator op) {
            return strategy.terminate(op, stream::reduce);
        }

        @Override
        public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
            return strategy.terminate(() -> stream.collect(supplier, accumulator, combiner));
        }

        @Override
        public long count() {
            return strategy.terminate(stream::count);
        }

        @Override
        public OptionalDouble average() {
            return strategy.terminate(stream::average);
        }

        @Override
        public boolean anyMatch(IntPredicate predicate) {
            return strategy.terminate(predicate, stream::anyMatch);
        }

        @Override
        public boolean allMatch(IntPredicate predicate) {
            return strategy.terminate(predicate, stream::allMatch);
        }

        @Override
        public OptionalInt findFirst() {
            return strategy.terminate(stream::findFirst);
        }

        @Override
        public OptionalInt findAny() {
            return strategy.terminate(stream::findAny);
        }
    }

    static final class CustomLongStreamEx extends LongStreamEx {
        private final CustomPoolStreamFactory strategy;

        CustomLongStreamEx(LongStream stream, CustomPoolStreamFactory strategy) {
            super(stream);
            this.strategy = strategy;
        }

        @Override
        StreamFactory strategy() {
            return strategy;
        }

        @Override
        public void forEach(LongConsumer action) {
            strategy.terminate(() -> {
                stream.forEach(action);
                return null;
            });
        }

        @Override
        public void forEachOrdered(LongConsumer action) {
            strategy.terminate(() -> {
                stream.forEachOrdered(action);
                return null;
            });
        }

        @Override
        public long[] toArray() {
            return strategy.terminate(stream::toArray);
        }

        @Override
        public long reduce(long identity, LongBinaryOperator op) {
            return strategy.terminate(() -> stream.reduce(identity, op));
        }

        @Override
        public OptionalLong reduce(LongBinaryOperator op) {
            return strategy.terminate(op, stream::reduce);
        }

        @Override
        public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
            return strategy.terminate(() -> stream.collect(supplier, accumulator, combiner));
        }

        @Override
        public long count() {
            return strategy.terminate(stream::count);
        }

        @Override
        public OptionalDouble average() {
            return strategy.terminate(stream::average);
        }

        @Override
        public boolean anyMatch(LongPredicate predicate) {
            return strategy.terminate(predicate, stream::anyMatch);
        }

        @Override
        public boolean allMatch(LongPredicate predicate) {
            return strategy.terminate(predicate, stream::allMatch);
        }

        @Override
        public OptionalLong findFirst() {
            return strategy.terminate(stream::findFirst);
        }

        @Override
        public OptionalLong findAny() {
            return strategy.terminate(stream::findAny);
        }
    }

    static final class CustomDoubleStreamEx extends DoubleStreamEx {
        private final CustomPoolStreamFactory strategy;

        CustomDoubleStreamEx(DoubleStream stream, CustomPoolStreamFactory strategy) {
            super(stream);
            this.strategy = strategy;
        }

        @Override
        StreamFactory strategy() {
            return strategy;
        }

        @Override
        public void forEach(DoubleConsumer action) {
            strategy.terminate(() -> {
                stream.forEach(action);
                return null;
            });
        }

        @Override
        public void forEachOrdered(DoubleConsumer action) {
            strategy.terminate(() -> {
                stream.forEachOrdered(action);
                return null;
            });
        }

        @Override
        public double[] toArray() {
            return strategy.terminate(stream::toArray);
        }

        @Override
        public double reduce(double identity, DoubleBinaryOperator op) {
            return strategy.terminate(() -> stream.reduce(identity, op));
        }

        @Override
        public OptionalDouble reduce(DoubleBinaryOperator op) {
            return strategy.terminate(op, stream::reduce);
        }

        @Override
        public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
            return strategy.terminate(() -> stream.collect(supplier, accumulator, combiner));
        }

        @Override
        public double sum() {
            return strategy.terminate(stream::sum);
        }

        @Override
        public long count() {
            return strategy.terminate(stream::count);
        }

        @Override
        public OptionalDouble average() {
            return strategy.terminate(stream::average);
        }

        @Override
        public boolean anyMatch(DoublePredicate predicate) {
            return strategy.terminate(predicate, stream::anyMatch);
        }

        @Override
        public boolean allMatch(DoublePredicate predicate) {
            return strategy.terminate(predicate, stream::allMatch);
        }

        @Override
        public OptionalDouble findFirst() {
            return strategy.terminate(stream::findFirst);
        }

        @Override
        public OptionalDouble findAny() {
            return strategy.terminate(stream::findAny);
        }
    }

    public <T> StreamEx<T> newStreamEx(Stream<T> src) {
        return new StreamEx<>(src);
    }

    public <K, V> EntryStream<K, V> newEntryStream(Stream<Entry<K, V>> src) {
        return new EntryStream<>(src);
    }

    public LongStreamEx newLongStreamEx(LongStream src) {
        return new LongStreamEx(src);
    }

    public IntStreamEx newIntStreamEx(IntStream src) {
        return new IntStreamEx(src);
    }

    public DoubleStreamEx newDoubleStreamEx(DoubleStream src) {
        return new DoubleStreamEx(src);
    }

    static final StreamFactory DEFAULT = new StreamFactory();

    static StreamFactory forCustomPool(ForkJoinPool fjp) {
        return new CustomPoolStreamFactory(fjp);
    }
}
