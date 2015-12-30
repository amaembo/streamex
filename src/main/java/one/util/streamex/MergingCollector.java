package one.util.streamex;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;

/**
 * A {@code MergingCollector} is a {@code Collector} with more specific
 * combining algorithm. Instead of providing a combiner which can create new
 * partial result the {@code MergingCollector} must provide a merger which
 * merges the second partial result into the first one.
 * 
 * @author Tagir Valeev
 *
 * @param <T> the type of input elements to the reduction operation
 * @param <A> the mutable accumulation type of the reduction operation (often
 *        hidden as an implementation detail)
 * @param <R> the result type of the reduction operation
 */
/* package */interface MergingCollector<T, A, R> extends Collector<T, A, R> {
    /**
     * A function that merges the second partial result into the first partial
     * result.
     * 
     * @return a function that merges the second partial result into the first
     *         partial result.
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
