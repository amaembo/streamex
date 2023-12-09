package one.util.functionex;

import java.io.Serializable;
import java.util.Objects;

/**
 * A {@linkplain java.util.function.Predicate} which may throw taking two arguments.
 *
 * @param <X> the exception which may throw
 * @param <T1> the type of the first input to the predicate
 * @param <T2> the type of the second input to the predicate
 */
@FunctionalInterface
public interface ThrowablePredicate2<X extends Throwable, T1, T2> extends Serializable {
    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param t1 the first input argument
     * @param t2 the second input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     * @throws X if an exception occured
     */
    boolean test(T1 t1, T2 t2) throws X;

    /**
     * Narrows the given {@code one.util.functionex.ThrowablePredicate2<? extends Throwable, ? super T1, ? super T2>} to
     * {@code one.util.functionex.ThrowablePredicate2<? extends Throwable,T1,T2>}
     *
     * @param f A {@code one.util.functionex.ThrowablePredicate2}
     * @param <T1> The first parameter type
     * @param <T2> The second parameter type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowablePredicate2<? extends Throwable,T1,T2>}
     */
    @SuppressWarnings("unchecked")
    static <T1, T2> ThrowablePredicate2<? extends Throwable, T1, T2> narrow(ThrowablePredicate2<? extends Throwable, ? super T1, ? super T2> f) {
        return (ThrowablePredicate2<? extends Throwable, T1, T2>) f;
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another.  When evaluating the composed
     * predicate, if this predicate is {@code false}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ANDed with this
     *              predicate
     * @return a composed predicate that represents the short-circuiting logical
     * AND of this predicate and the {@code other} predicate
     * @throws NullPointerException if other is null
     */
    default ThrowablePredicate2<X, T1, T2> and(ThrowablePredicate2<X, ? super T1, ? super T2> other) {
        Objects.requireNonNull(other);
        return (t1, t2) -> test(t1, t2) && other.test(t1, t2);
    }

    /**
     * Returns a predicate that represents the logical negation of this
     * predicate.
     *
     * @return a predicate that represents the logical negation of this
     * predicate
     */
    default ThrowablePredicate2<X, T1, T2> negate() {
        return (t1, t2) -> !test(t1, t2);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another.  When evaluating the composed
     * predicate, if this predicate is {@code true}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ORed with this
     *              predicate
     * @return a composed predicate that represents the short-circuiting logical
     * OR of this predicate and the {@code other} predicate
     * @throws NullPointerException if other is null
     */
    default ThrowablePredicate2<X, T1, T2> or(ThrowablePredicate2<X, ? super T1, ? super T2> other) {
        Objects.requireNonNull(other);
        return (t1, t2) -> test(t1, t2) || other.test(t1, t2);
    }

    /**
     * Returns a predicate that tests if two arguments are equal according
     * to {@link Objects#equals(Object, Object)}.
     *
     * @param <X>     the type of exception the test can throw
     * @param <T1>    the type of the first argument to the specified predicate
     * @param <T2>    the type of the second argument to the specified predicate
     * @param targetRef1 the object reference with which to compare for equality with the first parameter,
     * @param targetRef2 the object reference with which to compare for equality with the second parameter,
     *               which may be {@code null}
     * @return a predicate that tests if two arguments are equal according
     * to {@link Objects#equals(Object, Object)}
     */
    static <X extends Throwable, T1, T2> ThrowablePredicate2<X, T1, T2> isEqual(Object targetRef1, Object targetRef2) {
        return (t1, t2) ->
            ((null == targetRef1) ? Objects.isNull(t1) : targetRef1.equals(t1)) &&
            ((null == targetRef2) ? Objects.isNull(t2) : targetRef2.equals(t2));
    }

    /**
     * Returns a predicate that is the negation of the supplied predicate.
     * This is accomplished by returning result of the calling
     * {@code target.negate()}.
     *
     * @param <X>     the type of exception the test can throw
     * @param <T1>    the type of the first argument to the specified predicate
     * @param <T2>    the type of the second argument to the specified predicate
     * @param target  predicate to negate
     *
     * @return a predicate that negates the results of the supplied
     *         predicate
     *
     * @throws NullPointerException if target is null
     *
     * @since 11
     */
    @SuppressWarnings("unchecked")
    static <X extends Throwable, T1, T2> ThrowablePredicate2<X, T1, T2> not(ThrowablePredicate2<X, ? super T1, ? super T2> target) {
        Objects.requireNonNull(target);
        return (ThrowablePredicate2<X, T1, T2>)target.negate();
    }

}
