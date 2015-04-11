package javax.util.streamex;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class StreamEx<T> implements Stream<T> {
	@SuppressWarnings("rawtypes")
	private static final StreamEx EMPTY = StreamEx.of(Stream.empty());
	private final Stream<T> stream;

	StreamEx(Stream<T> stream) {
		this.stream = stream;
	}

	@Override
	public Iterator<T> iterator() {
		return stream.iterator();
	}

	@Override
	public Spliterator<T> spliterator() {
		return stream.spliterator();
	}

	@Override
	public boolean isParallel() {
		return stream.isParallel();
	}

	@Override
	public StreamEx<T> sequential() {
		return new StreamEx<>(stream.sequential());
	}

	@Override
	public StreamEx<T> parallel() {
		return new StreamEx<>(stream.parallel());
	}

	@Override
	public StreamEx<T> unordered() {
		return new StreamEx<>(stream.unordered());
	}

	@Override
	public StreamEx<T> onClose(Runnable closeHandler) {
		return new StreamEx<>(stream.onClose(closeHandler));
	}

	@Override
	public void close() {
		stream.close();
	}

	@Override
	public StreamEx<T> filter(Predicate<? super T> predicate) {
		return new StreamEx<>(stream.filter(predicate));
	}

	@Override
	public <R> StreamEx<R> map(Function<? super T, ? extends R> mapper) {
		return new StreamEx<>(stream.map(mapper));
	}

	@Override
	public IntStreamEx mapToInt(ToIntFunction<? super T> mapper) {
		return new IntStreamEx(stream.mapToInt(mapper));
	}

	@Override
	public LongStreamEx mapToLong(ToLongFunction<? super T> mapper) {
		return new LongStreamEx(stream.mapToLong(mapper));
	}

	@Override
	public DoubleStreamEx mapToDouble(ToDoubleFunction<? super T> mapper) {
		return new DoubleStreamEx(stream.mapToDouble(mapper));
	}

	@Override
	public <R> StreamEx<R> flatMap(
			Function<? super T, ? extends Stream<? extends R>> mapper) {
		return new StreamEx<>(stream.flatMap(mapper));
	}

	@Override
	public IntStreamEx flatMapToInt(
			Function<? super T, ? extends IntStream> mapper) {
		return new IntStreamEx(stream.flatMapToInt(mapper));
	}

	@Override
	public LongStreamEx flatMapToLong(
			Function<? super T, ? extends LongStream> mapper) {
		return new LongStreamEx(stream.flatMapToLong(mapper));
	}

	@Override
	public DoubleStreamEx flatMapToDouble(
			Function<? super T, ? extends DoubleStream> mapper) {
		return new DoubleStreamEx(stream.flatMapToDouble(mapper));
	}

	@Override
	public StreamEx<T> distinct() {
		return new StreamEx<>(stream.distinct());
	}

	@Override
	public StreamEx<T> sorted() {
		return new StreamEx<>(stream.sorted());
	}

	@Override
	public StreamEx<T> sorted(Comparator<? super T> comparator) {
		return new StreamEx<>(stream.sorted(comparator));
	}

	@Override
	public StreamEx<T> peek(Consumer<? super T> action) {
		return new StreamEx<>(stream.peek(action));
	}

	@Override
	public StreamEx<T> limit(long maxSize) {
		return new StreamEx<>(stream.limit(maxSize));
	}

	@Override
	public StreamEx<T> skip(long n) {
		return new StreamEx<>(stream.skip(n));
	}

	@Override
	public void forEach(Consumer<? super T> action) {
		stream.forEach(action);
	}

	@Override
	public void forEachOrdered(Consumer<? super T> action) {
		stream.forEachOrdered(action);
	}

	@Override
	public Object[] toArray() {
		return stream.toArray();
	}

	@Override
	public <A> A[] toArray(IntFunction<A[]> generator) {
		return stream.toArray(generator);
	}

	@Override
	public T reduce(T identity, BinaryOperator<T> accumulator) {
		return stream.reduce(identity, accumulator);
	}

	@Override
	public Optional<T> reduce(BinaryOperator<T> accumulator) {
		return stream.reduce(accumulator);
	}

	@Override
	public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator,
			BinaryOperator<U> combiner) {
		return stream.reduce(identity, accumulator, combiner);
	}

	@Override
	public <R> R collect(Supplier<R> supplier,
			BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
		return stream.collect(supplier, accumulator, combiner);
	}

	@Override
	public <R, A> R collect(Collector<? super T, A, R> collector) {
		return stream.collect(collector);
	}

	@Override
	public Optional<T> min(Comparator<? super T> comparator) {
		return stream.min(comparator);
	}

	@Override
	public Optional<T> max(Comparator<? super T> comparator) {
		return stream.max(comparator);
	}

	@Override
	public long count() {
		return stream.count();
	}

	@Override
	public boolean anyMatch(Predicate<? super T> predicate) {
		return stream.anyMatch(predicate);
	}

	@Override
	public boolean allMatch(Predicate<? super T> predicate) {
		return stream.allMatch(predicate);
	}

	@Override
	public boolean noneMatch(Predicate<? super T> predicate) {
		return stream.noneMatch(predicate);
	}

	@Override
	public Optional<T> findFirst() {
		return stream.findFirst();
	}

	@Override
	public Optional<T> findAny() {
		return stream.findAny();
	}

	@SuppressWarnings("unchecked")
	public <TT extends T> StreamEx<TT> select(Class<TT> clazz) {
		return new StreamEx<>(stream.filter(clazz::isInstance).map(e -> (TT) e));
	}

	public <R> StreamEx<R> flatCollection(
			Function<? super T, ? extends Collection<? extends R>> mapper) {
		return new StreamEx<>(
				stream.flatMap(mapper.andThen(Collection::stream)));
	}

	public <K> Map<K, List<T>> groupingBy(
			Function<? super T, ? extends K> classifier) {
		return stream.collect(Collectors.groupingBy(classifier));
	}

	public <K, D> Map<K, D> groupingBy(
			Function<? super T, ? extends K> classifier,
			Collector<? super T, ?, D> downstream) {
		return stream.collect(Collectors.groupingBy(classifier, downstream));
	}

	public <K, D, M extends Map<K, D>> M groupingBy(
			Function<? super T, ? extends K> classifier,
			Supplier<M> mapFactory, Collector<? super T, ?, D> downstream) {
		return stream.collect(Collectors.groupingBy(classifier, mapFactory,
				downstream));
	}

	public String joining() {
		return stream.map(String::valueOf).collect(Collectors.joining());
	}

	public String joining(CharSequence separator) {
		return stream.map(String::valueOf).collect(
				Collectors.joining(separator));
	}

	public String joining(CharSequence separator, CharSequence prefix,
			CharSequence suffix) {
		return stream.map(String::valueOf).collect(
				Collectors.joining(separator, prefix, suffix));
	}

	public List<T> toList() {
		return stream.collect(Collectors.toList());
	}

	public Set<T> toSet() {
		return stream.collect(Collectors.toSet());
	}

	public <C extends Collection<T>> C toCollection(
			Supplier<C> collectionFactory) {
		return stream.collect(Collectors.toCollection(collectionFactory));
	}

	public <V> Map<T, V> toMap(Function<T, V> valMapper) {
		return stream.collect(Collectors.toMap(Function.identity(), valMapper));
	}

	public <K, V> Map<K, V> toMap(Function<T, K> keyMapper,
			Function<T, V> valMapper) {
		return stream.collect(Collectors.toMap(keyMapper, valMapper));
	}

	public StreamEx<T> append(@SuppressWarnings("unchecked") T... values) {
		return new StreamEx<>(Stream.concat(stream, Stream.of(values)));
	}
	
	public StreamEx<T> append(Stream<T> other) {
		return new StreamEx<>(Stream.concat(stream, other));
	}

	public StreamEx<T> prepend(@SuppressWarnings("unchecked") T... values) {
		return new StreamEx<>(Stream.concat(Stream.of(values), stream));
	}

	public StreamEx<T> prepend(Stream<T> other) {
		return new StreamEx<>(Stream.concat(other, stream));
	}
	
	public StreamEx<T> nonNull() {
		return new StreamEx<>(stream.filter(Objects::nonNull));
	}

	public StreamEx<T> remove(Predicate<T> predicate) {
		return new StreamEx<>(stream.filter(predicate.negate()));
	}

	public Optional<T> findAny(Predicate<T> predicate) {
		return stream.filter(predicate).findAny();
	}

	public Optional<T> findFirst(Predicate<T> predicate) {
		return stream.filter(predicate).findFirst();
	}
	
	public StreamEx<T> reverseSorted(Comparator<? super T> comparator) {
		return new StreamEx<>(stream.sorted(comparator.reversed()));
	}
	
	public <V extends Comparable<? super V>> StreamEx<T> sortedBy(Function<T, ? extends V> keyExtractor) {
		return new StreamEx<>(stream.sorted(Comparator.comparing(keyExtractor)));
	}

	public <V extends Comparable<? super V>> StreamEx<T> sortedByInt(ToIntFunction<T> keyExtractor) {
		return new StreamEx<>(stream.sorted(Comparator.comparingInt(keyExtractor)));
	}
	
	public <V extends Comparable<? super V>> StreamEx<T> sortedByLong(ToLongFunction<T> keyExtractor) {
		return new StreamEx<>(stream.sorted(Comparator.comparingLong(keyExtractor)));
	}
	
	public <V extends Comparable<? super V>> StreamEx<T> sortedByDouble(ToDoubleFunction<T> keyExtractor) {
		return new StreamEx<>(stream.sorted(Comparator.comparingDouble(keyExtractor)));
	}
	
	@SuppressWarnings("unchecked")
	public static <T> StreamEx<T> empty() {
		return StreamEx.EMPTY;
	}

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

	public static <T> StreamEx<T> of(Stream<T> stream) {
		return new StreamEx<>(stream);
	}

	public static StreamEx<String> ofLines(BufferedReader reader) {
		return new StreamEx<>(reader.lines());
	}

	public static StreamEx<String> ofLines(Reader reader) {
		if(reader instanceof BufferedReader)
			return new StreamEx<>(((BufferedReader) reader).lines());
		return new StreamEx<>(new BufferedReader(reader).lines());
	}
	
	public static <T> StreamEx<T> ofKeys(Map<T, ?> map) {
		return new StreamEx<>(map.keySet().stream());
	}

	public static <T, V> StreamEx<T> ofKeys(Map<T, V> map,
			Predicate<V> valueFilter) {
		return new StreamEx<>(map.entrySet().stream()
				.filter(entry -> valueFilter.test(entry.getValue()))
				.map(Entry::getKey));
	}

	public static <T> StreamEx<T> ofValues(Map<?, T> map) {
		return new StreamEx<>(map.values().stream());
	}

	public static <K, T> StreamEx<T> ofValues(Map<K, T> map,
			Predicate<K> keyFilter) {
		return new StreamEx<>(map.entrySet().stream()
				.filter(entry -> keyFilter.test(entry.getKey()))
				.map(Entry::getValue));
	}

	public static <K, V> StreamEx<Entry<K, V>> ofEntries(Map<K, V> map) {
		return new StreamEx<>(map.entrySet().stream());
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
	
	public static<T> StreamEx<T> generate(Supplier<T> s) {
		return new StreamEx<>(Stream.generate(s));
	}
}
