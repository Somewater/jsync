package com.somewater.jeducation.core.manager;

import com.somewater.jeducation.core.model.Changes;
import com.somewater.jeducation.core.model.FileChange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileTreeUpdater {

    private final Path projectDir;

    public FileTreeUpdater(Path projectDir) {
        this.projectDir = projectDir;
    }

    public void update(Changes changes) {
        for (var fileChange : changes.files) {
            Path filepath = projectDir.resolve(Paths.get(fileChange.filepath));
            switch (fileChange.type) {
                case CREATE:
                    createFile((FileChange.CreateFile) fileChange, filepath);
                    break;
                case DELETE:
                    deleteFile((FileChange.DeleteFile) fileChange, filepath);
                    break;
                default:
                    throw new UnsupportedOperationException("File change type " + fileChange.type
                            + " not supported for this version of server");
            }
        }
    }

    private void createFile(FileChange.CreateFile change, Path filepath) {
        try(var writer = Files.newOutputStream(filepath)) {
            writer.write(change.content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteFile(FileChange.DeleteFile change, Path filepath) {
        if (filepath.toFile().exists() && filepath.toFile().isFile()) {
            filepath.toFile().delete();
        }
    }


}
