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

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/* package */final class EmitterSpliterator<T> extends Spliterators.AbstractSpliterator<T> implements Consumer<T> {
    StreamEx.Emitter<T> e;
    Spliterator<T> buf;
    boolean hasValue;
    Consumer<? super T> action;
    Stream.Builder<T> builder;

    EmitterSpliterator(StreamEx.Emitter<T> e) {
        super(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.IMMUTABLE);
        this.e = e;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (buf != null) {
            if (buf.tryAdvance(action))
                return true;
            buf = null;
        }
        hasValue = false;
        this.action = action;
        while (!hasValue && e != null) {
            e = e.next(this);
        }
        this.action = null;
        if (builder != null) {
            buf = builder.build().spliterator();
            builder = null;
        }
        return hasValue;
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        if (buf != null) {
            buf.forEachRemaining(action);
            buf = null;
        }
        StreamEx.Emitter<T> e = this.e;
        this.e = null;
        while (e != null)
            e = e.next(action);
    }

    @Override
    public void accept(T t) {
        if (hasValue) {
            if (builder == null) {
                builder = Stream.builder();
            }
            builder.accept(t);
        } else {
            action.accept(t);
            hasValue = true;
        }
    }

    static final class OfInt extends Spliterators.AbstractIntSpliterator implements IntConsumer {
        IntStreamEx.IntEmitter e;
        Spliterator.OfInt buf;
        boolean hasValue;
        IntConsumer action;
        IntStream.Builder builder;

        OfInt(IntStreamEx.IntEmitter e) {
            super(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL);
            this.e = e;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (buf != null) {
                if (buf.tryAdvance(action))
                    return true;
                buf = null;
            }
            hasValue = false;
            this.action = action;
            while (!hasValue && e != null) {
                e = e.next(this);
            }
            this.action = null;
            if (builder != null) {
                buf = builder.build().spliterator();
                builder = null;
            }
            return hasValue;
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            if (buf != null) {
                buf.forEachRemaining(action);
                buf = null;
            }
            IntStreamEx.IntEmitter e = this.e;
            this.e = null;
            while (e != null)
                e = e.next(action);
        }

        @Override
        public void accept(int t) {
            if (hasValue) {
                if (builder == null) {
                    builder = IntStream.builder();
                }
                builder.accept(t);
            } else {
                action.accept(t);
                hasValue = true;
            }
        }
    }

    static final class OfLong extends Spliterators.AbstractLongSpliterator implements LongConsumer {
        LongStreamEx.LongEmitter e;
        Spliterator.OfLong buf;
        boolean hasValue;
        LongConsumer action;
        LongStream.Builder builder;

        OfLong(LongStreamEx.LongEmitter e) {
            super(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL);
            this.e = e;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            if (buf != null) {
                if (buf.tryAdvance(action))
                    return true;
                buf = null;
            }
            hasValue = false;
            this.action = action;
            while (!hasValue && e != null) {
                e = e.next(this);
            }
            this.action = null;
            if (builder != null) {
                buf = builder.build().spliterator();
                builder = null;
            }
            return hasValue;
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            if (buf != null) {
                buf.forEachRemaining(action);
                buf = null;
            }
            LongStreamEx.LongEmitter e = this.e;
            this.e = null;
            while (e != null)
                e = e.next(action);
        }

        @Override
        public void accept(long t) {
            if (hasValue) {
                if (builder == null) {
                    builder = LongStream.builder();
                }
                builder.accept(t);
            } else {
                action.accept(t);
                hasValue = true;
            }
        }
    }

    static final class OfDouble extends Spliterators.AbstractDoubleSpliterator implements DoubleConsumer {
        DoubleStreamEx.DoubleEmitter e;
        Spliterator.OfDouble buf;
        boolean hasValue;
        DoubleConsumer action;
        DoubleStream.Builder builder;

        OfDouble(DoubleStreamEx.DoubleEmitter e) {
            super(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL);
            this.e = e;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if (buf != null) {
                if (buf.tryAdvance(action))
                    return true;
                buf = null;
            }
            hasValue = false;
            this.action = action;
            while (!hasValue && e != null) {
                e = e.next(this);
            }
            this.action = null;
            if (builder != null) {
                buf = builder.build().spliterator();
                builder = null;
            }
            return hasValue;
        }

        @Override
        public void forEachRemaining(DoubleConsumer action) {
            if (buf != null) {
                buf.forEachRemaining(action);
                buf = null;
            }
            DoubleStreamEx.DoubleEmitter e = this.e;
            this.e = null;
            while (e != null)
                e = e.next(action);
        }

        @Override
        public void accept(double t) {
            if (hasValue) {
                if (builder == null) {
                    builder = DoubleStream.builder();
                }
                builder.accept(t);
            } else {
                action.accept(t);
                hasValue = true;
            }
        }
    }
}