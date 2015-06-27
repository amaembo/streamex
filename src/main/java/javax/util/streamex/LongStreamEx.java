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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Spliterator;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfLong;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static javax.util.streamex.StreamExInternals.*;

/**
 * A {@link LongStream} implementation with additional functionality
 * 
 * @author Tagir Valeev
 */
public class LongStreamEx implements LongStream {
    final LongStream stream;

    LongStreamEx(LongStream stream) {
        this.stream = stream;
    }

    StreamFactory strategy() {
        return StreamFactory.DEFAULT;
    }

    @Override
    public boolean isParallel() {
        return stream.isParallel();
    }

    @Override
    public LongStreamEx unordered() {
        return strategy().newLongStreamEx(stream.unordered());
    }

    @Override
    public LongStreamEx onClose(Runnable closeHandler) {
        return strategy().newLongStreamEx(stream.onClose(closeHandler));
    }

    @Override
    public void close() {
        stream.close();
    }

    @Override
    public LongStreamEx filter(LongPredicate predicate) {
        return strategy().newLongStreamEx(stream.filter(predicate));
    }

    /**
     * Returns a stream consisting of the elements of this stream that don't
     * match the given predicate.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param predicate
     *            a non-interfering, stateless predicate to apply to each
     *            element to determine if it should be excluded
     * @return the new stream
     */
    public LongStreamEx remove(LongPredicate predicate) {
        return filter(predicate.negate());
    }

    /**
     * Returns true if this stream contains the specified value
     *
     * <p>
     * This is a short-circuiting terminal operation.
     * 
     * @param value
     *            the value too look for in the stream
     * @return true if this stream contains the specified value
     * @see LongStream#anyMatch(LongPredicate)
     */
    public boolean has(long value) {
        return anyMatch(x -> x == value);
    }

    /**
     * Returns a stream consisting of the elements of this stream that don't
     * equal to the given value.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param value
     *            the value to remove from the stream.
     * @return the new stream
     * @since 0.2.2
     */
    public LongStreamEx without(long value) {
        return filter(val -> val != value);
    }

    /**
     * Returns a stream consisting of the elements of this stream that strictly
     * greater than the specified value.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param value
     *            a value to compare to
     * @return the new stream
     * @since 0.2.3
     */
    public LongStreamEx greater(long value) {
        return filter(val -> val > value);
    }

    /**
     * Returns a stream consisting of the elements of this stream that greater
     * than or equal to the specified value.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param value
     *            a value to compare to
     * @return the new stream
     * @since 0.2.3
     */
    public LongStreamEx atLeast(long value) {
        return filter(val -> val >= value);
    }

    /**
     * Returns a stream consisting of the elements of this stream that strictly
     * less than the specified value.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param value
     *            a value to compare to
     * @return the new stream
     * @since 0.2.3
     */
    public LongStreamEx less(long value) {
        return filter(val -> val < value);
    }

    /**
     * Returns a stream consisting of the elements of this stream that less than
     * or equal to the specified value.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param value
     *            a value to compare to
     * @return the new stream
     * @since 0.2.3
     */
    public LongStreamEx atMost(long value) {
        return filter(val -> val <= value);
    }

    @Override
    public LongStreamEx map(LongUnaryOperator mapper) {
        return strategy().newLongStreamEx(stream.map(mapper));
    }

    @Override
    public <U> StreamEx<U> mapToObj(LongFunction<? extends U> mapper) {
        return strategy().newStreamEx(stream.mapToObj(mapper));
    }

    @Override
    public IntStreamEx mapToInt(LongToIntFunction mapper) {
        return strategy().newIntStreamEx(stream.mapToInt(mapper));
    }

    @Override
    public DoubleStreamEx mapToDouble(LongToDoubleFunction mapper) {
        return strategy().newDoubleStreamEx(stream.mapToDouble(mapper));
    }

