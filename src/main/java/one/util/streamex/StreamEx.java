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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Collector.Characteristics;

import one.util.streamex.PairSpliterator.PSOfRef;
import static one.util.streamex.StreamExInternals.*;

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
 * @param <T> the type of the stream elements
 */
public class StreamEx<T> extends AbstractStreamEx<T, StreamEx<T>> {

    StreamEx(Stream<? extends T> stream, StreamContext context) {
        super(stream, context);
    }

    StreamEx(Spliterator<? extends T> spliterator, StreamContext context) {
        super(spliterator, context);
    }

    @Override
    StreamEx<T> supply(Stream<T> stream) {
        return new StreamEx<>(stream, context);
    }

    @Override
    StreamEx<T> supply(Spliterator<T> spliterator) {
        return new StreamEx<>(spliterator, context);
    }

    final <R> StreamEx<R> collapseInternal(BiPredicate<? super T, ? super T> collapsible, Function<T, R> mapper,
            BiFunction<R, T, R> accumulator, BinaryOperator<R> combiner) {
        CollapseSpliterator<T, R> spliterator = new CollapseSpliterator<>(collapsible, mapper, accumulator, combiner,
                spliterator());
        return new StreamEx<>(spliterator, context);
    }

    private static <T> StreamEx<T> flatTraverse(Stream<T> src, Function<T, Stream<T>> streamProvider) {
        return StreamEx.of(src).flatMap(t -> {
            Stream<T> result = streamProvider.apply(t);
            return result == null ? Stream.of(t) : flatTraverse(result, streamProvider).prepend(t);
        });
    }

    /**
     * Returns a stream consisting of the elements of this stream which are
     * instances of given class.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <TT> a type of instances to select.
     * @param clazz a class which instances should be selected
     * @return the new stream
     */
    @SuppressWarnings("unchecked")
    public <TT> StreamEx<TT> select(Class<TT> clazz) {
        return (StreamEx<TT>) filter(clazz::isInstance);
    }

    /**
     * Returns an {@link EntryStream} consisting of the {@link Entry} objects
     * which keys are elements of this stream and values are results of applying
     * the given function to the elements of this stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <V> The {@code Entry} value type
     * @param valueMapper a non-interfering, stateless function to apply to each
     *        element
     * @return the new stream
     */
    public <V> EntryStream<T, V> mapToEntry(Function<? super T, ? extends V> valueMapper) {
        return new EntryStream<>(stream().map(e -> new SimpleImmutableEntry<>(e, valueMapper.apply(e))), context);
    }

    /**
     * Returns an {@link EntryStream} consisting of the {@link Entry} objects
     * which keys and values are results of applying the given functions to the
     * elements of this stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <K> The {@code Entry} key type
     * @param <V> The {@code Entry} value type
     * @param keyMapper a non-interfering, stateless function to apply to each
     *        element
     * @param valueMapper a non-interfering, stateless function to apply to each
     *        element
     * @return the new stream
     */
    public <K, V> EntryStream<K, V> mapToEntry(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        return new EntryStream<>(stream()
                .map(e -> new SimpleImmutableEntry<>(keyMapper.apply(e), valueMapper.apply(e))), context);
    }

    /**
     * Returns a stream where the first element is the replaced with the result
     * of applying the given function while the other elements are left intact.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a> with <a href="package-summary.html#TSO">tail-stream
     * optimization</a>.
     *
     * @param mapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to the first element
     * @return the new stream
     * @since 0.4.1
     */
    public StreamEx<T> mapFirst(Function<? super T, ? extends T> mapper) {
        return supply(new PairSpliterator.PSOfRef<>(mapper, spliterator(), true));
    }

    /**
     * Returns a stream where the first element is transformed using
     * {@code firstMapper} function and other elements are transformed using
     * {@code notFirstMapper} function.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     *
     * @param <R> The element type of the new stream element
     * @param firstMapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to the first element
     * @param notFirstMapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to all elements except the first one.
     * @return the new stream
     * @since 0.6.0
     * @see #mapFirst(Function)
     */
    public <R> StreamEx<R> mapFirstOrElse(Function<? super T, ? extends R> firstMapper,
            Function<? super T, ? extends R> notFirstMapper) {
        return new StreamEx<>(new PairSpliterator.PSOfRef<>(firstMapper, notFirstMapper, spliterator(), true), context);
    }

    /**
     * Returns a stream where the last element is the replaced with the result
     * of applying the given function while the other elements are left intact.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * <p>
     * The mapper function is called at most once. It could be not called at all
     * if the stream is empty or there is short-circuiting operation downstream.
     *
     * @param mapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to the last element
     * @return the new stream
     * @since 0.4.1
     */
    public StreamEx<T> mapLast(Function<? super T, ? extends T> mapper) {
        return supply(new PairSpliterator.PSOfRef<>(mapper, spliterator(), false));
    }

    /**
     * Returns a stream where the last element is transformed using
     * {@code lastMapper} function and other elements are transformed using
     * {@code notLastMapper} function.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     *
     * @param <R> The element type of the new stream element
     * @param notLastMapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to all elements except the last one.
     * @param lastMapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to the last element
     * @return the new stream
     * @since 0.6.0
     * @see #mapFirst(Function)
     */
    public <R> StreamEx<R> mapLastOrElse(Function<? super T, ? extends R> notLastMapper,
            Function<? super T, ? extends R> lastMapper) {
        return new StreamEx<>(new PairSpliterator.PSOfRef<>(lastMapper, notLastMapper, spliterator(), false), context);
    }

    /**
     * Returns a stream consisting of the elements of this stream, additionally
     * performing the provided action on the first stream element when it's
     * consumed from the resulting stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * <p>
     * The action is called at most once. For parallel stream pipelines, it's
     * not guaranteed in which thread it will be executed, so if it modifies
     * shared state, it is responsible for providing the required
     * synchronization.
     *
     * <p>
     * Note that the action might not be called at all if the first element is
     * not consumed from the input (for example, if there's short-circuiting
     * operation downstream which stopped the stream before the first element).
     * 
     * <p>
     * This method exists mainly to support debugging.
     *
     * @param action a <a href="package-summary.html#NonInterference">
     *        non-interfering</a> action to perform on the first stream element
     *        as it is consumed from the stream
     * @return the new stream
     * @since 0.6.0
     */
    public StreamEx<T> peekFirst(Consumer<? super T> action) {
        return mapFirst(x -> {
            action.accept(x);
            return x;
        });
    }

    /**
     * Returns a stream consisting of the elements of this stream, additionally
     * performing the provided action on the last stream element when it's
     * consumed from the resulting stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * <p>
     * The action is called at most once. For parallel stream pipelines, it's
     * not guaranteed in which thread it will be executed, so if it modifies
     * shared state, it is responsible for providing the required
     * synchronization.
     * 
     * <p>
     * Note that the action might not be called at all if the last element is
     * not consumed from the input (for example, if there's short-circuiting
     * operation downstream).
     * 
     * <p>
     * This method exists mainly to support debugging.
     *
     * @param action a <a href="package-summary.html#NonInterference">
     *        non-interfering</a> action to perform on the first stream element
     *        as it is consumed from the stream
     * @return the new stream
     * @since 0.6.0
     */
    public StreamEx<T> peekLast(Consumer<? super T> action) {
        return mapLast(x -> {
            action.accept(x);
            return x;
        });
    }

    /**
     * Creates a new {@code EntryStream} populated from entries of maps produced
     * by supplied mapper function which is applied to the every element of this
     * stream.
     * 
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     * 
     * @param <K> the type of {@code Map} keys.
     * @param <V> the type of {@code Map} values.
     * @param mapper a non-interfering, stateless function to apply to each
     *        element which produces a {@link Map} of the entries corresponding
     *        to the single element of the current stream. The mapper function
     *        may return null or empty {@code Map} if no mapping should
     *        correspond to some element.
     * @return the new {@code EntryStream}
     */
    public <K, V> EntryStream<K, V> flatMapToEntry(Function<? super T, ? extends Map<K, V>> mapper) {
        return new EntryStream<>(stream().flatMap(e -> {
            Map<K, V> s = mapper.apply(e);
            return s == null ? null : s.entrySet().stream();
        }), context);
    }

    /**
     * Performs a cross product of current stream with specified array of
     * elements. As a result the {@link EntryStream} is created whose keys are
     * elements of current stream and values are elements of the specified
     * array.
     * 
     * <p>
     * The resulting stream contains all the possible combinations of keys and
     * values.
     * 
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     * 
     * @param <V> the type of array elements
     * @param other the array to perform a cross product with
     * @return the new {@code EntryStream}
     * @throws NullPointerException if other is null
     * @since 0.2.3
     */
    @SuppressWarnings("unchecked")
    public <V> EntryStream<T, V> cross(V... other) {
        if (other.length == 0)
            return new EntryStream<>(Spliterators.emptySpliterator(), context);
        if (other.length == 1)
            return mapToEntry(e -> other[0]);
        return cross(t -> of(other));
    }

    /**
     * Performs a cross product of current stream with specified
     * {@link Collection} of elements. As a result the {@link EntryStream} is
     * created whose keys are elements of current stream and values are elements
     * of the specified collection.
     * 
     * <p>
     * The resulting stream contains all the possible combinations of keys and
     * values.
     * 
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     * 
     * @param <V> the type of collection elements
     * @param other the collection to perform a cross product with
     * @return the new {@code EntryStream}
     * @throws NullPointerException if other is null
     * @since 0.2.3
     */
    public <V> EntryStream<T, V> cross(Collection<? extends V> other) {
        if (other.isEmpty())
            return new EntryStream<>(Spliterators.emptySpliterator(), context);
        return cross(t -> of(other));
    }

