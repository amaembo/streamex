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

import java.util.HashMap;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static one.util.streamex.StreamExInternals.*;

/**
 * A {@link Collector} specialized to work with primitive {@code long}.
 * 
 * @author Tagir Valeev
 *
 * @param <A> the mutable accumulation type of the reduction operation (often
 *        hidden as an implementation detail)
 * @param <R> the result type of the reduction operation
 * @see LongStreamEx#collect(LongCollector)
 * @since 0.3.0
 */
public interface LongCollector<A, R> extends MergingCollector<Long, A, R> {
    /**
     * A function that folds a value into a mutable result container.
     *
     * @return a function which folds a value into a mutable result container
     */
    ObjLongConsumer<A> longAccumulator();

    /**
     * A function that folds a value into a mutable result container.
     * 
     * The default implementation calls {@link #longAccumulator()} on unboxed
     * value.
     *
     * @return a function which folds a value into a mutable result container
     */
    @Override
    default BiConsumer<A, Long> accumulator() {
        return longAccumulator()::accept;
    }

    /**
     * Adapts this collector to perform an additional finishing transformation.
     *
     * @param <RR> result type of the resulting collector
     * @param finisher a function to be applied to the final result of this
     *        collector
     * @return a collector which performs the action of this collector, followed
     *         by an additional finishing step
     * @since 0.3.7
     */
    default <RR> LongCollector<A, RR> andThen(Function<R, RR> finisher) {
        return of(supplier(), longAccumulator(), merger(), finisher().andThen(finisher));
    }

    /**
     * Returns a new {@code LongCollector} described by the given
     * {@code supplier}, {@code accumulator}, and {@code merger} functions. The
     * resulting {@code LongCollector} has the
     * {@code Collector.Characteristics.IDENTITY_FINISH} characteristic.
     *
     * @param supplier The supplier function for the new collector
     * @param longAccumulator The longAccumulator function for the new collector
     * @param merger The merger function for the new collector
     * @param <R> The type of intermediate accumulation result, and final
     *        result, for the new collector
     * @return the new {@code LongCollector}
     */
    static <R> LongCollector<R, R> of(Supplier<R> supplier, ObjLongConsumer<R> longAccumulator, BiConsumer<R, R> merger) {
        return new LongCollectorImpl<>(supplier, longAccumulator, merger, Function.identity(), ID_CHARACTERISTICS);
    }

    /**
     * Adapts a {@code Collector} which accepts elements of type {@code Long} to
     * a {@code LongCollector}.
     * 
     * @param <A> The intermediate accumulation type of the collector
     * @param <R> The final result type of the collector
     * @param collector a {@code Collector} to adapt
     * @return a {@code LongCollector} which behaves in the same way as input
     *         collector.
     */
    static <A, R> LongCollector<?, R> of(Collector<Long, A, R> collector) {
        if (collector instanceof LongCollector) {
            return (LongCollector<A, R>) collector;
        }
        return mappingToObj(Long::valueOf, collector);
    }

    /**
     * Returns a new {@code LongCollector} described by the given
     * {@code supplier}, {@code accumulator}, {@code merger}, and
     * {@code finisher} functions.
     *
     * @param supplier The supplier function for the new collector
     * @param longAccumulator The longAccumulator function for the new collector
     * @param merger The merger function for the new collector
     * @param finisher The finisher function for the new collector
     * @param <A> The intermediate accumulation type of the new collector
     * @param <R> The final result type of the new collector
     * @return the new {@code LongCollector}
     */
    static <A, R> LongCollector<A, R> of(Supplier<A> supplier, ObjLongConsumer<A> longAccumulator,
            BiConsumer<A, A> merger, Function<A, R> finisher) {
        return new LongCollectorImpl<>(supplier, longAccumulator, merger, finisher, NO_CHARACTERISTICS);
    }

