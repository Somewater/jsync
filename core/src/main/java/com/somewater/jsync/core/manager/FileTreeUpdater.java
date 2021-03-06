package com.somewater.jsync.core.manager;

import com.somewater.jsync.core.model.Changes;
import com.somewater.jsync.core.model.FileChange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileTreeUpdater {

    private final Path projectDir;

    public FileTreeUpdater(Path projectDir) {
        this.projectDir = projectDir;
    }

    public void update(Changes changes) {
        for (FileChange fileChange : changes.files) {
            Path filepath = projectDir.resolve(Paths.get(adaptFilePath(fileChange.filepath)));
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
        if (!filepath.getParent().toFile().exists()) {
            if (!filepath.getParent().toFile().mkdirs()) {
                throw new RuntimeException("Can't create directory: " + filepath.getParent());
            }
        }
        try(OutputStream writer = Files.newOutputStream(filepath)) {
            writer.write(adaptFileContent(change.content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteFile(FileChange.DeleteFile change, Path filepath) {
        if (filepath.toFile().exists() && filepath.toFile().isFile()) {
            filepath.toFile().delete();
            File directory = filepath.getParent().toFile();
            while (directory.list().length == 0) {
                directory.delete();
                directory = directory.getParentFile();
            }
        }
    }

    private static String adaptFilePath(String filepath) {
        return filepath.replace('/', File.separatorChar);
    }

    private static byte[] adaptFileContent(byte[] content) {
        return content;
    }
}
