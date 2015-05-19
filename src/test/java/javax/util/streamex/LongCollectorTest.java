package javax.util.streamex;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;

public class LongCollectorTest {
    @Test
    public void testJoining() {
        String expected = LongStream.range(0, 10000).mapToObj(String::valueOf).collect(Collectors.joining(", "));
        assertEquals(expected, LongStreamEx.range(10000).collect(LongCollector.joining(", ")));
        assertEquals(expected, LongStreamEx.range(10000).parallel().collect(LongCollector.joining(", ")));
    }
    
    @Test
    public void testCounting() {
        assertEquals(5000L, (long)LongStreamEx.range(10000).atLeast(5000).collect(LongCollector.counting()));
        assertEquals(5000L, (long)LongStreamEx.range(10000).parallel().atLeast(5000).collect(LongCollector.counting()));
    }
    
    @Test
    public void testSumming() {
        assertEquals(3725, (long)LongStreamEx.range(100).atLeast(50).collect(LongCollector.summing()));
        assertEquals(3725, (long)LongStreamEx.range(100).parallel().atLeast(50).collect(LongCollector.summing()));
    }
    
    @Test
    public void testMin() {
        assertEquals(50, LongStreamEx.range(100).atLeast(50).collect(LongCollector.min()).getAsLong());
        assertFalse(LongStreamEx.range(100).atLeast(200).collect(LongCollector.min()).isPresent());
    }
    
    @Test
    public void testMax() {
        assertEquals(99, LongStreamEx.range(100).atLeast(50).collect(LongCollector.max()).getAsLong());
        assertFalse(LongStreamEx.range(100).atLeast(200).collect(LongCollector.max()).isPresent());
    }
    
    @Test
    public void testToArray() {
        assertArrayEquals(new long[] {0,1,2,3,4}, LongStreamEx.of(0,1,2,3,4).collect(LongCollector.toArray()));
    }
    
    @Test
    public void testPartitioning() {
        long[] expectedEven = LongStream.range(0, 1000).map(i -> i*2).toArray();
        long[] expectedOdd = LongStream.range(0, 1000).map(i -> i*2+1).toArray();
        Map<Boolean, long[]> oddEven = LongStreamEx.range(2000).collect(LongCollector.partitioningBy(i -> i % 2 == 0));
        assertArrayEquals(expectedEven, oddEven.get(true));
        assertArrayEquals(expectedOdd, oddEven.get(false));
        oddEven = LongStreamEx.range(2000).parallel().collect(LongCollector.partitioningBy(i -> i % 2 == 0));
        assertArrayEquals(expectedEven, oddEven.get(true));
        assertArrayEquals(expectedOdd, oddEven.get(false));
    }
    
    @Test
    public void testGroupingBy() {
        Map<Long, long[]> collected = LongStreamEx.range(2000).collect(
                LongCollector.groupingBy(i -> i % 3));
        for(long i=0; i<3; i++) {
            long rem = i;
            assertArrayEquals(LongStream.range(0, 2000).filter(a -> a % 3 == rem).toArray(), collected.get(i));
        }
        collected = LongStreamEx.range(2000).parallel().collect(
                LongCollector.groupingBy(i -> i % 3));
        for(long i=0; i<3; i++) {
            long rem = i;
            assertArrayEquals(LongStream.range(0, 2000).filter(a -> a % 3 == rem).toArray(), collected.get(i));
        }
    }
}
