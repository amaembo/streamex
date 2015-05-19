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
}
