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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Tagir Valeev
 */
public class LimiterTest {
    @Test
    public void testLimiter() {
        Comparator<String> cmp = Comparator.nullsFirst(Comparator.comparingInt(String::length));
        exerciseLimiter("str", Arrays.asList("abc", "abgdc", "abd", "a", "fgssdfg", "sfsvsx", null,
            "wrffvs", "xcvbxvcb", "sffg", "abe", "adf", "abh"), cmp);
        for(int i : new int[] {10, 100, 1000, 10000, 100000}) {
            exerciseLimiter("asc, nat, "+i, IntStream.range(0, i).boxed().collect(Collectors.toList()), Comparator.<Integer>naturalOrder());
            exerciseLimiter("asc, dec, "+i, IntStream.range(0, i).boxed().collect(Collectors.toList()), Comparator.comparingInt(x -> x/10));
            exerciseLimiter("desc, nat, "+i, IntStream.range(0, i).mapToObj(x -> ~x).collect(Collectors.toList()), Comparator.<Integer>naturalOrder());
            exerciseLimiter("desc, dec, "+i, IntStream.range(0, i).mapToObj(x -> ~x).collect(Collectors.toList()), Comparator.comparingInt(x -> x/10));
            exerciseLimiter("rnd, nat, "+i, new Random(1).ints(i).boxed().collect(Collectors.toList()), Comparator.<Integer>naturalOrder());
            exerciseLimiter("rnd, dec, "+i, new Random(1).ints(i).boxed().collect(Collectors.toList()), Comparator.comparingInt(x -> x/10));
            exerciseLimiter("rnd2, nat, "+i, new Random(1).ints(i, -1000, 1000).boxed().collect(Collectors.toList()), Comparator.<Integer>naturalOrder());
            exerciseLimiter("rnd2, dec, "+i, new Random(1).ints(i, -1000, 1000).boxed().collect(Collectors.toList()), Comparator.comparingInt(x -> x/10));
        }
    }
    
    public static <T> void exerciseLimiter(String msg, Collection<T> input, Comparator<? super T> comp) {
        for(int limit : new int[] {0, 1, 2, 5, 10, 20, 100, 1000}) {
            exerciseLimiter(msg, input, limit, comp);
        }
    }

    public static <T> void exerciseLimiter(String msg, Collection<T> input, int limit, Comparator<? super T> comp) {
        List<T> expected = input.stream().sorted(comp).limit(limit).collect(Collectors.toList());
        List<T> actual = input.stream().collect(MoreCollectors.least(comp, limit));
        assertEquals("Mismatch (sequential), "+msg+", limit="+limit, expected, actual);
        actual = input.parallelStream().collect(MoreCollectors.least(comp, limit));
        assertEquals("Mismatch (parallel), "+msg+", limit="+limit, expected, actual);
    }
}
