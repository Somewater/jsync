package com.somewater.jeducation.core.model;

import java.io.Serializable;

/**
 * @author pnaydenov
 */
public final class Line implements Serializable {
    public final int index;
    public final String value;

    public Line(int index, String value) {
        this.index = index;
        this.value = value;
    }
}
