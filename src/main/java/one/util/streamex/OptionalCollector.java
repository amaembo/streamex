/*
 * Copyright 2015, 2016 Tagir Valeev
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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author Tagir Valeev
 */
public interface OptionalCollector<T, A, R> extends Collector<T, A, Optional<R>> {
    default public <U> OptionalCollector<T, A, U> map(Function<? super R, ? extends U> mapper) {
        return StreamExInternals.collectingAndThen(this, opt -> opt.map(mapper));
    }
    
    default public <U> OptionalCollector<T, A, U> flatMap(Function<? super R, Optional<U>> mapper) {
        return StreamExInternals.collectingAndThen(this, opt -> opt.flatMap(mapper));
    }
    
    default public OptionalCollector<T, A, R> filter(Predicate<? super R> predicate) {
        return StreamExInternals.collectingAndThen(this, opt -> opt.filter(predicate));
    }
    
    default public Collector<T, A, R> get() {
        return MoreCollectors.collectingAndThen(this, Optional::get);
    }
    
    default public Collector<T, A, R> orElse(R value) {
        return MoreCollectors.collectingAndThen(this, opt -> opt.orElse(value));
    }
    
    default public Collector<T, A, R> orElseGet(Supplier<? extends R> supplier) {
        return MoreCollectors.collectingAndThen(this, opt -> opt.orElseGet(supplier));
    }
    
    public static<T, A, R> OptionalCollector<T, A, R> of(Supplier<A> supplier,
            BiConsumer<A, T> accumulator,
            BinaryOperator<A> combiner,
            Function<A, Optional<R>> finisher,
            Characteristics... characteristics) {
        EnumSet<Characteristics> chr = EnumSet.noneOf(Characteristics.class);
        chr.addAll(Arrays.asList(characteristics));
        chr.remove(Characteristics.IDENTITY_FINISH);
        return new OptionalCollector<T, A, R>() {
            @Override
            public Supplier<A> supplier() {
                return supplier;
            }

            @Override
            public BiConsumer<A, T> accumulator() {
                return accumulator;
            }

            @Override
            public BinaryOperator<A> combiner() {
                return combiner;
            }

            @Override
            public Function<A, Optional<R>> finisher() {
                return finisher;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return chr;
            }
        };
    }
}
