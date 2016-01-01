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

import java.util.BitSet;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static one.util.streamex.StreamExInternals.*;

/**
 * A {@link Collector} specialized to work with primitive {@code int}.
 * 
 * @author Tagir Valeev
 *
 * @param <A> the mutable accumulation type of the reduction operation (often
 *        hidden as an implementation detail)
 * @param <R> the result type of the reduction operation
 * @see IntStreamEx#collect(IntCollector)
 * @since 0.3.0
 */
public interface IntCollector<A, R> extends MergingCollector<Integer, A, R> {
    /**
     * A function that folds a value into a mutable result container.
     *
     * @return a function which folds a value into a mutable result container
     */
    ObjIntConsumer<A> intAccumulator();

    /**
     * A function that folds a value into a mutable result container.
     * 
     * The default implementation calls {@link #intAccumulator()} on unboxed
     * value.
     *
     * @return a function which folds a value into a mutable result container
     */
    @Override
    default BiConsumer<A, Integer> accumulator() {
        return intAccumulator()::accept;
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
    default <RR> IntCollector<A, RR> andThen(Function<R, RR> finisher) {
        return of(supplier(), intAccumulator(), merger(), finisher().andThen(finisher));
    }

    /**
     * Returns a new {@code IntCollector} described by the given
     * {@code supplier}, {@code accumulator}, and {@code merger} functions. The
     * resulting {@code IntCollector} has the
     * {@code Collector.Characteristics.IDENTITY_FINISH} characteristic.
     *
     * @param supplier The supplier function for the new collector
     * @param intAccumulator The intAccumulator function for the new collector
     * @param merger The merger function for the new collector
     * @param <R> The type of intermediate accumulation result, and final
     *        result, for the new collector
     * @return the new {@code IntCollector}
     */
    static <R> IntCollector<R, R> of(Supplier<R> supplier, ObjIntConsumer<R> intAccumulator, BiConsumer<R, R> merger) {
        return new IntCollectorImpl<>(supplier, intAccumulator, merger, Function.identity(), ID_CHARACTERISTICS);
    }

    /**
     * Adapts a {@code Collector} which accepts elements of type {@code Integer}
     * to an {@code IntCollector}.
     * 
     * @param <A> The intermediate accumulation type of the collector
     * @param <R> The final result type of the collector
     * @param collector a {@code Collector} to adapt
     * @return an {@code IntCollector} which behaves in the same way as input
     *         collector.
     */
    static <A, R> IntCollector<?, R> of(Collector<Integer, A, R> collector) {
        if (collector instanceof IntCollector) {
            return (IntCollector<A, R>) collector;
        }
        return mappingToObj(Integer::valueOf, collector);
    }

    /**
     * Returns a new {@code IntCollector} described by the given
     * {@code supplier}, {@code accumulator}, {@code merger}, and
     * {@code finisher} functions.
     *
     * @param supplier The supplier function for the new collector
     * @param intAccumulator The intAccumulator function for the new collector
     * @param merger The merger function for the new collector
     * @param finisher The finisher function for the new collector
     * @param <A> The intermediate accumulation type of the new collector
     * @param <R> The final result type of the new collector
     * @return the new {@code IntCollector}
     */
    static <A, R> IntCollector<A, R> of(Supplier<A> supplier, ObjIntConsumer<A> intAccumulator,
            BiConsumer<A, A> merger, Function<A, R> finisher) {
        return new IntCollectorImpl<>(supplier, intAccumulator, merger, finisher, NO_CHARACTERISTICS);
    }

    /**
     * Returns an {@code IntCollector} that converts the input numbers to
     * strings and concatenates them, separated by the specified delimiter, with
     * the specified prefix and suffix, in encounter order.
     *
     * @param delimiter the delimiter to be used between each element
     * @param prefix the sequence of characters to be used at the beginning of
     *        the joined result
     * @param suffix the sequence of characters to be used at the end of the
     *        joined result
     * @return An {@code IntCollector} which concatenates the input numbers,
     *         separated by the specified delimiter, in encounter order
     */
    static IntCollector<?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return PartialCollector.joining(delimiter, prefix, suffix, true).asInt(
            StreamExInternals.joinAccumulatorInt(delimiter));
    }

