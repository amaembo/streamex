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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static one.util.streamex.AbstractStreamEx.addToMap;
import static one.util.streamex.Internals.*;

/**
 * Implementations of several collectors in addition to ones available in JDK.
 * 
 * @author Tagir Valeev
 * @see Collectors
 * @see Joining
 * @since 0.3.2
 */
@NullMarked
public final class MoreCollectors {
    private MoreCollectors() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a {@code Collector} which just ignores the input and calls the
     * provided supplier once to return the output.
     * 
     * @param <T> the type of input elements
     * @param <U> the type of output
     * @param supplier the supplier of the output
     * @return a {@code Collector} which just ignores the input and calls the
     *         provided supplier once to return the output.
     */
    private static <T extends @Nullable Object, U extends @Nullable Object> Collector<T, ?, U> empty(
            Supplier<U> supplier) {
        return new CancellableCollectorImpl<>(() -> NONE, (acc, t) -> {
            // empty
        }, selectFirst(), acc -> supplier.get(), alwaysTrue(), EnumSet.of(Characteristics.UNORDERED,
            Characteristics.CONCURRENT));
    }

    private static <T extends @Nullable Object> Collector<T, ?, List<T>> empty() {
        return empty(ArrayList::new);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new array.
     *
     * <p>
     * The operation performed by the returned collector is equivalent to
     * {@code stream.toArray(generator)}. This collector is mostly useful as a
     * downstream collector.
     *
     * @param <T> the type of the input elements
     * @param generator a function which produces a new array of the desired
     *        type and the provided length
     * @return a {@code Collector} which collects all the input elements into an
     *         array, in encounter order
     * @throws NullPointerException if generator is null.
     */
    public static <T extends @Nullable Object> Collector<T, ?, T[]> toArray(IntFunction<T[]> generator) {
        Objects.requireNonNull(generator);
        return Collectors.collectingAndThen(Collectors.toList(), list -> list.toArray(generator.apply(list.size())));
    }

    /**
     * Returns a {@code Collector} which produces a boolean array containing the
     * results of applying the given predicate to the input elements, in
     * encounter order.
     * 
     * @param <T> the type of the input elements
     * @param predicate a non-interfering, stateless predicate to apply to each
     *        input element. The result values of this predicate are collected
     *        to the resulting boolean array.
     * @return a {@code Collector} which collects the results of the predicate
     *         function to the boolean array, in encounter order.
     * @throws NullPointerException if predicate is null.
     * @since 0.3.8
     */
    public static <T extends @Nullable Object> Collector<T, ?, boolean[]> toBooleanArray(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return PartialCollector.booleanArray().asRef((box, t) -> {
            if (predicate.test(t))
                box.a.set(box.b);
            box.b = StrictMath.addExact(box.b, 1);
        });
    }

    /**
     * Returns a {@code Collector} that accumulates the input enum values into a
     * new {@code EnumSet}.
     *
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>: it may not process all the elements if the resulting set
     * contains all possible enum values.
     * 
     * @param <T> the type of the input elements
     * @param enumClass the class of input enum values
     * @return a {@code Collector} which collects all the input elements into a
     *         {@code EnumSet}
     * @throws NullPointerException if enumClass is null.
     */
    public static <T extends Enum<T>> Collector<T, ?, EnumSet<T>> toEnumSet(Class<T> enumClass) {
        int size = EnumSet.allOf(enumClass).size();
        return new CancellableCollectorImpl<>(() -> EnumSet.noneOf(enumClass), EnumSet::add, (s1, s2) -> {
            s1.addAll(s2);
            return s1;
        }, Function.identity(), set -> set.size() == size, UNORDERED_ID_CHARACTERISTICS);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code Map}
     * whose keys and values are taken from {@code Map.Entry}.
     *
     * <p>
     * There are no guarantees on the type or serializability of the {@code Map} returned;
     * if more control over the returned {@code Map} is required, use {@link #entriesToCustomMap(Supplier)}
     *
     * <p>
     * Returned {@code Map} is guaranteed to be modifiable. See {@link one.util.streamex.EntryStream#toMap()}.
     *
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     * @return {@code Collector} which collects elements into a {@code Map}
     * whose keys and values are taken from {@code Map.Entry}
     * @throws IllegalStateException if this stream contains duplicate keys
     *                               (according to {@link Object#equals(Object)}).
     * @see #entriesToMap(BinaryOperator)
     * @see Collectors#toMap(Function, Function)
     * @since 0.7.3
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> Collector<Entry<? extends K, ? extends V>, ?, Map<K, V>> entriesToMap() {
        return entriesToCustomMap(HashMap::new);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code Map}
     * whose keys and values are taken from {@code Map.Entry} and combining them
     * using the provided {@code combiner} function to the input elements.
     *
     * <p>
     * There are no guarantees on the type or serializability of the {@code Map} returned;
     * if more control over the returned {@code Map} is required, use {@link #entriesToCustomMap(BinaryOperator, Supplier)}
     *
     * <p>
     * Returned {@code Map} is guaranteed to be modifiable. See {@link one.util.streamex.EntryStream#toMap()}.
     *
     * <p>If the mapped keys contains duplicates (according to {@link Object#equals(Object)}),
     * the value mapping function is applied to each equal element, and the
     * results are merged using the provided {@code combiner} function.
     *
     * @param <K>      the type of the map keys
     * @param <V>      the type of the map values
     * @param combiner a merge function, used to resolve collisions between
     *                 values associated with the same key, as supplied
     *                 to {@link Map#merge(Object, Object, BiFunction)}
     * @return {@code Collector} which collects elements into a {@code Map}
     * whose keys and values are taken from {@code Map.Entry} and combining them
     * using the {@code combiner} function
     * @throws NullPointerException if combiner is null.
     * @see #entriesToMap()
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     * @since 0.7.3
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> Collector<Entry<? extends K, ? extends V>, ?, Map<K, V>> entriesToMap(
            BinaryOperator<V> combiner) {
        return entriesToCustomMap(combiner, HashMap::new);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into
     * a result {@code Map} defined by {@code mapSupplier} function
     * whose keys and values are taken from {@code Map.Entry}.
     *
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     * @param <M> the type of the resulting {@code Map}
     * @param mapSupplier a function which returns a new, empty {@code Map} into
     *                    which the results will be inserted
     * @return {@code Collector} which collects elements into a {@code Map}
     * defined by {@code mapSupplier} function
     * whose keys and values are taken from {@code Map.Entry}
     * @throws IllegalStateException if this stream contains duplicate keys
     *                               (according to {@link Object#equals(Object)}).
     * @throws NullPointerException  if mapSupplier is null.
     * @throws NullPointerException  if entry value is null.
     * @see #entriesToCustomMap(BinaryOperator, Supplier)
     * @see Collector#of(Supplier, BiConsumer, BinaryOperator, Collector.Characteristics...)
     * @since 0.7.3
     */
    public static <
            K extends @Nullable Object,
            V extends @Nullable Object,
            M extends Map<K, V>> Collector<Entry<? extends K, ? extends V>, ?, M> entriesToCustomMap(
            Supplier<M> mapSupplier) {
        return Collector.of(mapSupplier,
                (m, entry) -> addToMap(m, entry.getKey(), Objects.requireNonNull(entry.getValue())),
                (m1, m2) -> {
                    m2.forEach((k, v) -> addToMap(m1, k, v));
                    return m1;
                }
        );
    }

    /**
     * Returns a {@code Collector} that accumulates elements into
     * a result {@code Map} defined by {@code mapSupplier} function
     * whose keys and values are taken from {@code Map.Entry} and combining them
     * using the provided {@code combiner} function to the input elements.
     *
     * <p>If the mapped keys contains duplicates (according to {@link Object#equals(Object)}),
     * the value mapping function is applied to each equal element, and the
     * results are merged using the provided {@code combiner} function.
     *
     * @param <K>         the type of the map keys
     * @param <V>         the type of the map values
     * @param <M>         the type of the resulting {@code Map}
     * @param combiner    a merge function, used to resolve collisions between
     *                    values associated with the same key, as supplied
     *                    to {@link Map#merge(Object, Object, BiFunction)}
     * @param mapSupplier a function which returns a new, empty {@code Map} into
     *                    which the results will be inserted
     * @return {@code Collector} which collects elements into a {@code Map}
     * whose keys and values are taken from {@code Map.Entry} and combining them
     * using the {@code combiner} function
     * @throws NullPointerException if {@code combiner} is null.
     * @throws NullPointerException if {@code mapSupplier} is null.
     * @see #entriesToCustomMap(Supplier)
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     * @since 0.7.3
     */
    public static <
            K extends @Nullable Object,
            V extends @Nullable Object,
            M extends Map<K, V>> Collector<Entry<? extends K, ? extends V>, ?, M> entriesToCustomMap(
            BinaryOperator<V> combiner, Supplier<M> mapSupplier) {
        Objects.requireNonNull(combiner);
        Objects.requireNonNull(mapSupplier);
        return Collectors.toMap(Entry::getKey, Entry::getValue, combiner, mapSupplier);
    }

    /**
     * Returns a {@code Collector} which counts a number of distinct values the
     * mapper function returns for the stream elements.
     * 
     * <p>
     * The operation performed by the returned collector is equivalent to
     * {@code stream.map(mapper).distinct().count()}. This collector is mostly
     * useful as a downstream collector.
     * 
     * @param <T> the type of the input elements
     * @param mapper a function which classifies input elements.
     * @return a collector which counts a number of distinct classes the mapper
     *         function returns for the stream elements.
     * @throws NullPointerException if mapper is null.
     */
    public static <T extends @Nullable Object> Collector<T, ?, Integer> distinctCount(Function<? super T, ?> mapper) {
        Objects.requireNonNull(mapper);
        return Collectors.collectingAndThen(Collectors.mapping(mapper, Collectors.toSet()), Set::size);
    }

    /**
     * Returns a {@code Collector} which collects into the {@link List} the
     * input elements for which given mapper function returns distinct results.
     *
     * <p>
     * For ordered source the order of collected elements is preserved. If the
     * same result is returned by mapper function for several elements, only the
     * first element is included into the resulting list.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.
     * 
     * <p>
     * The operation performed by the returned collector is equivalent to
     * {@code stream.distinct(mapper).toList()}, but may work faster.
     * 
     * @param <T> the type of the input elements
     * @param mapper a function which classifies input elements.
     * @return a collector which collects distinct elements to the {@code List}.
     * @throws NullPointerException if mapper is null.
     * @since 0.3.8
     */
    public static <T extends @Nullable Object> Collector<T, ?, List<T>> distinctBy(Function<? super T, ?> mapper) {
        Objects.requireNonNull(mapper);
        return Collector.<T, Map<Object, T>, List<T>>of(LinkedHashMap::new, (map, t) -> map.putIfAbsent(mapper.apply(
            t), t), (m1, m2) -> {
                for (Entry<Object, T> e : m2.entrySet()) {
                    m1.putIfAbsent(e.getKey(), e.getValue());
                }
                return m1;
            }, map -> new ArrayList<>(map.values()));
    }

    /**
     * Returns a {@code Collector} accepting elements of type {@code T} that
     * counts the number of input elements and returns result as {@code Integer}
     * . If no elements are present, the result is 0.
     *
     * @param <T> the type of the input elements
     * @return a {@code Collector} that counts the input elements
     * @since 0.3.3
     * @see Collectors#counting()
     */
    public static <T extends @Nullable Object> Collector<T, ?, Integer> countingInt() {
        return PartialCollector.intSum().asRef((acc, t) -> acc[0]++);
    }

    /**
     * Returns a {@code Collector} which aggregates the results of two supplied
     * collectors using the supplied finisher function.
     * 
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> if both downstream collectors are short-circuiting. The
     * collection might stop when both downstream collectors report that the
     * collection is complete.
     * 
     * <p>
     * This collector is similar to the {@code teeing} collector available since
     * JDK 12. The only difference is that this collector correctly combines 
     * short-circuiting collectors.
     * 
     * @param <T> the type of the input elements
     * @param <A1> the intermediate accumulation type of the first collector
     * @param <A2> the intermediate accumulation type of the second collector
     * @param <R1> the result type of the first collector
     * @param <R2> the result type of the second collector
     * @param <R> the final result type
     * @param c1 the first collector
     * @param c2 the second collector
     * @param finisher the function which merges two results into the single
     *        one.
     * @return a {@code Collector} which aggregates the results of two supplied
     *         collectors.
     * @throws NullPointerException if c1 is null, or c2 is null, or finisher is null.
     */
    public static <
            T extends @Nullable Object,
            A1 extends @Nullable Object,
            A2 extends @Nullable Object,
            R1 extends @Nullable Object,
            R2 extends @Nullable Object,
            R extends @Nullable Object> Collector<T, ?, R> pairing(
            Collector<? super T, A1, R1> c1,
            Collector<? super T, A2, R2> c2,
            BiFunction<? super R1, ? super R2, ? extends R> finisher) {
        Objects.requireNonNull(finisher);
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
        Function<PairBox<A1, A2>, R> resFinisher = acc -> {
            R1 r1 = c1.finisher().apply(acc.a);
            R2 r2 = c2.finisher().apply(acc.b);
            return finisher.apply(r1, r2);
        };
        Predicate<A1> c1Finished = finished(c1);
        Predicate<A2> c2Finished = finished(c2);
        if (c1Finished != null && c2Finished != null) {
            Predicate<PairBox<A1, A2>> finished = acc -> c1Finished.test(acc.a) && c2Finished.test(acc.b);
            return new CancellableCollectorImpl<>(supplier, accumulator, combiner, resFinisher, finished, c);
        }
        return Collector.of(supplier, accumulator, combiner, resFinisher, c.toArray(new Characteristics[0]));
    }

    /**
     * Returns a {@code Collector} which finds the minimal and maximal element
     * according to the supplied comparator, then applies finisher function to
     * them producing the final result.
     * 
     * <p>
     * This collector produces stable result for ordered stream: if several
     * minimal or maximal elements appear, the collector always selects the
     * first encountered.
     * 
     * <p>
     * If there are no input elements, the finisher method is not called and
     * empty {@code Optional} is returned. Otherwise the finisher result is
     * wrapped into {@code Optional}.
     *
     * @param <T> the type of the input elements
     * @param <R> the type of the result wrapped into {@code Optional}
     * @param comparator comparator which is used to find minimal and maximal
     *        element
     * @param finisher a {@link BiFunction} which takes minimal and maximal
     *        element and produces the final result.
     * @return a {@code Collector} which finds minimal and maximal elements.
     * @throws NullPointerException if comparator is null, finisher is null,
     * or finisher returns null.
     */
    public static <T extends @Nullable Object, R> Collector<T, ?, Optional<R>> minMax(Comparator<? super T> comparator,
            BiFunction<? super T, ? super T, ? extends R> finisher) {
        Objects.requireNonNull(finisher);
        return pairing(Collectors.minBy(comparator), Collectors.maxBy(comparator),
            (min, max) -> min.isPresent() ? Optional.of(finisher.apply(min.get(), max.get())) : Optional.empty());
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the
     * specified {@link Comparator}. The found elements are reduced using the
     * specified downstream {@code Collector}.
     *
     * @param <T> the type of the input elements
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param comparator a {@code Comparator} to compare the elements
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the maximal elements.
     * @throws NullPointerException if comparator is null, or downstream is null.
     * @see #maxAll(Comparator)
     * @see #maxAll(Collector)
     * @see #maxAll()
     */
    public static <
            T extends @Nullable Object,
            A extends @Nullable Object,
            D extends @Nullable Object> Collector<T, ?, D> maxAll(
            Comparator<? super T> comparator,
            Collector<? super T, A, D> downstream) {
        Objects.requireNonNull(comparator);
        Supplier<A> downstreamSupplier = downstream.supplier();
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BinaryOperator<A> downstreamCombiner = downstream.combiner();
        Supplier<PairBox<A, T>> supplier = () -> new PairBox<>(downstreamSupplier.get(), none());
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

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the
     * specified {@link Comparator}. The found elements are collected to
     * {@link List}.
     *
     * @param <T> the type of the input elements
     * @param comparator a {@code Comparator} to compare the elements
     * @return a {@code Collector} which finds all the maximal elements and
     *         collects them to the {@code List}.
     * @throws NullPointerException if comparator is null.
     * @see #maxAll(Comparator, Collector)
     * @see #maxAll()
     */
    public static <T extends @Nullable Object> Collector<T, ?, List<T>> maxAll(Comparator<? super T> comparator) {
        return maxAll(comparator, Collectors.toList());
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the natural
     * order. The found elements are reduced using the specified downstream
     * {@code Collector}.
     *
     * @param <T> the type of the input elements
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the maximal elements.
     * @throws NullPointerException if downstream is null.
     * @see #maxAll(Comparator, Collector)
     * @see #maxAll(Comparator)
     * @see #maxAll()
     */
    public static <
            T extends Comparable<? super T>,
            A extends @Nullable Object,
            D extends @Nullable Object> Collector<T, ?, D> maxAll(Collector<T, A, D> downstream) {
        return maxAll(Comparator.<T>naturalOrder(), downstream);
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the natural
     * order. The found elements are collected to {@link List}.
     *
     * @param <T> the type of the input elements
     * @return a {@code Collector} which finds all the maximal elements and
     *         collects them to the {@code List}.
     * @see #maxAll(Comparator)
     * @see #maxAll(Collector)
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, List<T>> maxAll() {
        return maxAll(Comparator.naturalOrder(), Collectors.toList());
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the
     * specified {@link Comparator}. The found elements are reduced using the
     * specified downstream {@code Collector}.
     *
     * @param <T> the type of the input elements
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param comparator a {@code Comparator} to compare the elements
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the minimal elements.
     * @throws NullPointerException if comparator is null, or downstream is null.
     * @see #minAll(Comparator)
     * @see #minAll(Collector)
     * @see #minAll()
     */
    public static <
            T extends @Nullable Object,
            A extends @Nullable Object,
            D extends @Nullable Object> Collector<T, ?, D> minAll(
            Comparator<? super T> comparator, Collector<T, A, D> downstream) {
        return maxAll(comparator.reversed(), downstream);
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the
     * specified {@link Comparator}. The found elements are collected to
     * {@link List}.
     *
     * @param <T> the type of the input elements
     * @param comparator a {@code Comparator} to compare the elements
     * @return a {@code Collector} which finds all the minimal elements and
     *         collects them to the {@code List}.
     * @throws NullPointerException if comparator is null.
     * @see #minAll(Comparator, Collector)
     * @see #minAll()
     */
    public static <T extends @Nullable Object> Collector<T, ?, List<T>> minAll(Comparator<? super T> comparator) {
        return maxAll(comparator.reversed(), Collectors.toList());
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the natural
     * order. The found elements are reduced using the specified downstream
     * {@code Collector}.
     *
     * @param <T> the type of the input elements
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the minimal elements.
     * @throws NullPointerException if downstream is null.
     * @see #minAll(Comparator, Collector)
     * @see #minAll(Comparator)
     * @see #minAll()
     */
    public static <T extends Comparable<? super T>,
            A extends @Nullable Object,
            D extends @Nullable Object> Collector<T, ?, D> minAll(Collector<T, A, D> downstream) {
        return maxAll(Comparator.<T>reverseOrder(), downstream);
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the natural
     * order. The found elements are collected to {@link List}.
     *
     * @param <T> the type of the input elements
     * @return a {@code Collector} which finds all the minimal elements and
     *         collects them to the {@code List}.
     * @see #minAll(Comparator)
     * @see #minAll(Collector)
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, List<T>> minAll() {
        return maxAll(Comparator.reverseOrder(), Collectors.toList());
    }

    /**
     * Returns a {@code Collector} which collects the stream element if stream
     * contains exactly one element.
     * 
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>.
     * 
     * @param <T> the type of the input elements
     * @return a collector which returns an {@link Optional} describing the only
     *         element of the stream. For empty stream or stream containing more
     *         than one element an empty {@code Optional} is returned.
     * @throws NullPointerException if the only stream element is null.
     * @since 0.4.0
     */
    public static <T> Collector<T, ?, Optional<T>> onlyOne() {
        return new CancellableCollectorImpl<T, Box<Optional<T>>, Optional<T>>(Box::new, (box,
                t) -> box.a = box.a == null ? Optional.of(t) : Optional.empty(), (box1, box2) -> box1.a == null ? box2
                        : box2.a == null ? box1 : new Box<>(Optional.empty()), box -> box.a == null ? Optional.empty()
                                : box.a, box -> box.a != null && !box.a.isPresent(), UNORDERED_CHARACTERISTICS);
    }

    /**
     * Returns a {@code Collector} which collects the stream element satisfying the predicate
     * if there is only one such element.
     *
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>.
     *
     * @param predicate a predicate to be applied to the stream elements
     * @param <T> the type of the input elements
     * @return a collector which returns an {@link Optional} describing the only
     *         element of the stream satisfying the predicate. If stream contains no elements satisfying the predicate,
     *         or more than one such element, an empty {@code Optional} is returned.
     * @throws NullPointerException if predicate is null or the only stream element is null.
     * @since 0.6.7
     */
    public static <T> Collector<T, ?, Optional<T>> onlyOne(Predicate<? super T> predicate) {
        return filtering(predicate, onlyOne());
    }

    /**
     * Returns a {@code Collector} which collects only the first stream element
     * if any.
     * 
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>.
     * 
     * <p>
     * The operation performed by the returned collector is equivalent to
     * {@code stream.findFirst()}. This collector is mostly useful as a
     * downstream collector.
     * 
     * @param <T> the type of the input elements
     * @return a collector which returns an {@link Optional} which describes the
     *         first element of the stream. For empty stream an empty
     *         {@code Optional} is returned.
     * @throws NullPointerException if the first stream element is null.
     */
    public static <T> Collector<T, ?, Optional<T>> first() {
        return new CancellableCollectorImpl<>(() -> new Box<T>(none()), (box, t) -> {
            if (box.a == NONE)
                box.a = t;
        }, (box1, box2) -> box1.a == NONE ? box2 : box1, box -> box.a == NONE ? Optional.empty() : Optional.of(box.a),
                box -> box.a != NONE, NO_CHARACTERISTICS);
    }

    /**
     * Returns a {@code Collector} which collects only the last stream element
     * if any.
     * 
     * @param <T> the type of the input elements
     * @return a collector which returns an {@link Optional} which describes the
     *         last element of the stream. For empty stream an empty
     *         {@code Optional} is returned.
     * @throws NullPointerException if the last stream element is null.
     */
    public static <T> Collector<T, ?, Optional<T>> last() {
        return Collector.of(() -> new Box<T>(none()), (box, t) -> box.a = t,
            (box1, box2) -> box2.a == NONE ? box1 : box2, box -> box.a == NONE ? Optional.empty() : Optional.of(box.a));
    }

    /**
     * Returns a {@code Collector} which collects at most specified number of
     * the first stream elements into the {@link List}.
     *
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.
     * 
     * <p>
     * The operation performed by the returned collector is equivalent to
     * {@code stream.limit(n).collect(Collectors.toList())}. This collector is
     * mostly useful as a downstream collector.
     * 
     * @param <T> the type of the input elements
     * @param n maximum number of stream elements to preserve
     * @return a collector which returns a {@code List} containing the first n
     *         stream elements or less if the stream was shorter.
     */
    public static <T extends @Nullable Object> Collector<T, ?, List<T>> head(int n) {
        if (n <= 0)
            return empty();
        return new CancellableCollectorImpl<>(ArrayList::new, (acc, t) -> {
            if (acc.size() < n)
                acc.add(t);
        }, (acc1, acc2) -> {
            acc1.addAll(acc2.subList(0, Math.min(acc2.size(), n - acc1.size())));
            return acc1;
        }, Function.identity(), acc -> acc.size() >= n, ID_CHARACTERISTICS);
    }

    /**
     * Returns a {@code Collector} which collects at most specified number of
     * the last stream elements into the {@link List}.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.
     * 
     * <p>
     * When supplied {@code n} is less or equal to zero, this method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> which ignores the input and produces an empty list.
     * 
     * @param <T> the type of the input elements
     * @param n maximum number of stream elements to preserve
     * @return a collector which returns a {@code List} containing the last n
     *         stream elements or less if the stream was shorter.
     */
    public static <T extends @Nullable Object> Collector<T, ?, List<T>> tail(int n) {
        if (n <= 0)
            return empty();
        return Collector.<T, Deque<T>, List<T>>of(ArrayDeque::new, (acc, t) -> {
            if (acc.size() == n)
                acc.pollFirst();
            acc.addLast(t);
        }, (acc1, acc2) -> {
            while (acc2.size() < n && !acc1.isEmpty()) {
                acc2.addFirst(acc1.pollLast());
            }
            return acc2;
        }, ArrayList::new);
    }

    /**
     * Returns a {@code Collector} which collects at most specified number of
     * the greatest stream elements according to the specified
     * {@link Comparator} into the {@link List}. The resulting {@code List} is
     * sorted in comparator reverse order (greatest element is the first). The
     * order of equal elements is the same as in the input stream.
     * 
     * <p>
     * The operation performed by the returned collector is equivalent to
     * {@code stream.sorted(comparator.reversed()).limit(n).collect(Collectors.toList())}
     * , but usually performed much faster if {@code n} is much less than the
     * stream size.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.
     * 
     * <p>
     * When supplied {@code n} is less or equal to zero, this method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> which ignores the input and produces an empty list.
     * 
     * @param <T> the type of the input elements
     * @param comparator the comparator to compare the elements by
     * @param n maximum number of stream elements to preserve
     * @return a collector which returns a {@code List} containing the greatest
     *         n stream elements or less if the stream was shorter.
     * @throws NullPointerException if comparator is null.
     */
    public static <T extends @Nullable Object> Collector<T, ?, List<T>> greatest(Comparator<? super T> comparator, int n) {
        return least(comparator.reversed(), n);
    }

    /**
     * Returns a {@code Collector} which collects at most specified number of
     * the greatest stream elements according to the natural order into the
     * {@link List}. The resulting {@code List} is sorted in reverse order
     * (greatest element is the first). The order of equal elements is the same
     * as in the input stream.
     * 
     * <p>
     * The operation performed by the returned collector is equivalent to
     * {@code stream.sorted(Comparator.reverseOrder()).limit(n).collect(Collectors.toList())}
     * , but usually performed much faster if {@code n} is much less than the
     * stream size.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.
     * 
     * <p>
     * When supplied {@code n} is less or equal to zero, this method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> which ignores the input and produces an empty list.
     * 
     * @param <T> the type of the input elements
     * @param n maximum number of stream elements to preserve
     * @return a collector which returns a {@code List} containing the greatest
     *         n stream elements or less if the stream was shorter.
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, List<T>> greatest(int n) {
        return least(Comparator.reverseOrder(), n);
    }

    /**
     * Returns a {@code Collector} which collects at most specified number of
     * the least stream elements according to the specified {@link Comparator}
     * into the {@link List}. The resulting {@code List} is sorted in comparator
     * order (least element is the first). The order of equal elements is the
     * same as in the input stream.
     * 
     * <p>
     * The operation performed by the returned collector is equivalent to
     * {@code stream.sorted(comparator).limit(n).collect(Collectors.toList())},
     * but usually performed much faster if {@code n} is much less than the
     * stream size.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.
     * 
     * <p>
     * When supplied {@code n} is less or equal to zero, this method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> which ignores the input and produces an empty list.
     * 
     * @param <T> the type of the input elements
     * @param comparator the comparator to compare the elements by
     * @param n maximum number of stream elements to preserve
     * @return a collector which returns a {@code List} containing the least n
     *         stream elements or less if the stream was shorter.
     * @throws NullPointerException if comparator is null.
     */
    public static <T extends @Nullable Object> Collector<T, ?, List<T>> least(Comparator<? super T> comparator, int n) {
        Objects.requireNonNull(comparator);
        if (n <= 0)
            return empty();
        if (n == 1) {
            return Collector.of(() -> new Box<T>(none()), (box, t) -> {
                if (box.a == NONE || comparator.compare(t, box.a) < 0)
                    box.a = t;
            }, (box1, box2) -> (box2.a != NONE && (box1.a == NONE || comparator.compare(box2.a, box1.a) < 0)) ? box2
                    : box1, box -> box.a == NONE ? new ArrayList<>() : new ArrayList<>(Collections.singleton(box.a)));
        }
        if (n >= Integer.MAX_VALUE / 2)
            return collectingAndThen(Collectors.toList(), list -> {
                list.sort(comparator);
                if (list.size() <= n)
                    return list;
                return new ArrayList<>(list.subList(0, n));
            });
        return Collector.<T, Limiter<T>, List<T>>of(() -> new Limiter<>(n, comparator), Limiter::put, Limiter::putAll,
            pq -> {
                pq.sort();
                return new ArrayList<>(pq);
            });
    }

    /**
     * Returns a {@code Collector} which collects at most specified number of
     * the least stream elements according to the natural order into the
     * {@link List}. The resulting {@code List} is sorted in natural order
     * (least element is the first). The order of equal elements is the same as
     * in the input stream.
     * 
     * <p>
     * The operation performed by the returned collector is equivalent to
     * {@code stream.sorted().limit(n).collect(Collectors.toList())}, but
     * usually performed much faster if {@code n} is much less than the stream
     * size.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.
     * 
     * <p>
     * When supplied {@code n} is less or equal to zero, this method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> which ignores the input and produces an empty list.
     * 
     * @param <T> the type of the input elements
     * @param n maximum number of stream elements to preserve
     * @return a collector which returns a {@code List} containing the least n
     *         stream elements or less if the stream was shorter.
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, List<T>> least(int n) {
        return least(Comparator.naturalOrder(), n);
    }

    /**
     * Returns a {@code Collector} which finds the index of the minimal stream
     * element according to the specified {@link Comparator}. If there are
     * several minimal elements, the index of the first one is returned.
     *
     * @param <T> the type of the input elements
     * @param comparator a {@code Comparator} to compare the elements
     * @return a {@code Collector} which finds the index of the minimal element.
     * @throws NullPointerException if comparator is null.
     * @see #minIndex()
     * @since 0.3.5
     */
    public static <T extends @Nullable Object> Collector<T, ?, OptionalLong> minIndex(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        class Container {
            T value;
            long count = 0;
            long index = -1;
        }

        return Collector.of(Container::new, (c, t) -> {
            if (c.index == -1 || comparator.compare(c.value, t) > 0) {
                c.value = t;
                c.index = c.count;
            }
            c.count++;
        }, (c1, c2) -> {
            if (c1.index == -1 || (c2.index != -1 && comparator.compare(c1.value, c2.value) > 0)) {
                c2.index += c1.count;
                c2.count += c1.count;
                return c2;
            }
            c1.count += c2.count;
            return c1;
        }, c -> c.index == -1 ? OptionalLong.empty() : OptionalLong.of(c.index));
    }

    /**
     * Returns a {@code Collector} which finds the index of the minimal stream
     * element according to the elements natural order. If there are several
     * minimal elements, the index of the first one is returned.
     *
     * @param <T> the type of the input elements
     * @return a {@code Collector} which finds the index of the minimal element.
     * @see #minIndex(Comparator)
     * @since 0.3.5
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, OptionalLong> minIndex() {
        return minIndex(Comparator.naturalOrder());
    }

    /**
     * Returns a {@code Collector} which finds the index of the maximal stream
     * element according to the specified {@link Comparator}. If there are
     * several maximal elements, the index of the first one is returned.
     *
     * @param <T> the type of the input elements
     * @param comparator a {@code Comparator} to compare the elements
     * @return a {@code Collector} which finds the index of the maximal element.
     * @throws NullPointerException if comparator is null.
     * @see #maxIndex()
     * @since 0.3.5
     */
    public static <T extends @Nullable Object> Collector<T, ?, OptionalLong> maxIndex(Comparator<? super T> comparator) {
        return minIndex(comparator.reversed());
    }

    /**
     * Returns a {@code Collector} which finds the index of the maximal stream
     * element according to the elements natural order. If there are several
     * maximal elements, the index of the first one is returned.
     *
     * @param <T> the type of the input elements
     * @return a {@code Collector} which finds the index of the maximal element.
     * @see #maxIndex(Comparator)
     * @since 0.3.5
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, OptionalLong> maxIndex() {
        return minIndex(Comparator.reverseOrder());
    }

    /**
     * Returns a {@code Collector} implementing a cascaded "group by" operation
     * on input elements of type {@code T}, for classification function which
     * maps input elements to the enum values. The downstream reduction for
     * repeating keys is performed using the specified downstream
     * {@code Collector}.
     *
     * <p>
     * Unlike the {@link Collectors#groupingBy(Function, Collector)} collector
     * this collector produces an {@link EnumMap} which contains all possible
     * keys including keys which were never returned by the classification
     * function. These keys are mapped to the default collector value which is
     * equivalent to collecting an empty stream with the same collector.
     * 
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> if the downstream collector is short-circuiting. The
     * collection might stop when for every possible enum key the downstream
     * collection is known to be finished.
     *
     * @param <T> the type of the input elements
     * @param <K> the type of the enum values returned by the classifier
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param enumClass the class of enum values returned by the classifier
     * @param classifier a classifier function mapping input elements to enum
     *        values
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} implementing the cascaded group-by operation
     * @throws NullPointerException if enumClass is null, classifier is null, or downstream is null.
     * @see Collectors#groupingBy(Function, Collector)
     * @see #groupingBy(Function, Set, Supplier, Collector)
     * @since 0.3.7
     */
    public static <
            T extends @Nullable Object,
            K extends Enum<K>,
            A extends @Nullable Object,
            D extends @Nullable Object> Collector<T, ?, EnumMap<K, D>> groupingByEnum(
            Class<K> enumClass,
            Function<? super T, K> classifier,
            Collector<? super T, A, D> downstream) {
        return groupingBy(classifier, EnumSet.allOf(enumClass), () -> new EnumMap<>(enumClass), downstream);
    }

    /**
     * Returns a {@code Collector} implementing a cascaded "group by" operation
     * on input elements of type {@code T}, grouping elements according to a
     * classification function, and then performing a reduction operation on the
     * values associated with a given key using the specified downstream
     * {@code Collector}.
     *
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} returned.
     *
     * <p>
     * The main difference of this collector from
     * {@link Collectors#groupingBy(Function, Collector)} is that it accepts
     * additional domain parameter which is the {@code Set} of all possible map
     * keys. If the mapper function produces the key out of domain, an
     * {@code IllegalStateException} will occur. If the mapper function does not
     * produce some of domain keys at all, they are also added to the result.
     * These keys are mapped to the default collector value which is equivalent
     * to collecting an empty stream with the same collector.
     * 
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> if the downstream collector is short-circuiting. The
     * collection might stop when for every possible key from the domain the
     * downstream collection is known to be finished.
     *
     * @param <T> the type of the input elements
     * @param <K> the type of the keys
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param classifier a classifier function mapping input elements to keys
     * @param domain a domain of all possible key values
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} implementing the cascaded group-by operation
     *         with given domain
     * @throws NullPointerException if classifier is null, domain is null, or downstream is null.
     *
     * @see #groupingBy(Function, Set, Supplier, Collector)
     * @see #groupingByEnum(Class, Function, Collector)
     * @since 0.4.0
     */
    public static <
            T extends @Nullable Object,
            K,
            D extends @Nullable Object,
            A extends @Nullable Object> Collector<T, ?, Map<K, D>> groupingBy(Function<? super T, ? extends K> classifier,
            Set<K> domain, Collector<? super T, A, D> downstream) {
        return groupingBy(classifier, domain, HashMap::new, downstream);
    }

    /**
     * Returns a {@code Collector} implementing a cascaded "group by" operation
     * on input elements of type {@code T}, grouping elements according to a
     * classification function, and then performing a reduction operation on the
     * values associated with a given key using the specified downstream
     * {@code Collector}. The {@code Map} produced by the Collector is created
     * with the supplied factory function.
     *
     * <p>
     * The main difference of this collector from
     * {@link Collectors#groupingBy(Function, Supplier, Collector)} is that it
     * accepts additional domain parameter which is the {@code Set} of all
     * possible map keys. If the mapper function produces the key out of domain,
     * an {@code IllegalStateException} will occur. If the mapper function does
     * not produce some of domain keys at all, they are also added to the
     * result. These keys are mapped to the default collector value which is
     * equivalent to collecting an empty stream with the same collector.
     * 
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> if the downstream collector is short-circuiting. The
     * collection might stop when for every possible key from the domain the
     * downstream collection is known to be finished.
     *
     * @param <T> the type of the input elements
     * @param <K> the type of the keys
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param <M> the type of the resulting {@code Map}
     * @param classifier a classifier function mapping input elements to keys
     * @param domain a domain of all possible key values
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @param mapFactory a function which, when called, produces a new empty
     *        {@code Map} of the desired type
     * @return a {@code Collector} implementing the cascaded group-by operation
     *         with given domain
     * @throws NullPointerException if classifier is null, domain is null, mapFactory is null, or downstream is null.
     *
     * @see #groupingBy(Function, Set, Collector)
     * @see #groupingByEnum(Class, Function, Collector)
     * @since 0.4.0
     */
    public static <
            T extends @Nullable Object,
            K,
            D extends @Nullable Object,
            A extends @Nullable Object,
            M extends Map<K, D>> Collector<T, ?, M> groupingBy(
            Function<? super T, ? extends K> classifier,
            Set<K> domain,
            Supplier<M> mapFactory,
            Collector<? super T, A, D> downstream) {
        Objects.requireNonNull(classifier);
        Objects.requireNonNull(domain);
        Objects.requireNonNull(mapFactory);
        Supplier<A> downstreamSupplier = downstream.supplier();
        Collector<T, ?, M> groupingBy;
        Function<K, A> supplier = k -> {
            if (!domain.contains(k))
                throw new IllegalStateException("Classifier returned value '" + k + "' which is out of domain");
            return downstreamSupplier.get();
        };
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BiConsumer<Map<K, A>, T> accumulator = (m, t) -> {
            K key = Objects.requireNonNull(classifier.apply(t));
            A container = m.computeIfAbsent(key, supplier);
            downstreamAccumulator.accept(container, t);
        };
        PartialCollector<Map<K, A>, M> partial = PartialCollector.grouping(mapFactory, downstream);
        Predicate<A> downstreamFinished = finished(downstream);
        if (downstreamFinished != null) {
            int size = domain.size();
            groupingBy = partial.asCancellable(accumulator, map -> {
                if (map.size() < size)
                    return false;
                for (A container : map.values()) {
                    if (!downstreamFinished.test(container))
                        return false;
                }
                return true;
            });
        } else {
            groupingBy = partial.asRef(accumulator);
        }
        return collectingAndThen(groupingBy, map -> {
            Function<A, D> finisher = downstream.finisher();
            domain.forEach(key -> map.computeIfAbsent(key, k -> finisher.apply(downstreamSupplier.get())));
            return map;
        });
    }

    /**
     * Returns a {@code Collector} which collects the intersection of the input
     * collections into the newly-created {@link Set}.
     *
     * <p>
     * The returned collector produces an empty set if the input is empty or
     * intersection of the input collections is empty.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Set} returned.
     *
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>: it may not process all the elements if the resulting
     * intersection is empty.
     * 
     * @param <T> the type of the elements in the input collections
     * @param <S> the type of the input collections
     * @return a {@code Collector} which finds all the minimal elements and
     *         collects them to the {@code List}.
     * @since 0.4.0
     */
    public static <T extends @Nullable Object, S extends Collection<T>> Collector<S, ?, Set<T>> intersecting() {
        return new CancellableCollectorImpl<S, Box<Set<T>>, Set<T>>(Box::new, (b, t) -> {
            if (b.a == null) {
                b.a = new HashSet<>(t);
            } else {
                b.a.retainAll(t);
            }
        }, (b1, b2) -> {
            if (b1.a == null)
                return b2;
            if (b2.a != null)
                b1.a.retainAll(b2.a);
            return b1;
        }, b -> b.a == null ? Collections.emptySet() : b.a, b -> b.a != null && b.a.isEmpty(),
                UNORDERED_CHARACTERISTICS);
    }

    /**
     * Adapts a {@code Collector} to perform an additional finishing
     * transformation.
     * 
     * <p>
     * Unlike {@link Collectors#collectingAndThen(Collector, Function)} this
     * method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> if the downstream collector is short-circuiting.
     *
     * @param <T> the type of the input elements
     * @param <A> intermediate accumulation type of the downstream collector
     * @param <R> result type of the downstream collector
     * @param <RR> result type of the resulting collector
     * @param downstream a collector
     * @param finisher a function to be applied to the final result of the
     *        downstream collector
     * @return a collector which performs the action of the downstream
     *         collector, followed by an additional finishing step
     * @throws NullPointerException if downstream is null, or finisher is null.
     * @see Collectors#collectingAndThen(Collector, Function)
     * @since 0.4.0
     */
    public static <
            T extends @Nullable Object,
            A extends @Nullable Object,
            R extends @Nullable Object,
            RR extends @Nullable Object> Collector<T, A, RR> collectingAndThen(Collector<T, A, R> downstream,
            Function<R, RR> finisher) {
        Predicate<A> finished = finished(downstream);
        if (finished != null) {
            return new CancellableCollectorImpl<>(downstream.supplier(), downstream.accumulator(), downstream
                    .combiner(), downstream.finisher().andThen(finisher), finished, downstream.characteristics()
                            .contains(Characteristics.UNORDERED) ? UNORDERED_CHARACTERISTICS : NO_CHARACTERISTICS);
        }
        return Collectors.collectingAndThen(downstream, finisher);
    }

    /**
     * Returns a {@code Collector} which partitions the input elements according
     * to a {@code Predicate}, reduces the values in each partition according to
     * another {@code Collector}, and organizes them into a
     * {@code Map<Boolean, D>} whose values are the result of the downstream
     * reduction.
     * 
     * <p>
     * Unlike {@link Collectors#partitioningBy(Predicate, Collector)} this
     * method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> if the downstream collector is short-circuiting.
     * 
     * @param <T> the type of the input elements
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param predicate a predicate used for classifying input elements
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} implementing the cascaded partitioning
     *         operation
     * @throws NullPointerException if predicate is null, or downstream is null.
     * @since 0.4.0
     * @see Collectors#partitioningBy(Predicate, Collector)
     */
    public static <
            T extends @Nullable Object,
            D extends @Nullable Object,
            A extends @Nullable Object> Collector<T, ?, Map<Boolean, D>> partitioningBy(Predicate<? super T> predicate,
            Collector<? super T, A, D> downstream) {
        Objects.requireNonNull(predicate);
        Predicate<A> finished = finished(downstream);
        if (finished != null) {
            BiConsumer<A, ? super T> accumulator = downstream.accumulator();
            return BooleanMap.partialCollector(downstream).asCancellable((map, t) -> accumulator.accept(predicate.test(
                t) ? map.trueValue : map.falseValue, t), map -> finished.test(map.trueValue) && finished.test(
                    map.falseValue));
        }
        return Collectors.partitioningBy(predicate, downstream);
    }

    /**
     * Adapts a {@code Collector} accepting elements of type {@code U} to one
     * accepting elements of type {@code T} by applying a mapping function to
     * each input element before accumulation.
     *
     * <p>
     * Unlike {@link Collectors#mapping(Function, Collector)} this method
     * returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> if the downstream collector is short-circuiting.
     * 
     * @param <T> the type of the input elements
     * @param <U> type of elements accepted by downstream collector
     * @param <A> intermediate accumulation type of the downstream collector
     * @param <R> result type of collector
     * @param mapper a function to be applied to the input elements
     * @param downstream a collector which will accept mapped values
     * @return a collector which applies the mapping function to the input
     *         elements and provides the mapped results to the downstream
     *         collector
     * @throws NullPointerException if mapper is null, or downstream is null.
     * @see Collectors#mapping(Function, Collector)
     * @since 0.4.0
     */
    public static <
            T extends @Nullable Object,
            U extends @Nullable Object,
            A extends @Nullable Object,
            R extends @Nullable Object> Collector<T, ?, R> mapping(Function<? super T, ? extends U> mapper,
            Collector<? super U, A, R> downstream) {
        Objects.requireNonNull(mapper);
        Predicate<A> finished = finished(downstream);
        if (finished != null) {
            BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
            return new CancellableCollectorImpl<>(downstream.supplier(), (acc, t) -> {
                if (!finished.test(acc))
                    downstreamAccumulator.accept(acc, mapper.apply(t));
            }, downstream.combiner(), downstream.finisher(), finished, downstream.characteristics());
        }
        return Collectors.mapping(mapper, downstream);
    }

    /**
     * Returns a collector which collects input elements to the new {@code List}
     * transforming them with the supplied function beforehand.
     * 
     * <p>
     * This method behaves like
     * {@code Collectors.mapping(mapper, Collectors.toList())}.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.
     * 
     * @param <T> the type of the input elements
     * @param <U> the resulting type of the mapper function
     * @param mapper a function to be applied to the input elements
     * @return a collector which applies the mapping function to the input
     *         elements and collects the mapped results to the {@code List}
     * @throws NullPointerException if mapper is null.
     * @see #mapping(Function, Collector)
     * @since 0.6.0
     */
    public static <
            T extends @Nullable Object,
            U extends @Nullable Object> Collector<T, ?, List<U>> mapping(Function<? super T, ? extends U> mapper) {
        return Collectors.mapping(mapper, Collectors.toList());
    }

    /**
     * Adapts a {@code Collector} accepting elements of type {@code U} to one
     * accepting elements of type {@code T} by applying a flat mapping function
     * to each input element before accumulation. The flat mapping function maps
     * an input element to a {@link Stream stream} covering zero or more output
     * elements that are then accumulated downstream. Each mapped stream is
     * {@link java.util.stream.BaseStream#close() closed} after its contents
     * have been placed downstream. (If a mapped stream is {@code null} an empty
     * stream is used, instead.)
     * 
     * <p>
     * This method is similar to {@code Collectors.flatMapping} method which
     * appears in JDK 9. However when downstream collector is
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting</a>
     * , this method will also return a short-circuiting collector.
     * 
     * @param <T> the type of the input elements
     * @param <U> type of elements accepted by downstream collector
     * @param <A> intermediate accumulation type of the downstream collector
     * @param <R> result type of collector
     * @param mapper a function to be applied to the input elements, which
     *        returns a stream of results
     * @param downstream a collector which will receive the elements of the
     *        stream returned by mapper
     * @return a collector which applies the mapping function to the input
     *         elements and provides the flat mapped results to the downstream
     *         collector
     * @throws NullPointerException if mapper is null, or downstream is null.
     * @since 0.4.1
     */
    public static <
            T extends @Nullable Object,
            U extends @Nullable Object,
            A extends @Nullable Object,
            R extends @Nullable Object> Collector<T, ?, R> flatMapping(
            Function<? super T, ? extends @Nullable Stream<? extends U>> mapper,
            Collector<? super U, A, R> downstream) {
        Objects.requireNonNull(mapper);
        BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
        Predicate<A> finished = finished(downstream);
        if (finished != null) {
            return new CancellableCollectorImpl<>(downstream.supplier(), (acc, t) -> {
                if (finished.test(acc))
                    return;
                try (Stream<? extends U> stream = mapper.apply(t)) {
                    if (stream != null) {
                        stream.spliterator().forEachRemaining(u -> {
                            downstreamAccumulator.accept(acc, u);
                            if (finished.test(acc))
                                throw new CancelException();
                        });
                    }
                } catch (CancelException ex) {
                    // ignore
                }
            }, downstream.combiner(), downstream.finisher(), finished, downstream.characteristics());
        }
        return Collector.of(downstream.supplier(), (acc, t) -> {
            try (Stream<? extends U> stream = mapper.apply(t)) {
                if (stream != null) {
                    stream.spliterator().forEachRemaining(u -> downstreamAccumulator.accept(acc, u));
                }
            }
        }, downstream.combiner(), downstream.finisher(), downstream.characteristics().toArray(new Characteristics[0]));
    }

    /**
     * Returns a collector which launches a flat mapping function for each input
     * element and collects the elements of the resulting streams to the flat
     * {@code List}. Each mapped stream is
     * {@link java.util.stream.BaseStream#close() closed} after its contents
     * have been placed downstream. (If a mapped stream is {@code null} an empty
     * stream is used, instead.)
     * 
     * <p>
     * This method behaves like {@code flatMapping(mapper, Collectors.toList())}
     * .
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.
     * 
     * @param <T> the type of the input elements
     * @param <U> type of the resulting elements
     * @param mapper a function to be applied to the input elements, which
     *        returns a stream of results
     * @return a collector which applies the mapping function to the input
     *         elements and collects the flat mapped results to the {@code List}
     * @throws NullPointerException if mapper is null.
     * @since 0.6.0
     */
    public static <T extends @Nullable Object, U extends @Nullable Object> Collector<T, ?, List<U>> flatMapping(
            Function<? super T, ? extends @Nullable Stream<? extends U>> mapper) {
        return flatMapping(mapper, Collectors.toList());
    }

    /**
     * Returns a {@code Collector} which passes only those elements to the
     * specified downstream collector which match given predicate.
     *
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> if downstream collector is short-circuiting.
     * 
     * <p>
     * The operation performed by the returned collector is equivalent to
     * {@code stream.filter(predicate).collect(downstream)}. This collector is
     * mostly useful as a downstream collector in cascaded operation involving
     * {@link #pairing(Collector, Collector, BiFunction)} collector.
     *
     * <p>
     * This method is similar to {@code Collectors.filtering} method which
     * appears in JDK 9. However when downstream collector is
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting</a>
     * , this method will also return a short-circuiting collector.
     * 
     * @param <T> the type of the input elements
     * @param <A> intermediate accumulation type of the downstream collector
     * @param <R> result type of collector
     * @param predicate a filter function to be applied to the input elements
     * @param downstream a collector which will accept filtered values
     * @return a collector which applies the predicate to the input elements and
     *         provides the elements for which predicate returned true to the
     *         downstream collector
     * @throws NullPointerException if predicate is null, or downstream is null.
     * @see #pairing(Collector, Collector, BiFunction)
     * @since 0.4.0
     */
    public static <
            T extends @Nullable Object, 
            A extends @Nullable Object, 
            R extends @Nullable Object> Collector<T, ?, R> filtering(Predicate<? super T> predicate,
            Collector<T, A, R> downstream) {
        Objects.requireNonNull(predicate);
        BiConsumer<A, T> downstreamAccumulator = downstream.accumulator();
        BiConsumer<A, T> accumulator = (acc, t) -> {
            if (predicate.test(t))
                downstreamAccumulator.accept(acc, t);
        };
        Predicate<A> finished = finished(downstream);
        if (finished != null) {
            return new CancellableCollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream
                    .finisher(), finished, downstream.characteristics());
        }
        return Collector.of(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream
                .characteristics().toArray(new Characteristics[0]));
    }

    /**
     * Returns a {@code Collector} which filters input elements by the supplied
     * predicate, collecting them to the list.
     *
     * <p>
     * This method behaves like
     * {@code filtering(predicate, Collectors.toList())}.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.
     * 
     * @param <T> the type of the input elements
     * @param predicate a filter function to be applied to the input elements
     * @return a collector which applies the predicate to the input elements and
     *         collects the elements for which predicate returned true to the
     *         {@code List}
     * @throws NullPointerException if predicate is null.
     * @see #filtering(Predicate, Collector)
     * @since 0.6.0
     */
    public static <T extends @Nullable Object> Collector<T, ?, List<T>> filtering(Predicate<? super T> predicate) {
        return filtering(predicate, Collectors.toList());
    }

    /**
     * Returns a {@code Collector} which performs the bitwise-and operation of a
     * integer-valued function applied to the input elements. If no elements are
     * present, the result is empty {@link OptionalInt}.
     *
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>: it may not process all the elements if the result is zero.
     * 
     * @param <T> the type of the input elements
     * @param mapper a function extracting the property to be processed
     * @return a {@code Collector} that produces the bitwise-and operation of a
     *         derived property
     * @throws NullPointerException if mapper is null.
     * @since 0.4.0
     */
    public static <T extends @Nullable Object> Collector<T, ?, OptionalInt> andingInt(ToIntFunction<T> mapper) {
        Objects.requireNonNull(mapper);
        return new CancellableCollectorImpl<>(PrimitiveBox::new, (acc, t) -> {
            if (!acc.b) {
                acc.i = mapper.applyAsInt(t);
                acc.b = true;
            } else {
                acc.i &= mapper.applyAsInt(t);
            }
        }, (acc1, acc2) -> {
            if (!acc1.b)
                return acc2;
            if (!acc2.b)
                return acc1;
            acc1.i &= acc2.i;
            return acc1;
        }, PrimitiveBox::asInt, acc -> acc.b && acc.i == 0, UNORDERED_CHARACTERISTICS);
    }

    /**
     * Returns a {@code Collector} which performs the bitwise-and operation of a
     * long-valued function applied to the input elements. If no elements are
     * present, the result is empty {@link OptionalLong}.
     *
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>: it may not process all the elements if the result is zero.
     * 
     * @param <T> the type of the input elements
     * @param mapper a function extracting the property to be processed
     * @return a {@code Collector} that produces the bitwise-and operation of a
     *         derived property
     * @throws NullPointerException if mapper is null.
     * @since 0.4.0
     */
    public static <T extends @Nullable Object> Collector<T, ?, OptionalLong> andingLong(ToLongFunction<T> mapper) {
        Objects.requireNonNull(mapper);
        return new CancellableCollectorImpl<>(PrimitiveBox::new, (acc, t) -> {
            if (!acc.b) {
                acc.l = mapper.applyAsLong(t);
                acc.b = true;
            } else {
                acc.l &= mapper.applyAsLong(t);
            }
        }, (acc1, acc2) -> {
            if (!acc1.b)
                return acc2;
            if (!acc2.b)
                return acc1;
            acc1.l &= acc2.l;
            return acc1;
        }, PrimitiveBox::asLong, acc -> acc.b && acc.l == 0, UNORDERED_CHARACTERISTICS);
    }

    /**
     * Returns a {@code Collector} which computes a common prefix of input
     * {@code CharSequence} objects returning the result as {@code String}. For
     * empty input the empty {@code String} is returned.
     *
     * <p>
     * The returned {@code Collector} handles specially Unicode surrogate pairs:
     * the returned prefix may end with
     * <a href="http://www.unicode.org/glossary/#high_surrogate_code_unit">
     * Unicode high-surrogate code unit</a> only if it's not succeeded by
     * <a href="http://www.unicode.org/glossary/#low_surrogate_code_unit">
     * Unicode low-surrogate code unit</a> in any of the input sequences.
     * Normally the ending high-surrogate code unit is removed from the prefix.
     * 
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>: it may not process all the elements if the common prefix
     * is empty.
     * 
     * @return a {@code Collector} which computes a common prefix.
     * @since 0.5.0
     */
    public static Collector<CharSequence, ?, String> commonPrefix() {
        BiConsumer<ObjIntBox<CharSequence>, CharSequence> accumulator = (acc, t) -> {
            if (acc.b == -1) {
                acc.a = t;
                acc.b = t.length();
            } else if (acc.b > 0) {
                if (t.length() < acc.b)
                    acc.b = t.length();
                for (int i = 0; i < acc.b; i++) {
                    if (acc.a.charAt(i) != t.charAt(i)) {
                        if (i > 0 && Character.isHighSurrogate(t.charAt(i - 1)) && (Character.isLowSurrogate(t.charAt(
                            i)) || Character.isLowSurrogate(acc.a.charAt(i))))
                            i--;
                        acc.b = i;
                        break;
                    }
                }
            }
        };
        return new CancellableCollectorImpl<>(() -> new ObjIntBox<>(null, -1), accumulator, (acc1, acc2) -> {
            if (acc1.b == -1)
                return acc2;
            if (acc2.b != -1)
                accumulator.accept(acc1, acc2.a.subSequence(0, acc2.b));
            return acc1;
        }, acc -> acc.a == null ? "" : acc.a.subSequence(0, acc.b).toString(), acc -> acc.b == 0,
                UNORDERED_CHARACTERISTICS);
    }

    /**
     * Returns a {@code Collector} which computes a common suffix of input
     * {@code CharSequence} objects returning the result as {@code String}. For
     * empty input the empty {@code String} is returned.
     *
     * <p>
     * The returned {@code Collector} handles specially Unicode surrogate pairs:
     * the returned suffix may start with
     * <a href="http://www.unicode.org/glossary/#low_surrogate_code_unit">
     * Unicode low-surrogate code unit</a> only if it's not preceded by
     * <a href="http://www.unicode.org/glossary/#high_surrogate_code_unit">
     * Unicode high-surrogate code unit</a> in any of the input sequences.
     * Normally the starting low-surrogate code unit is removed from the suffix.
     * 
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>: it may not process all the elements if the common suffix
     * is empty.
     * 
     * @return a {@code Collector} which computes a common suffix.
     * @since 0.5.0
     */
    public static Collector<CharSequence, ?, String> commonSuffix() {
        BiConsumer<ObjIntBox<CharSequence>, CharSequence> accumulator = (acc, t) -> {
            if (acc.b == -1) {
                acc.a = t;
                acc.b = t.length();
            } else if (acc.b > 0) {
                int aLen = acc.a.length();
                int bLen = t.length();
                if (bLen < acc.b)
                    acc.b = bLen;
                for (int i = 0; i < acc.b; i++) {
                    if (acc.a.charAt(aLen - 1 - i) != t.charAt(bLen - 1 - i)) {
                        if (i > 0 && Character.isLowSurrogate(t.charAt(bLen - i)) && (Character.isHighSurrogate(t
                                .charAt(bLen - 1 - i)) || Character.isHighSurrogate(acc.a.charAt(aLen - 1 - i))))
                            i--;
                        acc.b = i;
                        break;
                    }
                }
            }
        };
        return new CancellableCollectorImpl<>(() -> new ObjIntBox<>(null, -1), accumulator, (acc1, acc2) -> {
            if (acc1.b == -1)
                return acc2;
            if (acc2.b != -1)
                accumulator.accept(acc1, acc2.a.subSequence(acc2.a.length() - acc2.b, acc2.a.length()));
            return acc1;
        }, acc -> acc.a == null ? "" : acc.a.subSequence(acc.a.length() - acc.b, acc.a.length()).toString(),
                acc -> acc.b == 0, UNORDERED_CHARACTERISTICS);
    }

    /**
     * Returns a collector which collects input elements into {@code List}
     * removing the elements following their dominator element. The dominator
     * elements are defined according to given isDominator {@code BiPredicate}.
     * The isDominator relation must be transitive (if A dominates over B and B
     * dominates over C, then A also dominates over C).
     * 
     * <p>
     * This operation is similar to
     * {@code streamEx.collapse(isDominator).toList()}. The important difference
     * is that in this method {@code BiPredicate} accepts not the adjacent
     * stream elements, but the leftmost element of the series (current
     * dominator) and the current element.
     * 
     * <p>
     * For example, consider the stream of numbers:
     * 
     * <pre>{@code
     * StreamEx<Integer> stream = StreamEx.of(1, 5, 3, 4, 2, 7);
     * }</pre>
     * 
     * <p>
     * Using {@code stream.collapse((a, b) -> a >= b).toList()} you will get the
     * numbers which are bigger than their immediate predecessor (
     * {@code [1, 5, 4, 7]}), because (3, 4) pair is not collapsed. However
     * using {@code stream.collect(dominators((a, b) -> a >= b))} you will get
     * the numbers which are bigger than any predecessor ({@code [1, 5, 7]}) as
     * 5 is the dominator element for the subsequent 3, 4 and 2.
     * 
     * @param <T> type of the input elements.
     * @param isDominator a non-interfering, stateless, transitive
     *        {@code BiPredicate} which returns true if the first argument is
     *        the dominator for the second argument.
     * @return a collector which collects input element into {@code List}
     *         leaving only dominator elements.
     * @throws NullPointerException if isDominator is null.
     * @see StreamEx#collapse(BiPredicate)
     * @since 0.5.1
     */
    public static <T extends @Nullable Object> Collector<T, ?, List<T>> dominators(
            BiPredicate<? super T, ? super T> isDominator) {
        Objects.requireNonNull(isDominator);
        return Collector.of(ArrayList::new, (acc, t) -> {
            if (acc.isEmpty() || !isDominator.test(acc.get(acc.size() - 1), t))
                acc.add(t);
        }, (acc1, acc2) -> {
            if (acc1.isEmpty())
                return acc2;
            int i = 0, l = acc2.size();
            T last = acc1.get(acc1.size() - 1);
            while (i < l && isDominator.test(last, acc2.get(i)))
                i++;
            if (i < l)
                acc1.addAll(acc2.subList(i, l));
            return acc1;
        });
    }

    /**
     * Returns a {@code Collector} which performs downstream reduction if all
     * elements satisfy the {@code Predicate}. The result is described as an
     * {@code Optional<R>}.
     * 
     * <p>
     * The resulting collector returns an empty optional if at least one input
     * element does not satisfy the predicate. Otherwise it returns an optional
     * which contains the result of the downstream collector.
     * 
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>: it may not process all the elements if some of items don't
     * satisfy the predicate or if downstream collector is a short-circuiting
     * collector.
     * 
     * <p>
     * It's guaranteed that the downstream collector is not called for elements
     * which don't satisfy the predicate.
     *
     * @param <T> the type of input elements
     * @param <A> intermediate accumulation type of the downstream collector
     * @param <R> result type of the downstream collector
     * @param predicate a non-interfering, stateless predicate to checks whether
     *        collector should proceed with element
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} witch performs downstream reduction if all
     *         elements satisfy the predicate
     * @throws NullPointerException if mapper is null.
     * @see Stream#allMatch(Predicate)
     * @see AbstractStreamEx#dropWhile(Predicate)
     * @see AbstractStreamEx#takeWhile(Predicate)
     * @since 0.6.3
     */
    public static <
            T extends @Nullable Object, 
            A extends @Nullable Object, 
            R> Collector<T, ?, Optional<R>> ifAllMatch(Predicate<T> predicate,
            Collector<T, A, R> downstream) {
        Objects.requireNonNull(predicate);
        Predicate<A> finished = finished(downstream);
        Supplier<A> supplier = downstream.supplier();
        BiConsumer<A, T> accumulator = downstream.accumulator();
        BinaryOperator<A> combiner = downstream.combiner();
        return new CancellableCollectorImpl<>(
                () -> new PairBox<>(supplier.get(), Boolean.TRUE),
                (acc, t) -> {
                    if (acc.b && predicate.test(t)) {
                        accumulator.accept(acc.a, t);
                    } else {
                        acc.b = Boolean.FALSE;
                    }
                },
                (acc1, acc2) -> {
                    if (acc1.b && acc2.b) {
                        acc1.a = combiner.apply(acc1.a, acc2.a);
                    } else {
                        acc1.b = Boolean.FALSE;
                    }
                    return acc1;
                },
                acc -> acc.b ? Optional.of(downstream.finisher().apply(acc.a)) : Optional.empty(),
                finished == null ? acc -> !acc.b : acc -> !acc.b || finished.test(acc.a),
                downstream.characteristics().contains(Characteristics.UNORDERED) ? UNORDERED_CHARACTERISTICS
                        : NO_CHARACTERISTICS);
    }

    /**
     * Returns a {@code Collector} which performs a possibly short-circuiting reduction of its
     * input elements under a specified {@code BinaryOperator}. The result
     * is described as an {@code Optional<T>}.
     *
     * <p>
     * This collector behaves like {@link Collectors#reducing(BinaryOperator)}. However,
     * it additionally accepts a zero element (also known as absorbing element). When zero element
     * is passed to the accumulator then the result must be zero as well. So the collector
     * takes the advantage of this and may short-circuit if zero is reached during the collection.
     *
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>: it may not process all the elements if the result of reduction is equal to zero.
     *
     * <p>
     * This collector is mostly useful as a downstream collector. To perform simple
     * short-circuiting reduction, use {@link AbstractStreamEx#reduceWithZero(Object, BinaryOperator)}
     * instead.
     *
     * @param zero zero element
     * @param op an <a href="package-summary.html#Associativity">associative</a>
     *        , <a href="package-summary.html#NonInterference">non-interfering
     *        </a>, <a href="package-summary.html#Statelessness">stateless</a>
     *        function to combine two elements into one.
     * @param <T> the type of input elements
     * @return a collector which returns an {@link Optional} describing the reduction result.
     *         For empty stream an empty {@code Optional} is returned.
     * @throws NullPointerException if op is null or the result of reduction is null
     * @see #reducingWithZero(Object, Object, BinaryOperator)
     * @see AbstractStreamEx#reduceWithZero(Object, BinaryOperator)
     * @see Collectors#reducing(BinaryOperator)
     * @since 0.7.3
     */
    public static <T> Collector<T, ?, Optional<T>> reducingWithZero(T zero, BinaryOperator<T> op) {
        Objects.requireNonNull(op);
        // acc.b: 0 = no element, 1 = has element, 2 = zero reached
        return new CancellableCollectorImpl<>(
            () -> new ObjIntBox<T>(null, 0),
            (acc, t) -> {
                if (acc.b != 2) {
                    if (acc.b == 1) {
                        t = op.apply(t, acc.a);
                    }
                    if (Objects.equals(t, zero)) {
                        acc.b = 2;
                        acc.a = zero;
                    } else {
                        acc.b = 1;
                        acc.a = t;
                    }
                }
            },
            (acc1, acc2) -> {
                if (acc1.b == 0 || acc2.b == 2) return acc2;
                if (acc2.b == 0 || acc1.b == 2) return acc1;
                T t = op.apply(acc1.a, acc2.a);
                if (Objects.equals(t, zero)) {
                    acc1.b = 2;
                    acc1.a = zero;
                } else {
                    acc1.a = t;
                }
                return acc1;
            },
            acc -> acc.b == 0 ? Optional.empty() : Optional.of(acc.a),
            acc -> acc.b == 2,
            UNORDERED_CHARACTERISTICS
        );
    }

    /**
     * Returns a {@code Collector} which performs a possibly short-circuiting reduction of its
     * input elements using the provided identity value and a {@code BinaryOperator}.
     *
     * <p>
     * This collector behaves like {@link Collectors#reducing(Object, BinaryOperator)}. However,
     * it additionally accepts a zero element (also known as absorbing element). When zero element
     * is passed to the accumulator then the result must be zero as well. So the collector
     * takes the advantage of this and may short-circuit if zero is reached during the collection.
     *
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>: it may not process all the elements if the result of reduction is equal to zero.
     *
     * <p>
     * This collector is mostly useful as a downstream collector. To perform simple
     * short-circuiting reduction, use {@link AbstractStreamEx#reduceWithZero(Object, BinaryOperator)}
     * instead.
     *
     * @param zero zero element
     * @param identity an identity element. For all {@code t}, {@code op.apply(t, identity)} is
     *                 equal to {@code op.apply(identity, t)} and is equal to {@code t}.
     * @param op an <a href="package-summary.html#Associativity">associative</a>
     *        , <a href="package-summary.html#NonInterference">non-interfering
     *        </a>, <a href="package-summary.html#Statelessness">stateless</a>
     *        function to combine two elements into one.
     * @param <T> the type of input elements
     * @return a collector which returns the reduction result.
     * @throws NullPointerException if op is null
     * @see #reducingWithZero(Object, BinaryOperator) 
     * @see AbstractStreamEx#reduceWithZero(Object, Object, BinaryOperator) 
     * @see Collectors#reducing(Object, BinaryOperator) 
     * @since 0.7.3
     */
    public static <T extends @Nullable Object> Collector<T, ?, T> reducingWithZero(T zero, T identity, BinaryOperator<T> op) {
        Objects.requireNonNull(op);
        // acc.b: 1 = has element, 2 = zero reached
        return new CancellableCollectorImpl<>(
            () -> new ObjIntBox<>(identity, 1),
            (acc, t) -> {
                if (acc.b != 2) {
                    t = op.apply(t, acc.a);
                    if (Objects.equals(t, zero)) {
                        acc.b = 2;
                        acc.a = zero;
                    } else {
                        acc.b = 1;
                        acc.a = t;
                    }
                }
            },
            (acc1, acc2) -> {
                if (acc2.b == 2) return acc2;
                if (acc1.b == 2) return acc1;
                T t = op.apply(acc1.a, acc2.a);
                if (Objects.equals(t, zero)) {
                    acc1.b = 2;
                    acc1.a = zero;
                } else {
                    acc1.a = t;
                }
                return acc1;
            },
            acc -> acc.a,
            acc -> acc.b == 2,
            UNORDERED_CHARACTERISTICS
        );
    }
}
