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

import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static one.util.streamex.StreamExInternals.*;

/**
 * A {@link Collector} specialized to work with primitive {@code double}.
 * 
 * @author Tagir Valeev
 *
 * @param <A> the mutable accumulation type of the reduction operation (often
 *        hidden as an implementation detail)
 * @param <R> the result type of the reduction operation
 * @see DoubleStreamEx#collect(DoubleCollector)
 * @since 0.3.0
 */
public interface DoubleCollector<A, R> extends MergingCollector<Double, A, R> {
    /**
     * A function that folds a value into a mutable result container.
     *
     * @return a function which folds a value into a mutable result container
     */
    ObjDoubleConsumer<A> doubleAccumulator();

    /**
     * A function that folds a value into a mutable result container.
     * 
     * The default implementation calls {@link #doubleAccumulator()} on unboxed
     * value.
     *
     * @return a function which folds a value into a mutable result container
     */
    @Override
    default BiConsumer<A, Double> accumulator() {
        return doubleAccumulator()::accept;
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
    default <RR> DoubleCollector<A, RR> andThen(Function<R, RR> finisher) {
        return of(supplier(), doubleAccumulator(), merger(), finisher().andThen(finisher));
    }

    /**
     * Returns a new {@code DoubleCollector} described by the given
     * {@code supplier}, {@code accumulator}, and {@code merger} functions. The
     * resulting {@code DoubleCollector} has the
     * {@code Collector.Characteristics.IDENTITY_FINISH} characteristic.
     *
     * @param supplier The supplier function for the new collector
     * @param doubleAccumulator The doubleAccumulator function for the new
     *        collector
     * @param merger The merger function for the new collector
     * @param <R> The type of intermediate accumulation result, and final
     *        result, for the new collector
     * @return the new {@code DoubleCollector}
     */
    static <R> DoubleCollector<R, R> of(Supplier<R> supplier, ObjDoubleConsumer<R> doubleAccumulator,
            BiConsumer<R, R> merger) {
        return new DoubleCollectorImpl<>(supplier, doubleAccumulator, merger, Function.identity(), ID_CHARACTERISTICS);
    }

    /**
     * Adapts a {@code Collector} which accepts elements of type {@code Double}
     * to a {@code DoubleCollector}.
     * 
     * @param <A> The intermediate accumulation type of the collector
     * @param <R> The final result type of the collector
     * @param collector a {@code Collector} to adapt
     * @return a {@code DoubleCollector} which behaves in the same way as input
     *         collector.
     */
    static <A, R> DoubleCollector<?, R> of(Collector<Double, A, R> collector) {
        if (collector instanceof DoubleCollector) {
            return (DoubleCollector<A, R>) collector;
        }
        return mappingToObj(Double::valueOf, collector);
    }

    /**
     * Returns a new {@code DoubleCollector} described by the given
     * {@code supplier}, {@code accumulator}, {@code merger}, and
     * {@code finisher} functions.
     *
     * @param supplier The supplier function for the new collector
     * @param doubleAccumulator The doubleAccumulator function for the new
     *        collector
     * @param merger The merger function for the new collector
     * @param finisher The finisher function for the new collector
     * @param <A> The intermediate accumulation type of the new collector
     * @param <R> The final result type of the new collector
     * @return the new {@code DoubleCollector}
     */
    static <A, R> DoubleCollector<A, R> of(Supplier<A> supplier, ObjDoubleConsumer<A> doubleAccumulator,
            BiConsumer<A, A> merger, Function<A, R> finisher) {
        return new DoubleCollectorImpl<>(supplier, doubleAccumulator, merger, finisher, NO_CHARACTERISTICS);
    }

    /**
     * Returns a {@code DoubleCollector} that converts the input numbers to
     * strings and concatenates them, separated by the specified delimiter, with
     * the specified prefix and suffix, in encounter order.
     *
     * @param delimiter the delimiter to be used between each element
     * @param prefix the sequence of characters to be used at the beginning of
     *        the joined result
     * @param suffix the sequence of characters to be used at the end of the
     *        joined result
     * @return A {@code DoubleCollector} which concatenates the input numbers,
     *         separated by the specified delimiter, in encounter order
     */
    static DoubleCollector<?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return PartialCollector.joining(delimiter, prefix, suffix, true).asDouble(
            StreamExInternals.joinAccumulatorDouble(delimiter));
    }

