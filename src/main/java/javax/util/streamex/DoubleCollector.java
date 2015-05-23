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
import java.util.function.LongBinaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static javax.util.streamex.StreamExInternals.*;

/**
 * A {@link Collector} specialized to work with primitive {@code double}.
 * 
 * @author Tagir Valeev
 *
 * @param <A>
 *            the mutable accumulation type of the reduction operation (often
 *            hidden as an implementation detail)
 * @param <R>
 *            the result type of the reduction operation
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
        ObjDoubleConsumer<A> doubleAccumulator = doubleAccumulator();
        return (a, i) -> doubleAccumulator.accept(a, i);
    }

    static <R> DoubleCollector<R, R> of(Supplier<R> supplier, ObjDoubleConsumer<R> doubleAccumulator,
            BiConsumer<R, R> merger) {
        return new DoubleCollectorImpl<>(supplier, doubleAccumulator, merger, Function.identity(), ID_CHARACTERISTICS);
    }

    static <A, R> DoubleCollector<?, R> of(Collector<Double, A, R> collector) {
        if (collector instanceof DoubleCollector) {
            return (DoubleCollector<A, R>) collector;
        }
        return mappingToObj(Double::valueOf, collector);
    }

    static <A, R> DoubleCollector<A, R> of(Supplier<A> supplier, ObjDoubleConsumer<A> doubleAccumulator,
            BiConsumer<A, A> merger, Function<A, R> finisher) {
        return new DoubleCollectorImpl<>(supplier, doubleAccumulator, merger, finisher, NO_CHARACTERISTICS);
    }

    /**
     * Returns a {@code DoubleCollector} that converts the input numbers to
     * strings and concatenates them, separated by the specified delimiter, with
     * the specified prefix and suffix, in encounter order.
     *
     * @param delimiter
     *            the delimiter to be used between each element
     * @param prefix
     *            the sequence of characters to be used at the beginning of the
     *            joined result
     * @param suffix
     *            the sequence of characters to be used at the end of the joined
     *            result
     * @return A {@code DoubleCollector} which concatenates the input numbers,
     *         separated by the specified delimiter, in encounter order
     */
    static DoubleCollector<?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                joinMerger(delimiter), joinFinisher(prefix, suffix));
    }

    /**
     * Returns a {@code DoubleCollector} that converts the input numbers to
     * strings and concatenates them, separated by the specified delimiter, in
     * encounter order.
     *
     * @param delimiter
     *            the delimiter to be used between each element
     * @return A {@code DoubleCollector} which concatenates the input numbers,
     *         separated by the specified delimiter, in encounter order
     */
    static DoubleCollector<?, String> joining(CharSequence delimiter) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                joinMerger(delimiter), StringBuilder::toString);
    }

    /**
     * Returns a {@code DoubleCollector} that counts the number of input
     * elements. If no elements are present, the result is 0.
     *
     * @return a {@code DoubleCollector} that counts the input elements
     */
    static DoubleCollector<?, Long> counting() {
        return of(() -> new long[1], (box, i) -> box[0]++, SUM_LONG, UNBOX_LONG);
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
        return collectingAndThen(summarizing(), DoubleSummaryStatistics::getSum);
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
     * @param <A>
     *            intermediate accumulation type of the downstream collector
     * @param <R>
     *            result type of collector
     * @param mapper
     *            a function to be applied to the input elements
     * @param downstream
     *            a collector which will accept mapped values
     * @return a collector which applies the mapping function to the input
     *         elements and provides the mapped results to the downstream
     *         collector
     */
    static <A, R> DoubleCollector<?, R> mapping(DoubleUnaryOperator mapper, DoubleCollector<A, R> downstream) {
        ObjDoubleConsumer<A> downstreamAccumulator = downstream.doubleAccumulator();
        return new DoubleCollectorImpl<>(downstream.supplier(), (r, t) -> downstreamAccumulator.accept(r,
                mapper.applyAsDouble(t)), downstream.merger(), downstream.finisher(), downstream.characteristics());
    }

    /**
     * Adapts a {@link Collector} accepting elements of type {@code U} to a
     * {@code DoubleCollector} by applying a mapping function to each input
     * element before accumulation.
     *
     * @param <U>
     *            type of elements accepted by downstream collector
     * @param <A>
     *            intermediate accumulation type of the downstream collector
     * @param <R>
     *            result type of collector
     * @param mapper
     *            a function to be applied to the input elements
     * @param downstream
     *            a collector which will accept mapped values
     * @return a collector which applies the mapping function to the input
     *         elements and provides the mapped results to the downstream
     *         collector
     */
    @SuppressWarnings("unchecked")
    static <U, A, R> DoubleCollector<?, R> mappingToObj(DoubleFunction<U> mapper, Collector<U, A, R> downstream) {
        BiConsumer<A, U> accumulator = downstream.accumulator();
        if (downstream instanceof MergingCollector) {
            return new DoubleCollectorImpl<>(downstream.supplier(),
                    (acc, i) -> accumulator.accept(acc, mapper.apply(i)),
                    ((MergingCollector<U, A, R>) downstream).merger(), downstream.finisher(),
                    downstream.characteristics());
        }
        return of(boxSupplier(downstream.supplier()), (box, i) -> accumulator.accept((A) box[0], mapper.apply(i)),
                boxCombiner(downstream.combiner()), boxFinisher(downstream.finisher()));
    }

    /**
     * Adapts a {@code DoubleCollector} to perform an additional finishing
     * transformation.
     *
     * @param <A>
     *            intermediate accumulation type of the downstream collector
     * @param <R>
     *            result type of the downstream collector
     * @param <RR>
     *            result type of the resulting collector
     * @param downstream
     *            a collector
     * @param finisher
     *            a function to be applied to the final result of the downstream
     *            collector
     * @return a collector which performs the action of the downstream
     *         collector, followed by an additional finishing step
     */
    static <A, R, RR> DoubleCollector<A, RR> collectingAndThen(DoubleCollector<A, R> downstream,
            Function<R, RR> finisher) {
        return of(downstream.supplier(), downstream.doubleAccumulator(), downstream.merger(), downstream.finisher()
                .andThen(finisher));
    }

    /**
     * Returns a {@code DoubleCollector} which performs a reduction of its input
     * numbers under a specified {@link LongBinaryOperator}. The result is
     * described as an {@link OptionalDouble}.
     *
     * @param op
     *            a {@code DoubleBinaryOperator} used to reduce the input
     *            numbers
     * @return a {@code DoubleCollector} which implements the reduction
     *         operation.
     */
    static DoubleCollector<?, OptionalDouble> reducing(DoubleBinaryOperator op) {
        return of(() -> new double[2], (box, i) -> {
            if (box[1] == 0) {
                box[0] = i;
                box[1] = 1;
            } else {
                box[0] = op.applyAsDouble(box[0], i);
            }
        }, (box1, box2) -> {
            if (box2[1] == 1) {
                if (box1[1] == 0) {
                    box1[0] = box2[0];
                    box1[1] = 1;
                } else {
                    box1[0] = op.applyAsDouble(box1[0], box2[0]);
                }
            }
        }, box -> box[1] == 1 ? OptionalDouble.of(box[0]) : OptionalDouble.empty());
    }

    /**
     * Returns a {@code DoubleCollector} which performs a reduction of its input
     * numbers under a specified {@code IntBinaryOperator} using the provided
     * identity.
     *
     * @param identity
     *            the identity value for the reduction (also, the value that is
     *            returned when there are no input elements)
     * @param op
     *            a {@code DoubleBinaryOperator} used to reduce the input
     *            numbers
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
     * @param predicate
     *            a predicate used for classifying input elements
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
     * @param <A>
     *            the intermediate accumulation type of the downstream collector
     * @param <D>
     *            the result type of the downstream reduction
     * @param predicate
     *            a predicate used for classifying input elements
     * @param downstream
     *            a {@code DoubleCollector} implementing the downstream
     *            reduction
     * @return a {@code DoubleCollector} implementing the cascaded partitioning
     *         operation
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static <A, D> DoubleCollector<?, Map<Boolean, D>> partitioningBy(DoublePredicate predicate,
            DoubleCollector<A, D> downstream) {
        ObjDoubleConsumer<A> downstreamAccumulator = downstream.doubleAccumulator();
        ObjDoubleConsumer<BooleanMap<A>> accumulator = (result, t) -> downstreamAccumulator.accept(
                predicate.test(t) ? result.trueValue : result.falseValue, t);
        BiConsumer<BooleanMap<A>, BooleanMap<A>> merger = BooleanMap.merger(downstream.merger());
        Supplier<BooleanMap<A>> supplier = BooleanMap.supplier(downstream.supplier());
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return (DoubleCollector) of(supplier, accumulator, merger);
        } else {
            return of(supplier, accumulator, merger, BooleanMap.finisher(downstream.finisher()));
        }
    }

    static <K> DoubleCollector<?, Map<K, double[]>> groupingBy(DoubleFunction<? extends K> classifier) {
        return groupingBy(classifier, toArray());
    }

    static <K, D, A> DoubleCollector<?, Map<K, D>> groupingBy(DoubleFunction<? extends K> classifier,
            DoubleCollector<A, D> downstream) {
        return groupingBy(classifier, HashMap::new, downstream);
    }

    @SuppressWarnings("unchecked")
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
        BiConsumer<Map<K, A>, Map<K, A>> merger = mapMerger(downstream.merger());

        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return (DoubleCollector<?, M>) of((Supplier<Map<K, A>>) mapFactory, accumulator, merger);
        } else {
            return of((Supplier<Map<K, A>>) mapFactory, accumulator, merger,
                    mapFinisher((Function<A, A>) downstream.finisher()));
        }
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
}
