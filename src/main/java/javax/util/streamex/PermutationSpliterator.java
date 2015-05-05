package javax.util.streamex;

import java.util.Spliterator;
import java.util.function.Consumer;

final class PermutationSpliterator implements Spliterator<int[]> {
    private static final long[] factorials = new long[] { 1L, 1L, 2L, 6L, 24L, 120L, 720L, 5040L, 40320L, 362880L,
        3628800L, 39916800L, 479001600L, 6227020800L, 87178291200L, 1307674368000L, 20922789888000L,
        355687428096000L, 6402373705728000L, 121645100408832000L, 2432902008176640000L };

    private final int[] value;
    private int nFixed;
    private int hi;
    private long remainingSize;
    
    public PermutationSpliterator(int length) {
        this(init(length), 0, 0, length -1);
    }

    private static int[] init(int length) {
        int[] initValue = new int[length];
        for(int i=0; i<length; i++)
            initValue[i] = i;
        return initValue;
    }

    public PermutationSpliterator(int[] startValue, int nFixed, int lo, int hi) {
        this.value = startValue;
        this.nFixed = nFixed;
        this.hi = hi;
        int len = startValue.length - nFixed;
        if(len > factorials.length)
            remainingSize = Long.MAX_VALUE;
        else
            remainingSize = factorials[len - 1] * (hi - lo + 1);
    }
    
    @Override
    public boolean tryAdvance(Consumer<? super int[]> action) {
        if(remainingSize == 0)
            return false;
        action.accept(value);
        int k = value.length-2;
        while(k >= nFixed && value[k] > value[k+1])
            k--;
        if (k < nFixed) {
            remainingSize = 0;
            return true;
        }
        int vk = value[k];
        if (k == nFixed && vk == hi) {
            remainingSize = 0;
            return true;
        }
        int l = value.length-1;
        while(vk > value[l])
            l--;
        value[k] = value[l];
        value[l] = vk;
        if (k == nFixed && value[k] == hi) {
            nFixed++;
            hi = value.length - 1;
        }
        int rem = value.length + k + 1;
        for(int i=k+1; i<rem/2; i++)
        {
            int tmp = value[i];
            value[i] = value[rem - 1 - i];
            value[rem - 1 - i] = tmp;
        }
        remainingSize--;
        return true;
    }

    @Override
    public Spliterator<int[]> trySplit() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long estimateSize() {
        return remainingSize;
    }

    @Override
    public int characteristics() {
        return ORDERED | NONNULL | IMMUTABLE | (remainingSize < Long.MAX_VALUE ? SIZED | SUBSIZED : null);
    }

}