    /**
     * Returns a {@code DoubleCollector} that converts the input numbers to
     * strings and concatenates them, separated by the specified delimiter, in
     * encounter order.
     *
     * @param delimiter the delimiter to be used between each element
     * @return A {@code DoubleCollector} which concatenates the input numbers,
     *         separated by the specified delimiter, in encounter order
     */
    static DoubleCollector<?, String> joining(CharSequence delimiter) {
        return PartialCollector.joining(delimiter, null, null, false).asDouble(
            StreamExInternals.joinAccumulatorDouble(delimiter));
    }

    /**
     * Returns a {@code DoubleCollector} that counts the number of input
     * elements and returns the result as {@code Long}. If no elements are
     * present, the result is 0.
     *
     * @return a {@code DoubleCollector} that counts the input elements
     */
    static DoubleCollector<?, Long> counting() {
        return PartialCollector.longSum().asDouble((box, i) -> box[0]++);
    }

    /**
     * Returns an {@code DoubleCollector} that counts the number of input
     * elements and returns the result as {@code Integer}. If no elements are
     * present, the result is 0.
     *
     * @return an {@code DoubleCollector} that counts the input elements
     */
    static DoubleCollector<?, Integer> countingInt() {
        return PartialCollector.intSum().asDouble((box, i) -> box[0]++);
    }

    /**
     * Returns a {@code DoubleCollector} that produces the sum of the input
     * elements. If no elements are present, the result is 0.0.
     *
     * @return a {@code DoubleCollector} that produces the sum of the input
     *         elements
     */
    static DoubleCollector<?, Double> summing() {
        // Using DoubleSummaryStatistics as Kahan algorithm is implemented there
        return summarizing().andThen(DoubleSummaryStatistics::getSum);
    }

    /**
     * Returns a {@code DoubleCollector} that produces the arithmetic mean of
     * the input elements or an empty optional if no elements are collected.
     *
     * @return a {@code DoubleCollector} that produces the arithmetic mean of
     *         the input elements
     * @since 0.3.7
     */
    static DoubleCollector<?, OptionalDouble> averaging() {
        return summarizing().andThen(
            dss -> dss.getCount() == 0L ? OptionalDouble.empty() : OptionalDouble.of(dss.getAverage()));
    }

    /**
     * Returns a {@code DoubleCollector} that produces the minimal element,
     * described as an {@link OptionalDouble}. If no elements are present, the
     * result is an empty {@code OptionalDouble}.
     *
     * @return a {@code DoubleCollector} that produces the minimal element.
     */
    static DoubleCollector<?, OptionalDouble> min() {
        return reducing(Double::min);
    }

    /**
     * Returns a {@code DoubleCollector} that produces the maximal element,
     * described as an {@link OptionalDouble}. If no elements are present, the
     * result is an empty {@code OptionalDouble}.
     *
     * @return a {@code DoubleCollector} that produces the maximal element.
     */
    static DoubleCollector<?, OptionalDouble> max() {
        return reducing(Double::max);
    }

