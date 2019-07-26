package com.somewater.jsync.client.controller;

import com.somewater.jsync.client.conf.Args;
import com.somewater.jsync.core.manager.FileTreeUpdater;
import com.somewater.jsync.core.manager.FileTreeWatcher;
import com.somewater.jsync.client.conf.ProjectId;
import com.somewater.jsync.client.network.ServerApi;
import com.somewater.jsync.client.conf.LocalConf;
import com.somewater.jsync.core.model.Changes;
import com.somewater.jsync.core.model.ProjectChanges;
import com.somewater.jsync.core.model.FileChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Controller {
    private final Args args;
    private final ServerApi server;
    private final FileTreeWatcher watcher;
    private final FileTreeUpdater updater;
    private final LocalConf localConf;
    private final ProjectId projectId;
    private final int MaxBatchSize = 10;
    private final int localSleepMs;
    private final int remoteSleepMs;
    private final boolean readonly;
    private final Logger logger = Logger.getLogger(getClass().getName());

    public Controller(Args args, ServerApi server, FileTreeWatcher watcher, FileTreeUpdater updater, LocalConf localConf,
                      ProjectId projectId) {
        this.args = args;
        this.server = server;
        this.watcher = watcher;
        this.updater = updater;
        this.localConf = localConf;
        this.projectId = projectId;

        this.localSleepMs = localConf.getLocalSleepMs();
        this.remoteSleepMs = localConf.getRemoteSleepMs();
        this.readonly = localConf.getReadonly();
    }

    public void start() {
        logger.info("File watching started on: " + watcher.directory);
        long lastRemoteFileCheck = 0;
        while (true) {
            sendAllChanges();
            if (!readonly) {
                long now = System.currentTimeMillis();
                if (now - lastRemoteFileCheck > remoteSleepMs) {
                    receiveChanges();
                    lastRemoteFileCheck = now;
                }
            }

            try {
                Thread.sleep(localSleepMs);
            } catch (InterruptedException e) {
                logger.info("File watching stopped");
                return;
            }
        }
    }

    private void sendAllChanges() {
        ArrayList<Map.Entry<String, Optional<byte[]>>> requestBuilder = new ArrayList<>();
        var files = new Object(){ int value = 0; };
        watcher.changedFiles((filepath, content) -> {
            requestBuilder.add(Map.entry(filepath, content));
            if (content.isPresent()) {
                files.value++;
            }
            if (files.value >= MaxBatchSize) {
                sendChanges(requestBuilder);
                requestBuilder.clear();
            }
        });
        if (!requestBuilder.isEmpty()) {
            sendChanges(requestBuilder);
        }
    }

    private boolean receiveChanges() {
        Optional<ProjectChanges> projectChangesOpt = server.getChanges(localConf.getUid(), projectId.getName());
        if (projectChangesOpt.isPresent() && !projectChangesOpt.get().changes.isEmpty()) {
            var projectChanges = projectChangesOpt.get();
            updater.update(projectChanges.changes);

            if (logger.isLoggable(Level.INFO)) {
                logger.info("Files changed from server:");
                for (var file : projectChanges.changes.files)
                    logger.info(String.format("  %8s %s", file.type, file.filepath));
            }
            return true;
        } else {
            return false;
        }
    }

    private void sendChanges(List<Map.Entry<String, Optional<byte[]>>> requestBuilder) {
        FileChange[] fileChange = requestBuilder.stream().map(entry -> {
            String filepath = entry.getKey();
            Optional<byte[]> content = entry.getValue();
            return content.<FileChange>map(bytes -> new FileChange.CreateFile(filepath, bytes))
                    .orElseGet(() -> new FileChange.DeleteFile(filepath));
        }).toArray(FileChange[]::new);
        Changes changes = new Changes(fileChange);
        server.putChanges(new ProjectChanges(changes, localConf.getUid(), projectId.getName()));

        if (logger.isLoggable(Level.INFO)) {
            logger.info("File changes sended:");
            for (var file : changes.files)
                logger.info(String.format("  %8s %s", file.type, file.filepath));
        }
    }
}
