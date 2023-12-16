package one.util.functionex;

import one.util.functionex.utils.CustomException;
import one.util.streamex.StreamEx;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static one.util.functionex.Tuple.of;
import static one.util.functionex.utils.Utils.checkNonNegative;
import static one.util.functionex.utils.Utils.throwableAction;
import static org.junit.Assert.assertEquals;

public class ThrowableFunction2_2Test {

    @Test
    public void should_compose_new_function_by_adding_runnable_after() throws CustomException {
        AtomicInteger result = new AtomicInteger();
        ThrowableFunction2_0<CustomException, Integer, Integer> composition = ThrowableFunction2_2
            .narrow((Integer v1, Integer v2) -> of(checkNonNegative(v1+v2), v1-v2))
            .andConsume((addition, conjugate) -> result.set(addition * conjugate));
        composition.apply(5, 3);
        assertEquals(16, result.get());
    }

    @Test
    public void should_compose_new_function_by_reducing_after() throws CustomException {
        ThrowableFunction2_1<CustomException, Integer, Integer, Integer> composition = ThrowableFunction2_2
            .narrow((Integer v1, Integer v2) -> of(checkNonNegative(v1+v2), v1-v2))
            .andReduce((addition, conjugate) -> addition * conjugate);
        assertEquals(16, composition.apply(5, 3).intValue());
    }

    @Test
    public void should_compose_new_function_by_mapping_entry_after() throws CustomException {
        ThrowableFunction2_2<CustomException, Integer, Integer, Integer, Integer> composition = ThrowableFunction2_2
            .narrow((Integer v1, Integer v2) -> of(checkNonNegative(v1+v2), v1-v2))
            .andMap((addition, conjugate) -> of(addition * conjugate, addition/conjugate));
        assertEquals(of(16,4), composition.apply(5, 3));
    }

    @Test
    public void should_compose_new_function_by_adding_supplier_entry_before() throws CustomException {
        Tuple<Integer, Integer> input = of(5, 3);
        ThrowableFunction0_2<CustomException, Integer, Integer> composition = ThrowableFunction2_2
            .narrow((Integer v1, Integer v2) -> of(checkNonNegative(v1+v2), v1-v2))
            .compose(() -> of(input.t1()* input.t1(), input.t2()*input.t2()));
        assertEquals(of(34,16), composition.apply());
    }

    @Test
    public void should_compose_new_function_by_adding_function_before() throws CustomException {
        ThrowableFunction1_2<CustomException, Tuple<Integer, Integer>, Integer, Integer> composition = ThrowableFunction2_2
            .narrow((Integer v1, Integer v2) -> of(checkNonNegative(v1+v2), v1-v2))
            .compose(input -> of(input.t1()* input.t1(), input.t2()*input.t2()));
        assertEquals(of(34,16), composition.apply(of(5,3)));
    }

    @Test
    public void should_compose_new_function_by_adding_bi_function_before() throws CustomException {
        ThrowableFunction2_2<CustomException, Integer, Integer, Integer, Integer> composition = ThrowableFunction2_2
            .narrow((Integer v1, Integer v2) -> of(checkNonNegative(v1+v2), v1-v2))
            .compose((v1, v2) -> of(v1*v1, v2*v2));
        assertEquals(of(34,16), composition.apply(5,3));
    }

}