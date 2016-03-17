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
import java.util.function.BinaryOperator;
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
 * 
 * TODO: three-arg reduce
 * TODO: skip
 * TODO: toMap-merge
 * TODO: pairMap
 */
public class CollectorBuilder<T, R> {
    private static final Collector<Object, ?, List<Object>> LIST = Collectors.toList();
    private final CollectorBuilder<?, ?> upstream;
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

    private CollectorBuilder(CollectorBuilder<T, ?> upstream,
            Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker) {
        this.upstream = upstream;
        this.maker = maker;
    }

    public <R1> CollectorBuilder<T, R1> map(Function<? super R, ? extends R1> mapper) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> MoreCollectors.mapping(
                mapper, (Collector) c);
        return new CollectorBuilder<>(this, maker);
    }

    public <R1> CollectorBuilder<T, R1> flatMap(
            Function<? super R, ? extends Stream<? extends R1>> mapper) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> MoreCollectors
                .flatMapping(mapper, (Collector) c);
        return new CollectorBuilder<>(this, maker);
    }

    public CollectorBuilder<T, R> limit(int count) {
        Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> fromList(
                MoreCollectors.head(count), c);
        return new CollectorBuilder<>(this, maker);
    }

    public CollectorBuilder<T, R> distinct() {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> fromList(
                MoreCollectors.distinctBy(Function.identity()), (Collector)c);
        return new CollectorBuilder<>(this, maker);
    }

    public CollectorBuilder<T, R> sorted() {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> fromList(
                MoreCollectors.collectingAndThen(LIST, l -> {
                    l.sort(null);
                    return l;
                }), (Collector) c);
        return new CollectorBuilder<>(this, maker);
    }

    public CollectorBuilder<T, R> sorted(Comparator<? super R> cmp) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> fromList(
                MoreCollectors.collectingAndThen(LIST, l -> {
                    l.sort((Comparator<? super Object>) cmp);
                    return l;
                }), (Collector) c);
        return new CollectorBuilder<>(this, maker);
    }
    
    public CollectorBuilder<T, R> filter(Predicate<? super R> predicate) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Function<Collector<?, ?, ?>, Collector<?, ?, ?>> maker = c -> MoreCollectors
                .filtering(predicate, (Collector) c);
        return new CollectorBuilder<>(this, maker);
    }

    public <K, A, D> Collector<T, ?, Map<K, D>> groupingBy(
            Function<? super R, ? extends K> classifier,
            Function<? super CollectorBuilder<R, R>, ? extends Collector<R, A, D>> builder) {
        return evaluate(Collectors.groupingBy(classifier, build(builder)));
    }

    public <K> Collector<T, ?, Map<K, List<R>>> groupingBy(
            Function<? super R, ? extends K> classifier) {
        return evaluate(Collectors.groupingBy(classifier, list()));
    }
    
    public Collector<T, ?, Map<Boolean, List<R>>> partitioningBy(
            Predicate<? super R> predicate) {
        return evaluate(MoreCollectors.partitioningBy(predicate, list()));
    }
    
    public <A, D> Collector<T, ?, Map<Boolean, D>> partitioningBy(
            Predicate<? super R> predicate, 
            Function<? super CollectorBuilder<R, R>, ? extends Collector<R, A, D>> builder) {
        return evaluate(MoreCollectors.partitioningBy(predicate, build(builder)));
    }
    
    public <A, U> Collector<T, ?, U> collect(Collector<R, A, U> collector) {
        return evaluate(collector);
    }

    public Collector<T, ?, Object[]> toArray() {
        return toArray(Object[]::new);
    }

    @SuppressWarnings({ "unchecked" })
    public <A> Collector<T, ?, A[]> toArray(IntFunction<A[]> generator) {
        return evaluate((Collector<R, ?, A[]>)(Collector<?,?,?>)MoreCollectors.toArray(generator));
    }

    public Collector<T, ?, List<R>> toList() {
        return evaluate(list());
    }
    
    public Collector<T, ?, Set<R>> toSet() {
        return evaluate(Collectors.toSet());
    }
    
    public <K, V> Collector<T, ?, Map<K, V>> toMap(Function<? super R, ? extends K> keyMapper,
            Function<? super R, ? extends V> valueMapper) {
        return evaluate(Collectors.toMap(keyMapper, valueMapper));
    }
    
    public <V> Collector<T, ?, Map<R, V>> toMap(Function<? super R, ? extends V> valueMapper) {
        return evaluate(Collectors.toMap(Function.identity(), valueMapper));
    }
    
    public Collector<T, ?, Long> count() {
        return evaluate(Collectors.summingLong(x -> 1L));
    }
    
    public OptionalCollector<T, ?, R> reduce(BinaryOperator<R> op) {
        return evaluate(MoreCollectors.reducing(op));
    }

    public Collector<T, ?, R> reduce(R identity, BinaryOperator<R> op) {
        return evaluate(Collectors.reducing(identity, op));
    }
    
    public OptionalCollector<T, ?, R> max(Comparator<? super R> cmp) {
        OptionalCollector<R, ?, R> c = MoreCollectors.maxBy(cmp);
        return evaluate(c);
    }

    public OptionalCollector<T, ?, R> min(Comparator<? super R> cmp) {
        OptionalCollector<R, ?, R> c = MoreCollectors.minBy(cmp);
        return evaluate(c);
    }
    
    public OptionalCollector<T, ?, R> findFirst() {
        OptionalCollector<R, ?, R> c = MoreCollectors.first();
        return evaluate(c);
    }

    public Collector<T, ?, String> joining() {
        return this.<CharSequence> map(Objects::toString).evaluate(Collectors.joining());
    }

    public Collector<T, ?, String> joining(String delimiter) {
        return this.<CharSequence> map(Objects::toString).evaluate(Collectors.joining(delimiter));
    }

    public Collector<T, ?, String> joining(String delimiter, String prefix, String suffix) {
        return this.<CharSequence> map(Objects::toString).evaluate(Collectors.joining(delimiter, prefix, suffix));
    }
    
    @SuppressWarnings("unchecked")
    private <U> Collector<T, ?, U> evaluate(Collector<R, ?, U> finisher) {
        Collector<?, ?, ?> cur = finisher;
        CollectorBuilder<?, ?> sc = this;
        while (sc != null) {
            cur = sc.maker.apply(cur);
            sc = sc.upstream;
        }
        return (Collector<T, ?, U>) cur;
    }

    private <U> OptionalCollector<T, ?, U> evaluate(OptionalCollector<R, ?, U> finisher) {
        Collector<T, ?, Optional<U>> collector = evaluate((Collector<R, ?, Optional<U>>)finisher);
        return collector instanceof OptionalCollector ?
                (OptionalCollector<T, ?, U>)collector :
                    StreamExInternals.collectingAndThen(collector, null);
    }
    
    static <T, R> Collector<T, ?, R> build(
            Function<? super CollectorBuilder<T, T>, ? extends Collector<T, ?, R>> builder) {
        return builder.apply(new CollectorBuilder<>(null, Function.identity()));
    }
}
