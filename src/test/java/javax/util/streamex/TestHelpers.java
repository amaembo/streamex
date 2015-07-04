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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Spliterator;
import java.util.Map.Entry;
import java.util.function.Function;
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
    
    static <T> void checkSpliterator(String msg, Supplier<Spliterator<T>> supplier) {
        List<T> expected = new ArrayList<>();
        supplier.get().forEachRemaining(expected::add);
        checkSpliterator(msg, expected, supplier);
    }

    /*
     * Tests whether spliterators produced by given supplier
     * produce the expected result under various splittings
     * 
     * This test is single-threaded and its results are stable
     */
    static <T> void checkSpliterator(String msg, List<T> expected, Supplier<Spliterator<T>> supplier) {
        List<T> seq = new ArrayList<>();
        Spliterator<T> sequential = supplier.get();
        sequential.forEachRemaining(seq::add);
        assertFalse(msg, sequential.tryAdvance(t -> fail(msg+": Advance called with " + t)));
        sequential.forEachRemaining(t -> fail(msg+": Advance called with " + t));
        assertEquals(msg, expected, seq);
        Random r = new Random(1);
        for (int n = 1; n < 500; n++) {
            Spliterator<T> spliterator = supplier.get();
            List<Spliterator<T>> spliterators = new ArrayList<>();
            spliterators.add(spliterator);
            int p = r.nextInt(10) + 2;
            for (int i = 0; i < p; i++) {
                int idx = r.nextInt(spliterators.size());
                Spliterator<T> split = spliterators.get(idx).trySplit();
                if (split != null)
                    spliterators.add(idx, split);
            }
            List<Integer> order = IntStreamEx.ofIndices(spliterators).boxed().toList();
            Collections.shuffle(order, r);
            List<T> list = StreamEx.of(order).mapToEntry(idx -> {
                Spliterator<T> s = spliterators.get(idx);
                Stream.Builder<T> builder = Stream.builder();
                s.forEachRemaining(builder);
                assertFalse(msg, s.tryAdvance(t -> fail(msg+": Advance called with " + t)));
                s.forEachRemaining(t -> fail(msg+": Advance called with " + t));
                return builder.build();
            }).sortedBy(Entry::getKey).values().flatMap(Function.identity()).toList();
            assertEquals(msg+":#" + n, expected, list);
        }
        for (int n = 1; n < 500; n++) {
            Spliterator<T> spliterator = supplier.get();
            List<Spliterator<T>> spliterators = new ArrayList<>();
            spliterators.add(spliterator);
            int p = r.nextInt(30) + 2;
            for (int i = 0; i < p; i++) {
                int idx = r.nextInt(spliterators.size());
                Spliterator<T> split = spliterators.get(idx).trySplit();
                if (split != null)
                    spliterators.add(idx, split);
            }
            List<List<T>> results = StreamEx.<List<T>> generate(() -> new ArrayList<>()).limit(spliterators.size())
                    .toList();
            int count = spliterators.size();
            while (count > 0) {
                int i;
                do {
                    i = r.nextInt(spliterators.size());
                    spliterator = spliterators.get(i);
                } while (spliterator == null);
                if (!spliterator.tryAdvance(results.get(i)::add)) {
                    spliterators.set(i, null);
                    count--;
                }
            }
            List<T> list = StreamEx.of(results).flatMap(List::stream).toList();
            assertEquals(msg+":#" + n, expected, list);
        }
    }
}
