/*
 * Copyright 2016 Tagir Valeev
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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import one.util.streamex.MoreCollectors;

/**
 * @author Tagir Valeev
 *
 * @param <T> initial type of the elements to collect
 * @param <R> current type of the elements
 */
public class StreamCollector<T, R> {
	private static final Collector<Object, ?, List<Object>> LIST = Collectors.toList();
	private final StreamCollector<?, ?> upstream;
	private final Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker;

	@SuppressWarnings("unchecked")
	private static <T> Collector<T, ?, List<T>> list() {
		return (Collector<T, ?, List<T>>) (Collector<?, ?, ?>) LIST;
	}

	@SuppressWarnings("unchecked")
	private static <T, A, R> Collector<T, ?, R> fromList(Collector<T, ?, List<T>> upstream,
			Collector<T, A, R> downstream) {
		if (downstream == LIST)
			return (Collector<T, ?, R>) upstream;
		return MoreCollectors.collectingAndThen(upstream,
				list -> list.stream().collect(downstream));
	}

	StreamCollector(StreamCollector<T, ?> upstream,
			Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker) {
		this.upstream = upstream;
		this.maker = maker;
	}

	public <R1> StreamCollector<T, R1> map(Function<? super R, ? extends R1> mapper) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> MoreCollectors.mapping(
				mapper, (Collector) c);
		return new StreamCollector<>(this, maker);
	}

	public <R1> StreamCollector<T, R1> flatMap(
			Function<? super R, ? extends Stream<? extends R1>> mapper) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> MoreCollectors
				.flatMapping(mapper, (Collector) c);
		return new StreamCollector<>(this, maker);
	}

	public StreamCollector<T, R> limit(int count) {
		Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> fromList(
				MoreCollectors.head(count), c);
		return new StreamCollector<>(this, maker);
	}

	public StreamCollector<T, R> distinct() {
		@SuppressWarnings({ "unchecked", "rawtypes" })
        Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> fromList(
				MoreCollectors.distinctBy(Function.identity()), (Collector)c);
		return new StreamCollector<>(this, maker);
	}

	public StreamCollector<T, R> sorted() {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> fromList(
				MoreCollectors.collectingAndThen(LIST, l -> {
					l.sort(null);
					return l;
				}), (Collector) c);
		return new StreamCollector<>(this, maker);
	}

	public StreamCollector<T, R> sorted(Comparator<? super R> cmp) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> fromList(
				MoreCollectors.collectingAndThen(LIST, l -> {
					l.sort((Comparator<? super Object>) cmp);
					return l;
				}), (Collector) c);
		return new StreamCollector<>(this, maker);
	}
	
	public StreamCollector<T, R> filter(Predicate<? super R> predicate) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> MoreCollectors
				.filtering(predicate, (Collector) c);
		return new StreamCollector<>(this, maker);
	}

	public <K, D> Collector<T, ?, Map<K, D>> groupingBy(
			Function<? super R, ? extends K> classifier,
			Function<StreamCollector<R, R>, Collector<R, ?, D>> builder) {
		return evaluate(Collectors.groupingBy(classifier, build(builder)));
	}

	public <K> Collector<T, ?, Map<K, List<R>>> groupingBy(
			Function<? super R, ? extends K> classifier) {
		return evaluate(Collectors.groupingBy(classifier, list()));
	}
	
	public Collector<T, ?, Object[]> toArray() {
		return evaluate(MoreCollectors.toArray(Object[]::new));
	}

	public <A> Collector<T, ?, A[]> toArray(IntFunction<A[]> generator) {
		return evaluate(MoreCollectors.toArray(generator));
	}
	
	public <A, U> Collector<T, ?, U> collect(Collector<R, A, U> collector) {
		return evaluate(collector);
	}

	public Collector<T, ?, List<R>> toList() {
		return evaluate(list());
	}
	
	public Collector<T, ?, Set<R>> toSet() {
		return evaluate(Collectors.toSet());
	}
	
	public Collector<T, ?, Optional<R>> max(Comparator<? super R> cmp) {
		return evaluate(Collectors.maxBy(cmp));
	}

	public Collector<T, ?, Long> count() {
		return evaluate(Collectors.summingLong(x -> 1L));
	}
	
	public Collector<T, ?, Optional<R>> min(Comparator<? super R> cmp) {
		return evaluate(Collectors.minBy(cmp));
	}
	
	public Collector<T, ?, Optional<R>> findFirst() {
		return evaluate(MoreCollectors.first());
	}

	public Collector<T, ?, String> joining() {
		return this.<CharSequence> map(Objects::toString).evaluate(Collectors.joining());
	}

	public Collector<T, ?, String> joining(String delimiter) {
		return this.<CharSequence> map(Objects::toString).evaluate(
				Collectors.joining(delimiter));
	}

	@SuppressWarnings("unchecked")
	private <U> Collector<T, ?, U> evaluate(Collector<R, ?, U> finisher) {
		Collector<?, ?, ?> cur = finisher;
		StreamCollector<?, ?> sc = this;
		while (sc != null) {
			cur = sc.maker.apply(cur);
			sc = sc.upstream;
		}
		return (Collector<T, ?, U>) cur;
	}

	static <T, U> Collector<T, ?, U> build(
			Function<StreamCollector<T, T>, Collector<T, ?, U>> builder) {
		return builder.apply(new StreamCollector<>(null, Function.identity()));
	}
}
