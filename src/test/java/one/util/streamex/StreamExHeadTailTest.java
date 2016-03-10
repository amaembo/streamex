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

import static java.util.Arrays.asList;
import static one.util.streamex.TestHelpers.*;
import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;





import org.junit.Test;

/**
 * Tests/examples for StreamEx.headTail() method
 * 
 * @author Tagir Valeev
 */
public class StreamExHeadTailTest {
    // ///////////////////////
    // JDK-8 intermediate ops

    // Stream.skip (TSO)
    static <T> StreamEx<T> skip(StreamEx<T> input, int n) {
        return input.headTail((head, tail) -> n > 0 ? skip(tail, n - 1) : tail.prepend(head));
    }

    // Stream.limit (TSO)
    static <T> StreamEx<T> limit(StreamEx<T> input, int n) {
        return input.headTail((head, tail) -> n > 1 ? limit(tail, n - 1).prepend(head) : Stream.of(head));
    }

    // Stream.filter (TSO)
    static <T> StreamEx<T> filter(StreamEx<T> input, Predicate<T> predicate) {
        return input.<T> headTail((head, tail) -> predicate.test(head) ? filter(tail, predicate).prepend(head) : filter(tail, predicate));
    }

    // Stream.distinct
    static <T> StreamEx<T> distinct(StreamEx<T> input) {
        return input.headTail((head, tail) -> distinct(tail.filter(n -> !Objects.equals(head, n))).prepend(head));
    }

    // Stream.distinct (TSO)
    static <T> StreamEx<T> distinctTSO(StreamEx<T> input) {
        return distinctTSO(input, new HashSet<>());
    }

    private static <T> StreamEx<T> distinctTSO(StreamEx<T> input, Set<T> observed) {
        return input.headTail((head, tail) -> observed.add(head) ? distinctTSO(tail, observed).prepend(head)
                : distinctTSO(tail, observed));
    }

    // Stream.map (TSO)
    static <T, R> StreamEx<R> map(StreamEx<T> input, Function<T, R> mapper) {
        return input.headTail((head, tail) -> map(tail, mapper).prepend(mapper.apply(head)));
    }

    // Stream.peek (TSO)
    static <T> StreamEx<T> peek(StreamEx<T> input, Consumer<T> consumer) {
        return input.headTail((head, tail) -> {
            consumer.accept(head);
            return peek(tail, consumer).prepend(head);
        });
    }
    
    // Stream.flatMap (TSO)
    static <T, R> StreamEx<R> flatMap(StreamEx<T> input, Function<T, Stream<R>> mapper) {
        return input.headTail((head, tail) -> flatMap(tail, mapper).prepend(mapper.apply(head)));
    }

    // Stream.sorted
    static <T> StreamEx<T> sorted(StreamEx<T> input) {
        return sorted(input, new ArrayList<>());
    }

    private static <T> StreamEx<T> sorted(StreamEx<T> input, List<T> cur) {
        return input.headTail((head, tail) -> {
            cur.add(head);
            return sorted(tail, cur);
        }, () -> {
            cur.sort(null);
            return cur.stream();
        });
    }

    // ///////////////////////
    // JDK-9 intermediate ops

    // Stream.takeWhile (TSO)
    static <T> StreamEx<T> takeWhile(StreamEx<T> input, Predicate<T> predicate) {
        return input.headTail((head, tail) -> predicate.test(head) ? takeWhile(tail, predicate).prepend(head) : null);
    }

    // Stream.dropWhile (TSO)
    static <T> StreamEx<T> dropWhile(StreamEx<T> input, Predicate<T> predicate) {
        return input.headTail((head, tail) -> predicate.test(head) ? dropWhile(tail, predicate) : tail.prepend(head));
    }

    // ///////////////////////
    // Other intermediate ops

    // Filters the input stream of natural numbers (2, 3, 4...) leaving only
    // prime numbers (lazy)
    static StreamEx<Integer> sieve(StreamEx<Integer> input) {
        return input.headTail((head, tail) -> sieve(tail.filter(n -> n % head != 0)).prepend(head));
    }

