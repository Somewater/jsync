package com.somewater.jsync.core.model;

import java.io.Serializable;

public class ProjectChanges implements Serializable {
    public final Changes changes;
    public final String uid;
    public final String projectName;

    public ProjectChanges(Changes changes, String uid, String projectName) {
        this.changes = changes;
        this.uid = uid;
        this.projectName = projectName;
    }
}
