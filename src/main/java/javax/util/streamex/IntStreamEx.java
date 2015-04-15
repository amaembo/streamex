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

import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * An {@link IntStream} implementation with additional functionality
 * 
 * @author Tagir Valeev
 */
public class IntStreamEx implements IntStream {
    private static final IntStreamEx EMPTY = new IntStreamEx(IntStream.empty());

    private final IntStream stream;

    IntStreamEx(IntStream stream) {
        this.stream = stream;
    }

    @Override
    public boolean isParallel() {
        return stream.isParallel();
    }

    @Override
    public IntStreamEx unordered() {
        return new IntStreamEx(stream.unordered());
    }

    @Override
    public IntStreamEx onClose(Runnable closeHandler) {
        return new IntStreamEx(stream.onClose(closeHandler));
    }

    @Override
    public void close() {
        stream.close();
    }

    @Override
    public IntStreamEx filter(IntPredicate predicate) {
        return new IntStreamEx(stream.filter(predicate));
    }

    @Override
    public IntStreamEx map(IntUnaryOperator mapper) {
        return new IntStreamEx(stream.map(mapper));
    }

    @Override
    public <U> StreamEx<U> mapToObj(IntFunction<? extends U> mapper) {
        return new StreamEx<>(stream.mapToObj(mapper));
    }

    @Override
    public LongStreamEx mapToLong(IntToLongFunction mapper) {
        return new LongStreamEx(stream.mapToLong(mapper));
    }

    @Override
    public DoubleStreamEx mapToDouble(IntToDoubleFunction mapper) {
        return new DoubleStreamEx(stream.mapToDouble(mapper));
    }

    @Override
    public IntStreamEx flatMap(IntFunction<? extends IntStream> mapper) {
        return new IntStreamEx(stream.flatMap(mapper));
    }

    @Override
    public IntStreamEx distinct() {
        return new IntStreamEx(stream.distinct());
    }

    @Override
    public IntStreamEx sorted() {
        return new IntStreamEx(stream.sorted());
    }

    @Override
    public IntStreamEx peek(IntConsumer action) {
        return new IntStreamEx(stream.peek(action));
    }

    @Override
    public IntStreamEx limit(long maxSize) {
        return new IntStreamEx(stream.limit(maxSize));
    }

    @Override
    public IntStreamEx skip(long n) {
        return new IntStreamEx(stream.skip(n));
    }

    @Override
    public void forEach(IntConsumer action) {
        stream.forEach(action);
    }

    @Override
    public void forEachOrdered(IntConsumer action) {
        stream.forEachOrdered(action);
    }

    @Override
    public int[] toArray() {
        return stream.toArray();
    }

    @Override
    public int reduce(int identity, IntBinaryOperator op) {
        return stream.reduce(identity, op);
    }

    @Override
    public OptionalInt reduce(IntBinaryOperator op) {
        return stream.reduce(op);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return stream.collect(supplier, accumulator, combiner);
    }

    @Override
    public int sum() {
        return stream.sum();
    }

    @Override
    public OptionalInt min() {
        return stream.min();
    }

