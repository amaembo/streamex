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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Stream;

/* package */final class StreamExInternals {
    static final int INITIAL_SIZE = 128;
    static final Function<int[], Integer> UNBOX_INT = box -> box[0];
    static final Function<long[], Long> UNBOX_LONG = box -> box[0];
    static final BiConsumer<long[], long[]> SUM_LONG = (box1, box2) -> box1[0] += box2[0];
    static final Function<double[], Double> UNBOX_DOUBLE = box -> box[0];
    static final Object NONE = new Object();
    static final Set<Characteristics> NO_CHARACTERISTICS = EnumSet.noneOf(Characteristics.class);
    static final Set<Characteristics> ID_CHARACTERISTICS = EnumSet.of(Characteristics.IDENTITY_FINISH);

    static final class ByteBuffer {
        int size = 0;
        byte[] data;

        ByteBuffer() {
            data = new byte[INITIAL_SIZE];
        }

        ByteBuffer(int size) {
            data = new byte[size];
        }

        void add(int n) {
            if (data.length == size) {
                byte[] newData = new byte[data.length * 2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = (byte) n;
        }

        void addUnsafe(int n) {
            data[size++] = (byte) n;
        }

        void addAll(ByteBuffer buf) {
            if (data.length < buf.size + size) {
                byte[] newData = new byte[buf.size + size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }

        byte[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
    }

    static final class CharBuffer {
        int size = 0;
        char[] data;

        CharBuffer() {
            data = new char[INITIAL_SIZE];
        }

        CharBuffer(int size) {
            data = new char[size];
        }

        void add(int n) {
            if (data.length == size) {
                char[] newData = new char[data.length * 2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = (char) n;
        }

        void addUnsafe(int n) {
            data[size++] = (char) n;
        }

        void addAll(CharBuffer buf) {
            if (data.length < buf.size + size) {
                char[] newData = new char[buf.size + size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }

        char[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
    }

    static final class ShortBuffer {
        int size = 0;
        short[] data;

        ShortBuffer() {
            data = new short[INITIAL_SIZE];
        }

        ShortBuffer(int size) {
            data = new short[size];
        }

        void add(int n) {
            if (data.length == size) {
                short[] newData = new short[data.length * 2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = (short) n;
        }

        void addUnsafe(int n) {
            data[size++] = (short) n;
        }

        void addAll(ShortBuffer buf) {
            if (data.length < buf.size + size) {
                short[] newData = new short[buf.size + size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }

        short[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
    }

    static final class FloatBuffer {
        int size = 0;
        float[] data;

        FloatBuffer() {
            data = new float[INITIAL_SIZE];
        }

        FloatBuffer(int size) {
            data = new float[size];
        }

        void add(double n) {
            if (data.length == size) {
                float[] newData = new float[data.length * 2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = (float) n;
        }

        void addUnsafe(double n) {
            data[size++] = (float) n;
        }

        void addAll(FloatBuffer buf) {
            if (data.length < buf.size + size) {
                float[] newData = new float[buf.size + size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }

        float[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
    }

    static final class IntBuffer {
        int size = 0;
        int[] data;

        IntBuffer() {
            data = new int[INITIAL_SIZE];
        }

        void add(int n) {
            if (data.length == size) {
                int[] newData = new int[data.length * 2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = n;
        }

        void addAll(IntBuffer buf) {
            if (data.length < buf.size + size) {
                int[] newData = new int[buf.size + size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }

        int[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
    }

    static final class LongBuffer {
        int size = 0;
        long[] data;

        LongBuffer() {
            data = new long[INITIAL_SIZE];
        }

        void add(long n) {
            if (data.length == size) {
                long[] newData = new long[data.length * 2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = n;
        }

        void addAll(LongBuffer buf) {
            if (data.length < buf.size + size) {
                long[] newData = new long[buf.size + size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }

        long[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
    }

    static final class DoubleBuffer {
        int size = 0;
        double[] data;

        DoubleBuffer() {
            data = new double[INITIAL_SIZE];
        }

        void add(double n) {
            if (data.length == size) {
                double[] newData = new double[data.length * 2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = n;
        }

        void addAll(DoubleBuffer buf) {
            if (data.length < buf.size + size) {
                double[] newData = new double[buf.size + size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }

        double[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
    }

    static final class BooleanMap<T> extends AbstractMap<Boolean, T> implements Map<Boolean, T> {
        final T trueValue, falseValue;

        BooleanMap(T trueValue, T falseValue) {
            this.trueValue = trueValue;
            this.falseValue = falseValue;
        }

        @Override
        public boolean containsKey(Object key) {
            return Boolean.TRUE.equals(key) || Boolean.FALSE.equals(key);
        }

        @Override
        public T get(Object key) {
            if (Boolean.TRUE.equals(key))
                return trueValue;
            if (Boolean.FALSE.equals(key))
                return falseValue;
            return null;
        }

        @Override
        public Set<Map.Entry<Boolean, T>> entrySet() {
            return new AbstractSet<Map.Entry<Boolean, T>>() {
                @Override
                public Iterator<Map.Entry<Boolean, T>> iterator() {
                    return new Iterator<Map.Entry<Boolean, T>>() {
                        int pos = 0;

                        @Override
                        public boolean hasNext() {
                            return pos < 2;
                        }

                        @Override
                        public java.util.Map.Entry<Boolean, T> next() {
                            switch (pos++) {
                            case 0:
                                return new SimpleEntry<>(true, trueValue);
                            case 1:
                                return new SimpleEntry<>(false, falseValue);
                            default:
                                pos = 2;
                                throw new NoSuchElementException();
                            }
                        }
                    };
                }

                @Override
                public int size() {
                    return 2;
                }
            };
        }

        @Override
        public int size() {
            return 2;
        }

        static <A> Supplier<BooleanMap<A>> supplier(Supplier<A> downstreamSupplier) {
            return () -> new BooleanMap<>(downstreamSupplier.get(), downstreamSupplier.get());
        }

        static <A> BiConsumer<BooleanMap<A>, BooleanMap<A>> merger(BiConsumer<A, A> downstreamMerger) {
            return (left, right) -> {
                downstreamMerger.accept(left.trueValue, right.trueValue);
                downstreamMerger.accept(left.falseValue, right.falseValue);
            };
        }

        static <A, D> Function<BooleanMap<A>, Map<Boolean, D>> finisher(Function<A, D> downstreamFinisher) {
            return par -> new BooleanMap<>(downstreamFinisher.apply(par.trueValue),
                    downstreamFinisher.apply(par.falseValue));
        }
    }

    static final class IntCollectorImpl<A, R> implements IntCollector<A, R> {
        private final Supplier<A> supplier;
        private final ObjIntConsumer<A> intAccumulator;
        private final BiConsumer<A, A> merger;
        private final Function<A, R> finisher;
        private final Set<Characteristics> characteristics;

        public IntCollectorImpl(Supplier<A> supplier, ObjIntConsumer<A> intAccumulator, BiConsumer<A, A> merger,
                Function<A, R> finisher, Set<Characteristics> characteristics) {
            this.supplier = supplier;
            this.intAccumulator = intAccumulator;
            this.merger = merger;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }

        @Override
        public Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public ObjIntConsumer<A> intAccumulator() {
            return intAccumulator;
        }

        @Override
        public BiConsumer<A, A> merger() {
            return merger;
        }
    }

    static final class LongCollectorImpl<A, R> implements LongCollector<A, R> {
        private final Supplier<A> supplier;
        private final ObjLongConsumer<A> longAccumulator;
        private final BiConsumer<A, A> merger;
        private final Function<A, R> finisher;
        private final Set<Characteristics> characteristics;

        public LongCollectorImpl(Supplier<A> supplier, ObjLongConsumer<A> longAccumulator, BiConsumer<A, A> merger,
                Function<A, R> finisher, Set<Characteristics> characteristics) {
            this.supplier = supplier;
            this.longAccumulator = longAccumulator;
            this.merger = merger;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }

        @Override
        public Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public ObjLongConsumer<A> longAccumulator() {
            return longAccumulator;
        }

        @Override
        public BiConsumer<A, A> merger() {
            return merger;
        }
    }

    static final class DoubleCollectorImpl<A, R> implements DoubleCollector<A, R> {
        private final Supplier<A> supplier;
        private final ObjDoubleConsumer<A> doubleAccumulator;
        private final BiConsumer<A, A> merger;
        private final Function<A, R> finisher;
        private final Set<Characteristics> characteristics;

        public DoubleCollectorImpl(Supplier<A> supplier, ObjDoubleConsumer<A> doubleAccumulator,
                BiConsumer<A, A> merger, Function<A, R> finisher, Set<Characteristics> characteristics) {
            this.supplier = supplier;
            this.doubleAccumulator = doubleAccumulator;
            this.merger = merger;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }

        @Override
        public Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public ObjDoubleConsumer<A> doubleAccumulator() {
            return doubleAccumulator;
        }

        @Override
        public BiConsumer<A, A> merger() {
            return merger;
        }
    }
    
    static final class Box<A> {
        A obj;
        
        Box(A obj) {
            this.obj = obj;
        }
        
        Box() {
        }

        static <A> Supplier<Box<A>> supplier(Supplier<A> supplier) {
            return () -> new Box<>(supplier.get());
        }

        static <A> BiConsumer<Box<A>, Box<A>> combiner(BinaryOperator<A> combiner) {
            return (box1, box2) -> box1.obj = combiner.apply(box1.obj, box2.obj);
        }

        static <A, R> Function<Box<A>, R> finisher(Function<A, R> finisher) {
            return box -> finisher.apply(box.obj);
        }
    }
    
    static final class PairBox<A, B> {
        A a;
        B b;
        
        PairBox(A a, B b) {
            this.a = a;
            this.b = b;
        }
    }

    static BiConsumer<StringBuilder, StringBuilder> joinMerger(CharSequence delimiter) {
        return (sb1, sb2) -> {
            if (sb2.length() > 0) {
                if (sb1.length() > 0)
                    sb1.append(delimiter);
                sb1.append(sb2);
            }
        };
    }

    static Function<StringBuilder, String> joinFinisher(CharSequence prefix, CharSequence suffix) {
        return sb -> new StringBuilder().append(prefix).append(sb).append(suffix).toString();
    }

    static <K, A> BiConsumer<Map<K, A>, Map<K, A>> mapMerger(BiConsumer<A, A> downstreamMerger) {
        return (map1, map2) -> {
            for (Map.Entry<K, A> e : map2.entrySet())
                map1.merge(e.getKey(), e.getValue(), (a, b) -> {
                    downstreamMerger.accept(a, b);
                    return a;
                });
        };
    }

    @SuppressWarnings("unchecked")
    static <K, A, D, M extends Map<K, D>> Function<Map<K, A>, M> mapFinisher(Function<A, A> downstreamFinisher) {
        return map -> {
            map.replaceAll((k, v) -> downstreamFinisher.apply(v));
            return (M) map;
        };
    }

    static <V> BinaryOperator<V> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    static IntStreamEx intStreamForLength(int a, int b) {
        if (a != b)
            throw new IllegalArgumentException("Length differs: " + a + " != " + b);
        return IntStreamEx.range(0, a);
    }

    static void rangeCheck(int arrayLength, int startInclusive, int endExclusive) {
        if (startInclusive > endExclusive) {
            throw new ArrayIndexOutOfBoundsException("startInclusive(" + startInclusive + ") > endExclusive("
                    + endExclusive + ")");
        }
        if (startInclusive < 0) {
            throw new ArrayIndexOutOfBoundsException(startInclusive);
        }
        if (endExclusive > arrayLength) {
            throw new ArrayIndexOutOfBoundsException(endExclusive);
        }
    }

    static <T> Stream<T> flatTraverse(Stream<T> src, Function<T, Stream<T>> streamProvider) {
        return src.flatMap(t -> {
            Stream<T> result = streamProvider.apply(t);
            return result == null ? Stream.of(t) : Stream.concat(Stream.of(t), flatTraverse(result, streamProvider));
        });
    }

    @SuppressWarnings("unchecked")
    static <T> Stream<T> unwrap(Stream<T> stream) {
        return stream instanceof AbstractStreamEx ? ((AbstractStreamEx<T, ?>) stream).stream : stream;
    }
}
