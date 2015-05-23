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

import java.util.HashMap;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
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

import static javax.util.streamex.StreamExInternals.*;

/**
 * A {@link Collector} specialized to work with primitive {@code long}.
 * 
 * @author Tagir Valeev
 *
 * @param <A>
 *            the mutable accumulation type of the reduction operation (often
 *            hidden as an implementation detail)
 * @param <R>
 *            the result type of the reduction operation
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
        ObjLongConsumer<A> longAccumulator = longAccumulator();
        return (a, i) -> longAccumulator.accept(a, i);
    }

    static <R> LongCollector<R, R> of(Supplier<R> supplier, ObjLongConsumer<R> longAccumulator, BiConsumer<R, R> merger) {
        return new LongCollectorImpl<>(supplier, longAccumulator, merger, Function.identity(), ID_CHARACTERISTICS);
    }

    static <A, R> LongCollector<?, R> of(Collector<Long, A, R> collector) {
        if (collector instanceof LongCollector) {
            return (LongCollector<A, R>) collector;
        }
        return mappingToObj(Long::valueOf, collector);
    }

    static <A, R> LongCollector<A, R> of(Supplier<A> supplier, ObjLongConsumer<A> longAccumulator,
            BiConsumer<A, A> merger, Function<A, R> finisher) {
        return new LongCollectorImpl<>(supplier, longAccumulator, merger, finisher, NO_CHARACTERISTICS);
    }

    /**
     * Returns a {@code LongCollector} that converts the input numbers to
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
     * @return A {@code LongCollector} which concatenates the input numbers,
     *         separated by the specified delimiter, in encounter order
     */
    static LongCollector<?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                joinMerger(delimiter), joinFinisher(prefix, suffix));
    }

    /**
     * Returns a {@code LongCollector} that converts the input numbers to
     * strings and concatenates them, separated by the specified delimiter, in
     * encounter order.
     *
     * @param delimiter
     *            the delimiter to be used between each element
     * @return A {@code LongCollector} which concatenates the input numbers,
     *         separated by the specified delimiter, in encounter order
     */
    static LongCollector<?, String> joining(CharSequence delimiter) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                joinMerger(delimiter), StringBuilder::toString);
    }

    /**
     * Returns a {@code LongCollector} that counts the number of input elements.
     * If no elements are present, the result is 0.
     *
     * @return a {@code LongCollector} that counts the input elements
     */
    static LongCollector<?, Long> counting() {
        return of(() -> new long[1], (box, i) -> box[0]++, SUM_LONG, UNBOX_LONG);
    }

    /**
     * Returns a {@code LongCollector} that produces the sum of the input
     * elements. If no elements are present, the result is 0.
     *
     * @return a {@code LongCollector} that produces the sum of the input
     *         elements
     */
    static LongCollector<?, Long> summing() {
        return of(() -> new long[1], (box, i) -> box[0] += i, SUM_LONG, UNBOX_LONG);
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
    static <A, R> LongCollector<?, R> mapping(LongUnaryOperator mapper, LongCollector<A, R> downstream) {
        ObjLongConsumer<A> downstreamAccumulator = downstream.longAccumulator();
        return new LongCollectorImpl<>(downstream.supplier(), (r, t) -> downstreamAccumulator.accept(r,
                mapper.applyAsLong(t)), downstream.merger(), downstream.finisher(), downstream.characteristics());
    }

    /**
     * Adapts a {@link Collector} accepting elements of type {@code U} to a
     * {@code LongCollector} by applying a mapping function to each input
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
    static <U, A, R> LongCollector<?, R> mappingToObj(LongFunction<U> mapper, Collector<U, A, R> downstream) {
        BiConsumer<A, U> accumulator = downstream.accumulator();
        if (downstream instanceof MergingCollector) {
            return new LongCollectorImpl<>(downstream.supplier(), (acc, i) -> accumulator.accept(acc, mapper.apply(i)),
                    ((MergingCollector<U, A, R>) downstream).merger(), downstream.finisher(),
                    downstream.characteristics());
        }
        return of(boxSupplier(downstream.supplier()), (box, i) -> accumulator.accept((A) box[0], mapper.apply(i)),
                boxCombiner(downstream.combiner()), boxFinisher(downstream.finisher()));
    }

    /**
     * Adapts a {@code LongCollector} to perform an additional finishing
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
    static <A, R, RR> LongCollector<A, RR> collectingAndThen(LongCollector<A, R> downstream, Function<R, RR> finisher) {
        return of(downstream.supplier(), downstream.longAccumulator(), downstream.merger(), downstream.finisher()
                .andThen(finisher));
    }

    /**
     * Returns a {@code LongCollector} which performs a reduction of its input
     * numbers under a specified {@link LongBinaryOperator}. The result is
     * described as an {@link OptionalLong}.
     *
     * @param op
     *            a {@code LongBinaryOperator} used to reduce the input numbers
     * @return a {@code LongCollector} which implements the reduction operation.
     */
    static LongCollector<?, OptionalLong> reducing(LongBinaryOperator op) {
        return of(() -> new long[2], (box, i) -> {
            if (box[1] == 0) {
                box[0] = i;
                box[1] = 1;
            } else {
                box[0] = op.applyAsLong(box[0], i);
            }
        }, (box1, box2) -> {
            if (box2[1] == 1) {
                if (box1[1] == 0) {
                    box1[0] = box2[0];
                    box1[1] = 1;
                } else {
                    box1[0] = op.applyAsLong(box1[0], box2[0]);
                }
            }
        }, box -> box[1] == 1 ? OptionalLong.of(box[0]) : OptionalLong.empty());
    }

    /**
     * Returns a {@code LongCollector} which performs a reduction of its input
     * numbers under a specified {@code IntBinaryOperator} using the provided
     * identity.
     *
     * @param identity
     *            the identity value for the reduction (also, the value that is
     *            returned when there are no input elements)
     * @param op
     *            a {@code LongBinaryOperator} used to reduce the input numbers
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
     * @param predicate
     *            a predicate used for classifying input elements
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
     * @param <A>
     *            the intermediate accumulation type of the downstream collector
     * @param <D>
     *            the result type of the downstream reduction
     * @param predicate
     *            a predicate used for classifying input elements
     * @param downstream
     *            a {@code LongCollector} implementing the downstream reduction
     * @return a {@code LongCollector} implementing the cascaded partitioning
     *         operation
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static <A, D> LongCollector<?, Map<Boolean, D>> partitioningBy(LongPredicate predicate,
            LongCollector<A, D> downstream) {
        ObjLongConsumer<A> downstreamAccumulator = downstream.longAccumulator();
        ObjLongConsumer<BooleanMap<A>> accumulator = (result, t) -> downstreamAccumulator.accept(
                predicate.test(t) ? result.trueValue : result.falseValue, t);
        BiConsumer<BooleanMap<A>, BooleanMap<A>> merger = BooleanMap.merger(downstream.merger());
        Supplier<BooleanMap<A>> supplier = BooleanMap.supplier(downstream.supplier());
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return (LongCollector) of(supplier, accumulator, merger);
        } else {
            return of(supplier, accumulator, merger, BooleanMap.finisher(downstream.finisher()));
        }
    }

    static <K> LongCollector<?, Map<K, long[]>> groupingBy(LongFunction<? extends K> classifier) {
        return groupingBy(classifier, toArray());
    }

    static <K, D, A> LongCollector<?, Map<K, D>> groupingBy(LongFunction<? extends K> classifier,
            LongCollector<A, D> downstream) {
        return groupingBy(classifier, HashMap::new, downstream);
    }

    @SuppressWarnings("unchecked")
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
        BiConsumer<Map<K, A>, Map<K, A>> merger = mapMerger(downstream.merger());

        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return (LongCollector<?, M>) of((Supplier<Map<K, A>>) mapFactory, accumulator, merger);
        } else {
            return of((Supplier<Map<K, A>>) mapFactory, accumulator, merger,
                    mapFinisher((Function<A, A>) downstream.finisher()));
        }
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
}