    /**
     * Creates a new {@code EntryStream} whose keys are elements of current
     * stream and corresponding values are supplied by given function. Each
     * mapped stream is {@link java.util.stream.BaseStream#close() closed} after
     * its contents have been placed into this stream. (If a mapped stream is
     * {@code null} an empty stream is used, instead.)
     * 
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     * 
     * @param <V> the type of values.
     * @param mapper a non-interfering, stateless function to apply to each
     *        element which produces a stream of the values corresponding to the
     *        single element of the current stream.
     * @return the new {@code EntryStream}
     * @since 0.2.3
     */
    public <V> EntryStream<T, V> cross(Function<? super T, ? extends Stream<? extends V>> mapper) {
        return new EntryStream<>(stream().flatMap(a -> EntryStream.withKey(a, mapper.apply(a))), context);
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
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * @param <K> the type of the keys
     * @param classifier the classifier function mapping input elements to keys
     * @return a {@code Map} containing the results of the group-by operation
     *
     * @see #groupingBy(Function, Collector)
     * @see Collectors#groupingBy(Function)
     * @see Collectors#groupingByConcurrent(Function)
     */
    public <K> Map<K, List<T>> groupingBy(Function<? super T, ? extends K> classifier) {
        return groupingBy(classifier, Collectors.toList());
    }

    /**
     * Returns a {@code Map} whose keys are the values resulting from applying
     * the classification function to the input elements, and whose
     * corresponding values are the result of reduction of the input elements
     * which map to the associated key under the classification function.
     *
     * <p>
     * There are no guarantees on the type, mutability or serializability of the
     * {@code Map} objects returned.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * @param <K> the type of the keys
     * @param <D> the result type of the downstream reduction
     * @param classifier the classifier function mapping input elements to keys
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Map} containing the results of the group-by operation
     *
     * @see #groupingBy(Function)
     * @see Collectors#groupingBy(Function, Collector)
     * @see Collectors#groupingByConcurrent(Function, Collector)
     */
    public <K, D> Map<K, D> groupingBy(Function<? super T, ? extends K> classifier,
            Collector<? super T, ?, D> downstream) {
        if (isParallel() && downstream.characteristics().contains(Characteristics.UNORDERED))
            return rawCollect(Collectors.groupingByConcurrent(classifier, downstream));
        return rawCollect(Collectors.groupingBy(classifier, downstream));
    }

    /**
     * Returns a {@code Map} whose keys are the values resulting from applying
     * the classification function to the input elements, and whose
     * corresponding values are the result of reduction of the input elements
     * which map to the associated key under the classification function.
     *
     * <p>
     * The {@code Map} will be created using the provided factory function.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * @param <K> the type of the keys
     * @param <D> the result type of the downstream reduction
     * @param <M> the type of the resulting {@code Map}
     * @param classifier the classifier function mapping input elements to keys
     * @param mapFactory a function which, when called, produces a new empty
     *        {@code Map} of the desired type
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Map} containing the results of the group-by operation
     *
     * @see #groupingBy(Function)
     * @see Collectors#groupingBy(Function, Supplier, Collector)
     * @see Collectors#groupingByConcurrent(Function, Supplier, Collector)
     */
    @SuppressWarnings("unchecked")
    public <K, D, M extends Map<K, D>> M groupingBy(Function<? super T, ? extends K> classifier,
            Supplier<M> mapFactory, Collector<? super T, ?, D> downstream) {
        if (isParallel() && downstream.characteristics().contains(Characteristics.UNORDERED)
            && mapFactory.get() instanceof ConcurrentMap)
            return (M) rawCollect(Collectors.groupingByConcurrent(classifier,
                (Supplier<ConcurrentMap<K, D>>) mapFactory, downstream));
        return rawCollect(Collectors.groupingBy(classifier, mapFactory, downstream));
    }

    /**
     * Returns a {@code Map} whose keys are the values resulting from applying
     * the classification function to the input elements, and whose
     * corresponding values are the collections of the input elements which map
     * to the associated key under the classification function.
     *
     * <p>
     * There are no guarantees on the type, mutability or serializability of the
     * {@code Map} objects returned.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * @param <K> the type of the keys
     * @param <C> the type of the collection used in resulting {@code Map}
     *        values
     * @param classifier the classifier function mapping input elements to keys
     * @param collectionFactory a function which returns a new empty
     *        {@code Collection} which will be used to store the stream
     *        elements.
     * @return a {@code Map} containing the results of the group-by operation
     *
     * @see #groupingBy(Function, Collector)
     * @see Collectors#groupingBy(Function, Collector)
     * @see Collectors#groupingByConcurrent(Function, Collector)
     * @since 0.2.2
     */
    public <K, C extends Collection<T>> Map<K, C> groupingTo(Function<? super T, ? extends K> classifier,
            Supplier<C> collectionFactory) {
        return groupingBy(classifier, Collectors.toCollection(collectionFactory));
    }

    /**
     * Returns a {@code Map} whose keys are the values resulting from applying
     * the classification function to the input elements, and whose
     * corresponding values are the collections of the input elements which map
     * to the associated key under the classification function.
     *
     * <p>
     * The {@code Map} will be created using the provided factory function.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * @param <K> the type of the keys
     * @param <C> the type of the collection used in resulting {@code Map}
     *        values
     * @param <M> the type of the resulting {@code Map}
     * @param classifier the classifier function mapping input elements to keys
     * @param mapFactory a function which, when called, produces a new empty
     *        {@code Map} of the desired type
     * @param collectionFactory a function which returns a new empty
     *        {@code Collection} which will be used to store the stream
     *        elements.
     * @return a {@code Map} containing the results of the group-by operation
     *
     * @see #groupingTo(Function, Supplier)
     * @see Collectors#groupingBy(Function, Supplier, Collector)
     * @see Collectors#groupingByConcurrent(Function, Supplier, Collector)
     * @since 0.2.2
     */
    public <K, C extends Collection<T>, M extends Map<K, C>> M groupingTo(Function<? super T, ? extends K> classifier,
            Supplier<M> mapFactory, Supplier<C> collectionFactory) {
        return groupingBy(classifier, mapFactory, Collectors.toCollection(collectionFactory));
    }

    /**
     * Returns a {@code Map<Boolean, List<T>>} which contains two partitions of
     * the input elements according to a {@code Predicate}.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     *
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} returned.
     *
     * @param predicate a predicate used for classifying input elements
     * @return a {@code Map<Boolean, List<T>>} which {@link Boolean#TRUE} key is
     *         mapped to the list of the stream elements for which predicate is
     *         true and {@link Boolean#FALSE} key is mapped to the list of all
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
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation. The operation may short-circuit if the downstream collector is
     * <a
     * href="package-summary.html#ShortCircuitReduction">short-circuiting</a>.
     *
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} returned.
     *
     * @param <D> the result type of the downstream reduction
     * @param predicate a predicate used for classifying input elements
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Map<Boolean, List<T>>} which {@link Boolean#TRUE} key is
     *         mapped to the result of downstream {@code Collector} collecting
     *         the the stream elements for which predicate is true and
     *         {@link Boolean#FALSE} key is mapped to the result of downstream
     *         {@code Collector} collecting the other stream elements.
     *
     * @see #partitioningBy(Predicate)
     * @see Collectors#partitioningBy(Predicate, Collector)
     * @since 0.2.2
     */
    public <D> Map<Boolean, D> partitioningBy(Predicate<? super T> predicate, Collector<? super T, ?, D> downstream) {
        return collect(MoreCollectors.partitioningBy(predicate, downstream));
    }

    /**
     * Returns a {@code Map<Boolean, C>} which contains two partitions of the
     * input elements according to a {@code Predicate}.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     *
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} returned.
     *
     * @param <C> the type of {@code Collection} used as returned {@code Map}
     *        values.
     * @param predicate a predicate used for classifying input elements
     * @param collectionFactory a function which returns a new empty
     *        {@code Collection} which will be used to store the stream
     *        elements.
     * @return a {@code Map<Boolean, C>} which {@link Boolean#TRUE} key is
     *         mapped to the collection of the stream elements for which
     *         predicate is true and {@link Boolean#FALSE} key is mapped to the
     *         collection of all other stream elements.
     *
     * @see #partitioningBy(Predicate, Collector)
     * @see Collectors#partitioningBy(Predicate)
     * @since 0.2.2
     */
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
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * @return a {@code String}. For empty input stream empty String is
     *         returned.
     */
    public String joining() {
        return map(String::valueOf).rawCollect(Collectors.joining());
    }

    /**
     * Returns a {@link String} which contains the results of calling
     * {@link String#valueOf(Object)} on each element of this stream, separated
     * by the specified delimiter, in encounter order.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * @param delimiter the delimiter to be used between each element
     * @return a {@code String}. For empty input stream empty String is
     *         returned.
     */
    public String joining(CharSequence delimiter) {
        return map(String::valueOf).rawCollect(Collectors.joining(delimiter));
    }

    /**
     * Returns a {@link String} which contains the results of calling
     * {@link String#valueOf(Object)} on each element of this stream, separated
     * by the specified delimiter, with the specified prefix and suffix in
     * encounter order.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * @param delimiter the delimiter to be used between each element
     * @param prefix the sequence of characters to be used at the beginning of
     *        the joined result
     * @param suffix the sequence of characters to be used at the end of the
     *        joined result
     * @return a {@code String}. For empty input stream empty String is
     *         returned.
     */
    public String joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return map(String::valueOf).rawCollect(Collectors.joining(delimiter, prefix, suffix));
    }

    /**
     * Returns a collection created by provided supplier function which contains
     * all the elements of the collections generated by provided mapper from
     * each element of this stream.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * <p>
     * This method is equivalent to
     * {@code flatCollection(mapper).toCollection(supplier)}, but may work
     * faster.
     * 
     * @param <U> the type of the elements of the resulting collection
     * @param <C> the type of the resulting collection
     * @param mapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to each element which produces a
     *        {@link Collection} of new values
     * @param supplier a supplier for the resulting collection
     * @return the new collection.
     * @since 0.3.7
     */
    public <U, C extends Collection<U>> C toFlatCollection(Function<? super T, ? extends Collection<U>> mapper,
            Supplier<C> supplier) {
        return map(mapper).collect(supplier, Collection::addAll, Collection::addAll);
    }

    /**
     * Returns a {@link List} which contains all the elements of the collections
     * generated by provided mapper from each element of this stream. There are
     * no guarantees on the type, mutability, serializability, or thread-safety
     * of the {@code List} returned; if more control over the returned
     * {@code List} is required, use
     * {@link #toFlatCollection(Function, Supplier)}.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * <p>
     * This method is equivalent to {@code flatCollection(mapper).toList()}, but
     * may work faster.
     * 
     * @param <U> the type of the elements of the resulting collection
     * @param mapper a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to each element which produces a
     *        {@link Collection} of new values
     * @return the new list.
     * @since 0.3.7
     */
    public <U> List<U> toFlatList(Function<? super T, ? extends Collection<U>> mapper) {
        return toFlatCollection(mapper, ArrayList::new);
    }

    /**
     * Returns a {@link Map} whose keys are elements from this stream and values
     * are the result of applying the provided mapping functions to the input
     * elements.
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
     * @param <V> the output type of the value mapping function
     * @param valMapper a mapping function to produce values
     * @return a {@code Map} whose keys are elements from this stream and values
     *         are the result of applying mapping function to the input elements
     * @throws IllegalStateException if this stream contains duplicate objects
     *         (according to {@link Object#equals(Object)})
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @see #toMap(Function, Function)
     */
    public <V> Map<T, V> toMap(Function<? super T, ? extends V> valMapper) {
        return toMap(Function.identity(), valMapper);
    }