    /**
     * Adapts a {@code DoubleCollector} to another one by applying a mapping
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
    static <A, R> DoubleCollector<?, R> mapping(DoubleUnaryOperator mapper, DoubleCollector<A, R> downstream) {
        ObjDoubleConsumer<A> downstreamAccumulator = downstream.doubleAccumulator();
        return new DoubleCollectorImpl<>(downstream.supplier(), (r, t) -> downstreamAccumulator.accept(r, mapper
                .applyAsDouble(t)), downstream.merger(), downstream.finisher(), downstream.characteristics());
    }

    /**
     * Adapts a {@link Collector} accepting elements of type {@code U} to a
     * {@code DoubleCollector} by applying a mapping function to each input
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
    static <U, A, R> DoubleCollector<?, R> mappingToObj(DoubleFunction<U> mapper, Collector<U, A, R> downstream) {
        BiConsumer<A, U> accumulator = downstream.accumulator();
        if (downstream instanceof MergingCollector) {
            return new DoubleCollectorImpl<>(downstream.supplier(), (acc, i) -> accumulator
                    .accept(acc, mapper.apply(i)), ((MergingCollector<U, A, R>) downstream).merger(), downstream
                    .finisher(), downstream.characteristics());
        }
        return Box.partialCollector(downstream).asDouble((box, i) -> accumulator.accept(box.a, mapper.apply(i)));
    }

    /**
     * Returns a {@code DoubleCollector} which performs a reduction of its input
     * numbers under a specified {@link DoubleBinaryOperator}. The result is
     * described as an {@link OptionalDouble}.
     *
     * @param op a {@code DoubleBinaryOperator} used to reduce the input numbers
     * @return a {@code DoubleCollector} which implements the reduction
     *         operation.
     */
    static DoubleCollector<?, OptionalDouble> reducing(DoubleBinaryOperator op) {
        return of(PrimitiveBox::new, (box, d) -> {
            if (!box.b) {
                box.b = true;
                box.d = d;
            } else {
                box.d = op.applyAsDouble(box.d, d);
            }
        }, (box1, box2) -> {
            if (box2.b) {
                if (!box1.b) {
                    box1.from(box2);
                } else {
                    box1.d = op.applyAsDouble(box1.d, box2.d);
                }
            }
        }, PrimitiveBox::asDouble);
    }

    /**
     * Returns a {@code DoubleCollector} which performs a reduction of its input
     * numbers under a specified {@code IntBinaryOperator} using the provided
     * identity.
     *
     * @param identity the identity value for the reduction (also, the value
     *        that is returned when there are no input elements)
     * @param op a {@code DoubleBinaryOperator} used to reduce the input numbers
     * @return a {@code DoubleCollector} which implements the reduction
     *         operation
     */
    static DoubleCollector<?, Double> reducing(double identity, DoubleBinaryOperator op) {
        return of(() -> new double[] { identity }, (box, i) -> box[0] = op.applyAsDouble(box[0], i),
            (box1, box2) -> box1[0] = op.applyAsDouble(box1[0], box2[0]), UNBOX_DOUBLE);
    }

    /**
     * Returns a {@code DoubleCollector} which returns summary statistics for
     * the input elements.
     *
     * @return a {@code DoubleCollector} implementing the summary-statistics
     *         reduction
     */
    static DoubleCollector<?, DoubleSummaryStatistics> summarizing() {
        return of(DoubleSummaryStatistics::new, DoubleSummaryStatistics::accept, DoubleSummaryStatistics::combine);
    }

    /**
     * Returns a {@code DoubleCollector} which partitions the input elements
     * according to a {@code DoublePredicate}, and organizes them into a
     * {@code Map<Boolean, double[]>}.
     *
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} returned.
     *
     * @param predicate a predicate used for classifying input elements
     * @return a {@code DoubleCollector} implementing the partitioning operation
     */
    static DoubleCollector<?, Map<Boolean, double[]>> partitioningBy(DoublePredicate predicate) {
        return partitioningBy(predicate, toArray());
    }

    /**
     * Returns a {@code DoubleCollector} which partitions the input numbers
     * according to a {@code DoublePredicate}, reduces the values in each
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
     * @param downstream a {@code DoubleCollector} implementing the downstream
     *        reduction
     * @return a {@code DoubleCollector} implementing the cascaded partitioning
     *         operation
     */
    static <A, D> DoubleCollector<?, Map<Boolean, D>> partitioningBy(DoublePredicate predicate,
            DoubleCollector<A, D> downstream) {
        ObjDoubleConsumer<A> downstreamAccumulator = downstream.doubleAccumulator();
        ObjDoubleConsumer<BooleanMap<A>> accumulator = (result, t) -> downstreamAccumulator.accept(
            predicate.test(t) ? result.trueValue : result.falseValue, t);
        return BooleanMap.partialCollector(downstream).asDouble(accumulator);
    }

