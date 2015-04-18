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
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A {@link Stream} implementation with additional functionality
 * 
 * @author Tagir Valeev
 *
 * @param <T> the type of the stream elements
 */
public class StreamEx<T> extends AbstractStreamEx<T, StreamEx<T>> {
    @SuppressWarnings("rawtypes")
    private static final StreamEx EMPTY = StreamEx.of(Stream.empty());

    StreamEx(Stream<T> stream) {
        super(stream);
    }

    @Override
    StreamEx<T> supply(Stream<T> stream) {
        return new StreamEx<>(stream);
    }

    /**
     * Returns a stream consisting of the results of applying the given
     * function to the elements of this stream.
     *
     * <p>This is an intermediate operation.
     *
     * @param <R> The element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return the new stream
     */
    @Override
    public <R> StreamEx<R> map(Function<? super T, ? extends R> mapper) {
        return new StreamEx<>(stream.map(mapper));
    }

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.  Each mapped stream is
     * {@link java.util.stream.BaseStream#close() closed} after its contents
     * have been placed into this stream.  (If a mapped stream is {@code null}
     * an empty stream is used, instead.)
     *
     * <p>This is an intermediate operation.
     *
     * <p>The {@code flatMap()} operation has the effect of applying a one-to-many
     * transformation to the elements of the stream, and then flattening the
     * resulting elements into a new stream.
     *
     * @param <R> The element type of the new stream
     * @param mapper a non-interfering, stateless function to apply 
     *               to each element which produces a stream of new values
     * @return the new stream
     */
    @Override
    public <R> StreamEx<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return new StreamEx<>(stream.flatMap(mapper));
    }

    /**
     * Returns a stream consisting of the elements of this stream 
     * which are instances of given class.
     *
     * <p>This is an intermediate operation.
     *
     * @param <TT> a type of instances to select.
     * @param clazz a class which instances should be selected
     * @return the new stream
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <TT extends T> StreamEx<TT> select(Class<TT> clazz) {
        return new StreamEx<>((Stream) stream.filter(clazz::isInstance));
    }

    /**
     * Returns an {@link EntryStream} consisting of the {@link Entry} objects
     * which keys are elements of this stream and values are results of 
     * applying the given function to the elements of this stream.
     *
     * <p>This is an intermediate operation.
     *
     * @param <V> The {@code Entry} value type
     * @param valueMapper a non-interfering, stateless function to apply to each element
     * @return the new stream
     */
    public <V> EntryStream<T, V> mapToEntry(Function<T, V> valueMapper) {
        return new EntryStream<>(stream, Function.identity(), valueMapper);
    }

    /**
     * Returns an {@link EntryStream} consisting of the {@link Entry} objects
     * which keys and values are results of applying the given functions to 
     * the elements of this stream.
     *
     * <p>This is an intermediate operation.
     *
     * @param <K> The {@code Entry} key type
     * @param <V> The {@code Entry} value type
     * @param keyMapper a non-interfering, stateless function to apply to each element
     * @param valueMapper a non-interfering, stateless function to apply to each element
     * @return the new stream
     */
    public <K, V> EntryStream<K, V> mapToEntry(Function<T, K> keyMapper, Function<T, V> valueMapper) {
        return new EntryStream<>(stream, keyMapper, valueMapper);
    }

    public <R> StreamEx<R> flatCollection(Function<? super T, ? extends Collection<? extends R>> mapper) {
        return flatMap(mapper.andThen(Collection::stream));
    }

    public <K, V> EntryStream<K, V> flatMapToEntry(Function<? super T, Map<K, V>> mapper) {
        return new EntryStream<>(stream.flatMap(e -> mapper.apply(e).entrySet().stream()));
    }
    
    /**
     * Returns a {@code Map} whose keys are the
     * values resulting from applying the classification function to the input
     * elements, and whose corresponding values are {@code List}s containing the
     * input elements which map to the associated key under the classification
     * function.
     *
     * <p>There are no guarantees on the type, mutability or serializability of 
     * the {@code Map} or {@code List} objects returned.
     * 
     * <p>For parallel stream concurrent collector is used and ConcurrentMap is 
     * returned.
     *
     * <p>This is a terminal operation.

     * @param <K> the type of the keys
     * @param classifier the classifier function mapping input elements to keys
     * @return a {@code Map} containing the results of the group-by operation
     *
     * @see #groupingBy(Function, Collector)
     * @see Collectors#groupingBy(Function)
     * @see Collectors#groupingByConcurrent(Function)
     */
    public <K> Map<K, List<T>> groupingBy(Function<? super T, ? extends K> classifier) {
        if(stream.isParallel())
            return stream.collect(Collectors.groupingByConcurrent(classifier));
        return stream.collect(Collectors.groupingBy(classifier));
    }

