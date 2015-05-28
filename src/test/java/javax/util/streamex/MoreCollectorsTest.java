package javax.util.streamex;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
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
        
        List<Integer> ints = IntStreamEx.of(new Random(1), 10000, 1, 1000).boxed().toList();
        List<Integer> expected = null;
        for(Integer i : ints) {
            if(expected == null || i > expected.get(0)) {
                expected = new ArrayList<>();
                expected.add(i);
            } else if(i.equals(expected.get(0))) {
                expected.add(i);
            }
        }
        assertEquals(expected, StreamEx.of(ints).collect(MoreCollectors.maxAll(Integer::compare)));
        assertEquals(expected, StreamEx.of(ints).parallel().collect(MoreCollectors.maxAll(Integer::compare)));
    }
}
