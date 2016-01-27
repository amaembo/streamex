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

import static one.util.streamex.StreamExInternals.*;

/**
 * This class controls stream execution mode (parallel/sequential), custom FJP
 * and close handlers.
 * 
 * Fields are package-private and mutable, but it's forbidden to change them
 * from outside of this class.
 * 
 * For performance reasons shared objects SEQUENTIAL and PARALLEL are used: then
 * have no custom FJP and no close handler. If custom FJP or close handler is
 * requested for shared object, a new object is created, otherwise the current
 * one is modified.
 * 
 * @author Tagir Valeev
 */
/* package */class StreamContext {
    static final StreamContext SEQUENTIAL = new StreamContext(false);
    static final StreamContext PARALLEL = new StreamContext(true);

    boolean parallel;
    ForkJoinPool fjp;
    Runnable closeHandler;

    private StreamContext(boolean parallel) {
        this.parallel = parallel;
    }

    <T> T terminate(Supplier<T> terminalOperation) {
        return fjp.submit(terminalOperation::get).join();
    }

    <T, U> T terminate(U value, Function<U, T> terminalOperation) {
        return fjp.submit(() -> terminalOperation.apply(value)).join();
    }

    StreamContext parallel() {
        if (this == SEQUENTIAL)
            return PARALLEL;
        this.parallel = true;
        this.fjp = null;
        return this;
    }

    StreamContext sequential() {
        if (this == PARALLEL)
            return SEQUENTIAL;
        this.parallel = false;
        this.fjp = null;
        return this;
    }

    StreamContext parallel(ForkJoinPool fjp) {
        StreamContext context = detach();
        context.parallel = true;
        context.fjp = fjp;
        return context;
    }

    StreamContext detach() {
        if (this == PARALLEL || this == SEQUENTIAL)
            return new StreamContext(parallel);
        return this;
    }

    StreamContext onClose(Runnable r) {
        StreamContext context = detach();
        if (context.closeHandler == null)
            context.closeHandler = r;
        else
            context.closeHandler = compose(context.closeHandler, r);
        return context;
    }

    void close() {
        if (closeHandler != null) {
            Runnable r = closeHandler;
            closeHandler = null;
            r.run();
        }
    }

    private static Runnable compose(Runnable r1, Runnable r2) {
        return () -> {
            try {
                r1.run();
            } catch (Throwable t1) {
                try {
                    r2.run();
                } catch (Throwable t2) {
                    t1.addSuppressed(t2);
                }
                throw t1;
            }
            r2.run();
        };
    }

    StreamContext combine(BaseStream<?, ?> other) {
        if (other == null)
            return this;
        StreamContext otherStrategy = of(other);
        StreamContext result = this;
        if (other.isParallel() && !parallel)
            result = parallel();
        if (otherStrategy.closeHandler != null)
            result = result.onClose(otherStrategy.closeHandler);
        return result;
    }

    static StreamContext of(BaseStream<?, ?> stream) {
        if (stream instanceof BaseStreamEx)
            return ((BaseStreamEx<?, ?, ?, ?>) stream).context;
        if (mustCloseStream(stream))
            return new StreamContext(stream.isParallel()).onClose(stream::close);
        return stream.isParallel() ? PARALLEL : SEQUENTIAL;
    }
}
