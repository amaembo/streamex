package javax.util.streamex;

import java.util.DoubleSummaryStatistics;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

import javax.util.streamex.Buffers.DoubleBuffer;
import javax.util.streamex.Buffers.FloatBuffer;
import javax.util.streamex.Buffers.BooleanMap;

public interface DoubleCollector<A, R> extends Collector<Double, A, R> {
    /**
     * A function that folds a value into a mutable result container.
     *
     * @return a function which folds a value into a mutable result container
     */
    ObjDoubleConsumer<A> doubleAccumulator();

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
    default BiConsumer<A, Double> accumulator() {
        ObjDoubleConsumer<A> doubleAccumulator = doubleAccumulator();
        return (a, i) -> doubleAccumulator.accept(a, i);
    }

    static <R> DoubleCollector<R, R> of(Supplier<R> supplier, ObjDoubleConsumer<R> doubleAccumulator,
            BiConsumer<R, R> merger) {
        return new DoubleCollector<R, R>() {

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
            public ObjDoubleConsumer<R> doubleAccumulator() {
                return doubleAccumulator;
            }

            @Override
            public BiConsumer<R, R> merger() {
                return merger;
            }
        };
    }

    static <A, R> DoubleCollector<?, R> of(Collector<Double, A, R> collector) {
        if (collector instanceof DoubleCollector) {
            return (DoubleCollector<A, R>) collector;
        }
        return mappingToObj(i -> i, collector);
    }

    static <A, R> DoubleCollector<A, R> of(Supplier<A> supplier, ObjDoubleConsumer<A> doubleAccumulator,
            BiConsumer<A, A> merger, Function<A, R> finisher) {
        return new DoubleCollector<A, R>() {

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
            public ObjDoubleConsumer<A> doubleAccumulator() {
                return doubleAccumulator;
            }

            @Override
            public BiConsumer<A, A> merger() {
                return merger;
            }
        };
    }

