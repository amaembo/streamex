/*
 * Copyright 2015, 2023 StreamEx contributors
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
     * <p>
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
