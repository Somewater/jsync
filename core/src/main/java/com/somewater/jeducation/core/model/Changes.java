package com.somewater.jeducation.core.model;

import java.io.Serializable;

/**
 * @author pnaydenov
 */
public final class Changes implements Serializable {
    public final FileChange[] files;

    public Changes(FileChange[] files) {
        this.files = files;
    }

    public boolean isEmpty() {
        return files.length == 0;
    }
}
