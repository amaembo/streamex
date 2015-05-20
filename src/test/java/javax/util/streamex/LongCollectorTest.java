/*
 * Copyright 2015 Tagir Valeev
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
package javax.util.streamex;

import static org.junit.Assert.*;

import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;

public class LongCollectorTest {
    @Test
    public void testJoining() {
        String expected = LongStream.range(0, 10000).mapToObj(String::valueOf).collect(Collectors.joining(", "));
        assertEquals(expected, LongStreamEx.range(10000).collect(LongCollector.joining(", ")));
        assertEquals(expected, LongStreamEx.range(10000).parallel().collect(LongCollector.joining(", ")));
        String expected2 = LongStreamEx.range(0, 1000).boxed().toList().toString();
        assertEquals(expected2, LongStreamEx.range(1000).collect(LongCollector.joining(", ", "[", "]")));
        assertEquals(expected2, LongStreamEx.range(1000).parallel().collect(LongCollector.joining(", ", "[", "]")));
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
        assertEquals(99, LongStreamEx.range(100).parallel().atLeast(50).collect(LongCollector.max()).getAsLong());
        assertFalse(LongStreamEx.range(100).atLeast(200).collect(LongCollector.max()).isPresent());
    }
    
    @Test
    public void testSummarizing() {
        long[] data = LongStreamEx.of(new Random(1), 1000, 1, Long.MAX_VALUE).toArray();
        LongSummaryStatistics expected = LongStream.of(data).summaryStatistics();
        LongSummaryStatistics statistics = LongStreamEx.of(data).collect(LongCollector.summarizing());
        assertEquals(expected.getCount(), statistics.getCount());
        assertEquals(expected.getSum(), statistics.getSum());
        assertEquals(expected.getMax(), statistics.getMax());
        assertEquals(expected.getMin(), statistics.getMin());
        statistics = LongStreamEx.of(data).parallel().collect(LongCollector.summarizing());
        assertEquals(expected.getCount(), statistics.getCount());
        assertEquals(expected.getSum(), statistics.getSum());
        assertEquals(expected.getMax(), statistics.getMax());
        assertEquals(expected.getMin(), statistics.getMin());
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
    
    @Test
    public void testAsCollector() {
        assertEquals(10000499500l, (long)LongStream.range(10000000, 10001000).boxed().collect(LongCollector.summing()));
        assertEquals(10000499500l, (long)LongStream.range(10000000, 10001000).boxed().parallel().collect(LongCollector.summing()));
        assertEquals(1000, (long)LongStream.range(0, 1000).boxed().collect(LongCollector.counting()));
    }
}
