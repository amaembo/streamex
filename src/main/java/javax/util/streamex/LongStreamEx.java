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
import java.util.Comparator;
import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.Random;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.BiConsumer;
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
import java.util.stream.LongStream;

/**
 * A {@link LongStream} implementation with additional functionality
 * 
 * @author Tagir Valeev
 */
public class LongStreamEx implements LongStream {
    private static final LongStreamEx EMPTY = new LongStreamEx(LongStream.empty());

    private final LongStream stream;

    LongStreamEx(LongStream stream) {
        this.stream = stream;
    }

    @Override
    public boolean isParallel() {
        return stream.isParallel();
    }

    @Override
    public LongStreamEx unordered() {
        return new LongStreamEx(stream.unordered());
    }

    @Override
    public LongStreamEx onClose(Runnable closeHandler) {
        return new LongStreamEx(stream.onClose(closeHandler));
    }

    @Override
    public void close() {
        stream.close();
    }

    @Override
    public LongStreamEx filter(LongPredicate predicate) {
        return new LongStreamEx(stream.filter(predicate));
    }

    @Override
    public LongStreamEx map(LongUnaryOperator mapper) {
        return new LongStreamEx(stream.map(mapper));
    }

    @Override
    public <U> StreamEx<U> mapToObj(LongFunction<? extends U> mapper) {
        return new StreamEx<>(stream.mapToObj(mapper));
    }

    @Override
    public IntStreamEx mapToInt(LongToIntFunction mapper) {
        return new IntStreamEx(stream.mapToInt(mapper));
    }

    @Override
    public DoubleStreamEx mapToDouble(LongToDoubleFunction mapper) {
        return new DoubleStreamEx(stream.mapToDouble(mapper));
    }

    @Override
    public LongStreamEx flatMap(LongFunction<? extends LongStream> mapper) {
        return new LongStreamEx(stream.flatMap(mapper));
    }

    @Override
    public LongStreamEx distinct() {
        return new LongStreamEx(stream.distinct());
    }

    /**
     * Returns a stream consisting of the elements of this stream in sorted
     * order.
     *
     * <p>This is a stateful intermediate operation.
     *
     * @return the new stream
     */
    @Override
    public LongStreamEx sorted() {
        return new LongStreamEx(stream.sorted());
    }

    @Override
    public LongStreamEx peek(LongConsumer action) {
        return new LongStreamEx(stream.peek(action));
    }

    @Override
    public LongStreamEx limit(long maxSize) {
        return new LongStreamEx(stream.limit(maxSize));
    }

    @Override
    public LongStreamEx skip(long n) {
        return new LongStreamEx(stream.skip(n));
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

    @Override
    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return stream.collect(supplier, accumulator, combiner);
    }

    @Override
    public long sum() {
        return stream.sum();
    }

    @Override
    public OptionalLong min() {
        return stream.min();
    }

    @Override
    public OptionalLong max() {
        return stream.max();
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
        return stream.summaryStatistics();
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
        return stream.noneMatch(predicate);
    }

    @Override
    public OptionalLong findFirst() {
        return stream.findFirst();
    }

    @Override
    public OptionalLong findAny() {
        return stream.findAny();
    }

    @Override
    public DoubleStreamEx asDoubleStream() {
        return new DoubleStreamEx(stream.asDoubleStream());
    }

    @Override
    public StreamEx<Long> boxed() {
        return new StreamEx<>(stream.boxed());
    }

    @Override
    public LongStreamEx sequential() {
        return new LongStreamEx(stream.sequential());
    }