    /**
     * Returns an {@code IntCollector} that converts the input numbers to
     * strings and concatenates them, separated by the specified delimiter, in
     * encounter order.
     *
     * @param delimiter the delimiter to be used between each element
     * @return An {@code IntCollector} which concatenates the input numbers,
     *         separated by the specified delimiter, in encounter order
     */
    static IntCollector<?, String> joining(CharSequence delimiter) {
        return PartialCollector.joining(delimiter, null, null, false).asInt(
            StreamExInternals.joinAccumulatorInt(delimiter));
    }

    /**
     * Returns an {@code IntCollector} that counts the number of input elements
     * and returns the result as {@code Long}. If no elements are present, the
     * result is 0.
     *
     * @return an {@code IntCollector} that counts the input elements
     */
    static IntCollector<?, Long> counting() {
        return PartialCollector.longSum().asInt((box, i) -> box[0]++);
    }

    /**
     * Returns an {@code IntCollector} that counts the number of input elements
     * and returns the result as {@code Integer}. If no elements are present,
     * the result is 0.
     *
     * @return an {@code IntCollector} that counts the input elements
     */
    static IntCollector<?, Integer> countingInt() {
        return PartialCollector.intSum().asInt((box, i) -> box[0]++);
    }

    /**
     * Returns an {@code IntCollector} that produces the sum of the input
     * elements. If no elements are present, the result is 0.
     *
     * @return an {@code IntCollector} that produces the sum of the input
     *         elements
     */
    static IntCollector<?, Integer> summing() {
        return PartialCollector.intSum().asInt((box, i) -> box[0] += i);
    }

    /**
     * Returns an {@code IntCollector} that produces the arithmetic mean of the
     * input elements or an empty optional if no elements are collected.
     * 
     * <p>
     * Note that unlike {@link IntStream#average()},
     * {@link Collectors#averagingInt(java.util.function.ToIntFunction)} and
     * {@link IntSummaryStatistics#getAverage()} this collector does not
     * overflow if an intermediate sum exceeds {@code Long.MAX_VALUE}.
     *
     * @return an {@code IntCollector} that produces the arithmetic mean of the
     *         input elements
     * @since 0.3.7
     */
    static IntCollector<?, OptionalDouble> averaging() {
        return of(AverageLong::new, AverageLong::accept, AverageLong::combine, AverageLong::result);
    }

    /**
     * Returns an {@code IntCollector} that produces the minimal element,
     * described as an {@link OptionalInt}. If no elements are present, the
     * result is an empty {@code OptionalInt}.
     *
     * @return an {@code IntCollector} that produces the minimal element.
     */
    static IntCollector<?, OptionalInt> min() {
        return reducing(Integer::min);
    }

    /**
     * Returns an {@code IntCollector} that produces the maximal element,
     * described as an {@link OptionalInt}. If no elements are present, the
     * result is an empty {@code OptionalInt}.
     *
     * @return an {@code IntCollector} that produces the maximal element.
     */
    static IntCollector<?, OptionalInt> max() {
        return reducing(Integer::max);
    }

    /**
     * Adapts an {@code IntCollector} to another one by applying a mapping
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
    static <A, R> IntCollector<?, R> mapping(IntUnaryOperator mapper, IntCollector<A, R> downstream) {
        ObjIntConsumer<A> downstreamAccumulator = downstream.intAccumulator();
        return new IntCollectorImpl<>(downstream.supplier(), (r, t) -> downstreamAccumulator.accept(r, mapper
                .applyAsInt(t)), downstream.merger(), downstream.finisher(), downstream.characteristics());
    }

    /**
     * Adapts a {@link Collector} accepting elements of type {@code U} to an
     * {@code IntCollector} by applying a mapping function to each input element
     * before accumulation.
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
    static <U, A, R> IntCollector<?, R> mappingToObj(IntFunction<U> mapper, Collector<U, A, R> downstream) {
        BiConsumer<A, U> accumulator = downstream.accumulator();
        if (downstream instanceof MergingCollector) {
            return new IntCollectorImpl<>(downstream.supplier(), (acc, i) -> accumulator.accept(acc, mapper.apply(i)),
                    ((MergingCollector<U, A, R>) downstream).merger(), downstream.finisher(), downstream
                            .characteristics());
        }
        return Box.partialCollector(downstream).asInt((box, i) -> accumulator.accept(box.a, mapper.apply(i)));
    }

    /**
     * Returns an {@code IntCollector} which performs a reduction of its input
     * numbers under a specified {@link IntBinaryOperator}. The result is
     * described as an {@link OptionalInt}.
     *
     * @param op an {@code IntBinaryOperator} used to reduce the input numbers
     * @return an {@code IntCollector} which implements the reduction operation.
     */
    static IntCollector<?, OptionalInt> reducing(IntBinaryOperator op) {
        return of(PrimitiveBox::new, (box, i) -> {
            if (!box.b) {
                box.b = true;
                box.i = i;
            } else {
                box.i = op.applyAsInt(box.i, i);
            }
        }, (box1, box2) -> {
            if (box2.b) {
                if (!box1.b) {
                    box1.from(box2);
                } else {
                    box1.i = op.applyAsInt(box1.i, box2.i);
                }
            }
        }, PrimitiveBox::asInt);
    }

