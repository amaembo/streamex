package javax.util.streamex;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;

/* package */ interface PrimitiveCollector<T, A, R> extends Collector<T, A, R> {
    /**
     * A function which merges the second container into the first container.
     * 
     * @return a function which merges the second container into the first
     *         container.
     */
    BiConsumer<A, A> merger();

    /**
     * A function that accepts two partial results and combines them returning
     * either existing partial result or new one.
     * 
     * The default implementation calls the {@link #merger()} and returns the
     * first partial result.
     *
     * @return a function which combines two partial results into a combined
     *         result
     */
    @Override
    default BinaryOperator<A> combiner() {
        BiConsumer<A, A> merger = merger();
        return (a, b) -> {
            merger.accept(a, b);
            return a;
        };
    }
}