    /**
     * Returns a {@code DoubleCollector} implementing a "group by" operation on
     * input numbers, grouping them according to a classification function, and
     * returning the results in a {@code Map}.
     *
     * <p>
     * The classification function maps elements to some key type {@code K}. The
     * collector produces a {@code Map<K, double[]>} whose keys are the values
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
     * @return a {@code DoubleCollector} implementing the group-by operation
     */
    static <K> DoubleCollector<?, Map<K, double[]>> groupingBy(DoubleFunction<? extends K> classifier) {
        return groupingBy(classifier, toArray());
    }

    /**
     * Returns a {@code DoubleCollector} implementing a cascaded "group by"
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
     * @param downstream a {@code DoubleCollector} implementing the downstream
     *        reduction
     * @return a {@code DoubleCollector} implementing the cascaded group-by
     *         operation
     */
    static <K, D, A> DoubleCollector<?, Map<K, D>> groupingBy(DoubleFunction<? extends K> classifier,
            DoubleCollector<A, D> downstream) {
        return groupingBy(classifier, HashMap::new, downstream);
    }

    /**
     * Returns a {@code DoubleCollector} implementing a cascaded "group by"
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
     * @param downstream a {@code DoubleCollector} implementing the downstream
     *        reduction
     * @param mapFactory a function which, when called, produces a new empty
     *        {@code Map} of the desired type
     * @return a {@code DoubleCollector} implementing the cascaded group-by
     *         operation
     */
    static <K, D, A, M extends Map<K, D>> DoubleCollector<?, M> groupingBy(DoubleFunction<? extends K> classifier,
            Supplier<M> mapFactory, DoubleCollector<A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        Function<K, A> supplier = k -> downstreamSupplier.get();
        ObjDoubleConsumer<A> downstreamAccumulator = downstream.doubleAccumulator();
        ObjDoubleConsumer<Map<K, A>> accumulator = (m, t) -> {
            K key = Objects.requireNonNull(classifier.apply(t));
            A container = m.computeIfAbsent(key, supplier);
            downstreamAccumulator.accept(container, t);
        };
        return PartialCollector.grouping(mapFactory, downstream).asDouble(accumulator);
    }

    /**
     * Returns a {@code DoubleCollector} that produces the array of the input
     * elements. If no elements are present, the result is an empty array.
     *
     * @return a {@code DoubleCollector} that produces the array of the input
     *         elements
     */
    static DoubleCollector<?, double[]> toArray() {
        return of(DoubleBuffer::new, DoubleBuffer::add, DoubleBuffer::addAll, DoubleBuffer::toArray);
    }

    /**
     * Returns a {@code DoubleCollector} that produces the {@code float[]} array
     * of the input elements converting them via {@code (float)} casting. If no
     * elements are present, the result is an empty array.
     *
     * @return a {@code DoubleCollector} that produces the {@code float[]} array
     *         of the input elements
     */
    static DoubleCollector<?, float[]> toFloatArray() {
        return of(FloatBuffer::new, FloatBuffer::add, FloatBuffer::addAll, FloatBuffer::toArray);
    }

    /**
     * Returns a {@code DoubleCollector} which produces a boolean array
     * containing the results of applying the given predicate to the input
     * elements, in encounter order.
     * 
     * @param predicate a non-interfering, stateless predicate to apply to each
     *        input element. The result values of this predicate are collected
     *        to the resulting boolean array.
     * @return a {@code DoubleCollector} which collects the results of the
     *         predicate function to the boolean array, in encounter order.
     * @since 0.3.8
     */
    static DoubleCollector<?, boolean[]> toBooleanArray(DoublePredicate predicate) {
        return PartialCollector.booleanArray().asDouble((box, t) -> {
            if (predicate.test(t))
                box.a.set(box.b);
            box.b = StrictMath.addExact(box.b, 1);
        });
    }
}