    /**
     * Returns a {@code LongCollector} that converts the input numbers to
     * strings and concatenates them, separated by the specified delimiter, with
     * the specified prefix and suffix, in encounter order.
     *
     * @param delimiter the delimiter to be used between each element
     * @param prefix the sequence of characters to be used at the beginning of
     *        the joined result
     * @param suffix the sequence of characters to be used at the end of the
     *        joined result
     * @return A {@code LongCollector} which concatenates the input numbers,
     *         separated by the specified delimiter, in encounter order
     */
    static LongCollector<?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return PartialCollector.joining(delimiter, prefix, suffix, true).asLong(
            StreamExInternals.joinAccumulatorLong(delimiter));
    }

    /**
     * Returns a {@code LongCollector} that converts the input numbers to
     * strings and concatenates them, separated by the specified delimiter, in
     * encounter order.
     *
     * @param delimiter the delimiter to be used between each element
     * @return A {@code LongCollector} which concatenates the input numbers,
     *         separated by the specified delimiter, in encounter order
     */
    static LongCollector<?, String> joining(CharSequence delimiter) {
        return PartialCollector.joining(delimiter, null, null, false).asLong(
            StreamExInternals.joinAccumulatorLong(delimiter));
    }

    /**
     * Returns a {@code LongCollector} that counts the number of input elements
     * and returns the result as {@code Long}. If no elements are present, the
     * result is 0.
     *
     * @return a {@code LongCollector} that counts the input elements
     */
    static LongCollector<?, Long> counting() {
        return PartialCollector.longSum().asLong((box, i) -> box[0]++);
    }

    /**
     * Returns an {@code LongCollector} that counts the number of input elements
     * and returns the result as {@code Integer}. If no elements are present,
     * the result is 0.
     *
     * @return an {@code LongCollector} that counts the input elements
     */
    static LongCollector<?, Integer> countingInt() {
        return PartialCollector.intSum().asLong((box, i) -> box[0]++);
    }

    /**
     * Returns a {@code LongCollector} that produces the sum of the input
     * elements. If no elements are present, the result is 0.
     *
     * @return a {@code LongCollector} that produces the sum of the input
     *         elements
     */
    static LongCollector<?, Long> summing() {
        return PartialCollector.longSum().asLong((box, i) -> box[0] += i);
    }

    /**
     * Returns a {@code LongCollector} that produces the arithmetic mean of the
     * input elements or an empty optional if no elements are collected.
     *
     * <p>
     * Note that unlike {@link LongStream#average()},
     * {@link Collectors#averagingLong(java.util.function.ToLongFunction)} and
     * {@link LongSummaryStatistics#getAverage()} this collector does not
     * overflow if an intermediate sum exceeds {@code Long.MAX_VALUE}.
     *
     * @return a {@code LongCollector} that produces the arithmetic mean of the
     *         input elements
     * @since 0.3.7
     */
    static LongCollector<?, OptionalDouble> averaging() {
        return of(AverageLong::new, AverageLong::accept, AverageLong::combine, AverageLong::result);
    }

    /**
     * Returns a {@code LongCollector} that produces the minimal element,
     * described as an {@link OptionalLong}. If no elements are present, the
     * result is an empty {@code OptionalLong}.
     *
     * @return a {@code LongCollector} that produces the minimal element.
     */
    static LongCollector<?, OptionalLong> min() {
        return reducing(Long::min);
    }

    /**
     * Returns a {@code LongCollector} that produces the maximal element,
     * described as an {@link OptionalLong}. If no elements are present, the
     * result is an empty {@code OptionalLong}.
     *
     * @return a {@code LongCollector} that produces the maximal element.
     */
    static LongCollector<?, OptionalLong> max() {
        return reducing(Long::max);
    }

    /**
     * Adapts a {@code LongCollector} to another one by applying a mapping
     * function to each input element before accumulation.
     *
     * @param <A> intermediate accumulation type of the downstream collector
     * @param <R> result type of collector
     * @param mapper a function to be applied to the input elements
     * @param downstream a collector which will accept mapped values
     * @return a collector which applies the mapping function to the input
     *         elements and provides the mapped results to the downstream
     *         collector
     */
    static <A, R> LongCollector<?, R> mapping(LongUnaryOperator mapper, LongCollector<A, R> downstream) {
        ObjLongConsumer<A> downstreamAccumulator = downstream.longAccumulator();
        return new LongCollectorImpl<>(downstream.supplier(), (r, t) -> downstreamAccumulator.accept(r, mapper
                .applyAsLong(t)), downstream.merger(), downstream.finisher(), downstream.characteristics());
    }

    /**
     * Adapts a {@link Collector} accepting elements of type {@code U} to a
     * {@code LongCollector} by applying a mapping function to each input
     * element before accumulation.
     *
     * @param <U> type of elements accepted by downstream collector
     * @param <A> intermediate accumulation type of the downstream collector
     * @param <R> result type of collector
     * @param mapper a function to be applied to the input elements
     * @param downstream a collector which will accept mapped values
     * @return a collector which applies the mapping function to the input
     *         elements and provides the mapped results to the downstream
     *         collector
     */
    static <U, A, R> LongCollector<?, R> mappingToObj(LongFunction<U> mapper, Collector<U, A, R> downstream) {
        BiConsumer<A, U> accumulator = downstream.accumulator();
        if (downstream instanceof MergingCollector) {
            return new LongCollectorImpl<>(downstream.supplier(), (acc, i) -> accumulator.accept(acc, mapper.apply(i)),
                    ((MergingCollector<U, A, R>) downstream).merger(), downstream.finisher(), downstream
                            .characteristics());
        }
        return Box.partialCollector(downstream).asLong((box, i) -> accumulator.accept(box.a, mapper.apply(i)));
    }

    /**
     * Returns a {@code LongCollector} which performs a reduction of its input
     * numbers under a specified {@link LongBinaryOperator}. The result is
     * described as an {@link OptionalLong}.
     *
     * @param op a {@code LongBinaryOperator} used to reduce the input numbers
     * @return a {@code LongCollector} which implements the reduction operation.
     */
    static LongCollector<?, OptionalLong> reducing(LongBinaryOperator op) {
        return of(PrimitiveBox::new, (box, l) -> {
            if (!box.b) {
                box.b = true;
                box.l = l;
            } else {
                box.l = op.applyAsLong(box.l, l);
            }
        }, (box1, box2) -> {
            if (box2.b) {
                if (!box1.b) {
                    box1.from(box2);
                } else {
                    box1.l = op.applyAsLong(box1.l, box2.l);
                }
            }
        }, PrimitiveBox::asLong);
    }

    /**
     * Returns a {@code LongCollector} which performs a reduction of its input
     * numbers under a specified {@code IntBinaryOperator} using the provided
     * identity.
     *
     * @param identity the identity value for the reduction (also, the value
     *        that is returned when there are no input elements)
     * @param op a {@code LongBinaryOperator} used to reduce the input numbers
     * @return a {@code LongCollector} which implements the reduction operation
     */
    static LongCollector<?, Long> reducing(long identity, LongBinaryOperator op) {
        return of(() -> new long[] { identity }, (box, i) -> box[0] = op.applyAsLong(box[0], i),
            (box1, box2) -> box1[0] = op.applyAsLong(box1[0], box2[0]), UNBOX_LONG);
    }

    /**
     * Returns a {@code LongCollector} which returns summary statistics for the
     * input elements.
     *
     * @return a {@code LongCollector} implementing the summary-statistics
     *         reduction
     */
    static LongCollector<?, LongSummaryStatistics> summarizing() {
        return of(LongSummaryStatistics::new, LongSummaryStatistics::accept, LongSummaryStatistics::combine);
    }

    /**
     * Returns a {@code LongCollector} which partitions the input elements
     * according to a {@code LongPredicate}, and organizes them into a
     * {@code Map<Boolean, long[]>}.
     *
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} returned.
     *
     * @param predicate a predicate used for classifying input elements
     * @return a {@code LongCollector} implementing the partitioning operation
     */
    static LongCollector<?, Map<Boolean, long[]>> partitioningBy(LongPredicate predicate) {
        return partitioningBy(predicate, toArray());
    }

    /**
     * Returns a {@code LongCollector} which partitions the input numbers
     * according to a {@code LongPredicate}, reduces the values in each
     * partition according to another {@code IntCollector}, and organizes them
     * into a {@code Map<Boolean, D>} whose values are the result of the
     * downstream reduction.
     *
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} returned.
     *
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param predicate a predicate used for classifying input elements
     * @param downstream a {@code LongCollector} implementing the downstream
     *        reduction
     * @return a {@code LongCollector} implementing the cascaded partitioning
     *         operation
     */
    static <A, D> LongCollector<?, Map<Boolean, D>> partitioningBy(LongPredicate predicate,
            LongCollector<A, D> downstream) {
        ObjLongConsumer<A> downstreamAccumulator = downstream.longAccumulator();
        ObjLongConsumer<BooleanMap<A>> accumulator = (result, t) -> downstreamAccumulator.accept(
            predicate.test(t) ? result.trueValue : result.falseValue, t);
        return BooleanMap.partialCollector(downstream).asLong(accumulator);
    }

    /**
     * Returns a {@code LongCollector} implementing a "group by" operation on
     * input numbers, grouping them according to a classification function, and
     * returning the results in a {@code Map}.
     *
     * <p>
     * The classification function maps elements to some key type {@code K}. The
     * collector produces a {@code Map<K, long[]>} whose keys are the values
     * resulting from applying the classification function to the input numbers,
     * and whose corresponding values are arrays containing the input numbers
     * which map to the associated key under the classification function.
     *
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} objects returned.
     *
     * @param <K> the type of the keys
     * @param classifier the classifier function mapping input elements to keys
     * @return a {@code LongCollector} implementing the group-by operation
     */
    static <K> LongCollector<?, Map<K, long[]>> groupingBy(LongFunction<? extends K> classifier) {
        return groupingBy(classifier, toArray());
    }

    /**
     * Returns a {@code LongCollector} implementing a cascaded "group by"
     * operation on input numbers, grouping them according to a classification
     * function, and then performing a reduction operation on the values
     * associated with a given key using the specified downstream
     * {@code IntCollector}.
     *
     * <p>
     * The classification function maps elements to some key type {@code K}. The
     * downstream collector produces a result of type {@code D}. The resulting
     * collector produces a {@code Map<K, D>}.
     *
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} returned.
     *
     * @param <K> the type of the keys
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param classifier a classifier function mapping input elements to keys
     * @param downstream a {@code LongCollector} implementing the downstream
     *        reduction
     * @return a {@code LongCollector} implementing the cascaded group-by
     *         operation
     */
    static <K, D, A> LongCollector<?, Map<K, D>> groupingBy(LongFunction<? extends K> classifier,
            LongCollector<A, D> downstream) {
        return groupingBy(classifier, HashMap::new, downstream);
    }

    /**
     * Returns a {@code LongCollector} implementing a cascaded "group by"
     * operation on input numbers, grouping them according to a classification
     * function, and then performing a reduction operation on the values
     * associated with a given key using the specified downstream
     * {@code IntCollector}. The {@code Map} produced by the Collector is
     * created with the supplied factory function.
     *
     * <p>
     * The classification function maps elements to some key type {@code K}. The
     * downstream collector produces a result of type {@code D}. The resulting
     * collector produces a {@code Map<K, D>}.
     *
     * @param <K> the type of the keys
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param <M> the type of the resulting {@code Map}
     * @param classifier a classifier function mapping input elements to keys
     * @param downstream a {@code LongCollector} implementing the downstream
     *        reduction
     * @param mapFactory a function which, when called, produces a new empty
     *        {@code Map} of the desired type
     * @return a {@code LongCollector} implementing the cascaded group-by
     *         operation
     */
    static <K, D, A, M extends Map<K, D>> LongCollector<?, M> groupingBy(LongFunction<? extends K> classifier,
            Supplier<M> mapFactory, LongCollector<A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        Function<K, A> supplier = k -> downstreamSupplier.get();
        ObjLongConsumer<A> downstreamAccumulator = downstream.longAccumulator();
        ObjLongConsumer<Map<K, A>> accumulator = (m, t) -> {
            K key = Objects.requireNonNull(classifier.apply(t));
            A container = m.computeIfAbsent(key, supplier);
            downstreamAccumulator.accept(container, t);
        };
        return PartialCollector.grouping(mapFactory, downstream).asLong(accumulator);
    }

    /**
     * Returns a {@code LongCollector} that produces the array of the input
     * elements. If no elements are present, the result is an empty array.
     *
     * @return a {@code LongCollector} that produces the array of the input
     *         elements
     */
    static LongCollector<?, long[]> toArray() {
        return of(LongBuffer::new, LongBuffer::add, LongBuffer::addAll, LongBuffer::toArray);
    }

    /**
     * Returns a {@code LongCollector} which produces a boolean array containing
     * the results of applying the given predicate to the input elements, in
     * encounter order.
     * 
     * @param predicate a non-interfering, stateless predicate to apply to each
     *        input element. The result values of this predicate are collected
     *        to the resulting boolean array.
     * @return a {@code LongCollector} which collects the results of the
     *         predicate function to the boolean array, in encounter order.
     * @since 0.3.8
     */
    static LongCollector<?, boolean[]> toBooleanArray(LongPredicate predicate) {
        return PartialCollector.booleanArray().asLong((box, t) -> {
            if (predicate.test(t))
                box.a.set(box.b);
            box.b = StrictMath.addExact(box.b, 1);
        });
    }
}
