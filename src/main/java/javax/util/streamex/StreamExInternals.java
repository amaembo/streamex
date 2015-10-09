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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators.AbstractDoubleSpliterator;
import java.util.Spliterators.AbstractIntSpliterator;
import java.util.Spliterators.AbstractLongSpliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/* package */final class StreamExInternals {
    static final boolean IS_JDK9 = System.getProperty("java.version", "").compareTo("1.9") >= 0;
    static final MethodHandle[][] JDK9_METHODS = IS_JDK9 ? initJdk9Methods() : null;
    static final int INITIAL_SIZE = 128;
    static final Function<int[], Integer> UNBOX_INT = box -> box[0];
    static final Function<long[], Long> UNBOX_LONG = box -> box[0];
    static final Function<double[], Double> UNBOX_DOUBLE = box -> box[0];
    static final Object NONE = new Object();
    static final Set<Characteristics> NO_CHARACTERISTICS = EnumSet.noneOf(Characteristics.class);
    static final Set<Characteristics> UNORDERED_CHARACTERISTICS = EnumSet.of(Characteristics.UNORDERED);
    static final Set<Characteristics> UNORDERED_ID_CHARACTERISTICS = EnumSet.of(Characteristics.UNORDERED,
        Characteristics.IDENTITY_FINISH);
    static final Set<Characteristics> ID_CHARACTERISTICS = EnumSet.of(Characteristics.IDENTITY_FINISH);
    static final int IDX_STREAM = 0;
    static final int IDX_INT_STREAM = 1;
    static final int IDX_LONG_STREAM = 2;
    static final int IDX_DOUBLE_STREAM = 3;
    static final int IDX_TAKE_WHILE = 0;
    static final int IDX_DROP_WHILE = 1;

    static MethodHandle[][] initJdk9Methods() {
        Lookup lookup = MethodHandles.publicLookup();
        MethodType[] types = { MethodType.methodType(Stream.class, Predicate.class),
                MethodType.methodType(IntStream.class, IntPredicate.class),
                MethodType.methodType(LongStream.class, LongPredicate.class),
                MethodType.methodType(DoubleStream.class, DoublePredicate.class) };
        MethodHandle[][] methods = new MethodHandle[types.length][];
        try {
            int i = 0;
            for (MethodType type : types) {
                methods[i++] = new MethodHandle[] { lookup.findVirtual(type.returnType(), "takeWhile", type),
                        lookup.findVirtual(type.returnType(), "dropWhile", type) };
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
        return methods;
    }

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

    static final class BooleanMap<T> extends AbstractMap<Boolean, T> {
        T trueValue, falseValue;

        BooleanMap(T trueValue, T falseValue) {
            this.trueValue = trueValue;
            this.falseValue = falseValue;
        }

        @Override
        public boolean containsKey(Object key) {
            return key instanceof Boolean;
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
                    return Arrays.<Map.Entry<Boolean, T>> asList(new SimpleEntry<>(Boolean.TRUE, trueValue),
                        new SimpleEntry<>(Boolean.FALSE, falseValue)).iterator();
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

        @SuppressWarnings({ "unchecked", "rawtypes" })
        static <A, R> PartialCollector<BooleanMap<A>, Map<Boolean, R>> partialCollector(Collector<?, A, R> downstream) {
            Supplier<A> downstreamSupplier = downstream.supplier();
            Supplier<BooleanMap<A>> supplier = () -> new BooleanMap<>(downstreamSupplier.get(),
                    downstreamSupplier.get());
            BinaryOperator<A> downstreamCombiner = downstream.combiner();
            BiConsumer<BooleanMap<A>, BooleanMap<A>> merger = (left, right) -> {
                left.trueValue = downstreamCombiner.apply(left.trueValue, right.trueValue);
                left.falseValue = downstreamCombiner.apply(left.falseValue, right.falseValue);
            };
            if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
                return (PartialCollector) new PartialCollector<>(supplier, merger, Function.identity(),
                        ID_CHARACTERISTICS);
            } else {
                Function<A, R> downstreamFinisher = downstream.finisher();
                return new PartialCollector<>(supplier, merger, par -> new BooleanMap<>(
                        downstreamFinisher.apply(par.trueValue), downstreamFinisher.apply(par.falseValue)),
                        NO_CHARACTERISTICS);
            }
        }
    }

    abstract static class BaseCollector<T, A, R> implements MergingCollector<T, A, R> {
        final Supplier<A> supplier;
        final BiConsumer<A, A> merger;
        final Function<A, R> finisher;
        final Set<Characteristics> characteristics;

        BaseCollector(Supplier<A> supplier, BiConsumer<A, A> merger, Function<A, R> finisher,
                Set<Characteristics> characteristics) {
            this.supplier = supplier;
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
        public BiConsumer<A, A> merger() {
            return merger;
        }

    }

    static final class PartialCollector<A, R> extends BaseCollector<Object, A, R> {
        PartialCollector(Supplier<A> supplier, BiConsumer<A, A> merger, Function<A, R> finisher,
                Set<Characteristics> characteristics) {
            super(supplier, merger, finisher, characteristics);
        }

        @Override
        public BiConsumer<A, Object> accumulator() {
            throw new InternalError();
        }

        IntCollector<A, R> asInt(ObjIntConsumer<A> intAccumulator) {
            return new IntCollectorImpl<>(supplier, intAccumulator, merger, finisher, characteristics);
        }

        LongCollector<A, R> asLong(ObjLongConsumer<A> longAccumulator) {
            return new LongCollectorImpl<>(supplier, longAccumulator, merger, finisher, characteristics);
        }

        DoubleCollector<A, R> asDouble(ObjDoubleConsumer<A> doubleAccumulator) {
            return new DoubleCollectorImpl<>(supplier, doubleAccumulator, merger, finisher, characteristics);
        }

        <T> Collector<T, A, R> asRef(BiConsumer<A, T> accumulator) {
            return Collector.of(supplier, accumulator, combiner(), finisher,
                characteristics.toArray(new Characteristics[characteristics.size()]));
        }

        <T> CancellableCollector<T, A, R> asCancellable(BiConsumer<A, T> accumulator, Predicate<A> finished) {
            return new CancellableCollectorImpl<>(supplier, accumulator, combiner(), finisher, finished,
                    characteristics);
        }

        static PartialCollector<int[], Integer> intSum() {
            return new PartialCollector<>(() -> new int[1], (box1, box2) -> box1[0] += box2[0], UNBOX_INT,
                    NO_CHARACTERISTICS);
        }

        static PartialCollector<long[], Long> longSum() {
            return new PartialCollector<>(() -> new long[1], (box1, box2) -> box1[0] += box2[0], UNBOX_LONG,
                    NO_CHARACTERISTICS);
        }

        static PartialCollector<ObjIntBox<BitSet>, boolean[]> booleanArray() {
            return new PartialCollector<>(() -> new ObjIntBox<>(new BitSet(), 0), (box1, box2) -> {
                box2.a.stream().forEach(i -> box1.a.set(i + box1.b));
                box1.b = StrictMath.addExact(box1.b, box2.b);
            }, box -> {
                boolean[] res = new boolean[box.b];
                box.a.stream().forEach(i -> res[i] = true);
                return res;
            }, NO_CHARACTERISTICS);
        }

        @SuppressWarnings("unchecked")
        static <K, D, A, M extends Map<K, D>> PartialCollector<Map<K, A>, M> grouping(Supplier<M> mapFactory,
                Collector<?, A, D> downstream) {
            BinaryOperator<A> downstreamMerger = downstream.combiner();
            BiConsumer<Map<K, A>, Map<K, A>> merger = (map1, map2) -> {
                for (Map.Entry<K, A> e : map2.entrySet())
                    map1.merge(e.getKey(), e.getValue(), downstreamMerger);
            };

            if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
                return (PartialCollector<Map<K, A>, M>) new PartialCollector<>((Supplier<Map<K, A>>) mapFactory,
                        merger, Function.identity(), ID_CHARACTERISTICS);
            } else {
                Function<A, D> downstreamFinisher = downstream.finisher();
                return new PartialCollector<>((Supplier<Map<K, A>>) mapFactory, merger, map -> {
                    map.replaceAll((k, v) -> ((Function<A, A>) downstreamFinisher).apply(v));
                    return (M) map;
                }, NO_CHARACTERISTICS);
            }
        }

        static PartialCollector<StringBuilder, String> joining(CharSequence delimiter, CharSequence prefix,
                CharSequence suffix, boolean hasPS) {
            BiConsumer<StringBuilder, StringBuilder> merger = (sb1, sb2) -> {
                if (sb2.length() > 0) {
                    if (sb1.length() > 0)
                        sb1.append(delimiter);
                    sb1.append(sb2);
                }
            };
            Supplier<StringBuilder> supplier = StringBuilder::new;
            if (hasPS)
                return new PartialCollector<>(supplier, merger, sb -> new StringBuilder().append(prefix).append(sb)
                        .append(suffix).toString(), NO_CHARACTERISTICS);
            return new PartialCollector<>(supplier, merger, StringBuilder::toString, NO_CHARACTERISTICS);
        }
    }

    static final class CancellableCollectorImpl<T, A, R> implements CancellableCollector<T, A, R> {
        private final Supplier<A> supplier;
        private final BiConsumer<A, T> accumulator;
        private final BinaryOperator<A> combiner;
        private final Function<A, R> finisher;
        private final Predicate<A> finished;
        private final Set<Characteristics> characteristics;

        public CancellableCollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner,
                Function<A, R> finisher, Predicate<A> finished,
                Set<java.util.stream.Collector.Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.finished = finished;
            this.characteristics = characteristics;
        }

        @Override
        public Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }

        @Override
        public BinaryOperator<A> combiner() {
            return combiner;
        }

        @Override
        public Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }

        @Override
        public Predicate<A> finished() {
            return finished;
        }
    }

    static final class IntCollectorImpl<A, R> extends BaseCollector<Integer, A, R> implements IntCollector<A, R> {
        private final ObjIntConsumer<A> intAccumulator;

        public IntCollectorImpl(Supplier<A> supplier, ObjIntConsumer<A> intAccumulator, BiConsumer<A, A> merger,
                Function<A, R> finisher, Set<Characteristics> characteristics) {
            super(supplier, merger, finisher, characteristics);
            this.intAccumulator = intAccumulator;
        }

        @Override
        public ObjIntConsumer<A> intAccumulator() {
            return intAccumulator;
        }
    }

    static final class LongCollectorImpl<A, R> extends BaseCollector<Long, A, R> implements LongCollector<A, R> {
        private final ObjLongConsumer<A> longAccumulator;

        public LongCollectorImpl(Supplier<A> supplier, ObjLongConsumer<A> longAccumulator, BiConsumer<A, A> merger,
                Function<A, R> finisher, Set<Characteristics> characteristics) {
            super(supplier, merger, finisher, characteristics);
            this.longAccumulator = longAccumulator;
        }

        @Override
        public ObjLongConsumer<A> longAccumulator() {
            return longAccumulator;
        }
    }

    static final class DoubleCollectorImpl<A, R> extends BaseCollector<Double, A, R> implements DoubleCollector<A, R> {
        private final ObjDoubleConsumer<A> doubleAccumulator;

        public DoubleCollectorImpl(Supplier<A> supplier, ObjDoubleConsumer<A> doubleAccumulator,
                BiConsumer<A, A> merger, Function<A, R> finisher, Set<Characteristics> characteristics) {
            super(supplier, merger, finisher, characteristics);
            this.doubleAccumulator = doubleAccumulator;
        }

        @Override
        public ObjDoubleConsumer<A> doubleAccumulator() {
            return doubleAccumulator;
        }
    }

    static class Box<A> {
        A a;

        Box(A obj) {
            this.a = obj;
        }

        static <A, R> PartialCollector<Box<A>, R> partialCollector(Collector<?, A, R> c) {
            Supplier<A> supplier = c.supplier();
            BinaryOperator<A> combiner = c.combiner();
            Function<A, R> finisher = c.finisher();
            return new PartialCollector<>(() -> new Box<>(supplier.get()), (box1, box2) -> box1.a = combiner.apply(
                box1.a, box2.a), box -> finisher.apply(box.a), NO_CHARACTERISTICS);
        }

        static <A> Optional<A> asOptional(Box<A> box) {
            return box == null ? Optional.empty() : Optional.of(box.a);
        }
    }

    static final class PairBox<A, B> extends Box<A> {
        B b;

        PairBox(A a, B b) {
            super(a);
            this.b = b;
        }

        static <T> PairBox<T, T> single(T a) {
            return new PairBox<>(a, a);
        }

        @Override
        public int hashCode() {
            return b == null ? 0 : b.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != PairBox.class)
                return false;
            return Objects.equals(b, ((PairBox<?, ?>) obj).b);
        }
    }

    static final class ObjIntBox<A> extends Box<A> implements Entry<Integer, A> {
        int b;

        ObjIntBox(A a, int b) {
            super(a);
            this.b = b;
        }

        @Override
        public Integer getKey() {
            return b;
        }

        @Override
        public A getValue() {
            return a;
        }

        @Override
        public A setValue(A value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(b) ^ (a == null ? 0 : a.hashCode());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return getKey().equals(e.getKey()) && Objects.equals(a, e.getValue());
        }

        @Override
        public String toString() {
            return b + "=" + a;
        }
    }

    static final class ObjLongBox<A> extends Box<A> implements Entry<A, Long> {
        long b;

        ObjLongBox(A a, long b) {
            super(a);
            this.b = b;
        }

        @Override
        public A getKey() {
            return a;
        }

        @Override
        public Long getValue() {
            return b;
        }

        @Override
        public Long setValue(Long value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return Long.hashCode(b) ^ (a == null ? 0 : a.hashCode());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return getValue().equals(e.getValue()) && Objects.equals(a, e.getKey());
        }

        @Override
        public String toString() {
            return a + "=" + b;
        }
    }

    static final class ObjDoubleBox<A> extends Box<A> {
        double b;

        ObjDoubleBox(A a, double b) {
            super(a);
            this.b = b;
        }
    }

    static final class PrimitiveBox {
        int i;
        double d;
        long l;
        boolean b;

        OptionalInt asInt() {
            return b ? OptionalInt.of(i) : OptionalInt.empty();
        }

        OptionalLong asLong() {
            return b ? OptionalLong.of(l) : OptionalLong.empty();
        }

        OptionalDouble asDouble() {
            return b ? OptionalDouble.of(d) : OptionalDouble.empty();
        }

        static final BiConsumer<PrimitiveBox, PrimitiveBox> MAX_LONG = (box1, box2) -> {
            if (box2.b && (!box1.b || box1.l < box2.l)) {
                box1.from(box2);
            }
        };

        static final BiConsumer<PrimitiveBox, PrimitiveBox> MIN_LONG = (box1, box2) -> {
            if (box2.b && (!box1.b || box1.l > box2.l)) {
                box1.from(box2);
            }
        };

        static final BiConsumer<PrimitiveBox, PrimitiveBox> MAX_INT = (box1, box2) -> {
            if (box2.b && (!box1.b || box1.i < box2.i)) {
                box1.from(box2);
            }
        };

        static final BiConsumer<PrimitiveBox, PrimitiveBox> MIN_INT = (box1, box2) -> {
            if (box2.b && (!box1.b || box1.i > box2.i)) {
                box1.from(box2);
            }
        };

        static final BiConsumer<PrimitiveBox, PrimitiveBox> MAX_DOUBLE = (box1, box2) -> {
            if (box2.b && (!box1.b || Double.compare(box1.d, box2.d) < 0)) {
                box1.from(box2);
            }
        };

        static final BiConsumer<PrimitiveBox, PrimitiveBox> MIN_DOUBLE = (box1, box2) -> {
            if (box2.b && (!box1.b || Double.compare(box1.d, box2.d) > 0)) {
                box1.from(box2);
            }
        };

        public void from(PrimitiveBox box) {
            b = box.b;
            i = box.i;
            d = box.d;
            l = box.l;
        }
    }

    static final class TDOfRef<T> extends AbstractSpliterator<T> implements Consumer<T> {
        private final Predicate<? super T> predicate;
        private final boolean drop;
        private boolean checked;
        private final Spliterator<T> source;
        private T cur;

        TDOfRef(Spliterator<T> source, boolean drop, Predicate<? super T> predicate) {
            super(source.estimateSize(), source.characteristics()
                & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.source = source;
        }

        @Override
        public Comparator<? super T> getComparator() {
            return source.getComparator();
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (drop) {
                if (checked)
                    return source.tryAdvance(action);
                while (source.tryAdvance(this)) {
                    if (!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if (!checked && source.tryAdvance(this) && predicate.test(cur)) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }

        @Override
        public void accept(T t) {
            this.cur = t;
        }
    }

    static final class TDOfInt extends AbstractIntSpliterator implements IntConsumer {
        private final IntPredicate predicate;
        private final boolean drop;
        private boolean checked;
        private final Spliterator.OfInt source;
        private int cur;

        TDOfInt(Spliterator.OfInt source, boolean drop, IntPredicate predicate) {
            super(source.estimateSize(), source.characteristics()
                & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.source = source;
        }

        @Override
        public Comparator<? super Integer> getComparator() {
            return source.getComparator();
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (drop) {
                if (checked)
                    return source.tryAdvance(action);
                while (source.tryAdvance(this)) {
                    if (!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if (!checked && source.tryAdvance(this) && predicate.test(cur)) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }

        @Override
        public void accept(int t) {
            this.cur = t;
        }
    }

    static final class TDOfLong extends AbstractLongSpliterator implements LongConsumer {
        private final LongPredicate predicate;
        private final boolean drop;
        private boolean checked;
        private final Spliterator.OfLong source;
        private long cur;

        TDOfLong(Spliterator.OfLong source, boolean drop, LongPredicate predicate) {
            super(source.estimateSize(), source.characteristics()
                & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.source = source;
        }

        @Override
        public Comparator<? super Long> getComparator() {
            return source.getComparator();
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            if (drop) {
                if (checked)
                    return source.tryAdvance(action);
                while (source.tryAdvance(this)) {
                    if (!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if (!checked && source.tryAdvance(this) && predicate.test(cur)) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }

        @Override
        public void accept(long t) {
            this.cur = t;
        }
    }

    static final class TDOfDouble extends AbstractDoubleSpliterator implements DoubleConsumer {
        private final DoublePredicate predicate;
        private final boolean drop;
        private boolean checked;
        private final Spliterator.OfDouble source;
        private double cur;

        TDOfDouble(Spliterator.OfDouble source, boolean drop, DoublePredicate predicate) {
            super(source.estimateSize(), source.characteristics()
                & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.source = source;
        }

        @Override
        public Comparator<? super Double> getComparator() {
            return source.getComparator();
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if (drop) {
                if (checked)
                    return source.tryAdvance(action);
                while (source.tryAdvance(this)) {
                    if (!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if (!checked && source.tryAdvance(this) && predicate.test(cur)) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }

        @Override
        public void accept(double t) {
            this.cur = t;
        }
    }

    static final class AverageLong {
        long hi, lo, cnt;

        public void accept(long val) {
            cnt++;
            int cmp = Long.compareUnsigned(lo, lo += val);
            if (val > 0) {
                if (cmp > 0)
                    hi++;
            } else if (cmp < 0)
                hi--;
        }

        public AverageLong combine(AverageLong other) {
            cnt += other.cnt;
            hi += other.hi;
            if (Long.compareUnsigned(lo, lo += other.lo) > 0) {
                hi++;
            }
            return this;
        }

        public OptionalDouble result() {
            if (cnt == 0)
                return OptionalDouble.empty();
            if (hi == 0 && lo >= 0 || hi == -1 && lo < 0) {
                return OptionalDouble.of(((double) lo) / cnt);
            }
            return OptionalDouble.of(new BigDecimal(new BigInteger(java.nio.ByteBuffer.allocate(16)
                    .order(ByteOrder.BIG_ENDIAN).putLong(hi).putLong(lo).array())).divide(BigDecimal.valueOf(cnt),
                MathContext.DECIMAL64).doubleValue());
        }
    }

    @SuppressWarnings("serial")
    static class CancelException extends Error {
        CancelException() {
            // Calling this constructor makes the Exception construction much
            // faster (like 0.3us vs 1.7us)
            super(null, null, false, false);
        }
    }

    static ObjIntConsumer<StringBuilder> joinAccumulatorInt(CharSequence delimiter) {
        return (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i);
    }

    static ObjLongConsumer<StringBuilder> joinAccumulatorLong(CharSequence delimiter) {
        return (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i);
    }

    static ObjDoubleConsumer<StringBuilder> joinAccumulatorDouble(CharSequence delimiter) {
        return (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i);
    }

    static <V> BinaryOperator<V> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    static <T> BinaryOperator<T> selectFirst() {
        return (u, v) -> u;
    }

    static int checkLength(int a, int b) {
        if (a != b)
            throw new IllegalArgumentException("Length differs: " + a + " != " + b);
        return a;
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

    @SuppressWarnings("unchecked")
    static <T> T none() {
        return (T) NONE;
    }
}
