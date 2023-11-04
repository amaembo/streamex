/*
 * Copyright 2015, 2019 StreamEx contributors
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

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.*;
import java.util.stream.IntStream;

/**
 * @author Tagir Valeev
 */
/* package */ class VersionSpecific {

    <T, S extends AbstractStreamEx<T, S>> S callWhile(AbstractStreamEx<T, S> stream, Predicate<? super T> predicate, boolean drop) {
        Spliterator<T> spltr = stream.spliterator();
        return stream.supply(
                spltr.hasCharacteristics(Spliterator.ORDERED) ? new TakeDrop.TDOfRef<>(spltr, drop, false, predicate)
                        : new TakeDrop.UnorderedTDOfRef<T>(spltr, drop, false, predicate));
    }

    IntStreamEx callWhile(IntStreamEx stream, IntPredicate predicate, boolean drop) {
        return stream.delegate(new TakeDrop.TDOfInt(stream.spliterator(), drop, false, predicate));
    }

    LongStreamEx callWhile(LongStreamEx stream, LongPredicate predicate, boolean drop) {
        return stream.delegate(new TakeDrop.TDOfLong(stream.spliterator(), drop, false, predicate));
    }

    DoubleStreamEx callWhile(DoubleStreamEx stream, DoublePredicate predicate, boolean drop) {
        return stream.delegate(new TakeDrop.TDOfDouble(stream.spliterator(), drop, false, predicate));
    }

    <T, R> StreamEx<R> callMapMulti(AbstractStreamEx<T, ?> s, BiConsumer<? super T, ? super Consumer<R>> mapper) {
        return s.flatCollection(e -> {
            List<R> result = new ArrayList<>();
            mapper.accept(e, (Consumer<R>) result::add);
            return result;
        });
    }

    <T> IntStreamEx callMapMultiToInt(AbstractStreamEx<T, ?> s, BiConsumer<? super T, ? super IntConsumer> mapper) {
        return s.flatMapToInt(e -> {
            Internals.IntBuffer result = new Internals.IntBuffer();
            mapper.accept(e, (IntConsumer) result::add);
            return result.stream();
        });
    }
    
    <T> LongStreamEx callMapMultiToLong(AbstractStreamEx<T, ?> s, BiConsumer<? super T, ? super LongConsumer> mapper) {
        return s.flatMapToLong(e -> {
            Internals.LongBuffer result = new Internals.LongBuffer();
            mapper.accept(e, (LongConsumer) result::add);
            return result.stream();
        });
    }

    <T> DoubleStreamEx callMapMultiToDouble(AbstractStreamEx<T, ?> s, BiConsumer<? super T, ? super DoubleConsumer> mapper) {
        return s.flatMapToDouble(e -> {
            Internals.DoubleBuffer result = new Internals.DoubleBuffer();
            mapper.accept(e, (DoubleConsumer) result::add);
            return result.stream();
        });
    }
    
    IntStream ofChars(CharSequence seq) {
        // In JDK 8 there's only default chars() method which uses
        // IteratorSpliterator
        // In JDK 9 chars() method for most of implementations is much better
        return CharBuffer.wrap(seq).chars();
    }
}
