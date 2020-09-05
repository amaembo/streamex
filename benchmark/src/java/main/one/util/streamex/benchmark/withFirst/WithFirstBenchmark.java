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

package one.util.streamex.benchmark.withFirst;

import one.util.streamex.IntStreamEx;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
public class WithFirstBenchmark {
    @Param({"100000"})
    private int N;

    @Benchmark
    public Object[] parallelOld() {
        return IntStreamEx.range(N).boxed().parallel().withFirstOld().toArray();
    }

    @Benchmark
    public boolean parallelOldShortCircuit() {
        return IntStreamEx.range(N).boxed().parallel().withFirstOld().anyMatch(x -> x.getKey() == -1);
    }

    @Benchmark
    public Object[] parallelNew() {
        return IntStreamEx.range(N).boxed().parallel().withFirst().toArray();
    }

    @Benchmark
    public boolean parallelNewShortCircuit() {
        return IntStreamEx.range(N).boxed().parallel().withFirst().anyMatch(x -> x.getKey() == -1);
    }

    @Benchmark
    public Object[] sequentialOld() {
        return IntStreamEx.range(N).boxed().withFirstOld().toArray();
    }
    
    @Benchmark
    public boolean sequentialOldShortCircuit() {
        return IntStreamEx.range(N).boxed().withFirstOld().anyMatch(x -> x.getKey() == -1);
    }

    @Benchmark
    public Object[] sequentialNew() {
        return IntStreamEx.range(N).boxed().withFirst().toArray();
    }

    @Benchmark
    public boolean sequentialNewShortCircuit() {
        return IntStreamEx.range(N).boxed().withFirst().anyMatch(x -> x.getKey() == -1);
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(WithFirstBenchmark.class.getSimpleName())
                .build();
        
        new Runner(opt).run();
    }
}
