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

import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import static javax.util.streamex.StreamExInternals.*;

/**
 * A {@link Collector} specialized to work with primitive {@code int}.
 * 
 * @author Tagir Valeev
 *
 * @param <A>
 *            the mutable accumulation type of the reduction operation (often
 *            hidden as an implementation detail)
 * @param <R>
 *            the result type of the reduction operation
 * @see IntStreamEx#collect(IntCollector)
 * @since 0.3.0
 */
public interface IntCollector<A, R> extends Collector<Integer, A, R> {
    /**
     * A function that folds a value into a mutable result container.
     *
     * @return a function which folds a value into a mutable result container
     */
    ObjIntConsumer<A> intAccumulator();

    /**
     * A function which merges the second container into the first container.
     * 
     * @return a function which merges the second container into the first
     *         container.
     */
    BiConsumer<A, A> merger();

    @Override
    default BinaryOperator<A> combiner() {
        BiConsumer<A, A> merger = merger();
        return (a, b) -> {
            merger.accept(a, b);
            return a;
        };
    }

    @Override
    default BiConsumer<A, Integer> accumulator() {
        ObjIntConsumer<A> intAccumulator = intAccumulator();
        return (a, i) -> intAccumulator.accept(a, i);
    }

    static <R> IntCollector<R, R> of(Supplier<R> supplier, ObjIntConsumer<R> intAccumulator, BiConsumer<R, R> merger) {
        return new IntCollector<R, R>() {

            @Override
            public Supplier<R> supplier() {
                return supplier;
            }

            @Override
            public Function<R, R> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Collector.Characteristics> characteristics() {
                return EnumSet.of(Collector.Characteristics.IDENTITY_FINISH);
            }

            @Override
            public ObjIntConsumer<R> intAccumulator() {
                return intAccumulator;
            }

            @Override
            public BiConsumer<R, R> merger() {
                return merger;
            }
        };
    }

    static <A, R> IntCollector<?, R> of(Collector<Integer, A, R> collector) {
        if (collector instanceof IntCollector) {
            return (IntCollector<A, R>) collector;
        }
        return mappingToObj(i -> i, collector);
    }

    static <A, R> IntCollector<A, R> of(Supplier<A> supplier, ObjIntConsumer<A> intAccumulator,
            BiConsumer<A, A> merger, Function<A, R> finisher) {
        return new IntCollector<A, R>() {

            @Override
            public Supplier<A> supplier() {
                return supplier;
            }

            @Override
            public Function<A, R> finisher() {
                return finisher;
            }

            @Override
            public ObjIntConsumer<A> intAccumulator() {
                return intAccumulator;
            }

            @Override
            public Set<Collector.Characteristics> characteristics() {
                return EnumSet.noneOf(Collector.Characteristics.class);
            }

            @Override
            public BiConsumer<A, A> merger() {
                return merger;
            }
        };
    }

