package one.util.streamex;

import static one.util.streamex.TestHelpers.*;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;

public class IfEmptySpliteratorTest {
    @Test
    public void testSpliterator() {
        List<Integer> data = IntStreamEx.range(1000).boxed().toList();
        checkSpliterator("++", data, () -> new IfEmptySpliterator<>(data.spliterator(), data.spliterator()));
        checkSpliterator("-+", data, () -> new IfEmptySpliterator<>(Spliterators.emptySpliterator(), data.spliterator()));
        checkSpliterator("+-", data, () -> new IfEmptySpliterator<>(data.spliterator(), Spliterators.emptySpliterator()));
        checkSpliterator("--", Collections.emptyList(), () -> new IfEmptySpliterator<>(Spliterators.emptySpliterator(), Spliterators.emptySpliterator()));
    }

    @Test
    public void testFiltered() {
        List<Integer> data = IntStreamEx.range(1000).boxed().toList();
        Supplier<Spliterator<Integer>> allMatch = () -> data.parallelStream().filter(x -> x >= 0).spliterator();
        Supplier<Spliterator<Integer>> noneMatch = () -> data.parallelStream().filter(x -> x < 0).spliterator();
        Supplier<Spliterator<Integer>> lastMatch = () -> data.parallelStream().filter(x -> x == 999).spliterator();
        checkSpliterator("++", data, () -> new IfEmptySpliterator<>(allMatch.get(), allMatch.get()));
        checkSpliterator("l+", Collections.singletonList(999), () -> new IfEmptySpliterator<>(lastMatch.get(), allMatch.get()));
        checkSpliterator("-+", data, () -> new IfEmptySpliterator<>(noneMatch.get(), allMatch.get()));
        checkSpliterator("+-", data, () -> new IfEmptySpliterator<>(allMatch.get(), noneMatch.get()));
        checkSpliterator("--", Collections.emptyList(), () -> new IfEmptySpliterator<>(noneMatch.get(), noneMatch.get()));
    }
}