    /**
     * Returns an {@code IntCollector} which performs a reduction of its input
     * numbers under a specified {@code IntBinaryOperator} using the provided
     * identity.
     *
     * @param identity the identity value for the reduction (also, the value
     *        that is returned when there are no input elements)
     * @param op an {@code IntBinaryOperator} used to reduce the input numbers
     * @return an {@code IntCollector} which implements the reduction operation
     */
    static IntCollector<?, Integer> reducing(int identity, IntBinaryOperator op) {
        return of(() -> new int[] { identity }, (box, i) -> box[0] = op.applyAsInt(box[0], i),
            (box1, box2) -> box1[0] = op.applyAsInt(box1[0], box2[0]), UNBOX_INT);
    }

    /**
     * Returns an {@code IntCollector} which returns summary statistics for the
     * input elements.
     *
     * @return an {@code IntCollector} implementing the summary-statistics
     *         reduction
     */
    static IntCollector<?, IntSummaryStatistics> summarizing() {
        return of(IntSummaryStatistics::new, IntSummaryStatistics::accept, IntSummaryStatistics::combine);
    }

    /**
     * Returns an {@code IntCollector} which partitions the input elements
     * according to an {@code IntPredicate}, and organizes them into a
     * {@code Map<Boolean, int[]>}.
     *
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} returned.
     *
     * @param predicate a predicate used for classifying input elements
     * @return an {@code IntCollector} implementing the partitioning operation
     */
    static IntCollector<?, Map<Boolean, int[]>> partitioningBy(IntPredicate predicate) {
        return partitioningBy(predicate, toArray());
    }

    /**
     * Returns an {@code IntCollector} which partitions the input numbers
     * according to an {@code IntPredicate}, reduces the values in each
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
     * @param downstream an {@code IntCollector} implementing the downstream
     *        reduction
     * @return an {@code IntCollector} implementing the cascaded partitioning
     *         operation
     */
    static <A, D> IntCollector<?, Map<Boolean, D>> partitioningBy(IntPredicate predicate, IntCollector<A, D> downstream) {
        ObjIntConsumer<A> downstreamAccumulator = downstream.intAccumulator();
        ObjIntConsumer<BooleanMap<A>> accumulator = (result, t) -> downstreamAccumulator.accept(
            predicate.test(t) ? result.trueValue : result.falseValue, t);
        return BooleanMap.partialCollector(downstream).asInt(accumulator);
    }

    /**
     * Returns an {@code IntCollector} implementing a "group by" operation on
     * input numbers, grouping them according to a classification function, and
     * returning the results in a {@code Map}.
     *
     * <p>
     * The classification function maps elements to some key type {@code K}. The
     * collector produces a {@code Map<K, int[]>} whose keys are the values
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
     * @return an {@code IntCollector} implementing the group-by operation
     */
    static <K> IntCollector<?, Map<K, int[]>> groupingBy(IntFunction<? extends K> classifier) {
        return groupingBy(classifier, toArray());
    }