    /**
     * Returns an {@link EntryStream} consisting of the {@link Entry} objects
     * which keys and values are results of applying the given functions to the
     * elements of this stream.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param <K>
     *            The {@code Entry} key type
     * @param <V>
     *            The {@code Entry} value type
     * @param keyMapper
     *            a non-interfering, stateless function to apply to each element
     * @param valueMapper
     *            a non-interfering, stateless function to apply to each element
     * @return the new stream
     * @since 0.3.1
     */
    public <K, V> EntryStream<K, V> mapToEntry(LongFunction<? extends K> keyMapper,
            LongFunction<? extends V> valueMapper) {
        return strategy().newEntryStream(
            stream.mapToObj(t -> new AbstractMap.SimpleImmutableEntry<>(keyMapper.apply(t), valueMapper.apply(t))));
    }

    @Override
    public LongStreamEx flatMap(LongFunction<? extends LongStream> mapper) {
        return strategy().newLongStreamEx(stream.flatMap(mapper));
    }

    /**
     * Returns an {@link IntStreamEx} consisting of the results of replacing
     * each element of this stream with the contents of a mapped stream produced
     * by applying the provided mapping function to each element. Each mapped
     * stream is closed after its contents have been placed into this stream.
     * (If a mapped stream is {@code null} an empty stream is used, instead.)
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param mapper
     *            a non-interfering, stateless function to apply to each element
     *            which produces an {@code IntStream} of new values
     * @return the new stream
     * @since 0.3.0
     */
    public IntStreamEx flatMapToInt(LongFunction<? extends IntStream> mapper) {
        return strategy().newIntStreamEx(stream.mapToObj(mapper).flatMapToInt(Function.identity()));
    }

    /**
     * Returns a {@link DoubleStreamEx} consisting of the results of replacing
     * each element of this stream with the contents of a mapped stream produced
     * by applying the provided mapping function to each element. Each mapped
     * stream is closed after its contents have been placed into this stream.
     * (If a mapped stream is {@code null} an empty stream is used, instead.)
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param mapper
     *            a non-interfering, stateless function to apply to each element
     *            which produces an {@code DoubleStream} of new values
     * @return the new stream
     * @since 0.3.0
     */
    public DoubleStreamEx flatMapToDouble(LongFunction<? extends DoubleStream> mapper) {
        return strategy().newDoubleStreamEx(stream.mapToObj(mapper).flatMapToDouble(Function.identity()));
    }

    /**
     * Returns a {@link StreamEx} consisting of the results of replacing each
     * element of this stream with the contents of a mapped stream produced by
     * applying the provided mapping function to each element. Each mapped
     * stream is closed after its contents have been placed into this stream.
     * (If a mapped stream is {@code null} an empty stream is used, instead.)
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param <R>
     *            The element type of the new stream
     * @param mapper
     *            a non-interfering, stateless function to apply to each element
     *            which produces a {@code Stream} of new values
     * @return the new stream
     * @since 0.3.0
     */
    public <R> StreamEx<R> flatMapToObj(LongFunction<? extends Stream<R>> mapper) {
        return strategy().newStreamEx(stream.mapToObj(mapper).flatMap(Function.identity()));
    }

    @Override
    public LongStreamEx distinct() {
        return strategy().newLongStreamEx(stream.distinct());
    }

    @Override
    public LongStreamEx sorted() {
        return strategy().newLongStreamEx(stream.sorted());
    }

    public LongStreamEx sorted(Comparator<Long> comparator) {
        return strategy().newLongStreamEx(stream.boxed().sorted(comparator).mapToLong(Long::longValue));
    }

