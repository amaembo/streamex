/*
 * Copyright 2015, 2024 StreamEx contributors
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

package one.util.streamex.benchmark.prefix;

import one.util.streamex.LongStreamEx;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class PrefixLongStreamBenchmark {
    @Param({"100000"})
    private int N;
    
    @Benchmark
    public long[] parallelOrdered() {
        return LongStreamEx.range(N).parallel().prefix(Long::sum).toArray();
    }
    
    @Benchmark
    public boolean parallelOrderedShortCircuit() {
        return LongStreamEx.range(N).parallel().prefix(Long::sum).anyMatch(x -> x == -1);
    }
    
    @Benchmark
    public long[] parallelUnordered() {
        return LongStreamEx.range(N).unordered().parallel().prefix(Long::sum).toArray();
    }
    
    @Benchmark
    public boolean parallelUnorderedShortCircuit() {
        return LongStreamEx.range(N).unordered().parallel().prefix(Long::sum).anyMatch(x -> x == -1);
    }
    
    @Benchmark
    public long[] sequentialOrdered() {
        return LongStreamEx.range(N).prefix(Long::sum).toArray();
    }
    
    @Benchmark
    public boolean sequentialOrderedShortCircuit() {
        return LongStreamEx.range(N).prefix(Long::sum).anyMatch(x -> x == -1);
    }
    
    @Benchmark
    public long[] sequentialUnordered() {
        return LongStreamEx.range(N).unordered().prefix(Long::sum).toArray();
    }
    
    @Benchmark
    public boolean sequentialUnorderedShortCircuit() {
        return LongStreamEx.range(N).unordered().prefix(Long::sum).anyMatch(x -> x == -1);
    }
}
