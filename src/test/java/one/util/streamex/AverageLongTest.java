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

import static one.util.streamex.StreamExInternals.*;
import static org.junit.Assert.*;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import one.util.streamex.LongStreamEx;
import one.util.streamex.MoreCollectors;

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

        int[] input = new Random(1).ints(1000).toArray();
        OptionalDouble expected = IntStream.of(input).average();
        assertEquals(expected, Arrays.stream(input)
                .collect(AverageLong::new, AverageLong::accept, AverageLong::combine).result());

        AverageLong avg1 = new AverageLong();
        avg1.accept(-2);
        AverageLong avg2 = new AverageLong();
        avg2.accept(-2);
        assertEquals(-2.0, avg1.combine(avg2).result().getAsDouble(), 0.0);

        assertEquals(expected, Arrays.stream(input).parallel().collect(AverageLong::new, AverageLong::accept,
            AverageLong::combine).result());
    }

    @Test
    public void testCombine() {
        Random r = new Random(1);
        for (int i = 0; i < 100; i++) {
            AverageLong avg1 = new AverageLong();
            AverageLong avg2 = new AverageLong();
            long[] set1 = r.longs(100).toArray();
            long[] set2 = r.longs(100).toArray();
            double expected = LongStreamEx.of(set1).append(set2).boxed().collect(getBigIntegerAverager()).getAsDouble();
            LongStream.of(set1).forEach(avg1::accept);
            LongStream.of(set2).forEach(avg2::accept);
            assertEquals("#" + i, expected, avg1.combine(avg2).result().getAsDouble(), Math.abs(expected / 1e14));
        }
    }

    @Test
    public void testCompareToBigInteger() {
        long[] input = LongStreamEx.of(new Random(1), 1000).toArray();
        Supplier<LongStream> supplier = () -> Arrays.stream(input);
        double expected = supplier.get().boxed().collect(getBigIntegerAverager()).getAsDouble();
        assertEquals(expected, supplier.get().collect(AverageLong::new, AverageLong::accept, AverageLong::combine)
                .result().getAsDouble(), Math.abs(expected) / 1e14);
        assertEquals(expected, supplier.get().parallel().collect(AverageLong::new, AverageLong::accept,
            AverageLong::combine).result().getAsDouble(), Math.abs(expected) / 1e14);
    }

    private Collector<Long, ?, OptionalDouble> getBigIntegerAverager() {
        BiFunction<BigInteger, Long, OptionalDouble> finisher = (BigInteger sum, Long cnt) -> cnt == 0L ? OptionalDouble
                .empty()
                : OptionalDouble.of(new BigDecimal(sum).divide(BigDecimal.valueOf(cnt), MathContext.DECIMAL64)
                        .doubleValue());
        Collector<Long, ?, OptionalDouble> averager = MoreCollectors.pairing(Collectors.reducing(BigInteger.ZERO,
            BigInteger::valueOf, (BigInteger b1, BigInteger b2) -> b1.add(b2)), Collectors.counting(), finisher);
        return averager;
    }
}
