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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import org.junit.Test;

import static one.util.streamex.TestHelpers.consumeElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
    consumeElement(spliterator, 0);
    consumeElement(spliterator, 1);
    consumeElement(spliterator, 3);
    consumeElement(spliterator, 6);
    assertNull(spliterator.trySplit());
  }

  @Test
  public void testLongNoSplitAfterAdvance() {
    Spliterator.OfLong spliterator = LongStreamEx.range(100).unordered().prefix(Long::sum).spliterator();
    assertNull(spliterator.trySplit());
    consumeElement(spliterator, 0L);
    consumeElement(spliterator, 1L);
    consumeElement(spliterator, 3L);
    consumeElement(spliterator, 6L);
    assertNull(spliterator.trySplit());
  }
  
  @Test
  public void testForEachAfterSplitAndAdvance() {
    Spliterator<String> spliterator = StreamEx.constant("a", 5).unordered().prefix(String::concat).spliterator();
    Spliterator<String> spliterator2 = spliterator.trySplit();
    assertNotNull(spliterator2);
    Set<String> remainingElements = new HashSet<>(Arrays.asList("a", "aa", "aaa", "aaaa", "aaaaa"));
    consumeElement(spliterator, remainingElements);
    consumeElement(spliterator2, remainingElements);
    spliterator.forEachRemaining(x -> assertTrue(remainingElements.remove(x)));
    spliterator2.forEachRemaining(x -> assertTrue(remainingElements.remove(x)));
    assertEquals(0, remainingElements.size());
  }
  
  @Test
  public void testIntForEachAfterSplitAndAdvance() {
    Spliterator.OfInt spliterator = IntStreamEx.constant(1, 5).unordered().prefix(Integer::sum).spliterator();
    Spliterator.OfInt spliterator2 = spliterator.trySplit();
    assertNotNull(spliterator2);
    Set<Integer> remainingElements = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
    consumeElement(spliterator, remainingElements);
    consumeElement(spliterator2, remainingElements);
    spliterator.forEachRemaining((IntConsumer) x -> assertTrue(remainingElements.remove(x)));
    spliterator2.forEachRemaining((IntConsumer) x -> assertTrue(remainingElements.remove(x)));
    assertEquals(0, remainingElements.size());
  }
  
  @Test
  public void testLongForEachAfterSplitAndAdvance() {
    Spliterator.OfLong spliterator = LongStreamEx.constant(1, 5).unordered().prefix(Long::sum).spliterator();
    Spliterator.OfLong spliterator2 = spliterator.trySplit();
    assertNotNull(spliterator2);
    Set<Long> remainingElements = new HashSet<>(Arrays.asList(1L, 2L, 3L, 4L, 5L));
    consumeElement(spliterator, remainingElements);
    consumeElement(spliterator2, remainingElements);
    spliterator.forEachRemaining((LongConsumer) x -> assertTrue(remainingElements.remove(x)));
    spliterator2.forEachRemaining((LongConsumer) x -> assertTrue(remainingElements.remove(x)));
    assertEquals(0, remainingElements.size());
  }
}
