package one.util.functionex;

import one.util.functionex.utils.CustomException;
import one.util.functionex.utils.Utils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static one.util.functionex.Tuple.of;
import static one.util.functionex.utils.Utils.checkNonNegative;
import static one.util.functionex.utils.Utils.throwableAction;
import static org.junit.Assert.assertEquals;

public class ThrowableFunction1_1Test {

    @Test
    public void should_compose_new_function_by_adding_consumer_after() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction1_0<CustomException, String> composition = ThrowableFunction1_1
            .narrow((String text) -> throwableAction(text).length())
            .andConsume(length -> builder.append("The text contains ").append(length).append(" characters"));
        composition.apply("Luke Skywalker is a jedi");
        assertEquals(builder.toString(), "The text contains 24 characters");
    }

    @Test
    public void should_compose_new_function_by_adding_mapping_after() throws CustomException {
        ThrowableFunction1_1<CustomException, String, String> composition = ThrowableFunction1_1
            .narrow((String text) -> throwableAction(text).length())
            .andMap(length -> "The text contains "+length+" characters");
        String result = composition.apply("Luke Skywalker is a jedi");
        assertEquals(result, "The text contains 24 characters");
    }

    @Test
    public void should_compose_new_function_by_adding_mapping_entry_after() throws CustomException {
        ThrowableFunction1_2<CustomException, String, Integer, String> composition = ThrowableFunction1_1
            .narrow((String text) -> throwableAction(text).length())
            .andMapToEntry(length -> of(checkNonNegative(length), "The text contains "+length+" characters"));
        Tuple<Integer, String> result = composition.apply("Luke Skywalker is a jedi");
        assertEquals(result, of(24, "The text contains 24 characters"));
    }

    @Test
    public void should_compose_new_function_by_adding_supplier_before() throws CustomException {
        String input = "Luke Skywalker is a jedi";
        ThrowableFunction0_1<CustomException, String> composition = ThrowableFunction1_1
            .narrow((Integer length) -> "The text contains "+checkNonNegative(length)+" characters")
            .compose(input::length);
        String result = composition.apply();
        assertEquals(result, "The text contains 24 characters");
    }

    @Test
    public void should_compose_new_function_by_adding_function_before() throws CustomException {
        ThrowableFunction1_1<CustomException, String, String> composition = ThrowableFunction1_1
            .narrow((Integer length) -> "The text contains "+checkNonNegative(length)+" characters")
            .compose(String::length);
        String result = composition.apply("Luke Skywalker is a jedi");
        assertEquals(result, "The text contains 24 characters");
    }

    @Test
    public void should_compose_new_function_by_adding_bi_function_before() throws CustomException {
        ThrowableFunction2_1<CustomException, String, Tuple<String, String>, String> composition = ThrowableFunction1_1
            .narrow((Integer length) -> "The text contains "+checkNonNegative(length)+" characters")
            .compose((input, replace) -> input.replaceAll(replace.t1(), replace.t2()).length());
        String result = composition.apply("Luke Skywalker is a jedi", of(" Skywalker", ""));
        assertEquals(result, "The text contains 14 characters");
    }

}