    /**
     * Returns a stream consisting of the elements of this stream in reverse
     * sorted order.
     *
     * <p>
     * This is a stateful intermediate operation.
     *
     * @return the new stream
     * @since 0.0.8
     */
    public LongStreamEx reverseSorted() {
        return sorted(Comparator.reverseOrder());
    }

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the natural order of the keys extracted by provided
     * function.
     *
     * <p>
     * For ordered streams, the sort is stable. For unordered streams, no
     * stability guarantees are made.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">stateful intermediate
     * operation</a>.
     *
     * @param <V>
     *            the type of the {@code Comparable} sort key
     * @param keyExtractor
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to be used to extract sorting keys
     * @return the new stream
     */
    public <V extends Comparable<? super V>> LongStreamEx sortedBy(LongFunction<V> keyExtractor) {
        return sorted(Comparator.comparing(i -> keyExtractor.apply(i)));
    }

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the int values extracted by provided function.
     *
     * <p>
     * For ordered streams, the sort is stable. For unordered streams, no
     * stability guarantees are made.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">stateful intermediate
     * operation</a>.
     *
     * @param keyExtractor
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to be used to extract sorting keys
     * @return the new stream
     */
    public LongStreamEx sortedByInt(LongToIntFunction keyExtractor) {
        return sorted(Comparator.comparingInt(i -> keyExtractor.applyAsInt(i)));
    }

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the long values extracted by provided function.
     *
     * <p>
     * For ordered streams, the sort is stable. For unordered streams, no
     * stability guarantees are made.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">stateful intermediate
     * operation</a>.
     *
     * @param keyExtractor
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to be used to extract sorting keys
     * @return the new stream
     */
    public LongStreamEx sortedByLong(LongUnaryOperator keyExtractor) {
        return sorted(Comparator.comparingLong(i -> keyExtractor.applyAsLong(i)));
    }

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the double values extracted by provided function.
     *
     * <p>
     * For ordered streams, the sort is stable. For unordered streams, no
     * stability guarantees are made.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">stateful intermediate
     * operation</a>.
     *
     * @param keyExtractor
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to be used to extract sorting keys
     * @return the new stream
     */
    public LongStreamEx sortedByDouble(LongToDoubleFunction keyExtractor) {
        return sorted(Comparator.comparingDouble(i -> keyExtractor.applyAsDouble(i)));
    }

    @Override
    public LongStreamEx peek(LongConsumer action) {
        return strategy().newLongStreamEx(stream.peek(action));
    }

    @Override
    public LongStreamEx limit(long maxSize) {
        return strategy().newLongStreamEx(stream.limit(maxSize));
    }

    @Override
    public LongStreamEx skip(long n) {
        return strategy().newLongStreamEx(stream.skip(n));
    }

    /**
     * Returns a stream consisting of the remaining elements of this stream
     * after discarding the first {@code n} elements of the stream. If this
     * stream contains fewer than {@code n} elements then an empty stream will
     * be returned.
     *
     * <p>
     * This is a stateful quasi-intermediate operation. Unlike
     * {@link #skip(long)} it skips the first elements even if the stream is
     * unordered. The main purpose of this method is to workaround the problem
     * of skipping the first elements from non-sized source with further
     * parallel processing and unordered terminal operation (such as
     * {@link #forEach(LongConsumer)}). Also it behaves much better with
     * infinite streams processed in parallel. For example,
     * {@code LongStreamEx.iterate(0L, i->i+1).skip(1).limit(100).parallel().toArray()}
     * will likely to fail with {@code OutOfMemoryError}, but will work nicely
     * if {@code skip} is replaced with {@code skipOrdered}.
     *
     * <p>
     * For sequential streams this method behaves exactly like
     * {@link #skip(long)}.
     *
     * @param n
     *            the number of leading elements to skip
     * @return the new stream
     * @throws IllegalArgumentException
     *             if {@code n} is negative
     * @see #skip(long)
     * @since 0.3.2
     */
    public LongStreamEx skipOrdered(long n) {
        LongStream result = stream.isParallel() ? StreamSupport.longStream(
            StreamSupport.longStream(stream.spliterator(), false).skip(n).spliterator(), true) : StreamSupport
                .longStream(stream.skip(n).spliterator(), false);
        return strategy().newLongStreamEx(result.onClose(stream::close));
    }

    @Override
    public void forEach(LongConsumer action) {
        stream.forEach(action);
    }

    @Override
    public void forEachOrdered(LongConsumer action) {
        stream.forEachOrdered(action);
    }

    @Override
    public long[] toArray() {
        return stream.toArray();
    }

    @Override
    public long reduce(long identity, LongBinaryOperator op) {
        return stream.reduce(identity, op);
    }

