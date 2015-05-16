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

/* package */abstract class AbstractStreamEx<T, S extends AbstractStreamEx<T, S>> implements Stream<T>, Iterable<T> {
    final Stream<T> stream;

    AbstractStreamEx(Stream<T> stream) {
        this.stream = stream;
    }

    StreamFactory strategy() {
        return StreamFactory.DEFAULT;
    }

    abstract S supply(Stream<T> stream);

    static <V> BinaryOperator<V> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    static IntStreamEx intStreamForLength(int a, int b) {
        if (a != b)
            throw new IllegalArgumentException("Length differs: " + a + " != " + b);
        return IntStreamEx.range(0, a);
    }

    static void rangeCheck(int arrayLength, int startInclusive, int endExclusive) {
        if (startInclusive > endExclusive) {
            throw new ArrayIndexOutOfBoundsException("startInclusive(" + startInclusive + ") > endExclusive("
                    + endExclusive + ")");
        }
        if (startInclusive < 0) {
            throw new ArrayIndexOutOfBoundsException(startInclusive);
        }
        if (endExclusive > arrayLength) {
            throw new ArrayIndexOutOfBoundsException(endExclusive);
        }
    }

    static <T> Stream<T> flatTraverse(Stream<T> src, Function<T, Stream<T>> streamProvider) {
        return src.flatMap(t -> {
            Stream<T> result = streamProvider.apply(t);
            return result == null ? Stream.of(t) : Stream.concat(Stream.of(t), flatTraverse(result, streamProvider));
        });
    }

    @SuppressWarnings("unchecked")
    static <T> Stream<T> unwrap(Stream<T> stream) {
        return stream instanceof AbstractStreamEx ? ((AbstractStreamEx<T, ?>) stream).stream : stream;
    }

    @Override
    public Iterator<T> iterator() {
        return stream.iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return stream.spliterator();
    }

    /**
     * Returns whether this stream, if a terminal operation were to be executed,
     * would execute in parallel. Calling this method after invoking an terminal
     * stream operation method may yield unpredictable results.
     *
     * @return {@code true} if this stream would execute in parallel if executed
     */
    @Override
    public boolean isParallel() {
        return stream.isParallel();
    }

    /**
     * Returns an equivalent stream that is unordered. May return itself, either
     * because the stream was already unordered, or because the underlying
     * stream state was modified to be unordered.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @return an unordered stream
     */
    @Override
    public S unordered() {
        return supply(stream.unordered());
    }

    /**
     * Closes this stream, causing all close handlers for this stream pipeline
     * to be called.
     *
     * @see AutoCloseable#close()
     */
    @Override
    public S onClose(Runnable closeHandler) {
        return supply(stream.onClose(closeHandler));
    }

    @Override
    public void close() {
        stream.close();
    }

    /**
     * Returns a stream consisting of the elements of this stream that match the
     * given predicate.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param predicate
     *            a non-interfering, stateless predicate to apply to each
     *            element to determine if it should be included
     * @return the new stream
     */
    @Override
    public S filter(Predicate<? super T> predicate) {
        return supply(stream.filter(predicate));
    }

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying the
     * provided mapping function to each element. Each mapped stream is
     * {@link java.util.stream.BaseStream#close() closed} after its contents
     * have been placed into this stream. (If a mapped stream is {@code null} an
     * empty stream is used, instead.)
     *
     * <p>
     * This is an intermediate operation.
     *
     * <p>
     * The {@code flatMap()} operation has the effect of applying a one-to-many
     * transformation to the elements of the stream, and then flattening the
     * resulting elements into a new stream.
     *
     * @param <R>
     *            The element type of the new stream
     * @param mapper
     *            a non-interfering, stateless function to apply to each element
     *            which produces a stream of new values
     * @return the new stream
     */
    @Override
    public <R> StreamEx<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return strategy().newStreamEx(stream.flatMap(mapper));
    }

    /**
     * Returns a stream consisting of the results of applying the given function
     * to the elements of this stream.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param <R>
     *            The element type of the new stream
     * @param mapper
     *            a non-interfering, stateless function to apply to each element
     * @return the new stream
     */
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
     * Returns a {@code StreamEx} consisting of the elements of this stream,
     * sorted according to natural order. If the elements of this stream are not
     * {@code Comparable}, a {@link java.lang.ClassCastException} may be thrown
     * when the terminal operation is executed.
     *
     * <p>
     * For ordered streams, the sort is stable. For unordered streams, no
     * stability guarantees are made.
     *
     * <p>
     * This is a stateful intermediate operation.
     *
     * @return the new stream
     */
    @Override
    public S sorted() {
        return supply(stream.sorted());
    }

    @Override
    public S sorted(Comparator<? super T> comparator) {
        return supply(stream.sorted(comparator));
    }

    /**
     * Returns a stream consisting of the elements of this stream, additionally
     * performing the provided action on each element as elements are consumed
     * from the resulting stream.
     *
     * <p>
     * This is an intermediate operation.
     *
     * <p>
     * For parallel stream pipelines, the action may be called at whatever time
     * and in whatever thread the element is made available by the upstream
     * operation. If the action modifies shared state, it is responsible for
     * providing the required synchronization.
     *
     * @param action
     *            a non-interfering action to perform on the elements as they
     *            are consumed from the stream
     * @return the new stream
     */
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

    /**
     * Performs an action for each element of this stream.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * The behavior of this operation is explicitly nondeterministic. For
     * parallel stream pipelines, this operation does <em>not</em> guarantee to
     * respect the encounter order of the stream, as doing so would sacrifice
     * the benefit of parallelism. For any given element, the action may be
     * performed at whatever time and in whatever thread the library chooses. If
     * the action accesses shared state, it is responsible for providing the
     * required synchronization.
     *
     * @param action
     *            a non-interfering action to perform on the elements
     */
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

    /**
     * Returns the count of elements in this stream.
     *
     * <p>
     * This is a terminal operation.
     *
     * @return the count of elements in this stream
     */
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
     * This is an intermediate operation.
     *
     * <p>
     * The {@code flatCollection()} operation has the effect of applying a
     * one-to-many transformation to the elements of the stream, and then
     * flattening the resulting elements into a new stream.
     *
     * @param <R>
     *            The element type of the new stream
     * @param mapper
     *            a non-interfering, stateless function to apply to each element
     *            which produces a {@link Collection} of new values
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
     * This is an intermediate operation.
     *
     * @param predicate
     *            a non-interfering, stateless predicate to apply to each
     *            element to determine if it should be excluded
     * @return the new stream
     */
    public S remove(Predicate<T> predicate) {
        return filter(predicate.negate());
    }

    /**
     * Returns a stream consisting of the elements of this stream that aren't
     * null.
     *
     * <p>
     * This is an intermediate operation.
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
     *            a non-interfering, stateless predicate which returned value
     *            should match
     * @return an {@code Optional} describing some element of this stream, or an
     *         empty {@code Optional} if the stream is empty
     * @throws NullPointerException
     *             if the element selected is null
     * @see Stream#findAny()
     * @see #findFirst(Predicate)
     */
    public Optional<T> findAny(Predicate<T> predicate) {
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
     *            a non-interfering, stateless predicate which returned value
     *            should match
     * @return an {@code Optional} describing the first element of this stream,
     *         or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException
     *             if the element selected is null
     * @see Stream#findFirst()
     */
    public Optional<T> findFirst(Predicate<T> predicate) {
        return filter(predicate).findFirst();
    }

    public S reverseSorted(Comparator<? super T> comparator) {
        return sorted(comparator.reversed());
    }

    public <V extends Comparable<? super V>> S sortedBy(Function<T, ? extends V> keyExtractor) {
        return sorted(Comparator.comparing(keyExtractor));
    }

    public S sortedByInt(ToIntFunction<T> keyExtractor) {
        return sorted(Comparator.comparingInt(keyExtractor));
    }

    public S sortedByLong(ToLongFunction<T> keyExtractor) {
        return sorted(Comparator.comparingLong(keyExtractor));
    }

    public S sortedByDouble(ToDoubleFunction<T> keyExtractor) {
        return sorted(Comparator.comparingDouble(keyExtractor));
    }

    public <V extends Comparable<? super V>> Optional<T> minBy(Function<T, ? extends V> keyExtractor) {
        return min(Comparator.comparing(keyExtractor));
    }

    public Optional<T> minByInt(ToIntFunction<T> keyExtractor) {
        return min(Comparator.comparingInt(keyExtractor));
    }

    public Optional<T> minByLong(ToLongFunction<T> keyExtractor) {
        return min(Comparator.comparingLong(keyExtractor));
    }

    public Optional<T> minByDouble(ToDoubleFunction<T> keyExtractor) {
        return min(Comparator.comparingDouble(keyExtractor));
    }

    public <V extends Comparable<? super V>> Optional<T> maxBy(Function<T, ? extends V> keyExtractor) {
        return max(Comparator.comparing(keyExtractor));
    }

    public Optional<T> maxByInt(ToIntFunction<T> keyExtractor) {
        return max(Comparator.comparingInt(keyExtractor));
    }

    public Optional<T> maxByLong(ToLongFunction<T> keyExtractor) {
        return max(Comparator.comparingLong(keyExtractor));
    }

    public Optional<T> maxByDouble(ToDoubleFunction<T> keyExtractor) {
        return max(Comparator.comparingDouble(keyExtractor));
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
    public S append(Stream<T> other) {
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
    public S prepend(Stream<T> other) {
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
     *            a non-interfering, stateless function for incorporating an
     *            additional element into a result
     * @return the result of the folding
     * @see #foldRight(Object, BiFunction)
     * @see #reduce(Object, BinaryOperator)
     * @see #reduce(Object, BiFunction, BinaryOperator)
     * @since 0.2.0
     */
    @SuppressWarnings("unchecked")
    public <U> U foldLeft(U identity, BiFunction<U, ? super T, U> accumulator) {
        Object[] result = new Object[] { identity };
        forEachOrdered(t -> result[0] = accumulator.apply((U) result[0], t));
        return (U) result[0];
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
     *            a non-interfering, stateless function for incorporating an
     *            additional element into a result
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
     *            a non-interfering, stateless function for incorporating an
     *            additional element into a result
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
     *            a non-interfering, stateless function for incorporating an
     *            additional element into a result
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
}
