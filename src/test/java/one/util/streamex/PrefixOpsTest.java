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

import java.util.Spliterator;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import org.junit.Test;

import static one.util.streamex.TestHelpers.consumeElement;
import static org.junit.Assert.*;

public class PrefixOpsTest {
  @Test
  public void testNoSplitAfterAdvance() {
    Spliterator<Integer> spliterator = IntStreamEx.range(100).boxed().unordered().prefix(Integer::sum).spliterator();
    consumeElement(spliterator, 0);
    consumeElement(spliterator, 1);
    consumeElement(spliterator, 3);
    consumeElement(spliterator, 6);
    assertNull(spliterator.trySplit());
  }

  @Test
  public void testIntNoSplitAfterAdvance() {
    Spliterator.OfInt spliterator = IntStreamEx.range(100).unordered().prefix(Integer::sum).spliterator();
    assertTrue(spliterator.tryAdvance((IntConsumer) i -> assertEquals(i, 0)));
    assertTrue(spliterator.tryAdvance((IntConsumer) i -> assertEquals(i, 1)));
    assertTrue(spliterator.tryAdvance((IntConsumer) i -> assertEquals(i, 3)));
    assertTrue(spliterator.tryAdvance((IntConsumer) i -> assertEquals(i, 6)));
    assertNull(spliterator.trySplit());
  }

  @Test
  public void testLongNoSplitAfterAdvance() {
    Spliterator.OfLong spliterator = LongStreamEx.range(100).unordered().prefix(Long::sum).spliterator();
    assertTrue(spliterator.tryAdvance((LongConsumer) i -> assertEquals(i, 0L)));
    assertTrue(spliterator.tryAdvance((LongConsumer) i -> assertEquals(i, 1L)));
    assertTrue(spliterator.tryAdvance((LongConsumer) i -> assertEquals(i, 3L)));
    assertTrue(spliterator.tryAdvance((LongConsumer) i -> assertEquals(i, 6L)));
    assertNull(spliterator.trySplit());
  }

  @Test
  public void testDoubleNoSplitAfterAdvance() {
    Spliterator.OfDouble spliterator = DoubleStreamEx.constant(1.0d, 1000).unordered().prefix(Double::sum).spliterator();
    assertTrue(spliterator.tryAdvance((DoubleConsumer) i -> assertEquals(i, 1d, 0.0)));
    assertTrue(spliterator.tryAdvance((DoubleConsumer) i -> assertEquals(i, 2d, 0.0)));
    assertTrue(spliterator.tryAdvance((DoubleConsumer) i -> assertEquals(i, 3d, 0.0)));
    assertTrue(spliterator.tryAdvance((DoubleConsumer) i -> assertEquals(i, 4d, 0.0)));
    assertNull(spliterator.trySplit());
  }
}
