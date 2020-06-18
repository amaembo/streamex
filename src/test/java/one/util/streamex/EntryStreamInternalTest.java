/*
 * Copyright 2015, 2020 StreamEx contributors
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
package one.util.streamex;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Tagir Valeev
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EntryStreamInternalTest {

    @Test
    public void testCreate() {
        Map<String, Integer> data = createMap();
        assertEquals(data, EntryStream.of(data).toMap());
        assertEquals(data, EntryStream.of(data.entrySet().stream()).toMap());

        EntryStream<String, Integer> stream = EntryStream.of(data);
        assertSame(stream.stream(), EntryStream.of(stream).stream());
        assertSame(stream.stream(), EntryStream.of(StreamEx.of(EntryStream.of(stream))).stream());
    }

    private static Map<String, Integer> createMap() {
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("a", 1);
        data.put("bb", 22);
        data.put("ccc", 33);
        return data;
    }

}
