/*
 * Copyright 2015, 2017 StreamEx contributors
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

import java.util.Spliterator;
import java.util.function.Consumer;

/* package */ final class CombinationSpliterator implements Spliterator<int[]> {
    private long pos;
    private int[] value;
    private final long fence;
    private final int n;

    public CombinationSpliterator(int n, long pos, long fence, int[] value) {
        this.n = n;
        this.pos = pos;
        this.fence = fence;
        this.value = value;
    }
    @Override
    public void forEachRemaining(Consumer<? super int[]> action) {
        long rest = pos - fence;
        pos = fence;
        while (rest > 0) {
            action.accept(value.clone());
            if (--rest > 0) {
                step(value, n);
            }
        }
    }

    @Override
    public Spliterator<int[]> trySplit() {
        if (pos - fence < 2) return null;
        long newPos = (fence + pos) >>> 1;

        CombinationSpliterator result = new CombinationSpliterator(n, pos, newPos, value);
        value = jump(newPos - 1, value.length, n);
        pos = newPos;
        return result;
    }

    @Override
    public long estimateSize() {
        return pos - fence;
    }

    @Override
    public int characteristics() {
        return DISTINCT | IMMUTABLE | NONNULL | ORDERED | SIZED | SUBSIZED;
    }

    static void step(int[] value, int n) {
        int i, k = value.length;
        for (i = k; --i >= 0 && ++value[i] >= n; ) {
            n--;
        }

        while (++i < k) {
            value[i] = value[i - 1] + 1;
        }
    }

    static int[] jump(long newPos, int k, int n) {
        int[] newValue = new int[k];
        int curK = k - 1;
        long bound = 0;
        for (int i = 0; i < k; i++) {
            long cnk = 1;
            int curN = curK;
            while (newPos >= bound + cnk) {
                bound += cnk;
                curN++;
                cnk = cnk * curN / (curN - curK);
            }
            curK--;
            newValue[i] = n - curN - 1;
        }
        return newValue;
    }


    static long gcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    /**
     * @param n n > k
     * @param k k > 0
     * @return CNK(n, k)
     */
    static long cnk(int n, int k) {
        long size = 1;

        int rest = n;
        for (long div = 1; div <= k; ++div, --rest) {
            long gcd = gcd(size, div);
            size /= gcd;
            long t = rest / (div / gcd);

            if (size > Long.MAX_VALUE / t) {
                throw new UnsupportedOperationException("Number of combinations exceed Long.MAX_VALUE: unsupported");
            }

            size *= t;
        }
        return size;
    }

    @Override
    public boolean tryAdvance(Consumer<? super int[]> action) {
        if (pos <= fence) {
            return false;
        }
        action.accept(value.clone());
        if (--pos > fence) {
            step(value, n);
        }
        return true;
    }
}
