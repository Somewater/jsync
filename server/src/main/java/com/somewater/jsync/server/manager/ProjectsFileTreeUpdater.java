package com.somewater.jsync.server.manager;

import com.somewater.jsync.core.conf.SharedConf;
import com.somewater.jsync.core.manager.FileTreeUpdater;
import com.somewater.jsync.core.manager.FileTreeWatcher;
import com.somewater.jsync.core.model.Changes;
import com.somewater.jsync.core.model.FileChange;
import com.somewater.jsync.server.conf.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectsFileTreeUpdater {
    private final Path projectsDir;
    private final Map<String, ProjectContext> projects;
    private final Set<String> fileExtensions;
    private final boolean readonly;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final long maxFiles;
    private final long maxFileSize;
    private final long sumFileSize;
    private final long maxProjects;

    public ProjectsFileTreeUpdater(Args args) {
        this.fileExtensions = args.fileExtensions();
        this.readonly = args.readonly();
        this.maxFiles = args.maxFiles();
        this.maxFileSize = args.maxFileSize();
        this.sumFileSize = args.sumFileSize();
        this.maxProjects = args.maxProjects();

        Path defaultProjectDir = Paths.get(System.getProperty("user.dir"), "PROJECTS");
        Path projectsDir = args.projectsDir().map(Paths::get).orElse(defaultProjectDir);
        if (projectsDir.toFile().exists() && !projectsDir.toFile().isDirectory()) {
            log.error("Projects dir not directory: " + projectsDir);
            System.exit(-1);
        } else if (!projectsDir.toFile().exists()) {
            if (!projectsDir.toFile().mkdirs()) {
                log.error("Can not create projects dir: " + projectsDir);
                System.exit(-1);
            }
        }
        this.projectsDir = projectsDir;
        projects = new HashMap<>();
    }

    public void putChanges(String uid, String projectName, Changes changes) {
        if (projects.size() > maxProjects) {
            throw new RuntimeException(String.format("Too many projects: %d", projects.size()));
        }
        ProjectContext project = getOrCreateProject(uid, projectName);
        synchronized (project) {
            if (project.watcher.fileCount() > maxFiles) {
                throw new RuntimeException(String.format("Too many files in project: %d", project.watcher.fileCount()));
            }
            if (project.watcher.maxFileSize() > maxFileSize) {
                throw new RuntimeException(String.format("Too big file in project: %d bytes", project.watcher.maxFileSize()));
            }
            if (project.watcher.sumFileSize() > sumFileSize) {
                throw new RuntimeException(String.format("Too big project size: %d bytes", project.watcher.sumFileSize()));
            }
            project.updater.update(changes);
            var filepaths = Arrays.stream(changes.files).map(l -> l.filepath).collect(Collectors.toList());
            project.watcher.clearCacheForFiles(filepaths);
        }
    }

    public Changes getChanges(String uid, String projectName) {
        if (readonly) {
            new RuntimeException(SharedConf.ERROR_MSG_READONLY_SERVER);
        }
        ProjectContext project = getOrCreateProject(uid, projectName);
        synchronized (project) {
            return new Changes(project.watcher.changedFilesList().toArray(FileChange[]::new));
        }
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
        public final FileTreeWatcher watcher;
        public long version = 0;

        public ProjectContext(FileTreeUpdater updater, FileTreeWatcher watcher) {
            this.updater = updater;
            this.watcher = watcher;
        }
    }
}
