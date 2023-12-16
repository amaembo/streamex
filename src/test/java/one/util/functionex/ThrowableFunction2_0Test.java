package one.util.functionex;

import one.util.functionex.utils.CustomException;
import one.util.functionex.utils.Utils;
import one.util.streamex.StreamEx;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static one.util.functionex.Tuple.of;
import static one.util.functionex.utils.Utils.throwableAction;
import static org.junit.Assert.assertEquals;

public class ThrowableFunction2_0Test {

    @Test
    public void should_compose_new_function_by_adding_runnable_after() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction2_0<CustomException, String, String> function = (firstname, lastname) -> builder.append(throwableAction(firstname+ " "+ lastname));
        ThrowableFunction2_0<CustomException, String, String> composition = function.andRun(() -> builder.append(" is a jedi."));
        composition.apply("Luke", "Skywalker");
        assertEquals(builder.toString(), "Luke Skywalker is a jedi.");
    }

    @Test
    public void should_compose_new_function_by_adding_supplier_after() throws CustomException {
        String name = "Skywalker";
        List<String> firstnames = new ArrayList<>();
        ThrowableFunction2_0<CustomException, String, String> function = (name1, name2) -> firstnames.addAll(asList(throwableAction(name1), name2));
        ThrowableFunction2_1<CustomException, String, String, List<String>> composition = function.andSupply(
            () -> StreamEx.of(firstnames).mapThrowing(first -> throwableAction(first + " " + name)).toList());
        List<String> names = composition.apply("Luke", "Anakin");
        assertEquals(names, asList("Luke Skywalker", "Anakin Skywalker"));
    }

    @Test
    public void should_compose_new_function_by_adding_entry_supplier_after() throws CustomException {
        String name = "Skywalker";
        List<String> firstnames = new ArrayList<>();
        ThrowableFunction2_0<CustomException, String, String> function = (name1, name2) -> firstnames.addAll(asList(throwableAction(name1), name2));
        ThrowableFunction2_2<CustomException, String, String, String, String> composition = function.andSupplyEntry(
            () -> of(firstnames.get(0)+" "+name, firstnames.get(1)+" "+name));
        Map.Entry<String, String> entry = composition.apply("Luke", "Anakin");
        assertEquals(entry, of("Luke Skywalker", "Anakin Skywalker"));
    }

    @Test
    public void should_compose_new_function_by_adding_supplier_before() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction2_0<CustomException, String, String> function =
            (firstname, lastname) -> builder.append(firstname).append(" ").append(throwableAction(lastname));
        ThrowableFunction0_0<CustomException> composition = function.compose(
            () -> of(throwableAction("Luke"), "Skywalker"));
        composition.apply();
        assertEquals(builder.toString(), "Luke Skywalker");
    }

    @Test
    public void should_compose_new_function_by_adding_function_before() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction2_0<CustomException, String, String> function =
            (firstname, lastname) -> builder.append(firstname).append(" ").append(throwableAction(lastname));
        ThrowableFunction1_0<CustomException, String> composition = function.compose(
            firstname -> of(throwableAction(firstname), "Skywalker"));
        composition.apply("Luke");
        assertEquals(builder.toString(), "Luke Skywalker");
    }

    @Test
    public void should_compose_new_function_by_adding_bi_function_before() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction2_0<CustomException, String, String> function =
            (firstname, lastname) -> builder.append(firstname).append(" ").append(throwableAction(lastname));
        ThrowableFunction2_0<CustomException, String, String> composition = function.compose(
            (firstname, lastname) -> of(throwableAction(firstname), lastname));
        composition.apply("Luke", "Skywalker");
        assertEquals(builder.toString(), "Luke Skywalker");
    }

}