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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class CrossSpliteratorTest {
    @Test
    public void testCross() {
        for(int limit : new int[] {1, 2, 4, 9}) {
			List<List<Integer>> input = Collections.nCopies(3, IntStreamEx
					.range(limit).boxed().toList());
			List<List<Integer>> expected = IntStreamEx
					.range(limit * limit * limit)
					.mapToObj(
							i -> Arrays.asList(i / limit / limit, i / limit
									% limit, i % limit)).toList();
            checkSpliterator("cross", expected, () -> new CrossSpliterator<>(input));
        }
    }
}
