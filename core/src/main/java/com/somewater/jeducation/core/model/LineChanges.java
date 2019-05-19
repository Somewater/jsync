package com.somewater.jeducation.core.model;

import java.io.Serializable;

/**
 * @author pnaydenov
 */
public abstract class LineChanges implements Serializable {
    public final Type type;
    public final int beginIndex;
    public final int endIndex;

    public LineChanges(Type type, int beginIndex, int endIndex) {
        this.type = type;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    public enum Type {
        ADD,
        DELETE,
        CHANGE
    }

    public static final class AddLines extends LineChanges {
        public AddLines(Line[] lines) {
            super(Type.ADD, minLine(lines), maxLine(lines));
        }
    }
    public static final class DeleteLines extends LineChanges {
        public DeleteLines(int beginIndex, int endIndex) {
            super(Type.DELETE, beginIndex, endIndex);
        }
    }
    public static final class ChangeLines extends LineChanges {
        public ChangeLines(Line[] lines) {
            super(Type.CHANGE, minLine(lines), maxLine(lines));
        }
    }

    private static int minLine(Line[] lines) {
        int min = -1;
        int endIndex = -1;
        for (Line line : lines) {
            if (min == -1 || min > line.index) {
                min = line.index;
            }
        }
        return min;
    }

    private static int maxLine(Line[] lines) {
        int max = -1;
        int endIndex = -1;
        for (Line line : lines) {
            if (max == -1 || max < line.index) {
                max = line.index;
            }
        }
        return max;
    }
}
