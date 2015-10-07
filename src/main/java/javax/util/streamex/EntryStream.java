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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static javax.util.streamex.StreamExInternals.*;

/**
 * A {@link Stream} of {@link Entry} objects which provides additional specific
 * functionality.
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
    @SuppressWarnings("unchecked")
    EntryStream(Stream<? extends Entry<K, V>> stream) {
        super((Stream<Entry<K, V>>) stream);
    }

    @Override
    EntryStream<K, V> supply(Stream<Map.Entry<K, V>> stream) {
        return strategy().newEntryStream(stream);
    }

    static <K, V> Consumer<? super Entry<K, V>> toConsumer(BiConsumer<? super K, ? super V> action) {
        return entry -> action.accept(entry.getKey(), entry.getValue());
    }

    static <K, V, R> Function<? super Entry<K, V>, ? extends R> toFunction(
            BiFunction<? super K, ? super V, ? extends R> mapper) {
        return entry -> mapper.apply(entry.getKey(), entry.getValue());
    }

    @Override
    public EntryStream<K, V> sequential() {
        return StreamFactory.DEFAULT.newEntryStream(stream.sequential());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * If this stream was created using {@link #parallel(ForkJoinPool)}, the new
     * stream forgets about supplied custom {@link ForkJoinPool} and its
     * terminal operation will be executed in common pool.
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
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
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
     * Returns a {@link StreamEx} of strings which are created joining the keys
     * and values of the current stream using the specified delimiter.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
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
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
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

    /**
     * Returns an {@code EntryStream} consisting of the entries whose keys are
     * results of replacing source keys with the contents of a mapped stream
     * produced by applying the provided mapping function to each source key and
     * values are left intact. Each mapped stream is
     * {@link java.util.stream.BaseStream#close() closed} after its contents
     * have been placed into this stream. (If a mapped stream is {@code null} an
     * empty stream is used, instead.)
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param <KK>
     *            The type of new keys
     * @param mapper
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to apply to each key which produces a stream of new
     *            keys
     * @return the new stream
     */
    public <KK> EntryStream<KK, V> flatMapKeys(Function<? super K, ? extends Stream<? extends KK>> mapper) {
        return strategy().newEntryStream(stream.flatMap(e -> {
            Stream<? extends KK> s = mapper.apply(e.getKey());
            return s == null ? null : s.map(k -> new SimpleImmutableEntry<KK, V>(k, e.getValue()));
        }));
    }

    /**
     * Returns an {@code EntryStream} consisting of the entries whose values are
     * results of replacing source values with the contents of a mapped stream
     * produced by applying the provided mapping function to each source value
     * and keys are left intact. Each mapped stream is
     * {@link java.util.stream.BaseStream#close() closed} after its contents
     * have been placed into this stream. (If a mapped stream is {@code null} an
     * empty stream is used, instead.)
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param <VV>
     *            The type of new values
     * @param mapper
     *            a <a
     *            href="package-summary.html#NonInterference">non-interfering
     *            </a>, <a
     *            href="package-summary.html#Statelessness">stateless</a>
     *            function to apply to each value which produces a stream of new
     *            values
     * @return the new stream
     */
    public <VV> EntryStream<K, VV> flatMapValues(Function<? super V, ? extends Stream<? extends VV>> mapper) {
        return strategy().newEntryStream(stream.flatMap(e -> {
            Stream<? extends VV> s = mapper.apply(e.getValue());
            return s == null ? null : s.map(v -> new SimpleImmutableEntry<>(e.getKey(), v));
        }));
    }

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying the
     * provided mapping function to each key-value pair. Each mapped stream is
     * closed after its contents have been placed into this stream. (If a mapped
     * stream is {@code null} an empty stream is used, instead.)
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <R>
     *            The element type of the new stream
     * @param mapper
     *            a non-interfering, stateless function to apply to each
     *            key-value pair which produces a stream of new values
     * @return the new stream
     * @since 0.3.0
     */
    public <R> StreamEx<R> flatMapKeyValue(BiFunction<? super K, ? super V, ? extends Stream<? extends R>> mapper) {
        return this.<R> flatMap(toFunction(mapper));
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
        return append(Stream.of(new SimpleImmutableEntry<>(key, value)));
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of this stream
     * and two supplied key-value pairs.
     * 
     * @param k1
     *            the key of the first {@code Entry} to append to this stream
     * @param v1
     *            the value of the first {@code Entry} to append to this stream
     * @param k2
     *            the key of the second {@code Entry} to append to this stream
     * @param v2
     *            the value of the second {@code Entry} to append to this stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> append(K k1, V v1, K k2, V v2) {
        return append(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2)));
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of this stream
     * and three supplied key-value pairs.
     * 
     * @param k1
     *            the key of the first {@code Entry} to append to this stream
     * @param v1
     *            the value of the first {@code Entry} to append to this stream
     * @param k2
     *            the key of the second {@code Entry} to append to this stream
     * @param v2
     *            the value of the second {@code Entry} to append to this stream
     * @param k3
     *            the key of the third {@code Entry} to append to this stream
     * @param v3
     *            the value of the third {@code Entry} to append to this stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> append(K k1, V v1, K k2, V v2, K k3, V v3) {
        return append(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2),
            new SimpleImmutableEntry<>(k3, v3)));
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
        return prepend(Stream.of(new SimpleImmutableEntry<>(key, value)));
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of two
     * supplied key-value pairs and this stream.
     * 
     * @param k1
     *            the key of the first {@code Entry} to prepend to this stream
     * @param v1
     *            the value of the first {@code Entry} to prepend to this stream
     * @param k2
     *            the key of the second {@code Entry} to prepend to this stream
     * @param v2
     *            the value of the second {@code Entry} to prepend to this
     *            stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> prepend(K k1, V v1, K k2, V v2) {
        return prepend(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2)));
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of three
     * supplied key-value pairs and this stream.
     * 
     * @param k1
     *            the key of the first {@code Entry} to prepend to this stream
     * @param v1
     *            the value of the first {@code Entry} to prepend to this stream
     * @param k2
     *            the key of the second {@code Entry} to prepend to this stream
     * @param v2
     *            the value of the second {@code Entry} to prepend to this
     *            stream
     * @param k3
     *            the key of the third {@code Entry} to prepend to this stream
     * @param v3
     *            the value of the third {@code Entry} to prepend to this stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> prepend(K k1, V v1, K k2, V v2, K k3, V v3) {
        return prepend(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2),
            new SimpleImmutableEntry<>(k3, v3)));
    }

    /**
     * Returns a stream consisting of the elements of this stream which have
     * distinct keys (according to object equality).
     *
     * <p>
     * For ordered streams, the selection of distinct keys is stable (for
     * elements with duplicating keys, the element appearing first in the
     * encounter order is preserved.) For unordered streams, no stability
     * guarantees are made.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">stateful intermediate
     * operation</a>.
     *
     * @return the new stream
     * @since 0.3.8
     */
    public EntryStream<K, V> distinctKeys() {
        return distinct(Entry::getKey);
    }

    /**
     * Returns a stream consisting of the elements of this stream which have
     * distinct values (according to object equality).
     *
     * <p>
     * For ordered streams, the selection of distinct values is stable (for
     * elements with duplicating values, the element appearing first in the
     * encounter order is preserved.) For unordered streams, no stability
     * guarantees are made.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">stateful intermediate
     * operation</a>.
     *
     * @return the new stream
     * @since 0.3.8
     */
    public EntryStream<K, V> distinctValues() {
        return distinct(Entry::getValue);
    }

    /**
     * Returns an {@code EntryStream} consisting of the entries whose keys are
     * modified by applying the given function and values are left unchanged.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <KK>
     *            The type of the keys of the new stream
     * @param keyMapper
     *            a non-interfering, stateless function to apply to each key
     * @return the new stream
     */
    public <KK> EntryStream<KK, V> mapKeys(Function<? super K, ? extends KK> keyMapper) {
        return strategy().newEntryStream(
            stream.map(e -> new SimpleImmutableEntry<>(keyMapper.apply(e.getKey()), e.getValue())));
    }

    /**
     * Returns an {@code EntryStream} consisting of the entries whose keys are
     * left unchanged and values are modified by applying the given function.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <VV>
     *            The type of the values of the new stream
     * @param valueMapper
     *            a non-interfering, stateless function to apply to each value
     * @return the new stream
     */
    public <VV> EntryStream<K, VV> mapValues(Function<? super V, ? extends VV> valueMapper) {
        return strategy().newEntryStream(
            stream.map(e -> new SimpleImmutableEntry<>(e.getKey(), valueMapper.apply(e.getValue()))));
    }

    /**
     * Returns a {@link StreamEx} consisting of the results of applying the
     * given function to the keys and values of this stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <R>
     *            The element type of the new stream
     * @param mapper
     *            a non-interfering, stateless function to apply to key and
     *            value of each {@link Entry} in this stream
     * @return the new stream
     */
    public <R> StreamEx<R> mapKeyValue(BiFunction<? super K, ? super V, ? extends R> mapper) {
        return this.<R> map(toFunction(mapper));
    }

    /**
     * Returns an {@code EntryStream} consisting of the entries whose keys are
     * modified by applying the given function and values are left unchanged.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <KK>
     *            The type of the keys of the new stream
     * @param keyMapper
     *            a non-interfering, stateless function to apply to each
     *            key-value pair which returns the updated key
     * @return the new stream
     * @since 0.3.0
     */
    public <KK> EntryStream<KK, V> mapToKey(BiFunction<? super K, ? super V, ? extends KK> keyMapper) {
        return strategy().newEntryStream(
            stream.map(e -> new SimpleImmutableEntry<>(keyMapper.apply(e.getKey(), e.getValue()), e.getValue())));
    }

    /**
     * Returns an {@code EntryStream} consisting of the entries whose keys are
     * left unchanged and values are modified by applying the given function.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <VV>
     *            The type of the values of the new stream
     * @param valueMapper
     *            a non-interfering, stateless function to apply to each
     *            key-value pair which returns the updated value
     * @return the new stream
     * @since 0.3.0
     */
    public <VV> EntryStream<K, VV> mapToValue(BiFunction<? super K, ? super V, ? extends VV> valueMapper) {
        return strategy().newEntryStream(
            stream.map(e -> new SimpleImmutableEntry<>(e.getKey(), valueMapper.apply(e.getKey(), e.getValue()))));
    }

    /**
     * Returns a stream consisting of the {@link Entry} objects which keys are
     * the values of this stream elements and vice versa.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @return the new stream
     */
    public EntryStream<V, K> invert() {
        return strategy().newEntryStream(stream.map(e -> new SimpleImmutableEntry<>(e.getValue(), e.getKey())));
    }

    /**
     * Returns a stream consisting of the elements of this stream which keys
     * match the given predicate.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param keyPredicate
     *            a non-interfering, stateless predicate to apply to the key of
     *            each element to determine if it should be included
     * @return the new stream
     */
    public EntryStream<K, V> filterKeys(Predicate<? super K> keyPredicate) {
        return filter(e -> keyPredicate.test(e.getKey()));
    }

    /**
     * Returns a stream consisting of the elements of this stream which values
     * match the given predicate.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param valuePredicate
     *            a non-interfering, stateless predicate to apply to the value
     *            of each element to determine if it should be included
     * @return the new stream
     */
    public EntryStream<K, V> filterValues(Predicate<? super V> valuePredicate) {
        return filter(e -> valuePredicate.test(e.getValue()));
    }

    /**
     * Returns a stream consisting of the elements of this stream which elements
     * match the given predicate.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param predicate
     *            a non-interfering, stateless predicate to apply to the
     *            key-value pairs of each element to determine if it should be
     *            included
     * @return the new stream
     * @since 0.3.0
     */
    public EntryStream<K, V> filterKeyValue(BiPredicate<? super K, ? super V> predicate) {
        return filter(e -> predicate.test(e.getKey(), e.getValue()));
    }

    /**
     * Returns a stream consisting of the elements of this stream which keys
     * don't match the given predicate.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param keyPredicate
     *            a non-interfering, stateless predicate to apply to the key of
     *            each element to determine if it should be excluded
     * @return the new stream
     */
    public EntryStream<K, V> removeKeys(Predicate<? super K> keyPredicate) {
        return filterKeys(keyPredicate.negate());
    }

    /**
     * Returns a stream consisting of the elements of this stream which values
     * don't match the given predicate.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param valuePredicate
     *            a non-interfering, stateless predicate to apply to the value
     *            of each element to determine if it should be excluded
     * @return the new stream
     */
    public EntryStream<K, V> removeValues(Predicate<? super V> valuePredicate) {
        return filterValues(valuePredicate.negate());
    }

    /**
     * Returns a stream consisting of the elements of this stream which key is
     * not null.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
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
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @return the new stream
     */
    public EntryStream<K, V> nonNullValues() {
        return filter(e -> e.getValue() != null);
    }

    /**
     * Returns a stream consisting of the elements of this stream which keys are
     * instances of given class.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <KK>
     *            a type of keys to select.
     * @param clazz
     *            a class to filter the keys.
     * @return the new stream
     */
    @SuppressWarnings({ "unchecked" })
    public <KK extends K> EntryStream<KK, V> selectKeys(Class<KK> clazz) {
        return (EntryStream<KK, V>) filter(e -> clazz.isInstance(e.getKey()));
    }

    /**
     * Returns a stream consisting of the elements of this stream which values
     * are instances of given class.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <VV>
     *            a type of values to select.
     * @param clazz
     *            a class to filter the values.
     * @return the new stream
     */
    @SuppressWarnings({ "unchecked" })
    public <VV extends V> EntryStream<K, VV> selectValues(Class<VV> clazz) {
        return (EntryStream<K, VV>) filter(e -> clazz.isInstance(e.getValue()));
    }

    /**
     * Returns a stream consisting of the entries of this stream, additionally
     * performing the provided action on each entry key as entries are consumed
     * from the resulting stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * <p>
     * For parallel stream pipelines, the action may be called at whatever time
     * and in whatever thread the element is made available by the upstream
     * operation. If the action modifies shared state, it is responsible for
     * providing the required synchronization.
     *
     * @param keyAction
     *            a non-interfering action to perform on the keys of the entries
     *            as they are consumed from the stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> peekKeys(Consumer<? super K> keyAction) {
        return peek(e -> keyAction.accept(e.getKey()));
    }

    /**
     * Returns a stream consisting of the entries of this stream, additionally
     * performing the provided action on each entry value as entries are
     * consumed from the resulting stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * <p>
     * For parallel stream pipelines, the action may be called at whatever time
     * and in whatever thread the element is made available by the upstream
     * operation. If the action modifies shared state, it is responsible for
     * providing the required synchronization.
     *
     * @param valueAction
     *            a non-interfering action to perform on the values of the
     *            entries as they are consumed from the stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> peekValues(Consumer<? super V> valueAction) {
        return peek(e -> valueAction.accept(e.getValue()));
    }

    /**
     * Returns a stream consisting of the entries of this stream, additionally
     * performing the provided action on each entry key-value pair as entries
     * are consumed from the resulting stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * <p>
     * For parallel stream pipelines, the action may be called at whatever time
     * and in whatever thread the element is made available by the upstream
     * operation. If the action modifies shared state, it is responsible for
     * providing the required synchronization.
     *
     * @param action
     *            a non-interfering action to perform on the keys and values of
     *            the entries as they are consumed from the stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> peekKeyValue(BiConsumer<? super K, ? super V> action) {
        return peek(toConsumer(action));
    }

    /**
     * Returns a stream consisting of the keys of this stream elements.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
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
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
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
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
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
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
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
        Function<Entry<K, V>, K> keyMapper = Entry::getKey;
        Function<Entry<K, V>, V> valueMapper = Entry::getValue;
        if (stream.isParallel())
            return collect(Collectors.toConcurrentMap(keyMapper, valueMapper, mergeFunction, ConcurrentHashMap::new));
        return collect(Collectors.toMap(keyMapper, valueMapper, mergeFunction, HashMap::new));
    }

    public <M extends Map<K, V>> M toCustomMap(Supplier<M> mapSupplier) {
        return toCustomMap(throwingMerger(), mapSupplier);
    }

    @SuppressWarnings("unchecked")
    public <M extends Map<K, V>> M toCustomMap(BinaryOperator<V> mergeFunction, Supplier<M> mapSupplier) {
        Function<Entry<K, V>, K> keyMapper = Entry::getKey;
        Function<Entry<K, V>, V> valueMapper = Entry::getValue;
        if (stream.isParallel() && mapSupplier.get() instanceof ConcurrentMap)
            return (M) collect(Collectors.toConcurrentMap(keyMapper, valueMapper, mergeFunction,
                (Supplier<? extends ConcurrentMap<K, V>>) mapSupplier));
        return collect(Collectors.toMap(keyMapper, valueMapper, mergeFunction, mapSupplier));
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
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
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
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
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
        Function<Entry<K, V>, K> keyMapper = Entry::getKey;
        Function<Entry<K, V>, V> valueMapper = Entry::getValue;
        if (stream.isParallel())
            return collect(Collectors
                    .toConcurrentMap(keyMapper, valueMapper, mergeFunction, ConcurrentSkipListMap::new));
        return collect(Collectors.toMap(keyMapper, valueMapper, mergeFunction, TreeMap::new));
    }

    public Map<K, List<V>> grouping() {
        return grouping(Collectors.toList());
    }

    public <M extends Map<K, List<V>>> M grouping(Supplier<M> mapSupplier) {
        return grouping(mapSupplier, Collectors.toList());
    }

    public <A, D> Map<K, D> grouping(Collector<? super V, A, D> downstream) {
        Function<Entry<K, V>, K> keyMapper = Entry::getKey;
        Collector<Entry<K, V>, ?, D> mapping = Collectors.mapping(Entry::getValue, downstream);
        if (stream.isParallel()) {
            return collect(Collectors.groupingByConcurrent(keyMapper, mapping));
        }
        return collect(Collectors.groupingBy(keyMapper, mapping));
    }

    @SuppressWarnings("unchecked")
    public <A, D, M extends Map<K, D>> M grouping(Supplier<M> mapSupplier, Collector<? super V, A, D> downstream) {
        Function<Entry<K, V>, K> keyMapper = Entry::getKey;
        Collector<Entry<K, V>, ?, D> mapping = Collectors.mapping(Entry::getValue, downstream);
        if (stream.isParallel() && mapSupplier.get() instanceof ConcurrentMap) {
            return (M) collect(Collectors.groupingByConcurrent(keyMapper,
                (Supplier<? extends ConcurrentMap<K, D>>) mapSupplier, mapping));
        }
        return collect(Collectors.groupingBy(keyMapper, mapSupplier, mapping));
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
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
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
        forEach(toConsumer(action));
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
    public static <K, V> EntryStream<K, V> of(Stream<? extends Entry<K, V>> stream) {
        return new EntryStream<>(unwrap(stream));
    }

    /**
     * Returns a sequential {@link EntryStream} created from given
     * {@link Spliterator}.
     *
     * @param <K>
     *            the type of stream keys
     * @param <V>
     *            the type of stream values
     * @param spliterator
     *            a spliterator to create the stream from.
     * @return the new stream
     * @since 0.3.4
     */
    public static <K, V> EntryStream<K, V> of(Spliterator<? extends Entry<K, V>> spliterator) {
        return new EntryStream<>(StreamSupport.stream(spliterator, false));
    }

    /**
     * Returns an {@code EntryStream} object which contains the entries of
     * supplied {@code Map}.
     * 
     * @param <K>
     *            the type of map keys
     * @param <V>
     *            the type of map values
     * @param map
     *            the map to create the stream from
     * @return a new {@code EntryStream}
     */
    public static <K, V> EntryStream<K, V> of(Map<K, V> map) {
        return new EntryStream<>(map.entrySet().stream());
    }

    /**
     * Returns an {@code EntryStream} object whose keys are indices of given
     * list and the values are the corresponding list elements.
     * 
     * <p>
     * The list elements are accessed using {@link List#get(int)}, so the list
     * should provide fast random access. The list is assumed to be unmodifiable
     * during the stream operations.
     * 
     * @param <V>
     *            list element type
     * @param list
     *            list to create the stream from
     * @return a new {@code EntryStream}
     * @since 0.2.3
     */
    public static <V> EntryStream<Integer, V> of(List<V> list) {
        return EntryStream.of(new RangeBasedSpliterator.AsEntry<>(list));
    }

    /**
     * Returns an {@code EntryStream} object whose keys are indices of given
     * array and the values are the corresponding array elements.
     * 
     * @param <V>
     *            array element type
     * @param array
     *            array to create the stream from
     * @return a new {@code EntryStream}
     * @since 0.2.3
     */
    public static <V> EntryStream<Integer, V> of(V[] array) {
        return of(Arrays.asList(array));
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
        return new EntryStream<>(Stream.of(new SimpleImmutableEntry<>(key, value)));
    }

    /**
     * Returns a sequential {@code EntryStream} containing two key-value pairs
     *
     * @param <K>
     *            the type of key
     * @param <V>
     *            the type of value
     * @param k1
     *            the key of the first element
     * @param v1
     *            the value of the first element
     * @param k2
     *            the key of the second element
     * @param v2
     *            the value of the second element
     * @return a sequential stream
     * @since 0.2.3
     */
    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2) {
        return new EntryStream<>(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2)));
    }

    /**
     * Returns a sequential {@code EntryStream} containing three key-value pairs
     *
     * @param <K>
     *            the type of key
     * @param <V>
     *            the type of value
     * @param k1
     *            the key of the first element
     * @param v1
     *            the value of the first element
     * @param k2
     *            the key of the second element
     * @param v2
     *            the value of the second element
     * @param k3
     *            the key of the third element
     * @param v3
     *            the value of the third element
     * @return a sequential stream
     * @since 0.2.3
     */
    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return new EntryStream<>(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2),
            new SimpleImmutableEntry<>(k3, v3)));
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
     * @see StreamEx#zip(List, List, BiFunction)
     * @since 0.2.1
     */
    public static <K, V> EntryStream<K, V> zip(List<K> keys, List<V> values) {
        return of(new RangeBasedSpliterator.ZipRef<>(0, checkLength(keys.size(), values.size()),
                SimpleImmutableEntry<K, V>::new, keys, values));
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
     * @see StreamEx#zip(Object[], Object[], BiFunction)
     * @since 0.2.1
     */
    public static <K, V> EntryStream<K, V> zip(K[] keys, V[] values) {
        return zip(Arrays.asList(keys), Arrays.asList(values));
    }

    /**
     * Returns a sequential ordered {@code EntryStream} containing the possible
     * pairs of elements taken from the provided list.
     * 
     * <p>
     * Both keys and values are taken from the input list. The index of the key
     * is always strictly less than the index of the value. The pairs in the
     * stream are lexicographically ordered. For example, for the list of three
     * elements the stream of three elements is created:
     * {@code Map.Entry(list.get(0), list.get(1))},
     * {@code Map.Entry(list.get(0), list.get(2))} and
     * {@code Map.Entry(list.get(1), list.get(2))}. The number of elements in
     * the resulting stream is {@code list.size()*(list.size()+1L)/2}.
     * 
     * <p>
     * The list values are accessed using {@link List#get(int)}, so the list
     * should provide fast random access. The list is assumed to be unmodifiable
     * during the stream operations.
     *
     * @param <T>
     *            type of the list elements
     * @param list
     *            a list to take the elements from
     * @return a new {@code EntryStream}
     * @see StreamEx#ofPairs(List, BiFunction)
     * @since 0.3.6
     */
    public static <T> EntryStream<T, T> ofPairs(List<T> list) {
        return of(new PairPermutationSpliterator<>(list, SimpleImmutableEntry<T, T>::new));
    }

    /**
     * Returns a sequential ordered {@code EntryStream} containing the possible
     * pairs of elements taken from the provided array.
     * 
     * <p>
     * Both keys and values are taken from the input array. The index of the key
     * is always strictly less than the index of the value. The pairs in the
     * stream are lexicographically ordered. For example, for the array of three
     * elements the stream of three elements is created:
     * {@code Map.Entry(array[0], array[1])},
     * {@code Map.Entry(array[0], array[2])} and
     * {@code Map.Entry(array[1], array[2])}. The number of elements in the
     * resulting stream is {@code array.length*(array.length+1L)/2}..
     * 
     * @param <T>
     *            type of the array elements
     * @param array
     *            a array to take the elements from
     * @return a new {@code EntryStream}
     * @see StreamEx#ofPairs(Object[], BiFunction)
     * @since 0.3.6
     */
    public static <T> EntryStream<T, T> ofPairs(T[] array) {
        return ofPairs(Arrays.asList(array));
    }
}
