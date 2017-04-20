/*
 * Copyright 2015, 2017 Tagir Valeev
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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static one.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
/* package */ class Java9Specific extends StreamExInternals.VerSpec {
    @Override
    final <T, S extends AbstractStreamEx<T, S>> S callWhile(AbstractStreamEx<T, S> stream, Predicate<? super T> predicate, boolean drop) {
        return stream.supply(drop ? stream.stream().dropWhile(predicate) : stream.stream().takeWhile(predicate));
    }

    @Override
    final IntStreamEx callWhile(IntStreamEx stream, IntPredicate predicate, boolean drop) {
        return new IntStreamEx(drop ? stream.stream().dropWhile(predicate) : stream.stream().takeWhile(predicate), stream.context);
    }

    @Override
    final LongStreamEx callWhile(LongStreamEx stream, LongPredicate predicate, boolean drop) {
        return new LongStreamEx(drop ? stream.stream().dropWhile(predicate) : stream.stream().takeWhile(predicate), stream.context);
    }

    @Override
    final DoubleStreamEx callWhile(DoubleStreamEx stream, DoublePredicate predicate, boolean drop) {
        return new DoubleStreamEx(drop ? stream.stream().dropWhile(predicate) : stream.stream().takeWhile(predicate), stream.context);
    }

    @Override
    IntStream ofChars(CharSequence seq) {
        return seq.chars();
    }
}
