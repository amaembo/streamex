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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.PrimitiveIterator.OfDouble;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

/**
 * A {@link DoubleStream} implementation with additional functionality
 * 
 * @author Tagir Valeev
 */
public class DoubleStreamEx implements DoubleStream {
    final DoubleStream stream;

    DoubleStreamEx(DoubleStream stream) {
        this.stream = stream;
    }

    StreamManagingStrategy strategy() {
        return StreamManagingStrategy.DEFAULT;
    }
    
    /**
     * Returns whether this stream, if a terminal operation were to be executed,
     * would execute in parallel.  Calling this method after invoking an
     * terminal stream operation method may yield unpredictable results.
     *
     * @return {@code true} if this stream would execute in parallel if executed
     */
    @Override
    public boolean isParallel() {
        return stream.isParallel();
    }

    @Override
    public DoubleStreamEx unordered() {
        return strategy().newDoubleStreamEx(stream.unordered());
    }

    @Override
    public DoubleStreamEx onClose(Runnable closeHandler) {
        return strategy().newDoubleStreamEx(stream.onClose(closeHandler));
    }

    @Override
    public void close() {
        stream.close();
    }

    @Override
    public DoubleStreamEx filter(DoublePredicate predicate) {
        return strategy().newDoubleStreamEx(stream.filter(predicate));
    }

    /**
     * Returns a {@link DoubleStreamEx} consisting of the results of applying
     * the given function to the elements of this stream.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param mapper
     *            a non-interfering, stateless function to apply to each element
     * @return the new stream
     */
    @Override
    public DoubleStreamEx map(DoubleUnaryOperator mapper) {
        return strategy().newDoubleStreamEx(stream.map(mapper));
    }

    /**
     * Returns an object-valued {@link StreamEx} consisting of the results of
     * applying the given function to the elements of this stream.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param <U>
     *            the element type of the new stream
     * @param mapper
     *            a non-interfering, stateless function to apply to each element
     * @return the new stream
     */
    @Override
    public <U> StreamEx<U> mapToObj(DoubleFunction<? extends U> mapper) {
        return strategy().newStreamEx(stream.mapToObj(mapper));
    }

    /**
     * Returns an {@link IntStreamEx} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param mapper
     *            a non-interfering, stateless function to apply to each element
     * @return the new stream
     */
    @Override
    public IntStreamEx mapToInt(DoubleToIntFunction mapper) {
        return strategy().newIntStreamEx(stream.mapToInt(mapper));
    }

    /**
     * Returns a {@link LongStreamEx} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param mapper
     *            a non-interfering, stateless function to apply to each element
     * @return the new stream
     */
    @Override
    public LongStreamEx mapToLong(DoubleToLongFunction mapper) {
        return strategy().newLongStreamEx(stream.mapToLong(mapper));
    }

