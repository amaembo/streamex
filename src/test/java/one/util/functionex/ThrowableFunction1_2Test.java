package one.util.functionex;

import one.util.functionex.utils.CustomException;
import one.util.functionex.utils.Utils;
import org.junit.Test;

import java.util.*;

import static one.util.functionex.Tuple.of;
import static one.util.functionex.utils.Utils.throwableAction;
import static org.junit.Assert.assertEquals;

public class ThrowableFunction1_2Test {

    @Test
    public void should_compose_new_function_by_adding_runnable_after() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction1_2<CustomException, String, String, Integer> function = text -> of(text, text.length());
        ThrowableFunction1_0<CustomException, String> composition = function.andConsume((text, length) -> builder.append(text).append(":").append(length));
        composition.apply("Luke Skywalker is a jedi");
        assertEquals(builder.toString(), "Luke Skywalker is a jedi:24");
    }

    @Test
    public void should_compose_new_function_by_adding_supplier_after() throws CustomException {
        ThrowableFunction1_2<CustomException, String, String, Integer> function = text -> of(text, text.length());
        ThrowableFunction1_1<CustomException, String, String> composition = function.andReduce((text, length) -> throwableAction(text+":"+length));
        String result = composition.apply("Luke Skywalker is a jedi");
        assertEquals(result, "Luke Skywalker is a jedi:24");
    }

    @Test
    public void should_compose_new_function_by_adding_entry_supplier_after() throws CustomException {
        String name = "Skywalker";
        List<String> firstnames = new ArrayList<>();
        ThrowableFunction1_2<CustomException, String, String, Integer> function = text -> of(text, text.length());
        ThrowableFunction1_2<CustomException, String, String, Integer> composition = function.andMap(
            (text, length) -> of(throwableAction(text), length));
        Tuple<String, Integer> entry = composition.apply("Luke Skywalker is a jedi");
        assertEquals(entry, of("Luke Skywalker is a jedi", 24));
    }

    @Test
    public void should_compose_new_function_by_adding_supplier_before() throws CustomException {
        ThrowableFunction1_2<CustomException, String, String, Integer> function =
            text -> of(text, text.length());
        ThrowableFunction0_2<CustomException, String, Integer> composition = function.compose(() -> throwableAction("Luke Skywalker is a jedi"));
        Map.Entry<String, Integer> entry = composition.apply();
        assertEquals(entry, of("Luke Skywalker is a jedi", 24));
    }

    @Test
    public void should_compose_new_function_by_adding_function_before() throws CustomException {
        ThrowableFunction1_2<CustomException, String, String, Integer> function =
            text -> of(text, text.length());
        ThrowableFunction1_2<CustomException, String, String, Integer> composition = function.compose(Utils::throwableAction);
        Map.Entry<String, Integer> entry = composition.apply("Luke Skywalker is a jedi");
        assertEquals(entry, of("Luke Skywalker is a jedi", 24));
    }

    @Test
    public void should_compose_new_function_by_adding_bi_function_before() throws CustomException {
        ThrowableFunction1_2<CustomException, String, String, Integer> function =
            text -> of(text, text.length());
        ThrowableFunction2_2<CustomException, String, String, String, Integer> composition = function.compose((name, text) -> throwableAction(name)+" "+text);
        Map.Entry<String, Integer> entry = composition.apply("Luke Skywalker", "is a jedi");
        assertEquals(entry, of("Luke Skywalker is a jedi", 24));
    }

}