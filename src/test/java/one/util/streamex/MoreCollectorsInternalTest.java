/*
 * Copyright 2015, 2020 StreamEx contributors
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import one.util.streamex.Internals.BooleanMap;

import static java.util.Arrays.asList;
import static one.util.streamex.TestHelpers.checkCollector;
import static one.util.streamex.TestHelpers.checkShortCircuitCollector;

/**
 * @author Tagir Valeev
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MoreCollectorsInternalTest {
    @Test
    public void testPartitioningBy() {
        Collector<Integer, ?, Map<Boolean, Optional<Integer>>> by20 = MoreCollectors.partitioningBy(x -> x % 20 == 0,
            MoreCollectors.first());
        Collector<Integer, ?, Map<Boolean, Optional<Integer>>> by200 = MoreCollectors.partitioningBy(x -> x % 200 == 0,
            MoreCollectors.first());
        Supplier<Stream<Integer>> supplier = () -> IntStreamEx.range(1, 100).boxed();
        checkShortCircuitCollector("by20", new BooleanMap<>(Optional.of(20), Optional.of(1)), 20, supplier, by20);
        checkShortCircuitCollector("by200", new BooleanMap<>(Optional.empty(), Optional.of(1)), 99, supplier, by200);
    }

    @Test
    public void testMapping() {
        List<String> input = asList("Capital", "lower", "Foo", "bar");
        Collector<String, ?, Map<Boolean, Optional<Integer>>> collector = MoreCollectors
            .partitioningBy(str -> Character.isUpperCase(str.charAt(0)), MoreCollectors.mapping(String::length,
                MoreCollectors.first()));
        checkShortCircuitCollector("mapping", new BooleanMap<>(Optional.of(7), Optional.of(5)), 2, input::stream,
            collector);

        Collector<String, ?, Map<Boolean, Optional<Integer>>> collectorLast = MoreCollectors.partitioningBy(
            str -> Character.isUpperCase(str.charAt(0)), MoreCollectors.mapping(String::length, MoreCollectors.last()));
        checkCollector("last", new BooleanMap<>(Optional.of(3), Optional.of(3)), input::stream, collectorLast);
    }
}
