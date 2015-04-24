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

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link Stream} of {@link Map.Entry} objects which provides additional
 * specific functionality
 * 
 * @author Tagir Valeev
 *
 * @param <K>
 *            the type of {@code Entry} keys
 * @param <V>
 *            the type of {@code Entry} values
 */
public class EntryStream<K, V> extends AbstractStreamEx<Entry<K, V>, EntryStream<K, V>> {
    EntryStream(Stream<Entry<K, V>> stream) {
        super(stream);
    }

    @Override
    EntryStream<K, V> supply(Stream<Map.Entry<K, V>> stream) {
        return strategy().newEntryStream(stream);
    }

    /**
     * Returns a stream consisting of the results of applying the given function
     * to the elements of this stream.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param <R>
     *            The element type of the new stream
     * @param mapper
     *            a non-interfering, stateless function to apply to each element
     * @return the new stream
     */
    @Override
    public <R> StreamEx<R> map(Function<? super Entry<K, V>, ? extends R> mapper) {
        return strategy().newStreamEx(stream.map(mapper));
    }

    /**
     * Returns a stream consisting of the results of applying the given function
     * to the keys and values of this stream.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param <R>
     *            The element type of the new stream
     * @param mapper
     *            a non-interfering, stateless function to apply to key and
     *            value of each {@link Entry} in this stream
     * @return the new stream
     */
    public <R> StreamEx<R> mapKeyValue(BiFunction<? super K, ? super V, ? extends R> mapper) {
        return map(entry -> mapper.apply(entry.getKey(), entry.getValue()));
    }

    @Override
    public <R> StreamEx<R> flatMap(Function<? super Entry<K, V>, ? extends Stream<? extends R>> mapper) {
        return strategy().newStreamEx(stream.flatMap(mapper));
    }

    public <KK> EntryStream<KK, V> flatMapKeys(Function<? super K, ? extends Stream<? extends KK>> mapper) {
        return strategy().newEntryStream(stream.flatMap(e -> mapper.apply(e.getKey()).map(
                k -> new SimpleEntry<KK, V>(k, e.getValue()))));
    }

    public <VV> EntryStream<K, VV> flatMapValues(Function<? super V, ? extends Stream<? extends VV>> mapper) {
        return strategy().newEntryStream(stream.flatMap(e -> mapper.apply(e.getValue()).map(
                v -> new SimpleEntry<>(e.getKey(), v))));
    }

    public <R> StreamEx<R> flatCollection(Function<? super Entry<K, V>, ? extends Collection<? extends R>> mapper) {
        return flatMap(mapper.andThen(Collection::stream));
    }

    public EntryStream<K, V> append(K key, V value) {
        return strategy().newEntryStream(Stream.concat(stream, Stream.of(new SimpleEntry<>(key, value))));
    }

    public EntryStream<K, V> prepend(K key, V value) {
        return supply(Stream.concat(Stream.of(new SimpleEntry<>(key, value)), stream));
    }

    public <KK> EntryStream<KK, V> mapKeys(Function<K, KK> keyMapper) {
        return strategy().newEntryStream(stream.map(e -> new SimpleEntry<>(keyMapper.apply(e.getKey()), e.getValue())));
    }

    public <VV> EntryStream<K, VV> mapValues(Function<V, VV> valueMapper) {
        return strategy().newEntryStream(stream.map(e -> new SimpleEntry<>(e.getKey(), valueMapper.apply(e.getValue()))));
    }

    public <KK> EntryStream<KK, V> mapEntryKeys(Function<Entry<K, V>, KK> keyMapper) {
        return strategy().newEntryStream(stream.map(e -> new SimpleEntry<>(keyMapper.apply(e), e.getValue())));
    }

    public <VV> EntryStream<K, VV> mapEntryValues(Function<Entry<K, V>, VV> valueMapper) {
        return strategy().newEntryStream(stream.map(e -> new SimpleEntry<>(e.getKey(), valueMapper.apply(e))));
    }

