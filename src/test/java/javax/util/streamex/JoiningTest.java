/*
 * Copyright 2015 Tagir Valeev
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.util.streamex;

import static javax.util.streamex.TestHelpers.*;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class JoiningTest {
    @Test
    public void testSimple() {
        Supplier<Stream<String>> s = () -> IntStream.range(0, 100).mapToObj(String::valueOf);
        checkCollector("joiningSimple", s.get().collect(Collectors.joining(", ")), s, Joining.with(", "));
        checkCollector("joiningWrap", s.get().collect(Collectors.joining(", ", "[", "]")), s,
            Joining.with(", ").wrap("[", "]"));
        checkCollector("joiningWrap2", s.get().collect(Collectors.joining(", ", "[(", ")]")), s, Joining.with(", ")
                .wrap("(", ")").wrap("[", "]"));
    }

    @Test
    public void testCutSimple() {
        List<String> input = Arrays.asList("one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten");
        assertEquals("", StreamEx.of(input).peek(Assert::fail).collect(Joining.with(", ").maxChars(0).cutAnywhere()));
        assertEquals("",
            StreamEx.of(input).parallel().peek(Assert::fail).collect(Joining.with(", ").maxChars(0).cutAnywhere()));
        String expected = "one, two, three, four, five, six, seven, eight, nine, ten";
        for (int i = 3; i < expected.length() + 5; i++) {
            String exp = expected;
            if (exp.length() > i) {
                exp = exp.substring(0, i - 3) + "...";
            }
            String exp2 = expected;
            while (exp2.length() > i) {
                int pos = exp2.lastIndexOf(", ", exp2.endsWith(", ...") ? exp2.length() - 6 : exp2.length());
                exp2 = pos >= 0 ? exp2.substring(0, pos + 2) + "..." : "...";
            }
            for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
                assertEquals(supplier + "/#" + i, exp,
                    supplier.get().collect(Joining.with(", ").maxChars(i).cutAnywhere()));
                assertEquals(supplier + "/#" + i, expected.substring(0, Math.min(i, expected.length())), supplier.get()
                        .collect(Joining.with(", ").ellipsis("").maxChars(i).cutAnywhere()));
                assertEquals(supplier + "/#" + i, exp2,
                    supplier.get().collect(Joining.with(", ").maxChars(i).cutAfterDelimiter()));
            }
        }

        byte[] data = { (byte) 0xFF, 0x30, 0x40, 0x50, 0x70, 0x12, (byte) 0xF0 };
        assertEquals(
            "FF 30 40 50 ...",
            IntStreamEx.of(data).mapToObj(b -> String.format(Locale.ENGLISH, "%02X", b & 0xFF))
                    .collect(Joining.with(" ").maxChars(15).cutAfterDelimiter()));
    }

    @Test
    public void testCuts() {
        List<String> input = Arrays.asList("one two", "three four", "five", "six seven");

        checkShortCircuitCollector("cutBefore", "one two, three four...", 4, input::stream, Joining.with(", ")
                .maxChars(25).cutBeforeDelimiter());
        checkShortCircuitCollector("cutBefore", "one two...", 2, input::stream, Joining.with(", ").maxChars(10)
                .cutBeforeDelimiter());
        checkShortCircuitCollector("cutBefore", "...", 2, input::stream, Joining.with(", ").maxChars(9)
                .cutBeforeDelimiter());

        checkShortCircuitCollector("cutAfter", "one two, three four, ...", 4, input::stream, Joining.with(", ")
                .maxChars(25).cutAfterDelimiter());
        checkShortCircuitCollector("cutAfter", "one two, ...", 2, input::stream, Joining.with(", ").maxChars(12)
                .cutAfterDelimiter());
        checkShortCircuitCollector("cutAfter", "...", 2, input::stream, Joining.with(", ").maxChars(11)
                .cutAfterDelimiter());

        checkShortCircuitCollector("cutWord", "one two, three four, ...", 4, input::stream, Joining.with(", ")
                .maxChars(25).cutAfterWord());
        checkShortCircuitCollector("cutWord", "one two, ...", 2, input::stream, Joining.with(", ").maxChars(12)
                .cutAfterWord());
        checkShortCircuitCollector("cutWord", "one two,...", 2, input::stream, Joining.with(", ").maxChars(11)
                .cutAfterWord());
        checkShortCircuitCollector("cutWord", "one two...", 2, input::stream, Joining.with(", ").maxChars(10)
                .cutAfterWord());
        checkShortCircuitCollector("cutWord", "one ...", 2, input::stream, Joining.with(", ").maxChars(9)
                .cutAfterWord());
        checkShortCircuitCollector("cutWord", "one...", 1, input::stream, Joining.with(", ").maxChars(6).cutAfterWord());

        checkShortCircuitCollector("cutCodePoint", "one two, three four, f...", 4, input::stream, Joining.with(", ")
                .maxChars(25));
        checkShortCircuitCollector("cutCodePoint", "one two, ...", 2, input::stream, Joining.with(", ").maxChars(12));
        checkShortCircuitCollector("cutCodePoint", "one two,...", 2, input::stream, Joining.with(", ").maxChars(11));
        checkShortCircuitCollector("cutCodePoint", "one two...", 2, input::stream, Joining.with(", ").maxChars(10));
        checkShortCircuitCollector("cutCodePoint", "one tw...", 2, input::stream, Joining.with(", ").maxChars(9));
        checkShortCircuitCollector("cutCodePoint", "one...", 1, input::stream, Joining.with(", ").maxChars(6));
    }

    @Test
    public void testPrefixSuffix() {
        List<String> input = Arrays.asList("one two", "three four", "five", "six seven");
        checkShortCircuitCollector("cutWord", "[one two, three four,...]", 3, input::stream, Joining.with(", ")
                .wrap("[", "]").maxChars(25).cutAfterWord());
        checkShortCircuitCollector("cutWord", "[one two...]", 2, input::stream, Joining.with(", ").maxChars(12)
            .wrap("[", "]").cutAfterWord());
        checkShortCircuitCollector("cutWord", "[one ...]", 2, input::stream, Joining.with(", ").maxChars(11)
            .wrap("[", "]").cutAfterWord());
        checkShortCircuitCollector("cutWord", "[one ...]", 2, input::stream, Joining.with(", ").maxChars(10)
            .wrap("[", "]").cutAfterWord());
        checkShortCircuitCollector("cutWord", "[one...]", 1, input::stream, Joining.with(", ").maxChars(8)
            .wrap("[", "]").cutAfterWord());
        checkShortCircuitCollector("cutWord", "[...]", 1, input::stream, Joining.with(", ").maxChars(6).wrap("[", "]").cutAfterWord());
        checkShortCircuitCollector("cutWord", "[..]", 1, input::stream, Joining.with(", ").maxChars(4).wrap("[", "]").cutAfterWord());
        checkShortCircuitCollector("cutWord", "[.]", 1, input::stream, Joining.with(", ").maxChars(3).wrap("[", "]").cutAfterWord());
        checkShortCircuitCollector("cutWord", "[]", 0, input::stream, Joining.with(", ").maxChars(2).wrap("[", "]").cutAfterWord());
        checkShortCircuitCollector("cutWord", "[", 0, input::stream, Joining.with(", ").maxChars(1).wrap("[", "]").cutAfterWord());
        checkShortCircuitCollector("cutWord", "", 0, input::stream, Joining.with(", ").maxChars(0).wrap("[", "]").cutAfterWord());
    }
}
