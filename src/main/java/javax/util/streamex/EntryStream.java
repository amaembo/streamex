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
import java.util.Arrays;
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
import java.util.concurrent.ForkJoinPool;
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
 * specific functionality.
 * 
 * <p>
 * While {@code EntryStream} implements {@code Iterable}, it is not a
 * general-purpose {@code Iterable} as it supports only a single
 * {@code Iterator}; invoking the {@link #iterator iterator} method to obtain a
 * second or subsequent iterator throws {@code IllegalStateException}.
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

    @Override
    public EntryStream<K, V> sequential() {
        return StreamFactory.DEFAULT.newEntryStream(stream.sequential());
    }

    /**
     * Returns an equivalent stream that is parallel. May return itself, either
     * because the stream was already parallel, or because the underlying stream
     * state was modified to be parallel.
     *
     * <p>
     * This is an intermediate operation.
     * 
     * <p>
     * If this stream was created using {@link #parallel(ForkJoinPool)}, the new
     * stream forgets about supplied custom {@link ForkJoinPool} and its
     * terminal operation will be executed in common pool.
     *
     * @return a parallel stream
     */
    @Override
    public EntryStream<K, V> parallel() {
        return StreamFactory.DEFAULT.newEntryStream(stream.parallel());
    }

    /**
     * Returns an equivalent stream that is parallel and bound to the supplied
     * {@link ForkJoinPool}.
     *
     * <p>
     * This is an intermediate operation.
     * 
     * <p>
     * The terminal operation of this stream or any derived stream (except the
     * streams created via {@link #parallel()} or {@link #sequential()} methods)
     * will be executed inside the supplied {@code ForkJoinPool}. If current
     * thread does not belong to that pool, it will wait till calculation
     * finishes.
     *
     * @param fjp
     *            a {@code ForkJoinPool} to submit the stream operation to.
     * @return a parallel stream bound to the supplied {@code ForkJoinPool}
     * @since 0.2.0
     */
    public EntryStream<K, V> parallel(ForkJoinPool fjp) {
        return StreamFactory.forCustomPool(fjp).newEntryStream(stream.parallel());
    }

    /**
     * Returns a {@link StreamEx} consisting of the results of applying the
     * given function to the keys and values of this stream.
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

    /**
     * Returns a {@link StreamEx} of strings which are created joining the keys
     * and values of the current stream using the specified delimiter.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param delimiter
     *            the delimiter to be used between key and value
     * @return the new stream
     * @since 0.2.2
     */
    public StreamEx<String> join(CharSequence delimiter) {
        return map(entry -> new StringBuilder().append(entry.getKey()).append(delimiter).append(entry.getValue())
                .toString());
    }

    /**
     * Returns a {@link StreamEx} of strings which are created joining the keys
     * and values of the current stream using the specified delimiter, with the
     * specified prefix and suffix.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param delimiter
     *            the delimiter to be used between key and value
     * @param prefix
     *            the sequence of characters to be used at the beginning of each
     *            resulting string
     * @param suffix
     *            the sequence of characters to be used at the end of each
     *            resulting string
     * @return the new stream
     * @since 0.2.2
     */
    public StreamEx<String> join(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return map(entry -> new StringBuilder(prefix).append(entry.getKey()).append(delimiter).append(entry.getValue())
                .append(suffix).toString());
    }

    public <KK> EntryStream<KK, V> flatMapKeys(Function<? super K, ? extends Stream<? extends KK>> mapper) {
        return strategy().newEntryStream(stream.flatMap(e -> {
            Stream<? extends KK> s = mapper.apply(e.getKey());
            return s == null ? null : s.map(k -> new SimpleEntry<KK, V>(k, e.getValue()));
        }));
    }

    public <VV> EntryStream<K, VV> flatMapValues(Function<? super V, ? extends Stream<? extends VV>> mapper) {
        return strategy().newEntryStream(stream.flatMap(e -> {
            Stream<? extends VV> s = mapper.apply(e.getValue());
            return s == null ? null : s.map(v -> new SimpleEntry<>(e.getKey(), v));
        }));
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of this stream
     * and the stream created from the supplied map entries.
     * 
     * @param map
     *            the map to prepend to the stream
     * @return the new stream
     * @since 0.2.1
     */
    public EntryStream<K, V> append(Map<K, V> map) {
        return append(map.entrySet().stream());
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of this stream
     * and the supplied key-value pair.
     * 
     * @param key
     *            the key of the new {@code Entry} to append to this stream
     * @param value
     *            the value of the new {@code Entry} to append to this stream
     * @return the new stream
     */
    public EntryStream<K, V> append(K key, V value) {
        return append(Stream.of(new SimpleEntry<>(key, value)));
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of the stream
     * created from the supplied map entries and this stream.
     * 
     * @param map
     *            the map to prepend to the stream
     * @return the new stream
     * @since 0.2.1
     */
    public EntryStream<K, V> prepend(Map<K, V> map) {
        return append(map.entrySet().stream());
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of the
     * supplied key-value pair and this stream.
     * 
     * @param key
     *            the key of the new {@code Entry} to prepend to this stream
     * @param value
     *            the value of the new {@code Entry} to prepend to this stream
     * @return the new stream
     */
    public EntryStream<K, V> prepend(K key, V value) {
        return prepend(Stream.of(new SimpleEntry<>(key, value)));
    }

    public <KK> EntryStream<KK, V> mapKeys(Function<K, KK> keyMapper) {
        return strategy().newEntryStream(stream.map(e -> new SimpleEntry<>(keyMapper.apply(e.getKey()), e.getValue())));
    }

    public <VV> EntryStream<K, VV> mapValues(Function<V, VV> valueMapper) {
        return strategy().newEntryStream(
                stream.map(e -> new SimpleEntry<>(e.getKey(), valueMapper.apply(e.getValue()))));
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
    public static <K, V> EntryStream<K, V> of(Stream<Entry<K, V>> stream) {
        return new EntryStream<>(unwrap(stream));
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

    /**
     * Returns a sequential {@code EntryStream} containing {@code Entry} objects
     * composed from corresponding key and value in given two lists.
     * 
     * <p>
     * The keys and values are accessed using {@link List#get(int)}, so the
     * lists should provide fast random access. The lists are assumed to be
     * unmodifiable during the stream operations.
     * 
     * @param <K>
     *            the type of stream element keys
     * @param <V>
     *            the type of stream element values
     * @param keys
     *            the list of keys, assumed to be unmodified during use
     * @param values
     *            the list of values, assumed to be unmodified during use
     * @return a new {@code EntryStream}
     * @throws IllegalArgumentException
     *             if length of the lists differs.
     * @since 0.2.1
     */
    public static <K, V> EntryStream<K, V> zip(List<K> keys, List<V> values) {
        return of(intStreamForLength(keys.size(), values.size()).mapToObj(
                i -> new SimpleEntry<>(keys.get(i), values.get(i))));
    }

    /**
     * Returns a sequential {@code EntryStream} containing {@code Entry} objects
     * composed from corresponding key and value in given two arrays.
     * 
     * @param <K>
     *            the type of stream element keys
     * @param <V>
     *            the type of stream element values
     * @param keys
     *            the array of keys
     * @param values
     *            the array of values
     * @return a new {@code EntryStream}
     * @throws IllegalArgumentException
     *             if length of the arrays differs.
     * @since 0.2.1
     */
    public static <K, V> EntryStream<K, V> zip(K[] keys, V[] values) {
        return zip(Arrays.asList(keys), Arrays.asList(values));
    }
}