    // Creates a reversed stream
    static <T> StreamEx<T> reverse(StreamEx<T> input) {
        return input.headTail((head, tail) -> reverse(tail).append(head));
    }

    // Creates a reversed stream (TSO)
    static <T> StreamEx<T> reverseTSO(StreamEx<T> input) {
        return reverseTSO(input, new ArrayDeque<>());
    }
    
    private static <T> StreamEx<T> reverseTSO(StreamEx<T> input, Deque<T> buf) {
        return input.headTail((head, tail) -> {
            buf.addFirst(head);
            return reverseTSO(tail, buf);
        }, buf::stream);
    }
    
    // Creates a stream which consists of this stream and this reversed stream
    static <T> StreamEx<T> mirror(StreamEx<T> input) {
        return input.headTail((head, tail) -> mirror(tail).append(head).prepend(head));
    }

    // Creates a reversed stream (TSO)
    static <T> StreamEx<T> mirrorTSO(StreamEx<T> input) {
        return mirrorTSO(input, new ArrayDeque<>());
    }
    
    private static <T> StreamEx<T> mirrorTSO(StreamEx<T> input, Deque<T> buf) {
        return input.headTail((head, tail) -> {
            buf.addFirst(head);
            return mirrorTSO(tail, buf).prepend(head);
        }, buf::stream);
    }
    
    // Creates an infinitely cycled stream
    static <T> StreamEx<T> cycle(StreamEx<T> input) {
        return input.headTail((head, tail) -> cycle(tail.append(head)).prepend(head));
    }

    // Creates a n-times cycled stream (TSO)
    static <T> StreamEx<T> cycleTSO(StreamEx<T> input, int n) {
        return cycleTSO(input, n, new ArrayList<>());
    }
    
    private static <T> StreamEx<T> cycleTSO(StreamEx<T> input, int n, List<T> buf) {
        return input.headTail((head, tail) -> {
            buf.add(head);
            return cycleTSO(tail, n, buf).prepend(head);
        }, () -> IntStreamEx.range(n-1).flatMapToObj(i -> buf.stream()));
    }
    
    // mapFirst (TSO)
    static <T> StreamEx<T> mapFirst(StreamEx<T> input, UnaryOperator<T> operator) {
        return input.headTail((head, tail) -> tail.prepend(operator.apply(head)));
    }

    // Creates lazy scanLeft stream (TSO)
    static <T> StreamEx<T> scanLeft(StreamEx<T> input, BinaryOperator<T> operator) {
        return input.headTail((head, tail) -> scanLeft(tail.mapFirst(cur -> operator.apply(head, cur)), operator)
                .prepend(head));
    }
    
    static <T> UnaryOperator<StreamEx<T>> scanLeft(BinaryOperator<T> operator) {
        return stream -> scanLeft(stream, operator);
    }

    // takeWhileClosed: takeWhile+first element violating the predicate (TSO)
    static <T> StreamEx<T> takeWhileClosed(StreamEx<T> input, Predicate<T> predicate) {
        return input.headTail((head, tail) -> predicate.test(head) ? takeWhileClosed(tail, predicate).prepend(head)
                : Stream.of(head));
    }

    // take every nth stream element (starting from the first) (TSO)
    static <T> StreamEx<T> every(StreamEx<T> input, int n) {
        return input.headTail((head, tail) -> every(skip(tail, n - 1), n).prepend(head));
    }
    
    static <T> StreamEx<T> every3(StreamEx<T> input) {
        return input.headTail(
            (first, tail1) -> tail1.<T>headTail(
                (second, tail2) -> tail2.headTail(
                    (third, tail3) -> every3(tail3))).prepend(first));
    }

    // maps every couple of elements using given mapper (in non-sliding manner)
    static <T, R> StreamEx<R> couples(StreamEx<T> input, BiFunction<T, T, R> mapper) {
        return input.headTail((left, tail1) -> tail1.headTail((right, tail2) -> couples(tail2, mapper).prepend(
            mapper.apply(left, right))));
    }

