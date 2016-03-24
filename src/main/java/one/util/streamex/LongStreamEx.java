/*
 * Copyright 2015, 2016 Tagir Valeev
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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfLong;
import java.util.Spliterators.AbstractLongSpliterator;
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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import one.util.streamex.StreamExInternals.Box;
import static one.util.streamex.StreamExInternals.*;

/**
 * A {@link LongStream} implementation with additional functionality
 * 
 * @author Tagir Valeev
 */
public class LongStreamEx extends BaseStreamEx<Long, LongStream, Spliterator.OfLong, LongStreamEx> implements
        LongStream {
    private static final class TDOfLong extends AbstractLongSpliterator implements LongConsumer {
        private final LongPredicate predicate;
        private final boolean drop;
        private final boolean inclusive;
        private boolean checked;
        private final Spliterator.OfLong source;
        private long cur;

        TDOfLong(Spliterator.OfLong source, boolean drop, boolean inclusive, LongPredicate predicate) {
            super(source.estimateSize(), source.characteristics()
                & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.inclusive = inclusive;
            this.source = source;
        }

        @Override
        public Comparator<? super Long> getComparator() {
            return source.getComparator();
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            if (drop) {
                if (checked)
                    return source.tryAdvance(action);
                while (source.tryAdvance(this)) {
                    if (!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if (!checked && source.tryAdvance(this) && (predicate.test(cur) || (checked = inclusive))) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            if (drop) {
                if (checked)
                    source.forEachRemaining(action);
                else {
                    source.forEachRemaining((long e) -> {
                        if (checked)
                            action.accept(e);
                        else {
                            if (!predicate.test(e)) {
                                checked = true;
                                action.accept(e);
                            }
                        }
                    });
                }
            } else
                super.forEachRemaining(action);
        }

        @Override
        public void accept(long t) {
            this.cur = t;
        }
    }

    LongStreamEx(LongStream stream, StreamContext context) {
        super(stream, context);
    }

    LongStreamEx(Spliterator.OfLong spliterator, StreamContext context) {
        super(spliterator, context);
    }

    @Override
    LongStream createStream() {
        return StreamSupport.longStream(spliterator, isParallel());
    }

    private static LongStreamEx seq(LongStream stream) {
        return new LongStreamEx(stream, StreamContext.SEQUENTIAL);
    }

    final LongStreamEx delegate(Spliterator.OfLong spliterator) {
        return new LongStreamEx(spliterator, context);
    }

    final LongStreamEx callWhile(LongPredicate predicate, int methodId) {
        try {
            return new LongStreamEx((LongStream) JDK9_METHODS[IDX_LONG_STREAM][methodId].invokeExact(stream(),
                predicate), context);
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    @Override
    public LongStreamEx unordered() {
        return (LongStreamEx) super.unordered();
    }

    @Override
    public LongStreamEx onClose(Runnable closeHandler) {
        return (LongStreamEx) super.onClose(closeHandler);
    }

    @Override
    public LongStreamEx filter(LongPredicate predicate) {
        return new LongStreamEx(stream().filter(predicate), context);
    }

    /**
     * Returns a stream consisting of the elements of this stream that don't
     * match the given predicate.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each
     *        element to determine if it should be excluded
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
     * @param value the value too look for in the stream
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
     * @param value the value to remove from the stream.
     * @return the new stream
     * @since 0.2.2
     * @see #without(long...)
     * @see #remove(LongPredicate)
     */
    public LongStreamEx without(long value) {
        return filter(val -> val != value);
    }

    /**
     * Returns a stream consisting of the elements of this stream that don't
     * equal to any of the supplied values.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation. May return itself if no values were supplied.
     * 
     * <p>
     * Current implementation scans the supplied values linearly for every
     * stream element.
     * 
     * <p>
     * If the {@code values} array is changed between calling this method and
     * finishing the stream traversal, then the result of the stream traversal
     * is undefined: changes may or may not be taken into account.
     *
     * @param values the values to remove from the stream.
     * @return the new stream
     * @since 0.5.5
     * @see #without(long)
     * @see #remove(LongPredicate)
     */
    public LongStreamEx without(long... values) {
        if (values.length == 0)
            return this;
        if (values.length == 1)
            return without(values[0]);
        return filter(x -> {
            for (long val : values) {
                if (x == val)
                    return false;
            }
            return true;
        });
    }

    /**
     * Returns a stream consisting of the elements of this stream that strictly
     * greater than the specified value.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param value a value to compare to
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
     * @param value a value to compare to
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
     * @param value a value to compare to
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
     * @param value a value to compare to
     * @return the new stream
     * @since 0.2.3
     */
    public LongStreamEx atMost(long value) {
        return filter(val -> val <= value);
    }

    @Override
    public LongStreamEx map(LongUnaryOperator mapper) {
        return new LongStreamEx(stream().map(mapper), context);
    }

    /**
     * Returns a stream where the first element is the replaced with the result
     * of applying the given function while the other elements are left intact.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     *
     * @param mapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to the first element
     * @return the new stream
     * @since 0.4.1
     */
    public LongStreamEx mapFirst(LongUnaryOperator mapper) {
        return delegate(new PairSpliterator.PSOfLong((a, b) -> b, mapper, spliterator(), PairSpliterator.MODE_MAP_FIRST));
    }

    /**
     * Returns a stream where the last element is the replaced with the result
     * of applying the given function while the other elements are left intact.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     *
     * <p>
     * The mapper function is called at most once. It could be not called at all
     * if the stream is empty or there is short-circuiting operation downstream.
     *
     * @param mapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to the last element
     * @return the new stream
     * @since 0.4.1
     */
    public LongStreamEx mapLast(LongUnaryOperator mapper) {
        return delegate(new PairSpliterator.PSOfLong((a, b) -> a, mapper, spliterator(), PairSpliterator.MODE_MAP_LAST));
    }

    @Override
    public <U> StreamEx<U> mapToObj(LongFunction<? extends U> mapper) {
        return new StreamEx<>(stream().mapToObj(mapper), context);
    }

    @Override
    public IntStreamEx mapToInt(LongToIntFunction mapper) {
        return new IntStreamEx(stream().mapToInt(mapper), context);
    }

    @Override
    public DoubleStreamEx mapToDouble(LongToDoubleFunction mapper) {
        return new DoubleStreamEx(stream().mapToDouble(mapper), context);
    }

    /**
     * Returns an {@link EntryStream} consisting of the {@link Entry} objects
     * which keys and values are results of applying the given functions to the
     * elements of this stream.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param <K> The {@code Entry} key type
     * @param <V> The {@code Entry} value type
     * @param keyMapper a non-interfering, stateless function to apply to each
     *        element
     * @param valueMapper a non-interfering, stateless function to apply to each
     *        element
     * @return the new stream
     * @since 0.3.1
     */
    public <K, V> EntryStream<K, V> mapToEntry(LongFunction<? extends K> keyMapper,
            LongFunction<? extends V> valueMapper) {
        return new EntryStream<>(stream().mapToObj(
            t -> new AbstractMap.SimpleImmutableEntry<>(keyMapper.apply(t), valueMapper.apply(t))), context);
    }

    @Override
    public LongStreamEx flatMap(LongFunction<? extends LongStream> mapper) {
        return new LongStreamEx(stream().flatMap(mapper), context);
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
     * @param mapper a non-interfering, stateless function to apply to each
     *        element which produces an {@code IntStream} of new values
     * @return the new stream
     * @since 0.3.0
     */
    public IntStreamEx flatMapToInt(LongFunction<? extends IntStream> mapper) {
        return new IntStreamEx(stream().mapToObj(mapper).flatMapToInt(Function.identity()), context);
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
     * @param mapper a non-interfering, stateless function to apply to each
     *        element which produces an {@code DoubleStream} of new values
     * @return the new stream
     * @since 0.3.0
     */
    public DoubleStreamEx flatMapToDouble(LongFunction<? extends DoubleStream> mapper) {
        return new DoubleStreamEx(stream().mapToObj(mapper).flatMapToDouble(Function.identity()), context);
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
     * @param <R> The element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each
     *        element which produces a {@code Stream} of new values
     * @return the new stream
     * @since 0.3.0
     */
    public <R> StreamEx<R> flatMapToObj(LongFunction<? extends Stream<R>> mapper) {
        return new StreamEx<>(stream().mapToObj(mapper).flatMap(Function.identity()), context);
    }

    @Override
    public LongStreamEx distinct() {
        return new LongStreamEx(stream().distinct(), context);
    }

    @Override
    public LongStreamEx sorted() {
        return new LongStreamEx(stream().sorted(), context);
    }

    /**
     * Returns a stream consisting of the elements of this stream sorted
     * according to the given comparator. Stream elements are boxed before
     * passing to the comparator.
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
    public LongStreamEx sorted(Comparator<Long> comparator) {
        return new LongStreamEx(stream().boxed().sorted(comparator).mapToLong(Long::longValue), context);
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
        LongUnaryOperator inv = x -> ~x;
        return new LongStreamEx(stream().map(inv).sorted().map(inv), context);
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
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to be used to extract sorting keys
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
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to be used to extract sorting keys
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
     * @param keyExtractor a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to be used to extract sorting keys
     * @return the new stream
     */
    public LongStreamEx sortedByDouble(LongToDoubleFunction keyExtractor) {
        return sorted(Comparator.comparingDouble(i -> keyExtractor.applyAsDouble(i)));
    }

    @Override
    public LongStreamEx peek(LongConsumer action) {
        return new LongStreamEx(stream().peek(action), context);
    }

    /**
     * Returns a stream consisting of the elements of this stream, additionally
     * performing the provided action on the first stream element when it's
     * consumed from the resulting stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * <p>
     * The action is called at most once. For parallel stream pipelines, it's
     * not guaranteed in which thread it will be executed, so if it modifies
     * shared state, it is responsible for providing the required
     * synchronization.
     *
     * <p>
     * Note that the action might not be called at all if the first element is
     * not consumed from the input (for example, if there's short-circuiting
     * operation downstream which stopped the stream before the first element).
     * 
     * <p>
     * This method exists mainly to support debugging.
     *
     * @param action a <a href="package-summary.html#NonInterference">
     *        non-interfering</a> action to perform on the first stream element
     *        as it is consumed from the stream
     * @return the new stream
     * @since 0.6.0
     */
    public LongStreamEx peekFirst(LongConsumer action) {
        return mapFirst(x -> {
            action.accept(x);
            return x;
        });
    }

    /**
     * Returns a stream consisting of the elements of this stream, additionally
     * performing the provided action on the last stream element when it's
     * consumed from the resulting stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * <p>
     * The action is called at most once. For parallel stream pipelines, it's
     * not guaranteed in which thread it will be executed, so if it modifies
     * shared state, it is responsible for providing the required
     * synchronization.
     * 
     * <p>
     * Note that the action might not be called at all if the last element is
     * not consumed from the input (for example, if there's short-circuiting
     * operation downstream).
     * 
     * <p>
     * This method exists mainly to support debugging.
     *
     * @param action a <a href="package-summary.html#NonInterference">
     *        non-interfering</a> action to perform on the first stream element
     *        as it is consumed from the stream
     * @return the new stream
     * @since 0.6.0
     */
    public LongStreamEx peekLast(LongConsumer action) {
        return mapLast(x -> {
            action.accept(x);
            return x;
        });
    }
    
    @Override
    public LongStreamEx limit(long maxSize) {
        return new LongStreamEx(stream().limit(maxSize), context);
    }

    @Override
    public LongStreamEx skip(long n) {
        return new LongStreamEx(stream().skip(n), context);
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
     * {@link #forEach(LongConsumer)}). This problem was fixed in OracleJDK
     * 8u60.
     * 
     * <p>
     * Also it behaves much better with infinite streams processed in parallel.
     * For example,
     * {@code LongStreamEx.iterate(0L, i->i+1).skip(1).limit(100).parallel().toArray()}
     * will likely to fail with {@code OutOfMemoryError}, but will work nicely
     * if {@code skip} is replaced with {@code skipOrdered}.
     *
     * <p>
     * For sequential streams this method behaves exactly like
     * {@link #skip(long)}.
     *
     * @param n the number of leading elements to skip
     * @return the new stream
     * @throws IllegalArgumentException if {@code n} is negative
     * @see #skip(long)
     * @since 0.3.2
     */
    public LongStreamEx skipOrdered(long n) {
        Spliterator.OfLong spliterator = (isParallel() ? StreamSupport.longStream(spliterator(), false) : stream())
                .skip(n).spliterator();
        return delegate(spliterator);
    }

    @Override
    public void forEach(LongConsumer action) {
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
    public void forEachOrdered(LongConsumer action) {
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
    public long[] toArray() {
        if (context.fjp != null)
            return context.terminate(stream()::toArray);
        return stream().toArray();
    }

    @Override
    public long reduce(long identity, LongBinaryOperator op) {
        if (context.fjp != null)
            return context.terminate(() -> stream().reduce(identity, op));
        return stream().reduce(identity, op);
    }

    @Override
    public OptionalLong reduce(LongBinaryOperator op) {
        if (context.fjp != null)
            return context.terminate(op, stream()::reduce);
        return stream().reduce(op);
    }

    /**
     * Folds the elements of this stream using the provided accumulation
     * function, going left to right. This is equivalent to:
     * 
     * <pre>
     * {@code
     *     boolean foundAny = false;
     *     long result = 0;
     *     for (long element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.apply(result, element);
     *     }
     *     return foundAny ? OptionalLong.of(result) : OptionalLong.empty();
     * }
     * </pre>
     * 
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * This method cannot take all the advantages of parallel streams as it must
     * process elements strictly left to right. If your accumulator function is
     * associative, consider using {@link #reduce(LongBinaryOperator)} method.
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
     * @see #foldLeft(long, LongBinaryOperator)
     * @see #reduce(LongBinaryOperator)
     * @since 0.4.0
     */
    public OptionalLong foldLeft(LongBinaryOperator accumulator) {
        PrimitiveBox b = new PrimitiveBox();
        forEachOrdered(t -> {
            if (b.b)
                b.l = accumulator.applyAsLong(b.l, t);
            else {
                b.l = t;
                b.b = true;
            }
        });
        return b.asLong();
    }

    /**
     * Folds the elements of this stream using the provided seed object and
     * accumulation function, going left to right. This is equivalent to:
     * 
     * <pre>
     * {@code
     *     long result = seed;
     *     for (long element : this stream)
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
     * associative, consider using {@link #reduce(long, LongBinaryOperator)}
     * method.
     * 
     * <p>
     * For parallel stream it's not guaranteed that accumulator will always be
     * executed in the same thread.
     *
     * @param seed the starting value
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element into a result
     * @return the result of the folding
     * @see #reduce(long, LongBinaryOperator)
     * @see #foldLeft(LongBinaryOperator)
     * @since 0.4.0
     */
    public long foldLeft(long seed, LongBinaryOperator accumulator) {
        long[] box = new long[] { seed };
        forEachOrdered(t -> box[0] = accumulator.applyAsLong(box[0], t));
        return box[0];
    }

    /**
     * Produces an array containing cumulative results of applying the
     * accumulation function going left to right.
     * 
     * <p>
     * This is a terminal operation.
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
     * @return the array where the first element is the first element of this
     *         stream and every successor element is the result of applying
     *         accumulator function to the previous array element and the
     *         corresponding stream element. The resulting array has the same
     *         length as this stream.
     * @see #foldLeft(LongBinaryOperator)
     * @since 0.5.1
     */
    public long[] scanLeft(LongBinaryOperator accumulator) {
        Spliterator.OfLong spliterator = spliterator();
        long size = spliterator.getExactSizeIfKnown();
        LongBuffer buf = new LongBuffer(size >= 0 && size <= Integer.MAX_VALUE ? (int) size : INITIAL_SIZE);
        delegate(spliterator).forEachOrdered(
            i -> buf.add(buf.size == 0 ? i : accumulator.applyAsLong(buf.data[buf.size - 1], i)));
        return buf.toArray();
    }

    /**
     * Produces an array containing cumulative results of applying the
     * accumulation function going left to right using given seed value.
     * 
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * For parallel stream it's not guaranteed that accumulator will always be
     * executed in the same thread.
     * 
     * <p>
     * This method cannot take all the advantages of parallel streams as it must
     * process elements strictly left to right.
     *
     * @param seed the starting value
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element into a result
     * @return the array where the first element is the seed and every successor
     *         element is the result of applying accumulator function to the
     *         previous array element and the corresponding stream element. The
     *         resulting array is one element longer than this stream.
     * @see #foldLeft(long, LongBinaryOperator)
     * @since 0.5.1
     */
    public long[] scanLeft(long seed, LongBinaryOperator accumulator) {
        return prepend(seed).scanLeft(accumulator);
    }

    /**
     * {@inheritDoc}
     *
     * @see #collect(LongCollector)
     */
    @Override
    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        if (context.fjp != null)
            return context.terminate(() -> stream().collect(supplier, accumulator, combiner));
        return stream().collect(supplier, accumulator, combiner);
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
     * @param <A> the intermediate accumulation type of the
     *        {@code LongCollector}
     * @param <R> type of the result
     * @param collector the {@code LongCollector} describing the reduction
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
     * @param comparator a non-interfering, stateless {@link Comparator} to
     *        compare elements of this stream
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
     * @param <V> the type of the {@code Comparable} sort key
     * @param keyExtractor a non-interfering, stateless function
     * @return an {@code OptionalLong} describing the first element of this
     *         stream for which the lowest value was returned by key extractor,
     *         or an empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public <V extends Comparable<? super V>> OptionalLong minBy(LongFunction<V> keyExtractor) {
        ObjLongBox<V> result = collect(() -> new ObjLongBox<>(null, 0), (box, i) -> {
            V val = Objects.requireNonNull(keyExtractor.apply(i));
            if (box.a == null || box.a.compareTo(val) > 0) {
                box.a = val;
                box.b = i;
            }
        }, (box1, box2) -> {
            if (box2.a != null && (box1.a == null || box1.a.compareTo(box2.a) > 0)) {
                box1.a = box2.a;
                box1.b = box2.b;
            }
        });
        return result.a == null ? OptionalLong.empty() : OptionalLong.of(result.b);
    }

    /**
     * Returns the minimum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param keyExtractor a non-interfering, stateless function
     * @return an {@code OptionalLong} describing the first element of this
     *         stream for which the lowest value was returned by key extractor,
     *         or an empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public OptionalLong minByInt(LongToIntFunction keyExtractor) {
        return collect(PrimitiveBox::new, (box, l) -> {
            int key = keyExtractor.applyAsInt(l);
            if (!box.b || box.i > key) {
                box.b = true;
                box.i = key;
                box.l = l;
            }
        }, PrimitiveBox.MIN_INT).asLong();
    }

    /**
     * Returns the minimum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param keyExtractor a non-interfering, stateless function
     * @return an {@code OptionalLong} describing the first element of this
     *         stream for which the lowest value was returned by key extractor,
     *         or an empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public OptionalLong minByLong(LongUnaryOperator keyExtractor) {
        long[] result = collect(() -> new long[3], (acc, l) -> {
            long key = keyExtractor.applyAsLong(l);
            if (acc[2] == 0 || acc[1] > key) {
                acc[0] = l;
                acc[1] = key;
                acc[2] = 1;
            }
        }, (acc1, acc2) -> {
            if (acc2[2] == 1 && (acc1[2] == 0 || acc1[1] > acc2[1]))
                System.arraycopy(acc2, 0, acc1, 0, 3);
        });
        return result[2] == 1 ? OptionalLong.of(result[0]) : OptionalLong.empty();
    }

    /**
     * Returns the minimum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param keyExtractor a non-interfering, stateless function
     * @return an {@code OptionalLong} describing the first element of this
     *         stream for which the lowest value was returned by key extractor,
     *         or an empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public OptionalLong minByDouble(LongToDoubleFunction keyExtractor) {
        return collect(PrimitiveBox::new, (box, l) -> {
            double key = keyExtractor.applyAsDouble(l);
            if (!box.b || Double.compare(box.d, key) > 0) {
                box.b = true;
                box.d = key;
                box.l = l;
            }
        }, PrimitiveBox.MIN_DOUBLE).asLong();
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
     * @param comparator a non-interfering, stateless {@link Comparator} to
     *        compare elements of this stream
     * @return an {@code OptionalLong} describing the maximum element of this
     *         stream, or an empty {@code OptionalLong} if the stream is empty
     */
    public OptionalLong max(Comparator<Long> comparator) {
        return reduce((a, b) -> comparator.compare(a, b) >= 0 ? a : b);
    }

    /**
     * Returns the maximum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param <V> the type of the {@code Comparable} sort key
     * @param keyExtractor a non-interfering, stateless function
     * @return an {@code OptionalLong} describing the first element of this
     *         stream for which the highest value was returned by key extractor,
     *         or an empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public <V extends Comparable<? super V>> OptionalLong maxBy(LongFunction<V> keyExtractor) {
        ObjLongBox<V> result = collect(() -> new ObjLongBox<>(null, 0), (box, i) -> {
            V val = Objects.requireNonNull(keyExtractor.apply(i));
            if (box.a == null || box.a.compareTo(val) < 0) {
                box.a = val;
                box.b = i;
            }
        }, (box1, box2) -> {
            if (box2.a != null && (box1.a == null || box1.a.compareTo(box2.a) < 0)) {
                box1.a = box2.a;
                box1.b = box2.b;
            }
        });
        return result.a == null ? OptionalLong.empty() : OptionalLong.of(result.b);
    }

    /**
     * Returns the maximum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param keyExtractor a non-interfering, stateless function
     * @return an {@code OptionalLong} describing the first element of this
     *         stream for which the highest value was returned by key extractor,
     *         or an empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public OptionalLong maxByInt(LongToIntFunction keyExtractor) {
        return collect(PrimitiveBox::new, (box, l) -> {
            int key = keyExtractor.applyAsInt(l);
            if (!box.b || box.i < key) {
                box.b = true;
                box.i = key;
                box.l = l;
            }
        }, PrimitiveBox.MAX_INT).asLong();
    }

    /**
     * Returns the maximum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param keyExtractor a non-interfering, stateless function
     * @return an {@code OptionalLong} describing the first element of this
     *         stream for which the highest value was returned by key extractor,
     *         or an empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public OptionalLong maxByLong(LongUnaryOperator keyExtractor) {
        long[] result = collect(() -> new long[3], (acc, l) -> {
            long key = keyExtractor.applyAsLong(l);
            if (acc[2] == 0 || acc[1] < key) {
                acc[0] = l;
                acc[1] = key;
                acc[2] = 1;
            }
        }, (acc1, acc2) -> {
            if (acc2[2] == 1 && (acc1[2] == 0 || acc1[1] < acc2[1]))
                System.arraycopy(acc2, 0, acc1, 0, 3);
        });
        return result[2] == 1 ? OptionalLong.of(result[0]) : OptionalLong.empty();

    }

    /**
     * Returns the maximum element of this stream according to the provided key
     * extractor function.
     *
     * <p>
     * This is a terminal operation.
     *
     * @param keyExtractor a non-interfering, stateless function
     * @return an {@code OptionalLong} describing the first element of this
     *         stream for which the highest value was returned by key extractor,
     *         or an empty {@code OptionalLong} if the stream is empty
     * @since 0.1.2
     */
    public OptionalLong maxByDouble(LongToDoubleFunction keyExtractor) {
        return collect(PrimitiveBox::new, (box, l) -> {
            double key = keyExtractor.applyAsDouble(l);
            if (!box.b || Double.compare(box.d, key) < 0) {
                box.b = true;
                box.d = key;
                box.l = l;
            }
        }, PrimitiveBox.MAX_DOUBLE).asLong();
    }

    @Override
    public long count() {
        if (context.fjp != null)
            return context.terminate(stream()::count);
        return stream().count();
    }

    @Override
    public OptionalDouble average() {
        if (context.fjp != null)
            return context.terminate(stream()::average);
        return stream().average();
    }

    @Override
    public LongSummaryStatistics summaryStatistics() {
        return collect(LongSummaryStatistics::new, LongSummaryStatistics::accept, LongSummaryStatistics::combine);
    }

    @Override
    public boolean anyMatch(LongPredicate predicate) {
        if (context.fjp != null)
            return context.terminate(predicate, stream()::anyMatch);
        return stream().anyMatch(predicate);
    }

    @Override
    public boolean allMatch(LongPredicate predicate) {
        if (context.fjp != null)
            return context.terminate(predicate, stream()::allMatch);
        return stream().allMatch(predicate);
    }

    @Override
    public boolean noneMatch(LongPredicate predicate) {
        return !anyMatch(predicate);
    }

    @Override
    public OptionalLong findFirst() {
        if (context.fjp != null)
            return context.terminate(stream()::findFirst);
        return stream().findFirst();
    }

    /**
     * Returns an {@link OptionalLong} describing the first element of this
     * stream, which matches given predicate, or an empty {@code OptionalLong}
     * if there's no matching element.
     *
     * <p>
     * This is a short-circuiting terminal operation.
     *
     * @param predicate a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        predicate which returned value should match
     * @return an {@code OptionalLong} describing the first matching element of
     *         this stream, or an empty {@code OptionalLong} if there's no
     *         matching element
     * @see #findFirst()
     */
    public OptionalLong findFirst(LongPredicate predicate) {
        return filter(predicate).findFirst();
    }

    @Override
    public OptionalLong findAny() {
        if (context.fjp != null)
            return context.terminate(stream()::findAny);
        return stream().findAny();
    }

    /**
     * Returns an {@link OptionalLong} describing some element of the stream,
     * which matches given predicate, or an empty {@code OptionalLong} if
     * there's no matching element.
     *
     * <p>
     * This is a short-circuiting terminal operation.
     *
     * <p>
     * The behavior of this operation is explicitly nondeterministic; it is free
     * to select any element in the stream. This is to allow for maximal
     * performance in parallel operations; the cost is that multiple invocations
     * on the same source may not return the same result. (If a stable result is
     * desired, use {@link #findFirst(LongPredicate)} instead.)
     *
     * @param predicate a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        predicate which returned value should match
     * @return an {@code OptionalLong} describing some matching element of this
     *         stream, or an empty {@code OptionalLong} if there's no matching
     *         element
     * @see #findAny()
     * @see #findFirst(LongPredicate)
     */
    public OptionalLong findAny(LongPredicate predicate) {
        return filter(predicate).findAny();
    }

    /**
     * Returns an {@link OptionalLong} describing the zero-based index of the
     * first element of this stream, which equals to the given value, or an
     * empty {@code OptionalLong} if there's no matching element.
     *
     * <p>
     * This is a short-circuiting terminal operation.
     *
     * @param value a value to look for
     * @return an {@code OptionalLong} describing the index of the first
     *         matching element of this stream, or an empty {@code OptionalLong}
     *         if there's no matching element.
     * @see #indexOf(LongPredicate)
     * @since 0.4.0
     */
    public OptionalLong indexOf(long value) {
        return boxed().indexOf(i -> i == value);
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
     * @see #findFirst(LongPredicate)
     * @since 0.4.0
     */
    public OptionalLong indexOf(LongPredicate predicate) {
        return boxed().indexOf(predicate::test);
    }

    @Override
    public DoubleStreamEx asDoubleStream() {
        return new DoubleStreamEx(stream().asDoubleStream(), context);
    }

    @Override
    public StreamEx<Long> boxed() {
        return new StreamEx<>(stream().boxed(), context);
    }

    @Override
    public LongStreamEx sequential() {
        return (LongStreamEx) super.sequential();
    }

    @Override
    public LongStreamEx parallel() {
        return (LongStreamEx) super.parallel();
    }

    @Override
    public LongStreamEx parallel(ForkJoinPool fjp) {
        return (LongStreamEx) super.parallel(fjp);
    }

    @Override
    public OfLong iterator() {
        return Spliterators.iterator(spliterator());
    }

    /**
     * Returns a new {@code LongStreamEx} which is a concatenation of this
     * stream and the stream containing supplied values
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * @param values the values to append to the stream
     * @return the new stream
     */
    public LongStreamEx append(long... values) {
        if (values.length == 0)
            return this;
        return new LongStreamEx(LongStream.concat(stream(), LongStream.of(values)), context);
    }

    /**
     * Creates a lazily concatenated stream whose elements are all the elements
     * of this stream followed by all the elements of the other stream. The
     * resulting stream is ordered if both of the input streams are ordered, and
     * parallel if either of the input streams is parallel. When the resulting
     * stream is closed, the close handlers for both input streams are invoked.
     *
     * @param other the other stream
     * @return this stream appended by the other stream
     * @see LongStream#concat(LongStream, LongStream)
     */
    public LongStreamEx append(LongStream other) {
        return new LongStreamEx(LongStream.concat(stream(), other), context.combine(other));
    }

    /**
     * Returns a new {@code LongStreamEx} which is a concatenation of the stream
     * containing supplied values and this stream
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * @param values the values to prepend to the stream
     * @return the new stream
     */
    public LongStreamEx prepend(long... values) {
        if (values.length == 0)
            return this;
        return new LongStreamEx(LongStream.concat(LongStream.of(values), stream()), context);
    }

    /**
     * Creates a lazily concatenated stream whose elements are all the elements
     * of the other stream followed by all the elements of this stream. The
     * resulting stream is ordered if both of the input streams are ordered, and
     * parallel if either of the input streams is parallel. When the resulting
     * stream is closed, the close handlers for both input streams are invoked.
     *
     * @param other the other stream
     * @return this stream prepended by the other stream
     * @see LongStream#concat(LongStream, LongStream)
     */
    public LongStreamEx prepend(LongStream other) {
        return new LongStreamEx(LongStream.concat(other, stream()), context.combine(other));
    }

    /**
     * Returns a stream consisting of the results of applying the given function
     * to the every adjacent pair of elements of this stream.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * <p>
     * The output stream will contain one element less than this stream. If this
     * stream contains zero or one element the output stream will be empty.
     *
     * @param mapper a non-interfering, stateless function to apply to each
     *        adjacent pair of this stream elements.
     * @return the new stream
     * @since 0.2.1
     */
    public LongStreamEx pairMap(LongBinaryOperator mapper) {
        return delegate(new PairSpliterator.PSOfLong(mapper, null, spliterator(), PairSpliterator.MODE_PAIRS));
    }

    /**
     * Returns a {@link String} which contains the results of calling
     * {@link String#valueOf(long)} on each element of this stream, separated by
     * the specified delimiter, in encounter order.
     *
     * <p>
     * This is a terminal operation.
     * 
     * @param delimiter the delimiter to be used between each element
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
     * @param delimiter the delimiter to be used between each element
     * @param prefix the sequence of characters to be used at the beginning of
     *        the joined result
     * @param suffix the sequence of characters to be used at the end of the
     *        joined result
     * @return a {@code String}. For empty input stream empty String is
     *         returned.
     * @since 0.3.1
     */
    public String joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return collect(LongCollector.joining(delimiter, prefix, suffix));
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
     * @param predicate a non-interfering, stateless predicate to apply to
     *        elements.
     * @return the new stream.
     * @since 0.3.6
     * @see #takeWhileInclusive(LongPredicate)
     * @see #dropWhile(LongPredicate)
     */
    public LongStreamEx takeWhile(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        if (JDK9_METHODS != null) {
            return callWhile(predicate, IDX_TAKE_WHILE);
        }
        return delegate(new LongStreamEx.TDOfLong(spliterator(), false, false, predicate));
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
     * quite expensive on parallel pipelines.
     * 
     * @param predicate a non-interfering, stateless predicate to apply to
     *        elements.
     * @return the new stream.
     * @since 0.5.5
     * @see #takeWhile(LongPredicate)
     */
    public LongStreamEx takeWhileInclusive(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return delegate(new LongStreamEx.TDOfLong(spliterator(), false, true, predicate));
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
     * @param predicate a non-interfering, stateless predicate to apply to
     *        elements.
     * @return the new stream.
     * @since 0.3.6
     */
    public LongStreamEx dropWhile(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        if (JDK9_METHODS != null) {
            return callWhile(predicate, IDX_DROP_WHILE);
        }
        return delegate(new LongStreamEx.TDOfLong(spliterator(), true, false, predicate));
    }

    // Necessary to generate proper JavaDoc
    // does not add overhead as it appears in bytecode anyways as bridge method
    @Override
    public <U> U chain(Function<? super LongStreamEx, U> mapper) {
        return mapper.apply(this);
    }

    /**
     * Returns an empty sequential {@code LongStreamEx}.
     *
     * @return an empty sequential stream
     */
    public static LongStreamEx empty() {
        return of(Spliterators.emptyLongSpliterator());
    }

    /**
     * Returns a sequential {@code LongStreamEx} containing a single element.
     *
     * @param element the single element
     * @return a singleton sequential stream
     */
    public static LongStreamEx of(long element) {
        return of(new ConstSpliterator.OfLong(element, 1, true));
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} whose elements are the
     * specified values.
     *
     * @param elements the elements of the new stream
     * @return the new stream
     */
    public static LongStreamEx of(long... elements) {
        return of(Arrays.spliterator(elements));
    }

    /**
     * Returns a sequential {@link LongStreamEx} with the specified range of the
     * specified array as its source.
     *
     * @param array the array, assumed to be unmodified during use
     * @param startInclusive the first index to cover, inclusive
     * @param endExclusive index immediately past the last index to cover
     * @return an {@code LongStreamEx} for the array range
     * @throws ArrayIndexOutOfBoundsException if {@code startInclusive} is
     *         negative, {@code endExclusive} is less than
     *         {@code startInclusive}, or {@code endExclusive} is greater than
     *         the array size
     * @since 0.1.1
     * @see Arrays#stream(long[], int, int)
     */
    public static LongStreamEx of(long[] array, int startInclusive, int endExclusive) {
        return of(Arrays.spliterator(array, startInclusive, endExclusive));
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} whose elements are the
     * unboxed elements of supplied array.
     *
     * @param array the array to create the stream from.
     * @return the new stream
     * @see Arrays#stream(Object[])
     * @since 0.5.0
     */
    public static LongStreamEx of(Long[] array) {
        return seq(Arrays.stream(array).mapToLong(Long::longValue));
    }

    /**
     * Returns a {@code LongStreamEx} object which wraps given
     * {@link LongStream}.
     * 
     * <p>
     * The supplied stream must not be consumed or closed when this method is
     * called. No operation must be performed on the supplied stream after it's
     * wrapped.
     * 
     * @param stream original stream
     * @return the wrapped stream
     * @since 0.0.8
     */
    public static LongStreamEx of(LongStream stream) {
        return stream instanceof LongStreamEx ? (LongStreamEx) stream : new LongStreamEx(stream, StreamContext
                .of(stream));
    }

    /**
     * Returns a sequential {@link LongStreamEx} created from given
     * {@link java.util.Spliterator.OfLong}.
     * 
     * @param spliterator a spliterator to create the stream from.
     * @return the new stream
     * @since 0.3.4
     */
    public static LongStreamEx of(Spliterator.OfLong spliterator) {
        return new LongStreamEx(spliterator, StreamContext.SEQUENTIAL);
    }

    /**
     * Returns a sequential, ordered {@link LongStreamEx} created from given
     * {@link java.util.PrimitiveIterator.OfLong}.
     *
     * <p>
     * This method is roughly equivalent to
     * {@code LongStreamEx.of(Spliterators.spliteratorUnknownSize(iterator, ORDERED))}
     * , but may show better performance for parallel processing.
     * 
     * <p>
     * Use this method only if you cannot provide better Stream source.
     *
     * @param iterator an iterator to create the stream from.
     * @return the new stream
     * @since 0.5.1
     */
    public static LongStreamEx of(PrimitiveIterator.OfLong iterator) {
        return of(new UnknownSizeSpliterator.USOfLong(iterator));
    }

    /**
     * Returns a sequential {@code LongStreamEx} containing an
     * {@link OptionalLong} value, if present, otherwise returns an empty
     * {@code LongStreamEx}.
     *
     * @param optional the optional to create a stream of
     * @return a stream with an {@code OptionalLong} value if present, otherwise
     *         an empty stream
     * @since 0.1.1
     */
    public static LongStreamEx of(OptionalLong optional) {
        return optional.isPresent() ? of(optional.getAsLong()) : empty();
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} whose elements are the
     * unboxed elements of supplied collection.
     *
     * @param collection the collection to create the stream from.
     * @return the new stream
     * @see Collection#stream()
     */
    public static LongStreamEx of(Collection<Long> collection) {
        return seq(collection.stream().mapToLong(Long::longValue));
    }

    /**
     * Returns an effectively unlimited stream of pseudorandom {@code long}
     * values produced by given {@link Random} object.
     *
     * <p>
     * A pseudorandom {@code long} value is generated as if it's the result of
     * calling the method {@link Random#nextLong()}.
     *
     * @param random a {@link Random} object to produce the stream from
     * @return a stream of pseudorandom {@code long} values
     * @see Random#longs()
     */
    public static LongStreamEx of(Random random) {
        return seq(random.longs());
    }

    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code long} values.
     *
     * <p>
     * A pseudorandom {@code long} value is generated as if it's the result of
     * calling the method {@link Random#nextLong()}
     *
     * @param random a {@link Random} object to produce the stream from
     * @param streamSize the number of values to generate
     * @return a stream of pseudorandom {@code long} values
     * @see Random#longs(long)
     */
    public static LongStreamEx of(Random random, long streamSize) {
        return seq(random.longs(streamSize));
    }

    /**
     * Returns an effectively unlimited stream of pseudorandom {@code long}
     * values, each conforming to the given origin (inclusive) and bound
     * (exclusive).
     *
     * @param random a {@link Random} object to produce the stream from
     * @param randomNumberOrigin the origin (inclusive) of each random value
     * @param randomNumberBound the bound (exclusive) of each random value
     * @return a stream of pseudorandom {@code long} values
     * @see Random#longs(long, long, long)
     */
    public static LongStreamEx of(Random random, long randomNumberOrigin, long randomNumberBound) {
        return seq(random.longs(randomNumberOrigin, randomNumberBound));
    }

    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code long} values, each conforming to the given origin
     * (inclusive) and bound (exclusive).
     *
     * @param random a {@link Random} object to produce the stream from
     * @param streamSize the number of values to generate
     * @param randomNumberOrigin the origin (inclusive) of each random value
     * @param randomNumberBound the bound (exclusive) of each random value
     * @return a stream of pseudorandom {@code long} values
     * @see Random#longs(long, long, long)
     */
    public static LongStreamEx of(Random random, long streamSize, long randomNumberOrigin, long randomNumberBound) {
        return seq(random.longs(streamSize, randomNumberOrigin, randomNumberBound));
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
     * @param seed the initial element
     * @param f a function to be applied to to the previous element to produce a
     *        new element
     * @return A new sequential {@code LongStream}
     * @see #iterate(long, LongPredicate, LongUnaryOperator)
     */
    public static LongStreamEx iterate(final long seed, final LongUnaryOperator f) {
        return iterate(seed, x -> true, f);
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} produced by iterative
     * application of a function to an initial element, conditioned on
     * satisfying the supplied predicate. The stream terminates as soon as the
     * predicate function returns false.
     *
     * <p>
     * {@code LongStreamEx.iterate} should produce the same sequence of elements
     * as produced by the corresponding for-loop:
     * 
     * <pre>{@code
     *     for (long index=seed; predicate.test(index); index = f.apply(index)) { 
     *         ... 
     *     }
     * }</pre>
     *
     * <p>
     * The resulting sequence may be empty if the predicate does not hold on the
     * seed value. Otherwise the first element will be the supplied seed value,
     * the next element (if present) will be the result of applying the function
     * f to the seed value, and so on iteratively until the predicate indicates
     * that the stream should terminate.
     *
     * @param seed the initial element
     * @param predicate a predicate to apply to elements to determine when the
     *        stream must terminate.
     * @param f a function to be applied to the previous element to produce a
     *        new element
     * @return a new sequential {@code LongStreamEx}
     * @see #iterate(long, LongUnaryOperator)
     * @since 0.6.0
     */
    public static LongStreamEx iterate(long seed, LongPredicate predicate, LongUnaryOperator f) {
        Objects.requireNonNull(f);
        Objects.requireNonNull(predicate);
        Spliterator.OfLong spliterator = new Spliterators.AbstractLongSpliterator(Long.MAX_VALUE, Spliterator.ORDERED
            | Spliterator.IMMUTABLE | Spliterator.NONNULL) {
            long prev;
            boolean started, finished;

            @Override
            public boolean tryAdvance(LongConsumer action) {
                Objects.requireNonNull(action);
                if (finished)
                    return false;
                long t;
                if (started)
                    t = f.applyAsLong(prev);
                else {
                    t = seed;
                    started = true;
                }
                if (!predicate.test(t)) {
                    finished = true;
                    return false;
                }
                action.accept(prev = t);
                return true;
            }

            @Override
            public void forEachRemaining(LongConsumer action) {
                Objects.requireNonNull(action);
                if (finished)
                    return;
                finished = true;
                long t = started ? f.applyAsLong(prev) : seed;
                while (predicate.test(t)) {
                    action.accept(t);
                    t = f.applyAsLong(t);
                }
            }
        };
        return of(spliterator);
    }

    /**
     * Returns an infinite sequential unordered stream where each element is
     * generated by the provided {@code LongSupplier}. This is suitable for
     * generating constant streams, streams of random elements, etc.
     *
     * @param s the {@code LongSupplier} for generated elements
     * @return a new infinite sequential unordered {@code LongStreamEx}
     * @see LongStream#generate(LongSupplier)
     */
    public static LongStreamEx generate(LongSupplier s) {
        return seq(LongStream.generate(s));
    }

    /**
     * Return an ordered stream produced by consecutive calls of the supplied
     * producer until it returns false.
     * 
     * <p>
     * The producer function may call the passed consumer any number of times
     * and return true if the producer should be called again or false
     * otherwise. It's guaranteed that the producer will not be called anymore,
     * once it returns false.
     * 
     * <p>
     * This method is particularly useful when producer changes the mutable
     * object which should be left in known state after the full stream
     * consumption. Note however that if a short-circuiting operation is used,
     * then the final state of the mutable object cannot be guaranteed.
     * 
     * @param producer a predicate which calls the passed consumer to emit
     *        stream element(s) and returns true if it producer should be
     *        applied again.
     * @return the new stream
     * @since 0.6.0
     */
    public static LongStreamEx produce(Predicate<LongConsumer> producer) {
        Box<LongEmitter> box = new Box<>();
        return (box.a = action -> producer.test(action) ? box.a : null).stream();
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} from 0 (inclusive) to
     * {@code Long.MAX_VALUE} (exclusive) by an incremental step of {@code 1}.
     *
     * @return a sequential {@code LongStreamEx} for the range of {@code long}
     *         elements
     * @see #range(long, long)
     * @since 0.5.5
     */
    public static LongStreamEx longs() {
        return seq(LongStream.range(0, Long.MAX_VALUE));
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} from 0 (inclusive) to
     * {@code endExclusive} (exclusive) by an incremental step of {@code 1}.
     *
     * @param endExclusive the exclusive upper bound
     * @return a sequential {@code LongStreamEx} for the range of {@code int}
     *         elements
     * @see #range(long, long)
     * @since 0.1.1
     */
    public static LongStreamEx range(long endExclusive) {
        return seq(LongStream.range(0, endExclusive));
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} from
     * {@code startInclusive} (inclusive) to {@code endExclusive} (exclusive) by
     * an incremental step of {@code 1}.
     *
     * @param startInclusive the (inclusive) initial value
     * @param endExclusive the exclusive upper bound
     * @return a sequential {@code LongStreamEx} for the range of {@code long}
     *         elements
     * @see LongStream#range(long, long)
     */
    public static LongStreamEx range(long startInclusive, long endExclusive) {
        return seq(LongStream.range(startInclusive, endExclusive));
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} from
     * {@code startInclusive} (inclusive) to {@code endExclusive} (exclusive) by
     * the specified incremental step. The negative step values are also
     * supported. In this case the {@code startInclusive} should be greater than
     * {@code endExclusive}.
     *
     * @param startInclusive the (inclusive) initial value
     * @param endExclusive the exclusive upper bound
     * @param step the non-zero value which designates the difference between
     *        the consecutive values of the resulting stream.
     * @return a sequential {@code LongStreamEx} for the range of {@code long}
     *         elements
     * @throws IllegalArgumentException if step is zero
     * @see LongStreamEx#range(long, long)
     * @since 0.4.0
     */
    public static LongStreamEx range(long startInclusive, long endExclusive, long step) {
        long endInclusive = endExclusive - Long.signum(step);
        if (endInclusive > endExclusive && step > 0 || endInclusive < endExclusive && step < 0)
            return empty();
        return rangeClosed(startInclusive, endInclusive, step);
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} from
     * {@code startInclusive} (inclusive) to {@code endInclusive} (inclusive) by
     * an incremental step of {@code 1}.
     *
     * @param startInclusive the (inclusive) initial value
     * @param endInclusive the inclusive upper bound
     * @return a sequential {@code LongStreamEx} for the range of {@code long}
     *         elements
     * @see LongStream#rangeClosed(long, long)
     */
    public static LongStreamEx rangeClosed(long startInclusive, long endInclusive) {
        return seq(LongStream.rangeClosed(startInclusive, endInclusive));
    }

    /**
     * Returns a sequential ordered {@code LongStreamEx} from
     * {@code startInclusive} (inclusive) to {@code endInclusive} (inclusive) by
     * the specified incremental step. The negative step values are also
     * supported. In this case the {@code startInclusive} should be greater than
     * {@code endInclusive}.
     * 
     * <p>
     * Note that depending on the step value the {@code endInclusive} bound may
     * still not be reached. For example
     * {@code LongStreamEx.rangeClosed(0, 5, 2)} will yield the stream of three
     * numbers: 0L, 2L and 4L.
     *
     * @param startInclusive the (inclusive) initial value
     * @param endInclusive the inclusive upper bound
     * @param step the non-zero value which designates the difference between
     *        the consecutive values of the resulting stream.
     * @return a sequential {@code LongStreamEx} for the range of {@code long}
     *         elements
     * @throws IllegalArgumentException if step is zero
     * @see LongStreamEx#rangeClosed(long, long)
     * @since 0.4.0
     */
    public static LongStreamEx rangeClosed(long startInclusive, long endInclusive, long step) {
        if (step == 0)
            throw new IllegalArgumentException("step = 0");
        if (step == 1)
            return seq(LongStream.rangeClosed(startInclusive, endInclusive));
        if (step == -1) {
            // Handled specially as number of elements can exceed
            // Long.MAX_VALUE
            long sum = endInclusive + startInclusive;
            return seq(LongStream.rangeClosed(endInclusive, startInclusive).map(x -> sum - x));
        }
        if ((endInclusive > startInclusive ^ step > 0) || endInclusive == startInclusive)
            return empty();
        long limit = (endInclusive - startInclusive) * Long.signum(step);
        limit = Long.divideUnsigned(limit, Math.abs(step));
        return seq(LongStream.rangeClosed(0, limit).map(x -> x * step + startInclusive));
    }

    /**
     * Returns a sequential unordered {@code LongStreamEx} of given length which
     * elements are equal to supplied value.
     * 
     * @param value the constant value
     * @param length the length of the stream
     * @return a new {@code LongStreamEx}
     * @since 0.1.2
     */
    public static LongStreamEx constant(long value, long length) {
        return of(new ConstSpliterator.OfLong(value, length, false));
    }

    /**
     * Returns a sequential {@code LongStreamEx} containing the results of
     * applying the given function to the corresponding pairs of values in given
     * two arrays.
     * 
     * @param first the first array
     * @param second the second array
     * @param mapper a non-interfering, stateless function to apply to each pair
     *        of the corresponding array elements.
     * @return a new {@code LongStreamEx}
     * @throws IllegalArgumentException if length of the arrays differs.
     * @since 0.2.1
     */
    public static LongStreamEx zip(long[] first, long[] second, LongBinaryOperator mapper) {
        return of(new RangeBasedSpliterator.ZipLong(0, checkLength(first.length, second.length), mapper, first, second));
    }

    /**
     * A helper interface to build a new stream by emitting elements and
     * creating new emitters in a chain.
     * 
     * <p>
     * Using this interface it's possible to create custom sources which cannot
     * be easily expressed using
     * {@link LongStreamEx#iterate(long, LongUnaryOperator)} or
     * {@link LongStreamEx#generate(LongSupplier)}. For example, the following
     * method generates a Collatz sequence starting from given number:
     * 
     * <pre>{@code
     * public static LongEmitter collatz(long start) {
     *    return action -> {
     *       action.accept(start);
     *       return start == 1 ? null : collatz(start % 2 == 0 ? start / 2 : start * 3 + 1);
     *    };
     * }}</pre>
     * 
     * <p>
     * Now you can use {@code collatz(17).stream()} to get the stream of Collatz
     * numbers.
     * 
     * @author Tagir Valeev
     *
     * @since 0.6.0
     */
    @FunctionalInterface
    public interface LongEmitter {
        /**
         * Calls the supplied consumer zero or more times to emit some elements,
         * then returns the next emitter which will emit more, or null if
         * nothing more to emit.
         * 
         * <p>
         * Normally one element is emitted during the {@code next()} method
         * call. However, it's not restricted: you may emit as many elements as
         * you want, though in some cases if many elements were emitted they
         * might be buffered consuming additional memory.
         * 
         * <p>
         * It's allowed not to emit anything (don't call the consumer). However
         * if you do this and return new emitter which also does not emit
         * anything, you will end up in endless loop.
         * 
         * @param action consumer to be called to emit elements
         * @return next emitter or null
         */
        LongEmitter next(LongConsumer action);

        /**
         * Returns the spliterator which covers all the elements emitted by this
         * emitter.
         * 
         * @return the new spliterator
         */
        default Spliterator.OfLong spliterator() {
            return new EmitterSpliterator.OfLong(this);
        }

        /**
         * Returns the stream which covers all the elements emitted by this
         * emitter.
         * 
         * @return the new stream
         */
        default LongStreamEx stream() {
            return of(spliterator());
        }
    }
}
