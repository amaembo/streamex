/*
 * Copyright 2015 Tagir Valeev
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
package javax.util.streamex;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.BaseStream;
import java.util.stream.Stream;

/**
 * @author Tagir Valeev
 */
public class TestHelpers {
    static enum Mode {
        NORMAL, APPEND, PREPEND
    }

    static class StreamSupplier<T, S extends BaseStream<T, ? super S>> implements Supplier<S> {
        private final Supplier<S> base;
        private final boolean parallel;

        public StreamSupplier(Supplier<S> base, boolean parallel) {
            this.base = base;
            this.parallel = parallel;
        }

        @SuppressWarnings("unchecked")
        @Override
        public S get() {
            return (S) (parallel ? base.get().parallel() : base.get().sequential());
        }

        @Override
        public String toString() {
            return parallel ? "Parallel" : "Sequential";
        }
    }
    
    static class StreamExSupplier<T> extends StreamSupplier<T, StreamEx<T>> {
        private final Mode mode;

        public StreamExSupplier(Supplier<Stream<T>> base, boolean parallel, Mode mode) {
            super(() -> StreamEx.of(base.get()), parallel);
            this.mode = mode;
        }

        @Override
        public StreamEx<T> get() {
            StreamEx<T> res = super.get();
            switch(mode) {
            case APPEND:
                return res.append(Stream.empty());
            case PREPEND:
                return res.prepend(Stream.empty());
            default:
                return res;
            }
        }

        @Override
        public String toString() {
            return super.toString()+"/"+mode;
        }
    }

    static <T, S extends BaseStream<T, ? super S>> List<StreamSupplier<T, S>> suppliers(Supplier<S> base) {
        return Arrays.asList(new StreamSupplier<>(base, false), new StreamSupplier<>(base, true));
    }

    static <T> List<StreamExSupplier<T>> streamEx(Supplier<Stream<T>> base) {
        return StreamEx.of(Boolean.FALSE, Boolean.TRUE)
                .cross(Mode.values()).mapKeyValue((parallel, mode) -> new StreamExSupplier<>(base, parallel, mode))
                .toList();
    }
    
    static <T> List<StreamExSupplier<T>> emptyStreamEx(Class<T> clazz) {
        return streamEx(() -> Stream.<T>empty());
    }
}