    public <K, D> Map<K, D> groupingBy(Function<? super T, ? extends K> classifier,
            Collector<? super T, ?, D> downstream) {
        if(stream.isParallel())
            return stream.collect(Collectors.groupingByConcurrent(classifier, downstream));
        return stream.collect(Collectors.groupingBy(classifier, downstream));
    }

    @SuppressWarnings("unchecked")
    public <K, D, M extends Map<K, D>> M groupingBy(Function<? super T, ? extends K> classifier,
            Supplier<M> mapFactory, Collector<? super T, ?, D> downstream) {
        if(stream.isParallel() && mapFactory.get() instanceof ConcurrentMap)
            return (M) stream.collect(Collectors.groupingByConcurrent(classifier, (Supplier<ConcurrentMap<K, D>>)mapFactory, downstream));
        return stream.collect(Collectors.groupingBy(classifier, mapFactory, downstream));
    }

    /**
     * Returns a {@link String} which contains the results of calling {@link String#valueOf(Object)}
     * on each element of this stream in encounter order.
     *
     * <p>This is a terminal operation.

     * @return a {@code String}. For empty input stream empty String is returned. 
     */
    public String joining() {
        return stream.map(String::valueOf).collect(Collectors.joining());
    }

    /**
     * Returns a {@link String} which contains the results of calling {@link String#valueOf(Object)}
     * on each element of this stream, separated by the specified delimiter, in encounter order.
     *
     * <p>This is a terminal operation.

     * @param delimiter the delimiter to be used between each element
     * @return a {@code String}. For empty input stream empty String is returned. 
     */
    public String joining(CharSequence delimiter) {
        return stream.map(String::valueOf).collect(Collectors.joining(delimiter));
    }