    /**
     * Returns a stream consisting of the {@link Entry} objects which keys are
     * the values of this stream elements and vice versa
     *
     * <p>
     * This is an intermediate operation.
     *
     * @return the new stream
     */
    public EntryStream<V, K> invert() {
        return strategy().newEntryStream(stream.map(e -> new SimpleEntry<>(e.getValue(), e.getKey())));
    }

    /**
     * Returns a stream consisting of the elements of this stream which keys
     * match the given predicate.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param keyPredicate
     *            a non-interfering, stateless predicate to apply to the key of
     *            each element to determine if it should be included
     * @return the new stream
     */
    public EntryStream<K, V> filterKeys(Predicate<K> keyPredicate) {
        return filter(e -> keyPredicate.test(e.getKey()));
    }

    /**
     * Returns a stream consisting of the elements of this stream which values
     * match the given predicate.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param valuePredicate
     *            a non-interfering, stateless predicate to apply to the value
     *            of each element to determine if it should be included
     * @return the new stream
     */
    public EntryStream<K, V> filterValues(Predicate<V> valuePredicate) {
        return filter(e -> valuePredicate.test(e.getValue()));
    }

    /**
     * Returns a stream consisting of the elements of this stream which keys
     * don't match the given predicate.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param keyPredicate
     *            a non-interfering, stateless predicate to apply to the key of
     *            each element to determine if it should be excluded
     * @return the new stream
     */
    public EntryStream<K, V> removeKeys(Predicate<K> keyPredicate) {
        return filterKeys(keyPredicate.negate());
    }

    /**
     * Returns a stream consisting of the elements of this stream which values
     * don't match the given predicate.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param valuePredicate
     *            a non-interfering, stateless predicate to apply to the value
     *            of each element to determine if it should be excluded
     * @return the new stream
     */
    public EntryStream<K, V> removeValues(Predicate<V> valuePredicate) {
        return filterValues(valuePredicate.negate());
    }

    /**
     * Returns a stream consisting of the elements of this stream which key is
     * not null.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @return the new stream
     */
    public EntryStream<K, V> nonNullKeys() {
        return filter(e -> e.getKey() != null);
    }

    /**
     * Returns a stream consisting of the elements of this stream which value is
     * not null.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @return the new stream
     */
    public EntryStream<K, V> nonNullValues() {
        return filter(e -> e.getValue() != null);
    }

    @SuppressWarnings({ "unchecked" })
    public <KK extends K> EntryStream<KK, V> selectKeys(Class<KK> clazz) {
        return (EntryStream<KK, V>) filter(e -> clazz.isInstance(e.getKey()));
    }

    @SuppressWarnings({ "unchecked" })
    public <VV extends V> EntryStream<K, VV> selectValues(Class<VV> clazz) {
        return (EntryStream<K, VV>) filter(e -> clazz.isInstance(e.getValue()));
    }

    /**
     * Returns a stream consisting of the keys of this stream elements.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @return the new stream
     */
    public StreamEx<K> keys() {
        return map(Entry::getKey);
    }

    /**
     * Returns a stream consisting of the values of this stream elements.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @return the new stream
     */
    public StreamEx<V> values() {
        return map(Entry::getValue);
    }

    /**
     * Returns a {@link Map} containing the elements of this stream. There are
     * no guarantees on the type or serializability of the {@code Map} returned;
     * if more control over the returned {@code Map} is required, use
     * {@link #toCustomMap(Supplier)}.
     *
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), an {@code IllegalStateException} is
     * thrown when the collection operation is performed.
     * 
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * Returned {@code Map} is guaranteed to be modifiable.
     *
     * <p>
     * For parallel stream the concurrent {@code Map} is created.
     *
     * @return a {@code Map} containing the elements of this stream
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     */
    public Map<K, V> toMap() {
        return toMap(throwingMerger());
    }

