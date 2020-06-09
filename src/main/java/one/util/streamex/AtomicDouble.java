package one.util.streamex;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleBinaryOperator;

import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.longBitsToDouble;

public final class AtomicDouble extends Number {
    private final AtomicLong bits;

    public AtomicDouble() {
        this(0.0d);
    }

    public AtomicDouble(double initialValue) {
        bits = new AtomicLong(toLong(initialValue));
    }

    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    public final boolean compareAndSet(double expect, double update) {
        return bits.compareAndSet(toLong(expect), toLong(update));
    }

    public final double get() {
        return toDouble(bits.get());
    }

    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the updated value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function
     * is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value
     */
    public final double accumulateAndGet(double x, DoubleBinaryOperator accumulatorFunction) {
        double prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsDouble(prev, x);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the previous value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function
     * is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     */
    public final double getAndAccumulate(double x, DoubleBinaryOperator accumulatorFunction) {
        double prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsDouble(prev, x);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    @Override
    public long longValue() {
        return (long) get();
    }

    @Override
    public String toString() {
        return Double.toString(get());
    }

    @Override
    public int intValue() {
        return (int) get();
    }

    @Override
    public float floatValue() {
        return (float) get();
    }

    @Override
    public double doubleValue() {
        return get();
    }

    private static double toDouble(long l) {
        return longBitsToDouble(l);
    }

    private static long toLong(double delta) {
        return doubleToLongBits(delta);
    }
}