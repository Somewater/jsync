package com.somewater.jeducation.core.model;

import java.io.Serializable;

/**
 * @author pnaydenov
 */
public abstract class FileChange implements Serializable {
    public final String filepath;
    public final Type type;

    public FileChange(Type type, String filepath) {
        this.type = type;
        this.filepath = filepath;
    }

    public enum Type {
        CREATE,
        DELETE,
        MOVE,
        CHANGE;
    }

    public static final class CreateFile extends FileChange {
        public final byte[] content;

        public CreateFile(String filepath, byte[] content) {
            super(Type.CREATE, filepath);
            this.content = content;
        }
    }

    public static final class DeleteFile extends FileChange {
        public DeleteFile(String filepath) {
            super(Type.DELETE, filepath);
        }
    }

    public static final class MoveFile extends FileChange {
        public final String newFilename;

        public MoveFile(String filepath, String newFilename) {
            super(Type.MOVE, filepath);
            this.newFilename = newFilename;
        }
    }

    public static final class ChangeFile extends FileChange {
        public final LineChanges[] lines;

        public ChangeFile(String filepath, LineChanges[] lines) {
            super(Type.CHANGE, filepath);
            this.lines = lines;
        }
    }
}
