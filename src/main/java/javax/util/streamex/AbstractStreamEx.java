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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static javax.util.streamex.StreamExInternals.*;

/* package */abstract class AbstractStreamEx<T, S extends AbstractStreamEx<T, S>> implements Stream<T>, Iterable<T> {
    final Stream<T> stream;

    AbstractStreamEx(Stream<T> stream) {
        this.stream = stream;
    }

    StreamFactory strategy() {
        return StreamFactory.DEFAULT;
    }

    final <R> Stream<R> delegate(Spliterator<R> spliterator) {
        return StreamSupport.stream(spliterator, stream.isParallel()).onClose(stream::close);
    }

    final S callWhile(Predicate<? super T> predicate, int methodId) {
        try {
            return supply((Stream<T>) JDK9_METHODS[IDX_STREAM][methodId].invokeExact(stream, predicate));
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new InternalError(e);
        }
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
    public <R> StreamEx<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return strategy().newStreamEx(stream.flatMap(mapper));
    }

    @Override
    public <R> StreamEx<R> map(Function<? super T, ? extends R> mapper) {
        return strategy().newStreamEx(stream.map(mapper));
    }

    @Override
    public IntStreamEx mapToInt(ToIntFunction<? super T> mapper) {
        return strategy().newIntStreamEx(stream.mapToInt(mapper));
    }

    @Override
    public LongStreamEx mapToLong(ToLongFunction<? super T> mapper) {
        return strategy().newLongStreamEx(stream.mapToLong(mapper));
    }

    @Override
    public DoubleStreamEx mapToDouble(ToDoubleFunction<? super T> mapper) {
        return strategy().newDoubleStreamEx(stream.mapToDouble(mapper));
    }

    @Override
    public IntStreamEx flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return strategy().newIntStreamEx(stream.flatMapToInt(mapper));
    }

    @Override
    public LongStreamEx flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return strategy().newLongStreamEx(stream.flatMapToLong(mapper));
    }

    @Override
    public DoubleStreamEx flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return strategy().newDoubleStreamEx(stream.flatMapToDouble(mapper));
    }

    @Override
    public S distinct() {
        return supply(stream.distinct());
    }

    /**
     * Returns a stream consisting of the distinct elements of this stream
     * (according to object equality of the results of applying the given
     * function).
     *
     * <p>
     * For ordered streams, the selection of distinct elements is stable (for
     * duplicated elements, the element appearing first in the encounter order
     * is preserved.) For unordered streams, no stability guarantees are made.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">stateful intermediate
     * operation</a>.
     *
     * @param keyExtractor
     *            a non-interfering, stateless function which classifies input
     *            elements.
     * @return the new stream
     * @since 0.3.8
     */
    public S distinct(Function<? super T, ?> keyExtractor) {
        return supply(stream.map(t -> new PairBox<>(t, keyExtractor.apply(t))).distinct().map(box -> box.a));
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
        return toArray(Object[]::new);
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
        return reduce(BinaryOperator.minBy(comparator));
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        return reduce(BinaryOperator.maxBy(comparator));
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
        return !anyMatch(predicate);
    }

    @Override
    public Optional<T> findFirst() {
        return stream.findFirst();
    }

    @Override
    public Optional<T> findAny() {
        return stream.findAny();
    }

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped collection produced by applying
     * the provided mapping function to each element. (If a mapped collection is
     * {@code null} nothing is added for given element to the resulting stream.)
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * <p>
     * The {@code flatCollection()} operation has the effect of applying a
     * one-to-many transformation to the elements of the stream, and then
     * flattening the resulting elements into a new stream.
     *
     * @param <R>
     *            The element type of the new stream
     * @param mapper
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to apply to each element which produces a
     *            {@link Collection} of new values
     * @return the new stream
     */
    public <R> StreamEx<R> flatCollection(Function<? super T, ? extends Collection<? extends R>> mapper) {
        return flatMap(t -> {
            Collection<? extends R> c = mapper.apply(t);
            return c == null ? null : c.stream();
        });
    }

    /**
     * Returns a stream consisting of the elements of this stream that don't
     * match the given predicate.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param predicate
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            predicate to apply to each element to determine if it should
     *            be excluded
     * @return the new stream
     */
    public S remove(Predicate<? super T> predicate) {
        return filter(predicate.negate());
    }

    /**
     * Returns a stream consisting of the elements of this stream that aren't
     * null.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @return the new stream
     */
    public S nonNull() {
        return filter(Objects::nonNull);
    }

    /**
     * Returns an {@link Optional} describing some element of the stream, which
     * matches given predicate, or an empty {@code Optional} if there's no
     * matching element.
     *
     * <p>
     * This is a short-circuiting terminal operation.
     *
     * <p>
     * The behavior of this operation is explicitly nondeterministic; it is free
     * to select any element in the stream. This is to allow for maximal
     * performance in parallel operations; the cost is that multiple invocations
     * on the same source may not return the same result. (If a stable result is
     * desired, use {@link #findFirst(Predicate)} instead.)
     *
     * @param predicate
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            predicate which returned value should match
     * @return an {@code Optional} describing some element of this stream, or an
     *         empty {@code Optional} if the stream is empty
     * @throws NullPointerException
     *             if the element selected is null
     * @see #findAny()
     * @see #findFirst(Predicate)
     */
    public Optional<T> findAny(Predicate<? super T> predicate) {
        return filter(predicate).findAny();
    }

    /**
     * Returns an {@link Optional} describing the first element of this stream,
     * which matches given predicate, or an empty {@code Optional} if there's no
     * matching element.
     *
     * <p>
     * This is a short-circuiting terminal operation.
     *
     * @param predicate
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            predicate which returned value should match
     * @return an {@code Optional} describing the first element of this stream,
     *         or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException
     *             if the element selected is null
     * @see #findFirst()
     */
    public Optional<T> findFirst(Predicate<? super T> predicate) {
        return filter(predicate).findFirst();
    }

    /**
     * Returns a stream consisting of the elements of this stream, sorted in
     * descending order according to the provided {@code Comparator}.
     *
     * <p>
     * For ordered streams, the sort is stable. For unordered streams, no
     * stability guarantees are made.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">stateful intermediate
     * operation</a>.
     *
     * @param comparator
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            {@code Comparator} to be used to compare stream elements
     * @return the new stream
     */
    public S reverseSorted(Comparator<? super T> comparator) {
        return sorted(comparator.reversed());
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
    public <V extends Comparable<? super V>> S sortedBy(Function<? super T, ? extends V> keyExtractor) {
        return sorted(Comparator.comparing(keyExtractor));
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
    public S sortedByInt(ToIntFunction<? super T> keyExtractor) {
        return sorted(Comparator.comparingInt(keyExtractor));
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
    public S sortedByLong(ToLongFunction<? super T> keyExtractor) {
        return sorted(Comparator.comparingLong(keyExtractor));
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
    public S sortedByDouble(ToDoubleFunction<? super T> keyExtractor) {
        return sorted(Comparator.comparingDouble(keyExtractor));
    }

    /**
     * Returns the minimum element of this stream according to the natural order
     * of the keys extracted by provided function. This is a special case of a
     * reduction.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * This method is equivalent to
     * {@code min(Comparator.comparing(keyExtractor))}, but may work faster as
     * keyExtractor function is applied only once per each input element.
     *
     * @param <V>
     *            the type of the comparable keys
     * @param keyExtractor
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to extract the comparable keys from this stream
     *            elements
     * @return an {@code Optional} describing the minimum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException
     *             if the minimum element is null
     */
    public <V extends Comparable<? super V>> Optional<T> minBy(Function<? super T, ? extends V> keyExtractor) {
        return Box
                .asOptional(reduce(
                    null,
                    (PairBox<T, V> acc, T t) -> {
                        V val = keyExtractor.apply(t);
                        if (acc == null)
                            return new PairBox<>(t, val);
                        if (val.compareTo(acc.b) < 0) {
                            acc.b = val;
                            acc.a = t;
                        }
                        return acc;
                    },
                    (PairBox<T, V> acc1, PairBox<T, V> acc2) -> (acc1 == null || acc2 != null
                        && acc1.b.compareTo(acc2.b) > 0) ? acc2 : acc1));
    }

    /**
     * Returns the minimum element of this stream according to the int values
     * extracted by provided function. This is a special case of a reduction.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * This method is equivalent to
     * {@code min(Comparator.comparingInt(keyExtractor))}, but may work faster
     * as keyExtractor function is applied only once per each input element.
     *
     * @param keyExtractor
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to extract the int keys from this stream elements
     * @return an {@code Optional} describing the minimum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException
     *             if the minimum element is null
     */
    public Optional<T> minByInt(ToIntFunction<? super T> keyExtractor) {
        return Box.asOptional(reduce(null, (ObjIntBox<T> acc, T t) -> {
            int val = keyExtractor.applyAsInt(t);
            if (acc == null)
                return new ObjIntBox<>(t, val);
            if (val < acc.b) {
                acc.b = val;
                acc.a = t;
            }
            return acc;
        }, (ObjIntBox<T> acc1, ObjIntBox<T> acc2) -> (acc1 == null || acc2 != null && acc1.b > acc2.b) ? acc2 : acc1));
    }

    /**
     * Returns the minimum element of this stream according to the long values
     * extracted by provided function. This is a special case of a reduction.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * This method is equivalent to
     * {@code min(Comparator.comparingLong(keyExtractor))}, but may work faster
     * as keyExtractor function is applied only once per each input element.
     *
     * @param keyExtractor
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to extract the long keys from this stream elements
     * @return an {@code Optional} describing the minimum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException
     *             if the minimum element is null
     */
    public Optional<T> minByLong(ToLongFunction<? super T> keyExtractor) {
        return Box
                .asOptional(reduce(null, (ObjLongBox<T> acc, T t) -> {
                    long val = keyExtractor.applyAsLong(t);
                    if (acc == null)
                        return new ObjLongBox<>(t, val);
                    if (val < acc.b) {
                        acc.b = val;
                        acc.a = t;
                    }
                    return acc;
                }, (ObjLongBox<T> acc1, ObjLongBox<T> acc2) -> (acc1 == null || acc2 != null && acc1.b > acc2.b) ? acc2
                        : acc1));
    }

    /**
     * Returns the minimum element of this stream according to the double values
     * extracted by provided function. This is a special case of a reduction.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * This method is equivalent to
     * {@code min(Comparator.comparingDouble(keyExtractor))}, but may work
     * faster as keyExtractor function is applied only once per each input
     * element.
     *
     * @param keyExtractor
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to extract the double keys from this stream elements
     * @return an {@code Optional} describing the minimum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException
     *             if the minimum element is null
     */
    public Optional<T> minByDouble(ToDoubleFunction<? super T> keyExtractor) {
        return Box.asOptional(reduce(
            null,
            (ObjDoubleBox<T> acc, T t) -> {
                double val = keyExtractor.applyAsDouble(t);
                if (acc == null)
                    return new ObjDoubleBox<>(t, val);
                if (Double.compare(val, acc.b) < 0) {
                    acc.b = val;
                    acc.a = t;
                }
                return acc;
            },
            (ObjDoubleBox<T> acc1, ObjDoubleBox<T> acc2) -> (acc1 == null || acc2 != null
                && Double.compare(acc1.b, acc2.b) > 0) ? acc2 : acc1));
    }

    /**
     * Returns the maximum element of this stream according to the natural order
     * of the keys extracted by provided function. This is a special case of a
     * reduction.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * This method is equivalent to
     * {@code min(Comparator.comparing(keyExtractor))}, but may work faster as
     * keyExtractor function is applied only once per each input element.
     *
     * @param <V>
     *            the type of the comparable keys
     * @param keyExtractor
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to extract the comparable keys from this stream
     *            elements
     * @return an {@code Optional} describing the maximum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException
     *             if the maximum element is null
     */
    public <V extends Comparable<? super V>> Optional<T> maxBy(Function<? super T, ? extends V> keyExtractor) {
        return Box
                .asOptional(reduce(
                    null,
                    (PairBox<T, V> acc, T t) -> {
                        V val = keyExtractor.apply(t);
                        if (acc == null)
                            return new PairBox<>(t, val);
                        if (val.compareTo(acc.b) > 0) {
                            acc.b = val;
                            acc.a = t;
                        }
                        return acc;
                    },
                    (PairBox<T, V> acc1, PairBox<T, V> acc2) -> (acc1 == null || acc2 != null
                        && acc1.b.compareTo(acc2.b) < 0) ? acc2 : acc1));
    }

    /**
     * Returns the maximum element of this stream according to the int values
     * extracted by provided function. This is a special case of a reduction.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * This method is equivalent to
     * {@code min(Comparator.comparingInt(keyExtractor))}, but may work faster
     * as keyExtractor function is applied only once per each input element.
     *
     * @param keyExtractor
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to extract the int keys from this stream elements
     * @return an {@code Optional} describing the maximum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException
     *             if the maximum element is null
     */
    public Optional<T> maxByInt(ToIntFunction<? super T> keyExtractor) {
        return Box.asOptional(reduce(null, (ObjIntBox<T> acc, T t) -> {
            int val = keyExtractor.applyAsInt(t);
            if (acc == null)
                return new ObjIntBox<>(t, val);
            if (val > acc.b) {
                acc.b = val;
                acc.a = t;
            }
            return acc;
        }, (ObjIntBox<T> acc1, ObjIntBox<T> acc2) -> (acc1 == null || acc2 != null && acc1.b < acc2.b) ? acc2 : acc1));
    }

    /**
     * Returns the maximum element of this stream according to the long values
     * extracted by provided function. This is a special case of a reduction.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * This method is equivalent to
     * {@code min(Comparator.comparingLong(keyExtractor))}, but may work faster
     * as keyExtractor function is applied only once per each input element.
     *
     * @param keyExtractor
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to extract the long keys from this stream elements
     * @return an {@code Optional} describing the maximum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException
     *             if the maximum element is null
     */
    public Optional<T> maxByLong(ToLongFunction<? super T> keyExtractor) {
        return Box
                .asOptional(reduce(null, (ObjLongBox<T> acc, T t) -> {
                    long val = keyExtractor.applyAsLong(t);
                    if (acc == null)
                        return new ObjLongBox<>(t, val);
                    if (val > acc.b) {
                        acc.b = val;
                        acc.a = t;
                    }
                    return acc;
                }, (ObjLongBox<T> acc1, ObjLongBox<T> acc2) -> (acc1 == null || acc2 != null && acc1.b < acc2.b) ? acc2
                        : acc1));
    }

    /**
     * Returns the maximum element of this stream according to the double values
     * extracted by provided function. This is a special case of a reduction.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * This method is equivalent to
     * {@code min(Comparator.comparingDouble(keyExtractor))}, but may work
     * faster as keyExtractor function is applied only once per each input
     * element.
     *
     * @param keyExtractor
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to extract the double keys from this stream elements
     * @return an {@code Optional} describing the maximum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException
     *             if the maximum element is null
     */
    public Optional<T> maxByDouble(ToDoubleFunction<? super T> keyExtractor) {
        return Box.asOptional(reduce(
            null,
            (ObjDoubleBox<T> acc, T t) -> {
                double val = keyExtractor.applyAsDouble(t);
                if (acc == null)
                    return new ObjDoubleBox<>(t, val);
                if (Double.compare(val, acc.b) > 0) {
                    acc.b = val;
                    acc.a = t;
                }
                return acc;
            },
            (ObjDoubleBox<T> acc1, ObjDoubleBox<T> acc2) -> (acc1 == null || acc2 != null
                && Double.compare(acc1.b, acc2.b) < 0) ? acc2 : acc1));
    }

    /**
     * Creates a lazily concatenated stream whose elements are all the elements
     * of this stream followed by all the elements of the other stream. The
     * resulting stream is ordered if both of the input streams are ordered, and
     * parallel if either of the input streams is parallel. When the resulting
     * stream is closed, the close handlers for both input streams are invoked.
     *
     * @param other
     *            the other stream
     * @return this stream appended by the other stream
     * @see Stream#concat(Stream, Stream)
     */
    public S append(Stream<? extends T> other) {
        return supply(Stream.concat(stream, unwrap(other)));
    }

    /**
     * Creates a lazily concatenated stream whose elements are all the elements
     * of the other stream followed by all the elements of this stream. The
     * resulting stream is ordered if both of the input streams are ordered, and
     * parallel if either of the input streams is parallel. When the resulting
     * stream is closed, the close handlers for both input streams are invoked.
     *
     * @param other
     *            the other stream
     * @return this stream prepended by the other stream
     * @see Stream#concat(Stream, Stream)
     */
    public S prepend(Stream<? extends T> other) {
        return supply(Stream.concat(unwrap(other), stream));
    }

    /**
     * Returns a {@link List} containing the elements of this stream. There are
     * no guarantees on the type, mutability, serializability, or thread-safety
     * of the {@code List} returned; if more control over the returned
     * {@code List} is required, use {@link #toCollection(Supplier)}.
     *
     * <p>
     * This is a terminal operation.
     *
     * @return a {@code List} containing the elements of this stream
     * @see Collectors#toList()
     */
    public List<T> toList() {
        return collect(Collectors.toList());
    }

    /**
     * Collecting the stream producing a {@link List} containing all the stream
     * elements and performing an additional finishing transformation.
     * 
     * <p>
     * This is a terminal operation.
     *
     * @param <R>
     *            the result type
     * @param finisher
     *            a function to be applied to the intermediate list
     * @return result of applying the finisher transformation to the list of the
     *         stream elements.
     * @since 0.2.3
     */
    public <R> R toListAndThen(Function<List<T>, R> finisher) {
        return collect(Collectors.collectingAndThen(Collectors.toList(), finisher));
    }

    /**
     * Returns a {@link Set} containing the elements of this stream. There are
     * no guarantees on the type, mutability, serializability, or thread-safety
     * of the {@code Set} returned; if more control over the returned
     * {@code Set} is required, use {@link #toCollection(Supplier)}.
     *
     * <p>
     * This is a terminal operation.
     *
     * @return a {@code Set} containing the elements of this stream
     * @see Collectors#toSet()
     */
    public Set<T> toSet() {
        return collect(Collectors.toSet());
    }

    /**
     * Collecting the stream producing a {@link Set} containing all the stream
     * elements and performing an additional finishing transformation.
     * 
     * <p>
     * This is a terminal operation.
     *
     * @param <R>
     *            the result type
     * @param finisher
     *            a function to be applied to the intermediate set
     * @return result of applying the finisher transformation to the set of the
     *         stream elements.
     * @since 0.2.3
     */
    public <R> R toSetAndThen(Function<Set<T>, R> finisher) {
        return collect(Collectors.collectingAndThen(Collectors.toSet(), finisher));
    }

    /**
     * Returns a {@link Collection} containing the elements of this stream. The
     * {@code Collection} is created by the provided factory.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param <C>
     *            the type of the resulting {@code Collection}
     * @param collectionFactory
     *            a {@code Supplier} which returns a new, empty
     *            {@code Collection} of the appropriate type
     * @return a {@code Collection} containing the elements of this stream
     * @see Collectors#toCollection(Supplier)
     */
    public <C extends Collection<T>> C toCollection(Supplier<C> collectionFactory) {
        return collect(Collectors.toCollection(collectionFactory));
    }

    /**
     * Folds the elements of this stream using the provided identity object and
     * accumulation function, going left to right. This is equivalent to:
     * 
     * <pre>
     * {@code
     *     U result = identity;
     *     for (T element : this stream)
     *         result = accumulator.apply(result, element)
     *     return result;
     * }
     * </pre>
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * This method may work slowly on parallel streams as it must process
     * elements strictly left to right. If your accumulator function is
     * associative and you can provide a combiner function, consider using
     * {@link #reduce(Object, BiFunction, BinaryOperator)} method.
     * 
     * <p>
     * For parallel stream it's not guaranteed that accumulator will always be
     * executed in the same thread.
     *
     * @param <U>
     *            The type of the result
     * @param identity
     *            the identity value
     * @param accumulator
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function for incorporating an additional element into a result
     * @return the result of the folding
     * @see #foldRight(Object, BiFunction)
     * @see #reduce(Object, BinaryOperator)
     * @see #reduce(Object, BiFunction, BinaryOperator)
     * @since 0.2.0
     */
    public <U> U foldLeft(U identity, BiFunction<U, ? super T, U> accumulator) {
        Box<U> result = new Box<>(identity);
        forEachOrdered(t -> result.a = accumulator.apply(result.a, t));
        return result.a;
    }

    /**
     * Folds the elements of this stream using the provided identity object and
     * accumulation function, going right to left.
     * 
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * As this method must process elements strictly right to left, it cannot
     * start processing till all the previous stream stages complete. Also it
     * requires intermediate memory to store the whole content of the stream as
     * the stream natural order is left to right. If your accumulator function
     * is associative and you can provide a combiner function, consider using
     * {@link #reduce(Object, BiFunction, BinaryOperator)} method.
     * 
     * <p>
     * For parallel stream it's not guaranteed that accumulator will always be
     * executed in the same thread.
     *
     * @param <U>
     *            The type of the result
     * @param identity
     *            the identity value
     * @param accumulator
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function for incorporating an additional element into a result
     * @return the result of the folding
     * @see #foldLeft(Object, BiFunction)
     * @see #reduce(Object, BinaryOperator)
     * @see #reduce(Object, BiFunction, BinaryOperator)
     * @since 0.2.2
     */
    public <U> U foldRight(U identity, BiFunction<? super T, U, U> accumulator) {
        return toListAndThen(list -> {
            U result = identity;
            for (int i = list.size() - 1; i >= 0; i--)
                result = accumulator.apply(list.get(i), result);
            return result;
        });
    }

    /**
     * Produces a collection containing cumulative results of applying the
     * accumulation function going left to right.
     * 
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * The result {@link List} is guaranteed to be mutable.
     * 
     * <p>
     * For parallel stream it's not guaranteed that accumulator will always be
     * executed in the same thread.
     * 
     * <p>
     * This method cannot take all the advantages of parallel streams as it must
     * process elements strictly left to right.
     *
     * @param <U>
     *            The type of the result
     * @param identity
     *            the identity value
     * @param accumulator
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function for incorporating an additional element into a result
     * @return the {@code List} where the first element is the identity and
     *         every successor element is the result of applying accumulator
     *         function to the previous list element and the corresponding
     *         stream element. The resulting list is one element longer than
     *         this stream.
     * @see #foldLeft(Object, BiFunction)
     * @see #scanRight(Object, BiFunction)
     * @since 0.2.1
     */
    public <U> List<U> scanLeft(U identity, BiFunction<U, ? super T, U> accumulator) {
        List<U> result = new ArrayList<>();
        result.add(identity);
        forEachOrdered(t -> result.add(accumulator.apply(result.get(result.size() - 1), t)));
        return result;
    }

    /**
     * Produces a collection containing cumulative results of applying the
     * accumulation function going right to left.
     * 
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * The result {@link List} is guaranteed to be mutable.
     * 
     * <p>
     * For parallel stream it's not guaranteed that accumulator will always be
     * executed in the same thread.
     * 
     * <p>
     * This method cannot take all the advantages of parallel streams as it must
     * process elements strictly right to left.
     *
     * @param <U>
     *            The type of the result
     * @param identity
     *            the identity value
     * @param accumulator
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function for incorporating an additional element into a result
     * @return the {@code List} where the last element is the identity and every
     *         predecessor element is the result of applying accumulator
     *         function to the corresponding stream element and the next list
     *         element. The resulting list is one element longer than this
     *         stream.
     * @see #scanLeft(Object, BiFunction)
     * @see #foldRight(Object, BiFunction)
     * @since 0.2.2
     */
    @SuppressWarnings("unchecked")
    public <U> List<U> scanRight(U identity, BiFunction<? super T, U, U> accumulator) {
        return toListAndThen(list -> {
            // Reusing the list for different object type as it will save memory
            List<U> result = (List<U>) list;
            result.add(identity);
            for (int i = result.size() - 2; i >= 0; i--) {
                result.set(i, accumulator.apply((T) result.get(i), result.get(i + 1)));
            }
            return result;
        });
    }

    /**
     * Returns a stream consisting of the remaining elements of this stream
     * after discarding the first {@code n} elements of the stream even if the
     * stream is unordered. If this stream contains fewer than {@code n}
     * elements then an empty stream will be returned.
     *
     * <p>
     * This is a stateful <a
     * href="package-summary.html#StreamOps">quasi-intermediate</a> operation.
     * Unlike {@link #skip(long)} it skips the first elements even if the stream
     * is unordered. The main purpose of this method is to workaround the
     * problem of skipping the first elements from non-sized source with further
     * parallel processing and unordered terminal operation (such as
     * {@link #forEach(Consumer)}). For example,
     * {@code StreamEx.ofLines(br).skip(1).parallel().toSet()} will skip
     * arbitrary line, but
     * {@code StreamEx.ofLines(br).skipOrdered(1).parallel().toSet()} will skip
     * the first one. Also it behaves much better with infinite streams
     * processed in parallel.
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
    public S skipOrdered(long n) {
        return supply(delegate((stream.isParallel() ? StreamSupport.stream(stream.spliterator(), false) : stream).skip(
            n).spliterator()));
    }

    /**
     * Returns a stream consisting of all elements from this stream until the
     * first element which does not match the given predicate is found.
     * 
     * <p>
     * This is a short-circuiting stateful operation. It can be either <a
     * href="package-summary.html#StreamOps">intermediate or
     * quasi-intermediate</a>. When using with JDK 1.9 or higher it calls the
     * corresponding JDK 1.9 implementation. When using with JDK 1.8 it uses own
     * implementation.
     * 
     * <p>
     * While this operation is quite cheap for sequential stream, it can be
     * quite expensive on parallel pipelines.
     * 
     * @param predicate
     *            a non-interfering, stateless predicate to apply to elements.
     * @return the new stream.
     * @since 0.3.6
     */
    public S takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (IS_JDK9 && JDK9_METHODS[IDX_STREAM] != null) {
            return callWhile(predicate, IDX_TAKE_WHILE);
        }
        return supply(delegate(new TDOfRef<>(stream.spliterator(), false, predicate)));
    }

    /**
     * Returns a stream consisting of all elements from this stream starting
     * from the first element which does not match the given predicate. If the
     * predicate is true for all stream elements, an empty stream is returned.
     * 
     * <p>
     * This is a stateful operation. It can be either <a
     * href="package-summary.html#StreamOps">intermediate or
     * quasi-intermediate</a>. When using with JDK 1.9 or higher it calls the
     * corresponding JDK 1.9 implementation. When using with JDK 1.8 it uses own
     * implementation.
     * 
     * <p>
     * While this operation is quite cheap for sequential stream, it can be
     * quite expensive on parallel pipelines.
     * 
     * @param predicate
     *            a non-interfering, stateless predicate to apply to elements.
     * @return the new stream.
     * @since 0.3.6
     */
    public S dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (IS_JDK9 && JDK9_METHODS[IDX_STREAM] != null) {
            return callWhile(predicate, IDX_DROP_WHILE);
        }
        return supply(delegate(new TDOfRef<>(stream.spliterator(), true, predicate)));
    }
}
