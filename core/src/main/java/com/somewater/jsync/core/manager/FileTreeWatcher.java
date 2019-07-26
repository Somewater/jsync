package com.somewater.jsync.core.manager;

import com.somewater.jsync.core.model.FileChange;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class FileTreeWatcher {
    private static String EMPTY_CACHE = "<empty>";

    public final Path directory;
    private final HashMap<String, FileInfo> fileCache;
    private final MessageDigest messageDigest;
    private final Set<String> fileExtensions;

    public FileTreeWatcher(Path directory, Set<String> fileExtensions) {
        this.directory = directory;
        this.fileExtensions = fileExtensions;
        if (!this.directory.toFile().isDirectory()) {
            throw new RuntimeException(directory + " is not directory");
        }
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        fileCache = new HashMap<>();
    }

    public Stream<Map.Entry<String, byte[]>> iterateAllFiles() {
        try {
            return Files.walk(directory)
                    .filter(p -> p.toFile().isFile() && fileExtensions.contains(extension(p.toFile().getName())))
                    .flatMap(path -> {
                        Path relativePath = directory.relativize(path);
                        Path fileOrDirName = relativePath;
                        while (fileOrDirName != null) {
                            if (fileOrDirName.getFileName().toString().startsWith(".")) {
                                return Stream.empty();
                            }
                            fileOrDirName = fileOrDirName.getParent();
                        }
                        String filepath = relativePath.toString();
                        byte[] content = null;
                        try {
                            content = Files.readAllBytes(path);
                        } catch (IOException e) {
                            throw new RuntimeException("File" + filepath + " read error", e);
                        }
                        return Stream.of(Map.entry(unifyFilePath(filepath), unifyFileContent(content)));
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearCacheForFiles(List<String> filepaths) {
        for (String filepath : filepaths) {
            var fileInfo = fileCache.get(filepath);
            if (fileInfo == null) {
                fileInfo = new FileInfo();
                fileCache.put(filepath, fileInfo);
                fileInfo.md5 = EMPTY_CACHE;
            }
        }
    }

    public void changedFiles(BiConsumer<String, Optional<byte[]>> consumer) {
        Set<String> allKnownFiles = new HashSet<>(fileCache.keySet());
        iterateAllFiles().filter(element -> {
            var filepath = element.getKey();
            var content = element.getValue();
            var fileInfo = fileCache.get(filepath);
            if (fileInfo == null) {
                fileInfo = new FileInfo();
                fileCache.put(filepath, fileInfo);
            }
            var md5 = fileInfo.md5;
            var contentMd5 = md5(content);
            allKnownFiles.remove(filepath);
            if (md5 != null && md5.equals(contentMd5)) {
                return false;
            }
            fileInfo.md5 = contentMd5;
            fileInfo.size = content.length;
            if (md5 != null && md5.equals(EMPTY_CACHE)) {
                // cache contains special value
                return false;
            } else {
                return true;
            }
        }).forEach(element -> consumer.accept(element.getKey(), Optional.of(element.getValue())));
        allKnownFiles.forEach(l -> {
            fileCache.remove(l);
            consumer.accept(l, Optional.empty());
        });
    }

    public List<FileChange> changedFilesList() {
        ArrayList<FileChange> result = new ArrayList<>();
        changedFiles((filepath, content) -> {
            FileChange change = content.<FileChange>map(c -> new FileChange.CreateFile(filepath, c))
                    .orElseGet(() -> new FileChange.DeleteFile(filepath));
            result.add(change);
        });
        return result;
    }

    public long fileCount() {
        return fileCache.size();
    }

    public long sumFileSize() {
        return fileCache.values().stream().mapToLong(fi -> fi.size).sum();
    }

    public long maxFileSize() {
        return fileCache.values().stream().mapToLong(fi -> fi.size).max().orElse(0);
    }

    private String md5(byte[] content) {
        MessageDigest m = messageDigest;
        m.reset();
        m.update(content);
        var digest = m.digest();
        BigInteger bigInt = new BigInteger(1, digest);
        String md5Hex = bigInt.toString(16);
        while( md5Hex.length() < 32 ){
            md5Hex = "0" + md5Hex;
        }
        return md5Hex;
    }

    private String extension(String filename) {
        int i = filename.lastIndexOf('.');
        if (i == -1) {
            return filename;
        } else {
            return filename.substring(i+1);
        }
    }

    private static String unifyFilePath(String filepath) {
        return filepath.replace(File.separatorChar, '/');
    }

    private static byte[] unifyFileContent(byte[] content) {
        return content;
    }

    private static class FileInfo {
        public String md5;
        public long size;
    }
}