    // maps every pair of elements using given mapper (in sliding manner)
    static <T, R> StreamEx<R> pairs(StreamEx<T> input, BiFunction<T, T, R> mapper) {
        return input.headTail((left, tail1) -> tail1.headTail((right, tail2) -> pairs(tail2.prepend(right), mapper)
                .prepend(mapper.apply(left, right))));
    }

    // Stream of fixed size batches (TSO)
    static <T> StreamEx<List<T>> batches(StreamEx<T> input, int size) {
        return batches(input, size, Collections.emptyList());
    }

    private static <T> StreamEx<List<T>> batches(StreamEx<T> input, int size, List<T> cur) {
        return input.headTail((head, tail) -> cur.size() >= size 
                ? batches(tail, size, asList(head)).prepend(cur)
                : batches(tail, size, StreamEx.of(cur).append(head).toList()), 
                () -> Stream.of(cur));
    }

    // Stream of single element lists -> stream of sliding windows (TSO)
    static <T> StreamEx<List<T>> sliding(StreamEx<List<T>> input, int size) {
        return input.headTail((head, tail) -> head.size() == size ? sliding(
            tail.mapFirst(next -> StreamEx.of(head.subList(1, size), next).toFlatList(l -> l)), size).prepend(head)
                : sliding(tail.mapFirst(next -> StreamEx.of(head, next).toFlatList(l -> l)), size));
    }

    // Stream of dominators (removes immediately following elements which the
    // current element dominates on) (TSO)
    static <T> StreamEx<T> dominators(StreamEx<T> input, BiPredicate<T, T> isDominator) {
        return input.headTail((head, tail) -> dominators(dropWhile(tail, e -> isDominator.test(head, e)), isDominator)
                .prepend(head));
    }

    // Stream of mappings of (index, element) (TSO)
    static <T, R> StreamEx<R> withIndices(StreamEx<T> input, BiFunction<Integer, T, R> mapper) {
        return withIndices(input, 0, mapper);
    }

    private static <T, R> StreamEx<R> withIndices(StreamEx<T> input, int idx, BiFunction<Integer, T, R> mapper) {
        return input.headTail((head, tail) -> withIndices(tail, idx + 1, mapper).prepend(mapper.apply(idx, head)));
    }

    // Appends the stream of Integer with their sum
    static StreamEx<Integer> appendSum(StreamEx<Integer> input) {
        return reduceLast(input.append(0), Integer::sum);
    }

    private static <T> StreamEx<T> reduceLast(StreamEx<T> input, BinaryOperator<T> op) {
        return input.headTail((head, tail) -> reduceLast(tail, op).prepend(head).mapLast(last -> op.apply(head, last)));
    }
    
    // Append the result of reduction of given stream (TCO)
    static <T> StreamEx<T> appendReduction(StreamEx<T> input, T identity, BinaryOperator<T> op) {
        return input.headTail((head, tail) -> appendReduction(tail, op.apply(identity, head), op).prepend(head),
            () -> Stream.of(identity));
    }
    
    // Returns stream filtered by f1, then concatenated with the original stream filtered by f2
    static <T> StreamEx<T> twoFilters(StreamEx<T> input, Predicate<T> f1, Predicate<T> f2) {
        return twoFilters(input, f1, f2, Stream.builder());
    }

    private static <T> StreamEx<T> twoFilters(StreamEx<T> input, Predicate<T> f1, Predicate<T> f2, Stream.Builder<T> buf) {
        return input.headTail((head, tail) -> {
            StreamEx<T> res = twoFilters(tail, f1, f2, buf);
            if(f2.test(head)) buf.add(head);
            return f1.test(head) ? res.prepend(head) : res;
        }, buf::build);
    }
    
    static <T> StreamEx<T> skipLast(Stream<T> input, int n) {
        return skipLast(StreamEx.of(input), n, new ArrayDeque<>());
    }
    
