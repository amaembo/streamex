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

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/* package */abstract class AbstractStreamEx<T, S extends AbstractStreamEx<T, S>> implements Stream<T> {
    final Stream<T> stream;

    AbstractStreamEx(Stream<T> stream) {
        this.stream = stream;
    }

    abstract S supply(Stream<T> stream);

    @Override
    public Iterator<T> iterator() {
        return stream.iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return stream.spliterator();
    }

    @Override
    public boolean isParallel() {
        return stream.isParallel();
    }

    @Override
    public S sequential() {
        return supply(stream.sequential());
    }

    @Override
    public S parallel() {
        return supply(stream.parallel());
    }

    @Override
    public S unordered() {
        return supply(stream.unordered());
    }

    @Override
    public S onClose(Runnable closeHandler) {
        return supply(stream.onClose(closeHandler));
    }

    @Override
    public void close() {
        stream.close();
    }

    @Override
    public S filter(Predicate<? super T> predicate) {
        return supply(stream.filter(predicate));
    }

    @Override
    public IntStreamEx mapToInt(ToIntFunction<? super T> mapper) {
        return new IntStreamEx(stream.mapToInt(mapper));
    }

    @Override
    public LongStreamEx mapToLong(ToLongFunction<? super T> mapper) {
        return new LongStreamEx(stream.mapToLong(mapper));
    }

    @Override
    public DoubleStreamEx mapToDouble(ToDoubleFunction<? super T> mapper) {
        return new DoubleStreamEx(stream.mapToDouble(mapper));
    }

    @Override
    public IntStreamEx flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return new IntStreamEx(stream.flatMapToInt(mapper));
    }

    @Override
    public LongStreamEx flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return new LongStreamEx(stream.flatMapToLong(mapper));
    }

    @Override
    public DoubleStreamEx flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return new DoubleStreamEx(stream.flatMapToDouble(mapper));
    }

    @Override
    public S distinct() {
        return supply(stream.distinct());
    }

    @Override
    public S sorted() {
        return supply(stream.sorted());
    }

    @Override
    public S sorted(Comparator<? super T> comparator) {
        return supply(stream.sorted(comparator));
    }

    @Override
    public S peek(Consumer<? super T> action) {
        return supply(stream.peek(action));
    }

    @Override
    public S limit(long maxSize) {
        return supply(stream.limit(maxSize));
    }

    @Override
    public S skip(long n) {
        return supply(stream.skip(n));
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        stream.forEach(action);
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        stream.forEachOrdered(action);
    }

    @Override
    public Object[] toArray() {
        return stream.toArray();
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return stream.toArray(generator);
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        return stream.reduce(identity, accumulator);
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return stream.reduce(accumulator);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        return stream.reduce(identity, accumulator, combiner);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return stream.collect(supplier, accumulator, combiner);
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return stream.collect(collector);
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        return stream.min(comparator);
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        return stream.max(comparator);
    }

    @Override
    public long count() {
        return stream.count();
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return stream.anyMatch(predicate);
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        return stream.allMatch(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return stream.noneMatch(predicate);
    }

    @Override
    public Optional<T> findFirst() {
        return stream.findFirst();
    }

    @Override
    public Optional<T> findAny() {
        return stream.findAny();
    }

    public S remove(Predicate<T> predicate) {
        return supply(stream.filter(predicate.negate()));
    }

    public S nonNull() {
        return supply(stream.filter(Objects::nonNull));
    }

    public Optional<T> findAny(Predicate<T> predicate) {
        return stream.filter(predicate).findAny();
    }

    public Optional<T> findFirst(Predicate<T> predicate) {
        return stream.filter(predicate).findFirst();
    }

    public S reverseSorted(Comparator<? super T> comparator) {
        return supply(stream.sorted(comparator.reversed()));
    }

    public <V extends Comparable<? super V>> S sortedBy(Function<T, ? extends V> keyExtractor) {
        return supply(stream.sorted(Comparator.comparing(keyExtractor)));
    }

    public S sortedByInt(ToIntFunction<T> keyExtractor) {
        return supply(stream.sorted(Comparator.comparingInt(keyExtractor)));
    }

    public S sortedByLong(ToLongFunction<T> keyExtractor) {
        return supply(stream.sorted(Comparator.comparingLong(keyExtractor)));
    }

    public S sortedByDouble(ToDoubleFunction<T> keyExtractor) {
        return supply(stream.sorted(Comparator.comparingDouble(keyExtractor)));
    }

    public S append(Stream<T> other) {
        return supply(Stream.concat(stream, other));
    }

    public S prepend(Stream<T> other) {
        return supply(Stream.concat(other, stream));
    }
}
