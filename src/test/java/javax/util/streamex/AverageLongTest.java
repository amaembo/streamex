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

import static javax.util.streamex.StreamExInternals.*;
import static org.junit.Assert.*;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class AverageLongTest {

    @Test
    public void testAverageLongNoOverflow() {
        AverageLong avg = new AverageLong();
        assertFalse(avg.result().isPresent());
        avg.accept(1);
        avg.accept(2);
        avg.accept(3);
        assertEquals(2.0, avg.result().getAsDouble(), 0.0);

        avg.accept(2);
        avg.accept(-4);
        avg.accept(8);
        assertEquals(2.0, avg.result().getAsDouble(), 0.0);
        
        OptionalDouble expected = IntStreamEx.of(new Random(1), 1000).average();
        assertEquals(expected,
            IntStreamEx.of(new Random(1), 1000).collect(AverageLong::new, AverageLong::accept, AverageLong::combine).result());
        
        AverageLong avg1 = new AverageLong();
        avg1.accept(-2);
        AverageLong avg2 = new AverageLong();
        avg2.accept(-2);
        assertEquals(-2.0, avg1.combine(avg2).result().getAsDouble(), 0.0);

        assertEquals(expected,
            IntStreamEx.of(new Random(1), 1000).parallel().collect(AverageLong::new, AverageLong::accept, AverageLong::combine).result());
    }
    
    @Test
    public void testCompareToBigInteger() {
        Collector<Long, ?, BigInteger> summing = Collectors.reducing(BigInteger.ZERO, BigInteger::valueOf,
            BigInteger::add);
        BiFunction<BigInteger, Long, OptionalDouble> finisher = (BigInteger sum, Long cnt) -> cnt == 0L ? OptionalDouble.empty() : 
            OptionalDouble.of(new BigDecimal(sum).divide(BigDecimal.valueOf(cnt), MathContext.DECIMAL64).doubleValue());
        OptionalDouble expected = LongStreamEx.of(new Random(1), 1000).boxed()
                .collect(MoreCollectors.pairing(summing, Collectors.counting(), finisher));
        assertEquals(expected,
            LongStreamEx.of(new Random(1), 1000).collect(AverageLong::new, AverageLong::accept, AverageLong::combine).result());
        assertEquals(expected,
            LongStreamEx.of(new Random(1), 1000).parallel().collect(AverageLong::new, AverageLong::accept, AverageLong::combine).result());
    }
}
