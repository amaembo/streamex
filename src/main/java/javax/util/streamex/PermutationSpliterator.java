package javax.util.streamex;

import java.util.BitSet;
import java.util.Spliterator;
import java.util.function.Consumer;

final class PermutationSpliterator implements Spliterator<int[]> {
    private static final long[] factorials = new long[] { 1L, 1L, 2L, 6L, 24L, 120L, 720L, 5040L, 40320L, 362880L,
            3628800L, 39916800L, 479001600L, 6227020800L, 87178291200L, 1307674368000L, 20922789888000L,
            355687428096000L, 6402373705728000L, 121645100408832000L, 2432902008176640000L };

    private final int[] value;
    private long remainingSize;
    private long pos;

    public PermutationSpliterator(int length) {
        this(init(length), 0, getSize(length));
    }

    private static long getSize(int length) {
        if (length > factorials.length)
            throw new IllegalArgumentException("Length is too big");
        return factorials[length];
    }

    private static int[] init(int length) {
        int[] initValue = new int[length];
        for (int i = 0; i < length; i++)
            initValue[i] = i;
        return initValue;
    }

    public PermutationSpliterator(int[] startValue, long pos, long remainingSize) {
        this.value = startValue;
        this.pos = pos;
        this.remainingSize = remainingSize;
    }

    @Override
    public boolean tryAdvance(Consumer<? super int[]> action) {
        if (remainingSize == 0)
            return false;
        action.accept(value);
        remainingSize--;
        pos++;
        if (remainingSize > 0) {
            int k = value.length - 2;
            while (value[k] > value[k + 1])
                k--;
            int vk = value[k];
            int l = value.length - 1;
            while (vk > value[l])
                l--;
            value[k] = value[l];
            value[l] = vk;
            int rem = value.length + k + 1;
            for (int i = k + 1; i < rem / 2; i++) {
                int tmp = value[i];
                value[i] = value[rem - 1 - i];
                value[rem - 1 - i] = tmp;
            }
        }
        return true;
    }

    @Override
    public Spliterator<int[]> trySplit() {
        if (remainingSize <= 1)
            return null;
        int[] newValue = value.clone();
        BitSet b = new BitSet();
        long newRemainingSize = remainingSize / 2;
        long newPos = pos + newRemainingSize;
        long s = newPos;
        for (int i = 0; i < value.length; i++) {
            long f = factorials[value.length - i - 1];
            int rem = (int)(s / f);
            s %= f;
            int idx = -1;
            while (rem >= 0) {
                idx = b.nextClearBit(idx+1);
                rem--;
            }
            b.set(idx);
            value[i] = idx;
        }
        PermutationSpliterator prefixSpliterator = new PermutationSpliterator(newValue, pos, newRemainingSize);
        remainingSize = remainingSize - newRemainingSize;
        pos = newPos;
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