    private static <T> StreamEx<T> skipLast(StreamEx<T> input, int n, Deque<T> buf) {
        return input.headTail((head, tail) -> {
            buf.addLast(head);
            return buf.size() > n ? skipLast(tail, n, buf).prepend(buf.pollFirst()) : skipLast(tail, n, buf);
        });
    }
    
    static <T> UnaryOperator<StreamEx<T>> limitSorted(Comparator<T> comparator, int n) {
        @SuppressWarnings("unchecked")
        Collector<T, Object, List<T>> collector = (Collector<T, Object, List<T>>) MoreCollectors.least(comparator, n);
        return stream -> collectAndStream(stream, collector.supplier().get(), collector.accumulator(), collector
                .finisher().andThen(StreamEx::of));
    }
    
    private static <T, A, R> StreamEx<R> collectAndStream(StreamEx<T> input, A buf, BiConsumer<A, T> accumulator, Function<A, StreamEx<R>> finisher) {
        return input.headTail((head, tail) -> {
            accumulator.accept(buf, head);
            return collectAndStream(tail, buf, accumulator, finisher);
        }, () -> finisher.apply(buf));
    }
    
    // ///////////////////////
    // Terminal ops

    // Returns either the first stream element matching the predicate or just
    // the first element if nothing matches
    static <T> T firstMatchingOrFirst(StreamEx<T> stream, Predicate<T> predicate) {
        return stream.headTail((head, tail) -> tail.prepend(head).filter(predicate).append(head)).findFirst().get();
    }

    // ///////////////////////
    // Tests

