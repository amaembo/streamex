package one.util.streamex;

import java.util.Spliterator;
import java.util.function.Consumer;

final class PermutationSpliterator implements Spliterator<int[]> {
    private static final long[] factorials = new long[] { 1L, 1L, 2L, 6L, 24L, 120L, 720L, 5040L, 40320L, 362880L,
            3628800L, 39916800L, 479001600L, 6227020800L, 87178291200L, 1307674368000L, 20922789888000L,
            355687428096000L, 6402373705728000L, 121645100408832000L, 2432902008176640000L };

    private final int[] value;
    private long remainingSize;
    private final long fence;

    public PermutationSpliterator(int length) {
        if (length < 0)
            throw new IllegalArgumentException("Length must be non-negative");
        if (length >= factorials.length)
            throw new IllegalArgumentException("Length " + length + " is bigger than " + factorials.length
                + ": not supported");
        this.value = new int[length];
        for (int i = 0; i < length; i++)
            this.value[i] = i;
        this.fence = this.remainingSize = factorials[length];
    }

    public PermutationSpliterator(int[] startValue, long fence, long remainingSize) {
        this.value = startValue;
        this.fence = fence;
        this.remainingSize = remainingSize;
    }

    @Override
    public boolean tryAdvance(Consumer<? super int[]> action) {
        if (remainingSize == 0)
            return false;
        int[] value = this.value;
        action.accept(value.clone());
        if (--remainingSize > 0) {
            int r = value.length - 1, k = r - 1;
            while (value[k] > value[k + 1])
                k--;
            int vk = value[k], l = r;
            while (vk > value[l])
                l--;
            value[k] = value[l];
            value[l] = vk;
            for (k++; k < r; k++, r--) {
                int tmp = value[k];
                value[k] = value[r];
                value[r] = tmp;
            }
        }
        return true;
    }

    @Override
    public Spliterator<int[]> trySplit() {
        if (remainingSize <= 1)
            return null;
        int[] newValue = value.clone();
        long used = -1L; // clear bit = used position
        long newRemainingSize = remainingSize / 2;
        long newPos = fence - remainingSize + newRemainingSize;
        long s = newPos;
        for (int i = 0; i < value.length; i++) {
            long f = factorials[value.length - i - 1];
            int rem = (int) (s / f);
            s %= f;
            int idx = -1;
            while (rem >= 0) {
                idx = Long.numberOfTrailingZeros(used >> (idx + 1)) + idx + 1;
                rem--;
            }
            used &= ~(1 << idx);
            value[i] = idx;
        }
        PermutationSpliterator prefixSpliterator = new PermutationSpliterator(newValue, newPos, newRemainingSize);
        remainingSize -= newRemainingSize;
        return prefixSpliterator;
    }

    @Override
    public long estimateSize() {
        return remainingSize;
    }

    @Override
    public int characteristics() {
        return ORDERED | NONNULL | IMMUTABLE | SIZED | SUBSIZED;
    }

}
