package com.somewater.jeducation.client.controller;

import com.somewater.jeducation.client.conf.Args;
import com.somewater.jeducation.core.manager.FileTreeUpdater;
import com.somewater.jeducation.core.manager.FileTreeWatcher;
import com.somewater.jeducation.client.conf.ProjectId;
import com.somewater.jeducation.client.network.ServerApi;
import com.somewater.jeducation.client.conf.LocalConf;
import com.somewater.jeducation.core.model.Changes;
import com.somewater.jeducation.core.model.ProjectChanges;
import com.somewater.jeducation.core.model.FileChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Controller {
    private final Args args;
    private final ServerApi server;
    private final FileTreeWatcher watcher;
    private final FileTreeUpdater updater;
    private final LocalConf localConf;
    private final ProjectId projectId;
    private final int MaxBatchSize = 10;
    private final int SleepBetweenFileChecksMs = 1000;

    public Controller(Args args, ServerApi server, FileTreeWatcher watcher, FileTreeUpdater updater, LocalConf localConf,
                      ProjectId projectId) {
        this.args = args;
        this.server = server;
        this.watcher = watcher;
        this.updater = updater;
        this.localConf = localConf;
        this.projectId = projectId;
    }

    public void start() {
        System.out.println("File watching started on: " + watcher.directory);
        while (true) {
            sendAllChanges();
            if (!args.readonly()) {
                receiveChanges();
            }

            try {
                Thread.sleep(SleepBetweenFileChecksMs);
            } catch (InterruptedException e) {
                System.out.println("File watching stopped");
                return;
            }
        }
    }

    private void sendAllChanges() {
        ArrayList<Map.Entry<String, Optional<String>>> requestBuilder = new ArrayList<>();
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

    private void receiveChanges() {
        ProjectChanges projectChanges = server.getChanges(localConf.getUid(), projectId.getName());
        updater.update(projectChanges.changes);
    }

    private void sendChanges(List<Map.Entry<String, Optional<String>>> requestBuilder) {
        FileChange[] fileChange = requestBuilder.stream().map(entry -> {
            String filepath = entry.getKey();
            Optional<String> content = entry.getValue();
            return content.<FileChange>map(s -> new FileChange.CreateFile(filepath, s.getBytes()))
                    .orElseGet(() -> new FileChange.DeleteFile(filepath));
        }).toArray(FileChange[]::new);
        Changes changes = new Changes(fileChange);
        server.putChanges(new ProjectChanges(changes, localConf.getUid(), projectId.getName()));
    }
}
