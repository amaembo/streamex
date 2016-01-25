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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Spliterator;

import org.junit.Test;

/**
 * @author Tagir Valeev
 *
 */
public class BaseStreamExTest {
    @Test
    public void testSpliterator() {
        Spliterator<Integer> spltr = Arrays.spliterator(new Integer[] {1,2,3}); 
        StreamEx<Integer> s = StreamEx.of(spltr);
        assertFalse(s.isParallel());
        assertSame(spltr, s.spliterator());
    }

    @Test(expected = IllegalStateException.class)
    public void testSpliteratorConsumed() {
        StreamEx<Integer> s = StreamEx.of(1, 2, 3);
        s.spliterator();
        s.spliterator();
    }
}
