package one.util.functionex;

import one.util.functionex.utils.CustomException;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static one.util.functionex.Tuple.of;
import static one.util.functionex.utils.Utils.throwableAction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ThrowableFunction0_0Test {

    @Test
    public void should_compose_new_function_by_adding_runnable_after() throws CustomException {
        List<String> responses = new ArrayList<>();
        ThrowableFunction0_0<CustomException> composition = ThrowableFunction0_0
            .narrow(() -> responses.add(throwableAction("Luke")))
            .andRun(() -> responses.add(throwableAction("Anakin")));
        composition.apply();
        assertEquals(responses, Arrays.asList("Luke", "Anakin"));
    }
    @Test
    public void should_compose_new_function_by_adding_runnable_after_but_raise_exception() {
        List<String> responses = new ArrayList<>();
        ThrowableFunction0_0<CustomException> composition = ThrowableFunction0_0
            .narrow(() -> responses.add(throwableAction("Luke")))
            .andRun(() -> responses.add(throwableAction("error")));
        assertThrows(CustomException.class, composition::apply);
    }

    @Test
    public void should_compose_new_function_by_adding_supplier_after() throws CustomException {
        List<String> firstnames = new ArrayList<>(Collections.singletonList("Luke"));
        ThrowableFunction0_1<CustomException, String> composition = ThrowableFunction0_0
            .narrow(() -> firstnames.add(throwableAction("Anakin")))
            .andSupply(() -> throwableAction(String.join(", ", firstnames)));
        String names = composition.apply();
        assertEquals(names, "Luke, Anakin");
    }

    @Test
    public void should_compose_new_function_by_adding_entry_supplier_after() throws CustomException {
        List<String> firstnames = new ArrayList<>(Collections.singletonList("Luke"));
        ThrowableFunction0_2<CustomException, String, String> composition = ThrowableFunction0_0
            .narrow(() -> firstnames.add(throwableAction("Anakin")))
            .andSupplyEntry(() -> of(firstnames.get(0), firstnames.get(1)));
        Map.Entry<String, String> entry = composition.apply();
        assertEquals(entry, of("Luke", "Anakin"));
    }

    @Test
    public void should_compose_new_function_by_adding_runnable_before() throws CustomException {
        List<String> responses = new ArrayList<>();
        ThrowableFunction0_0<CustomException> composition = ThrowableFunction0_0
            .narrow(() -> responses.add(throwableAction("Anakin")))
            .compose(() -> responses.add(throwableAction("Luke")));
        composition.apply();
        assertEquals(responses, Arrays.asList("Luke", "Anakin"));
    }

    @Test
    public void should_compose_new_function_by_adding_supplier_before() throws CustomException {
        AtomicInteger value = new AtomicInteger(0);
        ThrowableFunction1_0<CustomException, Integer> composition = ThrowableFunction0_0
            .narrow(() -> throwableAction(value.getAndIncrement()))
            .compose(value::set);
        composition.apply(5);
        assertEquals(value.get(), 6);
    }

    @Test
    public void should_compose_new_function_by_adding_entry_consumer_before() throws CustomException {
        AtomicInteger value = new AtomicInteger(0);
        ThrowableFunction2_0<CustomException, Integer, Integer> composition = ThrowableFunction0_0
            .narrow(() -> throwableAction(value.incrementAndGet()))
            .compose((i1, i2) -> value.set(i1 + i2));
        composition.apply(5, 3);
        assertEquals(value.get(),9);
    }

}