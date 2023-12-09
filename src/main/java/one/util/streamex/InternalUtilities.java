package one.util.streamex;

import one.util.functionex.*;

import java.util.Map;
import java.util.function.*;

class InternalUtilities {
    @SuppressWarnings("unchecked")
    public static <T extends Throwable, R> R sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
    public static Runnable toRunnableSneakyThrowing(ThrowableFunction0_0<? extends Throwable> action) {
        return () -> {
            try {
                action.apply();
            } catch (Throwable e) {
                sneakyThrow(e);
            }
        };
    }
    public static <T> Consumer<T> toConsumerSneakyThrowing(ThrowableFunction1_0<? extends Throwable, ? super T> action) {
        return element -> {
            try {
                action.apply(element);
            } catch (Throwable e) {
                sneakyThrow(e);
            }
        };
    }
    public static <T1, T2> BiConsumer<T1, T2> toBiConsumerSneakyThrowing(ThrowableFunction2_0<? extends Throwable, ? super T1, ? super T2> action) {
        return (t1, t2) -> {
            try {
                action.apply(t1, t2);
            } catch (Throwable e) {
                sneakyThrow(e);
            }
        };
    }
    public static <R> Supplier<R> toSupplierSneakyThrowing(ThrowableFunction0_1<? extends Throwable, ? extends R> action) {
        return () -> {
            try {
                return action.apply();
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }
    public static <T, R> Function<T, R> toFunctionSneakyThrowing(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends R> action) {
        return element -> {
            try {
                return action.apply(element);
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }
    public static <T> ToIntFunction<T> toIntFunctionSneakyThrowing(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends Integer> action) {
        return element -> {
            try {
                return action.apply(element);
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }
    public static <T> ToLongFunction<T> toLongFunctionSneakyThrowing(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends Long> action) {
        return element -> {
            try {
                return action.apply(element);
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }
    public static <T> ToDoubleFunction<T> toDoubleFunctionSneakyThrowing(ThrowableFunction1_1<? extends Throwable, ? super T, ? extends Double> action) {
        return element -> {
            try {
                return action.apply(element);
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }
    public static <T1, T2, R> BiFunction<T1, T2, R> toBiFunctionSneakyThrowing(ThrowableFunction2_1<? extends Throwable, ? super T1, ? super T2, ? extends R> action) {
        return (t1, t2) -> {
            try {
                return action.apply(t1, t2);
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }
    public static <T1, T2> ToIntBiFunction<T1, T2> toIntBiFunctionSneakyThrowing(ThrowableFunction2_1<? extends Throwable, ? super T1, ? super T2, ? extends Integer> action) {
        return (t1, t2) -> {
            try {
                return action.apply(t1, t2);
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }
    public static <T1, T2> ToLongBiFunction<T1, T2> toLongBiFunctionSneakyThrowing(ThrowableFunction2_1<? extends Throwable, ? super T1, ? super T2, ? extends Long> action) {
        return (t1, t2) -> {
            try {
                return action.apply(t1, t2);
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }
    public static <T1, T2> ToDoubleBiFunction<T1, T2> toDoubleBiFunctionSneakyThrowing(ThrowableFunction2_1<? extends Throwable, ? super T1, ? super T2, ? extends Double> action) {
        return (t1, t2) -> {
            try {
                return action.apply(t1, t2);
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }

    public static <T> BinaryOperator<T> toBinaryOperatorSneakyThrowing(ThrowableBinaryOperator<? extends Throwable, T> operator) {
        return (t1, t2) -> {
            try {
                return operator.apply(t1, t2);
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <R1, R2> Supplier<? extends Map.Entry<R1, R2>> toEntrySupplierSneakyThrowing(ThrowableFunction0_2<? extends Throwable, ? extends R1, ? extends R2> provider) {
        return () -> {
            try {
                return (Map.Entry<R1, R2>) provider.apply();
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T, R1, R2> Function<T, Map.Entry<R1, R2>> toEntryFunctionSneakyThrowing(ThrowableFunction1_2<? extends Throwable, ? super T, ? extends R1, ? extends R2> function) {
        return element -> {
            try {
                return (Map.Entry<R1, R2>) function.apply(element);
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }
    @SuppressWarnings("unchecked")
    public static <T1, T2, R1, R2> BiFunction<T1, T2, Map.Entry<R1, R2>> toEntryBiFunctionSneakyThrowing(ThrowableFunction2_2<? extends Throwable, ? super T1, ? super T2, ? extends R1, ? extends R2> function) {
        return (t1, t2) -> {
            try {
                return (Map.Entry<R1, R2>) function.apply(t1, t2);
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }
    public static <T> Predicate<T> toPredicateSneakyThrowing(ThrowablePredicate1<? extends Throwable, ? super T> predicate) {
        return element -> {
            try {
                return predicate.test(element);
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }
    public static <T1, T2> BiPredicate<T1, T2> toBiPredicateSneakyThrowing(ThrowablePredicate2<? extends Throwable, ? super T1, ? super T2> predicate) {
        return (t1, t2) -> {
            try {
                return predicate.test(t1, t2);
            } catch (Throwable e) {
                return sneakyThrow(e);
            }
        };
    }
}