    @Test
    public void testHeadTailRecursive() {
        streamEx(() -> StreamEx.iterate(2, x -> x + 1), s -> assertEquals(asList(2, 3, 5, 7, 11, 13, 17, 19, 23, 29,
            31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97), s.get().chain(str -> sieve(str)).takeWhile(
            x -> x < 100).toList()));

        emptyStreamEx(Integer.class, s -> {
            assertEquals(0L, s.get().headTail((first, head) -> {
                throw new IllegalStateException();
            }).count());
            assertFalse(s.get().headTail((first, head) -> {
                throw new IllegalStateException();
            }).findFirst().isPresent());
        });

        streamEx(() -> IntStreamEx.range(100).boxed(), s -> assertEquals(IntStreamEx.rangeClosed(99, 0, -1).boxed()
                .toList(), reverse(s.get()).toList()));
        streamEx(() -> IntStreamEx.range(100).boxed(), s -> assertEquals(IntStreamEx.rangeClosed(99, 0, -1).boxed()
            .toList(), reverseTSO(s.get()).toList()));

        streamEx(() -> StreamEx.of(1, 2), s -> assertEquals(0, s.get().headTail((head, stream) -> null).count()));

        streamEx(() -> StreamEx.iterate(1, x -> x + 1), s -> assertEquals(asList(1, 3, 6, 10, 15, 21, 28, 36, 45, 55,
            66, 78, 91), s.get().chain(scanLeft(Integer::sum)).takeWhile(x -> x < 100).toList()));

        streamEx(() -> StreamEx.iterate(1, x -> x + 1), s -> assertEquals(IntStreamEx.range(1, 100).boxed().toList(),
            takeWhile(s.get(), x -> x < 100).toList()));

        streamEx(() -> StreamEx.iterate(1, x -> x + 1), s -> assertEquals(IntStreamEx.rangeClosed(1, 100).boxed()
                .toList(), s.get().chain(str -> takeWhileClosed(str, x -> x < 100)).toList()));

        streamEx(() -> IntStreamEx.range(1000).boxed(), s -> assertEquals(IntStreamEx.range(0, 1000, 20).boxed()
                .toList(), every(s.get(), 20).toList()));

        // http://stackoverflow.com/q/34395943/4856258
        int[] input = { 1, 2, 3, -1, 3, -10, 9, 100, 1, 100, 0 };
        AtomicInteger counter = new AtomicInteger();
        assertEquals(5, IntStreamEx.of(input).peek(x -> counter.incrementAndGet()).boxed()
                .chain(scanLeft(Integer::sum)).indexOf(x -> x < 0).getAsLong());
        assertEquals(6, counter.get());

        assertEquals(4, (int) firstMatchingOrFirst(StreamEx.of(1, 2, 3, 4, 5), x -> x > 3));
        assertEquals(1, (int) firstMatchingOrFirst(StreamEx.of(1, 2, 3, 4, 5), x -> x > 5));
        assertEquals(1, (int) firstMatchingOrFirst(StreamEx.of(1, 2, 3, 4, 5), x -> x > 0));

        assertEquals(asList(1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3), cycle(StreamEx.of(1, 2, 3, 4, 5))
                .limit(18).toList());
        assertEquals(asList(1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5), cycleTSO(StreamEx.of(1, 2, 3, 4, 5), 3)
                .toList());
        assertEquals(asList(1, 2, 3, 4, 5, 5, 4, 3, 2, 1), mirror(StreamEx.of(1, 2, 3, 4, 5)).toList());

        assertEquals(asList(9, 13, 17), StreamEx.of(1, 3, 5, 7, 9).headTail(
            (head, tail) -> tail.pairMap((a, b) -> a + b + head)).toList());

        assertEquals("[[0, 1, 2, 3], [4, 5, 6, 7], [8, 9, 10, 11], [12, 13, 14, 15], [16, 17, 18, 19]]", batches(
            IntStreamEx.range(20).boxed(), 4).toList().toString());
        assertEquals("[[0, 1, 2, 3], [4, 5, 6, 7], [8, 9, 10, 11], [12, 13, 14, 15], [16, 17, 18, 19], [20]]", batches(
            IntStreamEx.range(21).boxed(), 4).toList().toString());
        assertEquals(
            "[[0, 1, 2, 3], [1, 2, 3, 4], [2, 3, 4, 5], [3, 4, 5, 6], [4, 5, 6, 7], [5, 6, 7, 8], [6, 7, 8, 9]]",
            sliding(IntStreamEx.range(10).mapToObj(Collections::singletonList), 4).toList().toString());

        assertEquals(IntStreamEx.range(50, 100).boxed().toList(), dropWhile(IntStreamEx.range(100).boxed(),
            i -> i != 50).toList());

        assertEquals(asList(1, 3, 4, 7, 10), dominators(
            StreamEx.of(1, 3, 4, 2, 1, 7, 5, 3, 4, 0, 4, 6, 7, 10, 4, 3, 2, 1), (a, b) -> a >= b).toList());

        assertEquals(asList(1, 3, 7, 5, 10), distinct(
            StreamEx.of(1, 1, 3, 1, 3, 7, 1, 3, 1, 7, 3, 5, 1, 3, 5, 5, 7, 7, 7, 10, 5, 3, 7, 1)).toList());

        assertEquals(asList("key1=1", "key2=2", "key3=3"), couples(StreamEx.of("key1", 1, "key2", 2, "key3", 3),
            (k, v) -> k + "=" + v).toList());

        assertEquals(asList("key1=1", "1=key2", "key2=2", "2=key3", "key3=3"), pairs(
            StreamEx.of("key1", 1, "key2", 2, "key3", 3), (k, v) -> k + "=" + v).toList());

        assertEquals(asList("1. Foo", "2. Bar", "3. Baz"), withIndices(StreamEx.of("Foo", "Bar", "Baz"),
            (idx, e) -> (idx + 1) + ". " + e).toList());
        
        assertEquals(asList(1,2,3,4,10), appendSum(StreamEx.of(1,2,3,4)).toList());
        assertFalse(appendSum(StreamEx.of(1,2,3,4)).has(11));
        
        assertEquals(asList(0, 3, 6, 9, 12, 15, 18, 0, 4, 8, 12, 16), twoFilters(IntStreamEx.range(20).boxed(),
            x -> x % 3 == 0, x -> x % 4 == 0).toList());
        
        assertEquals(asList(5, 10, 1, 6, 7), skipLast(Stream.of(5, 10, 1, 6, 7, 15, -1, 10), 3).toList());
        assertEquals(asList(0, 3, 6, 9, 12, 15, 18), every3(IntStreamEx.range(20).boxed()).toList());
        
        assertEquals(asList(0, 1, 2, 3, 3), StreamEx.of(0, 1, 4, 2, 10, 3, 5, 10, 3, 15).chain(
            limitSorted(Comparator.<Integer> naturalOrder(), 5)).toList());
    }

