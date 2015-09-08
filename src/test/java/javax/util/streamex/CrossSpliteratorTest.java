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

import static javax.util.streamex.TestHelpers.*;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class CrossSpliteratorTest {
    @Test
    public void testCross() {
        List<List<Integer>> input = Collections.nCopies(3, IntStreamEx.range(10).boxed().toList());
        List<Integer> expected = IntStreamEx.range(1000).boxed().toList();
        Function<List<Integer>, Integer> mapper = list -> list.get(0)*100+list.get(1)*10+list.get(2);
        checkSpliterator("cross", expected, () -> new CrossSpliterator<>(input, mapper));
    }
}
