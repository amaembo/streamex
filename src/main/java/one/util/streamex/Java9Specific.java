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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * @author Tagir Valeev
 */
/* package */ class Java9Specific extends VersionSpecific {
    private static final MethodHandle[][] JDK9_METHODS = initJdk9Methods();
    private static final int IDX_STREAM = 0;
    private static final int IDX_INT_STREAM = 1;
    private static final int IDX_LONG_STREAM = 2;
    private static final int IDX_DOUBLE_STREAM = 3;
    private static final int IDX_TAKE_WHILE = 0;
    private static final int IDX_DROP_WHILE = 1;

    static MethodHandle[][] initJdk9Methods() {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodType[] types = {MethodType.methodType(Stream.class, Predicate.class),
                MethodType.methodType(IntStream.class, IntPredicate.class),
                MethodType.methodType(LongStream.class, LongPredicate.class),
                MethodType.methodType(DoubleStream.class, DoublePredicate.class)};
        MethodHandle[][] methods = new MethodHandle[types.length][];
        try {
            int i = 0;
            for (MethodType type : types) {
                methods[i++] = new MethodHandle[]{
                        lookup.findVirtual(type.returnType(), "takeWhile", type),
                        lookup.findVirtual(type.returnType(), "dropWhile", type)
                };
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
        return methods;
    }

    @Override
    <T, S extends AbstractStreamEx<T, S>> S callWhile(AbstractStreamEx<T, S> stream, Predicate<? super T> predicate, boolean drop) {
        try {
            return stream.supply((Stream<T>) JDK9_METHODS[IDX_STREAM][drop ? IDX_DROP_WHILE : IDX_TAKE_WHILE]
                    .invokeExact(stream.stream(), predicate));
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    @Override
    final IntStreamEx callWhile(IntStreamEx stream, IntPredicate predicate, boolean drop) {
        try {
            return new IntStreamEx((IntStream) JDK9_METHODS[IDX_INT_STREAM][drop ? IDX_DROP_WHILE : IDX_TAKE_WHILE]
                    .invokeExact(stream.stream(), predicate), stream.context);
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    @Override
    final LongStreamEx callWhile(LongStreamEx stream, LongPredicate predicate, boolean drop) {
        try {
            return new LongStreamEx((LongStream) JDK9_METHODS[IDX_LONG_STREAM][drop ? IDX_DROP_WHILE : IDX_TAKE_WHILE]
                    .invokeExact(stream.stream(), predicate), stream.context);
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    @Override
    final DoubleStreamEx callWhile(DoubleStreamEx stream, DoublePredicate predicate, boolean drop) {
        try {
            return new DoubleStreamEx((DoubleStream) JDK9_METHODS[IDX_DOUBLE_STREAM][drop ? IDX_DROP_WHILE : IDX_TAKE_WHILE]
                    .invokeExact(stream.stream(), predicate), stream.context);
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    @Override
    IntStream ofChars(CharSequence seq) {
        return seq.chars();
    }
}
