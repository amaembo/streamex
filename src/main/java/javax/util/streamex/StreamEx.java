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

    public <V> EntryStream<T, V> mapToEntry(Function<T, V> valueMapper) {
        return new EntryStream<T, V>(stream, Function.identity(), valueMapper);
    }

    public <K, V> EntryStream<K, V> mapToEntry(Function<T, K> keyMapper, Function<T, V> valueMapper) {
        return new EntryStream<K, V>(stream, keyMapper, valueMapper);
    }

    public <R> StreamEx<R> flatCollection(Function<? super T, ? extends Collection<? extends R>> mapper) {
        return flatMap(mapper.andThen(Collection::stream));
    }

    public <K, V> EntryStream<K, V> flatMapToEntry(Function<? super T, Map<K, V>> mapper) {
        return new EntryStream<K, V>(stream.flatMap(e -> mapper.apply(e).entrySet().stream()));
    }
    
    public <K> Map<K, List<T>> groupingBy(Function<? super T, ? extends K> classifier) {
        return stream.collect(Collectors.groupingBy(classifier));
    }

    public <K, D> Map<K, D> groupingBy(Function<? super T, ? extends K> classifier,
            Collector<? super T, ?, D> downstream) {
        return stream.collect(Collectors.groupingBy(classifier, downstream));
    }

    public <K, D, M extends Map<K, D>> M groupingBy(Function<? super T, ? extends K> classifier,
            Supplier<M> mapFactory, Collector<? super T, ?, D> downstream) {
        return stream.collect(Collectors.groupingBy(classifier, mapFactory, downstream));
    }

    public String joining() {
        return stream.map(String::valueOf).collect(Collectors.joining());
    }

    public String joining(CharSequence separator) {
        return stream.map(String::valueOf).collect(Collectors.joining(separator));
    }

    public String joining(CharSequence separator, CharSequence prefix, CharSequence suffix) {
        return stream.map(String::valueOf).collect(Collectors.joining(separator, prefix, suffix));
    }

    public <V> Map<T, V> toMap(Function<T, V> valMapper) {
        return stream.collect(Collectors.toMap(Function.identity(), valMapper));
    }

    public <K, V> Map<K, V> toMap(Function<T, K> keyMapper, Function<T, V> valMapper) {
        return stream.collect(Collectors.toMap(keyMapper, valMapper));
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
     */
    public static <T> StreamEx<T> of(T element) {
        return new StreamEx<>(Stream.of(element));
    }

    @SafeVarargs
    public static <T> StreamEx<T> of(T... elements) {
        return new StreamEx<>(Stream.of(elements));
    }

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

    public static <T> StreamEx<T> ofKeys(Map<T, ?> map) {
        return new StreamEx<>(map.keySet().stream());
    }

    public static <T, V> StreamEx<T> ofKeys(Map<T, V> map, Predicate<V> valueFilter) {
        return new StreamEx<>(map.entrySet().stream().filter(entry -> valueFilter.test(entry.getValue()))
                .map(Entry::getKey));
    }

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
