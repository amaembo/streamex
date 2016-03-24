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
    int vals;
    Consumer<? super T> cons;

    EmitterSpliterator(StreamEx.Emitter<T> e) {
        super(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.IMMUTABLE);
        this.e = e;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (buf != null) {
            if (buf.tryAdvance(action))
                return true;
            buf = null;
        }
        cons = action;
        for (vals = 0; vals == 0; e = e.next(this)) {
            if (e == null)
                return false;
        }
        if (vals > 1) {
            buf = ((Stream.Builder<T>) cons).build().spliterator();
        }
        cons = null;
        return true;
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
        if ((vals += vals < 3 ? 1 : 0) == 2) {
            cons = Stream.builder();
        }
        cons.accept(t);
    }

    static final class OfInt extends Spliterators.AbstractIntSpliterator implements IntConsumer {
        IntStreamEx.IntEmitter e;
        Spliterator.OfInt buf;
        int vals;
        IntConsumer cons;

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
            cons = action;
            for (vals = 0; vals == 0; e = e.next(this)) {
                if (e == null)
                    return false;
            }
            if (vals > 1) {
                buf = ((IntStream.Builder) cons).build().spliterator();
            }
            cons = null;
            return true;
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
            if ((vals += vals < 3 ? 1 : 0) == 2) {
                cons = IntStream.builder();
            }
            cons.accept(t);
        }
    }

    static final class OfLong extends Spliterators.AbstractLongSpliterator implements LongConsumer {
        LongStreamEx.LongEmitter e;
        Spliterator.OfLong buf;
        int vals;
        LongConsumer cons;

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
            cons = action;
            for (vals = 0; vals == 0; e = e.next(this)) {
                if (e == null)
                    return false;
            }
            if (vals > 1) {
                buf = ((LongStream.Builder) cons).build().spliterator();
            }
            cons = null;
            return true;
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
            if ((vals += vals < 3 ? 1 : 0) == 2) {
                cons = LongStream.builder();
            }
            cons.accept(t);
        }
    }

    static final class OfDouble extends Spliterators.AbstractDoubleSpliterator implements DoubleConsumer {
        DoubleStreamEx.DoubleEmitter e;
        Spliterator.OfDouble buf;
        int vals;
        DoubleConsumer cons;

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
            cons = action;
            for (vals = 0; vals == 0; e = e.next(this)) {
                if (e == null)
                    return false;
            }
            if (vals > 1) {
                buf = ((DoubleStream.Builder) cons).build().spliterator();
            }
            cons = null;
            return true;
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
            if ((vals += vals < 3 ? 1 : 0) == 2) {
                cons = DoubleStream.builder();
            }
            cons.accept(t);
        }
    }
}