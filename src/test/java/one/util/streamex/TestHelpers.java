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

import org.junit.ComparisonFailure;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static one.util.streamex.StreamExInternals.TailSpliterator;
import static one.util.streamex.StreamExInternals.finished;
import static org.junit.Assert.*;

/**
 * @author Tagir Valeev
 */
public class TestHelpers {
    enum Mode {
        NORMAL, SPLITERATOR, PARALLEL, APPEND, PREPEND, RANDOM
    }

    static class StreamSupplier<T> {
        final Mode mode;
        final Supplier<Stream<T>> base;

        public StreamSupplier(Supplier<Stream<T>> base, Mode mode) {
            this.base = base;
            this.mode = mode;
        }

        public Stream<T> get() {
            Stream<T> res = base.get();
            switch (mode) {
            case NORMAL:
            case SPLITERATOR: 
                return res.sequential();
            case PARALLEL:
                return res.parallel();
            case APPEND:
                // using Stream.empty() or Arrays.asList() here is optimized out
                // in append/prepend which is undesired
                return StreamEx.of(res.parallel()).append(new ConcurrentLinkedQueue<>());
            case PREPEND:
                return StreamEx.of(res.parallel()).prepend(new ConcurrentLinkedQueue<>());
            case RANDOM:
                return StreamEx.of(new EmptyingSpliterator<>(res.parallel().spliterator())).parallel();
            default:
                throw new InternalError("Unsupported mode: " + mode);
            }
        }

        @Override
        public String toString() {
            return mode.toString();
        }
    }

    static class StreamExSupplier<T> extends StreamSupplier<T> {

        public StreamExSupplier(Supplier<Stream<T>> base, Mode mode) {
            super(base, mode);
        }

        @Override
        public StreamEx<T> get() {
            if(mode == Mode.SPLITERATOR)
                return StreamEx.of(base.get().spliterator());
            return StreamEx.of(super.get());
        }
    }

    static class EntryStreamSupplier<K, V> extends StreamSupplier<Map.Entry<K, V>> {

        public EntryStreamSupplier(Supplier<Stream<Map.Entry<K, V>>> base, Mode mode) {
            super(base, mode);
        }

        @Override
        public EntryStream<K, V> get() {
            return EntryStream.of(super.get());
        }
    }

    static <T> List<StreamExSupplier<T>> streamEx(Supplier<Stream<T>> base) {
        return StreamEx.of(Mode.values()).map(mode -> new StreamExSupplier<>(base, mode)).toList();
    }

    /**
     * Run the consumer once feeding it with RNG initialized with auto-generated seed
     * adding the seed value to every failed assertion message
     * 
     * @param cons consumer to run
     */
    static void withRandom(Consumer<Random> cons) {
        long seed = ThreadLocalRandom.current().nextLong();
        withRandom(seed, cons);
    }

    /**
     * Run the consumer once feeding it with RNG initialized with given seed
     * adding the seed value to every failed assertion message
     * 
     * @param seed random seed to use
     * @param cons consumer to run
     */
    static void withRandom(long seed, Consumer<Random> cons) {
        Random random = new Random(seed);
        withMessage("Using new Random("+seed+")", () -> cons.accept(random));
    }
    
    /**
     * Run the runnable automatically adding given message to every failed assertion
     * 
     * @param message message to prepend
     * @param r runnable to run
     */
    private static void withMessage(String message, Runnable r) {
        try {
            r.run();
        } catch (ComparisonFailure cmp) {
            ComparisonFailure ex = new ComparisonFailure(message + ": " + cmp.getMessage(), cmp.getExpected(), cmp
                    .getActual());
            ex.setStackTrace(cmp.getStackTrace());
            throw ex;
        } catch (AssertionError err) {
            AssertionError ex = new AssertionError(message + ": " + err.getMessage(), err.getCause());
            ex.setStackTrace(err.getStackTrace());
            throw ex;
        } catch (RuntimeException | Error err) {
            throw new RuntimeException(message + ": " + err.getMessage(), err);
        }
    }

    static void repeat(int times, IntConsumer consumer) {
        for (int i = 1; i <= times; i++) {
            int finalI = i;
            withMessage("#" + i, () -> consumer.accept(finalI));
        }
    }