    /**
     * Returns a {@link Map} whose keys and values are the result of applying
     * the provided mapping functions to the input elements.
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
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valMapper a mapping function to produce values
     * @return a {@code Map} whose keys and values are the result of applying
     *         mapping functions to the input elements
     * @throws IllegalStateException if duplicate mapped key is found (according
     *         to {@link Object#equals(Object)})
     *
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @see #toMap(Function)
     */
    public <K, V> Map<K, V> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valMapper) {
        Map<K, V> map = isParallel() ? new ConcurrentHashMap<>() : new HashMap<>();
        return toMapThrowing(keyMapper, valMapper, map);
    }

    /**
     * Returns a {@link Map} whose keys and values are the result of applying
     * the provided mapping functions to the input elements.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), the value mapping function is applied to
     * each equal element, and the results are merged using the provided merging
     * function.
     *
     * <p>
     * Returned {@code Map} is guaranteed to be modifiable.
     *
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *        values associated with the same key, as supplied to
     *        {@link Map#merge(Object, Object, BiFunction)}
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
    public <K, V> Map<K, V> toMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valMapper, BinaryOperator<V> mergeFunction) {
        return rawCollect(Collectors.toMap(keyMapper, valMapper, mergeFunction, HashMap::new));
    }

    /**
     * Returns a {@link SortedMap} whose keys are elements from this stream and
     * values are the result of applying the provided mapping functions to the
     * input elements.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
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
     * @param <V> the output type of the value mapping function
     * @param valMapper a mapping function to produce values
     * @return a {@code SortedMap} whose keys are elements from this stream and
     *         values are the result of applying mapping function to the input
     *         elements
     *
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @see #toSortedMap(Function, Function)
     * @since 0.1.0
     */
    public <V> SortedMap<T, V> toSortedMap(Function<? super T, ? extends V> valMapper) {
        return toSortedMap(Function.identity(), valMapper);
    }

    /**
     * Returns a {@link SortedMap} whose keys and values are the result of
     * applying the provided mapping functions to the input elements.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
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
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valMapper a mapping function to produce values
     * @return a {@code SortedMap} whose keys and values are the result of
     *         applying mapping functions to the input elements
     *
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @see #toSortedMap(Function)
     * @since 0.1.0
     */
    public <K, V> SortedMap<K, V> toSortedMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valMapper) {
        SortedMap<K, V> map = isParallel() ? new ConcurrentSkipListMap<>() : new TreeMap<>();
        return toMapThrowing(keyMapper, valMapper, map);
    }

    /**
     * Returns a {@link SortedMap} whose keys and values are the result of
     * applying the provided mapping functions to the input elements.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), the value mapping function is applied to
     * each equal element, and the results are merged using the provided merging
     * function.
     *
     * <p>
     * Returned {@code SortedMap} is guaranteed to be modifiable.
     *
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *        values associated with the same key, as supplied to
     *        {@link Map#merge(Object, Object, BiFunction)}
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
    public <K, V> SortedMap<K, V> toSortedMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valMapper, BinaryOperator<V> mergeFunction) {
        return rawCollect(Collectors.toMap(keyMapper, valMapper, mergeFunction, TreeMap::new));
    }

    /**
     * Returns a new {@code StreamEx} which is a concatenation of this stream
     * and the supplied values.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * <p>
     * May return this if no values are supplied.
     * 
     * @param values the values to append to the stream
     * @return the new stream
     */
    @SafeVarargs
    public final StreamEx<T> append(T... values) {
        return appendSpliterator(null, Spliterators.spliterator(values, Spliterator.ORDERED));
    }

    /**
     * Returns a new {@code StreamEx} which is a concatenation of this stream
     * and the supplied value.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a> with <a href="package-summary.html#TSO">tail-stream
     * optimization</a>.
     * 
     * @param value the value to append to the stream
     * @return the new stream
     * @since 0.5.4
     */
    public StreamEx<T> append(T value) {
        return appendSpliterator(null, new ConstSpliterator.OfRef<>(value, 1, true));
    }

    /**
     * Returns a new {@code StreamEx} which is a concatenation of this stream
     * and the stream created from supplied collection.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * <p>
     * May return this if the supplied collection is empty and non-concurrent.
     * 
     * @param collection the collection to append to the stream
     * @return the new stream
     * @since 0.2.1
     */
    public StreamEx<T> append(Collection<? extends T> collection) {
        return appendSpliterator(null, collection.spliterator());
    }

    /**
     * Returns a new {@code StreamEx} which is a concatenation of supplied
     * values and this stream.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a> with <a href="package-summary.html#TSO">tail-stream
     * optimization</a>.
     * 
     * <p>
     * May return this if no values are supplied.
     * 
     * @param values the values to prepend to the stream
     * @return the new stream
     */
    @SafeVarargs
    public final StreamEx<T> prepend(T... values) {
        return prependSpliterator(null, Spliterators.spliterator(values, Spliterator.ORDERED));
    }

    /**
     * Returns a new {@code StreamEx} which is a concatenation of supplied value
     * and this stream.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a> with <a href="package-summary.html#TSO">tail-stream
     * optimization</a>.
     * 
     * @param value the value to prepend to the stream
     * @return the new stream
     * @since 0.5.4
     */
    public StreamEx<T> prepend(T value) {
        return new StreamEx<>(new PrependSpliterator<>(spliterator(), value), context);
    }

    /**
     * Returns a new {@code StreamEx} which is a concatenation of the stream
     * created from supplied collection and this stream.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a> with <a href="package-summary.html#TSO">tail-stream
     * optimization</a>.
     * 
     * <p>
     * May return this if the supplied collection is empty and non-concurrent.
     * 
     * @param collection the collection to prepend to the stream
     * @return the new stream
     * @since 0.2.1
     */
    public StreamEx<T> prepend(Collection<? extends T> collection) {
        return prependSpliterator(null, collection.spliterator());
    }

    /**
     * Returns true if this stream contains the specified value.
     *
     * <p>
     * This is a short-circuiting <a
     * href="package-summary.html#StreamOps">terminal</a> operation.
     * 
     * @param value the value to look for in the stream. If the value is null
     *        then the method will return true if this stream contains at least
     *        one null. Otherwise {@code value.equals()} will be called to
     *        compare stream elements with the value.
     * @return true if this stream contains the specified value
     * @see Stream#anyMatch(Predicate)
     */
    public boolean has(T value) {
        return anyMatch(Predicate.isEqual(value));
    }

    /**
     * Returns a stream consisting of the elements of this stream that don't
     * equal to the given value.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param value the value to remove from the stream. If the value is null
     *        then all nulls will be removed (like {@link #nonNull()} works).
     *        Otherwise {@code value.equals()} will be used to test stream
     *        values and matching elements will be removed.
     * @return the new stream
     * @since 0.2.2
     * @see #without(Object...)
     * @see #remove(Predicate)
     */
    public StreamEx<T> without(T value) {
        if (value == null)
            return filter(Objects::nonNull);
        return remove(value::equals);
    }

    /**
     * Returns a stream consisting of the elements of this stream that don't
     * equal to any of the supplied values.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation. May return itself if no values were supplied.
     * 
     * <p>
     * Current implementation scans the supplied values linearly for every
     * stream element. If you have many values, consider using more efficient
     * alternative instead. For example,
     * {@code remove(StreamEx.of(values).toSet()::contains)}.
     * 
     * <p>
     * Future implementations may take advantage on using {@code hashCode()} or
     * {@code compareTo} for {@code Comparable} objects to improve the
     * performance.
     * 
     * <p>
     * If the {@code values} array is changed between calling this method and
     * finishing the stream traversal, then the result of the stream traversal
     * is undefined: changes may or may not be taken into account.
     *
     * @param values the values to remove from the stream.
     * @return the new stream
     * @since 0.5.5
     * @see #without(Object)
     * @see #remove(Predicate)
     */
    @SafeVarargs
    public final StreamEx<T> without(T... values) {
        if (values.length == 0)
            return this;
        if (values.length == 1)
            return without(values[0]);
        return remove(Arrays.asList(values)::contains);
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
     * This is a stateful <a
     * href="package-summary.html#StreamOps">intermediate</a> operation.
     *
     * @return the new stream
     * @since 0.2.0
     */
    @SuppressWarnings("unchecked")
    public StreamEx<T> reverseSorted() {
        return sorted((Comparator<? super T>) Comparator.reverseOrder());
    }

    /**
     * Returns a {@code StreamEx} consisting of the distinct elements (according
     * to {@link Object#equals(Object)}) which appear at least specified number
     * of times in this stream.
     *
     * <p>
     * This operation is not guaranteed to be stable: any of equal elements can
     * be selected for the output. However if this stream is ordered then order
     * is preserved.
     *
     * <p>
     * This is a stateful <a
     * href="package-summary.html#StreamOps">quasi-intermediate</a> operation.
     *
     * @param atLeast minimal number of occurrences required to select the
     *        element. If atLeast is 1 or less, then this method is equivalent
     *        to {@link #distinct()}.
     * @return the new stream
     * @see #distinct()
     * @since 0.3.1
     */
    public StreamEx<T> distinct(long atLeast) {
        if (atLeast <= 1)
            return distinct();
        Spliterator<T> spliterator = spliterator();
        Spliterator<T> result;
        if (spliterator.hasCharacteristics(Spliterator.DISTINCT))
            // already distinct: cannot have any repeating elements
            result = Spliterators.emptySpliterator();
        else
            result = new DistinctSpliterator<>(spliterator, atLeast);
        return supply(result);
    }

    /**
     * Returns a stream consisting of the results of applying the given function
     * to the every adjacent pair of elements of this stream.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * operation.
     * 
     * <p>
     * The output stream will contain one element less than this stream. If this
     * stream contains zero or one element the output stream will be empty.
     *
     * @param <R> The element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each
     *        adjacent pair of this stream elements.
     * @return the new stream
     * @since 0.2.1
     */
    public <R> StreamEx<R> pairMap(BiFunction<? super T, ? super T, ? extends R> mapper) {
        PSOfRef<T, R> spliterator = new PairSpliterator.PSOfRef<>(mapper, spliterator());
        return new StreamEx<>(spliterator, context);
    }

    /**
     * Performs an action for each adjacent pair of elements of this stream.
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
     * @param action a non-interfering action to perform on the elements
     * @since 0.2.2
     */
    public void forPairs(BiConsumer<? super T, ? super T> action) {
        pairMap((a, b) -> {
            action.accept(a, b);
            return null;
        }).reduce(null, selectFirst());
    }

    /**
     * Merge series of adjacent elements which satisfy the given predicate using
     * the merger function and return a new stream.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * partial reduction operation.
     * 
     * <p>
     * This operation is equivalent to
     * {@code collapse(collapsible, Collectors.reducing(merger)).map(Optional::get)}
     * , but more efficient.
     * 
     * @param collapsible a non-interfering, stateless predicate to apply to the
     *        pair of adjacent elements of the input stream which returns true
     *        for elements which are collapsible.
     * @param merger a non-interfering, stateless, associative function to merge
     *        two adjacent elements for which collapsible predicate returned
     *        true. Note that it can be applied to the results if previous
     *        merges.
     * @return the new stream
     * @since 0.3.1
     */
    public StreamEx<T> collapse(BiPredicate<? super T, ? super T> collapsible, BinaryOperator<T> merger) {
        return collapseInternal(collapsible, Function.identity(), merger, merger);
    }

    /**
     * Perform a partial mutable reduction using the supplied {@link Collector}
     * on a series of adjacent elements.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * partial reduction operation.
     * 
     * @param <R> the type of the elements in the resulting stream
     * @param <A> the intermediate accumulation type of the {@code Collector}
     * @param collapsible a non-interfering, stateless predicate to apply to the
     *        pair of adjacent elements of the input stream which returns true
     *        for elements which should be collected together.
     * @param collector a {@code Collector} which is used to combine the
     *        adjacent elements.
     * @return the new stream
     * @since 0.3.6
     */
    public <R, A> StreamEx<R> collapse(BiPredicate<? super T, ? super T> collapsible,
            Collector<? super T, A, R> collector) {
        Supplier<A> supplier = collector.supplier();
        BiConsumer<A, ? super T> accumulator = collector.accumulator();
        StreamEx<A> stream = collapseInternal(collapsible, t -> {
            A acc = supplier.get();
            accumulator.accept(acc, t);
            return acc;
        }, (acc, t) -> {
            accumulator.accept(acc, t);
            return acc;
        }, collector.combiner());
        if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            @SuppressWarnings("unchecked")
            StreamEx<R> result = (StreamEx<R>) stream;
            return result;
        }
        return stream.map(collector.finisher());
    }

    /**
     * Returns a stream consisting of elements of this stream where every series
     * of elements matched the predicate is replaced with first element from the
     * series.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * partial reduction operation.
     * 
     * <p>
     * This operation is equivalent to
     * {@code collapse(collapsible, MoreCollectors.first()).map(Optional::get)}
     * , but more efficient.
     * 
     * <p>
     * Note that this operation always tests the adjacent pairs of input
     * elements. In some scenarios it's desired to test every element with the
     * first element of the current series. In this case consider using
     * {@link MoreCollectors#dominators(BiPredicate)} collector instead.
     * 
     * <p>
     * For sorted stream {@code collapse(Objects::equals)} is equivalent to
     * {@code distinct()}.
     * 
     * @param collapsible a non-interfering, stateless predicate to apply to the
     *        pair of adjacent input elements which returns true for elements
     *        which are collapsible.
     * @return the new stream
     * @see MoreCollectors#dominators(BiPredicate)
     * @since 0.3.1
     */
    public StreamEx<T> collapse(BiPredicate<? super T, ? super T> collapsible) {
        return collapse(collapsible, selectFirst());
    }

    /**
     * Collapses adjacent equal elements and returns an {@link EntryStream}
     * where keys are input elements and values specify how many elements were
     * collapsed.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * partial reduction operation.
     * 
     * <p>
     * For sorted input {@code runLengths().toMap()} is the same as
     * {@code groupingBy(Function.identity(), Collectors.counting())}, but may
     * perform faster. For unsorted input the resulting stream may contain
     * repeating keys.
     * 
     * @return the new stream
     * @since 0.3.3
     */
    public EntryStream<T, Long> runLengths() {
        return new EntryStream<>(collapseInternal(Objects::equals, t -> new ObjLongBox<>(t, 1L), (acc, t) -> {
            acc.b++;
            return acc;
        }, (e1, e2) -> {
            e1.b += e2.b;
            return e1;
        }), context);
    }

    /**
     * Returns a stream consisting of lists of elements of this stream where
     * adjacent elements are grouped according to supplied predicate.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * partial reduction operation.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} objects of the resulting stream.
     * 
     * <p>
     * This operation is equivalent to
     * {@code collapse(sameGroup, Collectors.toList())}, but more efficient.
     * 
     * @param sameGroup a non-interfering, stateless predicate to apply to the
     *        pair of adjacent elements which returns true for elements which
     *        belong to the same group.
     * @return the new stream
     * @since 0.3.1
     */
    public StreamEx<List<T>> groupRuns(BiPredicate<? super T, ? super T> sameGroup) {
        return collapseInternal(sameGroup, Collections::singletonList, (acc, t) -> {
            if (!(acc instanceof ArrayList)) {
                T old = acc.get(0);
                acc = new ArrayList<>();
                acc.add(old);
            }
            acc.add(t);
            return acc;
        }, (acc1, acc2) -> {
            if (!(acc1 instanceof ArrayList)) {
                T old = acc1.get(0);
                acc1 = new ArrayList<>();
                acc1.add(old);
            }
            acc1.addAll(acc2);
            return acc1;
        });
    }

    /**
     * Returns a stream consisting of results of applying the given function to
     * the intervals created from the source elements.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * partial reduction operation. This operation is the same as
     * {@code groupRuns(sameInterval).map(list -> mapper.apply(list.get(0), list.get(list.size()-1)))}
     * , but has less overhead as only first and last elements of each interval
     * are tracked.
     * 
     * @param <U> the type of the resulting elements
     * @param sameInterval a non-interfering, stateless predicate to apply to
     *        the pair of adjacent elements which returns true for elements
     *        which belong to the same interval.
     * @param mapper a non-interfering, stateless function to apply to the
     *        interval borders and produce the resulting element. If value was
     *        not merged to the interval, then mapper will receive the same
     *        value twice, otherwise it will receive the leftmost and the
     *        rightmost values which were merged to the interval. Intermediate
     *        interval elements are not available to the mapper. If they are
     *        important, consider using {@link #groupRuns(BiPredicate)} and map
     *        afterwards.
     * @return the new stream
     * @see #collapse(BiPredicate, BinaryOperator)
     * @see #groupRuns(BiPredicate)
     * @since 0.3.3
     */
    public <U> StreamEx<U> intervalMap(BiPredicate<? super T, ? super T> sameInterval,
            BiFunction<? super T, ? super T, ? extends U> mapper) {
        return collapseInternal(sameInterval, PairBox::single, (box, t) -> {
            box.b = t;
            return box;
        }, (left, right) -> {
            left.b = right.b;
            return left;
        }).map(pair -> mapper.apply(pair.a, pair.b));
    }

    /**
     * Returns a stream consisting of the results of applying the given function
     * to the the first element and every other element of this stream.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * <p>
     * The size of the resulting stream is one element less than the input
     * stream. If the input stream is empty or contains just one element, then
     * the output stream will be empty.
     *
     * @param <R> The element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to the first
     *        stream element and every other element
     * @return the new stream
     * @see #withFirst()
     * @see #headTail(BiFunction)
     * @since 0.5.3
     */
    public <R> StreamEx<R> withFirst(BiFunction<? super T, ? super T, ? extends R> mapper) {
        WithFirstSpliterator<T, R> spliterator = new WithFirstSpliterator<>(spliterator(), mapper);
        return new StreamEx<>(spliterator, context);
    }

    /**
     * Creates an {@link EntryStream} consisting of the {@link Entry} objects
     * which keys are all the same and equal to the first element of this stream
     * and values are the rest elements of this stream.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * <p>
     * The size of the resulting stream is one element less than the input
     * stream. If the input stream is empty or contains just one element, then
     * the output stream will be empty.
     *
     * @return the new stream
     * @see #withFirst(BiFunction)
     * @see #headTail(BiFunction)
     * @since 0.5.3
     */
    public EntryStream<T, T> withFirst() {
        WithFirstSpliterator<T, Entry<T, T>> spliterator = new WithFirstSpliterator<>(spliterator(),
                AbstractMap.SimpleImmutableEntry<T, T>::new);
        return new EntryStream<>(spliterator, context);
    }

    /**
     * Creates a new {@link StreamEx} which is the result of applying of the
     * mapper {@code BiFunction} to the corresponding elements of this stream
     * and the supplied other stream. The resulting stream is ordered if both of
     * the input streams are ordered, and parallel if either of the input
     * streams is parallel. When the resulting stream is closed, the close
     * handlers for both input streams are invoked.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * <p>
     * The resulting stream finishes when either of the input streams finish:
     * the rest of the longer stream is discarded. It's unspecified whether the
     * rest elements of the longer stream are actually consumed.
     * 
     * <p>
     * The stream created by this operation may have poor characteristics and
     * parallelize badly, so it should be used only when there's no other
     * choice. If both input streams are random-access lists or arrays, consider
     * using {@link #zip(List, List, BiFunction)} or
     * {@link #zip(Object[], Object[], BiFunction)} respectively. If you want to
     * zip the stream with the stream of indices, consider using
     * {@link EntryStream#of(List)} instead.
     * 
     * @param <V> the type of the other stream elements
     * @param <R> the type of the resulting stream elements
     * @param other the stream to zip this stream with
     * @param mapper a non-interfering, stateless function to apply to the
     *        corresponding pairs of this stream and other stream elements
     * @return the new stream
     * @since 0.5.5
     * @see #zipWith(Stream)
     */
    public <V, R> StreamEx<R> zipWith(Stream<V> other, BiFunction<? super T, ? super V, ? extends R> mapper) {
        return new StreamEx<>(new ZipSpliterator<>(spliterator(), other.spliterator(), mapper, true), context
                .combine(other));
    }

    /**
     * Creates a new {@link EntryStream} which keys are elements of this stream
     * and values are the corresponding elements of the supplied other stream.
     * The resulting stream is ordered if both of the input streams are ordered,
     * and parallel if either of the input streams is parallel. When the
     * resulting stream is closed, the close handlers for both input streams are
     * invoked.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * <p>
     * The resulting stream finishes when either of the input streams finish:
     * the rest of the longer stream is discarded. It's unspecified whether the
     * rest elements of the longer stream are actually consumed.
     * 
     * <p>
     * The stream created by this operation may have poor characteristics and
     * parallelize badly, so it should be used only when there's no other
     * choice. If both input streams are random-access lists or arrays, consider
     * using {@link EntryStream#zip(List, List)} or
     * {@link EntryStream#zip(Object[], Object[])} respectively. If you want to
     * zip the stream with the stream of indices, consider using
     * {@link EntryStream#of(List)} instead.
     * 
     * @param <V> the type of the other stream elements
     * @param other the stream to zip this stream with
     * @return the new stream
     * @see #zipWith(Stream, BiFunction)
     * @since 0.5.5
     */
    public <V> EntryStream<T, V> zipWith(Stream<V> other) {
        return new EntryStream<>(new ZipSpliterator<>(spliterator(), other.spliterator(),
                AbstractMap.SimpleImmutableEntry<T, V>::new, true), context.combine(other));
    }

    /**
     * Creates a new Stream which is the result of applying of the mapper
     * {@code BiFunction} to the first element of the current stream (head) and
     * the stream containing the rest elements (tail). The mapper may return
     * {@code null} instead of empty stream.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a> with <a href="package-summary.html#TSO">tail-stream
     * optimization</a>.
     * 
     * <p>
     * The mapper function is not applied when the input stream is empty. This
     * operation is equivalent to {@code headTail(mapper, () -> null)}.
     * Otherwise it's applied at most once during the stream terminal operation
     * execution. Sometimes it's useful to generate stream recursively like
     * this:
     * 
     * <pre>{@code
     * // Returns lazily-generated stream which performs scanLeft operation on the input
     * static <T> StreamEx<T> scanLeft(StreamEx<T> input, BinaryOperator<T> operator) {
     *     return input.headTail((head, tail) -> 
     *         scanLeft(tail.mapFirst(cur -> operator.apply(head, cur)), operator).prepend(head));
     * }}</pre>
     * 
     * <p>
     * When possible, use tail-stream optimized operations to reduce the call
     * stack depth. In particular, the example shown above uses only
     * {@code headTail()}, {@link #mapFirst(Function)} and
     * {@link #prepend(Object...)} operations, all of them are tail-stream
     * optimized, so it will not fail with {@code StackOverflowError} on long
     * input stream.
     * 
     * <p>
     * This operation might perform badly with parallel streams. Sometimes the
     * same semantics could be expressed using other operations like
     * {@link #withFirst(BiFunction)} or {@link #mapFirst(Function)} which
     * parallelize better. Consider using these methods if its possible in your
     * case.
     *
     * @param <R> The element type of the new stream
     * @param mapper a <a
     *        href="package-summary.html#NonInterference">non-interfering</a>
     *        function to apply to the first stream element and the stream of
     *        the rest elements which creates a new stream.
     * @return the new stream
     * @see #withFirst()
     * @since 0.5.3
     */
    public <R> StreamEx<R> headTail(BiFunction<? super T, ? super StreamEx<T>, ? extends Stream<R>> mapper) {
        return headTail(mapper, () -> null);
    }

    /**
     * Creates a new Stream which is the result of applying of the mapper
     * {@code BiFunction} to the first element of the current stream (head) and
     * the stream containing the rest elements (tail) or supplier if the current
     * stream is empty. The mapper or supplier may return {@code null} instead
     * of empty stream.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a> with <a href="package-summary.html#TSO">tail-stream
     * optimization</a>.
     * 
     * <p>
     * Either mapper function or supplier (but not both) is applied at most once
     * during the stream terminal operation execution. Sometimes it's useful to
     * generate stream recursively like this:
     * 
     * <pre>{@code
     * // Stream of fixed size batches
     * static <T> StreamEx<List<T>> batches(StreamEx<T> input, int size) {
     *     return batches(input, size, Collections.emptyList());
     * }
     *
     * private static <T> StreamEx<List<T>> batches(StreamEx<T> input, int size, List<T> cur) {
     *     return input.headTail((head, tail) -> cur.size() >= size 
     *             ? batches(tail, size, Arrays.asList(head)).prepend(cur)
     *             : batches(tail, size, StreamEx.of(cur).append(head).toList()), 
     *             () -> Stream.of(cur));
     * }}</pre>
     * 
     * <p>
     * When possible, use tail-stream optimized operations to reduce the call
     * stack depth. In particular, the example shown above uses only
     * {@code headTail()}, and {@link #prepend(Object...)} operations, both of
     * them are tail-stream optimized, so it will not fail with
     * {@code StackOverflowError} on long input stream.
     * 
     * <p>
     * This operation might perform badly with parallel streams. Sometimes the
     * same semantics could be expressed using other operations like
     * {@link #withFirst(BiFunction)} or {@link #mapFirst(Function)} which
     * parallelize better. Consider using these methods if its possible in your
     * case.
     *
     * @param <R> The element type of the new stream
     * @param mapper a <a
     *        href="package-summary.html#NonInterference">non-interfering</a>
     *        function to apply to the first stream element and the stream of
     *        the rest elements which creates a new stream.
     * @param supplier a <a
     *        href="package-summary.html#NonInterference">non-interfering</a>
     *        supplier which creates a resulting stream when this stream is
     *        empty.
     * @return the new stream
     * @see #headTail(BiFunction)
     * @since 0.5.3
     */
    public <R> StreamEx<R> headTail(BiFunction<? super T, ? super StreamEx<T>, ? extends Stream<R>> mapper,
            Supplier<? extends Stream<R>> supplier) {
        HeadTailSpliterator<T, R> spliterator = new HeadTailSpliterator<>(spliterator(), mapper, supplier);
        spliterator.context = context = context.detach();
        return new StreamEx<>(spliterator, context);
    }

    /**
     * Returns an empty sequential {@code StreamEx}.
     *
     * @param <T> the type of stream elements
     * @return an empty sequential stream
     */
    public static <T> StreamEx<T> empty() {
        return of(Spliterators.emptySpliterator());
    }

    /**
     * Returns a sequential {@code StreamEx} containing a single element.
     *
     * @param <T> the type of stream element
     * @param element the single element
     * @return a singleton sequential stream
     * @see Stream#of(Object)
     */
    public static <T> StreamEx<T> of(T element) {
        return of(new ConstSpliterator.OfRef<>(element, 1, true));
    }

    /**
     * Returns a sequential ordered {@code StreamEx} whose elements are the
     * specified values.
     *
     * @param <T> the type of stream elements
     * @param elements the elements of the new stream
     * @return the new stream
     * @see Stream#of(Object...)
     */
    @SafeVarargs
    public static <T> StreamEx<T> of(T... elements) {
        return of(Arrays.spliterator(elements));
    }

    /**
     * Returns a sequential {@link StreamEx} with the specified range of the
     * specified array as its source.
     *
     * @param <T> the type of stream elements
     * @param array the array, assumed to be unmodified during use
     * @param startInclusive the first index to cover, inclusive
     * @param endExclusive index immediately past the last index to cover
     * @return a {@code StreamEx} for the array range
     * @throws ArrayIndexOutOfBoundsException if {@code startInclusive} is
     *         negative, {@code endExclusive} is less than
     *         {@code startInclusive}, or {@code endExclusive} is greater than
     *         the array size
     * @since 0.1.1
     * @see Arrays#stream(Object[], int, int)
     */
    public static <T> StreamEx<T> of(T[] array, int startInclusive, int endExclusive) {
        return of(Arrays.spliterator(array, startInclusive, endExclusive));
    }

    /**
     * Returns a sequential {@code StreamEx} with given collection as its
     * source.
     *
     * @param <T> the type of collection elements
     * @param collection collection to create the stream of
     * @return a sequential {@code StreamEx} over the elements in given
     *         collection
     * @see Collection#stream()
     */
    public static <T> StreamEx<T> of(Collection<? extends T> collection) {
        return of(collection.spliterator());
    }

    /**
     * Returns an {@link StreamEx} object which wraps given {@link Stream}.
     * 
     * <p>
     * The supplied stream must not be consumed or closed when this method is
     * called. No operation must be performed on the supplied stream after it's
     * wrapped.
     * 
     * @param <T> the type of stream elements
     * @param stream original stream
     * @return the wrapped stream
     */
    public static <T> StreamEx<T> of(Stream<T> stream) {
        if (stream instanceof AbstractStreamEx) {
            @SuppressWarnings("unchecked")
            AbstractStreamEx<T, ?> ase = (AbstractStreamEx<T, ?>) stream;
            if (ase.spliterator != null)
                return new StreamEx<>(ase.spliterator(), ase.context);
            return new StreamEx<>(ase.stream(), ase.context);
        }
        return new StreamEx<>(stream, StreamContext.of(stream));
    }

    /**
     * Returns a sequential {@link StreamEx} created from given
     * {@link Spliterator}.
     *
     * @param <T> the type of stream elements
     * @param spliterator a spliterator to create the stream from.
     * @return the new stream
     * @since 0.3.4
     */
    public static <T> StreamEx<T> of(Spliterator<? extends T> spliterator) {
        return new StreamEx<>(spliterator, StreamContext.SEQUENTIAL);
    }

    /**
     * Returns a sequential, ordered {@link StreamEx} created from given
     * {@link Iterator}.
     *
     * <p>
     * This method is roughly equivalent to
     * {@code StreamEx.of(Spliterators.spliteratorUnknownSize(iterator, ORDERED))}
     * , but may show better performance for parallel processing.
     *
     * <p>
     * Use this method only if you cannot provide better Stream source (like
     * {@code Collection} or {@code Spliterator}).
     *
     * @param <T> the type of iterator elements
     * @param iterator an iterator to create the stream from.
     * @return the new stream
     * @since 0.5.1
     */
    public static <T> StreamEx<T> of(Iterator<? extends T> iterator) {
        return of(new UnknownSizeSpliterator.USOfRef<>(iterator));
    }

    /**
     * Returns a sequential, ordered {@link StreamEx} created from given
     * {@link Enumeration}.
     *
     * <p>
     * Use this method only if you cannot provide better Stream source (like
     * {@code Collection} or {@code Spliterator}).
     *
     * @param <T> the type of enumeration elements
     * @param enumeration an enumeration to create the stream from.
     * @return the new stream
     * @since 0.5.1
     */
    public static <T> StreamEx<T> of(Enumeration<? extends T> enumeration) {
        return of(new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return enumeration.hasMoreElements();
            }

            @Override
            public T next() {
                return enumeration.nextElement();
            }
        });
    }

    /**
     * Returns a sequential {@code StreamEx} containing an {@link Optional}
     * value, if present, otherwise returns an empty {@code StreamEx}.
     *
     * @param <T> the type of stream elements
     * @param optional the optional to create a stream of
     * @return a stream with an {@code Optional} value if present, otherwise an
     *         empty stream
     * @since 0.1.1
     */
    public static <T> StreamEx<T> of(Optional<? extends T> optional) {
        return optional.isPresent() ? of(optional.get()) : empty();
    }

    /**
     * Returns a sequential {@code StreamEx} containing a single element, if
     * non-null, otherwise returns an empty {@code StreamEx}.
     *
     * @param element the single element
     * @param <T> the type of stream elements
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
     * @param reader the reader to get the lines from
     * @return a {@code StreamEx<String>} providing the lines of text described
     *         by supplied {@code BufferedReader}
     * @see BufferedReader#lines()
     */
    public static StreamEx<String> ofLines(BufferedReader reader) {
        return of(UnknownSizeSpliterator.optimize(reader.lines()));
    }

    /**
     * Returns a {@code StreamEx}, the elements of which are lines read from the
     * supplied {@link Reader}. The {@code StreamEx} is lazily populated, i.e.,
     * read only occurs during the terminal stream operation.
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
     * {@code Reader}, it is wrapped in an {@link UncheckedIOException} which
     * will be thrown from the {@code StreamEx} method that caused the read to
     * take place. This method will return a StreamEx if invoked on a Reader
     * that is closed. Any operation on that stream that requires reading from
     * the Reader after it is closed, will cause an UncheckedIOException to be
     * thrown.
     *
     * @param reader the reader to get the lines from
     * @return a {@code StreamEx<String>} providing the lines of text described
     *         by supplied {@code Reader}
     * @see #ofLines(BufferedReader)
     */
    public static StreamEx<String> ofLines(Reader reader) {
        if (reader instanceof BufferedReader)
            return ofLines((BufferedReader) reader);
        return of(UnknownSizeSpliterator.optimize(new BufferedReader(reader).lines()));
    }

    /**
     * Read all lines from a file as a {@code StreamEx}. Bytes from the file are
     * decoded into characters using the {@link StandardCharsets#UTF_8 UTF-8}
     * {@link Charset charset} and the same line terminators as specified by
     * {@link Files#readAllLines(Path, Charset)} are supported.
     *
     * <p>
     * After this method returns, then any subsequent I/O exception that occurs
     * while reading from the file or when a malformed or unmappable byte
     * sequence is read, is wrapped in an {@link UncheckedIOException} that will
     * be thrown from the {@code StreamEx} method that caused the read to take
     * place. In case an {@code IOException} is thrown when closing the file, it
     * is also wrapped as an {@code UncheckedIOException}.
     *
     * <p>
     * The returned stream encapsulates a {@link Reader}. If timely disposal of
     * file system resources is required, the try-with-resources construct
     * should be used to ensure that the stream's {@link #close close} method is
     * invoked after the stream operations are completed.
     *
     * @param path the path to the file
     * @return the lines from the file as a {@code StreamEx}
     * @throws IOException if an I/O error occurs opening the file
     * @since 0.5.0
     * @see Files#lines(Path)
     */
    public static StreamEx<String> ofLines(Path path) throws IOException {
        return of(UnknownSizeSpliterator.optimize(Files.lines(path)));
    }

    /**
     * Read all lines from a file as a {@code StreamEx}.
     *
     * <p>
     * Bytes from the file are decoded into characters using the specified
     * charset and the same line terminators as specified by
     * {@link Files#readAllLines(Path, Charset)} are supported.
     *
     * <p>
     * After this method returns, then any subsequent I/O exception that occurs
     * while reading from the file or when a malformed or unmappable byte
     * sequence is read, is wrapped in an {@link UncheckedIOException} that will
     * be thrown from the {@code StreamEx} method that caused the read to take
     * place. In case an {@code IOException} is thrown when closing the file, it
     * is also wrapped as an {@code UncheckedIOException}.
     *
     * <p>
     * The returned stream encapsulates a {@link Reader}. If timely disposal of
     * file system resources is required, the try-with-resources construct
     * should be used to ensure that the stream's {@link #close close} method is
     * invoked after the stream operations are completed.
     *
     * @param path the path to the file
     * @param charset the charset to use for decoding
     * @return the lines from the file as a {@code StreamEx}
     * @throws IOException if an I/O error occurs opening the file
     * @see Files#lines(Path, Charset)
     * @since 0.5.0
     */
    public static StreamEx<String> ofLines(Path path, Charset charset) throws IOException {
        return of(UnknownSizeSpliterator.optimize(Files.lines(path, charset)));
    }

    /**
     * Returns a sequential {@code StreamEx} with keySet of given {@link Map} as
     * its source.
     *
     * @param <T> the type of map keys
     * @param map input map
     * @return a sequential {@code StreamEx} over the keys of given {@code Map}
     * @throws NullPointerException if map is null
     * @see Map#keySet()
     */
    public static <T> StreamEx<T> ofKeys(Map<T, ?> map) {
        return of(map.keySet().spliterator());
    }

    /**
     * Returns a sequential {@code StreamEx} of given {@link Map} keys which
     * corresponding values match the supplied filter.
     *
     * @param <T> the type of map keys and created stream elements
     * @param <V> the type of map values
     * @param map input map
     * @param valueFilter a predicate used to test values
     * @return a sequential {@code StreamEx} over the keys of given {@code Map}
     *         which corresponding values match the supplied filter.
     * @throws NullPointerException if map is null
     * @see Map#keySet()
     */
    public static <T, V> StreamEx<T> ofKeys(Map<T, V> map, Predicate<? super V> valueFilter) {
        return EntryStream.of(map).filterValues(valueFilter).keys();
    }

    /**
     * Returns a sequential {@code StreamEx} with values of given {@link Map} as
     * its source.
     *
     * @param <T> the type of map keys
     * @param map input map
     * @return a sequential {@code StreamEx} over the values of given
     *         {@code Map}
     * @throws NullPointerException if map is null
     * @see Map#values()
     */
    public static <T> StreamEx<T> ofValues(Map<?, T> map) {
        return of(map.values().spliterator());
    }

    /**
     * Returns a sequential {@code StreamEx} of given {@link Map} values which
     * corresponding keys match the supplied filter.
     *
     * @param <K> the type of map keys
     * @param <T> the type of map values and created stream elements
     * @param map input map
     * @param keyFilter a predicate used to test keys
     * @return a sequential {@code StreamEx} over the values of given
     *         {@code Map} which corresponding keys match the supplied filter.
     * @throws NullPointerException if map is null
     * @see Map#values()
     */
    public static <K, T> StreamEx<T> ofValues(Map<K, T> map, Predicate<? super K> keyFilter) {
        return EntryStream.of(map).filterKeys(keyFilter).values();
    }

    /**
     * Returns a new {@code StreamEx} of {@code int[]} arrays containing all the
     * possible permutations of numbers from 0 to length-1 in lexicographic
     * order.
     * 
     * @param length length of permutations array. Lengths bigger than 20 are
     *        not supported currently.
     * @return new sequential {@code StreamEx} of possible permutations.
     * @since 0.2.2
     */
    public static StreamEx<int[]> ofPermutations(int length) {
        return of(new PermutationSpliterator(length));
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
     * If the input sequence is mutable, it must remain constant from the stream
     * creation until the execution of the terminal stream operation. Otherwise,
     * the result of the terminal stream operation is undefined.
     *
     * @param str The character sequence to be split
     * @param pattern The pattern to use for splitting
     *
     * @return The stream of strings computed by splitting the input around
     *         matches of this pattern
     * @see Pattern#splitAsStream(CharSequence)
     */
    public static StreamEx<String> split(CharSequence str, Pattern pattern) {
        if (str.length() == 0)
            return of("");
        return of(UnknownSizeSpliterator.optimize(pattern.splitAsStream(str)));
    }

    /**
     * Creates a stream from the given input sequence around matches of the
     * given pattern represented as String.
     *
     * <p>
     * This method is equivalent to
     * {@code StreamEx.split(str, Pattern.compile(regex))}.
     *
     * @param str The character sequence to be split
     * @param regex The regular expression String to use for splitting
     *
     * @return The stream of strings computed by splitting the input around
     *         matches of this pattern
     * @see Pattern#splitAsStream(CharSequence)
     * @see #split(CharSequence, char)
     */
    public static StreamEx<String> split(CharSequence str, String regex) {
        if (str.length() == 0)
            return of("");
        if (regex.isEmpty()) {
            return IntStreamEx.ofChars(str).mapToObj(ch -> new String(new char[] { (char) ch }));
        }
        char ch = regex.charAt(0);
        if (regex.length() == 1 && ".$|()[{^?*+\\".indexOf(ch) == -1) {
            return split(str, ch);
        } else if (regex.length() == 2 && ch == '\\') {
            ch = regex.charAt(1);
            if ((ch < '0' || ch > '9') && (ch < 'A' || ch > 'Z') && (ch < 'a' || ch > 'z')
                && (ch < Character.MIN_HIGH_SURROGATE || ch > Character.MAX_LOW_SURROGATE)) {
                return split(str, ch);
            }
        }
        return of(UnknownSizeSpliterator.optimize(Pattern.compile(regex).splitAsStream(str)));
    }

    /**
     * Creates a stream from the given input sequence around matches of the
     * given character.
     *
     * <p>
     * This method is equivalent to {@code StreamEx.split(str, delimiter, true)}.
     *
     * @param str The character sequence to be split
     * @param delimiter The delimiter character to use for splitting
     *
     * @return The stream of strings computed by splitting the input around the
     *         delimiters
     * @see Pattern#splitAsStream(CharSequence)
     * @since 0.5.1
     */
    public static StreamEx<String> split(CharSequence str, char delimiter) {
        return split(str, delimiter, true);
    }

    /**
     * Creates a stream from the given input sequence around matches of the
     * given character.
     *
     * <p>
     * The stream returned by this method contains each substring of the input
     * sequence that is terminated by supplied delimiter character or is
     * terminated by the end of the input sequence. The substrings in the stream
     * are in the order in which they occur in the input. If the trimEmpty
     * parameter is true, trailing empty strings will be discarded and not
     * encountered in the stream.
     *
     * <p>
     * If the given delimiter character does not appear in the input then the
     * resulting stream has just one element, namely the input sequence in
     * string form.
     *
     * <p>
     * If the input sequence is mutable, it must remain constant from the stream
     * creation until the execution of the terminal stream operation. Otherwise,
     * the result of the terminal stream operation is undefined.
     *
     * @param str The character sequence to be split
     * @param delimiter The delimiter character to use for splitting
     * @param trimEmpty If true, trailing empty strings will be discarded
     *
     * @return The stream of strings computed by splitting the input around the
     *         delimiters
     * @see Pattern#splitAsStream(CharSequence)
     * @since 0.5.1
     */
    public static StreamEx<String> split(CharSequence str, char delimiter, boolean trimEmpty) {
        if (str.length() == 0)
            return of("");
        return of(new CharSpliterator(str, delimiter, trimEmpty));
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
     * @param <T> the type of stream elements
     * @param seed the initial element
     * @param f a function to be applied to to the previous element to produce a
     *        new element
     * @return a new sequential {@code StreamEx}
     * @see #iterate(Object, Predicate, UnaryOperator)
     */
    public static <T> StreamEx<T> iterate(final T seed, final UnaryOperator<T> f) {
        return iterate(seed, x -> true, f);
    }

    /**
     * Returns a sequential ordered {@code StreamEx} produced by iterative
     * application of a function to an initial element, conditioned on
     * satisfying the supplied predicate. The stream terminates as soon as the
     * predicate function returns false.
     *
     * <p>
     * {@code StreamEx.iterate} should produce the same sequence of elements as
     * produced by the corresponding for-loop:
     * 
     * <pre>{@code
     *     for (T index=seed; predicate.test(index); index = f.apply(index)) { 
     *         ... 
     *     }
     * }</pre>
     *
     * <p>
     * The resulting sequence may be empty if the predicate does not hold on the
     * seed value. Otherwise the first element will be the supplied seed value,
     * the next element (if present) will be the result of applying the function
     * f to the seed value, and so on iteratively until the predicate indicates
     * that the stream should terminate.
     *
     * @param <T> the type of stream elements
     * @param seed the initial element
     * @param predicate a predicate to apply to elements to determine when the
     *        stream must terminate.
     * @param f a function to be applied to the previous element to produce a
     *        new element
     * @return a new sequential {@code StreamEx}
     * @see #iterate(Object, UnaryOperator)
     * @since 0.6.0
     */
    public static <T> StreamEx<T> iterate(T seed, Predicate<? super T> predicate, UnaryOperator<T> f) {
        Objects.requireNonNull(f);
        Objects.requireNonNull(predicate);
        Spliterator<T> spliterator = new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED
            | Spliterator.IMMUTABLE) {
            T prev;
            boolean started, finished;

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                if (finished)
                    return false;
                T t;
                if (started)
                    t = f.apply(prev);
                else {
                    t = seed;
                    started = true;
                }
                if (!predicate.test(t)) {
                    prev = null;
                    finished = true;
                    return false;
                }
                action.accept(prev = t);
                return true;
            }

            @Override
            public void forEachRemaining(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                if (finished)
                    return;
                finished = true;
                T t = started ? f.apply(prev) : seed;
                prev = null;
                while (predicate.test(t)) {
                    action.accept(t);
                    t = f.apply(t);
                }
            }
        };
        return of(spliterator);
    }

    /**
     * Returns an infinite sequential unordered {@code StreamEx} where each
     * element is generated by the provided {@link Supplier}. This is suitable
     * for generating constant streams, streams of random elements, etc.
     *
     * @param <T> the type of stream elements
     * @param s the {@code Supplier} of generated elements
     * @return a new infinite sequential unordered {@code StreamEx}
     * @see Stream#generate(Supplier)
     */
    public static <T> StreamEx<T> generate(Supplier<T> s) {
        return new StreamEx<>(Stream.generate(s), StreamContext.SEQUENTIAL);
    }

    /**
     * Return an ordered stream produced by consecutive calls of the supplied
     * producer until it returns false.
     * 
     * <p>
     * The producer function may call the passed consumer any number of times
     * and return true if the producer should be called again or false
     * otherwise. It's guaranteed that the producer will not be called anymore,
     * once it returns false.
     * 
     * <p>
     * This method is particularly useful when producer changes the mutable
     * object which should be left in known state after the full stream
     * consumption. For example, the following code could be used to drain
     * elements from the queue until it's empty or sentinel is reached
     * (consuming the sentinel):
     * 
     * <pre>{@code
     * return StreamEx.produce(action -> {
     *   T next = queue.poll();
     *   if(next == null || next.equals(sentinel))
     *       return false;
     *   action.accept(next);
     *   return true;
     * });}</pre>
     * 
     * <p>
     * Note however that if a short-circuiting operation is used, then the final
     * state of the mutable object cannot be guaranteed.
     * 
     * @param <T> the type of the resulting stream elements
     * @param producer a predicate which calls the passed consumer to emit
     *        stream element(s) and returns true if it producer should be
     *        applied again.
     * @return the new stream
     * @since 0.6.0
     */
    public static <T> StreamEx<T> produce(Predicate<Consumer<? super T>> producer) {
        Box<Emitter<T>> box = new Box<>();
        return (box.a = action -> producer.test(action) ? box.a : null).stream();
    }

    /**
     * Returns a sequential unordered {@code StreamEx} of given length which
     * elements are equal to supplied value.
     * 
     * @param <T> the type of stream elements
     * @param value the constant value
     * @param length the length of the stream
     * @return a new {@code StreamEx}
     * @since 0.1.2
     */
    public static <T> StreamEx<T> constant(T value, long length) {
        return of(new ConstSpliterator.OfRef<>(value, length, false));
    }

    /**
     * Returns a sequential ordered {@code StreamEx} containing the results of
     * applying the given mapper function to the all possible pairs of elements
     * taken from the provided list.
     * 
     * <p>
     * The indices of two elements supplied to the mapper function are always
     * ordered: first element index is strictly less than the second element
     * index. The pairs are lexicographically ordered. For example, for the list
     * of three elements the stream of three elements is created:
     * {@code mapper.apply(list.get(0), list.get(1))},
     * {@code mapper.apply(list.get(0), list.get(2))} and
     * {@code mapper.apply(list.get(1), list.get(2))}. The number of elements in
     * the resulting stream is {@code list.size()*(list.size()+1L)/2}.
     * 
     * <p>
     * The list values are accessed using {@link List#get(int)}, so the list
     * should provide fast random access. The list is assumed to be unmodifiable
     * during the stream operations.
     *
     * @param <U> type of the list elements
     * @param <T> type of the stream elements
     * @param list a list to take the elements from
     * @param mapper a non-interfering, stateless function to apply to each pair
     *        of list elements.
     * @return a new {@code StreamEx}
     * @see EntryStream#ofPairs(List)
     * @since 0.3.6
     */
    public static <U, T> StreamEx<T> ofPairs(List<U> list, BiFunction<? super U, ? super U, ? extends T> mapper) {
        return of(new PairPermutationSpliterator<>(list, mapper));
    }

    /**
     * Returns a sequential ordered {@code StreamEx} containing the results of
     * applying the given mapper function to the all possible pairs of elements
     * taken from the provided array.
     * 
     * <p>
     * The indices of two array elements supplied to the mapper function are
     * always ordered: first element index is strictly less than the second
     * element index. The pairs are lexicographically ordered. For example, for
     * the array of three elements the stream of three elements is created:
     * {@code mapper.apply(array[0], array[1])},
     * {@code mapper.apply(array[0], array[2])} and
     * {@code mapper.apply(array[1], array[2])}. The number of elements in the
     * resulting stream is {@code array.length*(array.length+1L)/2}.
     * 
     * @param <U> type of the array elements
     * @param <T> type of the stream elements
     * @param array an array to take the elements from
     * @param mapper a non-interfering, stateless function to apply to each pair
     *        of array elements.
     * @return a new {@code StreamEx}
     * @see EntryStream#ofPairs(Object[])
     * @since 0.3.6
     */
    public static <U, T> StreamEx<T> ofPairs(U[] array, BiFunction<? super U, ? super U, ? extends T> mapper) {
        return ofPairs(Arrays.asList(array), mapper);
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
     * @param <U> the type of the first list elements
     * @param <V> the type of the second list elements
     * @param <T> the type of the resulting stream elements
     * @param first the first list, assumed to be unmodified during use
     * @param second the second list, assumed to be unmodified during use
     * @param mapper a non-interfering, stateless function to apply to each pair
     *        of the corresponding list elements.
     * @return a new {@code StreamEx}
     * @throws IllegalArgumentException if length of the lists differs.
     * @see EntryStream#zip(List, List)
     * @since 0.2.1
     */
    public static <U, V, T> StreamEx<T> zip(List<U> first, List<V> second,
            BiFunction<? super U, ? super V, ? extends T> mapper) {
        return of(new RangeBasedSpliterator.ZipRef<>(0, checkLength(first.size(), second.size()), mapper, first, second));
    }

    /**
     * Returns a sequential {@code StreamEx} containing the results of applying
     * the given function to the corresponding pairs of values in given two
     * arrays.
     * 
     * @param <U> the type of the first array elements
     * @param <V> the type of the second array elements
     * @param <T> the type of the resulting stream elements
     * @param first the first array
     * @param second the second array
     * @param mapper a non-interfering, stateless function to apply to each pair
     *        of the corresponding array elements.
     * @return a new {@code StreamEx}
     * @throws IllegalArgumentException if length of the arrays differs.
     * @see EntryStream#zip(Object[], Object[])
     * @since 0.2.1
     */
    public static <U, V, T> StreamEx<T> zip(U[] first, V[] second, BiFunction<? super U, ? super V, ? extends T> mapper) {
        return zip(Arrays.asList(first), Arrays.asList(second), mapper);
    }

    /**
     * Return a new {@link StreamEx} containing all the nodes of tree-like data
     * structure in depth-first order.
     * 
     * @param <T> the type of tree nodes
     * @param root root node of the tree
     * @param mapper a non-interfering, stateless function to apply to each tree
     *        node which returns null for leaf nodes or stream of direct
     *        children for non-leaf nodes.
     * @return the new sequential ordered stream
     * @since 0.2.2
     * @see EntryStream#ofTree(Object, BiFunction)
     * @see #ofTree(Object, Class, Function)
     */
    public static <T> StreamEx<T> ofTree(T root, Function<T, Stream<T>> mapper) {
        Stream<T> rootStream = mapper.apply(root);
        return rootStream == null ? of(root) : flatTraverse(rootStream, mapper).prepend(root);
    }

    /**
     * Return a new {@link StreamEx} containing all the nodes of tree-like data
     * structure in depth-first order.
     * 
     * @param <T> the base type of tree nodes
     * @param <TT> the sub-type of composite tree nodes which may have children
     * @param root root node of the tree
     * @param collectionClass a class representing the composite tree node
     * @param mapper a non-interfering, stateless function to apply to each
     *        composite tree node which returns stream of direct children. May
     *        return null if the given node has no children.
     * @return the new sequential ordered stream
     * @since 0.2.2
     * @see EntryStream#ofTree(Object, Class, BiFunction)
     * @see #ofTree(Object, Function)
     */
    @SuppressWarnings("unchecked")
    public static <T, TT extends T> StreamEx<T> ofTree(T root, Class<TT> collectionClass, Function<TT, Stream<T>> mapper) {
        return ofTree(root, t -> collectionClass.isInstance(t) ? mapper.apply((TT) t) : null);
    }

    /**
     * Returns a new {@code StreamEx} which consists of non-overlapping sublists
     * of given source list having the specified length (the last sublist may be
     * shorter).
     * 
     * <p>
     * This method calls {@link List#subList(int, int)} internally, so source
     * list must have it properly implemented as well as provide fast random
     * access.
     * 
     * <p>
     * This method is equivalent to
     * {@code StreamEx.ofSubLists(source, length, length)}.
     * 
     * @param <T> the type of source list elements.
     * @param source the source list
     * @param length the length of each sublist except possibly the last one
     *        (must be positive number).
     * @return the new stream of sublists.
     * @throws IllegalArgumentException if length is negative or zero.
     * @since 0.3.3
     * @see #ofSubLists(List, int, int)
     * @see List#subList(int, int)
     */
    public static <T> StreamEx<List<T>> ofSubLists(List<T> source, int length) {
        return ofSubLists(source, length, length);
    }

    /**
     * Returns a new {@code StreamEx} which consists of possibly-overlapping
     * sublists of given source list having the specified length with given
     * shift value.
     * 
     * <p>
     * This method calls {@link List#subList(int, int)} internally, so source
     * list must have it properly implemented as well as provide fast random
     * access.
     * 
     * <p>
     * The shift value specifies how many elements the next sublist is shifted
     * relative to the previous one. If the shift value is greater than one,
     * then the last sublist might be shorter than the specified length value.
     * If the shift value is greater than the length, some elements will not
     * appear in sublists at all.
     * 
     * @param <T> the type of source list elements.
     * @param source the source list
     * @param length the length of each sublist except possibly the last one
     *        (must be positive number).
     * @param shift the number of elements the next sublist is shifted relative
     *        to the previous one (must be positive number).
     * @return the new stream of sublists.
     * @throws IllegalArgumentException if length is negative or zero.
     * @since 0.3.7
     * @see List#subList(int, int)
     */
    public static <T> StreamEx<List<T>> ofSubLists(List<T> source, int length, int shift) {
        if (length <= 0)
            throw new IllegalArgumentException("length = " + length);
        if (shift <= 0)
            throw new IllegalArgumentException("shift = " + shift);
        if (source.isEmpty())
            return StreamEx.empty();
        return of(new RangeBasedSpliterator.OfSubLists<>(source, length, shift));
    }

    /**
     * Returns a new {@code StreamEx} which elements are {@link List} objects
     * containing all possible tuples of the elements of supplied collection of
     * collections. The whole stream forms an n-fold Cartesian product (or
     * cross-product) of the input collections.
     * 
     * <p>
     * Every stream element is the {@code List} of the same size as supplied
     * collection. The first element in the list is taken from the first
     * collection which appears in source and so on. The elements are ordered
     * lexicographically according to the order of the input collections.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} elements. It's however guaranteed that
     * each element is the distinct object.
     * 
     * <p>
     * The supplied collection is assumed to be unchanged during the operation.
     *
     * @param <T> the type of the elements
     * @param source the input collection of collections which is used to
     *        generate the cross-product.
     * @return the new stream of lists.
     * @see #cartesianPower(int, Collection)
     * @since 0.3.8
     */
    public static <T> StreamEx<List<T>> cartesianProduct(Collection<? extends Collection<T>> source) {
        if (source.isEmpty())
            return StreamEx.of(new ConstSpliterator.OfRef<>(Collections.emptyList(), 1, true));
        return of(new CrossSpliterator.ToList<>(source));
    }

    /**
     * Returns a new {@code StreamEx} which elements are results of reduction of
     * all possible tuples composed from the elements of supplied collection of
     * collections. The whole stream forms an n-fold Cartesian product (or
     * cross-product) of the input collections.
     * 
     * <p>
     * The reduction is performed using the provided identity object and the
     * accumulator function which is capable to accumulate new element. The
     * accumulator function must not modify the previous accumulated value, but
     * must produce new value instead. That's because partially accumulated
     * values are reused for subsequent elements.
     * 
     * <p>
     * This method is equivalent to the following:
     *
     * <pre>
     * {@code StreamEx.cartesianProduct(source).map(list -> StreamEx.of(list).foldLeft(identity, accumulator))}
     * </pre>
     * 
     * <p>
     * However it may perform much faster as partial reduction results are
     * reused.
     * 
     * <p>
     * The supplied collection is assumed to be unchanged during the operation.
     *
     * @param <T> the type of the input elements
     * @param <U> the type of the elements of the resulting stream
     * @param source the input collection of collections which is used to
     *        generate the cross-product.
     * @param identity the identity value
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element from source
     *        collection into a stream element.
     * @return the new stream.
     * @see #cartesianProduct(Collection)
     * @see #cartesianPower(int, Collection, Object, BiFunction)
     * @since 0.4.0
     */
    public static <T, U> StreamEx<U> cartesianProduct(Collection<? extends Collection<T>> source, U identity,
            BiFunction<U, ? super T, U> accumulator) {
        if (source.isEmpty())
            return of(identity);
        return of(new CrossSpliterator.Reducing<>(source, identity, accumulator));
    }

    /**
     * Returns a new {@code StreamEx} which elements are {@link List} objects
     * containing all possible n-tuples of the elements of supplied collection.
     * The whole stream forms an n-fold Cartesian product of input collection
     * with itself or n-ary Cartesian power of the input collection.
     * 
     * <p>
     * Every stream element is the {@code List} of the supplied size. The
     * elements are ordered lexicographically according to the order of the
     * input collection.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} elements. It's however guaranteed that
     * each element is the distinct object.
     * 
     * <p>
     * The supplied collection is assumed to be unchanged during the operation.
     *
     * @param <T> the type of the elements
     * @param n the size of the {@code List} elements of the resulting stream.
     * @param source the input collection of collections which is used to
     *        generate the Cartesian power.
     * @return the new stream of lists.
     * @see #cartesianProduct(Collection)
     * @since 0.3.8
     */
    public static <T> StreamEx<List<T>> cartesianPower(int n, Collection<T> source) {
        if (n == 0)
            return StreamEx.of(new ConstSpliterator.OfRef<>(Collections.emptyList(), 1, true));
        return of(new CrossSpliterator.ToList<>(Collections.nCopies(n, source)));
    }

    /**
     * Returns a new {@code StreamEx} which elements are results of reduction of
     * all possible n-tuples composed from the elements of supplied collections.
     * The whole stream forms an n-fold Cartesian product of input collection
     * with itself or n-ary Cartesian power of the input collection.
     * 
     * <p>
     * The reduction is performed using the provided identity object and the
     * accumulator function which is capable to accumulate new element. The
     * accumulator function must not modify the previous accumulated value, but
     * must produce new value instead. That's because partially accumulated
     * values are reused for subsequent elements.
     * 
     * <p>
     * This method is equivalent to the following:
     *
     * <pre>
     * {@code StreamEx.cartesianPower(n, source).map(list -> StreamEx.of(list).foldLeft(identity, accumulator))}
     * </pre>
     * 
     * <p>
     * However it may perform much faster as partial reduction results are
     * reused.
     * 
     * <p>
     * The supplied collection is assumed to be unchanged during the operation.
     *
     * @param <T> the type of the input elements
     * @param <U> the type of the elements of the resulting stream
     * @param n the number of elements to incorporate into single element of the
     *        resulting stream.
     * @param source the input collection of collections which is used to
     *        generate the Cartesian power.
     * @param identity the identity value
     * @param accumulator a <a
     *        href="package-summary.html#NonInterference">non-interfering </a>,
     *        <a href="package-summary.html#Statelessness">stateless</a>
     *        function for incorporating an additional element from source
     *        collection into a stream element.
     * @return the new stream.
     * @see #cartesianProduct(Collection, Object, BiFunction)
     * @see #cartesianPower(int, Collection)
     * @since 0.4.0
     */
    public static <T, U> StreamEx<U> cartesianPower(int n, Collection<T> source, U identity,
            BiFunction<U, ? super T, U> accumulator) {
        if (n == 0)
            return of(identity);
        return of(new CrossSpliterator.Reducing<>(Collections.nCopies(n, source), identity, accumulator));
    }

    /**
     * A helper interface to build a new stream by emitting elements and
     * creating new emitters in a chain.
     * 
     * <p>
     * Using this interface it's possible to create custom sources which cannot
     * be easily expressed using {@link StreamEx#iterate(Object, UnaryOperator)}
     * or {@link StreamEx#generate(Supplier)}. For example, the following method
     * generates a Collatz sequence starting from given number:
     * 
     * <pre>{@code
     * public static Emitter<Integer> collatz(int start) {
     *    return action -> {
     *       action.accept(start);
     *       return start == 1 ? null : collatz(start % 2 == 0 ? start / 2 : start * 3 + 1);
     *    };
     * }}</pre>
     * 
     * <p>
     * Now you can use {@code collatz(17).stream()} to get the stream of Collatz
     * numbers.
     * 
     * @author Tagir Valeev
     *
     * @param <T> the type of the elements this emitter emits
     * @since 0.6.0
     */
    @FunctionalInterface
    public interface Emitter<T> {
        /**
         * Calls the supplied consumer zero or more times to emit some elements,
         * then returns the next emitter which will emit more, or null if
         * nothing more to emit.
         * 
         * <p>
         * Normally one element is emitted during the {@code next()} method
         * call. However, it's not restricted: you may emit as many elements as
         * you want, though in some cases if many elements were emitted they
         * might be buffered consuming additional memory.
         * 
         * <p>
         * It's allowed not to emit anything (don't call the consumer). However
         * if you do this and return new emitter which also does not emit
         * anything, you will end up in endless loop.
         * 
         * @param action consumer to be called to emit elements
         * @return next emitter or null
         */
        Emitter<T> next(Consumer<? super T> action);

        /**
         * Returns the spliterator which covers all the elements emitted by this
         * emitter.
         * 
         * @return the new spliterator
         */
        default Spliterator<T> spliterator() {
            return new EmitterSpliterator<>(this);
        }

        /**
         * Returns the stream which covers all the elements emitted by this
         * emitter.
         * 
         * @return the new stream
         */
        default StreamEx<T> stream() {
            return of(spliterator());
        }
    }
}
