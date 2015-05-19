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
    default BinaryOperator<A> combiner() {
        BiConsumer<A, A> merger = merger();
        return (a, b) -> {
            merger.accept(a, b);
            return a;
        };
    }

    @Override
    default BiConsumer<A, Long> accumulator() {
        ObjLongConsumer<A> longAccumulator = longAccumulator();
        return (a, i) -> longAccumulator.accept(a, i);
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
            public ObjLongConsumer<A> longAccumulator() {
                return longAccumulator;
            }

            @Override
            public Set<Collector.Characteristics> characteristics() {
                return EnumSet.noneOf(Collector.Characteristics.class);
            }

            @Override
            public BiConsumer<A, A> merger() {
                return merger;
            }
        };
    }

    public static LongCollector<?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                Buffers.joinMerger(delimiter), Buffers.joinFinisher(prefix, suffix));
    }

    public static LongCollector<?, String> joining(CharSequence delimiter) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                Buffers.joinMerger(delimiter), StringBuilder::toString);
    }

    public static LongCollector<?, Long> counting() {
        return of(() -> new long[1], (box, i) -> box[0]++, (box1, box2) -> box1[0] += box2[0], Buffers.UNBOX_LONG);
    }

    public static LongCollector<?, Long> summing() {
        return of(() -> new long[1], (box, i) -> box[0] += i, (box1, box2) -> box1[0] += box2[0], Buffers.UNBOX_LONG);
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
                (box1, box2) -> box1[0] = op.applyAsLong(box1[0], box2[0]), Buffers.UNBOX_LONG);
    }

    public static LongCollector<?, LongSummaryStatistics> summarizing() {
        return of(LongSummaryStatistics::new, LongSummaryStatistics::accept, LongSummaryStatistics::combine);
    }

    public static LongCollector<?, Map<Boolean, long[]>> partitioningBy(LongPredicate predicate) {
        return partitioningBy(predicate, toArray());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <A, D> LongCollector<?, Map<Boolean, D>> partitioningBy(LongPredicate predicate,
            LongCollector<A, D> downstream) {
        ObjLongConsumer<A> downstreamAccumulator = downstream.longAccumulator();
        ObjLongConsumer<BooleanMap<A>> accumulator = (result, t) -> downstreamAccumulator.accept(
                predicate.test(t) ? result.trueValue : result.falseValue, t);
        BiConsumer<BooleanMap<A>, BooleanMap<A>> merger = BooleanMap.merger(downstream.merger());
        Supplier<BooleanMap<A>> supplier = BooleanMap.supplier(downstream.supplier());
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return (LongCollector) of(supplier, accumulator, merger);
        } else {
            return of(supplier, accumulator, merger, BooleanMap.finisher(downstream.finisher()));
        }
    }

    public static <K> LongCollector<?, Map<K, long[]>> groupingBy(LongFunction<? extends K> classifier) {
        return groupingBy(classifier, toArray());
    }

    public static <K, D, A> LongCollector<?, Map<K, D>> groupingBy(LongFunction<? extends K> classifier,
            LongCollector<A, D> downstream) {
        return groupingBy(classifier, HashMap::new, downstream);
    }

    @SuppressWarnings("unchecked")
    public static <K, D, A, M extends Map<K, D>> LongCollector<?, M> groupingBy(LongFunction<? extends K> classifier,
            Supplier<M> mapFactory, LongCollector<A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        Function<K, A> supplier = k -> downstreamSupplier.get();
        ObjLongConsumer<A> downstreamAccumulator = downstream.longAccumulator();
        ObjLongConsumer<Map<K, A>> accumulator = (m, t) -> {
            K key = Objects.requireNonNull(classifier.apply(t));
            A container = m.computeIfAbsent(key, supplier);
            downstreamAccumulator.accept(container, t);
        };
        BiConsumer<Map<K, A>, Map<K, A>> merger = Buffers.mapMerger(downstream.merger());

        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return (LongCollector<?, M>) of((Supplier<Map<K, A>>) mapFactory, accumulator, merger);
        } else {
            return of((Supplier<Map<K, A>>) mapFactory, accumulator, merger,
                    Buffers.mapFinisher((Function<A, A>) downstream.finisher()));
        }
    }

    public static LongCollector<?, long[]> toArray() {
        return of(LongBuffer::new, LongBuffer::add, LongBuffer::addAll, LongBuffer::toArray);
    }
}
