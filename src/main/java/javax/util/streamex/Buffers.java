package javax.util.streamex;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/* package */ final class Buffers {
    static final int INITIAL_SIZE = 128;
    
    static final class ByteBuffer {
        int size = 0;
        byte[] data;
        
        ByteBuffer() {
            data = new byte[INITIAL_SIZE];
        }
        
        ByteBuffer(int size) {
            data = new byte[size];
        }
        
        void add(int n) {
            if(data.length == size) {
                byte[] newData = new byte[data.length*2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = (byte) n;
        }
        
        void addUnsafe(int n) {
            data[size++] = (byte) n;
        }
        
        void addAll(ByteBuffer buf) {
            if(data.length < buf.size+size) {
                byte[] newData = new byte[buf.size+size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }
        
        byte[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
    }
    
    static final class CharBuffer {
        int size = 0;
        char[] data;
        
        CharBuffer() {
            data = new char[INITIAL_SIZE];
        }
        
        CharBuffer(int size) {
            data = new char[size];
        }
        
        void add(int n) {
            if(data.length == size) {
                char[] newData = new char[data.length*2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = (char) n;
        }
        
        void addUnsafe(int n) {
            data[size++] = (char) n;
        }
        
        void addAll(CharBuffer buf) {
            if(data.length < buf.size+size) {
                char[] newData = new char[buf.size+size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }
        
        char[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
        
        @Override
        public String toString() {
            return new String(data, 0, size);
        }
    }
    
    static final class ShortBuffer {
        int size = 0;
        short[] data;
        
        ShortBuffer() {
            data = new short[INITIAL_SIZE];
        }
        
        ShortBuffer(int size) {
            data = new short[size];
        }
        
        void add(int n) {
            if(data.length == size) {
                short[] newData = new short[data.length*2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = (short) n;
        }
        
        void addUnsafe(int n) {
            data[size++] = (short) n;
        }
        
        void addAll(ShortBuffer buf) {
            if(data.length < buf.size+size) {
                short[] newData = new short[buf.size+size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }
        
        short[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
    }
    
    static final class FloatBuffer {
        int size = 0;
        float[] data;
        
        FloatBuffer() {
            data = new float[INITIAL_SIZE];
        }
        
        FloatBuffer(int size) {
            data = new float[size];
        }
        
        void add(double n) {
            if(data.length == size) {
                float[] newData = new float[data.length*2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = (float) n;
        }
        
        void addUnsafe(double n) {
            data[size++] = (float) n;
        }
        
        void addAll(FloatBuffer buf) {
            if(data.length < buf.size+size) {
                float[] newData = new float[buf.size+size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }
        
        float[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
    }

    static final class IntBuffer {
        int size = 0;
        int[] data;
        
        IntBuffer() {
            data = new int[INITIAL_SIZE];
        }
        
        void add(int n) {
            if(data.length == size) {
                int[] newData = new int[data.length*2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = n;
        }
        
        void addAll(IntBuffer buf) {
            if(data.length < buf.size+size) {
                int[] newData = new int[buf.size+size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }
        
        int[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
    }
    
    static final class LongBuffer {
        int size = 0;
        long[] data;
        
        LongBuffer() {
            data = new long[INITIAL_SIZE];
        }
        
        void add(long n) {
            if(data.length == size) {
                long[] newData = new long[data.length*2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = n;
        }
        
        void addAll(LongBuffer buf) {
            if(data.length < buf.size+size) {
                long[] newData = new long[buf.size+size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }
        
        long[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
    }
    
    static final class DoubleBuffer {
        int size = 0;
        double[] data;
        
        DoubleBuffer() {
            data = new double[INITIAL_SIZE];
        }
        
        void add(double n) {
            if(data.length == size) {
                double[] newData = new double[data.length*2];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = n;
        }
        
        void addAll(DoubleBuffer buf) {
            if(data.length < buf.size+size) {
                double[] newData = new double[buf.size+size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            System.arraycopy(buf.data, 0, data, size, buf.size);
            size += buf.size;
        }
        
        double[] toArray() {
            return data.length == size ? data : Arrays.copyOfRange(data, 0, size);
        }
    }

    static final class Partition<T> extends AbstractMap<Boolean, T> implements Map<Boolean, T> {
        final T forTrue;
        final T forFalse;

        Partition(T forTrue, T forFalse) {
            this.forTrue = forTrue;
            this.forFalse = forFalse;
        }

        @Override
        public Set<Map.Entry<Boolean, T>> entrySet() {
            return new AbstractSet<Map.Entry<Boolean, T>>() {
                @Override
                public Iterator<Map.Entry<Boolean, T>> iterator() {
                    Map.Entry<Boolean, T> falseEntry = new SimpleImmutableEntry<>(false, forFalse);
                    Map.Entry<Boolean, T> trueEntry = new SimpleImmutableEntry<>(true, forTrue);
                    return Arrays.asList(falseEntry, trueEntry).iterator();
                }

                @Override
                public int size() {
                    return 2;
                }
            };
        }
    }
}
