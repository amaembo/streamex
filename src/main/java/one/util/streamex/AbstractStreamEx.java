/*
 * Copyright 2015, 2023 StreamEx contributors
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ForkJoinPool;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static one.util.streamex.InternalUtilities.*;
import static one.util.streamex.Internals.ArrayCollection;
import static one.util.streamex.Internals.Box;
import static one.util.streamex.Internals.CancelException;
import static one.util.streamex.Internals.CancellableCollectorImpl;
import static one.util.streamex.Internals.IMMUTABLE_TO_LIST;
import static one.util.streamex.Internals.NONE;
import static one.util.streamex.Internals.NO_CHARACTERISTICS;
import static one.util.streamex.Internals.ObjDoubleBox;
import static one.util.streamex.Internals.ObjIntBox;
import static one.util.streamex.Internals.ObjLongBox;
import static one.util.streamex.Internals.PairBox;
import static one.util.streamex.Internals.finished;
import static one.util.streamex.Internals.none;
import one.util.functionex.*;

/**
 * Base class providing common functionality for {@link StreamEx} and {@link EntryStream}.
 *
 * @author Tagir Valeev
 *
 * @param <T> the type of the stream elements
 * @param <S> the type of the stream extending {@code AbstractStreamEx}
 */
public abstract class AbstractStreamEx<T, S extends AbstractStreamEx<T, S>> extends
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
        return StreamSupport.stream(spliterator, context.parallel);
    }

    final <K, V, M extends Map<K, V>> M toMapThrowing(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valMapper, M map) {
        forEach(t -> addToMap(map, keyMapper.apply(t), Objects.requireNonNull(valMapper.apply(t))));
        return map;
    }

    static <K, V, M extends Map<K, V>> void addToMap(M map, K key, V val) {
        V oldVal = map.putIfAbsent(key, val);
        if (oldVal != null) {
            throw new IllegalStateException("Duplicate entry for key '" + key + "' (attempt to merge values '" + oldVal
                + "' and '" + val + "')");
        }
    }

    <R, A> R rawCollect(Collector<? super T, A, R> collector) {
        if (context.fjp != null)
            return context.terminate(collector, stream()::collect);
        return stream().collect(collector);
    }

    @SuppressWarnings("unchecked")
    S appendSpliterator(Stream<? extends T> other, Spliterator<? extends T> right) {
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
    S prependSpliterator(Stream<? extends T> other, Spliterator<? extends T> left) {
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
    S ifEmpty(Stream<? extends T> other, Spliterator<? extends T> right) {
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
    public S sequential() {
        return (S) super.sequential();
    }

    @SuppressWarnings("unchecked")
    @Override
    public S parallel() {
        return (S) super.parallel();
    }

    @Override
    @SuppressWarnings("unchecked")
    public S parallel(ForkJoinPool fjp) {
        return (S) super.parallel(fjp);
    }

    @SuppressWarnings("unchecked")
    @Override
    public S unordered() {
        return (S) super.unordered();
    }

    @SuppressWarnings("unchecked")
    @Override
    public S onClose(Runnable closeHandler) {
        return (S) super.onClose(closeHandler);
    }

    /**
     * Declares that {@code exceptionType} may be thrown during processing stream
     * @param exceptionType exception type may be thrown
     * @return this
     * @param <X> Exception Type to throw
     * @throws X Exception type declared
     */
    @SuppressWarnings("unchecked")
    public <X extends Throwable> S throwing(Class<X> exceptionType) throws X {
        return (S) this;
    }

    /**
     * @see #nonNull()
     * @see #remove(Predicate)
     * @see StreamEx#select(Class)
     */
    @Override
    public S filter(Predicate<? super T> predicate) {
        return supply(stream().filter(predicate));
    }
    /**
     * Equivalent to {@link #filter(Predicate)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable> S filterThrowing(ThrowablePredicate1<X, ? super T> predicate) throws X {
        return tryFilter(predicate);
    }
    /**
     * Equivalent to {@link #filterThrowing(ThrowablePredicate1)} but hides the exception that mapper function may throw
     */
    public S tryFilter(ThrowablePredicate1<? extends Throwable, ? super T> predicate) {
        return filter(toPredicateSneakyThrowing(predicate));
    }

    @Override
    public <R> StreamEx<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return new StreamEx<>(stream().flatMap(mapper), context);
    }
    /**
     * Equivalent to {@link #flatMap(Function)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     *
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable, R> StreamEx<R> flatMapThrowing(ThrowableFunction1_1<X, ? super T, ? extends Stream<? extends R>> mapper) throws X {
        return tryFlatMap(mapper);
    }
    /**
     * Equivalent to {@link #flatMapThrowing(ThrowableFunction1_1)} but hides the exception that mapper function may throw
     */
    public <R> StreamEx<R> tryFlatMap(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends Stream<? extends R>> mapper) {
        return flatMap(toFunctionSneakyThrowing(mapper));
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
    public <R> StreamEx<R> mapMulti(BiConsumer<? super T, ? super Consumer<R>> mapper) {
        Objects.requireNonNull(mapper);
        return VerSpec.VER_SPEC.callMapMulti(this, mapper);
    }
    /**
     * Equivalent to {@link #mapMulti(BiConsumer)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @return the new stream
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable, R> StreamEx<R> mapMultiThrowing(ThrowableFunction2_0<X, ? super T, ? super Consumer<R>> mapper) throws X {
        return tryMapMulti(mapper);
    }
    /**
     * Equivalent to {@link #mapMultiThrowing(ThrowableFunction2_0)} but hides the exception that mapper function may throw
     */
    public <R> StreamEx<R> tryMapMulti(ThrowableFunction2_0<? extends Throwable, ? super T, ? super Consumer<R>> mapper) {
        Objects.requireNonNull(mapper);
        return mapMulti(toBiConsumerSneakyThrowing(mapper));
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
     * Equivalent to {@link #mapMultiToInt(BiConsumer)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable> IntStreamEx mapMultiToIntThrowing(ThrowableFunction2_0<X, ? super T, ? super IntConsumer> mapper) throws X {
        return tryMapMultiToInt(mapper);
    }
    /**
     * Equivalent to {@link #mapMultiToIntThrowing(ThrowableFunction2_0)} but hides the exception that mapper function may throw
     */
    public IntStreamEx tryMapMultiToInt(ThrowableFunction2_0<? extends Throwable, ? super T, ? super IntConsumer> mapper) {
        Objects.requireNonNull(mapper);
        return mapMultiToInt(toBiConsumerSneakyThrowing(mapper));
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
     * Equivalent to {@link #mapMultiToLong(BiConsumer)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable> LongStreamEx mapMultiToLongThrowing(ThrowableFunction2_0<X, ? super T, ? super LongConsumer> mapper) throws X {
        return tryMapMultiToLong(mapper);
    }
    /**
     * Equivalent to {@link #mapMultiToLongThrowing(ThrowableFunction2_0)} but hides the exception that mapper function may throw
     */
    public LongStreamEx tryMapMultiToLong(ThrowableFunction2_0<? extends Throwable, ? super T, ? super LongConsumer> mapper) {
        Objects.requireNonNull(mapper);
        return mapMultiToLong(toBiConsumerSneakyThrowing(mapper));
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
    /**
     * Equivalent to {@link #mapMultiToDouble(BiConsumer)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable> DoubleStreamEx mapMultiToDoubleThrowing(ThrowableFunction2_0<X, ? super T, ? super DoubleConsumer> mapper) throws X {
        return tryMapMultiToDouble(mapper);
    }
    /**
     * Equivalent to {@link #mapMultiToDoubleThrowing(ThrowableFunction2_0)} but hides the exception that mapper function may throw
     */
    public DoubleStreamEx tryMapMultiToDouble(ThrowableFunction2_0<? extends Throwable, ? super T, ? super DoubleConsumer> mapper) {
        Objects.requireNonNull(mapper);
        return mapMultiToDouble(toBiConsumerSneakyThrowing(mapper));
    }

    @Override
    public <R> StreamEx<R> map(Function<? super T, ? extends R> mapper) {
        return new StreamEx<>(stream().map(mapper), context);
    }
    /**
     * Equivalent to {@link #map(Function)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable, R> StreamEx<R> mapThrowing(ThrowableFunction1_1<X, ? super T, ? extends R> mapper ) throws X {
        return tryMap(mapper);
    }
    /**
     * Equivalent to {@link #mapThrowing(ThrowableFunction1_1)} but hides the exception that mapper function may throw
     */
    public <R> StreamEx<R> tryMap(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends R> mapper ) {
        return map(toFunctionSneakyThrowing(mapper));
    }

    @Override
    public IntStreamEx mapToInt(ToIntFunction<? super T> mapper) {
        return new IntStreamEx(stream().mapToInt(mapper), context);
    }
    /**
     * Equivalent to {@link #mapToInt(ToIntFunction)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable> IntStreamEx mapToIntThrowing(ThrowableFunction1_1<X, ? super T, ? extends Integer> mapper ) throws X {
        return tryMapToInt(mapper);
    }
    /**
     * Equivalent to {@link #mapToIntThrowing(ThrowableFunction1_1)} but hides the exception that mapper function may throw
     */
    public IntStreamEx tryMapToInt(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends Integer> mapper ) {
        return mapToInt(toIntFunctionSneakyThrowing(mapper));
    }

    @Override
    public LongStreamEx mapToLong(ToLongFunction<? super T> mapper) {
        return new LongStreamEx(stream().mapToLong(mapper), context);
    }
    /**
     * Equivalent to {@link #mapToLong(ToLongFunction)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable> LongStreamEx mapToLongThrowing(ThrowableFunction1_1<X, ? super T, ? extends Long> mapper ) throws X {
        return tryMapToLong(mapper);
    }
    /**
     * Equivalent to {@link #mapToLongThrowing(ThrowableFunction1_1)} but hides the exception that mapper function may throw
     */
    public LongStreamEx tryMapToLong(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends Long> mapper ) {
        return mapToLong(toLongFunctionSneakyThrowing(mapper));
    }

    @Override
    public DoubleStreamEx mapToDouble(ToDoubleFunction<? super T> mapper) {
        return new DoubleStreamEx(stream().mapToDouble(mapper), context);
    }
    /**
     * Equivalent to {@link #mapToDouble(ToDoubleFunction)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable> DoubleStreamEx mapToDoubleThrowing(ThrowableFunction1_1<X, ? super T, ? extends Double> mapper ) throws X {
        return tryMapToDouble(mapper);
    }
    /**
     * Equivalent to {@link #mapToDoubleThrowing(ThrowableFunction1_1)} but hides the exception that mapper function may throw
     */
    public DoubleStreamEx tryMapToDouble(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends Double> mapper ) {
        return mapToDouble(toDoubleFunctionSneakyThrowing(mapper));
    }

    @Override
    public IntStreamEx flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return new IntStreamEx(stream().flatMapToInt(mapper), context);
    }
    /**
     * Equivalent to {@link #flatMapToInt(Function)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable> IntStreamEx flatMapToIntThrowing(ThrowableFunction1_1<X, ? super T, ? extends IntStream> mapper ) throws X {
        return tryFlatMapToInt(mapper);
    }
    /**
     * Equivalent to {@link #flatMapToIntThrowing(ThrowableFunction1_1)} but hides the exception that mapper function may throw
     */
    public IntStreamEx tryFlatMapToInt(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends IntStream> mapper ) {
        return flatMapToInt(toFunctionSneakyThrowing(mapper));
    }

    @Override
    public LongStreamEx flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return new LongStreamEx(stream().flatMapToLong(mapper), context);
    }
    /**
     * Equivalent to {@link #flatMapToLong(Function)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable> LongStreamEx flatMapToLongThrowing(ThrowableFunction1_1<X, ? super T, ? extends LongStream> mapper ) throws X {
        return tryFlatMapToLong(mapper);
    }
    /**
     * Equivalent to {@link #flatMapToLongThrowing(ThrowableFunction1_1)} but hides the exception that mapper function may throw
     */
    public LongStreamEx tryFlatMapToLong(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends LongStream> mapper ) {
        return flatMapToLong(toFunctionSneakyThrowing(mapper));
    }

    @Override
    public DoubleStreamEx flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return new DoubleStreamEx(stream().flatMapToDouble(mapper), context);
    }
    /**
     * Equivalent to {@link #flatMapToDouble(Function)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable> DoubleStreamEx flatMapToDoubleThrowing(ThrowableFunction1_1<X, ? super T, ? extends DoubleStream> mapper ) throws X {
        return tryFlatMapToDouble(mapper);
    }
    /**
     * Equivalent to {@link #flatMapToDoubleThrowing(ThrowableFunction1_1)} but hides the exception that mapper function may throw
     */
    public DoubleStreamEx tryFlatMapToDouble(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends DoubleStream> mapper ) {
        return flatMapToDouble(toFunctionSneakyThrowing(mapper));
    }

    /**
     * Returns a new stream containing all the elements of the original stream interspersed with
     * given delimiter.
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
    public S intersperse(T delimiter) {
        return supply(stream().flatMap(s -> StreamEx.of(delimiter, s)).skip(1));
    }

    @Override
    public S distinct() {
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
    public S distinct(Function<? super T, ?> keyExtractor) {
        return supply(stream().map(t -> new PairBox<>(t, keyExtractor.apply(t))).distinct().map(box -> box.a));
    }

    /**
     * Returns a {@code StreamEx} consisting of the distinct elements (according
     * to {@link Object#equals(Object)}) which appear at least specified number
     * of times in this stream.
     *
     * <p>
     * This operation is not guaranteed to be stable: any of equal elements can
     * be selected for the output. However, if this stream is ordered then order
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
    public S distinct(long atLeast) {
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
    public S sorted() {
        return supply(stream().sorted());
    }

    @Override
    public S sorted(Comparator<? super T> comparator) {
        return supply(stream().sorted(comparator));
    }

    @Override
    public S peek(Consumer<? super T> action) {
        return supply(stream().peek(action));
    }

    @Override
    public S limit(long maxSize) {
        return supply(stream().limit(maxSize));
    }

    @Override
    public S skip(long n) {
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
    /**
     * Equivalent to {@link #forEach(Consumer)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable> void forEachThrowing(ThrowableFunction1_0<X, ? super T> action) throws X {
        tryForEach(action);
    }
    /**
     * Equivalent to {@link #forEachThrowing(ThrowableFunction1_0)} but hides the exception that mapper function may throw
     */
    public void tryForEach(ThrowableFunction1_0<? extends Throwable, ? super T> action) {
        forEach(toConsumerSneakyThrowing(action));
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
    /**
     * Equivalent to {@link #forEachOrdered(Consumer)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable> void forEachOrderedThrowing(ThrowableFunction1_0<X, ? super T> action) throws X {
        tryForEachOrdered(action);
    }
    /**
     * Equivalent to {@link #forEachOrderedThrowing(ThrowableFunction1_0)} but hides the exception that mapper function may throw
     */
    public void tryForEachOrdered(ThrowableFunction1_0<? extends Throwable, ? super T> action) {
        forEachOrdered(toConsumerSneakyThrowing(action));
    }

    @Override
    public Object[] toArray() {
        return toArray(Object[]::new);
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
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
    /**
     * Equivalent to {@link #reduce(Object, BinaryOperator)} but accepts Throwable Functions instead and declares exception
     * may accumulator function throw
     * @param <X> Exception Type the accumulator function may throw
     * @throws X Exception that accumulator function may throw
     */
    public <X extends Throwable> T reduceThrowing(T identity, ThrowableBinaryOperator<X, T> accumulator) throws X {
        return tryReduce(identity, accumulator);
    }
    /**
     * Equivalent to {@link #reduceThrowing(Object, ThrowableBinaryOperator)} but hides the exception that accumulator function may throw
     */
    public T tryReduce(T identity, ThrowableBinaryOperator<? extends Throwable, T> accumulator) {
        return reduce(identity, toBinaryOperatorSneakyThrowing(accumulator));
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        if (context.fjp != null)
            return context.terminate(accumulator, stream()::reduce);
        return stream().reduce(accumulator);
    }
    /**
     * Equivalent to {@link #reduce(BinaryOperator)} but accepts Throwable Functions instead and declares exception
     * may accumulator function throw
     * @param <X> Exception Type the accumulator function may throw
     * @throws X Exception that accumulator function may throw
     */
    public <X extends Throwable> Optional<T> reduceThrowing(ThrowableBinaryOperator<X, T> accumulator) throws X {
        return tryReduce(accumulator);
    }
    /**
     * Equivalent to {@link #reduceThrowing(ThrowableBinaryOperator)} but hides the exception that accumulator function may throw
     */
    public Optional<T> tryReduce(ThrowableBinaryOperator<? extends Throwable, T> accumulator) {
        return reduce(toBinaryOperatorSneakyThrowing(accumulator));
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        if (context.fjp != null)
            return context.terminate(() -> stream().reduce(identity, accumulator, combiner));
        return stream().reduce(identity, accumulator, combiner);
    }
    /**
     * Equivalent to {@link #reduce(Object, BiFunction, BinaryOperator)} but accepts Throwable Functions instead and declares exception
     * may accumulator or combiner functions throw
     * @param <X1> Exception Type the accumulator function may throw
     * @param <X2> Exception Type the combiner function may throw
     * @throws X1 Exception that accumulator function may throw
     * @throws X2 Exception that combiner function may throw
     */
    public <X1 extends Throwable, X2 extends Throwable, U> U reduceThrowing(U identity, ThrowableFunction2_1<X1, U, ? super T, U> accumulator, ThrowableBinaryOperator<X2, U> combiner) throws X1, X2 {
        return tryReduce(identity, accumulator, combiner);
    }
    /**
     * Equivalent to {@link #reduceThrowing(Object, ThrowableFunction2_1, ThrowableBinaryOperator)} but hides the exception that accumulator or combiner function may throw
     */
    public <U> U tryReduce(U identity, ThrowableFunction2_1<? extends Throwable, U, ? super T, U> accumulator, ThrowableBinaryOperator<? extends Throwable, U> combiner) {
        return reduce(identity, toBiFunctionSneakyThrowing(accumulator), toBinaryOperatorSneakyThrowing(combiner));
    }

    /**
     * Performs a possibly short-circuiting reduction of the stream elements using 
     * the provided {@code BinaryOperator}. The result is described as an {@code Optional<T>}.
     *
     * <p>
     * This is a short-circuiting terminal operation. It behaves like {@link #reduce(BinaryOperator)}. However,
     * it additionally accepts a zero element (also known as absorbing element). When zero element
     * is passed to the accumulator then the result must be zero as well. So the operation
     * takes the advantage of this and may short-circuit if zero is reached during the reduction.
     *
     * @param zero zero element
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>
     *        , <a href="package-summary.html#NonInterference">non-interfering
     *        </a>, <a href="package-summary.html#Statelessness">stateless</a>
     *        function to combine two elements into one.
     * @return the result of reduction. Empty Optional is returned if the input stream is empty.
     * @throws NullPointerException if accumulator is null or the result of reduction is null
     * @see MoreCollectors#reducingWithZero(Object, BinaryOperator) 
     * @see #reduceWithZero(Object, Object, BinaryOperator)
     * @see #reduce(BinaryOperator) 
     * @since 0.7.3
     */
    public Optional<T> reduceWithZero(T zero, BinaryOperator<T> accumulator) {
        return collect(MoreCollectors.reducingWithZero(zero, accumulator));
    }
    /**
     * Equivalent to {@link #reduceWithZero(Object, BinaryOperator)} but accepts Throwable Functions instead and declares exception
     * may accumulator function throw
     * @param <X> Exception Type the accumulator function may throw
     * @throws X Exception that accumulator function may throw
     */
    public <X extends Throwable> Optional<T> reduceWithZeroThrowing(T zero, ThrowableBinaryOperator<X, T> accumulator) throws X {
        return tryReduceWithZero(zero, accumulator);
    }
    /**
     * Equivalent to {@link #reduceWithZeroThrowing(Object, ThrowableBinaryOperator)} but hides the exception that accumulator function may throw
     */
    public Optional<T> tryReduceWithZero(T zero, ThrowableBinaryOperator<? extends Throwable, T> accumulator) {
        return reduceWithZero(zero, toBinaryOperatorSneakyThrowing(accumulator));
    }

    /**
     * Performs a possibly short-circuiting reduction of the stream elements using 
     * the provided identity value and a {@code BinaryOperator}.
     *
     * <p>
     * This is a short-circuiting terminal operation. It behaves like {@link #reduce(Object, BinaryOperator)}. 
     * However, it additionally accepts a zero element (also known as absorbing element). When zero element
     * is passed to the accumulator then the result must be zero as well. So the operation
     * takes the advantage of this and may short-circuit if zero is reached during the reduction.
     *
     * @param zero zero element
     * @param identity an identity element. For all {@code t}, {@code accumulator.apply(t, identity)} is
     *                 equal to {@code accumulator.apply(identity, t)} and is equal to {@code t}.
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>
     *        , <a href="package-summary.html#NonInterference">non-interfering
     *        </a>, <a href="package-summary.html#Statelessness">stateless</a>
     *        function to combine two elements into one.
     * @return the result of reduction. Empty Optional is returned if the input stream is empty.
     * @throws NullPointerException if accumulator is null or the result of reduction is null
     * @see MoreCollectors#reducingWithZero(Object, Object, BinaryOperator) 
     * @see #reduceWithZero(Object, BinaryOperator) 
     * @see #reduce(Object, BinaryOperator) 
     * @since 0.7.3
     */
    public T reduceWithZero(T zero, T identity, BinaryOperator<T> accumulator) {
        return collect(MoreCollectors.reducingWithZero(zero, identity, accumulator));
    }
    /**
     * Equivalent to {@link #reduceWithZero(Object, Object, BinaryOperator)} but accepts Throwable Functions instead and declares exception
     * may accumulator function throw
     * @param <X> Exception Type the accumulator function may throw
     * @throws X Exception that accumulator function may throw
     */
    public <X extends Throwable> T reduceWithZeroThrowing(T zero, T identity, ThrowableBinaryOperator<X, T> accumulator) throws X {
        return tryReduceWithZero(zero, identity, accumulator);
    }
    /**
     * Equivalent to {@link #reduceWithZeroThrowing(Object, Object, ThrowableBinaryOperator)} but hides the exception that accumulator function may throw
     */
    public T tryReduceWithZero(T zero, T identity, ThrowableBinaryOperator<? extends Throwable, T> accumulator) {
        return reduceWithZero(zero, identity, toBinaryOperatorSneakyThrowing(accumulator));
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        if (context.fjp != null)
            return context.terminate(() -> stream().collect(supplier, accumulator, combiner));
        return stream().collect(supplier, accumulator, combiner);
    }
    /**
     * Equivalent to {@link #collect(Supplier, BiConsumer, BiConsumer)} but accepts Throwable Functions instead and declares exception
     * may supplier, accumulator or combiner functions throw
     * @param <X1> Exception Type the supplier function may throw
     * @param <X2> Exception Type the accumulator function may throw
     * @param <X3> Exception Type the combiner function may throw
     * @throws X1 Exception that supplier function may throw
     * @throws X2 Exception that accumulator function may throw
     * @throws X3 Exception that combiner function may throw
     */
    public <X1 extends Throwable, X2 extends Throwable, X3 extends Throwable, R> R collectThrowing(ThrowableFunction0_1<X1, R> supplier, ThrowableFunction2_0<X2, R, ? super T> accumulator, ThrowableFunction2_0<X3, R, R> combiner) throws X1, X2, X3 {
        return tryCollect(supplier, accumulator, combiner);
    }
    /**
     * Equivalent to {@link #collectThrowing(ThrowableFunction0_1, ThrowableFunction2_0, ThrowableFunction2_0)} but hides the exception that
     * supplier, accumulator or combiner functions may throw
     */
    public <R> R tryCollect(ThrowableFunction0_1<? extends Throwable, R> supplier, ThrowableFunction2_0<? extends Throwable, R, ? super T> accumulator, ThrowableFunction2_0<? extends Throwable, R, R> combiner) {
        return collect(toSupplierSneakyThrowing(supplier), toBiConsumerSneakyThrowing(accumulator), toBiConsumerSneakyThrowing(combiner));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If special <a
     * href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> is passed, this operation becomes short-circuiting as well.
     */
    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
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
    /**
     * Equivalent to {@link #count(Predicate)} but accepts Throwable Functions instead and declares exception
     * may predicate function throw
     * @param <X> Exception Type the predicate function may throw
     * @throws X Exception that predicate function may throw
     */
    public <X extends Throwable> long countThrowing(ThrowablePredicate1<X, ? super T> predicate) throws X {
        return tryCount(predicate);
    }
    /**
     * Equivalent to {@link #countThrowing(ThrowablePredicate1)} but hides the exception that
     * predicate function may throw
     */
    public long tryCount(ThrowablePredicate1<? extends Throwable, ? super T> predicate) {
        return count(toPredicateSneakyThrowing(predicate));
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        if (context.fjp != null)
            return context.terminate(predicate, stream()::anyMatch);
        return stream().anyMatch(predicate);
    }
    /**
     * Equivalent to {@link #anyMatch(Predicate)} but accepts Throwable Functions instead and declares exception
     * may predicate function throw
     * @param <X> Exception Type the predicate function may throw
     * @throws X Exception that predicate function may throw
     */
    public <X extends Throwable> boolean anyMatchThrowing(ThrowablePredicate1<X, ? super T> predicate) throws X {
        return tryAnyMatch(predicate);
    }
    /**
     * Equivalent to {@link #anyMatchThrowing(ThrowablePredicate1)} but hides the exception that
     * predicate function may throw
     */
    public boolean tryAnyMatch(ThrowablePredicate1<? extends Throwable, ? super T> predicate) {
        return anyMatch(toPredicateSneakyThrowing(predicate));
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        if (context.fjp != null)
            return context.terminate(predicate, stream()::allMatch);
        return stream().allMatch(predicate);
    }
    /**
     * Equivalent to {@link #allMatch(Predicate)} but accepts Throwable Functions instead and declares exception
     * may predicate function throw
     * @param <X> Exception Type the predicate function may throw
     * @throws X Exception that predicate function may throw
     */
    public <X extends Throwable> boolean allMatchThrowing(ThrowablePredicate1<X, ? super T> predicate) throws X {
        return tryAllMatch(predicate);
    }
    /**
     * Equivalent to {@link #allMatchThrowing(ThrowablePredicate1)} but hides the exception that
     * predicate function may throw
     */
    public boolean tryAllMatch(ThrowablePredicate1<? extends Throwable, ? super T> predicate) {
        return allMatch(toPredicateSneakyThrowing(predicate));
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return !anyMatch(predicate);
    }
    /**
     * Equivalent to {@link #noneMatch(Predicate)} but accepts Throwable Functions instead and declares exception
     * may predicate function throw
     * @param <X> Exception Type the predicate function may throw
     * @throws X Exception that predicate function may throw
     */
    public <X extends Throwable> boolean noneMatchThrowing(ThrowablePredicate1<X, ? super T> predicate) throws X {
        return tryNoneMatch(predicate);
    }
    /**
     * Equivalent to {@link #noneMatchThrowing(ThrowablePredicate1)} but hides the exception that
     * predicate function may throw
     */
    public boolean tryNoneMatch(ThrowablePredicate1<? extends Throwable, ? super T> predicate) {
        return noneMatch(toPredicateSneakyThrowing(predicate));
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
        return collect(new CancellableCollectorImpl<>(() -> new long[]{-1}, (acc, t) -> {
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
        }, acc -> acc[0] < 0 ? OptionalLong.empty() : OptionalLong.of(acc[0]), acc -> acc[0] >= 0, NO_CHARACTERISTICS));
    }
    /**
     * Equivalent to {@link #indexOf(Predicate)} but accepts Throwable Functions instead and declares exception
     * may predicate function throw
     * @param <X> Exception Type the predicate function may throw
     * @throws X Exception that predicate function may throw
     */
    public <X extends Throwable> OptionalLong indexOfThrowing(ThrowablePredicate1<X, ? super T> predicate) throws X {
        return tryIndexOf(predicate);
    }
    /**
     * Equivalent to {@link #indexOfThrowing(ThrowablePredicate1)} but hides the exception that
     * predicate function may throw
     */
    public OptionalLong tryIndexOf(ThrowablePredicate1<? extends Throwable, ? super T> predicate) {
        return indexOf(toPredicateSneakyThrowing(predicate));
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
     * @param <R> The element type of the new stream
     * @param mapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to each element which produces a
     *        {@link Collection} of new values
     * @return the new stream
     */
    public <R> StreamEx<R> flatCollection(Function<? super T, ? extends Collection<? extends R>> mapper) {
        return flatMap(t -> {
            Collection<? extends R> c = mapper.apply(t);
            return c == null ? null : StreamEx.of(c.spliterator());
        });
    }
    /**
     * Equivalent to {@link #flatCollection(Function)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable, R> StreamEx<R> flatCollectionThrowing(ThrowableFunction1_1<X, ? super T, ? extends Collection<? extends R>> mapper) throws X {
        return tryFlatCollection(mapper);
    }
    /**
     * Equivalent to {@link #flatCollectionThrowing(ThrowableFunction1_1)} but hides the exception that
     * mapper function may throw
     */
    public <R> StreamEx<R> tryFlatCollection(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends Collection<? extends R>> mapper) {
        return flatCollection(toFunctionSneakyThrowing(mapper));
    }

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped array produced by applying
     * the provided mapping function to each element. (If a mapped array is
     * {@code null} nothing is added for given element to the resulting stream.)
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
    public <R> StreamEx<R> flatArray(Function<? super T, ? extends R[]> mapper) {
        return flatMap(t -> {
            R[] a = mapper.apply(t);
            return a == null ? null : StreamEx.of(Arrays.spliterator(a));
        });
    }
    /**
     * Equivalent to {@link #flatArray(Function)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable, R> StreamEx<R> flatArrayThrowing(ThrowableFunction1_1<X, ? super T, ? extends R[]> mapper) throws X {
        return tryFlatArray(mapper);
    }
    /**
     * Equivalent to {@link #flatArrayThrowing(ThrowableFunction1_1)} but hides the exception that
     * mapper function may throw
     */
    public <R> StreamEx<R> tryFlatArray(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends R[]> mapper) {
        return flatArray(toFunctionSneakyThrowing(mapper));
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
     * Equivalent to {@link #mapPartial(Function)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable, R> StreamEx<R> mapPartialThrowing(ThrowableFunction1_1<X, ? super T, ? extends Optional<? extends R>> mapper) throws X {
        return tryMapPartial(mapper);
    }
    /**
     * Equivalent to {@link #mapPartialThrowing(ThrowableFunction1_1)} but hides the exception that
     * mapper function may throw
     */
    public <R> StreamEx<R> tryMapPartial(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends Optional<? extends R>> mapper) {
        return mapPartial(toFunctionSneakyThrowing(mapper));
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
     * stream contains zero or one element the output stream will be empty.
     *
     * @param <R> The element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each
     *        adjacent pair of this stream elements.
     * @return the new stream
     * @since 0.2.1
     */
    public <R> StreamEx<R> pairMap(BiFunction<? super T, ? super T, ? extends R> mapper) {
        PairSpliterator.PSOfRef<T, R> spliterator = new PairSpliterator.PSOfRef<>(mapper, spliterator());
        return new StreamEx<>(spliterator, context);
    }
    /**
     * Equivalent to {@link #pairMap(BiFunction)} but accepts Throwable Functions instead and declares exception
     * may mapper function throw
     * @param <X> Exception Type the mapper function may throw
     * @throws X Exception that mapper function may throw
     */
    public <X extends Throwable, R> StreamEx<R> pairMapThrowing(ThrowableFunction2_1<X, ? super T, ? super T, ? extends R> mapper) throws X {
        return tryPairMap(mapper);
    }
    /**
     * Equivalent to {@link #pairMapThrowing(ThrowableFunction2_1)} but hides the exception that
     * mapper function may throw
     */
    public <R> StreamEx<R> tryPairMap(ThrowableFunction2_1<? extends Throwable, ? super T, ? super T, ? extends R> mapper) {
        return pairMap(toBiFunctionSneakyThrowing(mapper));
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
     * @see #filter(Predicate)
     * @see #remove(Predicate)
     * @see StreamEx#select(Class)
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
     * @param <V> the type of the {@code Comparable} sort key
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to be used to extract sorting keys
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
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to be used to extract sorting keys
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
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to be used to extract sorting keys
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
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to be used to extract sorting keys
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
     * of the keys extracted by provided function. This is a special case of a
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
     * extracted by provided function. This is a special case of a reduction.
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
     * extracted by provided function. This is a special case of a reduction.
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
     * extracted by provided function. This is a special case of a reduction.
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
    public S append(Stream<? extends T> other) {
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
    public S prepend(Stream<? extends T> other) {
        return prependSpliterator(other, other.spliterator());
    }

    /**
     * Returns a stream whose content is the same as this stream, except the case when
     * this stream is empty. In this case, its contents is replaced with other stream contents.
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
     * @param other other stream to replace the contents of this stream if this stream is empty.
     * @return the stream whose content is replaced by other stream contents only if this stream is empty.
     * @since 0.6.6
     */
    public S ifEmpty(Stream<? extends T> other) {
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
     * There's no guarantees on exact type of the returned {@code List}. The
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
     * guarantees on the type, serializability or thread-safety of the
     * {@code List} created.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param <R> the type of the result
     * @param finisher a function to be applied to the intermediate list
     * @return result of applying the finisher transformation to the list of the
     *         stream elements.
     * @since 0.2.3
     * @see #toList()
     */
    public <R> R toListAndThen(Function<? super List<T>, R> finisher) {
        if (context.fjp != null)
            return context.terminate(() -> finisher.apply(toMutableList()));
        return finisher.apply(toMutableList());
    }
    /**
     * Equivalent to {@link #toListAndThen(Function)} but accepts Throwable Functions instead and declares exception
     * may finisher function throw
     * @param <X> Exception Type the finisher function may throw
     * @throws X Exception that finisher function may throw
     */
    public <X extends Throwable, R> R toListAndThenThrowing(ThrowableFunction1_1<X, ? super List<T>, R> finisher) throws X {
        return toListAndThenTry(finisher);
    }
    /**
     * Equivalent to {@link #toListAndThenThrowing(ThrowableFunction1_1)} but hides the exception that
     * finisher function may throw
     */
    public <R> R toListAndThenTry(ThrowableFunction1_1<? extends Throwable, ? super List<T>, R> finisher) {
        return toListAndThen(toFunctionSneakyThrowing(finisher));
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
     * There's no guarantees on exact type of the returned {@code Set}. In
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
        if (result.size() == 0)
            return Collections.emptySet();
        return Collections.unmodifiableSet(result);
    }

    /**
     * Creates a {@link Set} containing the elements of this stream, then
     * performs finishing transformation and returns its result. There are no
     * guarantees on the type, serializability or thread-safety of the
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
    public <R> R toSetAndThen(Function<? super Set<T>, R> finisher) {
        if (context.fjp != null)
            return context.terminate(() -> finisher.apply(toMutableSet()));
        return finisher.apply(toMutableSet());
    }
    /**
     * Equivalent to {@link #toSetAndThen(Function)} but accepts Throwable Functions instead and declares exception
     * may finisher function throw
     * @param <X> Exception Type the finisher function may throw
     * @throws X Exception that finisher function may throw
     */
    public <X extends Throwable, R> R toSetAndThenThrowing(ThrowableFunction1_1<X, ? super Set<T>, R> finisher) throws X {
        return toSetAndThenTry(finisher);
    }
    /**
     * Equivalent to {@link #toSetAndThenThrowing(ThrowableFunction1_1)} but hides the exception that
     * finisher function may throw
     */
    public <R> R toSetAndThenTry(ThrowableFunction1_1<? extends Throwable, ? super Set<T>, R> finisher) {
        return toSetAndThen(toFunctionSneakyThrowing(finisher));
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
    public <C extends Collection<T>, R> R toCollectionAndThen(Supplier<C> collectionFactory, 
                                                              Function<? super C, R> finisher) {
        if (context.fjp != null)
            return context.terminate(() -> finisher.apply(toCollection(collectionFactory)));
        return finisher.apply(toCollection(collectionFactory));
    }
    /**
     * Equivalent to {@link #toCollectionAndThen(Supplier, Function)} but accepts Throwable Functions instead and declares exception
     * may collectionFactory or finisher functions throw
     * @param <X1> Exception Type the collectionFactory function may throw
     * @param <X2> Exception Type the finisher function may throw
     * @throws X1 Exception that collectionFactory function may throw
     * @throws X2 Exception that finisher function may throw
     */
    public <X1 extends Throwable, X2 extends Throwable, C extends Collection<T>, R> R toCollectionAndThenThrowing(ThrowableFunction0_1<X1, C> collectionFactory, ThrowableFunction1_1<X2, ? super C, R> finisher) throws X1, X2 {
        return toCollectionAndThenTry(collectionFactory, finisher);
    }
    /**
     * Equivalent to {@link #toCollectionAndThenThrowing(ThrowableFunction0_1, ThrowableFunction1_1)} but hides the exception that
     * collectionFactory or finisher functions may throw
     */
    public <C extends Collection<T>, R> R toCollectionAndThenTry(ThrowableFunction0_1<? extends Throwable, C> collectionFactory, ThrowableFunction1_1<? extends Throwable, ? super C, R> finisher) {
        return toCollectionAndThen(toSupplierSneakyThrowing(collectionFactory), toFunctionSneakyThrowing(finisher));
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
     * Equivalent to {@link #toCollection(Supplier)} but accepts Throwable Functions instead and declares exception
     * may collectionFactory function throw
     * @param <X> Exception Type the collectionFactory function may throw
     * @throws X Exception that collectionFactory function may throw
     */
    public <X extends Throwable, C extends Collection<T>> C toCollectionThrowing(ThrowableFunction0_1<X, C> collectionFactory) throws X {
        return tryToCollection(collectionFactory);
    }
    /**
     * Equivalent to {@link #toCollectionThrowing(ThrowableFunction0_1)} but hides the exception that
     * collectionFactory function may throw
     */
    public <C extends Collection<T>> C tryToCollection(ThrowableFunction0_1<? extends Throwable, C> collectionFactory) {
        return toCollection(toSupplierSneakyThrowing(collectionFactory));
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
     * For parallel stream it's not guaranteed that accumulator will always be
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
    public <U> U foldLeft(U seed, BiFunction<U, ? super T, U> accumulator) {
        Box<U> result = new Box<>(seed);
        forEachOrdered(t -> result.a = accumulator.apply(result.a, t));
        return result.a;
    }
    /**
     * Equivalent to {@link #foldLeft(Object, BiFunction)} but accepts Throwable Functions instead and declares exception
     * may accumulator function throw
     * @param <X> Exception Type the accumulator function may throw
     * @throws X Exception that accumulator function may throw
     */
    public <X extends Throwable, U> U foldLeftThrowing(U seed, ThrowableFunction2_1<X, U, ? super T, U> accumulator) throws X {
        return tryFoldLeft(seed, accumulator);
    }
    /**
     * Equivalent to {@link #foldLeftThrowing(Object, ThrowableFunction2_1)} but hides the exception that
     * accumulator function may throw
     */
    public <U> U tryFoldLeft(U seed, ThrowableFunction2_1<? extends Throwable, U, ? super T, U> accumulator) {
        return foldLeft(seed, toBiFunctionSneakyThrowing(accumulator));
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
     * For parallel stream it's not guaranteed that accumulator will always be
     * executed in the same thread.
     *
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element into a result
     * @return the result of the folding
     * @see #foldLeft(Object, BiFunction)
     * @see #foldRight(BinaryOperator)
     * @see #reduce(BinaryOperator)
     * @since 0.4.0
     */
    public Optional<T> foldLeft(BinaryOperator<T> accumulator) {
        Box<T> result = new Box<>(none());
        forEachOrdered(t -> result.a = result.a == NONE ? t : accumulator.apply(result.a, t));
        return result.a == NONE ? Optional.empty() : Optional.of(result.a);
    }
    /**
     * Equivalent to {@link #foldLeft(BinaryOperator)} but accepts Throwable Functions instead and declares exception
     * may accumulator function throw
     * @param <X> Exception Type the accumulator function may throw
     * @throws X Exception that accumulator function may throw
     */
    public <X extends Throwable> Optional<T> foldLeftThrowing(ThrowableBinaryOperator<X, T> accumulator) throws X {
        return tryFoldLeft(accumulator);
    }
    /**
     * Equivalent to {@link #foldLeftThrowing(ThrowableBinaryOperator)} but hides the exception that
     * accumulator function may throw
     */
    public Optional<T> tryFoldLeft(ThrowableBinaryOperator<? extends Throwable, T> accumulator) {
        return foldLeft(toBinaryOperatorSneakyThrowing(accumulator));
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
     * the stream natural order is left to right. If your accumulator function
     * is associative, and you can provide a combiner function, consider using
     * {@link #reduce(Object, BiFunction, BinaryOperator)} method.
     *
     * <p>
     * For parallel stream it's not guaranteed that accumulator will always be
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
    public <U> U foldRight(U seed, BiFunction<? super T, U, U> accumulator) {
        return toListAndThen(list -> {
            U result = seed;
            for (int i = list.size() - 1; i >= 0; i--)
                result = accumulator.apply(list.get(i), result);
            return result;
        });
    }
    /**
     * Equivalent to {@link #foldRight(Object, BiFunction)} but accepts Throwable Functions instead and declares exception
     * may accumulator function throw
     * @param <X> Exception Type the accumulator function may throw
     * @throws X Exception that accumulator function may throw
     */
    public <X extends Throwable, U> U foldRightThrowing(U seed, ThrowableFunction2_1<X, ? super T, U, U> accumulator) throws X {
        return tryFoldRight(seed, accumulator);
    }
    /**
     * Equivalent to {@link #foldRightThrowing(Object, ThrowableFunction2_1)} but hides the exception that
     * accumulator function may throw
     */
    public <U> U tryFoldRight(U seed, ThrowableFunction2_1<? extends Throwable, ? super T, U, U> accumulator) {
        return foldRight(seed, toBiFunctionSneakyThrowing(accumulator));
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
     * the stream natural order is left to right. If your accumulator function
     * is associative, consider using {@link #reduce(BinaryOperator)} method.
     *
     * <p>
     * For parallel stream it's not guaranteed that accumulator will always be
     * executed in the same thread.
     *
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element into a result
     * @return the result of the folding
     * @see #foldRight(Object, BiFunction)
     * @see #foldLeft(BinaryOperator)
     * @see #reduce(BinaryOperator)
     * @since 0.4.0
     */
    public Optional<T> foldRight(BinaryOperator<T> accumulator) {
        return toListAndThen(list -> {
            if (list.isEmpty())
                return Optional.empty();
            int i = list.size() - 1;
            T result = list.get(i--);
            for (; i >= 0; i--)
                result = accumulator.apply(list.get(i), result);
            return Optional.of(result);
        });
    }
    /**
     * Equivalent to {@link #foldRight(BinaryOperator)} but accepts Throwable Functions instead and declares exception
     * may accumulator function throw
     * @param <X> Exception Type the accumulator function may throw
     * @throws X Exception that accumulator function may throw
     */
    public <X extends Throwable> Optional<T> foldRightThrowing(ThrowableBinaryOperator<X, T> accumulator) throws X {
        return tryFoldRight(accumulator);
    }
    /**
     * Equivalent to {@link #foldRightThrowing(ThrowableBinaryOperator)} but hides the exception that
     * accumulator function may throw
     */
    public Optional<T> tryFoldRight(ThrowableBinaryOperator<? extends Throwable, T> accumulator) {
        return foldRight(toBinaryOperatorSneakyThrowing(accumulator));
    }

    /**
     * Produces a list containing cumulative results of applying the
     * accumulation function going left to right using given seed value.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * The resulting {@link List} is guaranteed to be mutable.
     *
     * <p>
     * For parallel stream it's not guaranteed that accumulator will always be
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
     * @return the {@code List} where the first element is the seed and every
     *         successor element is the result of applying accumulator function
     *         to the previous list element and the corresponding stream
     *         element. The resulting list is one element longer than this
     *         stream.
     * @see #foldLeft(Object, BiFunction)
     * @see #scanRight(Object, BiFunction)
     * @since 0.2.1
     */
    public <U> List<U> scanLeft(U seed, BiFunction<U, ? super T, U> accumulator) {
        List<U> result = new ArrayList<>();
        result.add(seed);
        forEachOrdered(t -> result.add(accumulator.apply(result.get(result.size() - 1), t)));
        return result;
    }
    /**
     * Equivalent to {@link #scanLeft(Object, BiFunction)} but accepts Throwable Functions instead and declares exception
     * may accumulator function throw
     * @param <X> Exception Type the accumulator function may throw
     * @throws X Exception that accumulator function may throw
     */
    public <X extends Throwable, U> List<U> scanLeftThrowing(U seed, ThrowableFunction2_1<X, U, ? super T, U> accumulator) throws X {
        return tryScanLeft(seed, accumulator);
    }
    /**
     * Equivalent to {@link #scanLeftThrowing(Object, ThrowableFunction2_1)} but hides the exception that
     * accumulator function may throw
     */
    public <U> List<U> tryScanLeft(U seed, ThrowableFunction2_1<? extends Throwable, U, ? super T, U> accumulator) {
        return scanLeft(seed, toBiFunctionSneakyThrowing(accumulator));
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
     * For parallel stream it's not guaranteed that accumulator will always be
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
     * Equivalent to {@link #scanLeft(BinaryOperator)} but accepts Throwable Functions instead and declares exception
     * may accumulator function throw
     * @param <X> Exception Type the accumulator function may throw
     * @throws X Exception that accumulator function may throw
     */
    public <X extends Throwable> List<T> scanLeftThrowing(ThrowableBinaryOperator<X, T> accumulator) throws X {
        return tryScanLeft(accumulator);
    }
    /**
     * Equivalent to {@link #scanLeftThrowing(ThrowableBinaryOperator)} but hides the exception that
     * accumulator function may throw
     */
    public List<T> tryScanLeft(ThrowableBinaryOperator<? extends Throwable, T> accumulator) {
        return scanLeft(toBinaryOperatorSneakyThrowing(accumulator));
    }

    /**
     * Produces a list containing cumulative results of applying the
     * accumulation function going right to left using given seed value.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * The resulting {@link List} is guaranteed to be mutable.
     *
     * <p>
     * For parallel stream it's not guaranteed that accumulator will always be
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
    public <U> List<U> scanRight(U seed, BiFunction<? super T, U, U> accumulator) {
        return toListAndThen(list -> {
            // Reusing the list for different object type as it will save memory
            List<U> result = (List<U>) list;
            result.add(seed);
            for (int i = result.size() - 2; i >= 0; i--) {
                result.set(i, accumulator.apply((T) result.get(i), result.get(i + 1)));
            }
            return result;
        });
    }
    /**
     * Equivalent to {@link #scanRight(Object, BiFunction)} but accepts Throwable Functions instead and declares exception
     * may accumulator function throw
     * @param <X> Exception Type the accumulator function may throw
     * @throws X Exception that accumulator function may throw
     */
    public <X extends Throwable, U> List<U> scanRightThrowing(U seed, ThrowableFunction2_1<X, ? super T, U, U> accumulator) throws X {
        return tryScanRight(seed, accumulator);
    }
    /**
     * Equivalent to {@link #scanRightThrowing(Object, ThrowableFunction2_1)} but hides the exception that
     * accumulator function may throw
     */
    public <U> List<U> tryScanRight(U seed, ThrowableFunction2_1<? extends Throwable, ? super T, U, U> accumulator) {
        return scanRight(seed, toBiFunctionSneakyThrowing(accumulator));
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
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element into a result
     * @return the {@code List} where the last element is the last element of
     *         this stream and every predecessor element is the result of
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
     * Equivalent to {@link #scanRight(BinaryOperator)} but accepts Throwable Functions instead and declares exception
     * may accumulator function throw
     * @param <X> Exception Type the accumulator function may throw
     * @throws X Exception that accumulator function may throw
     */
    public <X extends Throwable> List<T> scanRightThrowing(ThrowableBinaryOperator<X, T> accumulator) throws X {
        return tryScanRight(accumulator);
    }
    /**
     * Equivalent to {@link #scanRightThrowing(ThrowableBinaryOperator)} but hides the exception that
     * accumulator function may throw
     */
    public List<T> tryScanRight(ThrowableBinaryOperator<? extends Throwable, T> accumulator) {
        return scanRight(toBinaryOperatorSneakyThrowing(accumulator));
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
    public S takeWhile(Predicate<? super T> predicate) {
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
    public S takeWhileInclusive(Predicate<? super T> predicate) {
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
     * quasi-intermediate</a>. When using with JDK 1.9 or higher it calls the
     * corresponding JDK 1.9 implementation. When using with JDK 1.8 it uses own
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
    public S dropWhile(Predicate<? super T> predicate) {
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
    public S prefix(BinaryOperator<T> op) {
        Spliterator<T> spltr = spliterator();
        return supply(spltr.hasCharacteristics(Spliterator.ORDERED) ? new PrefixOps.OfRef<>(spltr, op)
                : new PrefixOps.OfUnordRef<>(spltr, op));
    }

    // Necessary to generate proper JavaDoc
    @SuppressWarnings("unchecked")
    @Override
    public <U> U chain(Function<? super S, U> mapper) {
        return mapper.apply((S) this);
    }

}
