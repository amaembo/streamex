package javax.util.streamex;

import java.util.EnumSet;
import java.util.IntSummaryStatistics;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

import javax.util.streamex.Buffers.ByteBuffer;
import javax.util.streamex.Buffers.CharBuffer;
import javax.util.streamex.Buffers.IntBuffer;
import javax.util.streamex.Buffers.Partition;
import javax.util.streamex.Buffers.ShortBuffer;

public interface IntCollector<A, R> extends Collector<Integer, A, R> {
    /**
     * A function that folds a value into a mutable result container.
     *
     * @return a function which folds a value into a mutable result container
     */
    ObjIntConsumer<A> intAccumulator();

    BiConsumer<A, A> merger();

    @Override
    default BiConsumer<A, Integer> accumulator() {
        ObjIntConsumer<A> intAccumulator = intAccumulator();
        return (a, i) -> intAccumulator.accept(a, i);
    }

    @Override
    default BinaryOperator<A> combiner() {
        BiConsumer<A, A> merger = merger();
        return (a, b) -> {
            merger.accept(a, b);
            return a;
        };
    }

    static <R> IntCollector<R, R> of(Supplier<R> supplier, ObjIntConsumer<R> intAccumulator, BiConsumer<R, R> merger) {
        return new IntCollector<R, R>() {

            @Override
            public Supplier<R> supplier() {
                return supplier;
            }

            @Override
            public Function<R, R> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Collector.Characteristics> characteristics() {
                return EnumSet.of(Collector.Characteristics.IDENTITY_FINISH);
            }

            @Override
            public ObjIntConsumer<R> intAccumulator() {
                return intAccumulator;
            }

            @Override
            public BiConsumer<R, R> merger() {
                return merger;
            }
        };
    }
    
    static <A, R> IntCollector<?, R> of(Collector<Integer, A, R> collector) {
        if(collector instanceof IntCollector) {
            return (IntCollector<A, R>) collector;
        }
        return mappingToObj(i -> i, collector);
    }

    static <A, R> IntCollector<A, R> of(Supplier<A> supplier, ObjIntConsumer<A> intAccumulator,
            BiConsumer<A, A> merger, Function<A, R> finisher) {
        return new IntCollector<A, R>() {

            @Override
            public Supplier<A> supplier() {
                return supplier;
            }

            @Override
            public Function<A, R> finisher() {
                return finisher;
            }

            @Override
            public Set<Collector.Characteristics> characteristics() {
                return EnumSet.noneOf(Collector.Characteristics.class);
            }

            @Override
            public ObjIntConsumer<A> intAccumulator() {
                return intAccumulator;
            }

            @Override
            public BiConsumer<A, A> merger() {
                return merger;
            }
        };
    }

