package one.util.streamex;

import static org.junit.Assert.*;
import static one.util.streamex.TestHelpers.*;

import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Test;

public class UnknownSizeSpliteratorTest {
    @Test
    public void testSplit() {
        List<Integer> input = IntStreamEx.range(100).boxed().toList();
        Spliterator<Integer> spliterator = new UnknownSizeSpliterator.USOfRef<>(input.iterator());
        assertEquals(Long.MAX_VALUE, spliterator.estimateSize());
        AtomicInteger count = new AtomicInteger();
        assertTrue(spliterator.tryAdvance(count::addAndGet));
        assertTrue(spliterator.tryAdvance(count::addAndGet));
        assertEquals(1, count.get());
        assertEquals(Long.MAX_VALUE, spliterator.estimateSize());
        Spliterator<Integer> spliterator2 = spliterator.trySplit();
        assertTrue(spliterator.tryAdvance(count::addAndGet));
        assertTrue(spliterator2.tryAdvance(count::addAndGet));
        assertEquals(54, count.get());
    }

    @Test
    public void testRefSpliterator() {
        for (int size : new int[] { 1, 5, 100, 1000, 1023, 1024, 1025, 2049 }) {
            List<Integer> input = IntStreamEx.range(size).boxed().toList();
            checkSpliterator(String.valueOf(size), input, () -> new UnknownSizeSpliterator.USOfRef<>(input.iterator()));
        }
    }

    @Test
    public void testIntSpliterator() {
        for (int size : new int[] { 1, 5, 100, 1000, 1023, 1024, 1025, 2049 }) {
            int[] input = IntStreamEx.range(size).toArray();
            checkSpliterator(String.valueOf(size), IntStreamEx.of(input).boxed().toList(),
                () -> new UnknownSizeSpliterator.USOfInt(Spliterators.iterator(Spliterators.spliterator(input, 0))));
        }
    }

    @Test
    public void testLongSpliterator() {
        for (int size : new int[] { 1, 5, 100, 1000, 1023, 1024, 1025, 2049 }) {
            long[] input = LongStreamEx.range(size).toArray();
            checkSpliterator(String.valueOf(size), LongStreamEx.of(input).boxed().toList(),
                () -> new UnknownSizeSpliterator.USOfLong(Spliterators.iterator(Spliterators.spliterator(input, 0))));
        }
    }

    @Test
    public void testDoubleSpliterator() {
        for (int size : new int[] { 1, 5, 100, 1000, 1023, 1024, 1025, 2049 }) {
            double[] input = LongStreamEx.range(size).asDoubleStream().toArray();
            checkSpliterator(String.valueOf(size), DoubleStreamEx.of(input).boxed().toList(),
                () -> new UnknownSizeSpliterator.USOfDouble(Spliterators.iterator(Spliterators.spliterator(input, 0))));
        }
    }

    @Test
    public void testAsStream() {
        List<Integer> input = IntStreamEx.range(100).boxed().toList();
        assertEquals(4950, StreamSupport.stream(new UnknownSizeSpliterator.USOfRef<>(input.iterator()), false)
                .mapToInt(x -> x).sum());
        assertEquals(4950, StreamSupport.stream(new UnknownSizeSpliterator.USOfRef<>(input.iterator()), true).mapToInt(
            x -> x).sum());

        input = IntStreamEx.range(5000).boxed().toList();
        assertEquals(12497500, StreamSupport.stream(new UnknownSizeSpliterator.USOfRef<>(input.iterator()), false)
                .mapToInt(x -> x).sum());
        assertEquals(12497500, StreamSupport.stream(new UnknownSizeSpliterator.USOfRef<>(input.iterator()), true)
                .mapToInt(x -> x).sum());
    }

    @Test
    public void testOptimize() {
        Stream<String> stream = Stream.of("a", "b");
        assertSame(stream, UnknownSizeSpliterator.optimize(stream));
        stream = StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(Arrays.asList("a", "b").iterator(), Spliterator.ORDERED), false)
                .distinct(); // not Head
        assertSame(stream, UnknownSizeSpliterator.optimize(stream));
        stream = StreamSupport.stream(Spliterators.spliterator(Arrays.asList("a", "b").iterator(), 2,
            Spliterator.ORDERED), false); // SIZED
        assertSame(stream, UnknownSizeSpliterator.optimize(stream));
        stream = new ConcurrentLinkedDeque<String>().stream(); // not SIZED
        assertSame(stream, UnknownSizeSpliterator.optimize(stream));

        AtomicBoolean flag = new AtomicBoolean();
        stream = StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(Arrays.asList("a", "b").iterator(), Spliterator.ORDERED), false)
                .onClose(() -> flag.set(true));
        Stream<String> optimized = UnknownSizeSpliterator.optimize(stream);
        assertNotSame(stream, optimized);
        assertTrue(optimized.spliterator() instanceof UnknownSizeSpliterator);
        assertFalse(optimized.isParallel());
        optimized.close();
        assertTrue(flag.get());

        stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(Arrays.asList("a", "b").iterator(),
            Spliterator.ORDERED), true);
        optimized = UnknownSizeSpliterator.optimize(stream);
        assertNotSame(stream, optimized);
        assertTrue(optimized.spliterator() instanceof UnknownSizeSpliterator);
        assertTrue(optimized.isParallel());
    }
}
