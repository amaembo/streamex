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
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.Map.Entry;
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
import java.util.stream.Collector.Characteristics;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/* package */final class StreamExInternals {
    static final boolean IS_JDK9 = System.getProperty("java.version", "").compareTo("1.9") >= 0;
    static final int INITIAL_SIZE = 128;
    static final Supplier<long[]> LONG_BOX = () -> new long[1];
    static final Supplier<int[]> INT_BOX = () -> new int[1];
    static final Function<int[], Integer> UNBOX_INT = box -> box[0];
    static final Function<long[], Long> UNBOX_LONG = box -> box[0];
    static final Function<double[], Double> UNBOX_DOUBLE = box -> box[0];
    static final BiConsumer<long[], long[]> SUM_LONG = (box1, box2) -> box1[0] += box2[0];
    static final BiConsumer<int[], int[]> SUM_INT = (box1, box2) -> box1[0] += box2[0];
    static final Object NONE = new Object();
    static final Set<Characteristics> NO_CHARACTERISTICS = EnumSet.noneOf(Characteristics.class);
    static final Set<Characteristics> ID_CHARACTERISTICS = EnumSet.of(Characteristics.IDENTITY_FINISH);
    static final MethodHandle[][] JDK9_METHODS;
    static final int IDX_STREAM = 0;
    static final int IDX_INT_STREAM = 1;
    static final int IDX_LONG_STREAM = 2;
    static final int IDX_DOUBLE_STREAM = 3;
    static final int IDX_TAKE_WHILE = 0;
    static final int IDX_DROP_WHILE = 1;

    static {
        Lookup lookup = MethodHandles.publicLookup();
        MethodType[] types = {MethodType.methodType(Stream.class, Predicate.class),
                MethodType.methodType(IntStream.class, IntPredicate.class),
                MethodType.methodType(LongStream.class, LongPredicate.class),
                MethodType.methodType(DoubleStream.class, DoublePredicate.class)};
        JDK9_METHODS = new MethodHandle[types.length][];
        try {
            int i=0;
            for(MethodType type : types) {
                JDK9_METHODS[i++] = new MethodHandle[] {
                        lookup.findVirtual(type.returnType(), "takeWhile", type),
                        lookup.findVirtual(type.returnType(), "dropWhile", type)
                };
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // ignore
        }
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
        final T trueValue, falseValue;

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
    
    private static abstract class BaseCollector<T, A, R> implements MergingCollector<T, A, R> {
        private final Supplier<A> supplier;
        private final BiConsumer<A, A> merger;
        private final Function<A, R> finisher;
        private final Set<Characteristics> characteristics;

        public BaseCollector(Supplier<A> supplier, BiConsumer<A, A> merger,
                Function<A, R> finisher, Set<Characteristics> characteristics) {
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

        static <A> Supplier<Box<A>> supplier(Supplier<A> supplier) {
            return () -> new Box<>(supplier.get());
        }

        static <A> BiConsumer<Box<A>, Box<A>> combiner(BinaryOperator<A> combiner) {
            return (box1, box2) -> box1.a = combiner.apply(box1.a, box2.a);
        }

        static <A, R> Function<Box<A>, R> finisher(Function<A, R> finisher) {
            return box -> finisher.apply(box.a);
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
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
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
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
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
            super(source.estimateSize(), source.characteristics() & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
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
            if(drop) {
                if(checked)
                    return source.tryAdvance(action);
                while(source.tryAdvance(this)) {
                    if(!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if(!checked && source.tryAdvance(this) && predicate.test(cur)) {
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
            super(source.estimateSize(), source.characteristics() & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
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
            if(drop) {
                if(checked)
                    return source.tryAdvance(action);
                while(source.tryAdvance(this)) {
                    if(!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if(!checked && source.tryAdvance(this) && predicate.test(cur)) {
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
            super(source.estimateSize(), source.characteristics() & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
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
            if(drop) {
                if(checked)
                    return source.tryAdvance(action);
                while(source.tryAdvance(this)) {
                    if(!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if(!checked && source.tryAdvance(this) && predicate.test(cur)) {
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
            super(source.estimateSize(), source.characteristics() & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
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
            if(drop) {
                if(checked)
                    return source.tryAdvance(action);
                while(source.tryAdvance(this)) {
                    if(!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if(!checked && source.tryAdvance(this) && predicate.test(cur)) {
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

    static ObjIntConsumer<StringBuilder> joinAccumulatorInt(CharSequence delimiter) {
        return (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i);
    }

    static ObjLongConsumer<StringBuilder> joinAccumulatorLong(CharSequence delimiter) {
        return (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i);
    }

    static ObjDoubleConsumer<StringBuilder> joinAccumulatorDouble(CharSequence delimiter) {
        return (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i);
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
        return (T)NONE;
    }
}
