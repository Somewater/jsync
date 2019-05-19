package com.somewater.jeducation.server.manager;

import com.somewater.jeducation.core.manager.FileTreeUpdater;
import com.somewater.jeducation.core.manager.FileTreeWatcher;
import com.somewater.jeducation.core.model.Changes;
import com.somewater.jeducation.core.model.FileChange;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectsFileTreeUpdater {
    private final Path projectsDir;
    private final Map<String, ProjectContext> projects;
    private final Set<String> fileExtensions;
    private final boolean readonly;

    public ProjectsFileTreeUpdater(Optional<String> projectsDirArg,
                                   Set<String> fileExtensions,
                                   boolean readonly) {
        this.fileExtensions = fileExtensions;
        this.readonly = readonly;

        Path defaultProjectDir = Paths.get(System.getProperty("user.dir"), "PROJECTS");
        Path projectsDir = projectsDirArg.map(Paths::get).orElse(defaultProjectDir);
        if (projectsDir.toFile().exists() && !projectsDir.toFile().isDirectory()) {
            System.out.println("Projects dir not directory: " + projectsDir);
            System.exit(-1);
        } else if (!projectsDir.toFile().exists()) {
            if (!projectsDir.toFile().mkdirs()) {
                System.out.println("Cannot create projects dir: " + projectsDir);
                System.exit(-1);
            }
        }
        this.projectsDir = projectsDir;
        projects = new HashMap<>();
    }

    public void putChanges(String uid, String projectName, Changes changes) {
        ProjectContext project = getOrCreateProject(uid, projectName);
        synchronized (project) {
            project.updater.update(changes);
            var filepaths = Arrays.stream(changes.files).map(l -> l.filepath).collect(Collectors.toList());
            project.watcher.ifPresent(w -> w.clearCacheForFiles(filepaths));
        }
    }

    public Changes getChanges(String uid, String projectName) {
        ProjectContext project = getOrCreateProject(uid, projectName);
        return project.watcher
                .map(watcher -> new Changes(project.watcher.get().changedFilesList().toArray(FileChange[]::new)))
                .orElseThrow(() -> new RuntimeException("Server works in readonly mode: " +
                        "can't fetch changes from server file tree"));
    }

    private Path tryCreateProjectDir(String uid, String projectName) {
        Path projectRoot = projectsDir.resolve(uid).resolve(projectName);
        if (!projectRoot.toFile().exists()) {
            if (!projectRoot.toFile().mkdirs()) {
                throw new RuntimeException("Caoont create project dir: " + projectRoot);
            }
        }
        return projectRoot;
    }

    private ProjectContext getOrCreateProject(String uid, String projectName) {
        String key = uid + ":" + projectName;
        Path projectDir = tryCreateProjectDir(uid, projectName);
        ProjectContext project;
        synchronized (projects) {
            project = projects.get(key);
            if (project == null) {
                project = new ProjectContext(new FileTreeUpdater(projectDir),
                        (readonly ? null : new FileTreeWatcher(projectDir, fileExtensions)));
                projects.put(key, project);
            }
        }
        return project;
    }

    private static class ProjectContext {
        public final FileTreeUpdater updater;
        public final Optional<FileTreeWatcher> watcher;
        public long version = 0;

        public ProjectContext(FileTreeUpdater updater, FileTreeWatcher watcher) {
            this.updater = updater;
            this.watcher = Optional.ofNullable(watcher);
        }
    }
}
