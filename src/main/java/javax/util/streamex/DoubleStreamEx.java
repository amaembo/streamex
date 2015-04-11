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

import java.util.Collection;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.PrimitiveIterator.OfDouble;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

public class DoubleStreamEx implements DoubleStream {
    private static final DoubleStreamEx EMPTY = new DoubleStreamEx(DoubleStream.empty());

    private final DoubleStream stream;

    DoubleStreamEx(DoubleStream stream) {
        this.stream = stream;
    }

    @Override
    public boolean isParallel() {
        return stream.isParallel();
    }

    @Override
    public DoubleStreamEx unordered() {
        return new DoubleStreamEx(stream.unordered());
    }

    @Override
    public DoubleStreamEx onClose(Runnable closeHandler) {
        return new DoubleStreamEx(stream.onClose(closeHandler));
    }

    @Override
    public void close() {
        stream.close();
    }

    @Override
    public DoubleStreamEx filter(DoublePredicate predicate) {
        return new DoubleStreamEx(stream.filter(predicate));
    }

    @Override
    public DoubleStreamEx map(DoubleUnaryOperator mapper) {
        return new DoubleStreamEx(stream.map(mapper));
    }

    @Override
    public <U> StreamEx<U> mapToObj(DoubleFunction<? extends U> mapper) {
        return new StreamEx<>(stream.mapToObj(mapper));
    }

    @Override
    public IntStreamEx mapToInt(DoubleToIntFunction mapper) {
        return new IntStreamEx(stream.mapToInt(mapper));
    }

    @Override
    public LongStreamEx mapToLong(DoubleToLongFunction mapper) {
        return new LongStreamEx(stream.mapToLong(mapper));
    }

