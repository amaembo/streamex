package javax.util.streamex;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;

import static javax.util.streamex.StreamExInternals.*;

public final class MoreCollectors {
    private MoreCollectors() {
        throw new UnsupportedOperationException();
    }

    public static <T> Collector<T, ?, T[]> toArray(IntFunction<T[]> supplier) {
        return Collectors.collectingAndThen(Collectors.toList(), list -> list.toArray(supplier.apply(list.size())));
    }

    public static <T, U> Collector<T, ?, Integer> distinctCount(Function<T, U> mapper) {
        return Collectors.collectingAndThen(Collectors.mapping(mapper, Collectors.toSet()), Set::size);
    }

    public static <T, A1, A2, R1, R2, R> Collector<T, ?, R> pairing(Collector<T, A1, R1> c1, Collector<T, A2, R2> c2,
            BiFunction<R1, R2, R> finisher) {
        EnumSet<Characteristics> c = EnumSet.noneOf(Characteristics.class);
        c.addAll(c1.characteristics());
        c.retainAll(c2.characteristics());
        c.remove(Characteristics.IDENTITY_FINISH);

        Supplier<A1> c1Supplier = c1.supplier();
        Supplier<A2> c2Supplier = c2.supplier();
        BiConsumer<A1, T> c1Accumulator = c1.accumulator();
        BiConsumer<A2, T> c2Accumulator = c2.accumulator();
        BinaryOperator<A1> c1Combiner = c1.combiner();
        BinaryOperator<A2> c2combiner = c2.combiner();

        Supplier<PairBox<A1, A2>> supplier = () -> new PairBox<>(c1Supplier.get(), c2Supplier.get());
        BiConsumer<PairBox<A1, A2>, T> accumulator = (acc, v) -> {
            c1Accumulator.accept(acc.a, v);
            c2Accumulator.accept(acc.b, v);
        };
        BinaryOperator<PairBox<A1, A2>> combiner = (acc1, acc2) -> {
            acc1.a = c1Combiner.apply(acc1.a, acc2.a);
            acc1.b = c2combiner.apply(acc1.b, acc2.b);
            return acc1;
        };
        return Collector.of(supplier, accumulator, combiner, acc -> {
            R1 r1 = c1.finisher().apply(acc.a);
            R2 r2 = c2.finisher().apply(acc.b);
            return finisher.apply(r1, r2);
        }, c.toArray(new Characteristics[c.size()]));
    }

    public static <T> Collector<T, ?, List<T>> maxAll(Comparator<? super T> comparator) {
        return maxAll(comparator, Collectors.toList());
    }

    public static <T, A, D> Collector<T, ?, D> maxAll(Comparator<? super T> comparator,
            Collector<? super T, A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BinaryOperator<A> downstreamCombiner = downstream.combiner();
        @SuppressWarnings("unchecked")
        Supplier<PairBox<A, T>> supplier = () -> new PairBox<>(downstreamSupplier.get(), (T) NONE);
        BiConsumer<PairBox<A, T>, T> accumulator = (acc, t) -> {
            if (acc.b == NONE) {
                downstreamAccumulator.accept(acc.a, t);
                acc.b = t;
            } else {
                int cmp = comparator.compare(t, acc.b);
                if (cmp > 0) {
                    acc.a = downstreamSupplier.get();
                    acc.b = t;
                }
                if (cmp >= 0)
                    downstreamAccumulator.accept(acc.a, t);
            }
        };
        BinaryOperator<PairBox<A, T>> combiner = (acc1, acc2) -> {
            if (acc2.b == NONE) {
                return acc1;
            }
            if (acc1.b == NONE) {
                return acc2;
            }
            int cmp = comparator.compare(acc1.b, acc2.b);
            if (cmp > 0) {
                return acc1;
            }
            if (cmp < 0) {
                return acc2;
            }
            acc1.a = downstreamCombiner.apply(acc1.a, acc2.a);
            return acc1;
        };
        Function<PairBox<A, T>, D> finisher = acc -> downstream.finisher().apply(acc.a);
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    public static <T, A, D> Collector<T, ?, D> minAll(Comparator<? super T> comparator, Collector<T, A, D> downstream) {
        return maxAll(comparator.reversed(), downstream);
    }

    public static <T> Collector<T, ?, List<T>> minAll(Comparator<? super T> comparator) {
        return maxAll(comparator.reversed(), Collectors.toList());
    }
}