    @Test
    public void testHeadTailTCO() {
        assertTrue(couples(IntStreamEx.range(20000).boxed(), (a, b) -> b - a).allMatch(x -> x == 1));
        assertTrue(pairs(IntStreamEx.range(20000).boxed(), (a, b) -> b - a).allMatch(x -> x == 1));
        // 20001+20002+...+40000
        assertEquals(600010000, limit(skip(StreamEx.iterate(1, x -> x + 1), 20000), 20000).mapToInt(Integer::intValue)
                .sum());
        // 1+3+5+...+39999
        assertEquals(400000000, limit(every(StreamEx.iterate(1, x -> x + 1), 2), 20000).mapToInt(Integer::intValue)
                .sum());
        assertEquals(400000000, limit(filter(StreamEx.iterate(1, x -> x + 1), x -> x % 2 != 0), 20000).mapToInt(
            Integer::intValue).sum());
        // 1+2+...+10000
        assertEquals(50005000, (int) limit(scanLeft(StreamEx.iterate(1, x -> x + 1), Integer::sum), 10000).reduce(
            (a, b) -> b).get());
        assertEquals(50005000, (int) limit(flatMap(StreamEx.iterate(1, x -> x + 3), (Integer x) -> StreamEx.of(x, x + 1, x + 2)),
            10000).reduce(Integer::sum).get());
        assertEquals(asList(50005000), skip(
            appendReduction(IntStreamEx.rangeClosed(1, 10000).boxed(), 0, Integer::sum), 10000).toList());
        AtomicInteger sum = new AtomicInteger();
        assertEquals(10000, peek(IntStreamEx.rangeClosed(1, 10000).boxed(), sum::addAndGet).count());
        assertEquals(50005000, sum.get());
        

        assertEquals(400020000, (int) limit(map(StreamEx.iterate(1, x -> x + 1), x -> x * 2), 20000).reduce(
            Integer::sum).get());

        assertEquals(19999, takeWhile(StreamEx.iterate(1, x -> x + 1), x -> x < 20000).count());
        assertTrue(takeWhile(StreamEx.iterate(1, x -> x + 1), x -> x < 20000).has(19999));
        assertEquals(20000, takeWhileClosed(StreamEx.iterate(1, x -> x + 1), x -> x < 20000).count());
        assertTrue(takeWhileClosed(StreamEx.iterate(1, x -> x + 1), x -> x < 20000).has(20000));
        assertEquals(IntStreamEx.range(20000, 40000).boxed().toList(), dropWhile(IntStreamEx.range(40000).boxed(),
            i -> i != 20000).toList());
        assertEquals(5000, batches(IntStreamEx.range(20000).boxed(), 4).count());
        assertEquals(4, batches(IntStreamEx.range(20000).boxed(), 5000).count());
        assertEquals(19997, sliding(IntStreamEx.range(20000).mapToObj(Collections::singletonList), 4).count());
        assertEquals(IntStreamEx.range(40000).boxed().toList(), dominators(IntStreamEx.range(40000).boxed(),
            (a, b) -> a >= b).toList());
        assertEquals(15, dominators(IntStreamEx.of(new Random(1)).boxed(), (a, b) -> a >= b).takeWhile(
            x -> x < Integer.MAX_VALUE - 100000).count());

        assertEquals(IntStreamEx.of(new Random(1), 10000).boxed().sorted().toList(), sorted(
            IntStreamEx.of(new Random(1), 10000).boxed()).toList());

        assertEquals(10000, withIndices(IntStreamEx.of(new Random(1), 10000).boxed(), (idx, e) -> idx + ": " + e)
                .count());

        assertEquals(10000, limit(distinctTSO(IntStreamEx.of(new Random(1)).boxed()), 10000).toSet().size());
        assertEquals(IntStreamEx.range(10000).boxed().toList(), sorted(limit(
            distinctTSO(IntStreamEx.of(new Random(1), 0, 10000).boxed()), 10000)).toList());
        assertEquals(IntStreamEx.rangeClosed(9999, 0, -1).boxed()
            .toList(), reverseTSO(IntStreamEx.range(10000).boxed()).toList());
        assertEquals(IntStreamEx.range(10000).append(IntStreamEx.rangeClosed(9999, 0, -1)).boxed().toList(),
            mirrorTSO(IntStreamEx.range(10000).boxed()).toList());
    }

