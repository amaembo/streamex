/*
 * Copyright 2015, 2024 StreamEx contributors
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.*;
import java.util.stream.*;
import java.util.stream.Collector.Characteristics;

import static one.util.streamex.Internals.*;

/**
 * Base class providing common functionality for {@link StreamEx} and {@link EntryStream}.
 *
 * @param <T> the type of the stream elements
 * @param <S> the type of the stream extending {@code AbstractStreamEx}
 */
@NullMarked
public abstract class AbstractStreamEx<T extends @Nullable Object, S extends AbstractStreamEx<T, S>> extends
        BaseStreamEx<T, Stream<T>, Spliterator<T>, S> implements Stream<T>, Iterable<T> {
    @SuppressWarnings("unchecked")
    AbstractStreamEx(Stream<? extends T> stream, StreamContext context) {
        super((Stream<T>) stream, context);
    }

    @SuppressWarnings("unchecked")
    AbstractStreamEx(Spliterator<? extends T> spliterator, StreamContext context) {
        super((Spliterator<T>) spliterator, context);
    }

    @Override
    final Stream<T> createStream() {
      //When called, spliterator must never be null
      //noinspection DataFlowIssue
      return StreamSupport.stream(spliterator, context.parallel);
    }

    final <K extends @Nullable Object, V, M extends Map<K, V>> M toMapThrowing(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valMapper, M map) {
        forEach(t -> addToMap(map, keyMapper.apply(t), Objects.requireNonNull(valMapper.apply(t))));
        return map;
    }

    static <K extends @Nullable Object, V, M extends Map<K, V>> void addToMap(M map, K key, V val) {
        V oldVal = map.putIfAbsent(key, val);
        if (oldVal != null) {
            throw new IllegalStateException("Duplicate entry for key '" + key + "' (attempt to merge values '" + oldVal
                    + "' and '" + val + "')");
        }
    }

    <R extends @Nullable Object, A extends @Nullable Object> R rawCollect(Collector<? super T, A, R> collector) {
        if (context.fjp != null)
            return context.terminate(collector, stream()::collect);
        return stream().collect(collector);
    }

    @SuppressWarnings("unchecked")
    S appendSpliterator(@Nullable Stream<? extends T> other, Spliterator<? extends T> right) {
        if (right.getExactSizeIfKnown() == 0)
            return (S) this;
        Spliterator<T> left = spliterator();
        Spliterator<T> result;
        if (left.getExactSizeIfKnown() == 0)
            result = (Spliterator<T>) right;
        else
            result = new TailConcatSpliterator<>(left, right);
        context = context.combine(other);
        return supply(result);
    }

    @SuppressWarnings("unchecked")
    S prependSpliterator(@Nullable Stream<? extends T> other, Spliterator<? extends T> left) {
        if (left.getExactSizeIfKnown() == 0)
            return (S) this;
        Spliterator<T> right = spliterator();
        Spliterator<T> result;
        if (right.getExactSizeIfKnown() == 0)
            result = (Spliterator<T>) left;
        else
            result = new TailConcatSpliterator<>(left, right);
        context = context.combine(other);
        return supply(result);
    }

    @SuppressWarnings("unchecked")
    S ifEmpty(@Nullable Stream<? extends T> other, Spliterator<? extends T> right) {
        if (right.getExactSizeIfKnown() == 0)
            return (S) this;
        Spliterator<T> left = spliterator();
        Spliterator<T> result;
        if (left.getExactSizeIfKnown() == 0)
            result = (Spliterator<T>) right;
        else
            result = new IfEmptySpliterator<>(left, right);
        context = context.combine(other);
        return supply(result);
    }

    abstract S supply(Stream<T> stream);

    abstract S supply(Spliterator<T> spliterator);

    @Override
    public Iterator<T> iterator() {
        return Spliterators.iterator(spliterator());
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull S sequential() {
        return (S) super.sequential();
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull S parallel() {
        return (S) super.parallel();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NonNull S parallel(ForkJoinPool fjp) {
        return (S) super.parallel(fjp);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull S unordered() {
        return (S) super.unordered();
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull S onClose(Runnable closeHandler) {
        return (S) super.onClose(closeHandler);
    }

    /**
     * @see #nonNull()
     * @see #remove(Predicate)
     * @see StreamEx#select(Class)
     */
    @Override
    public @NonNull S filter(Predicate<? super T> predicate) {
        return supply(stream().filter(predicate));
    }

    @Override
    public <R extends @Nullable Object> StreamEx<R> flatMap(Function<? super T, ? extends @Nullable Stream<? extends R>> mapper) {
        return new StreamEx<>(stream().flatMap(mapper), context);
    }

    /**
     * Returns a stream where every element of this stream is replaced by elements produced
     * by a mapper function.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function that generates replacement elements. It accepts an element of this
     *               stream and a consumer that accepts new elements produced from the input element.
     * @return the new stream
     * @param <R> type of the resulting elements
     * @since 0.8.3
     */
    public <R extends @Nullable Object> StreamEx<R> mapMulti(BiConsumer<? super T, ? super Consumer<R>> mapper) {
        Objects.requireNonNull(mapper);
        return VerSpec.VER_SPEC.callMapMulti(this, mapper);
    }

    /**
     * Returns an {@link IntStreamEx} where every element of this stream is replaced by elements produced
     * by a mapper function.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function that generates replacement elements. It accepts an element of this
     *               stream and a consumer that accepts new elements produced from the input element.
     * @return the new stream
     * @since 0.8.3
     */
    public IntStreamEx mapMultiToInt(BiConsumer<? super T, ? super IntConsumer> mapper) {
        Objects.requireNonNull(mapper);
        return VerSpec.VER_SPEC.callMapMultiToInt(this, mapper);
    }

    /**
     * Returns a {@link LongStreamEx} where every element of this stream is replaced by elements produced
     * by a mapper function.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function that generates replacement elements. It accepts an element of this
     *               stream and a consumer that accepts new elements produced from the input element.
     * @return the new stream
     * @since 0.8.3
     */
    public LongStreamEx mapMultiToLong(BiConsumer<? super T, ? super LongConsumer> mapper) {
        Objects.requireNonNull(mapper);
        return VerSpec.VER_SPEC.callMapMultiToLong(this, mapper);
    }

    /**
     * Returns a {@link DoubleStreamEx} where every element of this stream is replaced by elements produced
     * by a mapper function.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function that generates replacement elements. It accepts an element of this
     *               stream and a consumer that accepts new elements produced from the input element.
     * @return the new stream
     * @since 0.8.3
     */
    public DoubleStreamEx mapMultiToDouble(BiConsumer<? super T, ? super DoubleConsumer> mapper) {
        Objects.requireNonNull(mapper);
        return VerSpec.VER_SPEC.callMapMultiToDouble(this, mapper);
    }

    @Override
    public <R extends @Nullable Object> StreamEx<R> map(Function<? super T, ? extends R> mapper) {
        return new StreamEx<>(stream().map(mapper), context);
    }

    @Override
    public IntStreamEx mapToInt(ToIntFunction<? super T> mapper) {
        return new IntStreamEx(stream().mapToInt(mapper), context);
    }

    @Override
    public LongStreamEx mapToLong(ToLongFunction<? super T> mapper) {
        return new LongStreamEx(stream().mapToLong(mapper), context);
    }

    @Override
    public DoubleStreamEx mapToDouble(ToDoubleFunction<? super T> mapper) {
        return new DoubleStreamEx(stream().mapToDouble(mapper), context);
    }

    @Override
    public IntStreamEx flatMapToInt(Function<? super T, ? extends @Nullable IntStream> mapper) {
        return new IntStreamEx(stream().flatMapToInt(mapper), context);
    }

    @Override
    public LongStreamEx flatMapToLong(Function<? super T, ? extends @Nullable LongStream> mapper) {
        return new LongStreamEx(stream().flatMapToLong(mapper), context);
    }

    @Override
    public DoubleStreamEx flatMapToDouble(Function<? super T, ? extends @Nullable DoubleStream> mapper) {
        return new DoubleStreamEx(stream().flatMapToDouble(mapper), context);
    }

    /**
     * Returns a new stream containing all the elements of the original stream interspersed with
     * a given delimiter.
     *
     * <p>
     * For example, {@code StreamEx.of("a", "b", "c").intersperse("x")} will yield a stream containing
     * five elements: a, x, b, x, c.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate operation</a>.
     *
     * @param delimiter a delimiter to be inserted between each pair of elements
     * @return the new stream
     * @since 0.6.6
     */
    public @NonNull S intersperse(T delimiter) {
        return supply(stream().flatMap(s -> StreamEx.of(delimiter, s)).skip(1));
    }

    @Override
    public @NonNull S distinct() {
        return supply(stream().distinct());
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
     * @param keyExtractor a non-interfering, stateless function which
     *        classifies input elements.
     * @return the new stream
     * @since 0.3.8
     */
    public @NonNull S distinct(Function<? super T, ? extends @Nullable Object> keyExtractor) {
        return supply(stream().map(t -> new PairBox<>(t, keyExtractor.apply(t))).distinct().map(box -> box.a));
    }

    /**
     * Returns a {@code StreamEx} consisting of the distinct elements (according
     * to {@link Object#equals(Object)}) that appear at least specified number
     * of times in this stream.
     *
     * <p>
     * This operation is not guaranteed to be stable: any of the equal elements can
     * be selected for the output. However, if this stream is ordered, then the order
     * is preserved.
     *
     * <p>
     * This is a stateful <a
     * href="package-summary.html#StreamOps">quasi-intermediate</a> operation.
     *
     * @param atLeast minimal number of occurrences required to select the
     *        element. If atLeast is 1 or less, then this method is equivalent
     *        to {@link #distinct()}.
     * @return the new stream
     * @see #distinct()
     * @since 0.3.1
     */
    public @NonNull S distinct(long atLeast) {
        if (atLeast <= 1)
            return distinct();
        Spliterator<T> spliterator = spliterator();
        Spliterator<T> result;
        if (spliterator.hasCharacteristics(Spliterator.DISTINCT))
            // already distinct: cannot have any repeating elements
            result = Spliterators.emptySpliterator();
        else
            result = new DistinctSpliterator<>(spliterator, atLeast);
        return supply(result);
    }

    @Override
    public @NonNull S sorted() {
        return supply(stream().sorted());
    }

    @Override
    public @NonNull S sorted(Comparator<? super T> comparator) {
        return supply(stream().sorted(comparator));
    }

    @Override
    public @NonNull S peek(Consumer<? super T> action) {
        return supply(stream().peek(action));
    }

    @Override
    public @NonNull S limit(long maxSize) {
        return supply(stream().limit(maxSize));
    }

    @Override
    public @NonNull S skip(long n) {
        return supply(stream().skip(n));
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        if (spliterator != null && !isParallel()) {
            spliterator().forEachRemaining(action);
        } else {
            if (context.fjp != null)
                context.terminate(() -> {
                    stream().forEach(action);
                    return null;
                });
            else {
                stream().forEach(action);
            }
        }
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        if (spliterator != null && !isParallel()) {
            spliterator().forEachRemaining(action);
        } else {
            if (context.fjp != null)
                context.terminate(() -> {
                    stream().forEachOrdered(action);
                    return null;
                });
            else {
                stream().forEachOrdered(action);
            }
        }
    }

    @Override
    @NullUnmarked // may return nulls inside the array
    public Object @NonNull [] toArray() {
        return toArray(Object[]::new);
    }

    @Override
    @NullUnmarked // may return nulls inside the array
    public <A> A @NonNull [] toArray(@NonNull IntFunction<A @NonNull []> generator) {
        if (context.fjp != null)
            return context.terminate(generator, stream()::toArray);
        return stream().toArray(generator);
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        if (context.fjp != null)
            return context.terminate(() -> stream().reduce(identity, accumulator));
        return stream().reduce(identity, accumulator);
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        if (context.fjp != null)
            return context.terminate(accumulator, stream()::reduce);
        return stream().reduce(accumulator);
    }

    @Override
    public <U extends @Nullable Object> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        if (context.fjp != null)
            return context.terminate(() -> stream().reduce(identity, accumulator, combiner));
        return stream().reduce(identity, accumulator, combiner);
    }

    /**
     * Performs a possibly short-circuiting reduction of the stream elements using 
     * the provided {@code BinaryOperator}. The result is described as an {@code Optional<T>}.
     *
     * <p>
     * This is a short-circuiting terminal operation. It behaves like {@link #reduce(BinaryOperator)}. However,
     * it additionally accepts a zero-element (also known as an absorbing element). When the zero-element
     * is passed to the accumulator, then the result must be zero as well. So the operation
     * takes advantage of this and may short-circuit if zero is reached during the reduction.
     *
     * @param zero zero element
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>
     *        , <a href="package-summary.html#NonInterference">non-interfering
     *        </a>, <a href="package-summary.html#Statelessness">stateless</a>
     *        function to combine two elements into one.
     * @return the result of reduction. Empty Optional is returned if the input stream is empty.
     * @throws NullPointerException if the accumulator is null or the result of reduction is null
     * @see MoreCollectors#reducingWithZero(Object, BinaryOperator)
     * @see #reduceWithZero(Object, Object, BinaryOperator)
     * @see #reduce(BinaryOperator)
     * @since 0.7.3
     */
    public Optional<T> reduceWithZero(T zero, BinaryOperator<T> accumulator) {
        return collect(MoreCollectors.reducingWithZero(zero, accumulator));
    }

    /**
     * Performs a possibly short-circuiting reduction of the stream elements using 
     * the provided identity value and a {@code BinaryOperator}.
     *
     * <p>
     * This is a short-circuiting terminal operation. It behaves like {@link #reduce(Object, BinaryOperator)}. 
     * However, it additionally accepts a zero element (also known as an absorbing element). When the zero-element
     * is passed to the accumulator, then the result must be zero as well. So the operation
     * takes advantage of this and may short-circuit if zero is reached during the reduction.
     *
     * @param zero zero element
     * @param identity an identity element. For all {@code t}, {@code accumulator.apply(t, identity)} is
     *                 equal to {@code accumulator.apply(identity, t)} and is equal to {@code t}.
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>
     *        , <a href="package-summary.html#NonInterference">non-interfering
     *        </a>, <a href="package-summary.html#Statelessness">stateless</a>
     *        function to combine two elements into one.
     * @return the result of reduction. Empty Optional is returned if the input stream is empty.
     * @throws NullPointerException if the accumulator is null or the result of reduction is null
     * @see MoreCollectors#reducingWithZero(Object, Object, BinaryOperator)
     * @see #reduceWithZero(Object, BinaryOperator)
     * @see #reduce(Object, BinaryOperator)
     * @since 0.7.3
     */
    public T reduceWithZero(T zero, T identity, BinaryOperator<T> accumulator) {
        return collect(MoreCollectors.reducingWithZero(zero, identity, accumulator));
    }

    @Override
    public <R extends @Nullable Object> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        if (context.fjp != null)
            return context.terminate(() -> stream().collect(supplier, accumulator, combiner));
        return stream().collect(supplier, accumulator, combiner);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If a special <a
     * href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> is passed, this operation becomes short-circuiting as well.
     */
    @Override
    public <R extends @Nullable Object, A extends @Nullable Object> R collect(Collector<? super T, A, R> collector) {
        Predicate<A> finished = finished(collector);
        if (finished != null) {
            BiConsumer<A, ? super T> acc = collector.accumulator();
            BinaryOperator<A> combiner = collector.combiner();
            Spliterator<T> spliterator = spliterator();
            if (!isParallel()) {
                A a = collector.supplier().get();
                if (!finished.test(a)) {
                    try {
                        // forEachRemaining can be much faster
                        // and take much less memory than tryAdvance for certain
                        // spliterators
                        spliterator.forEachRemaining(e -> {
                            acc.accept(a, e);
                            if (finished.test(a))
                                throw new CancelException();
                        });
                    } catch (CancelException ex) {
                        // ignore
                    }
                }
                return collector.finisher().apply(a);
            }
            Spliterator<A> spltr;
            if (!spliterator.hasCharacteristics(Spliterator.ORDERED)
                    || collector.characteristics().contains(Characteristics.UNORDERED)) {
                spltr = new UnorderedCancellableSpliterator<>(spliterator, collector.supplier(), acc, combiner,
                        finished);
            } else {
                spltr = new OrderedCancellableSpliterator<>(spliterator, collector.supplier(), acc, combiner, finished);
            }
            return collector.finisher().apply(
                    new StreamEx<>(StreamSupport.stream(spltr, true), context).findFirst().get());
        }
        return rawCollect(collector);
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
        if (context.fjp != null)
            return context.terminate(stream()::count);
        return stream().count();
    }

    /**
     * Counts the number of elements in the stream that satisfy the predicate.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param predicate a <a
     *                  href="package-summary.html#NonInterference">non-interfering </a>,
     *                  <a href="package-summary.html#Statelessness">stateless</a>
     *                  predicate to apply to stream elements. Only elements passing
     *                  the predicate will be counted.
     * @return the count of elements in this stream satisfying the predicate.
     */
    public long count(Predicate<? super T> predicate) {
        return filter(predicate).count();
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        if (context.fjp != null)
            return context.terminate(predicate, stream()::anyMatch);
        return stream().anyMatch(predicate);
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        if (context.fjp != null)
            return context.terminate(predicate, stream()::allMatch);
        return stream().allMatch(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return !anyMatch(predicate);
    }

    @Override
    public Optional<T> findFirst() {
        if (context.fjp != null)
            return context.terminate(stream()::findFirst);
        return stream().findFirst();
    }

    @Override
    public Optional<T> findAny() {
        if (context.fjp != null)
            return context.terminate(stream()::findAny);
        return stream().findAny();
    }

    /**
     * Returns an {@link OptionalLong} describing the zero-based index of the
     * first element of this stream, which equals to the given element, or an
     * empty {@code OptionalLong} if there's no matching element.
     *
     * <p>
     * This is a short-circuiting terminal operation.
     *
     * @param element an element to look for
     * @return an {@code OptionalLong} describing the index of the first
     *         matching element of this stream, or an empty {@code OptionalLong}
     *         if there's no matching element.
     * @see #indexOf(Predicate)
     * @since 0.4.0
     */
    public OptionalLong indexOf(T element) {
        return indexOf(Predicate.isEqual(element));
    }

    /**
     * Returns an {@link OptionalLong} describing the zero-based index of the
     * first element of this stream, which matches given predicate, or an empty
     * {@code OptionalLong} if there's no matching element.
     *
     * <p>
     * This is a short-circuiting terminal operation.
     *
     * @param predicate a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        predicate which returned value should match
     * @return an {@code OptionalLong} describing the index of the first
     *         matching element of this stream, or an empty {@code OptionalLong}
     *         if there's no matching element.
     * @see #findFirst(Predicate)
     * @see #indexOf(Object)
     * @since 0.4.0
     */
    public OptionalLong indexOf(Predicate<? super T> predicate) {
      CancellableCollectorImpl<T, long[], OptionalLong> collector = new CancellableCollectorImpl<>(
              () -> new long[]{-1}, (acc, t) -> {
        if (predicate.test(t)) {
          acc[0] = -acc[0] - 1;
        } else {
          acc[0]--;
        }
      }, (acc1, acc2) -> {
        if (acc2[0] < 0) {
          acc1[0] = acc1[0] + acc2[0] + 1;
        } else {
          acc1[0] = acc2[0] - acc1[0] - 1;
        }
        return acc1;
      }, acc -> acc[0] < 0 ? OptionalLong.empty() : OptionalLong.of(acc[0]), acc -> acc[0] >= 0, NO_CHARACTERISTICS);
      return collect(collector);
    }

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped collection produced by applying
     * the provided mapping function to each element. (If a mapped collection is
     * {@code null} nothing is added for a given element to the resulting stream.)
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
     * @param <R> The element type of the new stream
     * @param mapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to each element which produces a
     *        {@link Collection} of new values
     * @return the new stream
     */
    public <R extends @Nullable Object> StreamEx<R> flatCollection(
            Function<? super T, ? extends @Nullable Collection<? extends R>> mapper) {
        return flatMap(t -> {
            Collection<? extends R> c = mapper.apply(t);
            return c == null ? null : StreamEx.of(c.spliterator());
        });
    }

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped array produced by applying
     * the provided mapping function to each element. (If a mapped array is
     * {@code null} nothing is added for a given element to the resulting stream.)
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * <p>
     * The {@code flatArray()} operation has the effect of applying a
     * one-to-many transformation to the elements of the stream, and then
     * flattening the resulting elements into a new stream.
     *
     * @param <R> The element type of the new stream
     * @param mapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to each element which produces an
     *        array of new values
     * @return the new stream
     * @since 0.6.5
     */
    public <R extends @Nullable Object> StreamEx<R> flatArray(Function<? super T, ? extends R @Nullable []> mapper) {
        return flatMap(t -> {
            R[] a = mapper.apply(t);
            return a == null ? null : StreamEx.of(Arrays.spliterator(a));
        });
    }

    /**
     * Performs a mapping of the stream content to a partial function
     * removing the elements to which the function is not applicable.
     *
     * <p>
     * If the mapping function returns {@link Optional#empty()}, the original
     * value will be removed from the resulting stream. The mapping function
     * may not return null.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * <p>
     * The {@code mapPartial()} operation has the effect of applying a
     * one-to-zero-or-one transformation to the elements of the stream, and then
     * flattening the resulting elements into a new stream.
     *
     * @param <R> The element type of the new stream
     * @param mapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        partial function to apply to each element which returns a present optional
     *        if it's applicable, or an empty optional otherwise
     * @return the new stream
     * @since 0.6.8
     */
    public <R> StreamEx<R> mapPartial(Function<? super T, ? extends Optional<? extends R>> mapper) {
        return new StreamEx<>(stream().map(value -> mapper.apply(value).orElse(null)).filter(Objects::nonNull),
                context);
    }

    /**
     * Returns a stream consisting of the results of applying the given function
     * to the every adjacent pair of elements of this stream.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * operation.
     *
     * <p>
     * The output stream will contain one element less than this stream. If this
     * stream contains zero or one element, the output stream will be empty.
     *
     * @param <R> The element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each
     *        adjacent pair of this stream elements.
     * @return the new stream
     * @since 0.2.1
     */
    public <R extends @Nullable Object> StreamEx<R> pairMap(BiFunction<? super T, ? super T, ? extends R> mapper) {
        PairSpliterator.PSOfRef<T, R> spliterator = new PairSpliterator.PSOfRef<>(mapper, spliterator());
        return new StreamEx<>(spliterator, context);
    }

    /**
     * Returns a stream consisting of the elements of this stream that don't
     * match the given predicate.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param predicate a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        predicate to apply to each element to determine if it should be
     *        excluded
     * @return the new stream
     * @see #filter(Predicate)
     * @see #nonNull()
     * @see StreamEx#select(Class)
     */
    public @NonNull S remove(Predicate<? super T> predicate) {
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
     * @see #filter(Predicate)
     * @see #remove(Predicate)
     * @see StreamEx#select(Class)
     */
    public @NonNull S nonNull() {
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
     * @param predicate a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        predicate which returned value should match
     * @return an {@code Optional} describing some matching element of this
     *         stream, or an empty {@code Optional} if there's no matching
     *         element
     * @throws NullPointerException if the element selected is null
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
     * @param predicate a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        predicate which returned value should match
     * @return an {@code Optional} describing the first matching element of this
     *         stream, or an empty {@code Optional} if there's no matching
     *         element
     * @throws NullPointerException if the element selected is null
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
     * @param comparator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        {@code Comparator} to be used to compare stream elements
     * @return the new stream
     */
    public @NonNull S reverseSorted(Comparator<? super T> comparator) {
        return sorted(comparator.reversed());
    }

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the natural order of the keys extracted by the provided
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
     * @param <V> the type of the {@code Comparable} sort key
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to be used to extract sorting keys
     * @return the new stream
     */
    public <V extends Comparable<? super V>> @NonNull S sortedBy(Function<? super T, ? extends V> keyExtractor) {
        return sorted(Comparator.comparing(keyExtractor));
    }

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the int values extracted by the provided function.
     *
     * <p>
     * For ordered streams, the sort is stable. For unordered streams, no
     * stability guarantees are made.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">stateful intermediate
     * operation</a>.
     *
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to be used to extract sorting keys
     * @return the new stream
     */
    public @NonNull S sortedByInt(ToIntFunction<? super T> keyExtractor) {
        return sorted(Comparator.comparingInt(keyExtractor));
    }

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the long values extracted by the provided function.
     *
     * <p>
     * For ordered streams, the sort is stable. For unordered streams, no
     * stability guarantees are made.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">stateful intermediate
     * operation</a>.
     *
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to be used to extract sorting keys
     * @return the new stream
     */
    public @NonNull S sortedByLong(ToLongFunction<? super T> keyExtractor) {
        return sorted(Comparator.comparingLong(keyExtractor));
    }

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the double values extracted by the provided function.
     *
     * <p>
     * For ordered streams, the sort is stable. For unordered streams, no
     * stability guarantees are made.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">stateful intermediate
     * operation</a>.
     *
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to be used to extract sorting keys
     * @return the new stream
     */
    public @NonNull S sortedByDouble(ToDoubleFunction<? super T> keyExtractor) {
        return sorted(Comparator.comparingDouble(keyExtractor));
    }

    /**
     * Returns the minimum element of this stream according to the natural order
     * of the keys extracted by the provided function. This is a special case of a
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
     * @param <V> the type of the comparable keys
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to extract the comparable keys from this stream elements
     * @return an {@code Optional} describing the minimum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException if the minimum element is null
     */
    public <V extends Comparable<? super V>> Optional<T> minBy(Function<? super T, ? extends V> keyExtractor) {
        return Box
                .asOptional(reduce(null, (PairBox<T, V> acc, T t) -> {
                    V val = keyExtractor.apply(t);
                    if (acc == null)
                        return new PairBox<>(t, val);
                    if (val.compareTo(acc.b) < 0) {
                        acc.b = val;
                        acc.a = t;
                    }
                    return acc;
                }, (PairBox<T, V> acc1, PairBox<T, V> acc2) -> (acc1 == null || acc2 != null
                        && acc1.b.compareTo(acc2.b) > 0) ? acc2 : acc1));
    }

    /**
     * Returns the minimum element of this stream according to the int values
     * extracted by the provided function. This is a special case of a reduction.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * This method is equivalent to
     * {@code min(Comparator.comparingInt(keyExtractor))}, but may work faster
     * as keyExtractor function is applied only once per each input element.
     *
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to extract the int keys from this stream elements
     * @return an {@code Optional} describing the minimum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException if the minimum element is null
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
     * extracted by the provided function. This is a special case of a reduction.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * This method is equivalent to
     * {@code min(Comparator.comparingLong(keyExtractor))}, but may work faster
     * as keyExtractor function is applied only once per each input element.
     *
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to extract the long keys from this stream elements
     * @return an {@code Optional} describing the minimum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException if the minimum element is null
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
     * extracted by the provided function. This is a special case of a reduction.
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
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to extract the double keys from this stream elements
     * @return an {@code Optional} describing the minimum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException if the minimum element is null
     */
    public Optional<T> minByDouble(ToDoubleFunction<? super T> keyExtractor) {
        return Box.asOptional(reduce(null, (ObjDoubleBox<T> acc, T t) -> {
            double val = keyExtractor.applyAsDouble(t);
            if (acc == null)
                return new ObjDoubleBox<>(t, val);
            if (Double.compare(val, acc.b) < 0) {
                acc.b = val;
                acc.a = t;
            }
            return acc;
        }, (ObjDoubleBox<T> acc1, ObjDoubleBox<T> acc2) -> (acc1 == null || acc2 != null
                && Double.compare(acc1.b, acc2.b) > 0) ? acc2 : acc1));
    }

    /**
     * Returns the maximum element of this stream according to the natural order
     * of the keys extracted by the provided function. This is a special case of a
     * reduction.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * This method is equivalent to
     * {@code max(Comparator.comparing(keyExtractor))}, but may work faster as
     * keyExtractor function is applied only once per each input element.
     *
     * @param <V> the type of the comparable keys
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to extract the comparable keys from this stream elements
     * @return an {@code Optional} describing the maximum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException if the maximum element is null
     */
    public <V extends Comparable<? super V>> Optional<T> maxBy(Function<? super T, ? extends V> keyExtractor) {
        return Box
                .asOptional(reduce(null, (PairBox<T, V> acc, T t) -> {
                    V val = keyExtractor.apply(t);
                    if (acc == null)
                        return new PairBox<>(t, val);
                    if (val.compareTo(acc.b) > 0) {
                        acc.b = val;
                        acc.a = t;
                    }
                    return acc;
                }, (PairBox<T, V> acc1, PairBox<T, V> acc2) -> (acc1 == null || acc2 != null
                        && acc1.b.compareTo(acc2.b) < 0) ? acc2 : acc1));
    }

    /**
     * Returns the maximum element of this stream according to the int values
     * extracted by the provided function. This is a special case of a reduction.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * This method is equivalent to
     * {@code max(Comparator.comparingInt(keyExtractor))}, but may work faster
     * as keyExtractor function is applied only once per each input element.
     *
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to extract the int keys from this stream elements
     * @return an {@code Optional} describing the maximum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException if the maximum element is null
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
     * extracted by the provided function. This is a special case of a reduction.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * This method is equivalent to
     * {@code max(Comparator.comparingLong(keyExtractor))}, but may work faster
     * as keyExtractor function is applied only once per each input element.
     *
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to extract the long keys from this stream elements
     * @return an {@code Optional} describing the maximum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException if the maximum element is null
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
     * extracted by the provided function. This is a special case of a reduction.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * This method is equivalent to
     * {@code max(Comparator.comparingDouble(keyExtractor))}, but may work
     * faster as keyExtractor function is applied only once per each input
     * element.
     *
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to extract the double keys from this stream elements
     * @return an {@code Optional} describing the maximum element of this
     *         stream, or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException if the maximum element is null
     */
    public Optional<T> maxByDouble(ToDoubleFunction<? super T> keyExtractor) {
        return Box.asOptional(reduce(null, (ObjDoubleBox<T> acc, T t) -> {
            double val = keyExtractor.applyAsDouble(t);
            if (acc == null)
                return new ObjDoubleBox<>(t, val);
            if (Double.compare(val, acc.b) > 0) {
                acc.b = val;
                acc.a = t;
            }
            return acc;
        }, (ObjDoubleBox<T> acc1, ObjDoubleBox<T> acc2) -> (acc1 == null || acc2 != null
                && Double.compare(acc1.b, acc2.b) < 0) ? acc2 : acc1));
    }

    /**
     * Creates a lazily concatenated stream whose elements are all the elements
     * of this stream followed by all the elements of the other stream. The
     * resulting stream is ordered if both of the input streams are ordered, and
     * parallel if either of the input streams is parallel. When the resulting
     * stream is closed, the close handlers for both input streams are invoked.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a> with <a href="package-summary.html#TSO">tail-stream
     * optimization</a>.
     *
     * <p>
     * May return this if the supplied stream is known to be empty.
     *
     * @param other the other stream
     * @return this stream appended by the other stream
     * @see Stream#concat(Stream, Stream)
     */
    public @NonNull S append(Stream<? extends T> other) {
        return appendSpliterator(other, other.spliterator());
    }

    /**
     * Creates a lazily concatenated stream whose elements are all the elements
     * of the other stream followed by all the elements of this stream. The
     * resulting stream is ordered if both of the input streams are ordered, and
     * parallel if either of the input streams is parallel. When the resulting
     * stream is closed, the close handlers for both input streams are invoked.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a> with <a href="package-summary.html#TSO">tail-stream
     * optimization</a>.
     *
     * <p>
     * May return this if the supplied stream is known to be empty.
     *
     * @param other the other stream
     * @return this stream prepended by the other stream
     * @see Stream#concat(Stream, Stream)
     */
    public @NonNull S prepend(Stream<? extends T> other) {
        return prependSpliterator(other, other.spliterator());
    }

    /**
     * Returns a stream whose content is the same as this stream, except in the case when
     * this stream is empty. In this case, its content is replaced with the other stream's content.
     *
     * <p>
     * The other stream will not be traversed if this stream is not empty.
     *
     * <p>
     * If this stream is parallel and empty, the other stream is not guaranteed to be parallelized.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     *
     * @param other the other stream to replace the contents of this stream if this stream is empty.
     * @return the stream whose content is replaced by other stream contents only if this stream is empty.
     * @since 0.6.6
     */
    public @NonNull S ifEmpty(Stream<? extends T> other) {
        return ifEmpty(other, other.spliterator());
    }

    /**
     * Returns a {@link List} containing the elements of this stream. There
     * are no guarantees on the type, mutability, serializability, or thread-safety
     * of the returned {@code List}; if more control over the returned
     * {@code List} is required, use {@link #toCollection(Supplier)}.
     *
     * <p>
     * This is a terminal operation.
     *
     * @return a {@code List} containing the elements of this stream
     * @see Collectors#toList()
     * @see #toMutableList()
     * @see #toImmutableList()
     */
    public List<T> toList() {
        return IMMUTABLE_TO_LIST ? toImmutableList() : toMutableList();
    }

    /**
     * Returns a mutable {@link List} containing the elements of this stream.
     * There are no guarantees on the type, serializability, or thread-safety
     * of the returned {@code List}; if more control over the returned
     * {@code List} is required, use {@link #toCollection(Supplier)}.
     *
     * <p>
     * This is a terminal operation.
     *
     * @return a {@code List} containing the elements of this stream
     * @see Collectors#toList()
     * @see #toImmutableList()
     * @since 0.8.0
     */
    @SuppressWarnings("unchecked")
    public List<T> toMutableList() {
        return new ArrayList<>((Collection<T>) new ArrayCollection(toArray(Object[]::new)));
    }

    /**
     * Returns an immutable {@link List} containing the elements of this stream.
     * There are no guarantees on an exact type of the returned {@code List}. The
     * returned {@code List} is guaranteed to be serializable if all its
     * elements are serializable.
     *
     * <p>
     * This is a terminal operation.
     *
     * @return a {@code List} containing the elements of this stream
     * @see #toList()
     * @see #toMutableList()
     * @since 0.6.3
     */
    @SuppressWarnings("unchecked")
    public List<T> toImmutableList() {
        Object[] array = toArray(Object[]::new);
        switch (array.length) {
            case 0:
                return Collections.emptyList();
            case 1:
                return Collections.singletonList((T) array[0]);
            default:
                return Collections.unmodifiableList(Arrays.asList((T[]) array));
        }
    }

    /**
     * Creates a {@link List} containing the elements of this stream, then
     * performs finishing transformation and returns its result. There are no
     * guarantees on the type, serializability, or thread-safety of the
     * {@code List} created.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param <R> the type of the result
     * @param finisher a function to be applied to the intermediate list
     * @return result of applying the finishing transformation to the list of the
     *         stream elements.
     * @since 0.2.3
     * @see #toList()
     */
    public <R extends @Nullable Object> R toListAndThen(Function<? super List<T>, R> finisher) {
        if (context.fjp != null)
            return context.terminate(() -> finisher.apply(toMutableList()));
        return finisher.apply(toMutableList());
    }

    /**
     * Returns a {@link Set} containing the elements of this stream. There
     * are no guarantees on the type, mutability, serializability, or thread-safety
     * of the returned {@code Set}; if more control over the returned
     * {@code Set} is required, use {@link #toCollection(Supplier)}.
     *
     * <p>
     * This is a terminal operation.
     *
     * @return a {@code Set} containing the elements of this stream
     * @see Collectors#toSet()
     * @see #toMutableSet()
     * @see #toImmutableSet()
     */
    public Set<T> toSet() {
        return IMMUTABLE_TO_LIST ? toImmutableSet() : toMutableSet();
    }

    /**
     * Returns a mutable {@link Set} containing the elements of this stream.
     * There are no guarantees on the type, serializability, or thread-safety
     * of the returned {@code Set}; if more control over the returned
     * {@code Set} is required, use {@link #toCollection(Supplier)}.
     *
     * <p>
     * This is a terminal operation.
     *
     * @return a {@code Set} containing the elements of this stream
     * @see Collectors#toSet()
     * @see #toImmutableSet()
     * @since 0.8.0
     */
    public Set<T> toMutableSet() {
        return rawCollect(Collectors.toSet());
    }

    /**
     * Returns an immutable {@link Set} containing the elements of this stream.
     * There are no guarantees on an exact type of the returned {@code Set}. In
     * particular, no specific element order in the resulting set is guaranteed.
     * The returned {@code Set} is guaranteed to be serializable if all its
     * elements are serializable.
     *
     * <p>
     * This is a terminal operation.
     *
     * @return a {@code Set} containing the elements of this stream
     * @see #toSet()
     * @see #toMutableSet()
     * @since 0.6.3
     */
    public Set<T> toImmutableSet() {
        Set<T> result = toMutableSet();
        if (result.isEmpty())
            return Collections.emptySet();
        return Collections.unmodifiableSet(result);
    }

    /**
     * Creates a {@link Set} containing the elements of this stream, then
     * performs finishing transformation and returns its result. There are no
     * guarantees on the type, serializability, or thread-safety of the
     * {@code Set} created.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param <R> the result type
     * @param finisher a function to be applied to the intermediate {@code Set}
     * @return result of applying the finisher transformation to the {@code Set}
     *         of the stream elements.
     * @since 0.2.3
     * @see #toSet()
     */
    public <R extends @Nullable Object> R toSetAndThen(Function<? super Set<T>, R> finisher) {
        if (context.fjp != null)
            return context.terminate(() -> finisher.apply(toMutableSet()));
        return finisher.apply(toMutableSet());
    }

    /**
     * Creates a custom {@link Collection} containing the elements of this stream, 
     * then performs finishing transformation and returns its result. The
     * {@code Collection} is created by the provided factory.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param <C> the type of the resulting {@code Collection}
     * @param <R> the result type
     * @param collectionFactory a {@code Supplier} which returns a new, empty
     *        {@code Collection} of the appropriate type
     * @param finisher a function to be applied to the intermediate {@code Collection}
     * @return result of applying the finisher transformation to the {@code Collection}
     *         of the stream elements.
     * @since 0.7.3
     * @see #toCollection(Supplier)
     */
    public <C extends Collection<T>, R extends @Nullable Object> R toCollectionAndThen(
            Supplier<C> collectionFactory,
            Function<? super C, R> finisher) {
        if (context.fjp != null)
            return context.terminate(() -> finisher.apply(toCollection(collectionFactory)));
        return finisher.apply(toCollection(collectionFactory));
    }

    /**
     * Returns a {@link Collection} containing the elements of this stream. The
     * {@code Collection} is created by the provided factory.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param <C> the type of the resulting {@code Collection}
     * @param collectionFactory a {@code Supplier} which returns a new, empty
     *        {@code Collection} of the appropriate type
     * @return a {@code Collection} containing the elements of this stream
     * @see Collectors#toCollection(Supplier)
     * @see #toCollectionAndThen(Supplier, Function)
     */
    public <C extends Collection<T>> C toCollection(Supplier<C> collectionFactory) {
        return rawCollect(Collectors.toCollection(collectionFactory));
    }

    /**
     * Folds the elements of this stream using the provided seed object and
     * accumulation function, going left to right. This is equivalent to:
     *
     * <pre>
     * {@code
     *     U result = seed;
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
     * This method cannot take all the advantages of parallel streams as it must
     * process elements strictly left to right. If your accumulator function is
     * associative, and you can provide a combiner function, consider using
     * {@link #reduce(Object, BiFunction, BinaryOperator)} method.
     *
     * <p>
     * For parallel stream, it's not guaranteed that accumulator will always be
     * executed in the same thread.
     *
     * @param <U> The type of the result
     * @param seed the starting value
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element into a result
     * @return the result of the folding
     * @see #foldRight(Object, BiFunction)
     * @see #reduce(Object, BinaryOperator)
     * @see #reduce(Object, BiFunction, BinaryOperator)
     * @since 0.2.0
     */
    public <U extends @Nullable Object> U foldLeft(U seed, BiFunction<U, ? super T, U> accumulator) {
        Box<U> result = new Box<>(seed);
        forEachOrdered(t -> result.a = accumulator.apply(result.a, t));
        return result.a;
    }

    /**
     * Folds the elements of this stream using the provided accumulation
     * function, going left to right. This is equivalent to:
     *
     * <pre>
     * {@code
     *     boolean foundAny = false;
     *     T result = null;
     *     for (T element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.apply(result, element);
     *     }
     *     return foundAny ? Optional.of(result) : Optional.empty();
     * }
     * </pre>
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * This method cannot take all the advantages of parallel streams as it must
     * process elements strictly left to right. If your accumulator function is
     * associative, consider using {@link #reduce(BinaryOperator)} method.
     *
     * <p>
     * For parallel stream, it's not guaranteed that accumulator will always be
     * executed in the same thread.
     *
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element into a result
     * @return the result of the folding
     * @throws NullPointerException if the stream is non-empty, and the result of the folding is null
     * @see #foldLeft(Object, BiFunction)
     * @see #foldRight(BinaryOperator)
     * @see #reduce(BinaryOperator)
     * @since 0.4.0
     */
    public Optional<T> foldLeft(BinaryOperator<T> accumulator) {
        Box<T> result = new Box<>(none());
        forEachOrdered(t -> result.a = result.a == NONE ? t : accumulator.apply(result.a, t));
        if (result.a == NONE) return Optional.empty();
        //Throwing NPE here if the result is null is expected and specified
        //noinspection DataFlowIssue
        return Optional.of(result.a);
    }

    /**
     * Folds the elements of this stream using the provided seed object and
     * accumulation function, going right to left.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * As this method must process elements strictly right to left, it cannot
     * start processing till all the previous stream stages complete. Also, it
     * requires intermediate memory to store the whole content of the stream as
     * the stream's natural order is left to right. If your accumulator function
     * is associative, and you can provide a combiner function, consider using
     * {@link #reduce(Object, BiFunction, BinaryOperator)} method.
     *
     * <p>
     * For parallel stream, it's not guaranteed that accumulator will always be
     * executed in the same thread.
     *
     * @param <U> The type of the result
     * @param seed the starting value
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element into a result
     * @return the result of the folding
     * @see #foldLeft(Object, BiFunction)
     * @see #reduce(Object, BinaryOperator)
     * @see #reduce(Object, BiFunction, BinaryOperator)
     * @since 0.2.2
     */
    public <U extends @Nullable Object> U foldRight(U seed, BiFunction<? super T, U, U> accumulator) {
        return toListAndThen(list -> {
            U result = seed;
            for (int i = list.size() - 1; i >= 0; i--)
                result = accumulator.apply(list.get(i), result);
            return result;
        });
    }

    /**
     * Folds the elements of this stream using the provided accumulation
     * function, going right to left.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * As this method must process elements strictly right to left, it cannot
     * start processing till all the previous stream stages complete. Also, it
     * requires intermediate memory to store the whole content of the stream as
     * the stream's natural order is left to right. If your accumulator function
     * is associative, consider using {@link #reduce(BinaryOperator)} method.
     *
     * <p>
     * For parallel stream, it's not guaranteed that accumulator will always be
     * executed in the same thread.
     *
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element into a result
     * @return the result of the folding
     * @throws NullPointerException if the stream is non-empty, and the result of the folding is null
     * @see #foldRight(Object, BiFunction)
     * @see #foldLeft(BinaryOperator)
     * @see #reduce(BinaryOperator)
     * @since 0.4.0
     */
    public Optional<T> foldRight(BinaryOperator<T> accumulator) {
      Function<List<T>, Optional<T>> finisher = list -> {
        if (list.isEmpty())
          return Optional.empty();
        int i = list.size() - 1;
        T result = list.get(i--);
        for (; i >= 0; i--)
          result = accumulator.apply(list.get(i), result);
        //Throwing NPE here if the result is null is expected and specified
        //noinspection DataFlowIssue
        return Optional.of(result);
      };
      return toListAndThen(finisher);
    }

    /**
     * Produces a list containing cumulative results of applying the
     * accumulation function going left to right using a given seed value.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * The resulting {@link List} is guaranteed to be mutable.
     *
     * <p>
     * For parallel stream, it's not guaranteed that accumulator will always be
     * executed in the same thread.
     *
     * <p>
     * This method cannot take all the advantages of parallel streams as it must
     * process elements strictly left to right.
     *
     * @param <U> The type of the result
     * @param seed the starting value
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element into a result
     * @return the {@code List} where the first element is the seed, and every
     *         successor element is the result of applying accumulator function
     *         to the previous list element and the corresponding stream
     *         element. The resulting list is one element longer than this
     *         stream.
     * @see #foldLeft(Object, BiFunction)
     * @see #scanRight(Object, BiFunction)
     * @since 0.2.1
     */
    public <U extends @Nullable Object> List<U> scanLeft(U seed, BiFunction<U, ? super T, U> accumulator) {
        List<U> result = new ArrayList<>();
        result.add(seed);
        forEachOrdered(t -> result.add(accumulator.apply(result.get(result.size() - 1), t)));
        return result;
    }

    /**
     * Produces a list containing cumulative results of applying the
     * accumulation function going left to right.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * The resulting {@link List} is guaranteed to be mutable.
     *
     * <p>
     * For parallel stream, it's not guaranteed that accumulator will always be
     * executed in the same thread.
     *
     * <p>
     * This method cannot take all the advantages of parallel streams as it must
     * process elements strictly left to right.
     *
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element into a result
     * @return the {@code List} where the first element is the first element of
     *         this stream and every successor element is the result of applying
     *         accumulator function to the previous list element and the
     *         corresponding stream element. The resulting list has the same
     *         size as this stream.
     * @see #foldLeft(BinaryOperator)
     * @see #scanRight(BinaryOperator)
     * @see #prefix(BinaryOperator)
     * @since 0.4.0
     */
    public List<T> scanLeft(BinaryOperator<T> accumulator) {
        List<T> result = new ArrayList<>();
        forEachOrdered(t -> {
            if (result.isEmpty())
                result.add(t);
            else
                result.add(accumulator.apply(result.get(result.size() - 1), t));
        });
        return result;
    }

    /**
     * Produces a list containing cumulative results of applying the
     * accumulation function going right to left using a given seed value.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * The resulting {@link List} is guaranteed to be mutable.
     *
     * <p>
     * For parallel stream, it's not guaranteed that accumulator will always be
     * executed in the same thread.
     *
     * <p>
     * This method cannot take all the advantages of parallel streams as it must
     * process elements strictly right to left.
     *
     * @param <U> The type of the result
     * @param seed the starting value
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element into a result
     * @return the {@code List} where the last element is the seed and every
     *         predecessor element is the result of applying accumulator
     *         function to the corresponding stream element and the next list
     *         element. The resulting list is one element longer than this
     *         stream.
     * @see #scanLeft(Object, BiFunction)
     * @see #foldRight(Object, BiFunction)
     * @since 0.2.2
     */
    @SuppressWarnings("unchecked")
    public <U extends @Nullable Object> List<U> scanRight(U seed, BiFunction<? super T, U, U> accumulator) {
      Function<List<T>, List<U>> finisher = list -> {
        // Reusing the list for the different object type as it will save memory
        List<U> result = (List<U>) list;
        result.add(seed);
        for (int i = result.size() - 2; i >= 0; i--) {
          result.set(i, accumulator.apply((T) result.get(i), result.get(i + 1)));
        }
        return result;
      };
      return toListAndThen(finisher);
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
     * For parallel stream, it's not guaranteed that accumulator will always be
     * executed in the same thread.
     *
     * <p>
     * This method cannot take all the advantages of parallel streams as it must
     * process elements strictly right to left.
     *
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element into a result
     * @return the {@code List} where the last element is the last element of
     *         this stream, and every predecessor element is the result of
     *         applying accumulator function to the corresponding stream element
     *         and the next list element. The resulting list is one element
     *         longer than this stream.
     * @see #scanLeft(BinaryOperator)
     * @see #foldRight(BinaryOperator)
     * @since 0.4.0
     */
    public List<T> scanRight(BinaryOperator<T> accumulator) {
        return toListAndThen(list -> {
            for (int i = list.size() - 2; i >= 0; i--) {
                list.set(i, accumulator.apply(list.get(i), list.get(i + 1)));
            }
            return list;
        });
    }

    /**
     * Returns a stream consisting of all elements from this stream until the
     * first element which does not match the given predicate is found.
     *
     * <p>
     * This is a short-circuiting stateful operation. It can be either <a
     * href="package-summary.html#StreamOps">intermediate or
     * quasi-intermediate</a>. When using with JDK 1.9 or higher, it calls the
     * corresponding JDK 1.9 implementation. When using with JDK 1.8, it uses its own
     * implementation.
     *
     * <p>
     * While this operation is quite cheap for sequential stream, it can be
     * quite expensive on parallel pipelines. Using unordered source or making it
     * explicitly unordered with {@link #unordered()} call may improve the parallel
     * processing performance if semantics permit.
     *
     * @param predicate a non-interfering, stateless predicate to apply to
     *        elements.
     * @return the new stream.
     * @since 0.3.6
     * @see #takeWhileInclusive(Predicate)
     * @see #dropWhile(Predicate)
     */
    public @NonNull S takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return VerSpec.VER_SPEC.callWhile(this, predicate, false);
    }

    /**
     * Returns a stream consisting of all elements from this stream until the
     * first element which does not match the given predicate is found
     * (including the first mismatching element).
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     *
     * <p>
     * While this operation is quite cheap for sequential stream, it can be
     * quite expensive on parallel pipelines. Using unordered source or making it
     * explicitly unordered with {@link #unordered()} call may improve the parallel
     * processing performance if semantics permit.
     *
     * @param predicate a non-interfering, stateless predicate to apply to
     *        elements.
     * @return the new stream.
     * @since 0.5.5
     * @see #takeWhile(Predicate)
     */
    public @NonNull S takeWhileInclusive(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        Spliterator<T> spltr = spliterator();
        return supply(
                spltr.hasCharacteristics(Spliterator.ORDERED) ? new TakeDrop.TDOfRef<>(spltr, false, true, predicate)
                        : new TakeDrop.UnorderedTDOfRef<>(spltr, false, true, predicate));
    }

    /**
     * Returns a stream consisting of all elements from this stream starting
     * from the first element which does not match the given predicate. If the
     * predicate is true for all stream elements, an empty stream is returned.
     *
     * <p>
     * This is a stateful operation. It can be either <a
     * href="package-summary.html#StreamOps">intermediate or
     * quasi-intermediate</a>. When using with JDK 1.9 or higher, it calls the
     * corresponding JDK 1.9 implementation. When using with JDK 1.8, it uses its own
     * implementation.
     *
     * <p>
     * While this operation is quite cheap for sequential stream, it can be
     * quite expensive on parallel pipelines. Using unordered source or making it
     * explicitly unordered with {@link #unordered()} call may improve the parallel
     * processing performance if semantics permit.
     *
     * @param predicate a non-interfering, stateless predicate to apply to
     *        elements.
     * @return the new stream.
     * @since 0.3.6
     */
    public @NonNull S dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return VerSpec.VER_SPEC.callWhile(this, predicate, true);
    }

    /**
     * Returns a stream containing cumulative results of applying the
     * accumulation function going left to right.
     *
     * <p>
     * This is a stateful <a
     * href="package-summary.html#StreamOps">quasi-intermediate</a> operation.
     *
     * <p>
     * This operation resembles {@link #scanLeft(BinaryOperator)}, but unlike
     * {@code scanLeft} this operation is intermediate and accumulation function
     * must be associative.
     *
     * <p>
     * This method cannot take all the advantages of parallel streams as it must
     * process elements strictly left to right. Using an unordered source or
     * removing the ordering constraint with {@link #unordered()} may improve
     * the parallel processing speed.
     *
     * @param op an <a
     *        href="package-summary.html#Associativity">associative</a>, <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for computing the next element based on the previous one
     * @return the new stream.
     * @see #scanLeft(BinaryOperator)
     * @since 0.6.1
     */
    public @NonNull S prefix(BinaryOperator<T> op) {
        Spliterator<T> spltr = spliterator();
        return supply(spltr.hasCharacteristics(Spliterator.ORDERED) ? new PrefixOps.OfRef<>(spltr, op)
                : new PrefixOps.OfUnordRef<>(spltr, op));
    }

    // Necessary to generate proper Javadoc
    @SuppressWarnings("unchecked")
    @Override
    public <U extends @Nullable Object> U chain(Function<? super S, U> mapper) {
        return mapper.apply((S) this);
    }
}