    /**
     * Returns an {@code IntCollector} that converts the input numbers to
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
     * @return An {@code IntCollector} which concatenates the input numbers,
     *         separated by the specified delimiter, in encounter order
     */
    static IntCollector<?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                joinMerger(delimiter), joinFinisher(prefix, suffix));
    }

    /**
     * Returns an {@code IntCollector} that converts the input numbers to
     * strings and concatenates them, separated by the specified delimiter, in
     * encounter order.
     *
     * @param delimiter
     *            the delimiter to be used between each element
     * @return An {@code IntCollector} which concatenates the input numbers,
     *         separated by the specified delimiter, in encounter order
     */
    static IntCollector<?, String> joining(CharSequence delimiter) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                joinMerger(delimiter), StringBuilder::toString);
    }

    /**
     * Returns an {@code IntCollector} that counts the number of input elements.
     * If no elements are present, the result is 0.
     *
     * @return an {@code IntCollector} that counts the input elements
     */
    static IntCollector<?, Long> counting() {
        return of(() -> new long[1], (box, i) -> box[0]++, (box1, box2) -> box1[0] += box2[0], UNBOX_LONG);
    }

    /**
     * Returns an {@code IntCollector} that produces the sum of the input
     * elements. If no elements are present, the result is 0.
     *
     * @return an {@code IntCollector} that produces the sum of the input
     *         elements
     */
    static IntCollector<?, Integer> summing() {
        return of(() -> new int[1], (box, i) -> box[0] += i, (box1, box2) -> box1[0] += box2[0], UNBOX_INT);
    }

    /**
     * Returns an {@code IntCollector} that produces the minimal element,
     * described as an {@link OptionalInt}. If no elements are present, the
     * result is an empty {@code OptionalInt}.
     *
     * @return an {@code IntCollector} that produces the minimal element.
     */
    static IntCollector<?, OptionalInt> min() {
        return reducing((a, b) -> a > b ? b : a);
    }

    /**
     * Returns an {@code IntCollector} that produces the maximal element,
     * described as an {@link OptionalInt}. If no elements are present, the
     * result is an empty {@code OptionalInt}.
     *
     * @return an {@code IntCollector} that produces the maximal element.
     */
    static IntCollector<?, OptionalInt> max() {
        return reducing((a, b) -> a > b ? a : b);
    }

    @SuppressWarnings("unchecked")
    static <A, R> IntCollector<?, R> mapping(IntUnaryOperator mapper, IntCollector<A, R> downstream) {
        ObjIntConsumer<A> downstreamAccumulator = downstream.intAccumulator();
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH))
            return (IntCollector<?, R>) of(downstream.supplier(),
                    (r, t) -> downstreamAccumulator.accept(r, mapper.applyAsInt(t)), downstream.merger());
        return of(downstream.supplier(), (r, t) -> downstreamAccumulator.accept(r, mapper.applyAsInt(t)),
                downstream.merger(), downstream.finisher());
    }

    @SuppressWarnings("unchecked")
    static <U, A, R> IntCollector<?, R> mappingToObj(IntFunction<U> mapper, Collector<U, A, R> downstream) {
        Supplier<A> supplier = downstream.supplier();
        BiConsumer<A, U> accumulator = downstream.accumulator();
        BinaryOperator<A> combiner = downstream.combiner();
        Function<A, R> finisher = downstream.finisher();
        return of(() -> new Object[] { supplier.get() }, (box, i) -> accumulator.accept((A) box[0], mapper.apply(i)), (
                box1, box2) -> box1[0] = combiner.apply((A) box1[0], (A) box2[0]), box -> finisher.apply((A) box[0]));
    }

    static <A, R, RR> IntCollector<A, RR> collectingAndThen(IntCollector<A, R> collector, Function<R, RR> finisher) {
        return of(collector.supplier(), collector.intAccumulator(), collector.merger(),
                collector.finisher().andThen(finisher));
    }

    /**
     * Returns an {@code IntCollector} which performs a reduction of its input
     * numbers under a specified {@link IntBinaryOperator}. The result is
     * described as an {@link OptionalInt}.
     *
     * @param op
     *            an {@code IntBinaryOperator} used to reduce the input numbers
     * @return an {@code IntCollector} which implements the reduction operation.
     */
    static IntCollector<?, OptionalInt> reducing(IntBinaryOperator op) {
        return of(() -> new int[2], (box, i) -> {
            if (box[1] == 0) {
                box[0] = i;
                box[1] = 1;
            } else {
                box[0] = op.applyAsInt(box[0], i);
            }
        }, (box1, box2) -> {
            if (box2[1] == 1) {
                if (box1[1] == 0) {
                    box1[0] = box2[0];
                    box1[1] = 1;
                } else {
                    box1[0] = op.applyAsInt(box1[0], box2[0]);
                }
            }
        }, box -> box[1] == 1 ? OptionalInt.of(box[0]) : OptionalInt.empty());
    }

    /**
     * Returns an {@code IntCollector} which performs a reduction of its input
     * numbers under a specified {@code IntBinaryOperator} using the provided
     * identity.
     *
     * @param identity
     *            the identity value for the reduction (also, the value that is
     *            returned when there are no input elements)
     * @param op
     *            an {@code IntBinaryOperator} used to reduce the input numbers
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

    static IntCollector<?, Map<Boolean, int[]>> partitioningBy(IntPredicate predicate) {
        return partitioningBy(predicate, toArray());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static <A, D> IntCollector<?, Map<Boolean, D>> partitioningBy(IntPredicate predicate, IntCollector<A, D> downstream) {
        ObjIntConsumer<A> downstreamAccumulator = downstream.intAccumulator();
        ObjIntConsumer<BooleanMap<A>> accumulator = (result, t) -> downstreamAccumulator.accept(
                predicate.test(t) ? result.trueValue : result.falseValue, t);
        BiConsumer<BooleanMap<A>, BooleanMap<A>> merger = BooleanMap.merger(downstream.merger());
        Supplier<BooleanMap<A>> supplier = BooleanMap.supplier(downstream.supplier());
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return (IntCollector) of(supplier, accumulator, merger);
        } else {
            return of(supplier, accumulator, merger, BooleanMap.finisher(downstream.finisher()));
        }
    }

    static <K> IntCollector<?, Map<K, int[]>> groupingBy(IntFunction<? extends K> classifier) {
        return groupingBy(classifier, toArray());
    }

    static <K, D, A> IntCollector<?, Map<K, D>> groupingBy(IntFunction<? extends K> classifier,
            IntCollector<A, D> downstream) {
        return groupingBy(classifier, HashMap::new, downstream);
    }

    @SuppressWarnings("unchecked")
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
        BiConsumer<Map<K, A>, Map<K, A>> merger = mapMerger(downstream.merger());

        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return (IntCollector<?, M>) of((Supplier<Map<K, A>>) mapFactory, accumulator, merger);
        } else {
            return of((Supplier<Map<K, A>>) mapFactory, accumulator, merger,
                    mapFinisher((Function<A, A>) downstream.finisher()));
        }
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
}
