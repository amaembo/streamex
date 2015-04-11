package javax.util.streamex;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntryStream<K, V> extends
		AbstractStreamEx<Entry<K, V>, EntryStream<K, V>> {
	private static class EntryImpl<K, V> implements Entry<K, V> {
		private final K key;
		private V value;

		EntryImpl(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			V oldValue = this.value;
			this.value = value;
			return oldValue;
		}
	}

	EntryStream(Stream<Entry<K, V>> stream) {
		super(stream);
	}

	@Override
	EntryStream<K, V> supply(Stream<Map.Entry<K, V>> stream) {
		return new EntryStream<>(stream);
	};

	<T> EntryStream(Stream<T> stream, Function<T, K> keyMapper,
			Function<T, V> valueMapper) {
		this(stream.map(e -> new EntryImpl<>(keyMapper.apply(e), valueMapper
				.apply(e))));
	}

	@Override
	public <R> StreamEx<R> map(Function<? super Entry<K, V>, ? extends R> mapper) {
		return new StreamEx<>(stream.map(mapper));
	}

	@Override
	public <R> StreamEx<R> flatMap(
			Function<? super Entry<K, V>, ? extends Stream<? extends R>> mapper) {
		return new StreamEx<>(stream.flatMap(mapper));
	}

	public <R> StreamEx<R> flatCollection(
			Function<? super Entry<K, V>, ? extends Collection<? extends R>> mapper) {
		return flatMap(mapper.andThen(Collection::stream));
	}

	public EntryStream<K, V> append(K key, V value) {
		return new EntryStream<>(Stream.concat(stream,
				Stream.of(new EntryImpl<>(key, value))));
	}

	public EntryStream<K, V> prepend(K key, V value) {
		return new EntryStream<>(Stream.concat(
				Stream.of(new EntryImpl<>(key, value)), stream));
	}

	public <KK> EntryStream<KK, V> mapKeys(Function<K, KK> keyMapper) {
		return new EntryStream<>(stream.map(e -> new EntryImpl<>(keyMapper
				.apply(e.getKey()), e.getValue())));
	}

	public <VV> EntryStream<K, VV> mapValues(Function<V, VV> valueMapper) {
		return new EntryStream<>(stream.map(e -> new EntryImpl<>(e.getKey(),
				valueMapper.apply(e.getValue()))));
	}

	public <KK> EntryStream<KK, V> mapEntryKeys(
			Function<Entry<K, V>, KK> keyMapper) {
		return new EntryStream<>(stream.map(e -> new EntryImpl<>(keyMapper
				.apply(e), e.getValue())));
	}

	public <VV> EntryStream<K, VV> mapEntryValues(
			Function<Entry<K, V>, VV> valueMapper) {
		return new EntryStream<>(stream.map(e -> new EntryImpl<>(e.getKey(),
				valueMapper.apply(e))));
	}

	public EntryStream<K, V> filterKeys(Predicate<K> keyPredicate) {
		return new EntryStream<>(stream.filter(e -> keyPredicate.test(e
				.getKey())));
	}

	public EntryStream<K, V> filterValues(Predicate<V> valuePredicate) {
		return new EntryStream<>(stream.filter(e -> valuePredicate.test(e
				.getValue())));
	}

	public StreamEx<K> keys() {
		return new StreamEx<>(stream.map(Entry::getKey));
	}

	public StreamEx<V> values() {
		return new StreamEx<>(stream.map(Entry::getValue));
	}

	public Map<K, V> toMap() {
		return stream.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	public <M extends Map<K, V>> M toMap(Supplier<M> mapSupplier) {
		return stream.collect(Collectors.toMap(
				Entry::getKey,
				Entry::getValue,
				(u, v) -> {
					throw new IllegalStateException(String.format(
							"Duplicate key %s", u));
				}, mapSupplier));
	}

	public Map<K, List<V>> grouping() {
		return stream.collect(Collectors.groupingBy(Entry::getKey,
				Collectors.mapping(Entry::getValue, Collectors.toList())));
	}

	public <M extends Map<K, List<V>>> M grouping(Supplier<M> mapSupplier) {
		return stream.collect(Collectors.groupingBy(Entry::getKey, mapSupplier,
				Collectors.mapping(Entry::getValue, Collectors.toList())));
	}

	public <A, D> Map<K, D> grouping(Collector<? super V, A, D> downstream) {
		return stream.collect(Collectors.groupingBy(Entry::getKey, Collectors
				.<Entry<K, V>, V, A, D> mapping(Entry::getValue, downstream)));
	}

	public <A, D, M extends Map<K, D>> M grouping(Supplier<M> mapSupplier,
			Collector<? super V, A, D> downstream) {
		return stream.collect(Collectors.groupingBy(Entry::getKey, mapSupplier,
				Collectors.<Entry<K, V>, V, A, D> mapping(Entry::getValue,
						downstream)));
	}

	public <C extends Collection<V>> Map<K, C> groupingTo(
			Supplier<C> collectionFactory) {
		return stream.collect(Collectors.groupingBy(
				Entry::getKey,
				Collectors.mapping(Entry::getValue,
						Collectors.toCollection(collectionFactory))));
	}

	public <C extends Collection<V>, M extends Map<K, C>> M groupingTo(
			Supplier<M> mapSupplier, Supplier<C> collectionFactory) {
		return stream.collect(Collectors.groupingBy(
				Entry::getKey,
				mapSupplier,
				Collectors.mapping(Entry::getValue,
						Collectors.toCollection(collectionFactory))));
	}

	public static <K, V> EntryStream<K, V> of(Map<K, V> map) {
		return new EntryStream<>(map.entrySet().stream());
	}
}
