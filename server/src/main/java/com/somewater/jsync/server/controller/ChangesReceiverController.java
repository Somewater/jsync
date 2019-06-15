package com.somewater.jsync.server.controller;

import com.somewater.jsync.core.model.Changes;
import com.somewater.jsync.core.model.FileChange;
import com.somewater.jsync.core.model.ProjectChanges;
import com.somewater.jsync.core.model.ProjectChangesResponse;
import com.somewater.jsync.core.util.SerializationUtil;
import com.somewater.jsync.core.util.StringUtil;
import com.somewater.jsync.server.manager.ProjectsFileTreeUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/v1")
public class ChangesReceiverController {
    @Autowired
    ProjectsFileTreeUpdater projectsFileTreeUpdater;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @PutMapping(path = "/changes", consumes = "application/octet-stream", produces = "text/plain")
    @ResponseBody
    public String putChanges(@RequestBody byte[] data) {
        try {
            if (data.length > 10 * 1024 * 1024)
                throw new RuntimeException("Changes size too big: " + data.length + " bytes");
            ProjectChanges projectChanges = null;
            try {
                projectChanges = SerializationUtil.bytesToObject(data);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Data parsing exception", e);
            }
            if (!StringUtil.removeUnsupportedPathSymbols(projectChanges.uid).equals(projectChanges.uid))
                throw new RuntimeException("Unsupported symbol in uid: " + projectChanges.uid);

            if (!StringUtil.removeUnsupportedPathSymbols(projectChanges.projectName).equals(projectChanges.projectName))
                throw new RuntimeException("Unsupported symbol in projectName: " + projectChanges.projectName);
            projectsFileTreeUpdater.putChanges(projectChanges.uid, projectChanges.projectName, projectChanges.changes);
            logProjectChanges(false, projectChanges);
            return "OK";
        } catch (Throwable e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    @GetMapping(path = "/changes/{uid}/{projectName}", produces = "application/octet-stream")
    @ResponseBody
    public byte[] getChanges(@PathVariable ("uid") String uid,
                             @PathVariable ("projectName") String projectName) {
        try {
            Changes changes = projectsFileTreeUpdater.getChanges(uid, projectName);
            var projectChanges = new ProjectChanges(changes, uid, projectName);
            logProjectChanges(true, projectChanges);
            return SerializationUtil.objectToBytes(ProjectChangesResponse.success(projectChanges));
        } catch (Throwable e) {
            return SerializationUtil.objectToBytes(ProjectChangesResponse.failed(e.getMessage()));
        }
    }

    private void logProjectChanges(boolean getAction, ProjectChanges projectChanges) {
        String uid = projectChanges.uid;
        String projectName = projectChanges.projectName;
        String actionSign = getAction ? "<" : ">";
        if (log.isInfoEnabled() && projectChanges.changes.files.length > 0) {
            log.info("{} Files changes in {}/{}: {}", actionSign, uid, projectName, projectChanges.changes.files.length);
            for (var fileChange : projectChanges.changes.files) {
                String details = "";
                if (fileChange.type == FileChange.Type.CREATE) {
                    details = String.format("bytes=%d", ((FileChange.CreateFile) fileChange).content.length);
                }
                if (details.isEmpty()) {
                    log.info("{} {} {}/{}/{}", actionSign, fileChange.type.name(), uid, projectName, fileChange.filepath);
                } else {
                    log.info("{} {} {}/{}/{} : {}", actionSign, fileChange.type.name(), uid, projectName, fileChange.filepath, details);
                }
            }
        }
    }
}