    @Override
    public DoubleStreamEx flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        return new DoubleStreamEx(stream.flatMap(mapper));
    }

    @Override
    public DoubleStreamEx distinct() {
        return new DoubleStreamEx(stream.distinct());
    }

    @Override
    public DoubleStreamEx sorted() {
        return new DoubleStreamEx(stream.sorted());
    }

    @Override
    public DoubleStreamEx peek(DoubleConsumer action) {
        return new DoubleStreamEx(stream.peek(action));
    }

    @Override
    public DoubleStreamEx limit(long maxSize) {
        return new DoubleStreamEx(stream.limit(maxSize));
    }

    @Override
    public DoubleStreamEx skip(long n) {
        return new DoubleStreamEx(stream.skip(n));
    }

    @Override
    public void forEach(DoubleConsumer action) {
        stream.forEach(action);
    }

    @Override
    public void forEachOrdered(DoubleConsumer action) {
        stream.forEachOrdered(action);
    }

    @Override
    public double[] toArray() {
        return stream.toArray();
    }

    @Override
    public double reduce(double identity, DoubleBinaryOperator op) {
        return stream.reduce(identity, op);
    }

    @Override
    public OptionalDouble reduce(DoubleBinaryOperator op) {
        return stream.reduce(op);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return stream.collect(supplier, accumulator, combiner);
    }

    @Override
    public double sum() {
        return stream.sum();
    }

    @Override
    public OptionalDouble min() {
        return stream.min();
    }

    @Override
    public OptionalDouble max() {
        return stream.max();
    }

    @Override
    public long count() {
        return stream.count();
    }

    @Override
    public OptionalDouble average() {
        return stream.average();
    }

    @Override
    public DoubleSummaryStatistics summaryStatistics() {
        return stream.summaryStatistics();
    }

    @Override
    public boolean anyMatch(DoublePredicate predicate) {
        return stream.anyMatch(predicate);
    }

    @Override
    public boolean allMatch(DoublePredicate predicate) {
        return stream.allMatch(predicate);
    }

    @Override
    public boolean noneMatch(DoublePredicate predicate) {
        return stream.noneMatch(predicate);
    }

    @Override
    public OptionalDouble findFirst() {
        return stream.findFirst();
    }

    @Override
    public OptionalDouble findAny() {
        return stream.findAny();
    }

    @Override
    public StreamEx<Double> boxed() {
        return new StreamEx<>(stream.boxed());
    }

    @Override
    public DoubleStreamEx sequential() {
        return new DoubleStreamEx(stream.sequential());
    }

    @Override
    public DoubleStreamEx parallel() {
        return new DoubleStreamEx(stream.parallel());
    }

    @Override
    public OfDouble iterator() {
        return stream.iterator();
    }

    @Override
    public java.util.Spliterator.OfDouble spliterator() {
        return stream.spliterator();
    }

    public DoubleStreamEx append(double... values) {
        return new DoubleStreamEx(DoubleStream.concat(stream, DoubleStream.of(values)));
    }

    public DoubleStreamEx append(DoubleStream other) {
        return new DoubleStreamEx(DoubleStream.concat(stream, other));
    }

    public DoubleStreamEx prepend(double... values) {
        return new DoubleStreamEx(DoubleStream.concat(DoubleStream.of(values), stream));
    }

    public DoubleStreamEx prepend(DoubleStream other) {
        return new DoubleStreamEx(DoubleStream.concat(other, stream));
    }

    public DoubleStreamEx remove(DoublePredicate predicate) {
        return new DoubleStreamEx(stream.filter(predicate.negate()));
    }

    public OptionalDouble findAny(DoublePredicate predicate) {
        return stream.filter(predicate).findAny();
    }

    public OptionalDouble findFirst(DoublePredicate predicate) {
        return stream.filter(predicate).findFirst();
    }

    public DoubleStreamEx sorted(Comparator<Double> comparator) {
        return new DoubleStreamEx(stream.boxed().sorted(comparator).mapToDouble(Double::doubleValue));
    }

    public <V extends Comparable<? super V>> DoubleStreamEx sortedBy(DoubleFunction<V> keyExtractor) {
        return new DoubleStreamEx(stream.boxed().sorted(Comparator.comparing(i -> keyExtractor.apply(i)))
                .mapToDouble(Double::doubleValue));
    }

    public DoubleStreamEx sortedByInt(DoubleToIntFunction keyExtractor) {
        return new DoubleStreamEx(stream.boxed().sorted(Comparator.comparingInt(i -> keyExtractor.applyAsInt(i)))
                .mapToDouble(Double::doubleValue));
    }

    public DoubleStreamEx sortedByLong(DoubleToLongFunction keyExtractor) {
        return new DoubleStreamEx(stream.boxed().sorted(Comparator.comparingLong(i -> keyExtractor.applyAsLong(i)))
                .mapToDouble(Double::doubleValue));
    }

    public DoubleStreamEx sortedByDouble(DoubleUnaryOperator keyExtractor) {
        return new DoubleStreamEx(stream.boxed().sorted(Comparator.comparingDouble(i -> keyExtractor.applyAsDouble(i)))
                .mapToDouble(Double::doubleValue));
    }

    public static DoubleStreamEx empty() {
        return EMPTY;
    }

    public static DoubleStreamEx of(double element) {
        return new DoubleStreamEx(DoubleStream.of(element));
    }

    public static DoubleStreamEx of(double... elements) {
        return new DoubleStreamEx(DoubleStream.of(elements));
    }

    public static DoubleStreamEx of(Collection<Double> c) {
        return new DoubleStreamEx(c.stream().mapToDouble(Double::doubleValue));
    }

    public static DoubleStreamEx of(Random random) {
        return new DoubleStreamEx(random.doubles());
    }

    public static DoubleStreamEx of(Random random, long streamSize) {
        return new DoubleStreamEx(random.doubles(streamSize));
    }

    public static DoubleStreamEx of(Random random, double randomNumberOrigin, double randomNumberBound) {
        return new DoubleStreamEx(random.doubles(randomNumberOrigin, randomNumberBound));
    }

    public static DoubleStreamEx of(Random random, long streamSize, double randomNumberOrigin, double randomNumberBound) {
        return new DoubleStreamEx(random.doubles(streamSize, randomNumberOrigin, randomNumberBound));
    }

    public static DoubleStreamEx iterate(final double seed, final DoubleUnaryOperator f) {
        return new DoubleStreamEx(DoubleStream.iterate(seed, f));
    }

    public static DoubleStreamEx generate(DoubleSupplier s) {
        return new DoubleStreamEx(DoubleStream.generate(s));
    }
}
