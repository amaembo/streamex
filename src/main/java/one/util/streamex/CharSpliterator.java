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
package one.util.streamex;

import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * @author Tagir Valeev
 */
/*package*/ class CharSpliterator implements Spliterator<String> {
    private final CharSequence source;
    private final char delimiter;
    private int pos;
    private final int fence;
    private int nEmpty;
    private String next;
    
    CharSpliterator(CharSequence source, char delimiter) {
        this(source, delimiter, 0, source.length());
    }

    CharSpliterator(CharSequence source, char delimiter, int pos, int fence) {
        this.source = source;
        this.delimiter = delimiter;
        this.pos = pos;
        this.fence = fence;
    }
    
    private int next(int pos) {
        if(source instanceof String) {
            int nextPos = ((String)source).indexOf(delimiter, pos);
            return nextPos == -1 || nextPos > fence ? fence : nextPos;
        }
        while(pos < fence) {
            if(source.charAt(pos) == delimiter)
                return pos;
            pos++;
        }
        return fence;
    }

    @Override
    public boolean tryAdvance(Consumer<? super String> action) {
        if(nEmpty > 0) {
            nEmpty--;
            action.accept("");
            return true;
        }
        if(next != null) {
            action.accept(next);
            next = null;
            return true;
        }
        if(pos == fence) {
            return false;
        }
        int nextPos = next(pos);
        while(nextPos == pos && nextPos != fence) {
            nEmpty++;
            nextPos = next(++pos);
        }
        String str = source.subSequence(pos, nextPos).toString();
        if(nextPos == fence) {
            pos = fence;
            if(fence == source.length()) {
                if(nEmpty > 0 && str.isEmpty()) {
                    nEmpty = 0; // discard empty strings at the end
                    return false;
                }
            }
        } else {
            pos = nextPos+1;
        }
        if(nEmpty > 0) {
            next = str;
            nEmpty--;
            action.accept("");
        } else
            action.accept(str);
        return true;
    }

    @Override
    public Spliterator<String> trySplit() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long estimateSize() {
        return fence-pos;
    }

    @Override
    public int characteristics() {
        return NONNULL | ORDERED;
    }
}