    /**
     * Returns a {@link String} which contains the results of calling {@link String#valueOf(Object)}
     * on each element of this stream, separated by the specified delimiter, with the specified prefix 
     * and suffix in encounter order.
     *
     * <p>This is a terminal operation.
     * 
     * @param delimiter the delimiter to be used between each element
     * @param prefix the sequence of characters to be used at the beginning
     *                of the joined result
     * @param suffix the sequence of characters to be used at the end
     *                of the joined result
     * @return a {@code String}. For empty input stream empty String is returned. 
     */
    public String joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return stream.map(String::valueOf).collect(Collectors.joining(delimiter, prefix, suffix));
    }

    /**
     * Returns a {@link Map} whose keys are elements from this stream and values are 
     * the result of applying the provided mapping functions to the input elements.
     *
     * <p>This is a terminal operation.

     * <p>If this stream contains duplicates (according to
     * {@link Object#equals(Object)}), an {@code IllegalStateException} is
     * thrown when the collection operation is performed.
     *
     * <p>For parallel stream the concurrent {@code Map} is created.
     *
     * @param <V> the output type of the value mapping function
     * @param valMapper a mapping function to produce values
     * @return a {@code Map} whose keys are elements from this stream and 
     * values are the result of applying mapping function to the input elements
     *
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @see #toMap(Function, Function)
     */
    public <V> Map<T, V> toMap(Function<T, V> valMapper) {
        if(stream.isParallel())
            return stream.collect(Collectors.toConcurrentMap(Function.identity(), valMapper));
        return stream.collect(Collectors.toMap(Function.identity(), valMapper));
    }

    /**
     * Returns a {@link Map} whose keys and values are 
     * the result of applying the provided mapping functions to the input elements.
     *
     * <p>This is a terminal operation.

     * <p>If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), an {@code IllegalStateException} is
     * thrown when the collection operation is performed.
     *
     * <p>For parallel stream the concurrent {@code Map} is created.
     *
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valMapper a mapping function to produce values
     * @return a {@code Map} whose keys and values are 
     * the result of applying mapping functions to the input elements
     *
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @see #toMap(Function)
     */
    public <K, V> Map<K, V> toMap(Function<T, K> keyMapper, Function<T, V> valMapper) {
        if(stream.isParallel())
            return stream.collect(Collectors.toConcurrentMap(keyMapper, valMapper));
        return stream.collect(Collectors.toMap(keyMapper, valMapper));
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
     * @since 0.0.9
     */
    public <K, V> Map<K, V> toMap(Function<T, K> keyMapper, Function<T, V> valMapper, BinaryOperator<V> mergeFunction) {
        if(stream.isParallel())
            return stream.collect(Collectors.toConcurrentMap(keyMapper, valMapper, mergeFunction));
        return stream.collect(Collectors.toMap(keyMapper, valMapper, mergeFunction));
    }
    
    /**
     * Returns a new {@code StreamEx} which is a concatenation of this stream
     * and the stream containing supplied values
     * 
     * @param values the values to append to the stream
     * @return the new stream
     */
    public StreamEx<T> append(@SuppressWarnings("unchecked") T... values) {
        return append(Stream.of(values));
    }

    /**
     * Returns a new {@code StreamEx} which is a concatenation of
     * the stream containing supplied values and this stream
     *  
     * @param values the values to prepend to the stream
     * @return the new stream
     */
    public StreamEx<T> prepend(@SuppressWarnings("unchecked") T... values) {
        return prepend(Stream.of(values));
    }

    public boolean has(T element) {
        if (element == null)
            return stream.anyMatch(Objects::isNull);
        return stream.anyMatch(element::equals);
    }

    /**
     * Returns an empty sequential {@code StreamEx}.
     *
     * @param <T> the type of stream elements
     * @return an empty sequential stream
     */
    @SuppressWarnings("unchecked")
    public static <T> StreamEx<T> empty() {
        return StreamEx.EMPTY;
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
        return new StreamEx<>(Stream.of(element));
    }

    /**
     * Returns a sequential ordered {@code StreamEx} whose elements are the specified values.
     *
     * @param <T> the type of stream elements
     * @param elements the elements of the new stream
     * @return the new stream
     * @see Stream#of(Object...)
     */
    @SafeVarargs
    public static <T> StreamEx<T> of(T... elements) {
        return new StreamEx<>(Stream.of(elements));
    }

    /**
     * Returns a sequential {@code StreamEx} with given collection as its source.
     *
     * @param <T> the type of collection elements
     * @param collection collection to create the stream of
     * @return a sequential {@code StreamEx} over the elements in given collection
     * @see Collection#stream()
     */
    public static <T> StreamEx<T> of(Collection<T> collection) {
        return new StreamEx<>(collection.stream());
    }

    /**
     * Returns an {@link StreamEx} object which wraps given {@link Stream}
     * @param <T> the type of stream elements
     * @param stream original stream
     * @return the wrapped stream
     */
    public static <T> StreamEx<T> of(Stream<T> stream) {
        return stream instanceof StreamEx ? (StreamEx<T>) stream : new StreamEx<>(stream);
    }

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
     * @param <T> the type of map keys
     * @param map input map
     * @return a sequential {@code StreamEx} over the keys of given {@code Map}
     * @throws NullPointerException if map is null
     * @see Map#keySet()
     */
    public static <T> StreamEx<T> ofKeys(Map<T, ?> map) {
        return new StreamEx<>(map.keySet().stream());
    }

    public static <T, V> StreamEx<T> ofKeys(Map<T, V> map, Predicate<V> valueFilter) {
        return new StreamEx<>(map.entrySet().stream().filter(entry -> valueFilter.test(entry.getValue()))
                .map(Entry::getKey));
    }

    /**
     * Returns a sequential {@code StreamEx} with values of given {@link Map} as
     * its source.
     *
     * @param <T> the type of map keys
     * @param map input map
     * @return a sequential {@code StreamEx} over the values of given {@code Map}
     * @throws NullPointerException if map is null
     * @see Map#values()
     */
    public static <T> StreamEx<T> ofValues(Map<?, T> map) {
        return new StreamEx<>(map.values().stream());
    }

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

    public static StreamEx<String> split(CharSequence str, Pattern pattern) {
        return new StreamEx<>(pattern.splitAsStream(str));
    }

    public static StreamEx<String> split(CharSequence str, String regex) {
        return new StreamEx<>(Pattern.compile(regex).splitAsStream(str));
    }

    public static <T> StreamEx<T> iterate(final T seed, final UnaryOperator<T> f) {
        return new StreamEx<>(Stream.iterate(seed, f));
    }

    public static <T> StreamEx<T> generate(Supplier<T> s) {
        return new StreamEx<>(Stream.generate(s));
    }
}
