package javax.util.streamex;

import static org.junit.Assert.*;

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
    }
    
    @Test
    public void testMin() {
        assertEquals(50, IntStreamEx.range(100).atLeast(50).collect(IntCollector.min()).getAsInt());
        assertFalse(IntStreamEx.range(100).atLeast(200).collect(IntCollector.min()).isPresent());
    }
    
    @Test
    public void testMax() {
        assertEquals(99, IntStreamEx.range(100).atLeast(50).collect(IntCollector.max()).getAsInt());
        assertFalse(IntStreamEx.range(100).atLeast(200).collect(IntCollector.max()).isPresent());
    }
    
    @Test
    public void testToArray() {
        assertArrayEquals(new int[] {0,1,2,3,4}, IntStreamEx.of(0,1,2,3,4).collect(IntCollector.toArray()));
    }
}
