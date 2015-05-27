package javax.util.streamex;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class MoreCollectorsTest {
    
    @Test
    public void testMaxAll() {
        List<String> input = Arrays.asList("a", "bb", "c", "", "cc", "eee", "bb", "ddd");
        assertEquals(Arrays.asList("eee", "ddd"),
                StreamEx.of(input).collect(MoreCollectors.maxAll(Comparator.comparingInt(String::length))));
        assertEquals("eee,ddd",
                StreamEx.of(input).collect(MoreCollectors.maxAll(Comparator.comparingInt(String::length), Collectors.joining(","))));
        assertEquals(Arrays.asList("eee", "ddd"),
                StreamEx.of(input).parallel().collect(MoreCollectors.maxAll(Comparator.comparingInt(String::length))));
        assertEquals(Arrays.asList(""),
                StreamEx.of(input).collect(MoreCollectors.minAll(Comparator.comparingInt(String::length))));
        assertEquals(Arrays.asList(""),
                StreamEx.of(input).parallel().collect(MoreCollectors.minAll(Comparator.comparingInt(String::length))));
        assertEquals(Collections.emptyList(), StreamEx.<String>empty().collect(MoreCollectors.maxAll(Comparator.comparingInt(String::length))));
        assertEquals(Collections.emptyList(), StreamEx.<String>empty().parallel().collect(MoreCollectors.maxAll(Comparator.comparingInt(String::length))));
    }
}