    @Override
    public LongStreamEx parallel() {
        return new LongStreamEx(stream.parallel());
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
     * Returns a new {@code LongStreamEx} which is a concatenation of this stream
     * and the stream containing supplied values
     * 
     * @param values the values to append to the stream
     * @return the new stream
     */
    public LongStreamEx append(long... values) {
        return new LongStreamEx(LongStream.concat(stream, LongStream.of(values)));
    }

    public LongStreamEx append(LongStream other) {
        return new LongStreamEx(LongStream.concat(stream, other));
    }

    /**
     * Returns a new {@code LongStreamEx} which is a concatenation of
     * the stream containing supplied values and this stream
     *  
     * @param values the values to prepend to the stream
     * @return the new stream
     */
    public LongStreamEx prepend(long... values) {
        return new LongStreamEx(LongStream.concat(LongStream.of(values), stream));
    }

    public LongStreamEx prepend(LongStream other) {
        return new LongStreamEx(LongStream.concat(other, stream));
    }

    public LongStreamEx remove(LongPredicate predicate) {
        return new LongStreamEx(stream.filter(predicate.negate()));
    }

    public OptionalLong findAny(LongPredicate predicate) {
        return stream.filter(predicate).findAny();
    }

    public OptionalLong findFirst(LongPredicate predicate) {
        return stream.filter(predicate).findFirst();
    }

    /**
     * Returns true if this stream contains the specified value
     *
     * <p>This is a short-circuiting terminal operation.
     * 
     * @param value the value too look for in the stream
     * @return true if this stream contains the specified value
     * @see LongStream#anyMatch(LongPredicate)
     */
    public boolean has(long value) {
        return stream.anyMatch(x -> x == value);
    }

    public LongStreamEx sorted(Comparator<Long> comparator) {
        return new LongStreamEx(stream.boxed().sorted(comparator).mapToLong(Long::longValue));
    }

    /**
     * Returns a stream consisting of the elements of this stream in reverse 
     * sorted order.
     *
     * <p>This is a stateful intermediate operation.
     *
     * @return the new stream
     * @since 0.0.8
     */
    public LongStreamEx reverseSorted() {
        return sorted((a, b) -> b.compareTo(a));
    }
    
    public <V extends Comparable<? super V>> LongStreamEx sortedBy(LongFunction<V> keyExtractor) {
        return new LongStreamEx(stream.boxed().sorted(Comparator.comparing(i -> keyExtractor.apply(i)))
                .mapToLong(Long::longValue));
    }

    public LongStreamEx sortedByInt(LongToIntFunction keyExtractor) {
        return new LongStreamEx(stream.boxed().sorted(Comparator.comparingInt(i -> keyExtractor.applyAsInt(i)))
                .mapToLong(Long::longValue));
    }

    public LongStreamEx sortedByLong(LongUnaryOperator keyExtractor) {
        return new LongStreamEx(stream.boxed().sorted(Comparator.comparingLong(i -> keyExtractor.applyAsLong(i)))
                .mapToLong(Long::longValue));
    }

    public LongStreamEx sortedByDouble(LongToDoubleFunction keyExtractor) {
        return new LongStreamEx(stream.boxed().sorted(Comparator.comparingDouble(i -> keyExtractor.applyAsDouble(i)))
                .mapToLong(Long::longValue));
    }

    public static LongStreamEx empty() {
        return EMPTY;
    }

    /**
     * Returns a sequential {@code LongStreamEx} containing a single element.
     *
     * @param element the single element
     * @return a singleton sequential stream
     */
    public static LongStreamEx of(long element) {
        return new LongStreamEx(LongStream.of(element));
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} whose elements are the specified values.
     *
     * @param elements the elements of the new stream
     * @return the new stream
     */
    public static LongStreamEx of(long... elements) {
        return new LongStreamEx(LongStream.of(elements));
    }

    /**
     * Returns a {@code LongStreamEx} object which wraps given {@link LongStream}
     * @param stream original stream
     * @return the wrapped stream
     * @since 0.0.8
     */
    public static LongStreamEx of(LongStream stream) {
        return stream instanceof LongStreamEx ? (LongStreamEx) stream : new LongStreamEx(stream);
    }

    public static LongStreamEx of(Collection<Long> c) {
        return new LongStreamEx(c.stream().mapToLong(Long::longValue));
    }

    /**
     * Returns an effectively unlimited stream of pseudorandom {@code long}
     * values produced by given {@link Random} object.
     *
     * <p>A pseudorandom {@code long} value is generated as if it's the result of
     * calling the method {@link Random#nextLong()}.
     *
     * @param random a {@link Random} object to produce the stream from
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
     * Returns an infinite sequential ordered {@code LongStreamEx} produced by iterative
     * application of a function {@code f} to an initial element {@code seed},
     * producing a stream consisting of {@code seed}, {@code f(seed)},
     * {@code f(f(seed))}, etc.
     *
     * <p>The first element (position {@code 0}) in the {@code LongStreamEx} will be
     * the provided {@code seed}.  For {@code n > 0}, the element at position
     * {@code n}, will be the result of applying the function {@code f} to the
     * element at position {@code n - 1}.
     *
     * @param seed the initial element
     * @param f a function to be applied to to the previous element to produce
     *          a new element
     * @return A new sequential {@code LongStream}
     * @see LongStream#iterate(long, LongUnaryOperator)
     */
    public static LongStreamEx iterate(final long seed, final LongUnaryOperator f) {
        return new LongStreamEx(LongStream.iterate(seed, f));
    }

    /**
     * Returns an infinite sequential unordered stream where each element is
     * generated by the provided {@code LongSupplier}.  This is suitable for
     * generating constant streams, streams of random elements, etc.
     *
     * @param s the {@code LongSupplier} for generated elements
     * @return a new infinite sequential unordered {@code LongStreamEx}
     * @see LongStream#generate(LongSupplier)
     */
    public static LongStreamEx generate(LongSupplier s) {
        return new LongStreamEx(LongStream.generate(s));
    }

    public static LongStreamEx range(long startInclusive, long endExclusive) {
        return new LongStreamEx(LongStream.range(startInclusive, endExclusive));
    }

    public static LongStreamEx rangeClosed(long startInclusive, long endInclusive) {
        return new LongStreamEx(LongStream.rangeClosed(startInclusive, endInclusive));
    }
}
