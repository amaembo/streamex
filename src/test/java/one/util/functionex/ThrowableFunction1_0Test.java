package one.util.functionex;

import one.util.functionex.utils.CustomException;
import one.util.functionex.utils.Utils;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static one.util.functionex.Tuple.of;
import static one.util.functionex.utils.Utils.throwableAction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ThrowableFunction1_0Test {

    @Test
    public void should_compose_new_function_by_adding_runnable_after() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction1_0<CustomException, String> function = firstname -> builder.append(throwableAction(firstname));
        ThrowableFunction1_0<CustomException, String> composition = function.andRun(() -> builder.append(" Skywalker"));
        composition.apply("Luke");
        assertEquals(builder.toString(), "Luke Skywalker");
    }

    @Test
    public void should_compose_new_function_by_adding_supplier_after() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction1_0<CustomException, String> function = firstname -> builder.append(throwableAction(firstname));
        ThrowableFunction1_1<CustomException, String, String> composition = function.andSupply(() -> builder.append(throwableAction(" Skywalker")).toString());
        String name = composition.apply("Luke");
        assertEquals(name, "Luke Skywalker");
    }

    @Test
    public void should_compose_new_function_by_adding_entry_supplier_after() throws CustomException {
        String name = "Skywalker";
        List<String> firstnames = new ArrayList<>();
        ThrowableFunction1_0<CustomException, List<String>> function = list -> list.addAll(asList(throwableAction("Luke"), "Anakin"));
        ThrowableFunction1_2<CustomException, List<String>, String, String> composition = function.andSupplyEntry(
            () -> of(firstnames.get(0) + " " + name, firstnames.get(1) + " " + name));
        Map.Entry<String, String> entry = composition.apply(firstnames);
        assertEquals(entry, of("Luke Skywalker", "Anakin Skywalker"));
    }

    @Test
    public void should_compose_new_function_by_adding_supplier_before() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction1_0<CustomException, String> function =
            firstname -> builder.append(firstname).append(" ").append(throwableAction("Skywalker"));
        ThrowableFunction0_0<CustomException> composition = function.compose(() -> throwableAction("Luke"));
        composition.apply();
        assertEquals(builder.toString(), "Luke Skywalker");
    }

    @Test
    public void should_compose_new_function_by_adding_function_before() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction1_0<CustomException, String> function =
            firstname -> builder.append(firstname).append(" ").append(throwableAction("Skywalker"));
        ThrowableFunction1_0<CustomException, String> composition = function.compose(Utils::throwableAction);
        composition.apply("Luke");
        assertEquals(builder.toString(), "Luke Skywalker");
    }

    @Test
    public void should_compose_new_function_by_adding_bi_function_before() throws CustomException {
        StringBuilder builder = new StringBuilder();
        ThrowableFunction1_0<CustomException, String> function = name -> builder.append(throwableAction(name));
        ThrowableFunction2_0<CustomException, String, String> composition = function.compose((s1, s2) -> s1+" "+s2);
        composition.apply("Luke", "Skywalker");
        assertEquals(builder.toString(), "Luke Skywalker");
    }

}