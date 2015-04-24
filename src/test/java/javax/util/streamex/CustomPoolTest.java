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

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import static org.junit.Assert.*;

public class CustomPoolTest {
    private static class MyForkJoinWorkerThread extends ForkJoinWorkerThread {
        protected MyForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool);
        }
    }
    
    @Test
    public void testBasics() {
        AtomicLong threadCount = new AtomicLong();
        ForkJoinPool pool = new ForkJoinPool(3, new ForkJoinWorkerThreadFactory() {
            @Override
            public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                threadCount.incrementAndGet();
                return new MyForkJoinWorkerThread(pool);
            }
        }, null, false);
        assertEquals(0, threadCount.get());
        assertEquals(999999000000L, IntStreamEx.range(1000000).parallel(pool).asLongStream().map(x -> x*2).sum());
        assertEquals(3, threadCount.get());
    }
}
