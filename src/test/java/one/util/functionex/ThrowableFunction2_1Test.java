package one.util.functionex;

import one.util.functionex.utils.CustomException;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static one.util.functionex.Tuple.of;
import static one.util.functionex.utils.Utils.checkNonNegative;
import static org.junit.Assert.assertEquals;

public class ThrowableFunction2_1Test {

    @Test
    public void should_compose_new_function_by_adding_runnable_after() throws CustomException {
        AtomicInteger result = new AtomicInteger();
        ThrowableFunction2_0<CustomException, Integer, Integer> composition = ThrowableFunction2_1
            .narrow((Integer width, Integer height) -> checkNonNegative(2*(width+height)))
            .andConsume(perimeter -> result.set(perimeter*perimeter));
        composition.apply(5, 3);
        assertEquals(256, result.get());
    }

    @Test
    public void should_compose_new_function_by_mapping_after() throws CustomException {
        ThrowableFunction2_1<CustomException, Integer, Integer, Integer> composition = ThrowableFunction2_1
            .narrow((Integer width, Integer height) -> checkNonNegative(2*(width+height)))
            .andMap(perimeter -> perimeter*perimeter);
        assertEquals(256, composition.apply(5, 3).intValue());
    }

    @Test
    public void should_compose_new_function_by_mapping_entry_after() throws CustomException {
        ThrowableFunction2_2<CustomException, Integer, Integer, Integer, Double> composition = ThrowableFunction2_1
            .narrow((Integer width, Integer height) -> checkNonNegative(2*(width+height)))
            .andMapToEntry(perimeter -> of(perimeter*perimeter, Math.sqrt(perimeter)));
        assertEquals(of(256, 4.0), composition.apply(5, 3));
    }

    @Test
    public void should_compose_new_function_by_adding_supplier_entry_before() throws CustomException {
        Tuple<Integer, Integer> input = of(5, 3);
        ThrowableFunction0_1<CustomException, Integer> composition = ThrowableFunction2_1
            .narrow((Integer v1, Integer v2) -> checkNonNegative(v1)-v2)
            .compose(() -> of(input.t1()* input.t1(), input.t2()*input.t2()));
        assertEquals(16, composition.apply().intValue());
    }

    @Test
    public void should_compose_new_function_by_adding_function_before() throws CustomException {
        ThrowableFunction1_1<CustomException, Tuple<Integer, Integer>, Integer> composition = ThrowableFunction2_1
            .narrow((Integer v1, Integer v2) -> checkNonNegative(v1)-v2)
            .compose(input -> of(input.t1()* input.t1(), input.t2()*input.t2()));
        assertEquals(16, composition.apply(of(5,3)).intValue());
    }

    @Test
    public void should_compose_new_function_by_adding_bi_function_before() throws CustomException {
        ThrowableFunction2_1<CustomException, Integer, Integer, Integer> composition = ThrowableFunction2_1
            .narrow((Integer v1, Integer v2) -> checkNonNegative(v1)-v2)
            .compose((v1, v2) -> of(v1*v1, v2*v2));
        assertEquals(16, composition.apply(5,3).intValue());
    }

}