    /**
     * Returns an {@code IntCollector} implementing a cascaded "group by"
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
     * @param downstream an {@code IntCollector} implementing the downstream
     *        reduction
     * @return an {@code IntCollector} implementing the cascaded group-by
     *         operation
     */
    static <K, D, A> IntCollector<?, Map<K, D>> groupingBy(IntFunction<? extends K> classifier,
            IntCollector<A, D> downstream) {
        return groupingBy(classifier, HashMap::new, downstream);
    }

    /**
     * Returns an {@code IntCollector} implementing a cascaded "group by"
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
     * @param downstream an {@code IntCollector} implementing the downstream
     *        reduction
     * @param mapFactory a function which, when called, produces a new empty
     *        {@code Map} of the desired type
     * @return an {@code IntCollector} implementing the cascaded group-by
     *         operation
     */
    static <K, D, A, M extends Map<K, D>> IntCollector<?, M> groupingBy(IntFunction<? extends K> classifier,
            Supplier<M> mapFactory, IntCollector<A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        Function<K, A> supplier = k -> downstreamSupplier.get();
        ObjIntConsumer<A> downstreamAccumulator = downstream.intAccumulator();
        ObjIntConsumer<Map<K, A>> accumulator = (m, t) -> {
            K key = Objects.requireNonNull(classifier.apply(t));
            A container = m.computeIfAbsent(key, supplier);
            downstreamAccumulator.accept(container, t);
        };
        return PartialCollector.grouping(mapFactory, downstream).asInt(accumulator);
    }

    /**
     * Returns an {@code IntCollector} that produces the {@link BitSet} of the
     * input elements.
     *
     * @return an {@code IntCollector} that produces the {@link BitSet} of the
     *         input elements
     */
    static IntCollector<?, BitSet> toBitSet() {
        return of(BitSet::new, BitSet::set, BitSet::or);
    }

    /**
     * Returns an {@code IntCollector} that produces the array of the input
     * elements. If no elements are present, the result is an empty array.
     *
     * @return an {@code IntCollector} that produces the array of the input
     *         elements
     */
    static IntCollector<?, int[]> toArray() {
        return of(IntBuffer::new, IntBuffer::add, IntBuffer::addAll, IntBuffer::toArray);
    }

    /**
     * Returns an {@code IntCollector} that produces the {@code byte[]} array of
     * the input elements converting them via {@code (byte)} casting. If no
     * elements are present, the result is an empty array.
     *
     * @return an {@code IntCollector} that produces the {@code byte[]} array of
     *         the input elements
     */
    static IntCollector<?, byte[]> toByteArray() {
        return of(ByteBuffer::new, ByteBuffer::add, ByteBuffer::addAll, ByteBuffer::toArray);
    }

    /**
     * Returns an {@code IntCollector} that produces the {@code char[]} array of
     * the input elements converting them via {@code (char)} casting. If no
     * elements are present, the result is an empty array.
     *
     * @return an {@code IntCollector} that produces the {@code char[]} array of
     *         the input elements
     */
    static IntCollector<?, char[]> toCharArray() {
        return of(CharBuffer::new, CharBuffer::add, CharBuffer::addAll, CharBuffer::toArray);
    }

    /**
     * Returns an {@code IntCollector} that produces the {@code short[]} array
     * of the input elements converting them via {@code (short)} casting. If no
     * elements are present, the result is an empty array.
     *
     * @return an {@code IntCollector} that produces the {@code short[]} array
     *         of the input elements
     */
    static IntCollector<?, short[]> toShortArray() {
        return of(ShortBuffer::new, ShortBuffer::add, ShortBuffer::addAll, ShortBuffer::toArray);
    }

    /**
     * Returns an {@code IntCollector} which produces a boolean array containing
     * the results of applying the given predicate to the input elements, in
     * encounter order.
     * 
     * @param predicate a non-interfering, stateless predicate to apply to each
     *        input element. The result values of this predicate are collected
     *        to the resulting boolean array.
     * @return an {@code IntCollector} which collects the results of the
     *         predicate function to the boolean array, in encounter order.
     * @since 0.3.8
     */
    static IntCollector<?, boolean[]> toBooleanArray(IntPredicate predicate) {
        return PartialCollector.booleanArray().asInt((box, t) -> {
            if (predicate.test(t))
                box.a.set(box.b);
            box.b = StrictMath.addExact(box.b, 1);
        });
    }
}