    static <T> void streamEx(Supplier<Stream<T>> base, Consumer<StreamExSupplier<T>> consumer) {
        for (StreamExSupplier<T> supplier : StreamEx.of(Mode.values()).map(mode -> new StreamExSupplier<>(base, mode))) {
            withMessage(supplier.toString(), () -> consumer.accept(supplier));
        }
    }

    static <T> void emptyStreamEx(Class<T> clazz, Consumer<StreamExSupplier<T>> consumer) {
        streamEx(Stream::<T>empty, consumer);
    }

    static <K, V> void entryStream(Supplier<Stream<Map.Entry<K, V>>> base, Consumer<EntryStreamSupplier<K, V>> consumer) {
        for (EntryStreamSupplier<K, V> supplier : StreamEx.of(Mode.values()).map(
            mode -> new EntryStreamSupplier<>(base, mode))) {
            withMessage(supplier.toString(), () -> consumer.accept(supplier));
        }
    }

    /**
     * Spliterator which randomly inserts empty spliterators on splitting
     * 
     * @author Tagir Valeev
     *
     * @param <T> type of the elements
     */
    private static class EmptyingSpliterator<T> implements Spliterator<T> {
        private Spliterator<T> source;

        public EmptyingSpliterator(Spliterator<T> source) {
            this.source = Objects.requireNonNull(source);
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            return source.tryAdvance(action);
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            source.forEachRemaining(action);
        }

        @Override
        public Comparator<? super T> getComparator() {
            return source.getComparator();
        }

        @Override
        public Spliterator<T> trySplit() {
            Spliterator<T> source = this.source;
            switch (ThreadLocalRandom.current().nextInt(3)) {
            case 0:
                return Spliterators.emptySpliterator();
            case 1:
                this.source = Spliterators.emptySpliterator();
                return source;
            default:
                Spliterator<T> split = source.trySplit();
                return split == null ? null : new EmptyingSpliterator<>(split);
            }
        }

        @Override
        public long estimateSize() {
            return source.estimateSize();
        }

        @Override
        public int characteristics() {
            return source.characteristics();
        }
    }

    static <T, R> void checkCollectorEmpty(String message, R expected, Collector<T, ?, R> collector) {
        if (finished(collector) != null)
            checkShortCircuitCollector(message, expected, 0, Stream::empty, collector);
        else
            checkCollector(message, expected, Stream::empty, collector);
    }

    static <T, TT extends T, R> void checkShortCircuitCollector(String message, R expected,
            int expectedConsumedElements, Supplier<Stream<TT>> base, Collector<T, ?, R> collector) {
        checkShortCircuitCollector(message, expected, expectedConsumedElements, base, collector, false);
    }

    static <T, TT extends T, R> void checkShortCircuitCollector(String message, R expected,
            int expectedConsumedElements, Supplier<Stream<TT>> base, Collector<T, ?, R> collector, boolean skipIdentity) {
        assertNotNull(message, finished(collector));
        Collector<T, ?, R> withIdentity = Collectors.collectingAndThen(collector, Function.identity());
        for (StreamExSupplier<TT> supplier : streamEx(base)) {
            AtomicInteger counter = new AtomicInteger();
            assertEquals(message + ": " + supplier, expected, supplier.get().peek(t -> counter.incrementAndGet())
                    .collect(collector));
            if (!supplier.get().isParallel())
                assertEquals(message + ": " + supplier + ": consumed: ", expectedConsumedElements, counter.get());
            if (!skipIdentity)
                assertEquals(message + ": " + supplier, expected, supplier.get().collect(withIdentity));
        }
    }

    static <T, TT extends T, R> void checkCollector(String message, R expected, Supplier<Stream<TT>> base,
            Collector<T, ?, R> collector) {
        // use checkShortCircuitCollector for CancellableCollector
        assertNull(message, finished(collector));
        for (StreamExSupplier<TT> supplier : streamEx(base)) {
            assertEquals(message + ": " + supplier, expected, supplier.get().collect(collector));
        }
    }

