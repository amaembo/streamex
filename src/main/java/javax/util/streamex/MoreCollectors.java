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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;

import static javax.util.streamex.StreamExInternals.*;

/**
 * Implementations of several collectors in addition to ones available in JDK.
 * 
 * @author Tagir Valeev
 * @see Collectors
 * @since 0.3.2
 */
public final class MoreCollectors {
    private MoreCollectors() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a {@code Collector} which just ignores the input and calls the
     * provided supplier once to return the output.
     * 
     * @param <T>
     *            the type of input elements
     * @param <U>
     *            the type of output
     * @param supplier
     *            the supplier of the output
     * @return a {@code Collector} which just ignores the input and calls the
     *         provided supplier once to return the output.
     */
    private static <T, U> Collector<T, ?, U> empty(Supplier<U> supplier) {
        return Collector.of(() -> null, (acc, t) -> {
        }, selectFirst(), acc -> supplier.get(), Collector.Characteristics.UNORDERED,
                Collector.Characteristics.CONCURRENT);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new array.
     * 
     * The operation performed by the returned collector is equivalent to
     * {@code stream.toArray(generator)}. This collector is mostly useful as a
     * downstream collector.
     *
     * @param <T>
     *            the type of the input elements
     * @param generator
     *            a function which produces a new array of the desired type and
     *            the provided length
     * @return a {@code Collector} which collects all the input elements into an
     *         array, in encounter order
     */
    public static <T> Collector<T, ?, T[]> toArray(IntFunction<T[]> generator) {
        return Collectors.collectingAndThen(Collectors.toList(), list -> list.toArray(generator.apply(list.size())));
    }

    /**
     * Returns a {@code Collector} which counts a number of distinct values the
     * mapper function returns for the stream elements.
     * 
     * The operation performed by the returned collector is equivalent to
     * {@code stream.map(mapper).distinct().count()}. This collector is mostly
     * useful as a downstream collector.
     * 
     * @param <T>
     *            the type of the input elements
     * @param <U>
     *            the type of objects the mapper function produces
     * @param mapper
     *            a function which classifies input elements.
     * @return a collector which counts a number of distinct classes the mapper
     *         function returns for the stream elements.
     */
    public static <T, U> Collector<T, ?, Integer> distinctCount(Function<? super T, U> mapper) {
        return Collectors.collectingAndThen(Collectors.mapping(mapper, Collectors.toSet()), Set::size);
    }

    /**
     * Returns a {@code Collector} which aggregates the results of two supplied
     * collectors using the supplied finisher function.
     * 
     * @param <T>
     *            the type of the input elements
     * @param <A1>
     *            the intermediate accumulation type of the first collector
     * @param <A2>
     *            the intermediate accumulation type of the second collector
     * @param <R1>
     *            the result type of the first collector
     * @param <R2>
     *            the result type of the second collector
     * @param <R>
     *            the final result type
     * @param c1
     *            the first collector
     * @param c2
     *            the second collector
     * @param finisher
     *            the function which merges two results into the single one.
     * @return a {@code Collector} which aggregates the results of two supplied
     *         collectors.
     */
    public static <T, A1, A2, R1, R2, R> Collector<T, ?, R> pairing(Collector<? super T, A1, R1> c1,
            Collector<? super T, A2, R2> c2, BiFunction<? super R1, ? super R2, ? extends R> finisher) {
        EnumSet<Characteristics> c = EnumSet.noneOf(Characteristics.class);
        c.addAll(c1.characteristics());
        c.retainAll(c2.characteristics());
        c.remove(Characteristics.IDENTITY_FINISH);

        Supplier<A1> c1Supplier = c1.supplier();
        Supplier<A2> c2Supplier = c2.supplier();
        BiConsumer<A1, ? super T> c1Accumulator = c1.accumulator();
        BiConsumer<A2, ? super T> c2Accumulator = c2.accumulator();
        BinaryOperator<A1> c1Combiner = c1.combiner();
        BinaryOperator<A2> c2combiner = c2.combiner();

        Supplier<PairBox<A1, A2>> supplier = () -> new PairBox<>(c1Supplier.get(), c2Supplier.get());
        BiConsumer<PairBox<A1, A2>, T> accumulator = (acc, v) -> {
            c1Accumulator.accept(acc.a, v);
            c2Accumulator.accept(acc.b, v);
        };
        BinaryOperator<PairBox<A1, A2>> combiner = (acc1, acc2) -> {
            acc1.a = c1Combiner.apply(acc1.a, acc2.a);
            acc1.b = c2combiner.apply(acc1.b, acc2.b);
            return acc1;
        };
        return Collector.of(supplier, accumulator, combiner, acc -> {
            R1 r1 = c1.finisher().apply(acc.a);
            R2 r2 = c2.finisher().apply(acc.b);
            return finisher.apply(r1, r2);
        }, c.toArray(new Characteristics[c.size()]));
    }

    public static <T, A, D> Collector<T, ?, D> maxAll(Comparator<? super T> comparator,
            Collector<? super T, A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BinaryOperator<A> downstreamCombiner = downstream.combiner();
        @SuppressWarnings("unchecked")
        Supplier<PairBox<A, T>> supplier = () -> new PairBox<>(downstreamSupplier.get(), (T) NONE);
        BiConsumer<PairBox<A, T>, T> accumulator = (acc, t) -> {
            if (acc.b == NONE) {
                downstreamAccumulator.accept(acc.a, t);
                acc.b = t;
            } else {
                int cmp = comparator.compare(t, acc.b);
                if (cmp > 0) {
                    acc.a = downstreamSupplier.get();
                    acc.b = t;
                }
                if (cmp >= 0)
                    downstreamAccumulator.accept(acc.a, t);
            }
        };
        BinaryOperator<PairBox<A, T>> combiner = (acc1, acc2) -> {
            if (acc2.b == NONE) {
                return acc1;
            }
            if (acc1.b == NONE) {
                return acc2;
            }
            int cmp = comparator.compare(acc1.b, acc2.b);
            if (cmp > 0) {
                return acc1;
            }
            if (cmp < 0) {
                return acc2;
            }
            acc1.a = downstreamCombiner.apply(acc1.a, acc2.a);
            return acc1;
        };
        Function<PairBox<A, T>, D> finisher = acc -> downstream.finisher().apply(acc.a);
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    public static <T> Collector<T, ?, List<T>> maxAll(Comparator<? super T> comparator) {
        return maxAll(comparator, Collectors.toList());
    }

    public static <T extends Comparable<? super T>, A, D> Collector<T, ?, D> maxAll(Collector<T, A, D> downstream) {
        return maxAll(Comparator.<T> naturalOrder(), downstream);
    }

    public static <T extends Comparable<? super T>> Collector<T, ?, List<T>> maxAll() {
        return maxAll(Comparator.<T> naturalOrder(), Collectors.toList());
    }

    public static <T, A, D> Collector<T, ?, D> minAll(Comparator<? super T> comparator, Collector<T, A, D> downstream) {
        return maxAll(comparator.reversed(), downstream);
    }

    public static <T> Collector<T, ?, List<T>> minAll(Comparator<? super T> comparator) {
        return maxAll(comparator.reversed(), Collectors.toList());
    }

    public static <T extends Comparable<? super T>, A, D> Collector<T, ?, D> minAll(Collector<T, A, D> downstream) {
        return maxAll(Comparator.<T> reverseOrder(), downstream);
    }

    public static <T extends Comparable<? super T>> Collector<T, ?, List<T>> minAll() {
        return maxAll(Comparator.<T> reverseOrder(), Collectors.toList());
    }

    /**
     * Returns a {@code Collector} which collects only the first stream element
     * if any.
     * 
     * The operation performed by the returned collector is equivalent to
     * {@code stream.findFirst()}. This collector is mostly useful as a
     * downstream collector.
     * 
     * @param <T>
     *            the type of the input elements
     * @return a collector which returns an {@link Optional} which describes the
     *         first element of the stream. For empty stream an empty
     *         {@code Optional} is returned.
     */
    public static <T> Collector<T, ?, Optional<T>> first() {
        return Collectors.reducing(selectFirst());
    }

    /**
     * Returns a {@code Collector} which collects only the last stream element
     * if any.
     * 
     * @param <T>
     *            the type of the input elements
     * @return a collector which returns an {@link Optional} which describes the
     *         last element of the stream. For empty stream an empty
     *         {@code Optional} is returned.
     */
    public static <T> Collector<T, ?, Optional<T>> last() {
        return Collectors.reducing(selectLast());
    }

    /**
     * Returns a {@code Collector} which collects at most specified number of
     * the first stream elements into the {@link List}.
     * 
     * The operation performed by the returned collector is equivalent to
     * {@code stream.limit(n).collect(Collectors.toList())}. This collector is
     * mostly useful as a downstream collector.
     * 
     * @param <T>
     *            the type of the input elements
     * @param n
     *            maximum number of stream elements to preserve
     * @return a collector which returns a {@code List} containing the first n
     *         stream elements or less if the stream was shorter.
     */
    public static <T> Collector<T, ?, List<T>> head(int n) {
        if (n <= 0)
            return empty(ArrayList<T>::new);
        return Collector.<T, List<T>> of(ArrayList::new, (acc, t) -> {
            if (acc.size() < n)
                acc.add(t);
        }, (acc1, acc2) -> {
            acc1.addAll(acc2.subList(0, Math.min(acc2.size(), n - acc1.size())));
            return acc1;
        });
    }

    /**
     * Returns a {@code Collector} which collects at most specified number of
     * the last stream elements into the {@link List}.
     * 
     * @param <T>
     *            the type of the input elements
     * @param n
     *            maximum number of stream elements to preserve
     * @return a collector which returns a {@code List} containing the last n
     *         stream elements or less if the stream was shorter.
     */
    public static <T> Collector<T, ?, List<T>> tail(int n) {
        if (n <= 0)
            return empty(ArrayList<T>::new);
        return Collector.<T, Deque<T>, List<T>> of(ArrayDeque::new, (acc, t) -> {
            if (acc.size() == n)
                acc.pollFirst();
            acc.addLast(t);
        }, (acc1, acc2) -> {
            while (acc2.size() < n && !acc1.isEmpty()) {
                acc2.addFirst(acc1.pollLast());
            }
            return acc2;
        }, ArrayList<T>::new);
    }

    public static <T> Collector<T, ?, List<T>> maxN(Comparator<? super T> comparator, int limit) {
        BiConsumer<PriorityQueue<T>, T> accumulator = (queue, t) -> {
            queue.add(t);
            if (queue.size() > limit)
                queue.poll();
        };
        return Collector.of(() -> new PriorityQueue<>(comparator), accumulator, (q1, q2) -> {
            for (T t : q2) {
                accumulator.accept(q1, t);
            }
            return q1;
        }, queue -> {
            List<T> result = new ArrayList<>(queue.size());
            while (!queue.isEmpty()) {
                result.add(queue.poll());
            }
            Collections.reverse(result);
            return result;
        });
    }

    public static <T extends Comparable<? super T>> Collector<T, ?, List<T>> maxN(int limit) {
        return maxN(Comparator.<T> naturalOrder(), limit);
    }

    public static <T> Collector<T, ?, List<T>> minN(Comparator<? super T> comparator, int limit) {
        return maxN(comparator.reversed(), limit);
    }

    public static <T extends Comparable<? super T>> Collector<T, ?, List<T>> minN(int limit) {
        return maxN(Comparator.<T> reverseOrder(), limit);
    }
}
