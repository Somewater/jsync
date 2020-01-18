package com.somewater.jsync.idea;

import com.somewater.jsync.core.model.FileChange;

import java.util.*;

// TODO: only creates and deletes supported!
public class ChangeBuffer {
    private final Map<String, FileChange> buffer = new HashMap<>();

    public synchronized void prepend(FileChange change) {
        if (!buffer.containsKey(change.filepath)) {
            buffer.put(change.filepath, change);
        }
    }

    public synchronized boolean isEmpty() {
        return buffer.isEmpty();
    }

    public synchronized void add(FileChange change) {
        buffer.put(change.filepath, change);
    }

    public synchronized FileChange[] getResultAndClear() {
        FileChange[] result = buffer.values().toArray(new FileChange[0]);
        buffer.clear();
        return result;
    }
}
