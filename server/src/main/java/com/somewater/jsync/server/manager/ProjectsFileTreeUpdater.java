package com.somewater.jsync.server.manager;

import com.somewater.jsync.core.conf.SharedConf;
import com.somewater.jsync.core.manager.FileTreeUpdater;
import com.somewater.jsync.core.manager.FileTreeWatcher;
import com.somewater.jsync.core.model.Changes;
import com.somewater.jsync.core.model.FileChange;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
                System.out.println("Can not create projects dir: " + projectsDir);
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
                .orElseThrow(() -> new RuntimeException(SharedConf.ERROR_MSG_READONLY_SERVER));
    }

    private Path tryCreateProjectDir(String uid, String projectName) {
        Path projectRoot = projectsDir.resolve(uid).resolve(projectName);
        if (!projectRoot.toFile().exists()) {
            if (!projectRoot.toFile().mkdirs()) {
                throw new RuntimeException("Can not create project dir: " + projectRoot);
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
