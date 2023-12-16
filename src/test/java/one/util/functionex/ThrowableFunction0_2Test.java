package one.util.functionex;

import one.util.functionex.utils.CustomException;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Map;

import static one.util.functionex.Tuple.of;
import static one.util.functionex.utils.Utils.throwableAction;
import static org.junit.Assert.assertEquals;

public class ThrowableFunction0_2Test {

    @Test
    public void should_compose_new_function_by_adding_consumer_after() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction0_2<CustomException, String, String> function = () -> of(throwableAction("Luke"), "Skywalker");
        ThrowableFunction0_0<CustomException> composition = function.andConsumes((first, last) -> builder.append(first).append(" ").append(last));
        composition.apply();
        assertEquals(builder.toString(), "Luke Skywalker");
    }

    @Test
    public void should_compose_new_function_by_adding_map_function_after() throws CustomException {
        ThrowableFunction0_2<CustomException, String, String> function = () -> of(throwableAction("Luke"), "Skywalker");
        ThrowableFunction0_1<CustomException, String> composition = function.andReduce((first, last) -> first+" "+last);
        String result = composition.apply();
        assertEquals(result, "Luke Skywalker");
    }

    @Test
    public void should_compose_new_function_by_adding_map_to_entry_function_after() throws CustomException {
        ThrowableFunction0_2<CustomException, String, String> function = () -> of(throwableAction("Luke"), "Skywalker");
        ThrowableFunction0_2<CustomException, String, Integer> composition = function.andMap((first, last) -> of(first+" "+last, (first+" "+last).length()));
        Map.Entry<String, Integer> entry = composition.apply();
        assertEquals(entry.getKey(), "Luke Skywalker");
        assertEquals(entry.getValue().intValue(), 14);
    }

    @Test
    public void should_compose_new_function_by_adding_supplier_before() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction0_2<CustomException, String, Integer> function = () -> of(builder.toString(), builder.toString().length());
        ThrowableFunction0_2<CustomException, String, Integer> composition = function.compose(() -> builder.append("Luke Skywalker"));
        Map.Entry<String, Integer> entry = composition.apply();
        assertEquals(entry.getKey(), "Luke Skywalker");
        assertEquals(entry.getValue().intValue(), 14);
    }

    @Test
    public void should_compose_new_function_by_adding_consumer_before() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction0_2<CustomException, String, Integer> function = () -> of(builder.toString(), builder.toString().length());
        ThrowableFunction1_2<CustomException, String, String, Integer> composition = function.compose(builder::append);
        Map.Entry<String, Integer> entry = composition.apply("Luke Skywalker");
        assertEquals(entry.getKey(), "Luke Skywalker");
        assertEquals(entry.getValue().intValue(), 14);
    }

    @Test
    public void should_compose_new_function_by_adding_entry_consumer_before() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction0_2<CustomException, String, Integer> function = () -> of(builder.toString(), builder.toString().length());
        ThrowableFunction2_2<CustomException, String, String, String, Integer> composition = function.compose((first, last) -> builder.append(first).append(" ").append(last));
        Map.Entry<String, Integer> entry = composition.apply("Luke", "Skywalker");
        assertEquals(entry.getKey(), "Luke Skywalker");
        assertEquals(entry.getValue().intValue(), 14);
    }
}