    static <T> void checkSpliterator(String msg, Supplier<Spliterator<T>> supplier) {
        List<T> expected = new ArrayList<>();
        supplier.get().forEachRemaining(expected::add);
        checkSpliterator(msg, expected, supplier);
    }

    /*
     * Tests whether spliterators produced by given supplier produce the
     * expected result under various splittings
     * 
     * This test is single-threaded. Its behavior is randomized, but random seed
     * will be printed in case of failure, so the results could be reproduced
     */
    static <T> void checkSpliterator(String msg, List<T> expected, Supplier<Spliterator<T>> supplier) {
        List<T> seq = new ArrayList<>();
        // Test forEachRemaining
        Spliterator<T> sequential = supplier.get();
        sequential.forEachRemaining(seq::add);
        assertFalse(msg, sequential.tryAdvance(t -> fail(msg + ": Advance called with " + t)));
        sequential.forEachRemaining(t -> fail(msg + ": Advance called with " + t));
        assertEquals(msg, expected, seq);

        // Test tryAdvance
        seq.clear();
        sequential = supplier.get();
        while (true) {
            AtomicBoolean called = new AtomicBoolean();
            boolean res = sequential.tryAdvance(t -> {
                seq.add(t);
                called.set(true);
            });
            if (res != called.get()) {
                fail(msg
                    + (res ? ": Consumer not called, but spliterator returned true"
                            : ": Consumer called, but spliterator returned false"));
            }
            if (!res)
                break;
        }
        assertFalse(msg, sequential.tryAdvance(t -> fail(msg + ": Advance called with " + t)));
        assertEquals(msg, expected, seq);
        
        // Test TailSpliterator
        if(sequential instanceof TailSpliterator) {
            seq.clear();
            TailSpliterator.forEachWithTail(supplier.get(), seq::add);
            assertEquals(msg, expected, seq);
            seq.clear();
            sequential = supplier.get();
            while(sequential != null) {
                sequential = TailSpliterator.tryAdvanceWithTail(sequential, seq::add);
            }
        }
        assertEquals(msg, expected, seq);
        
        // Test advance+remaining
        for (int i = 1; i < Math.min(4, expected.size() - 1); i++) {
            seq.clear();
            sequential = supplier.get();
            for(int j=0; j<i; j++) assertTrue(msg, sequential.tryAdvance(seq::add));
            sequential.forEachRemaining(seq::add);
            assertEquals(msg, expected, seq);
        }

        // Test trySplit
        withRandom(r -> {
            repeat(500, n -> {
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
                List<T> list = StreamEx.of(order).mapToEntry(idx -> idx, idx -> {
                    Spliterator<T> s = spliterators.get(idx);
                    Stream.Builder<T> builder = Stream.builder();
                    s.forEachRemaining(builder);
                    assertFalse(msg, s.tryAdvance(t -> fail(msg + ": Advance called with " + t)));
                    s.forEachRemaining(t -> fail(msg + ": Advance called with " + t));
                    return builder.build();
                }).sortedBy(Entry::getKey).values().flatMap(Function.identity()).toList();
                assertEquals(msg, expected, list);
            });
            repeat(500, n -> {
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
                List<List<T>> results = StreamEx.<List<T>> generate(ArrayList::new).limit(spliterators.size())
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
                assertEquals(msg, expected, list);
            });
        });
    }

    static void checkIllegalStateException(Runnable r, String key, String value1, String value2) {
        try {
            r.run();
            fail("no exception");
        } catch (IllegalStateException ex) {
            String exmsg = ex.getMessage();
            if (!exmsg.equals("Duplicate entry for key '" + key + "' (attempt to merge values '" + value1 + "' and '"
                + value2 + "')")
                && !exmsg.equals("Duplicate entry for key '" + key + "' (attempt to merge values '" + value2
                    + "' and '" + value1 + "')")
                && !exmsg.equals("java.lang.IllegalStateException: Duplicate entry for key '" + key
                    + "' (attempt to merge values '" + value1 + "' and '" + value2 + "')")
                && !exmsg.equals("java.lang.IllegalStateException: Duplicate entry for key '" + key
                    + "' (attempt to merge values '" + value2 + "' and '" + value1 + "')"))
                fail("wrong exception message: " + exmsg);
        }
    }
}
