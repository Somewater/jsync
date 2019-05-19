package com.somewater.jeducation.server.controller;

import com.somewater.jeducation.core.model.Changes;
import com.somewater.jeducation.core.model.FileChange;
import com.somewater.jeducation.core.model.ProjectChanges;
import com.somewater.jeducation.core.model.ProjectChangesResponse;
import com.somewater.jeducation.core.util.SerializationUtil;
import com.somewater.jeducation.core.manager.FileTreeUpdater;
import com.somewater.jeducation.core.util.StringUtil;
import com.somewater.jeducation.server.manager.ProjectsFileTreeUpdater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.io.IOException;

@RestController
@RequestMapping("/v1")
public class ChangesReceiverController {
    @Autowired
    ProjectsFileTreeUpdater projectsFileTreeUpdater;

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
            return SerializationUtil.objectToBytes(ProjectChangesResponse
                    .success(new ProjectChanges(changes, uid, projectName)));
        } catch (Throwable e) {
            return SerializationUtil.objectToBytes(ProjectChangesResponse.failed(e.getMessage()));
        }
    }
}
