package com.somewater.jsync.core.model;

import java.io.Serializable;

public class ProjectChangesResponse implements Serializable {
    public final ProjectChanges projectChanges;
    public final String errorMessage;

    private ProjectChangesResponse(ProjectChanges projectChanges, String errorMessage) {
        this.projectChanges = projectChanges;
        this.errorMessage = errorMessage;
    }

    public static ProjectChangesResponse success(ProjectChanges projectChanges) {
        return new ProjectChangesResponse(projectChanges, null);
    }

    public static ProjectChangesResponse failed(String errorMessage) {
        return new ProjectChangesResponse(null, errorMessage);
    }
}
