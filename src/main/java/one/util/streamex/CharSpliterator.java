/*
 * Copyright 2015, 2016 Tagir Valeev
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
/* package */class CharSpliterator implements Spliterator<String> {
    private final CharSequence source;
    private final char delimiter;
    private int pos;
    private final int fence;
    private int nEmpty;
    private String next;
    private final boolean trimEmpty;

    CharSpliterator(CharSequence source, char delimiter, boolean trimEmpty) {
        this.source = source;
        this.delimiter = delimiter;
        this.fence = source.length();
        this.trimEmpty = trimEmpty;
    }

    // Create prefix spliterator and update suffix fields
    private CharSpliterator(CharSpliterator suffix, int fence, boolean trimEmpty, int suffixNEmpty, int suffixPos) {
        this.source = suffix.source;
        this.delimiter = suffix.delimiter;
        this.fence = fence;
        this.trimEmpty = trimEmpty;
        
        this.pos = suffix.pos;
        suffix.pos = suffixPos;
        this.nEmpty = suffix.nEmpty;
        suffix.nEmpty = suffixNEmpty;
        this.next = suffix.next;
        suffix.next = null;
    }

    private int next(int pos) {
        if (pos == fence)
            return pos;
        if (source instanceof String) {
            int nextPos = ((String) source).indexOf(delimiter, pos);
            return nextPos == -1 ? fence : nextPos;
        }
        while (pos < fence) {
            if (source.charAt(pos) == delimiter)
                return pos;
            pos++;
        }
        return fence;
    }

    @Override
    public boolean tryAdvance(Consumer<? super String> action) {
        if (nEmpty > 0) {
            nEmpty--;
            action.accept("");
            return true;
        }
        if (next != null) {
            action.accept(next);
            next = null;
            return true;
        }
        if (pos > fence) {
            return false;
        }
        int nextPos = next(pos);
        if (trimEmpty) {
            while (nextPos == pos && nextPos != fence) {
                nEmpty++;
                nextPos = next(++pos);
            }
        }
        String str = source.subSequence(pos, nextPos).toString();
        pos = nextPos + 1;
        if (trimEmpty && nextPos == fence && str.isEmpty()) {
            nEmpty = 0; // discard empty strings at the end
            return false;
        }
        if (nEmpty > 0) {
            next = str;
            nEmpty--;
            action.accept("");
        } else
            action.accept(str);
        return true;
    }

    @Override
    public Spliterator<String> trySplit() {
        int mid = (pos + fence) >>> 1;
        int nextPos = next(mid);
        if (nextPos == fence)
            return null;
        if (trimEmpty && nextPos == mid) {
            while (nextPos < fence && source.charAt(nextPos) == delimiter)
                nextPos++;
            return nextPos == fence ? 
                    new CharSpliterator(this, mid, true, 0, nextPos + 1) : 
                        new CharSpliterator(this, mid, false, nextPos - mid - 1, nextPos);
        }
        return new CharSpliterator(this, nextPos, false, 0, nextPos + 1);
    }

    @Override
    public long estimateSize() {
        return pos > fence ? 0 : fence - pos;
    }

    @Override
    public int characteristics() {
        return NONNULL | ORDERED;
    }
}
