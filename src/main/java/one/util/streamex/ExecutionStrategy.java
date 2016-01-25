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

import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.BaseStream;

/**
 * @author Tagir Valeev
 */
/*package*/class ExecutionStrategy {
    static final ExecutionStrategy SEQUENTIAL = new ExecutionStrategy(false);
    static final ExecutionStrategy PARALLEL = new ExecutionStrategy(true);
    
    private final boolean parallel;
    private final ForkJoinPool fjp;

    private ExecutionStrategy(boolean parallel) {
        this.parallel = parallel;
        this.fjp = null;
    }
    
    ExecutionStrategy(ForkJoinPool fjp) {
        this.parallel = true;
        this.fjp = fjp;
    }

    public boolean isParallel() {
        return parallel;
    }

    public ForkJoinPool getFjp() {
        return fjp;
    }

    public <T> T terminate(Supplier<T> terminalOperation) {
        return fjp.submit(terminalOperation::get).join();
    }

    public <T, U> T terminate(U value, Function<U, T> terminalOperation) {
        return fjp.submit(() -> terminalOperation.apply(value)).join();
    }

    public ExecutionStrategy combine(BaseStream<?,?> other) {
        if(other.isParallel() && !isParallel())
            return PARALLEL;
        return this;
    }
    
    public static ExecutionStrategy of(BaseStream<?,?> stream) {
        if(stream instanceof BaseStreamEx)
            return ((BaseStreamEx<?,?,?>)stream).strategy;
        return stream.isParallel() ? PARALLEL : SEQUENTIAL;
    }
}