    /**
     * Returns a {@link Map} containing the elements of this stream. There are
     * no guarantees on the type or serializability of the {@code Map} returned;
     * if more control over the returned {@code Map} is required, use
     * {@link #toCustomMap(BinaryOperator, Supplier)}.
     *
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), the value mapping function is applied to
     * each equal element, and the results are merged using the provided merging
     * function.
     * 
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * Returned {@code Map} is guaranteed to be modifiable.
     *
     * <p>
     * For parallel stream the concurrent {@code Map} is created.
     *
     * @param mergeFunction
     *            a merge function, used to resolve collisions between values
     *            associated with the same key, as supplied to
     *            {@link Map#merge(Object, Object, BiFunction)}
     * @return a {@code Map} containing the elements of this stream
     * @throws IllegalStateException
     *             if duplicate key was encountered in the stream
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @since 0.1.0
     */
    public Map<K, V> toMap(BinaryOperator<V> mergeFunction) {
        if (stream.isParallel())
            return collect(Collectors.toConcurrentMap(Entry::getKey, Entry::getValue, mergeFunction,
                    ConcurrentHashMap::new));
        return collect(Collectors.toMap(Entry::getKey, Entry::getValue, mergeFunction, HashMap::new));
    }

    public <M extends Map<K, V>> M toCustomMap(Supplier<M> mapSupplier) {
        return toCustomMap(throwingMerger(), mapSupplier);
    }

    @SuppressWarnings("unchecked")
    public <M extends Map<K, V>> M toCustomMap(BinaryOperator<V> mergeFunction, Supplier<M> mapSupplier) {
        if (stream.isParallel() && mapSupplier.get() instanceof ConcurrentMap)
            return (M) collect(Collectors.toConcurrentMap(Entry::getKey, Entry::getValue, mergeFunction,
                    (Supplier<? extends ConcurrentMap<K, V>>) mapSupplier));
        return collect(Collectors.toMap(Entry::getKey, Entry::getValue, mergeFunction, mapSupplier));
    }

    /**
     * Returns a {@link SortedMap} containing the elements of this stream. There
     * are no guarantees on the type or serializability of the {@code SortedMap}
     * returned; if more control over the returned {@code Map} is required, use
     * {@link #toCustomMap(Supplier)}.
     *
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), an {@code IllegalStateException} is
     * thrown when the collection operation is performed.
     * 
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * Returned {@code SortedMap} is guaranteed to be modifiable.
     *
     * <p>
     * For parallel stream the concurrent {@code SortedMap} is created.
     *
     * @return a {@code SortedMap} containing the elements of this stream
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @since 0.1.0
     */
    public SortedMap<K, V> toSortedMap() {
        return toSortedMap(throwingMerger());
    }

    /**
     * Returns a {@link SortedMap} containing the elements of this stream. There
     * are no guarantees on the type or serializability of the {@code SortedMap}
     * returned; if more control over the returned {@code Map} is required, use
     * {@link #toCustomMap(BinaryOperator, Supplier)}.
     *
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), the value mapping function is applied to
     * each equal element, and the results are merged using the provided merging
     * function.
     * 
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * Returned {@code SortedMap} is guaranteed to be modifiable.
     *
     * <p>
     * For parallel stream the concurrent {@code SortedMap} is created.
     *
     * @param mergeFunction
     *            a merge function, used to resolve collisions between values
     *            associated with the same key, as supplied to
     *            {@link Map#merge(Object, Object, BiFunction)}
     * @return a {@code SortedMap} containing the elements of this stream
     * @throws IllegalStateException
     *             if duplicate key was encountered in the stream
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @since 0.1.0
     */
    public SortedMap<K, V> toSortedMap(BinaryOperator<V> mergeFunction) {
        if (stream.isParallel())
            return collect(Collectors.toConcurrentMap(Entry::getKey, Entry::getValue, mergeFunction,
                    ConcurrentSkipListMap::new));
        return collect(Collectors.toMap(Entry::getKey, Entry::getValue, mergeFunction, TreeMap::new));
    }

    public Map<K, List<V>> grouping() {
        return grouping(Collectors.toList());
    }

