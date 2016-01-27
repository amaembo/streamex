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
import java.util.function.Function;
import java.util.stream.BaseStream;

/**
 * @author Tagir Valeev
 */
/* package */abstract class BaseStreamEx<T, S extends BaseStream<T, S>, SPLTR extends Spliterator<T>, B extends BaseStreamEx<T, S, SPLTR, B>>
        implements BaseStream<T, S> {
    static final String CONSUMED_MESSAGE = "Stream is already consumed";

    private S stream;
    SPLTR spliterator;
    StreamContext context;

    BaseStreamEx(S stream, StreamContext context) {
        this.stream = stream;
        this.context = context;
    }

    BaseStreamEx(SPLTR spliterator, StreamContext context) {
        this.spliterator = spliterator;
        this.context = context;
    }

    abstract S createStream();

    final S stream() {
        if (stream != null)
            return stream;
        if (spliterator == null)
            throw new IllegalStateException(CONSUMED_MESSAGE);
        stream = createStream();
        spliterator = null;
        return stream;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SPLTR spliterator() {
        if (stream != null)
            return (SPLTR) stream.spliterator();
        if (spliterator != null) {
            SPLTR s = spliterator;
            spliterator = null;
            return s;
        }
        throw new IllegalStateException(CONSUMED_MESSAGE);
    }

    @Override
    public boolean isParallel() {
        return context.parallel;
    }

    @SuppressWarnings("unchecked")
    @Override
    public S sequential() {
        context = context.sequential();
        if (stream != null)
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
        context = context.parallel();
        if (stream != null)
            stream = stream.parallel();
        return (S) this;
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
        context = context.parallel(fjp);
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
        context = context.onClose(closeHandler);
        return (S) this;
    }

    @Override
    public void close() {
        context.close();
    }

    /**
     * Applies the supplied function to this stream and returns the result of
     * the function.
     * 
     * <p>
     * This method can be used to add more functionality in the fluent style.
     * For example, consider user-defined static method
     * {@code batches(stream, n)} which breaks the stream into batches of given
     * length. Normally you would write
     * {@code batches(StreamEx.of(input).map(...), 10).filter(...)}. Using the
     * {@code chain()} method you can write in more fluent manner:
     * {@code StreamEx.of(input).map(...).chain(s -> batches(s, 10)).filter(...)}.
     * 
     * <p>
     * You could even go further and define a method which returns a function
     * like {@code <T> UnaryOperator<StreamEx<T>> batches(int n)} and use it
     * like this:
     * {@code StreamEx.of(input).map(...).chain(batches(10)).filter(...)}.
     * 
     * @param <U> the type of the function result.
     * @param mapper function to invoke.
     * @return the result of the function invocation.
     * @since 0.5.4
     */
    abstract public <U> U chain(Function<? super B, U> mapper);
}