    @Override
    public OptionalInt max() {
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
    public IntSummaryStatistics summaryStatistics() {
        return stream.summaryStatistics();
    }

    @Override
    public boolean anyMatch(IntPredicate predicate) {
        return stream.anyMatch(predicate);
    }

    @Override
    public boolean allMatch(IntPredicate predicate) {
        return stream.allMatch(predicate);
    }

    @Override
    public boolean noneMatch(IntPredicate predicate) {
        return stream.noneMatch(predicate);
    }

    @Override
    public OptionalInt findFirst() {
        return stream.findFirst();
    }

    @Override
    public OptionalInt findAny() {
        return stream.findAny();
    }

    @Override
    public LongStreamEx asLongStream() {
        return new LongStreamEx(stream.asLongStream());
    }

    @Override
    public DoubleStreamEx asDoubleStream() {
        return new DoubleStreamEx(stream.asDoubleStream());
    }

    @Override
    public StreamEx<Integer> boxed() {
        return new StreamEx<>(stream.boxed());
    }

    @Override
    public IntStreamEx sequential() {
        return new IntStreamEx(stream.sequential());
    }

    @Override
    public IntStreamEx parallel() {
        return new IntStreamEx(stream.parallel());
    }

    @Override
    public OfInt iterator() {
        return stream.iterator();
    }

    @Override
    public java.util.Spliterator.OfInt spliterator() {
        return stream.spliterator();
    }

    public IntStreamEx append(int... values) {
        return new IntStreamEx(IntStream.concat(stream, IntStream.of(values)));
    }

    public IntStreamEx append(IntStream other) {
        return new IntStreamEx(IntStream.concat(stream, other));
    }

    public IntStreamEx prepend(int... values) {
        return new IntStreamEx(IntStream.concat(IntStream.of(values), stream));
    }

    public IntStreamEx prepend(IntStream other) {
        return new IntStreamEx(IntStream.concat(other, stream));
    }

    public IntStreamEx remove(IntPredicate predicate) {
        return new IntStreamEx(stream.filter(predicate.negate()));
    }

    public OptionalInt findAny(IntPredicate predicate) {
        return stream.filter(predicate).findAny();
    }

    public OptionalInt findFirst(IntPredicate predicate) {
        return stream.filter(predicate).findFirst();
    }

    public boolean has(int value) {
        return stream.anyMatch(x -> x == value);
    }

    public IntStreamEx sorted(Comparator<Integer> comparator) {
        return new IntStreamEx(stream.boxed().sorted(comparator).mapToInt(Integer::intValue));
    }

    public <V extends Comparable<? super V>> IntStreamEx sortedBy(IntFunction<V> keyExtractor) {
        return new IntStreamEx(stream.boxed().sorted(Comparator.comparing(i -> keyExtractor.apply(i)))
                .mapToInt(Integer::intValue));
    }

    public IntStreamEx sortedByInt(IntUnaryOperator keyExtractor) {
        return new IntStreamEx(stream.boxed().sorted(Comparator.comparingInt(i -> keyExtractor.applyAsInt(i)))
                .mapToInt(Integer::intValue));
    }

    public IntStreamEx sortedByLong(IntToLongFunction keyExtractor) {
        return new IntStreamEx(stream.boxed().sorted(Comparator.comparingLong(i -> keyExtractor.applyAsLong(i)))
                .mapToInt(Integer::intValue));
    }

    public IntStreamEx sortedByDouble(IntToDoubleFunction keyExtractor) {
        return new IntStreamEx(stream.boxed().sorted(Comparator.comparingDouble(i -> keyExtractor.applyAsDouble(i)))
                .mapToInt(Integer::intValue));
    }

    public static IntStreamEx empty() {
        return EMPTY;
    }

    /**
     * Returns a sequential {@code IntStreamEx} containing a single element.
     *
     * @param element the single element
     * @return a singleton sequential stream
     */
    public static IntStreamEx of(int element) {
        return new IntStreamEx(IntStream.of(element));
    }

    /**
     * Returns a sequential ordered {@code IntStreamEx} whose elements are the specified values.
     *
     * @param elements the elements of the new stream
     * @return the new stream
     */
    public static IntStreamEx of(int... elements) {
        return new IntStreamEx(IntStream.of(elements));
    }

    /**
     * Returns an {@code IntStreamEx} object which wraps given {@link IntStream}
     * @param stream original stream
     * @return the wrapped stream
     * @since 0.0.8
     */
    public static IntStreamEx of(IntStream stream) {
        return stream instanceof IntStreamEx ? (IntStreamEx) stream : new IntStreamEx(stream);
    }

    /**
     * Returns an {@code IntStreamEx} of indices for which the specified {@link BitSet}
     * contains a bit in the set state. The indices are returned
     * in order, from lowest to highest. The size of the stream
     * is the number of bits in the set state, equal to the value
     * returned by the {@link BitSet#cardinality()} method.
     *
     * <p>The bit set must remain constant during the execution of the
     * terminal stream operation.  Otherwise, the result of the terminal
     * stream operation is undefined.
     *
     * @param bitSet a {@link BitSet} to produce the stream from
     * @return a stream of integers representing set indices
     * @see BitSet#stream()
     */
    public static IntStreamEx of(BitSet bitSet) {
        return new IntStreamEx(bitSet.stream());
    }

    /**
     * Returns an {@code IntStreamEx} containing primitive
     * integers from given collection
     *
     * @param c a collection to produce the stream from
     * @return the new stream
     * @see Collection#stream()
     */
    public static IntStreamEx of(Collection<Integer> c) {
        return new IntStreamEx(c.stream().mapToInt(Integer::intValue));
    }

    /**
     * Returns an effectively unlimited stream of pseudorandom {@code int}
     * values produced by given {@link Random} object.
     *
     * <p>A pseudorandom {@code int} value is generated as if it's the result of
     * calling the method {@link Random#nextInt()}.
     *
     * @param random a {@link Random} object to produce the stream from
     * @return a stream of pseudorandom {@code int} values
     * @see Random#ints()
     */
    public static IntStreamEx of(Random random) {
        return new IntStreamEx(random.ints());
    }

    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code int} values.
     *
     * <p>A pseudorandom {@code int} value is generated as if it's the result of
     * calling the method {@link Random#nextInt()}.
     *
     * @param random a {@link Random} object to produce the stream from
     * @param streamSize the number of values to generate
     * @return a stream of pseudorandom {@code int} values
     * @see Random#ints(long)
     */
    public static IntStreamEx of(Random random, long streamSize) {
        return new IntStreamEx(random.ints(streamSize));
    }

    public static IntStreamEx of(Random random, int randomNumberOrigin, int randomNumberBound) {
        return new IntStreamEx(random.ints(randomNumberOrigin, randomNumberBound));
    }

    public static IntStreamEx of(Random random, long streamSize, int randomNumberOrigin, int randomNumberBound) {
        return new IntStreamEx(random.ints(streamSize, randomNumberOrigin, randomNumberBound));
    }

    public static IntStreamEx ofChars(CharSequence seq) {
        return new IntStreamEx(seq.chars());
    }

    public static IntStreamEx ofCodePoints(CharSequence seq) {
        return new IntStreamEx(seq.codePoints());
    }

    public static IntStreamEx iterate(final int seed, final IntUnaryOperator f) {
        return new IntStreamEx(IntStream.iterate(seed, f));
    }

    public static IntStreamEx generate(IntSupplier s) {
        return new IntStreamEx(IntStream.generate(s));
    }

    public static IntStreamEx range(int startInclusive, int endExclusive) {
        return new IntStreamEx(IntStream.range(startInclusive, endExclusive));
    }

    public static IntStreamEx rangeClosed(int startInclusive, int endInclusive) {
        return new IntStreamEx(IntStream.rangeClosed(startInclusive, endInclusive));
    }
}
