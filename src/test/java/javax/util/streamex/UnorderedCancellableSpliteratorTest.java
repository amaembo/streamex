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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static javax.util.streamex.TestHelpers.*;

import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class UnorderedCancellableSpliteratorTest {
    private static class BoxedInteger {
        int value;
        
        public BoxedInteger(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass())
                return false;
            BoxedInteger other = (BoxedInteger) obj;
            return value == other.value;
        }
    }

    @Test
    public void testSpliterator() {
        List<Integer> input = Arrays.asList(0b11100, 0b01110, 0b00011, 0b11010);
        Supplier<BoxedInteger> s = () -> new BoxedInteger(0xFFFFFFFF);
        BiConsumer<BoxedInteger, Integer> a = (acc, t) -> acc.value &= t;
        BinaryOperator<BoxedInteger> c = (a1, a2) -> new BoxedInteger(a1.value & a2.value);
        Predicate<BoxedInteger> p = acc -> acc.value == 0;
        List<BoxedInteger> expected = Collections.singletonList(new BoxedInteger(0));
        checkSpliterator("intersecting", expected, () -> new UnorderedCancellableSpliterator<>(input.spliterator(), s, a, c, p));
    }
}