    public static DoubleCollector<?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                Buffers.joinMerger(delimiter), Buffers.joinFinisher(prefix, suffix));
    }

    public static DoubleCollector<?, String> joining(CharSequence delimiter) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                Buffers.joinMerger(delimiter), StringBuilder::toString);
    }

    public static DoubleCollector<?, Long> counting() {
        return of(() -> new long[1], (box, i) -> box[0]++, (box1, box2) -> box1[0] += box2[0], Buffers.UNBOX_LONG);
    }

    public static DoubleCollector<?, Double> summing() {
        // Using DoubleSummaryStatistics as Kahan algorithm is implemented there
        return collectingAndThen(summarizing(), DoubleSummaryStatistics::getSum);
    }

    public static DoubleCollector<?, OptionalDouble> min() {
        return reducing((a, b) -> Double.compare(a, b) > 0 ? b : a);
    }

    public static DoubleCollector<?, OptionalDouble> max() {
        return reducing((a, b) -> Double.compare(a, b) > 0 ? a : b);
    }

    @SuppressWarnings("unchecked")
    public static <A, R> DoubleCollector<?, R> mapping(DoubleUnaryOperator mapper, DoubleCollector<A, R> downstream) {
        ObjDoubleConsumer<A> downstreamAccumulator = downstream.doubleAccumulator();
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH))
            return (DoubleCollector<?, R>) of(downstream.supplier(),
                    (r, t) -> downstreamAccumulator.accept(r, mapper.applyAsDouble(t)), downstream.merger());
        return of(downstream.supplier(), (r, t) -> downstreamAccumulator.accept(r, mapper.applyAsDouble(t)),
                downstream.merger(), downstream.finisher());
    }

    @SuppressWarnings("unchecked")
    public static <U, A, R> DoubleCollector<?, R> mappingToObj(DoubleFunction<U> mapper, Collector<U, A, R> downstream) {
        Supplier<A> supplier = downstream.supplier();
        BiConsumer<A, U> accumulator = downstream.accumulator();
        BinaryOperator<A> combiner = downstream.combiner();
        Function<A, R> finisher = downstream.finisher();
        return of(() -> new Object[] { supplier.get() }, (box, i) -> accumulator.accept((A) box[0], mapper.apply(i)), (
                box1, box2) -> box1[0] = combiner.apply((A) box1[0], (A) box2[0]), box -> finisher.apply((A) box[0]));
    }

    public static <A, R, RR> DoubleCollector<A, RR> collectingAndThen(DoubleCollector<A, R> collector,
            Function<R, RR> finisher) {
        return of(collector.supplier(), collector.doubleAccumulator(), collector.merger(), collector.finisher()
                .andThen(finisher));
    }

    public static DoubleCollector<?, OptionalDouble> reducing(DoubleBinaryOperator op) {
        return of(() -> new double[2], (box, i) -> {
            if (box[1] == 0) {
                box[0] = i;
                box[1] = 1;
            } else {
                box[0] = op.applyAsDouble(box[0], i);
            }
        }, (box1, box2) -> {
            if (box2[1] == 1) {
                if (box1[1] == 0) {
                    box1[0] = box2[0];
                    box1[1] = 1;
                } else {
                    box1[0] = op.applyAsDouble(box1[0], box2[0]);
                }
            }
        }, box -> box[1] == 1 ? OptionalDouble.of(box[0]) : OptionalDouble.empty());
    }

    public static DoubleCollector<?, Double> reducing(double identity, DoubleBinaryOperator op) {
        return of(() -> new double[] { identity }, (box, i) -> box[0] = op.applyAsDouble(box[0], i),
                (box1, box2) -> box1[0] = op.applyAsDouble(box1[0], box2[0]), Buffers.UNBOX_DOUBLE);
    }

    public static DoubleCollector<?, DoubleSummaryStatistics> summarizing() {
        return of(DoubleSummaryStatistics::new, DoubleSummaryStatistics::accept, DoubleSummaryStatistics::combine);
    }

    public static DoubleCollector<?, Map<Boolean, double[]>> partitioningBy(DoublePredicate predicate) {
        return partitioningBy(predicate, toArray());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <A, D> DoubleCollector<?, Map<Boolean, D>> partitioningBy(DoublePredicate predicate,
            DoubleCollector<A, D> downstream) {
        ObjDoubleConsumer<A> downstreamAccumulator = downstream.doubleAccumulator();
        ObjDoubleConsumer<BooleanMap<A>> accumulator = (result, t) -> downstreamAccumulator.accept(
                predicate.test(t) ? result.trueValue : result.falseValue, t);
        BiConsumer<BooleanMap<A>, BooleanMap<A>> merger = BooleanMap.merger(downstream.merger());
        Supplier<BooleanMap<A>> supplier = BooleanMap.supplier(downstream.supplier());
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return (DoubleCollector) of(supplier, accumulator, merger);
        } else {
            return of(supplier, accumulator, merger, BooleanMap.finisher(downstream.finisher()));
        }
    }

    public static <K> DoubleCollector<?, Map<K, double[]>> groupingBy(DoubleFunction<? extends K> classifier) {
        return groupingBy(classifier, toArray());
    }

    public static <K, D, A> DoubleCollector<?, Map<K, D>> groupingBy(DoubleFunction<? extends K> classifier,
            DoubleCollector<A, D> downstream) {
        return groupingBy(classifier, HashMap::new, downstream);
    }

    @SuppressWarnings("unchecked")
    public static <K, D, A, M extends Map<K, D>> DoubleCollector<?, M> groupingBy(
            DoubleFunction<? extends K> classifier, Supplier<M> mapFactory, DoubleCollector<A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        Function<K, A> supplier = k -> downstreamSupplier.get();
        ObjDoubleConsumer<A> downstreamAccumulator = downstream.doubleAccumulator();
        ObjDoubleConsumer<Map<K, A>> accumulator = (m, t) -> {
            K key = Objects.requireNonNull(classifier.apply(t));
            A container = m.computeIfAbsent(key, supplier);
            downstreamAccumulator.accept(container, t);
        };
        BiConsumer<Map<K, A>, Map<K, A>> merger = Buffers.mapMerger(downstream.merger());

        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return (DoubleCollector<?, M>) of((Supplier<Map<K, A>>) mapFactory, accumulator, merger);
        } else {
            return of((Supplier<Map<K, A>>) mapFactory, accumulator, merger,
                    Buffers.mapFinisher((Function<A, A>) downstream.finisher()));
        }
    }

    public static DoubleCollector<?, double[]> toArray() {
        return of(DoubleBuffer::new, DoubleBuffer::add, DoubleBuffer::addAll, DoubleBuffer::toArray);
    }

    public static DoubleCollector<?, float[]> toFloatArray() {
        return of(FloatBuffer::new, FloatBuffer::add, FloatBuffer::addAll, FloatBuffer::toArray);
    }
}
