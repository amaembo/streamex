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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
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
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A {@link Stream} implementation with additional functionality.
 * 
 * <p>
 * While {@code StreamEx} implements {@code Iterable}, it is not a
 * general-purpose {@code Iterable} as it supports only a single
 * {@code Iterator}; invoking the {@link #iterator iterator} method to obtain a
 * second or subsequent iterator throws {@code IllegalStateException}.
 *
 * @author Tagir Valeev
 *
 * @param <T>
 *            the type of the stream elements
 */
public class StreamEx<T> extends AbstractStreamEx<T, StreamEx<T>> {
    StreamEx(Stream<T> stream) {
        super(stream);
    }

    @Override
    StreamEx<T> supply(Stream<T> stream) {
        return strategy().newStreamEx(stream);
    }

    /**
     * Returns an equivalent stream that is sequential. May return itself,
     * either because the stream was already sequential, or because the
     * underlying stream state was modified to be sequential.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @return a sequential stream
     */
    @Override
    public StreamEx<T> sequential() {
        return StreamFactory.DEFAULT.newStreamEx(stream.sequential());
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
    public StreamEx<T> parallel() {
        return StreamFactory.DEFAULT.newStreamEx(stream.parallel());
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
    public StreamEx<T> parallel(ForkJoinPool fjp) {
        return StreamFactory.forCustomPool(fjp).newStreamEx(stream.parallel());
    }

    /**
     * Returns a stream consisting of the elements of this stream which are
     * instances of given class.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param <TT>
     *            a type of instances to select.
     * @param clazz
     *            a class which instances should be selected
     * @return the new stream
     */
    @SuppressWarnings({ "unchecked" })
    public <TT extends T> StreamEx<TT> select(Class<TT> clazz) {
        return (StreamEx<TT>) filter(clazz::isInstance);
    }

    /**
     * Returns an {@link EntryStream} consisting of the {@link Entry} objects
     * which keys are elements of this stream and values are results of applying
     * the given function to the elements of this stream.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param <V>
     *            The {@code Entry} value type
     * @param valueMapper
     *            a non-interfering, stateless function to apply to each element
     * @return the new stream
     */
    public <V> EntryStream<T, V> mapToEntry(Function<T, V> valueMapper) {
        return strategy().newEntryStream(stream.map(e -> new SimpleEntry<>(e, valueMapper.apply(e))));
    }

    /**
     * Returns an {@link EntryStream} consisting of the {@link Entry} objects
     * which keys and values are results of applying the given functions to the
     * elements of this stream.
     *
     * <p>
     * This is an intermediate operation.
     *
     * @param <K>
     *            The {@code Entry} key type
     * @param <V>
     *            The {@code Entry} value type
     * @param keyMapper
     *            a non-interfering, stateless function to apply to each element
     * @param valueMapper
     *            a non-interfering, stateless function to apply to each element
     * @return the new stream
     */
    public <K, V> EntryStream<K, V> mapToEntry(Function<T, K> keyMapper, Function<T, V> valueMapper) {
        return strategy().newEntryStream(stream.map(e -> new SimpleEntry<>(keyMapper.apply(e), valueMapper.apply(e))));
    }

    public <K, V> EntryStream<K, V> flatMapToEntry(Function<? super T, Map<K, V>> mapper) {
        return strategy().newEntryStream(stream.flatMap(e -> {
            Map<K, V> s = mapper.apply(e);
            return s == null ? null : s.entrySet().stream();
        }));
    }

    /**
     * Returns a {@code Map} whose keys are the values resulting from applying
     * the classification function to the input elements, and whose
     * corresponding values are {@code List}s containing the input elements
     * which map to the associated key under the classification function.
     *
     * <p>
     * There are no guarantees on the type, mutability or serializability of the
     * {@code Map} or {@code List} objects returned.
     * 
     * <p>
     * For parallel stream concurrent collector is used and ConcurrentMap is
     * returned.
     *
     * <p>
     * This is a terminal operation.
     * 
     * @param <K>
     *            the type of the keys
     * @param classifier
     *            the classifier function mapping input elements to keys
     * @return a {@code Map} containing the results of the group-by operation
     *
     * @see #groupingBy(Function, Collector)
     * @see Collectors#groupingBy(Function)
     * @see Collectors#groupingByConcurrent(Function)
     */
    public <K> Map<K, List<T>> groupingBy(Function<? super T, ? extends K> classifier) {
        return groupingBy(classifier, Collectors.toList());
    }

    public <K, D> Map<K, D> groupingBy(Function<? super T, ? extends K> classifier,
            Collector<? super T, ?, D> downstream) {
        if (stream.isParallel())
            return collect(Collectors.groupingByConcurrent(classifier, downstream));
        return collect(Collectors.groupingBy(classifier, downstream));
    }

    @SuppressWarnings("unchecked")
    public <K, D, M extends Map<K, D>> M groupingBy(Function<? super T, ? extends K> classifier,
            Supplier<M> mapFactory, Collector<? super T, ?, D> downstream) {
        if (stream.isParallel() && mapFactory.get() instanceof ConcurrentMap)
            return (M) collect(Collectors.groupingByConcurrent(classifier, (Supplier<ConcurrentMap<K, D>>) mapFactory,
                    downstream));
        return collect(Collectors.groupingBy(classifier, mapFactory, downstream));
    }

    public <K, C extends Collection<T>> Map<K, C> groupingTo(Function<? super T, ? extends K> classifier,
            Supplier<C> collectionFactory) {
        return groupingBy(classifier, Collectors.toCollection(collectionFactory));
    }

    public <K, C extends Collection<T>, M extends Map<K, C>> M groupingTo(Function<? super T, ? extends K> classifier,
            Supplier<M> mapFactory, Supplier<C> collectionFactory) {
        return groupingBy(classifier, mapFactory, Collectors.toCollection(collectionFactory));
    }

    /**
     * Returns a {@code Map<Boolean, List<T>>} which contains two partitions of
     * the input elements according to a {@code Predicate}.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} returned.
     *
     * @param predicate
     *            a predicate used for classifying input elements
     * @return a {@code Map<Boolean, List<T>>} which {@link Boolean.TRUE} key is
     *         mapped to the list of the stream elements for which predicate is
     *         true and {@link Boolean.FALSE} key is mapped to the list of all
     *         other stream elements.
     *
     * @see #partitioningBy(Predicate, Collector)
     * @see Collectors#partitioningBy(Predicate)
     * @since 0.2.2
     */
    public Map<Boolean, List<T>> partitioningBy(Predicate<? super T> predicate) {
        return collect(Collectors.partitioningBy(predicate));
    }

    /**
     * Returns a {@code Map<Boolean, D>} which contains two partitions of the
     * input elements according to a {@code Predicate}, which are reduced
     * according to the supplied {@code Collector}.
     *
     * <p>
     * This is a terminal operation.
     *
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} returned.
     *
     * @param <D>
     *            the result type of the downstream reduction
     * @param predicate
     *            a predicate used for classifying input elements
     * @param downstream
     *            a {@code Collector} implementing the downstream reduction
     * @return a {@code Map<Boolean, List<T>>} which {@link Boolean.TRUE} key is
     *         mapped to the result of downstream {@code Collector} collecting
     *         the the stream elements for which predicate is true and
     *         {@link Boolean.FALSE} key is mapped to the result of downstream
     *         {@code Collector} collecting the other stream elements.
     *
     * @see #partitioningBy(Predicate)
     * @see Collectors#partitioningBy(Predicate, Collector)
     * @since 0.2.2
     */
    public <D> Map<Boolean, D> partitioningBy(Predicate<? super T> predicate, Collector<? super T, ?, D> downstream) {
        return collect(Collectors.partitioningBy(predicate, downstream));
    }

    public <C extends Collection<T>> Map<Boolean, C> partitioningTo(Predicate<? super T> predicate,
            Supplier<C> collectionFactory) {
        return collect(Collectors.partitioningBy(predicate, Collectors.toCollection(collectionFactory)));
    }

    /**
     * Returns a {@link String} which contains the results of calling
     * {@link String#valueOf(Object)} on each element of this stream in
     * encounter order.
     *
     * <p>
     * This is a terminal operation.
     * 
     * @return a {@code String}. For empty input stream empty String is
     *         returned.
     */
    public String joining() {
        return map(String::valueOf).collect(Collectors.joining());
    }

    /**
     * Returns a {@link String} which contains the results of calling
     * {@link String#valueOf(Object)} on each element of this stream, separated
     * by the specified delimiter, in encounter order.
     *
     * <p>
     * This is a terminal operation.
     * 
     * @param delimiter
     *            the delimiter to be used between each element
     * @return a {@code String}. For empty input stream empty String is
     *         returned.
     */
    public String joining(CharSequence delimiter) {
        return map(String::valueOf).collect(Collectors.joining(delimiter));
    }

    /**
     * Returns a {@link String} which contains the results of calling
     * {@link String#valueOf(Object)} on each element of this stream, separated
     * by the specified delimiter, with the specified prefix and suffix in
     * encounter order.
     *
     * <p>
     * This is a terminal operation.
     * 
     * @param delimiter
     *            the delimiter to be used between each element
     * @param prefix
     *            the sequence of characters to be used at the beginning of the
     *            joined result
     * @param suffix
     *            the sequence of characters to be used at the end of the joined
     *            result
     * @return a {@code String}. For empty input stream empty String is
     *         returned.
     */
    public String joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return map(String::valueOf).collect(Collectors.joining(delimiter, prefix, suffix));
    }

    /**
     * Returns a {@link Map} whose keys are elements from this stream and values
     * are the result of applying the provided mapping functions to the input
     * elements.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * If this stream contains duplicates (according to
     * {@link Object#equals(Object)}), an {@code IllegalStateException} is
     * thrown when the collection operation is performed.
     *
     * <p>
     * Returned {@code Map} is guaranteed to be modifiable.
     *
     * <p>
     * For parallel stream the concurrent {@code Map} is created.
     *
     * @param <V>
     *            the output type of the value mapping function
     * @param valMapper
     *            a mapping function to produce values
     * @return a {@code Map} whose keys are elements from this stream and values
     *         are the result of applying mapping function to the input elements
     *
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @see #toMap(Function, Function)
     */
    public <V> Map<T, V> toMap(Function<T, V> valMapper) {
        return toMap(Function.identity(), valMapper);
    }

    /**
     * Returns a {@link Map} whose keys and values are the result of applying
     * the provided mapping functions to the input elements.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), an {@code IllegalStateException} is
     * thrown when the collection operation is performed.
     * 
     * <p>
     * Returned {@code Map} is guaranteed to be modifiable.
     *
     * <p>
     * For parallel stream the concurrent {@code Map} is created.
     *
     * @param <K>
     *            the output type of the key mapping function
     * @param <V>
     *            the output type of the value mapping function
     * @param keyMapper
     *            a mapping function to produce keys
     * @param valMapper
     *            a mapping function to produce values
     * @return a {@code Map} whose keys and values are the result of applying
     *         mapping functions to the input elements
     *
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @see #toMap(Function)
     */
    public <K, V> Map<K, V> toMap(Function<T, K> keyMapper, Function<T, V> valMapper) {
        return toMap(keyMapper, valMapper, throwingMerger());
    }

    /**
     * Returns a {@link Map} whose keys and values are the result of applying
     * the provided mapping functions to the input elements.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), the value mapping function is applied to
     * each equal element, and the results are merged using the provided merging
     * function.
     *
     * <p>
     * For parallel stream the concurrent {@code Map} is created.
     *
     * <p>
     * Returned {@code Map} is guaranteed to be modifiable.
     *
     * @param <K>
     *            the output type of the key mapping function
     * @param <V>
     *            the output type of the value mapping function
     * @param keyMapper
     *            a mapping function to produce keys
     * @param valMapper
     *            a mapping function to produce values
     * @param mergeFunction
     *            a merge function, used to resolve collisions between values
     *            associated with the same key, as supplied to
     *            {@link Map#merge(Object, Object, BiFunction)}
     * @return a {@code Map} whose keys are the result of applying a key mapping
     *         function to the input elements, and whose values are the result
     *         of applying a value mapping function to all input elements equal
     *         to the key and combining them using the merge function
     *
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     * @see Collectors#toConcurrentMap(Function, Function, BinaryOperator)
     * @see #toMap(Function, Function)
     * @since 0.1.0
     */
    public <K, V> Map<K, V> toMap(Function<T, K> keyMapper, Function<T, V> valMapper, BinaryOperator<V> mergeFunction) {
        if (stream.isParallel())
            return collect(Collectors.toConcurrentMap(keyMapper, valMapper, mergeFunction, ConcurrentHashMap::new));
        return collect(Collectors.toMap(keyMapper, valMapper, mergeFunction, HashMap::new));
    }

    /**
     * Returns a {@link SortedMap} whose keys are elements from this stream and
     * values are the result of applying the provided mapping functions to the
     * input elements.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * If this stream contains duplicates (according to
     * {@link Object#equals(Object)}), an {@code IllegalStateException} is
     * thrown when the collection operation is performed.
     *
     * <p>
     * For parallel stream the concurrent {@code SortedMap} is created.
     *
     * <p>
     * Returned {@code SortedMap} is guaranteed to be modifiable.
     *
     * @param <V>
     *            the output type of the value mapping function
     * @param valMapper
     *            a mapping function to produce values
     * @return a {@code SortedMap} whose keys are elements from this stream and
     *         values are the result of applying mapping function to the input
     *         elements
     *
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @see #toSortedMap(Function, Function)
     * @since 0.1.0
     */
    public <V> SortedMap<T, V> toSortedMap(Function<T, V> valMapper) {
        return toSortedMap(Function.identity(), valMapper);
    }

    /**
     * Returns a {@link SortedMap} whose keys and values are the result of
     * applying the provided mapping functions to the input elements.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), an {@code IllegalStateException} is
     * thrown when the collection operation is performed.
     *
     * <p>
     * For parallel stream the concurrent {@code SortedMap} is created.
     *
     * <p>
     * Returned {@code SortedMap} is guaranteed to be modifiable.
     * 
     * @param <K>
     *            the output type of the key mapping function
     * @param <V>
     *            the output type of the value mapping function
     * @param keyMapper
     *            a mapping function to produce keys
     * @param valMapper
     *            a mapping function to produce values
     * @return a {@code SortedMap} whose keys and values are the result of
     *         applying mapping functions to the input elements
     *
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @see #toSortedMap(Function)
     * @since 0.1.0
     */
    public <K, V> SortedMap<K, V> toSortedMap(Function<T, K> keyMapper, Function<T, V> valMapper) {
        return toSortedMap(keyMapper, valMapper, throwingMerger());
    }

    /**
     * Returns a {@link SortedMap} whose keys and values are the result of
     * applying the provided mapping functions to the input elements.
     *
     * <p>
     * This is a terminal operation.
     * 
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), the value mapping function is applied to
     * each equal element, and the results are merged using the provided merging
     * function.
     *
     * <p>
     * For parallel stream the concurrent {@code SortedMap} is created.
     *
     * <p>
     * Returned {@code SortedMap} is guaranteed to be modifiable.
     *
     * @param <K>
     *            the output type of the key mapping function
     * @param <V>
     *            the output type of the value mapping function
     * @param keyMapper
     *            a mapping function to produce keys
     * @param valMapper
     *            a mapping function to produce values
     * @param mergeFunction
     *            a merge function, used to resolve collisions between values
     *            associated with the same key, as supplied to
     *            {@link Map#merge(Object, Object, BiFunction)}
     * @return a {@code SortedMap} whose keys are the result of applying a key
     *         mapping function to the input elements, and whose values are the
     *         result of applying a value mapping function to all input elements
     *         equal to the key and combining them using the merge function
     *
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     * @see Collectors#toConcurrentMap(Function, Function, BinaryOperator)
     * @see #toSortedMap(Function, Function)
     * @since 0.1.0
     */
    public <K, V> SortedMap<K, V> toSortedMap(Function<T, K> keyMapper, Function<T, V> valMapper,
            BinaryOperator<V> mergeFunction) {
        if (stream.isParallel())
            return collect(Collectors.toConcurrentMap(keyMapper, valMapper, mergeFunction, ConcurrentSkipListMap::new));
        return collect(Collectors.toMap(keyMapper, valMapper, mergeFunction, TreeMap::new));
    }

    /**
     * Returns a new {@code StreamEx} which is a concatenation of this stream
     * and the stream containing supplied values.
     * 
     * @param values
     *            the values to append to the stream
     * @return the new stream
     */
    @SuppressWarnings("unchecked")
    public StreamEx<T> append(T... values) {
        return append(Stream.of(values));
    }

    /**
     * Returns a new {@code StreamEx} which is a concatenation of this stream
     * and the stream created from supplied collection.
     * 
     * @param collection
     *            the collection to append to the stream
     * @return the new stream
     * @since 0.2.1
     */
    public StreamEx<T> append(Collection<T> collection) {
        return append(collection.stream());
    }

    /**
     * Returns a new {@code StreamEx} which is a concatenation of the stream
     * containing supplied values and this stream.
     * 
     * @param values
     *            the values to prepend to the stream
     * @return the new stream
     */
    @SuppressWarnings("unchecked")
    public StreamEx<T> prepend(T... values) {
        return prepend(Stream.of(values));
    }

    /**
     * Returns a new {@code StreamEx} which is a concatenation of the stream
     * created from supplied collection and this stream.
     * 
     * @param collection
     *            the collection to prepend to the stream
     * @return the new stream
     * @since 0.2.1
     */
    public StreamEx<T> prepend(Collection<T> collection) {
        return prepend(collection.stream());
    }

    /**
     * Returns true if this stream contains the specified value.
     *
     * <p>
     * This is a short-circuiting terminal operation.
     * 
     * @param value
     *            the value too look for in the stream. If the value is null
     *            then the method will return true if this stream contains at
     *            least one null. Otherwise {@code value.equals()} will be
     *            called to compare stream elements with the value.
     * @return true if this stream contains the specified value
     * @see Stream#anyMatch(Predicate)
     */
    public boolean has(T value) {
        if (value == null)
            return anyMatch(Objects::isNull);
        return anyMatch(value::equals);
    }

    /**
     * Returns a {@code StreamEx} consisting of the elements of this stream,
     * sorted according to reverse natural order. If the elements of this stream
     * are not {@code Comparable}, a {@link java.lang.ClassCastException} may be
     * thrown when the terminal operation is executed.
     *
     * <p>
     * For ordered streams, the sort is stable. For unordered streams, no
     * stability guarantees are made.
     *
     * <p>
     * This is a stateful intermediate operation.
     *
     * @return the new stream
     * @since 0.2.0
     */
    @SuppressWarnings("unchecked")
    public StreamEx<T> reverseSorted() {
        return sorted((Comparator<? super T>) Comparator.reverseOrder());
    }

    /**
     * Returns a stream consisting of the results of applying the given function
     * to the every adjacent pair of elements of this stream.
     *
     * <p>
     * This is an intermediate operation.
     * 
     * <p>
     * The output stream will contain one element less than this stream. If this
     * stream contains zero or one element the output stream will be empty.
     *
     * @param <R>
     *            The element type of the new stream
     * @param mapper
     *            a non-interfering, stateless function to apply to each
     *            adjacent pair of this stream elements.
     * @return the new stream
     * @since 0.2.1
     */
    public <R> StreamEx<R> pairMap(BiFunction<T, T, R> mapper) {
        return strategy().newStreamEx(
                StreamSupport.stream(
                        new PairSpliterator.PSOfRef<>(mapper, stream.spliterator(), null, false, null, false),
                        stream.isParallel()).onClose(stream::close));
    }

    /**
     * Performs an action for each adjacent pair of elements of this stream.
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
     *            a non-interfering action to perform on the elements
     * @since 0.2.2
     */
    public void forPairs(BiConsumer<T, T> action) {
        pairMap((a, b) -> {
            action.accept(a, b);
            return null;
        }).reduce(null, (a, b) -> null);
    }

    /**
     * Returns an empty sequential {@code StreamEx}.
     *
     * @param <T>
     *            the type of stream elements
     * @return an empty sequential stream
     */
    public static <T> StreamEx<T> empty() {
        return new StreamEx<>(Stream.empty());
    }

    /**
     * Returns a sequential {@code StreamEx} containing a single element.
     *
     * @param <T>
     *            the type of stream element
     * @param element
     *            the single element
     * @return a singleton sequential stream
     * @see Stream#of(Object)
     */
    public static <T> StreamEx<T> of(T element) {
        return new StreamEx<>(Stream.of(element));
    }

    /**
     * Returns a sequential ordered {@code StreamEx} whose elements are the
     * specified values.
     *
     * @param <T>
     *            the type of stream elements
     * @param elements
     *            the elements of the new stream
     * @return the new stream
     * @see Stream#of(Object...)
     */
    @SafeVarargs
    public static <T> StreamEx<T> of(T... elements) {
        return new StreamEx<>(Stream.of(elements));
    }

    /**
     * Returns a sequential {@link StreamEx} with the specified range of the
     * specified array as its source.
     *
     * @param <T>
     *            the type of stream elements
     * @param array
     *            the array, assumed to be unmodified during use
     * @param startInclusive
     *            the first index to cover, inclusive
     * @param endExclusive
     *            index immediately past the last index to cover
     * @return a {@code StreamEx} for the array range
     * @throws ArrayIndexOutOfBoundsException
     *             if {@code startInclusive} is negative, {@code endExclusive}
     *             is less than {@code startInclusive}, or {@code endExclusive}
     *             is greater than the array size
     * @since 0.1.1
     * @see Arrays#stream(Object[], int, int)
     */
    public static <T> StreamEx<T> of(T[] array, int startInclusive, int endExclusive) {
        return new StreamEx<>(Arrays.stream(array, startInclusive, endExclusive));
    }

    /**
     * Returns a sequential {@code StreamEx} with given collection as its
     * source.
     *
     * @param <T>
     *            the type of collection elements
     * @param collection
     *            collection to create the stream of
     * @return a sequential {@code StreamEx} over the elements in given
     *         collection
     * @see Collection#stream()
     */
    public static <T> StreamEx<T> of(Collection<T> collection) {
        return new StreamEx<>(collection.stream());
    }

    /**
     * Returns an {@link StreamEx} object which wraps given {@link Stream}
     * 
     * @param <T>
     *            the type of stream elements
     * @param stream
     *            original stream
     * @return the wrapped stream
     */
    @SuppressWarnings("unchecked")
    public static <T> StreamEx<T> of(Stream<T> stream) {
        return new StreamEx<>(stream instanceof AbstractStreamEx ? ((AbstractStreamEx<T, ?>) stream).stream : stream);
    }

    /**
     * Returns a sequential {@code StreamEx} containing an {@link Optional}
     * value, if present, otherwise returns an empty {@code StreamEx}.
     *
     * @param <T>
     *            the type of stream elements
     * @param optional
     *            the optional to create a stream of
     * @return a stream with an {@code Optional} value if present, otherwise an
     *         empty stream
     * @since 0.1.1
     */
    public static <T> StreamEx<T> of(Optional<T> optional) {
        return optional.isPresent() ? of(optional.get()) : empty();
    }

    /**
     * Returns a sequential {@code StreamEx} containing a single element, if
     * non-null, otherwise returns an empty {@code StreamEx}.
     *
     * @param element
     *            the single element
     * @param <T>
     *            the type of stream elements
     * @return a stream with a single element if the specified element is
     *         non-null, otherwise an empty stream
     * @since 0.1.1
     */
    public static <T> StreamEx<T> ofNullable(T element) {
        return element == null ? empty() : of(element);
    }

    /**
     * Returns a {@code StreamEx}, the elements of which are lines read from the
     * supplied {@link BufferedReader}. The {@code StreamEx} is lazily
     * populated, i.e., read only occurs during the terminal stream operation.
     *
     * <p>
     * The reader must not be operated on during the execution of the terminal
     * stream operation. Otherwise, the result of the terminal stream operation
     * is undefined.
     *
     * <p>
     * After execution of the terminal stream operation there are no guarantees
     * that the reader will be at a specific position from which to read the
     * next character or line.
     *
     * <p>
     * If an {@link IOException} is thrown when accessing the underlying
     * {@code BufferedReader}, it is wrapped in an {@link UncheckedIOException}
     * which will be thrown from the {@code StreamEx} method that caused the
     * read to take place. This method will return a StreamEx if invoked on a
     * BufferedReader that is closed. Any operation on that stream that requires
     * reading from the BufferedReader after it is closed, will cause an
     * UncheckedIOException to be thrown.
     *
     * @param reader
     *            the reader to get the lines from
     * @return a {@code StreamEx<String>} providing the lines of text described
     *         by this {@code BufferedReader}
     * @see BufferedReader#lines()
     */
    public static StreamEx<String> ofLines(BufferedReader reader) {
        return new StreamEx<>(reader.lines());
    }

    public static StreamEx<String> ofLines(Reader reader) {
        if (reader instanceof BufferedReader)
            return new StreamEx<>(((BufferedReader) reader).lines());
        return new StreamEx<>(new BufferedReader(reader).lines());
    }

    /**
     * Returns a sequential {@code StreamEx} with keySet of given {@link Map} as
     * its source.
     *
     * @param <T>
     *            the type of map keys
     * @param map
     *            input map
     * @return a sequential {@code StreamEx} over the keys of given {@code Map}
     * @throws NullPointerException
     *             if map is null
     * @see Map#keySet()
     */
    public static <T> StreamEx<T> ofKeys(Map<T, ?> map) {
        return new StreamEx<>(map.keySet().stream());
    }

    /**
     * Returns a sequential {@code StreamEx} of given {@link Map} keys which
     * corresponding values match the supplied filter.
     *
     * @param <T>
     *            the type of map keys and created stream elements
     * @param <V>
     *            the type of map values
     * @param map
     *            input map
     * @param valueFilter
     *            a predicate used to test values
     * @return a sequential {@code StreamEx} over the keys of given {@code Map}
     *         which corresponding values match the supplied filter.
     * @throws NullPointerException
     *             if map is null
     * @see Map#keySet()
     */
    public static <T, V> StreamEx<T> ofKeys(Map<T, V> map, Predicate<V> valueFilter) {
        return new StreamEx<>(map.entrySet().stream().filter(entry -> valueFilter.test(entry.getValue()))
                .map(Entry::getKey));
    }

    /**
     * Returns a sequential {@code StreamEx} with values of given {@link Map} as
     * its source.
     *
     * @param <T>
     *            the type of map keys
     * @param map
     *            input map
     * @return a sequential {@code StreamEx} over the values of given
     *         {@code Map}
     * @throws NullPointerException
     *             if map is null
     * @see Map#values()
     */
    public static <T> StreamEx<T> ofValues(Map<?, T> map) {
        return new StreamEx<>(map.values().stream());
    }

    /**
     * Returns a sequential {@code StreamEx} of given {@link Map} values which
     * corresponding keys match the supplied filter.
     *
     * @param <K>
     *            the type of map keys
     * @param <T>
     *            the type of map values and created stream elements
     * @param map
     *            input map
     * @param keyFilter
     *            a predicate used to test keys
     * @return a sequential {@code StreamEx} over the values of given
     *         {@code Map} which corresponding keys match the supplied filter.
     * @throws NullPointerException
     *             if map is null
     * @see Map#values()
     */
    public static <K, T> StreamEx<T> ofValues(Map<K, T> map, Predicate<K> keyFilter) {
        return new StreamEx<>(map.entrySet().stream().filter(entry -> keyFilter.test(entry.getKey()))
                .map(Entry::getValue));
    }

    public static StreamEx<? extends ZipEntry> ofEntries(ZipFile file) {
        return new StreamEx<>(file.stream());
    }

    public static StreamEx<JarEntry> ofEntries(JarFile file) {
        return new StreamEx<>(file.stream());
    }

    private static void generatePermutation(int length, long permutationNumber, IntBinaryOperator op) {
        BitSet usedIndices = new BitSet();
        for (int i = 0; i < length; i++) {
            int curIdx = (int) (permutationNumber % (length - i));
            permutationNumber /= (length - i);
            int freeIdx = -1;
            while (curIdx >= 0) {
                do {
                    freeIdx++;
                } while (usedIndices.get(freeIdx));
                curIdx--;
            }
            usedIndices.set(freeIdx);
            op.applyAsInt(i, freeIdx);
        }
    }

    private static final long[] factorials = new long[] { 1L, 1L, 2L, 6L, 24L, 120L, 720L, 5040L, 40320L, 362880L,
            3628800L, 39916800L, 479001600L, 6227020800L, 87178291200L, 1307674368000L, 20922789888000L,
            355687428096000L, 6402373705728000L, 121645100408832000L, 2432902008176640000L };

    private static final long getFactorial(int length) {
        if (length > factorials.length) {
            throw new IllegalArgumentException("Source array is longer than " + length
                    + "; too many permutations expected.");
        }
        long factorial = factorials[length];
        return factorial;
    }

    public static StreamEx<int[]> ofPermutations(int[] source) {
        int length = source.length;
        return LongStreamEx.range(getFactorial(length))
                .mapToObj(perm -> {
                    int[] arr = new int[length];
                    generatePermutation(length, perm, (pos, val) -> {arr[pos] = source[val]; return 0;});
                    return arr;
                }).unordered();
    }

    public static StreamEx<int[]> ofPermutations2(int[] source) {
        int length = source.length;
        return new StreamEx<>(StreamSupport.stream(new PermutationSpliterator(length), false).map(perm -> {
                    int[] arr = new int[length];
                    for(int i=0; i<length; i++) arr[i] = source[perm[i]];
                    return arr;
                }));
    }
    
    public static StreamEx<long[]> ofPermutations(long[] source) {
        int length = source.length;
        return LongStreamEx.range(getFactorial(length))
                .mapToObj(perm -> {
                    long[] arr = new long[length];
                    generatePermutation(length, perm, (pos, val) -> {arr[pos] = source[val]; return 0;});
                    return arr;
                }).unordered();
    }

    public static StreamEx<double[]> ofPermutations(double[] source) {
        int length = source.length;
        return LongStreamEx.range(getFactorial(length))
                .mapToObj(perm -> {
                    double[] arr = new double[length];
                    generatePermutation(length, perm, (pos, val) -> {arr[pos] = source[val]; return 0;});
                    return arr;
                }).unordered();
    }

    @SuppressWarnings("unchecked")
    public static <T> StreamEx<T[]> ofPermutations(T[] source) {
        int length = source.length;
        Class<?> componentType = source.getClass().getComponentType();
        return LongStreamEx
                .range(getFactorial(length))
                .mapToObj(perm -> {
                    T[] arr = (T[]) Array.newInstance(componentType, length);
                    generatePermutation(length, perm, (pos, val) -> {arr[pos] = source[val]; return 0;});
                    return arr;
                }).unordered();
    }

    /**
     * Creates a stream from the given input sequence around matches of the
     * given pattern.
     *
     * <p>
     * The stream returned by this method contains each substring of the input
     * sequence that is terminated by another subsequence that matches this
     * pattern or is terminated by the end of the input sequence. The substrings
     * in the stream are in the order in which they occur in the input. Trailing
     * empty strings will be discarded and not encountered in the stream.
     *
     * <p>
     * If the given pattern does not match any subsequence of the input then the
     * resulting stream has just one element, namely the input sequence in
     * string form.
     *
     * <p>
     * When there is a positive-width match at the beginning of the input
     * sequence then an empty leading substring is included at the beginning of
     * the stream. A zero-width match at the beginning however never produces
     * such empty leading substring.
     *
     * <p>
     * If the input sequence is mutable, it must remain constant during the
     * execution of the terminal stream operation. Otherwise, the result of the
     * terminal stream operation is undefined.
     *
     * @param str
     *            The character sequence to be split
     * @param pattern
     *            The pattern to use for splitting
     *
     * @return The stream of strings computed by splitting the input around
     *         matches of this pattern
     * @see Pattern#splitAsStream(CharSequence)
     */
    public static StreamEx<String> split(CharSequence str, Pattern pattern) {
        return new StreamEx<>(pattern.splitAsStream(str));
    }

    public static StreamEx<String> split(CharSequence str, String regex) {
        return new StreamEx<>(Pattern.compile(regex).splitAsStream(str));
    }

    /**
     * Returns an infinite sequential ordered {@code StreamEx} produced by
     * iterative application of a function {@code f} to an initial element
     * {@code seed}, producing a {@code StreamEx} consisting of {@code seed},
     * {@code f(seed)}, {@code f(f(seed))}, etc.
     *
     * <p>
     * The first element (position {@code 0}) in the {@code StreamEx} will be
     * the provided {@code seed}. For {@code n > 0}, the element at position
     * {@code n}, will be the result of applying the function {@code f} to the
     * element at position {@code n - 1}.
     *
     * @param <T>
     *            the type of stream elements
     * @param seed
     *            the initial element
     * @param f
     *            a function to be applied to to the previous element to produce
     *            a new element
     * @return a new sequential {@code StreamEx}
     * @see Stream#iterate(Object, UnaryOperator)
     */
    public static <T> StreamEx<T> iterate(final T seed, final UnaryOperator<T> f) {
        return new StreamEx<>(Stream.iterate(seed, f));
    }

    /**
     * Returns an infinite sequential unordered {@code StreamEx} where each
     * element is generated by the provided {@link Supplier}. This is suitable
     * for generating constant streams, streams of random elements, etc.
     *
     * @param <T>
     *            the type of stream elements
     * @param s
     *            the {@code Supplier} of generated elements
     * @return a new infinite sequential unordered {@code StreamEx}
     * @see Stream#generate(Supplier)
     */
    public static <T> StreamEx<T> generate(Supplier<T> s) {
        return new StreamEx<>(Stream.generate(s));
    }

    /**
     * Returns a sequential unordered {@code StreamEx} of given length which
     * elements are equal to supplied value.
     * 
     * @param <T>
     *            the type of stream elements
     * @param value
     *            the constant value
     * @param length
     *            the length of the stream
     * @return a new {@code StreamEx}
     * @since 0.1.2
     */
    public static <T> StreamEx<T> constant(T value, long length) {
        return new StreamEx<>(Stream.generate(() -> value).limit(length));
    }

    /**
     * Returns a sequential {@code StreamEx} containing the results of applying
     * the given function to the corresponding pairs of values in given two
     * lists.
     * 
     * <p>
     * The list values are accessed using {@link List#get(int)}, so the lists
     * should provide fast random access. The lists are assumed to be
     * unmodifiable during the stream operations.
     * 
     * @param <U>
     *            the type of the first list elements
     * @param <V>
     *            the type of the second list elements
     * @param <T>
     *            the type of the resulting stream elements
     * @param first
     *            the first list, assumed to be unmodified during use
     * @param second
     *            the second list, assumed to be unmodified during use
     * @param mapper
     *            a non-interfering, stateless function to apply to each pair of
     *            the corresponding list elements.
     * @return a new {@code StreamEx}
     * @throws IllegalArgumentException
     *             if length of the lists differs.
     * @since 0.2.1
     */
    public static <U, V, T> StreamEx<T> zip(List<U> first, List<V> second, BiFunction<U, V, T> mapper) {
        return intStreamForLength(first.size(), second.size()).mapToObj(i -> mapper.apply(first.get(i), second.get(i)));
    }

    /**
     * Returns a sequential {@code StreamEx} containing the results of applying
     * the given function to the corresponding pairs of values in given two
     * arrays.
     * 
     * @param <U>
     *            the type of the first array elements
     * @param <V>
     *            the type of the second array elements
     * @param <T>
     *            the type of the resulting stream elements
     * @param first
     *            the first array
     * @param second
     *            the second array
     * @param mapper
     *            a non-interfering, stateless function to apply to each pair of
     *            the corresponding array elements.
     * @return a new {@code StreamEx}
     * @throws IllegalArgumentException
     *             if length of the arrays differs.
     * @since 0.2.1
     */
    public static <U, V, T> StreamEx<T> zip(U[] first, V[] second, BiFunction<U, V, T> mapper) {
        return zip(Arrays.asList(first), Arrays.asList(second), mapper);
    }
}
