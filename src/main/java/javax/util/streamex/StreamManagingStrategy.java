package javax.util.streamex;

import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

interface StreamManagingStrategy {
    <T> StreamEx<T> newStreamEx(Stream<T> src);
    <K, V> EntryStream<K, V> newEntryStream(Stream<Entry<K, V>> src);
    IntStreamEx newIntStreamEx(IntStream src);
    LongStreamEx newLongStreamEx(LongStream src);
    DoubleStreamEx newDoubleStreamEx(DoubleStream src);
    <T> T terminate(Supplier<T> terminalOperation);
    
    static final StreamManagingStrategy DEFAULT = new StreamManagingStrategy() {
        @Override
        public <T> StreamEx<T> newStreamEx(Stream<T> src) {
            return new StreamEx<>(src);
        }
        
        @Override
        public <K, V> EntryStream<K, V> newEntryStream(Stream<Entry<K, V>> src) {
            return new EntryStream<>(src);
        }

        @Override
        public LongStreamEx newLongStreamEx(LongStream src) {
            return new LongStreamEx(src);
        }
        
        @Override
        public IntStreamEx newIntStreamEx(IntStream src) {
            return new IntStreamEx(src);
        }
        
        @Override
        public DoubleStreamEx newDoubleStreamEx(DoubleStream src) {
            return new DoubleStreamEx(src);
        }

        @Override
        public <T> T terminate(Supplier<T> terminalOperation) {
            return terminalOperation.get();
        }
    };
}
