package javax.util.streamex;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;

interface AbstractPrimitiveCollector<T, A, R> extends Collector<T, A, R> {

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
    default Set<Collector.Characteristics> characteristics() {
        return EnumSet.noneOf(Collector.Characteristics.class);
    }
}
