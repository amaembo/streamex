package javax.util.streamex;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

import javax.util.streamex.Buffers.LongBuffer;
import javax.util.streamex.Buffers.BooleanMap;

public interface LongCollector<A, R> extends Collector<Long, A, R> {
    /**
     * A function that folds a value into a mutable result container.
     *
     * @return a function which folds a value into a mutable result container
     */
    ObjLongConsumer<A> longAccumulator();

    BiConsumer<A, A> merger();

    @Override
    default BiConsumer<A, Long> accumulator() {
        ObjLongConsumer<A> longAccumulator = longAccumulator();
        return (a, i) -> longAccumulator.accept(a, i);
    }

    @Override
    default BinaryOperator<A> combiner() {
        BiConsumer<A, A> merger = merger();
        return (a, b) -> {
            merger.accept(a, b);
            return a;
        };
    }

    static <R> LongCollector<R, R> of(Supplier<R> supplier, ObjLongConsumer<R> longAccumulator, BiConsumer<R, R> merger) {
        return new LongCollector<R, R>() {

            @Override
            public Supplier<R> supplier() {
                return supplier;
            }

            @Override
            public Function<R, R> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Collector.Characteristics> characteristics() {
                return EnumSet.of(Collector.Characteristics.IDENTITY_FINISH);
            }

            @Override
            public ObjLongConsumer<R> longAccumulator() {
                return longAccumulator;
            }

            @Override
            public BiConsumer<R, R> merger() {
                return merger;
            }
        };
    }

    static <A, R> LongCollector<?, R> of(Collector<Long, A, R> collector) {
        if (collector instanceof LongCollector) {
            return (LongCollector<A, R>) collector;
        }
        return mappingToObj(i -> i, collector);
    }

    static <A, R> LongCollector<A, R> of(Supplier<A> supplier, ObjLongConsumer<A> longAccumulator,
            BiConsumer<A, A> merger, Function<A, R> finisher) {
        return new LongCollector<A, R>() {

            @Override
            public Supplier<A> supplier() {
                return supplier;
            }

            @Override
            public Function<A, R> finisher() {
                return finisher;
            }

            @Override
            public Set<Collector.Characteristics> characteristics() {
                return EnumSet.noneOf(Collector.Characteristics.class);
            }

            @Override
            public ObjLongConsumer<A> longAccumulator() {
                return longAccumulator;
            }

            @Override
            public BiConsumer<A, A> merger() {
                return merger;
            }
        };
    }

