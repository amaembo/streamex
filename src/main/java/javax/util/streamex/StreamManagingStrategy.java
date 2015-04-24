package javax.util.streamex;

import java.util.Map.Entry;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/* package */interface StreamManagingStrategy {
    static class CustomPoolStreamManagingStrategy implements StreamManagingStrategy {
        private final ForkJoinPool fjp;

        public CustomPoolStreamManagingStrategy(ForkJoinPool fjp) {
            this.fjp = fjp;
        }

        @Override
        public <T> StreamEx<T> newStreamEx(Stream<T> src) {
            return new StreamEx<T>(src) {
                @Override
                StreamManagingStrategy strategy() {
                    return CustomPoolStreamManagingStrategy.this;
                }
            };
        }

        @Override
        public <K, V> EntryStream<K, V> newEntryStream(Stream<Entry<K, V>> src) {
            return new EntryStream<K, V>(src) {
                @Override
                StreamManagingStrategy strategy() {
                    return CustomPoolStreamManagingStrategy.this;
                }
            };
        }

        @Override
        public LongStreamEx newLongStreamEx(LongStream src) {
            return new LongStreamEx(src) {
                @Override
                StreamManagingStrategy strategy() {
                    return CustomPoolStreamManagingStrategy.this;
                }
            };
        }

        @Override
        public IntStreamEx newIntStreamEx(IntStream src) {
            return new IntStreamEx(src) {
                @Override
                StreamManagingStrategy strategy() {
                    return CustomPoolStreamManagingStrategy.this;
                }
            };
        }

        @Override
        public DoubleStreamEx newDoubleStreamEx(DoubleStream src) {
            return new DoubleStreamEx(src) {
                @Override
                StreamManagingStrategy strategy() {
                    return CustomPoolStreamManagingStrategy.this;
                }
            };
        }

        @Override
        public <T> T terminate(Supplier<T> terminalOperation) {
            ForkJoinTask<T> task = fjp.submit(terminalOperation::get);
            return task.join();
        }
    }

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
    
    static StreamManagingStrategy forCustomPool(ForkJoinPool fjp) {
        return new CustomPoolStreamManagingStrategy(fjp);
    }
}
