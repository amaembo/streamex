package javax.util.streamex;

import static org.junit.Assert.*;

import java.util.IntSummaryStatistics;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

public class IntCollectorTest {
    @Test
    public void testJoining() {
        String expected = IntStream.range(0, 10000).mapToObj(String::valueOf).collect(Collectors.joining(", "));
        assertEquals(expected, IntStreamEx.range(10000).collect(IntCollector.joining(", ")));
        assertEquals(expected, IntStreamEx.range(10000).parallel().collect(IntCollector.joining(", ")));
    }
    
    @Test
    public void testCounting() {
        assertEquals(5000L, (long)IntStreamEx.range(10000).atLeast(5000).collect(IntCollector.counting()));
        assertEquals(5000L, (long)IntStreamEx.range(10000).parallel().atLeast(5000).collect(IntCollector.counting()));
    }
    
    @Test
    public void testSumming() {
        assertEquals(3725, (int)IntStreamEx.range(100).atLeast(50).collect(IntCollector.summing()));
        assertEquals(3725, (int)IntStreamEx.range(100).parallel().atLeast(50).collect(IntCollector.summing()));
        
        int[] input = IntStreamEx.of(new Random(1), 10000, 1, 1000).toArray();
        Map<Boolean, Integer> expected = IntStream.of(input).boxed().collect(Collectors.partitioningBy(i -> i%2 == 0, Collectors.summingInt(Integer::intValue)));
        Map<Boolean, Integer> sumEvenOdd = IntStreamEx.of(input).collect(IntCollector.partitioningBy(i -> i%2 == 0, IntCollector.summing()));
        assertEquals(expected, sumEvenOdd);
        sumEvenOdd = IntStreamEx.of(input).parallel().collect(IntCollector.partitioningBy(i -> i%2 == 0, IntCollector.summing()));
        assertEquals(expected, sumEvenOdd);
    }
    
    @Test
    public void testMin() {
        assertEquals(50, IntStreamEx.range(100).atLeast(50).collect(IntCollector.min()).getAsInt());
        assertFalse(IntStreamEx.range(100).atLeast(200).collect(IntCollector.min()).isPresent());
    }
    
    @Test
    public void testMax() {
        assertEquals(99, IntStreamEx.range(100).atLeast(50).collect(IntCollector.max()).getAsInt());
        assertEquals(99, IntStreamEx.range(100).parallel().atLeast(50).collect(IntCollector.max()).getAsInt());
        assertFalse(IntStreamEx.range(100).atLeast(200).collect(IntCollector.max()).isPresent());
    }
    
    @Test
    public void testSummarizing() {
        int[] data = IntStreamEx.of(new Random(1), 1000, 1, Integer.MAX_VALUE).toArray();
        IntSummaryStatistics expected = IntStream.of(data).summaryStatistics();
        IntSummaryStatistics statistics = IntStreamEx.of(data).collect(IntCollector.summarizing());
        assertEquals(expected.getCount(), statistics.getCount());
        assertEquals(expected.getSum(), statistics.getSum());
        assertEquals(expected.getMax(), statistics.getMax());
        assertEquals(expected.getMin(), statistics.getMin());
        statistics = IntStreamEx.of(data).parallel().collect(IntCollector.summarizing());
        assertEquals(expected.getCount(), statistics.getCount());
        assertEquals(expected.getSum(), statistics.getSum());
        assertEquals(expected.getMax(), statistics.getMax());
        assertEquals(expected.getMin(), statistics.getMin());
    }
    
    @Test
    public void testToArray() {
        assertArrayEquals(new int[] {0,1,2,3,4}, IntStreamEx.of(0,1,2,3,4).collect(IntCollector.toArray()));
    }
    
    @Test
    public void testPartitioning() {
        int[] expectedEven = IntStream.range(0, 1000).map(i -> i*2).toArray();
        int[] expectedOdd = IntStream.range(0, 1000).map(i -> i*2+1).toArray();
        Map<Boolean, int[]> oddEven = IntStreamEx.range(2000).collect(IntCollector.partitioningBy(i -> i % 2 == 0));
        assertArrayEquals(expectedEven, oddEven.get(true));
        assertArrayEquals(expectedOdd, oddEven.get(false));
        oddEven = IntStreamEx.range(2000).parallel().collect(IntCollector.partitioningBy(i -> i % 2 == 0));
        assertArrayEquals(expectedEven, oddEven.get(true));
        assertArrayEquals(expectedOdd, oddEven.get(false));
    }
    
    @Test
    public void testGroupingBy() {
        Map<Integer, int[]> collected = IntStreamEx.range(2000).collect(
                IntCollector.groupingBy(i -> i % 3));
        for(int i=0; i<3; i++) {
            int rem = i;
            assertArrayEquals(IntStream.range(0, 2000).filter(a -> a % 3 == rem).toArray(), collected.get(i));
        }
        collected = IntStreamEx.range(2000).parallel().collect(
                IntCollector.groupingBy(i -> i % 3));
        for(int i=0; i<3; i++) {
            int rem = i;
            assertArrayEquals(IntStream.range(0, 2000).filter(a -> a % 3 == rem).toArray(), collected.get(i));
        }
    }
    
    @Test
    public void testAsCollector() {
        assertEquals(499500, (int)IntStream.range(0, 1000).boxed().collect(IntCollector.summing()));
        assertEquals(499500, (int)IntStream.range(0, 1000).boxed().parallel().collect(IntCollector.summing()));
        assertEquals(1000, (long)IntStream.range(0, 1000).boxed().collect(IntCollector.counting()));
    }
}