    public static LongCollector<?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                (sb1, sb2) -> {
                    if (sb2.length() > 0) {
                        if (sb1.length() > 0)
                            sb1.append(delimiter);
                        sb1.append(sb2);
                    }
                }, sb -> new StringBuilder().append(prefix).append(sb).append(suffix).toString());
    }

    public static LongCollector<?, String> joining(CharSequence delimiter) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                (sb1, sb2) -> {
                    if (sb2.length() > 0) {
                        if (sb1.length() > 0)
                            sb1.append(delimiter);
                        sb1.append(sb2);
                    }
                }, StringBuilder::toString);
    }

    public static LongCollector<?, Long> counting() {
        return of(() -> new long[1], (box, i) -> box[0]++, (box1, box2) -> box1[0] += box2[0], box -> box[0]);
    }

    public static LongCollector<?, Long> summing() {
        return of(() -> new long[1], (box, i) -> box[0] += i, (box1, box2) -> box1[0] += box2[0], box -> box[0]);
    }

    public static LongCollector<?, OptionalLong> min() {
        return reducing((a, b) -> a > b ? b : a);
    }

    public static LongCollector<?, OptionalLong> max() {
        return reducing((a, b) -> a > b ? a : b);
    }

    @SuppressWarnings("unchecked")
    public static <A, R> LongCollector<?, R> mapping(LongUnaryOperator mapper, LongCollector<A, R> downstream) {
        ObjLongConsumer<A> downstreamAccumulator = downstream.longAccumulator();
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH))
            return (LongCollector<?, R>) of(downstream.supplier(),
                    (r, t) -> downstreamAccumulator.accept(r, mapper.applyAsLong(t)), downstream.merger());
        return of(downstream.supplier(), (r, t) -> downstreamAccumulator.accept(r, mapper.applyAsLong(t)),
                downstream.merger(), downstream.finisher());
    }

    @SuppressWarnings("unchecked")
    public static <U, A, R> LongCollector<?, R> mappingToObj(LongFunction<U> mapper, Collector<U, A, R> downstream) {
        Supplier<A> supplier = downstream.supplier();
        BiConsumer<A, U> accumulator = downstream.accumulator();
        BinaryOperator<A> combiner = downstream.combiner();
        Function<A, R> finisher = downstream.finisher();
        return of(() -> new Object[] { supplier.get() }, (box, i) -> accumulator.accept((A) box[0], mapper.apply(i)), (
                box1, box2) -> box1[0] = combiner.apply((A) box1[0], (A) box2[0]), box -> finisher.apply((A) box[0]));
    }

    public static <A, R, RR> LongCollector<A, RR> collectingAndThen(LongCollector<A, R> collector,
            Function<R, RR> finisher) {
        return of(collector.supplier(), collector.longAccumulator(), collector.merger(),
                collector.finisher().andThen(finisher));
    }

    public static LongCollector<?, OptionalLong> reducing(LongBinaryOperator op) {
        return of(() -> new long[2], (box, i) -> {
            if (box[1] == 0) {
                box[0] = i;
                box[1] = 1;
            } else {
                box[0] = op.applyAsLong(box[0], i);
            }
        }, (box1, box2) -> {
            if (box2[1] == 1) {
                if (box1[1] == 0) {
                    box1[0] = box2[0];
                    box1[1] = 1;
                } else {
                    box1[0] = op.applyAsLong(box1[0], box2[0]);
                }
            }
        }, box -> box[1] == 1 ? OptionalLong.of(box[0]) : OptionalLong.empty());
    }

    public static LongCollector<?, Long> reducing(long identity, LongBinaryOperator op) {
        return of(() -> new long[] { identity }, (box, i) -> box[0] = op.applyAsLong(box[0], i),
                (box1, box2) -> box1[0] = op.applyAsLong(box1[0], box2[0]), box -> box[0]);
    }

    public static LongCollector<?, LongSummaryStatistics> summarizing() {
        return of(LongSummaryStatistics::new, LongSummaryStatistics::accept, LongSummaryStatistics::combine);
    }

    public static LongCollector<?, Map<Boolean, long[]>> partitioningBy(LongPredicate predicate) {
        return partitioningBy(predicate, toArray());
    }

    public static <A, D> LongCollector<?, Map<Boolean, D>> partitioningBy(LongPredicate predicate,
            LongCollector<A, D> downstream) {
        ObjLongConsumer<A> downstreamAccumulator = downstream.longAccumulator();
        ObjLongConsumer<BooleanMap<A>> accumulator = (result, t) -> downstreamAccumulator.accept(
                predicate.test(t) ? result.trueValue : result.falseValue, t);
        BiConsumer<A, A> op = downstream.merger();
        BiConsumer<BooleanMap<A>, BooleanMap<A>> merger = (left, right) -> {
            op.accept(left.trueValue, right.trueValue);
            op.accept(left.falseValue, right.falseValue);
        };
        Supplier<BooleanMap<A>> supplier = () -> new BooleanMap<>(downstream.supplier().get(), downstream.supplier()
                .get());
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            LongCollector<?, Map<Boolean, D>> result = (LongCollector) of(supplier, accumulator, merger);
            return result;
        } else {
            Function<BooleanMap<A>, Map<Boolean, D>> finisher = par -> new BooleanMap<>(downstream.finisher().apply(
                    par.trueValue), downstream.finisher().apply(par.falseValue));
            return of(supplier, accumulator, merger, finisher);
        }
    }

    public static <K> LongCollector<?, Map<K, long[]>> groupingBy(LongFunction<? extends K> classifier) {
        return groupingBy(classifier, HashMap::new, toArray());
    }

    public static <K, D, A> LongCollector<?, Map<K, D>> groupingBy(LongFunction<? extends K> classifier,
            LongCollector<A, D> downstream) {
        return groupingBy(classifier, HashMap::new, downstream);
    }

    public static <K, D, A, M extends Map<K, D>> LongCollector<?, M> groupingBy(LongFunction<? extends K> classifier,
            Supplier<M> mapFactory, LongCollector<A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        ObjLongConsumer<A> downstreamAccumulator = downstream.longAccumulator();
        ObjLongConsumer<Map<K, A>> accumulator = (m, t) -> {
            K key = Objects.requireNonNull(classifier.apply(t));
            A container = m.computeIfAbsent(key, k -> downstreamSupplier.get());
            downstreamAccumulator.accept(container, t);
        };
        BiConsumer<A, A> downstreamMerger = downstream.merger();
        BiConsumer<Map<K, A>, Map<K, A>> merger = (m1, m2) -> {
            for (Map.Entry<K, A> e : m2.entrySet())
                m1.merge(e.getKey(), e.getValue(), (a, b) -> {
                    downstreamMerger.accept(a, b);
                    return a;
                });
        };

        @SuppressWarnings("unchecked")
        Supplier<Map<K, A>> mangledFactory = (Supplier<Map<K, A>>) mapFactory;

        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            @SuppressWarnings("unchecked")
            LongCollector<?, M> result = (LongCollector<?, M>) of(mangledFactory, accumulator, merger);
            return result;
        } else {
            @SuppressWarnings("unchecked")
            Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();
            Function<Map<K, A>, M> finisher = intermediate -> {
                intermediate.replaceAll((k, v) -> downstreamFinisher.apply(v));
                @SuppressWarnings("unchecked")
                M castResult = (M) intermediate;
                return castResult;
            };
            return of(mangledFactory, accumulator, merger, finisher);
        }
    }

    public static LongCollector<?, long[]> toArray() {
        return of(LongBuffer::new, LongBuffer::add, LongBuffer::addAll, LongBuffer::toArray);
    }
}