    public static IntCollector<?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                (sb1, sb2) -> {
                    if (sb2.length() > 0) {
                        if (sb1.length() > 0)
                            sb1.append(delimiter);
                        sb1.append(sb2);
                    }
                }, sb -> new StringBuilder().append(prefix).append(sb).append(suffix).toString());
    }

    public static IntCollector<?, String> joining(CharSequence delimiter) {
        return of(StringBuilder::new, (sb, i) -> (sb.length() > 0 ? sb.append(delimiter) : sb).append(i),
                (sb1, sb2) -> {
                    if (sb2.length() > 0) {
                        if (sb1.length() > 0)
                            sb1.append(delimiter);
                        sb1.append(sb2);
                    }
                }, StringBuilder::toString);
    }

    public static IntCollector<?, Long> counting() {
        return of(() -> new long[1], (box, i) -> box[0]++, (box1, box2) -> box1[0] += box2[0], box -> box[0]);
    }

    public static IntCollector<?, Integer> summing() {
        return of(() -> new int[1], (box, i) -> box[0] += i, (box1, box2) -> box1[0] += box2[0], box -> box[0]);
    }

    public static IntCollector<?, OptionalInt> min() {
        return reducing((a, b) -> a > b ? b : a);
    }

    public static IntCollector<?, OptionalInt> max() {
        return reducing((a, b) -> a > b ? a : b);
    }

    @SuppressWarnings("unchecked")
    public static <A, R> IntCollector<?, R> mapping(IntUnaryOperator mapper, IntCollector<A, R> downstream) {
        ObjIntConsumer<A> downstreamAccumulator = downstream.intAccumulator();
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH))
            return (IntCollector<?, R>) of(downstream.supplier(),
                    (r, t) -> downstreamAccumulator.accept(r, mapper.applyAsInt(t)), downstream.merger());
        return of(downstream.supplier(), (r, t) -> downstreamAccumulator.accept(r, mapper.applyAsInt(t)),
                downstream.merger(), downstream.finisher());
    }

    @SuppressWarnings("unchecked")
    public static <U, A, R> IntCollector<?, R> mappingToObj(IntFunction<U> mapper, Collector<U, A, R> downstream) {
        Supplier<A> supplier = downstream.supplier();
        BiConsumer<A, U> accumulator = downstream.accumulator();
        BinaryOperator<A> combiner = downstream.combiner();
        Function<A, R> finisher = downstream.finisher();
        return of(() -> new Object[] { supplier.get() }, (box, i) -> accumulator.accept((A) box[0], mapper.apply(i)), (
                box1, box2) -> box1[0] = combiner.apply((A) box1[0], (A) box2[0]), box -> finisher.apply((A) box[0]));
    }
    
    public static <A, R, RR> IntCollector<A, RR> collectingAndThen(IntCollector<A, R> collector, Function<R, RR> finisher) {
        return of(collector.supplier(), collector.intAccumulator(), collector.merger(),
                collector.finisher().andThen(finisher));
    }

    public static IntCollector<?, OptionalInt> reducing(IntBinaryOperator op) {
        return of(() -> new int[2], (box, i) -> {
            if (box[1] == 0) {
                box[0] = i;
                box[1] = 1;
            } else {
                box[0] = op.applyAsInt(box[0], i);
            }
        }, (box1, box2) -> {
            if (box2[1] == 1) {
                if (box1[1] == 0) {
                    box1[0] = box2[0];
                    box1[1] = 1;
                } else {
                    box1[0] = op.applyAsInt(box1[0], box2[0]);
                }
            }
        }, box -> box[1] == 1 ? OptionalInt.of(box[0]) : OptionalInt.empty());
    }

    public static IntCollector<?, Integer> reducing(int identity, IntBinaryOperator op) {
        return of(() -> new int[] { identity }, (box, i) -> box[0] = op.applyAsInt(box[0], i),
                (box1, box2) -> box1[0] = op.applyAsInt(box1[0], box2[0]), box -> box[0]);
    }

    public static IntCollector<?, IntSummaryStatistics> summarizing() {
        return of(IntSummaryStatistics::new, IntSummaryStatistics::accept, IntSummaryStatistics::combine);
    }
    
    public static IntCollector<?, Map<Boolean, int[]>> partitioningBy(IntPredicate predicate) {
        return partitioningBy(predicate, toArray());
    }

    public static <A, D> IntCollector<?, Map<Boolean, D>> partitioningBy(IntPredicate predicate, IntCollector<A, D> downstream) {
        ObjIntConsumer<A> downstreamAccumulator = downstream.intAccumulator();
        ObjIntConsumer<Partition<A>> accumulator = (result, t) ->
                downstreamAccumulator.accept(predicate.test(t) ? result.forTrue : result.forFalse, t);
        BiConsumer<A, A> op = downstream.merger();
        BiConsumer<Partition<A>, Partition<A>> merger = (left, right) -> {
                op.accept(left.forTrue, right.forTrue);
                op.accept(left.forFalse, right.forFalse);
        };
        Supplier<Partition<A>> supplier = () ->
                new Partition<>(downstream.supplier().get(),
                                downstream.supplier().get());
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            IntCollector<?, Map<Boolean, D>> result = (IntCollector)of(supplier, accumulator, merger);
            return result;
        }
        else {
            Function<Partition<A>, Map<Boolean, D>> finisher = par ->
                    new Partition<>(downstream.finisher().apply(par.forTrue),
                                    downstream.finisher().apply(par.forFalse));
            return of(supplier, accumulator, merger, finisher);
        }
    }

    public static IntCollector<?, int[]> toArray() {
        return of(IntBuffer::new, IntBuffer::add, IntBuffer::addAll, IntBuffer::toArray);
    }

    public static IntCollector<?, byte[]> toByteArray() {
        return of(ByteBuffer::new, ByteBuffer::add, ByteBuffer::addAll, ByteBuffer::toArray);
    }
    
    public static IntCollector<?, char[]> toCharArray() {
        return of(CharBuffer::new, CharBuffer::add, CharBuffer::addAll, CharBuffer::toArray);
    }

    public static IntCollector<?, short[]> toShortArray() {
        return of(ShortBuffer::new, ShortBuffer::add, ShortBuffer::addAll, ShortBuffer::toArray);
    }

}
