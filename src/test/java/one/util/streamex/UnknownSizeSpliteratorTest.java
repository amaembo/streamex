package one.util.streamex;

import static org.junit.Assert.*;
import static one.util.streamex.TestHelpers.*;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
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
        assertEquals(49, spliterator.estimateSize());
        assertEquals(49, spliterator2.estimateSize());
        assertTrue(spliterator.tryAdvance(count::addAndGet));
        assertTrue(spliterator2.tryAdvance(count::addAndGet));
        assertEquals(48, spliterator.estimateSize());
        assertEquals(48, spliterator2.estimateSize());
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
        assertEquals(4950,
            StreamSupport.stream(new UnknownSizeSpliterator.USOfRef<>(input.iterator()), true).mapToInt(x -> x).sum());

        input = IntStreamEx.range(5000).boxed().toList();
        assertEquals(12497500, StreamSupport.stream(new UnknownSizeSpliterator.USOfRef<>(input.iterator()), false)
                .mapToInt(x -> x).sum());
        assertEquals(12497500, StreamSupport.stream(new UnknownSizeSpliterator.USOfRef<>(input.iterator()), true)
                .mapToInt(x -> x).sum());
    }
}