    @Override
    public OptionalLong reduce(LongBinaryOperator op) {
        return stream.reduce(op);
    }

    /**
     * {@inheritDoc}
     *
     * @see #collect(LongCollector)
     */
    @Override
    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return stream.collect(supplier, accumulator, combiner);
    }

    /**
     * Performs a mutable reduction operation on the elements of this stream
     * using an {@link LongCollector} which encapsulates the supplier,
     * accumulator and merger functions making easier to reuse collection
     * strategies.
     *
     * <p>
     * Like {@link #reduce(long, LongBinaryOperator)}, {@code collect}
     * operations can be parallelized without requiring additional
     * synchronization.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param <A>
     *            the intermediate accumulation type of the
     *            {@code LongCollector}
     * @param <R>
     *            type of the result
     * @param collector
     *            the {@code LongCollector} describing the reduction
     * @return the result of the reduction
     * @see #collect(Supplier, ObjLongConsumer, BiConsumer)
     * @since 0.3.0
     */
    @SuppressWarnings("unchecked")
    public <A, R> R collect(LongCollector<A, R> collector) {
        if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH))
            return (R) collect(collector.supplier(), collector.longAccumulator(), collector.merger());
        return collector.finisher().apply(
            collect(collector.supplier(), collector.longAccumulator(), collector.merger()));
    }

    @Override
    public long sum() {
        return reduce(0, Long::sum);
    }

    @Override
    public OptionalLong min() {
        return reduce(Long::min);
    }

    /**
     * Returns the minimum element of this stream according to the provided
     * {@code Comparator}.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param comparator
     *            a non-interfering, stateless {@link Comparator} to compare
     *            elements of this stream
     * @return an {@code OptionalLong} describing the minimum element of this
     *         stream, or an empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public OptionalLong min(Comparator<Long> comparator) {
        return reduce((a, b) -> comparator.compare(a, b) > 0 ? b : a);
    }

    /**
     * Returns the minimum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param <V>
     *            the type of the {@code Comparable} sort key
     * @param keyExtractor
     *            a non-interfering, stateless function
     * @return an {@code OptionalLong} describing some element of this stream
     *         for which the lowest value was returned by key extractor, or an
     *         empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public <V extends Comparable<? super V>> OptionalLong minBy(LongFunction<V> keyExtractor) {
        return reduce((a, b) -> keyExtractor.apply(a).compareTo(keyExtractor.apply(b)) > 0 ? b : a);
    }

    /**
     * Returns the minimum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param keyExtractor
     *            a non-interfering, stateless function
     * @return an {@code OptionalLong} describing some element of this stream
     *         for which the lowest value was returned by key extractor, or an
     *         empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public OptionalLong minByInt(LongToIntFunction keyExtractor) {
        return reduce((a, b) -> Integer.compare(keyExtractor.applyAsInt(a), keyExtractor.applyAsInt(b)) > 0 ? b : a);
    }

    /**
     * Returns the minimum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param keyExtractor
     *            a non-interfering, stateless function
     * @return an {@code OptionalLong} describing some element of this stream
     *         for which the lowest value was returned by key extractor, or an
     *         empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public OptionalLong minByLong(LongUnaryOperator keyExtractor) {
        return reduce((a, b) -> Long.compare(keyExtractor.applyAsLong(a), keyExtractor.applyAsLong(b)) > 0 ? b : a);
    }

    /**
     * Returns the minimum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param keyExtractor
     *            a non-interfering, stateless function
     * @return an {@code OptionalLong} describing some element of this stream
     *         for which the lowest value was returned by key extractor, or an
     *         empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public OptionalLong minByDouble(LongToDoubleFunction keyExtractor) {
        return reduce((a, b) -> Double.compare(keyExtractor.applyAsDouble(a), keyExtractor.applyAsDouble(b)) > 0 ? b
                : a);
    }

    @Override
    public OptionalLong max() {
        return reduce(Long::max);
    }

    /**
     * Returns the maximum element of this stream according to the provided
     * {@code Comparator}.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param comparator
     *            a non-interfering, stateless {@link Comparator} to compare
     *            elements of this stream
     * @return an {@code OptionalLong} describing the minimum element of this
     *         stream, or an empty {@code OptionalLong} if the stream is empty
     */
    public OptionalLong max(Comparator<Long> comparator) {
        return reduce((a, b) -> comparator.compare(a, b) > 0 ? a : b);
    }

    /**
     * Returns the maximum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param <V>
     *            the type of the {@code Comparable} sort key
     * @param keyExtractor
     *            a non-interfering, stateless function
     * @return an {@code OptionalLong} describing some element of this stream
     *         for which the highest value was returned by key extractor, or an
     *         empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public <V extends Comparable<? super V>> OptionalLong maxBy(LongFunction<V> keyExtractor) {
        return reduce((a, b) -> keyExtractor.apply(a).compareTo(keyExtractor.apply(b)) > 0 ? a : b);
    }

    /**
     * Returns the maximum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param keyExtractor
     *            a non-interfering, stateless function
     * @return an {@code OptionalLong} describing some element of this stream
     *         for which the highest value was returned by key extractor, or an
     *         empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public OptionalLong maxByInt(LongToIntFunction keyExtractor) {
        return reduce((a, b) -> Integer.compare(keyExtractor.applyAsInt(a), keyExtractor.applyAsInt(b)) > 0 ? a : b);
    }

    /**
     * Returns the maximum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param keyExtractor
     *            a non-interfering, stateless function
     * @return an {@code OptionalLong} describing some element of this stream
     *         for which the highest value was returned by key extractor, or an
     *         empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public OptionalLong maxByLong(LongUnaryOperator keyExtractor) {
        return reduce((a, b) -> Long.compare(keyExtractor.applyAsLong(a), keyExtractor.applyAsLong(b)) > 0 ? a : b);
    }

    /**
     * Returns the maximum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param keyExtractor
     *            a non-interfering, stateless function
     * @return an {@code OptionalLong} describing some element of this stream
     *         for which the highest value was returned by key extractor, or an
     *         empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public OptionalLong maxByDouble(LongToDoubleFunction keyExtractor) {
        return reduce((a, b) -> Double.compare(keyExtractor.applyAsDouble(a), keyExtractor.applyAsDouble(b)) > 0 ? a
                : b);
    }

    @Override
    public long count() {
        return stream.count();
    }

    @Override
    public OptionalDouble average() {
        return stream.average();
    }

    @Override
    public LongSummaryStatistics summaryStatistics() {
        return collect(LongSummaryStatistics::new, LongSummaryStatistics::accept, LongSummaryStatistics::combine);
    }

    @Override
    public boolean anyMatch(LongPredicate predicate) {
        return stream.anyMatch(predicate);
    }

    @Override
    public boolean allMatch(LongPredicate predicate) {
        return stream.allMatch(predicate);
    }

    @Override
    public boolean noneMatch(LongPredicate predicate) {
        return !anyMatch(predicate);
    }

    @Override
    public OptionalLong findFirst() {
        return stream.findFirst();
    }

    public OptionalLong findFirst(LongPredicate predicate) {
        return filter(predicate).findFirst();
    }

    @Override
    public OptionalLong findAny() {
        return stream.findAny();
    }

    public OptionalLong findAny(LongPredicate predicate) {
        return filter(predicate).findAny();
    }

    @Override
    public DoubleStreamEx asDoubleStream() {
        return strategy().newDoubleStreamEx(stream.asDoubleStream());
    }

    @Override
    public StreamEx<Long> boxed() {
        return strategy().newStreamEx(stream.boxed());
    }

    @Override
    public LongStreamEx sequential() {
        return StreamFactory.DEFAULT.newLongStreamEx(stream.sequential());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * If this stream was created using {@link #parallel(ForkJoinPool)}, the new
     * stream forgets about supplied custom {@link ForkJoinPool} and its
     * terminal operation will be executed in common pool.
     */
    @Override
    public LongStreamEx parallel() {
        return StreamFactory.DEFAULT.newLongStreamEx(stream.parallel());
    }

    /**
     * Returns an equivalent stream that is parallel and bound to the supplied
     * {@link ForkJoinPool}.
     *
     * <p>
     * This is an intermediate operation.
     * 
     * <p>
     * The terminal operation of this stream or any derived stream (except the
     * streams created via {@link #parallel()} or {@link #sequential()} methods)
     * will be executed inside the supplied {@code ForkJoinPool}. If current
     * thread does not belong to that pool, it will wait till calculation
     * finishes.
     *
     * @param fjp
     *            a {@code ForkJoinPool} to submit the stream operation to.
     * @return a parallel stream bound to the supplied {@code ForkJoinPool}
     * @since 0.2.0
     */
    public LongStreamEx parallel(ForkJoinPool fjp) {
        return StreamFactory.forCustomPool(fjp).newLongStreamEx(stream.parallel());
    }

    @Override
    public OfLong iterator() {
        return stream.iterator();
    }

    @Override
    public java.util.Spliterator.OfLong spliterator() {
        return stream.spliterator();
    }

    /**
     * Returns a new {@code LongStreamEx} which is a concatenation of this
     * stream and the stream containing supplied values
     * 
     * @param values
     *            the values to append to the stream
     * @return the new stream
     */
    public LongStreamEx append(long... values) {
        if (values.length == 0)
            return this;
        return strategy().newLongStreamEx(LongStream.concat(stream, LongStream.of(values)));
    }

    public LongStreamEx append(LongStream other) {
        return strategy().newLongStreamEx(LongStream.concat(stream, other));
    }

    /**
     * Returns a new {@code LongStreamEx} which is a concatenation of the stream
     * containing supplied values and this stream
     * 
     * @param values
     *            the values to prepend to the stream
     * @return the new stream
     */
    public LongStreamEx prepend(long... values) {
        if (values.length == 0)
            return this;
        return strategy().newLongStreamEx(LongStream.concat(LongStream.of(values), stream));
    }

    public LongStreamEx prepend(LongStream other) {
        return strategy().newLongStreamEx(LongStream.concat(other, stream));
    }

    /**
     * Returns a stream consisting of the results of applying the given function
     * to the every adjacent pair of elements of this stream.
     *
     * <p>
     * This is a quasi-intermediate operation.
     * 
     * <p>
     * The output stream will contain one element less than this stream. If this
     * stream contains zero or one element the output stream will be empty.
     *
     * @param mapper
     *            a non-interfering, stateless function to apply to each
     *            adjacent pair of this stream elements.
     * @return the new stream
     * @since 0.2.1
     */
    public LongStreamEx pairMap(LongBinaryOperator mapper) {
        return strategy().newLongStreamEx(
            StreamSupport.longStream(new PairSpliterator.PSOfLong(mapper, stream.spliterator(), 0, false, 0, false),
                stream.isParallel()).onClose(stream::close));
    }

    /**
     * Returns a {@link String} which contains the results of calling
     * {@link String#valueOf(long)} on each element of this stream, separated by
     * the specified delimiter, in encounter order.
     *
     * <p>
     * This is a terminal operation.
     * 
     * @param delimiter
     *            the delimiter to be used between each element
     * @return a {@code String}. For empty input stream empty String is
     *         returned.
     * @since 0.3.1
     */
    public String joining(CharSequence delimiter) {
        return collect(LongCollector.joining(delimiter));
    }

    /**
     * Returns a {@link String} which contains the results of calling
     * {@link String#valueOf(long)} on each element of this stream, separated by
     * the specified delimiter, with the specified prefix and suffix in
     * encounter order.
     *
     * <p>
     * This is a terminal operation.
     * 
     * @param delimiter
     *            the delimiter to be used between each element
     * @param prefix
     *            the sequence of characters to be used at the beginning of the
     *            joined result
     * @param suffix
     *            the sequence of characters to be used at the end of the joined
     *            result
     * @return a {@code String}. For empty input stream empty String is
     *         returned.
     * @since 0.3.1
     */
    public String joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return collect(LongCollector.joining(delimiter, prefix, suffix));
    }

    /**
     * Returns an empty sequential {@code LongStreamEx}.
     *
     * @return an empty sequential stream
     */
    public static LongStreamEx empty() {
        return new LongStreamEx(LongStream.empty());
    }

    /**
     * Returns a sequential {@code LongStreamEx} containing a single element.
     *
     * @param element
     *            the single element
     * @return a singleton sequential stream
     */
    public static LongStreamEx of(long element) {
        return new LongStreamEx(LongStream.of(element));
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} whose elements are the
     * specified values.
     *
     * @param elements
     *            the elements of the new stream
     * @return the new stream
     */
    public static LongStreamEx of(long... elements) {
        return new LongStreamEx(LongStream.of(elements));
    }

    /**
     * Returns a sequential {@link LongStreamEx} with the specified range of the
     * specified array as its source.
     *
     * @param array
     *            the array, assumed to be unmodified during use
     * @param startInclusive
     *            the first index to cover, inclusive
     * @param endExclusive
     *            index immediately past the last index to cover
     * @return an {@code LongStreamEx} for the array range
     * @throws ArrayIndexOutOfBoundsException
     *             if {@code startInclusive} is negative, {@code endExclusive}
     *             is less than {@code startInclusive}, or {@code endExclusive}
     *             is greater than the array size
     * @since 0.1.1
     * @see Arrays#stream(long[], int, int)
     */
    public static LongStreamEx of(long[] array, int startInclusive, int endExclusive) {
        return new LongStreamEx(Arrays.stream(array, startInclusive, endExclusive));
    }

    /**
     * Returns a {@code LongStreamEx} object which wraps given
     * {@link LongStream}
     * 
     * @param stream
     *            original stream
     * @return the wrapped stream
     * @since 0.0.8
     */
    public static LongStreamEx of(LongStream stream) {
        return stream instanceof LongStreamEx ? (LongStreamEx) stream : new LongStreamEx(stream);
    }

    public static LongStreamEx of(Spliterator.OfLong spliterator) {
        return new LongStreamEx(StreamSupport.longStream(spliterator, false));
    }

    /**
     * Returns a sequential {@code LongStreamEx} containing an
     * {@link OptionalLong} value, if present, otherwise returns an empty
     * {@code LongStreamEx}.
     *
     * @param optional
     *            the optional to create a stream of
     * @return a stream with an {@code OptionalLong} value if present, otherwise
     *         an empty stream
     * @since 0.1.1
     */
    public static LongStreamEx of(OptionalLong optional) {
        return optional.isPresent() ? of(optional.getAsLong()) : empty();
    }

    public static LongStreamEx of(Collection<Long> c) {
        return new LongStreamEx(c.stream().mapToLong(Long::longValue));
    }

    /**
     * Returns an effectively unlimited stream of pseudorandom {@code long}
     * values produced by given {@link Random} object.
     *
     * <p>
     * A pseudorandom {@code long} value is generated as if it's the result of
     * calling the method {@link Random#nextLong()}.
     *
     * @param random
     *            a {@link Random} object to produce the stream from
     * @return a stream of pseudorandom {@code long} values
     * @see Random#longs()
     */
    public static LongStreamEx of(Random random) {
        return new LongStreamEx(random.longs());
    }

    public static LongStreamEx of(Random random, long streamSize) {
        return new LongStreamEx(random.longs(streamSize));
    }

    public static LongStreamEx of(Random random, long randomNumberOrigin, long randomNumberBound) {
        return new LongStreamEx(random.longs(randomNumberOrigin, randomNumberBound));
    }

    public static LongStreamEx of(Random random, long streamSize, long randomNumberOrigin, long randomNumberBound) {
        return new LongStreamEx(random.longs(streamSize, randomNumberOrigin, randomNumberBound));
    }

    /**
     * Returns an infinite sequential ordered {@code LongStreamEx} produced by
     * iterative application of a function {@code f} to an initial element
     * {@code seed}, producing a stream consisting of {@code seed},
     * {@code f(seed)}, {@code f(f(seed))}, etc.
     *
     * <p>
     * The first element (position {@code 0}) in the {@code LongStreamEx} will
     * be the provided {@code seed}. For {@code n > 0}, the element at position
     * {@code n}, will be the result of applying the function {@code f} to the
     * element at position {@code n - 1}.
     *
     * @param seed
     *            the initial element
     * @param f
     *            a function to be applied to to the previous element to produce
     *            a new element
     * @return A new sequential {@code LongStream}
     * @see LongStream#iterate(long, LongUnaryOperator)
     */
    public static LongStreamEx iterate(final long seed, final LongUnaryOperator f) {
        return new LongStreamEx(LongStream.iterate(seed, f));
    }

    /**
     * Returns an infinite sequential unordered stream where each element is
     * generated by the provided {@code LongSupplier}. This is suitable for
     * generating constant streams, streams of random elements, etc.
     *
     * @param s
     *            the {@code LongSupplier} for generated elements
     * @return a new infinite sequential unordered {@code LongStreamEx}
     * @see LongStream#generate(LongSupplier)
     */
    public static LongStreamEx generate(LongSupplier s) {
        return new LongStreamEx(LongStream.generate(s));
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} from 0 (inclusive) to
     * {@code endExclusive} (exclusive) by an incremental step of {@code 1}.
     *
     * @param endExclusive
     *            the exclusive upper bound
     * @return a sequential {@code LongStreamEx} for the range of {@code int}
     *         elements
     * @see #range(long, long)
     * @since 0.1.1
     */
    public static LongStreamEx range(long endExclusive) {
        return new LongStreamEx(LongStream.range(0, endExclusive));
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} from
     * {@code startInclusive} (inclusive) to {@code endExclusive} (exclusive) by
     * an incremental step of {@code 1}.
     *
     * @param startInclusive
     *            the (inclusive) initial value
     * @param endExclusive
     *            the exclusive upper bound
     * @return a sequential {@code LongStreamEx} for the range of {@code long}
     *         elements
     * @see LongStream#range(long, long)
     */
    public static LongStreamEx range(long startInclusive, long endExclusive) {
        return new LongStreamEx(LongStream.range(startInclusive, endExclusive));
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} from
     * {@code startInclusive} (inclusive) to {@code endInclusive} (inclusive) by
     * an incremental step of {@code 1}.
     *
     * @param startInclusive
     *            the (inclusive) initial value
     * @param endInclusive
     *            the inclusive upper bound
     * @return a sequential {@code LongStreamEx} for the range of {@code long}
     *         elements
     * @see LongStream#rangeClosed(long, long)
     */
    public static LongStreamEx rangeClosed(long startInclusive, long endInclusive) {
        return new LongStreamEx(LongStream.rangeClosed(startInclusive, endInclusive));
    }

    /**
     * Returns a sequential unordered {@code LongStreamEx} of given length which
     * elements are equal to supplied value.
     * 
     * @param value
     *            the constant value
     * @param length
     *            the length of the stream
     * @return a new {@code LongStreamEx}
     * @since 0.1.2
     */
    public static LongStreamEx constant(long value, long length) {
        return new LongStreamEx(LongStream.generate(() -> value).limit(length));
    }

    /**
     * Returns a sequential {@code LongStreamEx} containing the results of
     * applying the given function to the corresponding pairs of values in given
     * two arrays.
     * 
     * @param first
     *            the first array
     * @param second
     *            the second array
     * @param mapper
     *            a non-interfering, stateless function to apply to each pair of
     *            the corresponding array elements.
     * @return a new {@code LongStreamEx}
     * @throws IllegalArgumentException
     *             if length of the arrays differs.
     * @since 0.2.1
     */
    public static LongStreamEx zip(long[] first, long[] second, LongBinaryOperator mapper) {
        return of(new RangeMapSpliterator.RMOfLong(0, checkLength(first.length, second.length),
                i -> mapper.applyAsLong(first[i], second[i])));
    }
}
