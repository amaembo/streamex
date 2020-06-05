/*
 * Copyright 2015, 2020 StreamEx contributors
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

import org.junit.Test;

import java.util.Spliterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PrefixOpsTest {
  @Test
  public void testNoSplitAfterAdvance() {
    Spliterator<Integer> spliterator = IntStreamEx.range(100).boxed().unordered().prefix(Integer::sum).spliterator();
    assertTrue(spliterator.tryAdvance(i -> assertEquals((int) i, 0)));
    assertTrue(spliterator.tryAdvance(i -> assertEquals((int) i, 1)));
    assertTrue(spliterator.tryAdvance(i -> assertEquals((int) i, 3)));
    assertTrue(spliterator.tryAdvance(i -> assertEquals((int) i, 6)));
    assertNull(spliterator.trySplit());
  }
}
