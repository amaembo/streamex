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
        class Container {
            A1 a1;
            A2 a2;

            Container(A1 a1, A2 a2) {
                this.a1 = a1;
                this.a2 = a2;
            }
        }
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

        Supplier<Container> supplier = () -> new Container(c1Supplier.get(), c2Supplier.get());
        BiConsumer<Container, T> accumulator = (acc, v) -> {
            c1Accumulator.accept(acc.a1, v);
            c2Accumulator.accept(acc.a2, v);
        };
        BinaryOperator<Container> combiner = (acc1, acc2) -> {
            acc1.a1 = c1Combiner.apply(acc1.a1, acc2.a1);
            acc1.a2 = c2combiner.apply(acc1.a2, acc2.a2);
            return acc1;
        };
        return Collector.of(supplier, accumulator, combiner, acc -> {
            R1 r1 = c1.finisher().apply(acc.a1);
            R2 r2 = c2.finisher().apply(acc.a2);
            return finisher.apply(r1, r2);
        }, c.toArray(new Characteristics[c.size()]));
    }

    public static <T> Collector<T, ?, List<T>> maxAll(Comparator<? super T> comparator) {
        return maxAll(comparator, Collectors.toList());
    }

    public static <T, A, D> Collector<T, ?, D> maxAll(Comparator<? super T> comparator, Collector<? super T, A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BinaryOperator<A> downstreamCombiner = downstream.combiner();
        class Container {
            A acc;
            T obj;
            boolean hasAny;
            
            Container(A acc) {
                this.acc = acc;
            }
        }
        Supplier<Container> supplier = () -> new Container(downstreamSupplier.get());
        BiConsumer<Container, T> accumulator = (acc, t) -> {
            if(!acc.hasAny) {
                downstreamAccumulator.accept(acc.acc, t);
                acc.obj = t;
                acc.hasAny = true;
            } else {
                int cmp = comparator.compare(t, acc.obj);
                if (cmp > 0) {
                    acc.acc = downstreamSupplier.get();
                    acc.obj = t;
                }
                if (cmp >= 0)
                    downstreamAccumulator.accept(acc.acc, t);
            }
        };
        BinaryOperator<Container> combiner = (acc1, acc2) -> {
            if (!acc2.hasAny) {
                return acc1;
            }
            if (!acc1.hasAny) {
                return acc2;
            }
            int cmp = comparator.compare(acc1.obj, acc2.obj);
            if (cmp > 0) {
                return acc1;
            }
            if (cmp < 0) {
                return acc2;
            }
            acc1.acc = downstreamCombiner.apply(acc1.acc, acc2.acc);
            return acc1;
        };
        Function<Container, D> finisher = acc -> downstream.finisher().apply(acc.acc);
        return Collector.of(supplier, accumulator, combiner, finisher);
    }
    
    public static <T, A, D> Collector<T, ?, D> minAll(Comparator<? super T> comparator, Collector<T, A, D> downstream) {
        return maxAll(comparator.reversed(), downstream);
    }

    public static <T> Collector<T, ?, List<T>> minAll(Comparator<? super T> comparator) {
        return maxAll(comparator.reversed(), Collectors.toList());
    }
}