    @Test
    public void testHeadTailClose() {
        AtomicBoolean origClosed = new AtomicBoolean();
        AtomicBoolean internalClosed = new AtomicBoolean();
        AtomicBoolean finalClosed = new AtomicBoolean();
        StreamEx<Integer> res = StreamEx.of(1, 2, 3).onClose(() -> origClosed.set(true)).<Integer> headTail(
            (head, stream) -> stream.onClose(() -> internalClosed.set(true)).map(x -> x + head)).onClose(
            () -> finalClosed.set(true));
        assertEquals(asList(3, 4), res.toList());
        res.close();
        assertTrue(origClosed.get());
        assertTrue(internalClosed.get());
        assertTrue(finalClosed.get());

        res = StreamEx.<Integer> empty().headTail((head, tail) -> tail);
        assertEquals(0, res.count());
        res.close();
    }

    // Test simple non-recursive scenarios
    @Test
    public void testHeadTailSimple() {
        repeat(10, i -> {
            // Header mapping
            String input = "name,type,value\nID,int,5\nSurname,string,Smith\nGiven name,string,John";
            List<Map<String, String>> expected = asList(EntryStream.of("name", "ID", "type", "int", "value", "5")
                    .toMap(), EntryStream.of("name", "Surname", "type", "string", "value", "Smith").toMap(),
                EntryStream.of("name", "Given name", "type", "string", "value", "John").toMap());
            streamEx(() -> StreamEx.ofLines(new StringReader(input)), s -> assertEquals(expected, s.get().map(
                str -> str.split(",")).headTail(
                (header, stream) -> stream.map(row -> EntryStream.zip(header, row).toMap())).toList()));
        });
        streamEx(() -> StreamEx.of("a", "b", "c", "d"), s -> assertEquals(Collections.singletonMap("a", asList("b",
            "c", "d")), s.get().headTail((x, str) -> str.mapToEntry(e -> x, e -> e)).mapToEntry(Entry::getKey,
            Entry::getValue).grouping()));
        assertEquals(asList("b:c", "c:d"), StreamEx.of(":", "b", "c", "d").headTail(
            (head, tail) -> tail.pairMap((left, right) -> left + head + right)).toList());
        assertEquals(asList("b:", "c", "d"), StreamEx.of(":", "b", "c", "d").headTail(
            (head, tail) -> tail.mapFirst(first -> first + head)).toList());
        
    }
    
    @Test
    public void testSpliterator() {
        Spliterator<Integer> spltr = map(StreamEx.of(1,2,3,4), x -> x*2).spliterator();
        assertTrue(spltr.hasCharacteristics(Spliterator.ORDERED));
        assertEquals(4, spltr.estimateSize());
        assertTrue(spltr.tryAdvance(x -> assertEquals(2, (int)x)));
        assertEquals(3, spltr.estimateSize());
        assertTrue(spltr.tryAdvance(x -> assertEquals(4, (int)x)));
        assertEquals(2, spltr.estimateSize());
        assertTrue(spltr.tryAdvance(x -> assertEquals(6, (int)x)));
        assertEquals(1, spltr.estimateSize());
        assertTrue(spltr.tryAdvance(x -> assertEquals(8, (int)x)));
        assertFalse(spltr.tryAdvance(x -> fail("Should not be called")));
        assertEquals(0, spltr.estimateSize());
    }
}
