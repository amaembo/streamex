package one.util.functionex;

import one.util.functionex.utils.CustomException;
import org.junit.Test;

import java.util.*;

import static one.util.functionex.Tuple.of;
import static one.util.functionex.utils.Utils.throwableAction;
import static org.junit.Assert.assertEquals;

public class ThrowableFunction0_1Test {

    @Test
    public void should_compose_new_function_by_adding_consumer_after() throws CustomException {
        List<String> responses = new ArrayList<>();
        ThrowableFunction0_0<CustomException> composition = ThrowableFunction0_1
            .narrow(() -> throwableAction("Luke"))
            .andConsumes(first -> responses.add(first+" "+throwableAction("Skywalker")));
        composition.apply();
        assertEquals(responses, Collections.singletonList("Luke Skywalker"));
    }

    @Test
    public void should_compose_new_function_by_adding_map_function_after() throws CustomException {
        ThrowableFunction0_1<CustomException, String> composition = ThrowableFunction0_1
            .narrow(() -> throwableAction("Luke"))
            .andMap(first -> first +" Skywalker");
        String name = composition.apply();
        assertEquals(name, "Luke Skywalker");
    }

    @Test
    public void should_compose_new_function_by_adding_map_to_entry_function_after() throws CustomException {
        String firstname = "Luke";
        ThrowableFunction0_2<CustomException, String, String> composition = ThrowableFunction0_1
            .narrow(() -> throwableAction("Skywalker"))
            .andMapToEntry(name -> of(firstname, name));
        Map.Entry<String, String> entry = composition.apply();
        assertEquals(entry, of("Luke", "Skywalker"));
    }

    @Test
    public void should_compose_new_function_by_adding_supplier_before() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction0_1<CustomException, String> composition = ThrowableFunction0_1
            .narrow(() -> builder.append(throwableAction(" Skywalker")).toString())
            .compose(() -> builder.append(throwableAction("Luke")));
        String result = composition.apply();
        assertEquals(result, "Luke Skywalker");
    }

    @Test
    public void should_compose_new_function_by_adding_consumer_before() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction1_1<CustomException, String, String> composition = ThrowableFunction0_1
            .narrow(() -> builder.append(throwableAction("Skywalker")).toString())
            .compose(first -> builder.append(first).append(" "));
        String result = composition.apply("Luke");
        assertEquals(result, "Luke Skywalker");
    }

    @Test
    public void should_compose_new_function_by_adding_entry_consumer_before() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction2_1<CustomException, String, String, String> composition = ThrowableFunction0_1
            .narrow(() -> builder.append(throwableAction("Skywalker")).toString())
            .compose((first, delimiter) -> builder.append(first).append(delimiter));
        String result = composition.apply("Luke", " ");
        assertEquals(result,"Luke Skywalker");
    }
}