    public <M extends Map<K, List<V>>> M grouping(Supplier<M> mapSupplier) {
        return grouping(mapSupplier, Collectors.toList());
    }

    public <A, D> Map<K, D> grouping(Collector<? super V, A, D> downstream) {
        if (stream.isParallel())
            return collect(Collectors.groupingByConcurrent(Entry::getKey,
                    Collectors.<Entry<K, V>, V, A, D> mapping(Entry::getValue, downstream)));
        return collect(Collectors.groupingBy(Entry::getKey,
                Collectors.<Entry<K, V>, V, A, D> mapping(Entry::getValue, downstream)));
    }

    @SuppressWarnings("unchecked")
    public <A, D, M extends Map<K, D>> M grouping(Supplier<M> mapSupplier, Collector<? super V, A, D> downstream) {
        if (stream.isParallel() && mapSupplier.get() instanceof ConcurrentMap)
            return (M) collect(Collectors.groupingByConcurrent(Entry::getKey,
                    (Supplier<? extends ConcurrentMap<K, D>>) mapSupplier,
                    Collectors.<Entry<K, V>, V, A, D> mapping(Entry::getValue, downstream)));
        return collect(Collectors.groupingBy(Entry::getKey, mapSupplier,
                Collectors.<Entry<K, V>, V, A, D> mapping(Entry::getValue, downstream)));
    }

    public <C extends Collection<V>> Map<K, C> groupingTo(Supplier<C> collectionFactory) {
        return grouping(Collectors.toCollection(collectionFactory));
    }

    public <C extends Collection<V>, M extends Map<K, C>> M groupingTo(Supplier<M> mapSupplier,
            Supplier<C> collectionFactory) {
        return grouping(mapSupplier, Collectors.toCollection(collectionFactory));
    }

    /**
     * Performs an action for each key-value pair of this stream.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * The behavior of this operation is explicitly nondeterministic. For
     * parallel stream pipelines, this operation does <em>not</em> guarantee to
     * respect the encounter order of the stream, as doing so would sacrifice
     * the benefit of parallelism. For any given element, the action may be
     * performed at whatever time and in whatever thread the library chooses. If
     * the action accesses shared state, it is responsible for providing the
     * required synchronization.
     *
     * @param action
     *            a non-interfering action to perform on the key and value
     * @see #forEach(java.util.function.Consumer)
     */
    public void forKeyValue(BiConsumer<? super K, ? super V> action) {
        forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
    }

    /**
     * Returns an empty sequential {@code EntryStream}.
     *
     * @param <K>
     *            the type of stream element keys
     * @param <V>
     *            the type of stream element values
     * @return an empty sequential stream
     * @since 0.0.8
     */
    public static <K, V> EntryStream<K, V> empty() {
        return new EntryStream<>(Stream.empty());
    }

    /**
     * Returns an {@code EntryStream} object which wraps given {@link Stream} of
     * {@link Entry} elements
     * 
     * @param <K>
     *            the type of original stream keys
     * @param <V>
     *            the type of original stream values
     * @param stream
     *            original stream
     * @return the wrapped stream
     */
    @SuppressWarnings("unchecked")
    public static <K, V> EntryStream<K, V> of(Stream<Entry<K, V>> stream) {
        return stream instanceof EntryStream ? (EntryStream<K, V>) stream : new EntryStream<>(stream);
    }

    public static <K, V> EntryStream<K, V> of(Map<K, V> map) {
        return new EntryStream<>(map.entrySet().stream());
    }

    /**
     * Returns a sequential {@code EntryStream} containing a single key-value
     * pair
     *
     * @param <K>
     *            the type of key
     * @param <V>
     *            the type of value
     * @param key
     *            the key of the single element
     * @param value
     *            the value of the single element
     * @return a singleton sequential stream
     */
    public static <K, V> EntryStream<K, V> of(K key, V value) {
        return new EntryStream<>(Stream.of(new SimpleEntry<>(key, value)));
    }
}