    @Override
    public DoubleStreamEx flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        return strategy().newDoubleStreamEx(stream.flatMap(mapper));
    }

    @Override
    public DoubleStreamEx distinct() {
        return strategy().newDoubleStreamEx(stream.distinct());
    }

    /**
     * Returns a stream consisting of the elements of this stream in sorted
     * order. The elements are compared for equality according to
     * {@link java.lang.Double#compare(double, double)}.
     *
     * <p>
     * This is a stateful intermediate operation.
     *
     * @return the new stream
     */
    @Override
    public DoubleStreamEx sorted() {
        return strategy().newDoubleStreamEx(stream.sorted());
    }

    @Override
    public DoubleStreamEx peek(DoubleConsumer action) {
        return strategy().newDoubleStreamEx(stream.peek(action));
    }

    @Override
    public DoubleStreamEx limit(long maxSize) {
        return strategy().newDoubleStreamEx(stream.limit(maxSize));
    }

    @Override
    public DoubleStreamEx skip(long n) {
        return strategy().newDoubleStreamEx(stream.skip(n));
    }

    @Override
    public void forEach(DoubleConsumer action) {
        stream.forEach(action);
    }

    @Override
    public void forEachOrdered(DoubleConsumer action) {
        stream.forEachOrdered(action);
    }

    @Override
    public double[] toArray() {
        return stream.toArray();
    }

    @Override
    public double reduce(double identity, DoubleBinaryOperator op) {
        return stream.reduce(identity, op);
    }

    @Override
    public OptionalDouble reduce(DoubleBinaryOperator op) {
        return stream.reduce(op);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return stream.collect(supplier, accumulator, combiner);
    }

    @Override
    public double sum() {
        return stream.sum();
    }

    @Override
    public OptionalDouble min() {
        return reduce(Math::min);
    }

    @Override
    public OptionalDouble max() {
        return reduce(Math::max);
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
    public DoubleSummaryStatistics summaryStatistics() {
        return collect(DoubleSummaryStatistics::new, DoubleSummaryStatistics::accept,
                DoubleSummaryStatistics::combine);
    }

    @Override
    public boolean anyMatch(DoublePredicate predicate) {
        return stream.anyMatch(predicate);
    }

    @Override
    public boolean allMatch(DoublePredicate predicate) {
        return stream.allMatch(predicate);
    }

    @Override
    public boolean noneMatch(DoublePredicate predicate) {
        return !anyMatch(predicate);
    }

    @Override
    public OptionalDouble findFirst() {
        return stream.findFirst();
    }

    @Override
    public OptionalDouble findAny() {
        return stream.findAny();
    }

    @Override
    public StreamEx<Double> boxed() {
        return strategy().newStreamEx(stream.boxed());
    }

    @Override
    public DoubleStreamEx sequential() {
        return StreamManagingStrategy.DEFAULT.newDoubleStreamEx(stream.sequential());
    }

    /**
     * Returns an equivalent stream that is parallel. May return itself, either
     * because the stream was already parallel, or because the underlying stream
     * state was modified to be parallel.
     *
     * <p>
     * This is an intermediate operation.
     * 
     * <p>
     * If this stream was created using {@link #parallel(ForkJoinPool)}, the new
     * stream forgets about supplied custom {@link ForkJoinPool} and its
     * terminal operation will be executed in common pool.
     *
     * @return a parallel stream
     */
    @Override
    public DoubleStreamEx parallel() {
        return StreamManagingStrategy.DEFAULT.newDoubleStreamEx(stream.parallel());
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
    public DoubleStreamEx parallel(ForkJoinPool fjp) {
        return StreamManagingStrategy.forCustomPool(fjp).newDoubleStreamEx(stream.parallel());
    }

    @Override
    public OfDouble iterator() {
        return stream.iterator();
    }

    @Override
    public java.util.Spliterator.OfDouble spliterator() {
        return stream.spliterator();
    }

    /**
     * Returns a new {@code DoubleStreamEx} which is a concatenation of this
     * stream and the stream containing supplied values
     * 
     * @param values
     *            the values to append to the stream
     * @return the new stream
     */
    public DoubleStreamEx append(double... values) {
        return strategy().newDoubleStreamEx(DoubleStream.concat(stream, DoubleStream.of(values)));
    }

    public DoubleStreamEx append(DoubleStream other) {
        return strategy().newDoubleStreamEx(DoubleStream.concat(stream, other));
    }

    /**
     * Returns a new {@code DoubleStreamEx} which is a concatenation of the
     * stream containing supplied values and this stream
     * 
     * @param values
     *            the values to prepend to the stream
     * @return the new stream
     */
    public DoubleStreamEx prepend(double... values) {
        return strategy().newDoubleStreamEx(DoubleStream.concat(DoubleStream.of(values), stream));
    }

    public DoubleStreamEx prepend(DoubleStream other) {
        return strategy().newDoubleStreamEx(DoubleStream.concat(other, stream));
    }

    public DoubleStreamEx remove(DoublePredicate predicate) {
        return filter(predicate.negate());
    }

    public OptionalDouble findAny(DoublePredicate predicate) {
        return filter(predicate).findAny();
    }

    public OptionalDouble findFirst(DoublePredicate predicate) {
        return filter(predicate).findFirst();
    }

    public DoubleStreamEx sorted(Comparator<Double> comparator) {
        return strategy().newDoubleStreamEx(stream.boxed().sorted(comparator).mapToDouble(Double::doubleValue));
    }

    /**
     * Returns a stream consisting of the elements of this stream in reverse
     * sorted order. The elements are compared for equality according to
     * {@link java.lang.Double#compare(double, double)}.
     *
     * <p>
     * This is a stateful intermediate operation.
     *
     * @return the new stream
     * @since 0.0.8
     */
    public DoubleStreamEx reverseSorted() {
        return sorted((a, b) -> b.compareTo(a));
    }

    public <V extends Comparable<? super V>> DoubleStreamEx sortedBy(DoubleFunction<V> keyExtractor) {
        return sorted(Comparator.comparing(i -> keyExtractor.apply(i)));
    }

    public DoubleStreamEx sortedByInt(DoubleToIntFunction keyExtractor) {
        return sorted(Comparator.comparingInt(i -> keyExtractor.applyAsInt(i)));
    }

    public DoubleStreamEx sortedByLong(DoubleToLongFunction keyExtractor) {
        return sorted(Comparator.comparingLong(i -> keyExtractor.applyAsLong(i)));
    }

    public DoubleStreamEx sortedByDouble(DoubleUnaryOperator keyExtractor) {
        return sorted(Comparator.comparingDouble(i -> keyExtractor.applyAsDouble(i)));
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
     * @return an {@code OptionalDouble} describing the minimum element of this
     *         stream, or an empty {@code OptionalDouble} if the stream is empty
     * @since 0.1.2
     */
    public OptionalDouble min(Comparator<Double> comparator) {
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
     * @return an {@code OptionalDouble} describing some element of this stream
     *         for which the lowest value was returned by key extractor, or an
     *         empty {@code OptionalDouble} if the stream is empty
     * @since 0.1.2
     */
    public <V extends Comparable<? super V>> OptionalDouble minBy(DoubleFunction<V> keyExtractor) {
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
     * @return an {@code OptionalDouble} describing some element of this stream
     *         for which the lowest value was returned by key extractor, or an
     *         empty {@code OptionalDouble} if the stream is empty
     * @since 0.1.2
     */
    public OptionalDouble minByInt(DoubleToIntFunction keyExtractor) {
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
     * @return an {@code OptionalDouble} describing some element of this stream
     *         for which the lowest value was returned by key extractor, or an
     *         empty {@code OptionalDouble} if the stream is empty
     * @since 0.1.2
     */
    public OptionalDouble minByLong(DoubleToLongFunction keyExtractor) {
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
     * @return an {@code OptionalDouble} describing some element of this stream
     *         for which the lowest value was returned by key extractor, or an
     *         empty {@code OptionalDouble} if the stream is empty
     * @since 0.1.2
     */
    public OptionalDouble minByDouble(DoubleUnaryOperator keyExtractor) {
        return reduce((a, b) -> Double.compare(keyExtractor.applyAsDouble(a), keyExtractor.applyAsDouble(b)) > 0 ? b : a);
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
     * @return an {@code OptionalDouble} describing the minimum element of this
     *         stream, or an empty {@code OptionalDouble} if the stream is empty
     */
    public OptionalDouble max(Comparator<Double> comparator) {
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
     * @return an {@code OptionalDouble} describing some element of this stream
     *         for which the highest value was returned by key extractor, or an
     *         empty {@code OptionalDouble} if the stream is empty
     * @since 0.1.2
     */
    public <V extends Comparable<? super V>> OptionalDouble maxBy(DoubleFunction<V> keyExtractor) {
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
     * @return an {@code OptionalDouble} describing some element of this stream
     *         for which the highest value was returned by key extractor, or an
     *         empty {@code OptionalDouble} if the stream is empty
     * @since 0.1.2
     */
    public OptionalDouble maxByInt(DoubleToIntFunction keyExtractor) {
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
     * @return an {@code OptionalDouble} describing some element of this stream
     *         for which the highest value was returned by key extractor, or an
     *         empty {@code OptionalDouble} if the stream is empty
     * @since 0.1.2
     */
    public OptionalDouble maxByLong(DoubleToLongFunction keyExtractor) {
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
     * @return an {@code OptionalDouble} describing some element of this stream
     *         for which the highest value was returned by key extractor, or an
     *         empty {@code OptionalDouble} if the stream is empty
     * @since 0.1.2
     */
    public OptionalDouble maxByDouble(DoubleUnaryOperator keyExtractor) {
        return reduce((a, b) -> Double.compare(keyExtractor.applyAsDouble(a), keyExtractor.applyAsDouble(b)) > 0 ? a : b);
    }

    public static DoubleStreamEx empty() {
        return new DoubleStreamEx(DoubleStream.empty());
    }

    /**
     * Returns a sequential {@code DoubleStreamEx} containing a single element.
     *
     * @param element
     *            the single element
     * @return a singleton sequential stream
     */
    public static DoubleStreamEx of(double element) {
        return new DoubleStreamEx(DoubleStream.of(element));
    }

    /**
     * Returns a sequential ordered {@code DoubleStreamEx} whose elements are
     * the specified values.
     *
     * @param elements
     *            the elements of the new stream
     * @return the new stream
     */
    public static DoubleStreamEx of(double... elements) {
        return new DoubleStreamEx(DoubleStream.of(elements));
    }

    /**
     * Returns a sequential {@link DoubleStreamEx} with the specified range of
     * the specified array as its source.
     *
     * @param array
     *            the array, assumed to be unmodified during use
     * @param startInclusive
     *            the first index to cover, inclusive
     * @param endExclusive
     *            index immediately past the last index to cover
     * @return an {@code DoubleStreamEx} for the array range
     * @throws ArrayIndexOutOfBoundsException
     *             if {@code startInclusive} is negative, {@code endExclusive}
     *             is less than {@code startInclusive}, or {@code endExclusive}
     *             is greater than the array size
     * @since 0.1.1
     * @see Arrays#stream(double[], int, int)
     */
    public static DoubleStreamEx of(double[] array, int startInclusive, int endExclusive) {
        return new DoubleStreamEx(Arrays.stream(array, startInclusive, endExclusive));
    }

    /**
     * Returns a {@code DoubleStreamEx} object which wraps given
     * {@link DoubleStream}
     * 
     * @param stream
     *            original stream
     * @return the wrapped stream
     * @since 0.0.8
     */
    public static DoubleStreamEx of(DoubleStream stream) {
        return stream instanceof DoubleStreamEx ? (DoubleStreamEx) stream : new DoubleStreamEx(stream);
    }

    /**
     * Returns a sequential {@code DoubleStreamEx} containing an
     * {@link OptionalDouble} value, if present, otherwise returns an empty
     * {@code DoubleStreamEx}.
     *
     * @param optional
     *            the optional to create a stream of
     * @return a stream with an {@code OptionalDouble} value if present,
     *         otherwise an empty stream
     * @since 0.1.1
     */
    public static DoubleStreamEx of(OptionalDouble optional) {
        return optional.isPresent() ? of(optional.getAsDouble()) : empty();
    }

    public static DoubleStreamEx of(Collection<Double> c) {
        return new DoubleStreamEx(c.stream().mapToDouble(Double::doubleValue));
    }

    /**
     * Returns an effectively unlimited stream of pseudorandom {@code double}
     * values, each between zero (inclusive) and one (exclusive) produced by
     * given {@link Random} object.
     *
     * <p>
     * A pseudorandom {@code double} value is generated as if it's the result of
     * calling the method {@link Random#nextDouble()}.
     *
     * @param random
     *            a {@link Random} object to produce the stream from
     * @return a stream of pseudorandom {@code double} values
     * @see Random#doubles()
     */
    public static DoubleStreamEx of(Random random) {
        return new DoubleStreamEx(random.doubles());
    }

    public static DoubleStreamEx of(Random random, long streamSize) {
        return new DoubleStreamEx(random.doubles(streamSize));
    }

    public static DoubleStreamEx of(Random random, double randomNumberOrigin, double randomNumberBound) {
        return new DoubleStreamEx(random.doubles(randomNumberOrigin, randomNumberBound));
    }

    public static DoubleStreamEx of(Random random, long streamSize, double randomNumberOrigin, double randomNumberBound) {
        return new DoubleStreamEx(random.doubles(streamSize, randomNumberOrigin, randomNumberBound));
    }

    /**
     * Returns an infinite sequential ordered {@code DoubleStreamEx} produced by
     * iterative application of a function {@code f} to an initial element
     * {@code seed}, producing a stream consisting of {@code seed},
     * {@code f(seed)}, {@code f(f(seed))}, etc.
     *
     * <p>
     * The first element (position {@code 0}) in the {@code DoubleStreamEx} will
     * be the provided {@code seed}. For {@code n > 0}, the element at position
     * {@code n}, will be the result of applying the function {@code f} to the
     * element at position {@code n - 1}.
     *
     * @param seed
     *            the initial element
     * @param f
     *            a function to be applied to to the previous element to produce
     *            a new element
     * @return A new sequential {@code DoubleStream}
     * @see DoubleStream#iterate(double, DoubleUnaryOperator)
     */
    public static DoubleStreamEx iterate(final double seed, final DoubleUnaryOperator f) {
        return new DoubleStreamEx(DoubleStream.iterate(seed, f));
    }

    /**
     * Returns an infinite sequential unordered stream where each element is
     * generated by the provided {@code DoubleSupplier}. This is suitable for
     * generating constant streams, streams of random elements, etc.
     *
     * @param s
     *            the {@code DoubleSupplier} for generated elements
     * @return a new infinite sequential unordered {@code DoubleStreamEx}
     * @see DoubleStream#generate(DoubleSupplier)
     */
    public static DoubleStreamEx generate(DoubleSupplier s) {
        return new DoubleStreamEx(DoubleStream.generate(s));
    }

    /**
     * Returns a sequential unordered {@code DoubleStreamEx} of given length
     * which elements are equal to supplied value.
     * 
     * @param value
     *            the constant value
     * @param length
     *            the length of the stream
     * @return a new {@code DoubleStreamEx}
     * @since 0.1.2
     */
    public static DoubleStreamEx constant(double value, long length) {
        return new DoubleStreamEx(DoubleStream.generate(() -> value).limit(length));
    }
}
