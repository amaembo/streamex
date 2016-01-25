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

import java.util.Spliterator;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.BaseStream;

/**
 * @author Tagir Valeev
 */
/* package */abstract class BaseStreamEx<T, S extends BaseStream<T, S>, SPLTR extends Spliterator<T>>
        implements BaseStream<T, S> {
    static final String CONSUMED_MESSAGE = "Stream is already consumed";

    private S stream;
    SPLTR spliterator;
    ExecutionStrategy strategy;
    
    BaseStreamEx(S stream, ExecutionStrategy strategy) {
        this.stream = stream;
        this.strategy = strategy;
    }

    BaseStreamEx(SPLTR spliterator, ExecutionStrategy strategy) {
        this.spliterator = spliterator;
        this.strategy = strategy;
    }
    
    abstract S createStream();

    S stream() {
        if(stream != null)
            return stream;
        if(spliterator == null)
            throw new IllegalStateException(CONSUMED_MESSAGE);
        stream = createStream();
        spliterator = null;
        return stream;
    }
    
    <B extends BaseStream<?, ?>> B forwardClose(B proxy) {
        return StreamExInternals.delegateClose(proxy, stream);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SPLTR spliterator() {
        if(stream != null)
            return (SPLTR) stream.spliterator();
        if(spliterator != null) {
            SPLTR s = spliterator;
            spliterator = null;
            return s;
        }
        throw new IllegalStateException(CONSUMED_MESSAGE);
    }

    @Override
    public boolean isParallel() {
        return strategy.isParallel();
    }

    @SuppressWarnings("unchecked")
    @Override
    public S sequential() {
        strategy = ExecutionStrategy.SEQUENTIAL;
        if(stream != null)
            stream = stream.sequential();
        return (S) this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * If this stream was created using {@link #parallel(ForkJoinPool)}, the new
     * stream forgets about supplied custom {@link ForkJoinPool} and its
     * terminal operation will be executed in common pool.
     */
    @SuppressWarnings("unchecked")
    @Override
    public S parallel() {
        strategy = ExecutionStrategy.PARALLEL;
        if(stream != null)
            stream = stream.parallel();
        return (S)this;
    }

    /**
     * Returns an equivalent stream that is parallel and bound to the supplied
     * {@link ForkJoinPool}.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     * 
     * <p>
     * The terminal operation of this stream or any derived stream (except the
     * streams created via {@link #parallel()} or {@link #sequential()} methods)
     * will be executed inside the supplied {@code ForkJoinPool}. If current
     * thread does not belong to that pool, it will wait till calculation
     * finishes.
     *
     * @param fjp a {@code ForkJoinPool} to submit the stream operation to.
     * @return a parallel stream bound to the supplied {@code ForkJoinPool}
     * @since 0.2.0
     */
    @SuppressWarnings("unchecked")
    public S parallel(ForkJoinPool fjp) {
        strategy = new ExecutionStrategy(fjp);
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public S unordered() {
        stream = stream().unordered();
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public S onClose(Runnable closeHandler) {
        stream = stream().onClose(closeHandler);
        return (S) this;
    }

    @Override
    public void close() {
        if(stream != null)
            stream.close();
    }

    
}
