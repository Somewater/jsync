package com.somewater.jsync.idea;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.VetoableProjectManagerListener;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.util.messages.MessageBusConnection;
import com.somewater.jsync.core.conf.LocalConf;
import com.somewater.jsync.core.model.Changes;
import com.somewater.jsync.core.model.FileChange;
import com.somewater.jsync.core.model.ProjectChanges;
import com.somewater.jsync.core.util.ArgsParser;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ControllerImpl implements IController,
        DocumentListener,
        VetoableProjectManagerListener,
        Disposable {
    private JSyncServerApi jSyncServerApi = null;
    private final AtomicBoolean initialStart = new AtomicBoolean(true);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean sendScheduled = new AtomicBoolean(false);
    private final Map<Project, MessageBusConnection> projectConnections = new WeakHashMap<>();
    private final Map<Project, ChangeBuffer> changes = new HashMap<>();
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final Logger log = LoggerFactory.getLogger(getClass());

    private ScheduledFuture periodicalChangesSync = null;
    private ScheduledFuture periodicalFullSync = null;

    private void initialize() {
        JSyncConfService jSyncConfService = getConfService();
        jSyncServerApi = new JSyncServerApi(jSyncConfService.getHostPort());
    }

    @Override
    public void start() {
        if (initialStart.getAndSet(false)) {
            initialize();
        }
        addListeners();
        enabled.set(true);
        periodicalChangesSync = executor
                .scheduleAtFixedRate(this::tryToSendChanges, 5000, 5000, TimeUnit.MILLISECONDS);
        periodicalFullSync = executor
                .scheduleAtFixedRate(this::reinitAllProjects, 1, 1, TimeUnit.MINUTES);
        log.info("Sync started");
    }

    @Override
    public void stop() {
        removeListeners();
        enabled.set(false);
        if (periodicalChangesSync != null) {
            periodicalChangesSync.cancel(false);
            periodicalChangesSync = null;
        }
        if (periodicalFullSync != null) {
            periodicalFullSync.cancel(false);
            periodicalFullSync = null;
        }
        log.info("Sync paused");
    }

    @Override
    public boolean isEnabled() {
        return enabled.get();
    }

    private void addListeners() {
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(this, () -> {});
        ProjectManager.getInstance().addProjectManagerListener(this);
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            projectOpened(project);
        }
    }

    private void removeListeners() {
        EditorFactory.getInstance().getEventMulticaster().removeDocumentListener(this);
        ProjectManager.getInstance().removeProjectManagerListener(this);
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed())
                projectClosed(project);
        }
    }

    private JSyncConfService getConfService() {
        return ServiceManager.getService(JSyncConfService.class);
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        // Document
        VirtualFile file = FileDocumentManager.getInstance().getFile(event.getDocument());
        if (file != null && file.isInLocalFileSystem()) {
            String ext = file.getExtension();
            if (ext != null && ArgsParser.DefaultExts.contains(ext)) {
                // TODO: change file event
                for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                    if (!project.isDisposed() && project.getBasePath() != null
                            && file.getPath().startsWith(project.getBasePath())) {
                        getOrCreate(project).add(new FileChange.CreateFile(
                                toRelativePath(file.getPath()), event.getDocument().getText().getBytes()));
                        tryToSendChanges();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void projectOpened(@NotNull Project project) {
        if (isProjectSyncable(project)) {
            MessageBusConnection c = project.getMessageBus().connect();
            c.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
                @Override
                public void after(@NotNull List<? extends VFileEvent> events) {
                    afterVFSChange(project, events);
                }
            });
            projectConnections.put(project, c);

            initProjectFiles(project);
        }
    }

    private void reinitAllProjects() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (isProjectSyncable(project)) {
                initProjectFiles(project);
            }
        }
    }

    private boolean isProjectSyncable(Project project) {
        // TODO
        return true;
    }

    private void initProjectFiles(@NotNull Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            public void run() {
                ApplicationManager.getApplication().runReadAction(new Runnable() {
                    public void run() {
                        ProjectFileIndex.getInstance(project).iterateContent(fileOrDir -> {
                            Document document = FileDocumentManager.getInstance().getDocument(fileOrDir);
                            if (document == null) {
                                log.error(fileOrDir.getPath() + " has no content and ignored");
                            } else {
                                getOrCreate(project).add(
                                        new FileChange.CreateFile(toRelativePath(fileOrDir.getPath()),
                                                document.getText().getBytes()));
                            }
                            return true;
                        }, file -> {
                            if (!file.isDirectory()) {
                                String ext = file.getExtension();
                                return ext != null && ArgsParser.DefaultExts.contains(ext);
                            } else {
                                return false;
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void projectClosed(@NotNull Project project) {
        MessageBusConnection c = projectConnections.remove(project);
        if (c != null) {
            c.disconnect();
        }
    }


    @Override
    public void dispose() {

    }

    @Override
    public boolean canClose(@NotNull Project project) {
        return true;
    }

    private void afterVFSChange(Project project, @NotNull List<? extends VFileEvent> events) {
        // VFS
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file == null) continue;
            String ext = file.getExtension();
            if (ext == null || !ArgsParser.DefaultExts.contains(ext)) continue;

            if (event instanceof VFileCreateEvent
                    || event instanceof VFileCopyEvent
                    || event instanceof VFileContentChangeEvent) {
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document != null) {
                    getOrCreate(project).add(new FileChange.CreateFile(toRelativePath(event.getPath()),
                            document.getText().getBytes()));
                    tryToSendChanges();
                }
            } else if (event instanceof VFileDeleteEvent) {
                getOrCreate(project).add(new FileChange.DeleteFile(toRelativePath(event.getPath())));
                tryToSendChanges();
            } else if (event instanceof VFileMoveEvent) {
                VFileMoveEvent moveEvent = (VFileMoveEvent) event;
                // TODO
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document != null) {
                    getOrCreate(project).add(new FileChange.DeleteFile(toRelativePath(moveEvent.getOldPath())));
                    getOrCreate(project).add(new FileChange.CreateFile(toRelativePath(moveEvent.getNewPath()),
                            document.getText().getBytes()));
                    tryToSendChanges();
                }
            } else if (event instanceof VFilePropertyChangeEvent) {
                VFilePropertyChangeEvent propertyChangeEvent = (VFilePropertyChangeEvent) event;
                if (propertyChangeEvent.getPropertyName().equals("name")) {
                    // TODO: like MOVE code above
                    Document document = FileDocumentManager.getInstance().getDocument(file);
                    if (document != null) {
                        getOrCreate(project).add(new FileChange.DeleteFile(toRelativePath(propertyChangeEvent.getOldPath())));
                        getOrCreate(project).add(new FileChange.CreateFile(toRelativePath(propertyChangeEvent.getNewPath()),
                                document.getText().getBytes()));
                        tryToSendChanges();
                    }
                }
            }
        }
    }

    private ChangeBuffer getOrCreate(Project project) {
        ChangeBuffer buffer = changes.get(project);
        if (buffer == null) {
            buffer = new ChangeBuffer();
            changes.put(project, buffer);
        }
        return buffer;
    }

    private String toRelativePath(String path) {
        for (Project project : projectConnections.keySet()) {
            if (project.getBasePath() != null && path.startsWith(project.getBasePath())) {
                return path.substring(project.getBasePath().length() + 1);
            }
        }
        return path;
    }

    private void tryToSendChanges() {
        boolean hasChanges = false;
        for (Map.Entry<Project, ChangeBuffer> entry : changes.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                hasChanges = true;
                break;
            }
        }
        if (hasChanges && sendScheduled.compareAndSet(false, true)) {
            executor.schedule(() -> {
                try {
                    sendChanges();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
                sendScheduled.set(false);
            }, 2000, TimeUnit.MILLISECONDS);
        }
    }

    private void sendChanges() {
        LocalConf conf = getConfService().getConf();

        for (Map.Entry<Project, ChangeBuffer> c : this.changes.entrySet()) {
            Project project = c.getKey();
            ChangeBuffer buffer = c.getValue();
            FileChange[] fileChanges = buffer.getResultAndClear();
            ProjectChanges projectChanges = new ProjectChanges(
                    new Changes(fileChanges),
                    conf.getUid(), project.getName());
            synchronized (jSyncServerApi) {
                try {
                    jSyncServerApi.putChanges(projectChanges);
                    log.info("Project " + project.getName() + " synced");
                } catch (Throwable ex) {
                    for (FileChange fc : fileChanges) {
                        buffer.prepend(fc);
                    }
                    log.error("Project " + project.getName() + " sync failure", ex);
                }
            }
        }
    }
}
