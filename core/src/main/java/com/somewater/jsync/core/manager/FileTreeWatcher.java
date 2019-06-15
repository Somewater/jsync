package com.somewater.jsync.core.manager;

import com.somewater.jsync.core.model.FileChange;

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
    private final HashMap<String, String> fileCache;
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

    public Stream<Map.Entry<String, String>> iterateAllFiles() {
        try {
            return Files.walk(directory)
                    .filter(p -> p.toFile().isFile() && fileExtensions.contains(extension(p.toFile().getName())))
                    .map(path -> {
                        String filepath = directory.relativize(path).toString();
                        String content = null;
                        try {
                            content = Files.readString(path);
                        } catch (IOException e) {
                            throw new RuntimeException();
                        }
                        return Map.entry(filepath, content);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearCacheForFiles(List<String> filepaths) {
        for (String filepath : filepaths)
            fileCache.put(filepath, EMPTY_CACHE);
    }

    public void changedFiles(BiConsumer<String, Optional<String>> consumer) {
        Set<String> allKnownFiles = new HashSet<>(fileCache.keySet());
        iterateAllFiles().filter(element -> {
            var cache = fileCache.get(element.getKey());
            var contentMd5 = md5(element.getValue());
            allKnownFiles.remove(element.getKey());
            if (cache != null && cache.equals(contentMd5)) {
                return false;
            }
            fileCache.put(element.getKey(), contentMd5);
            if (cache != null && cache.equals(EMPTY_CACHE)) {
                // cache contains special value
                return false;
            } else {
                return true;
            }
        }).forEach(element -> consumer.accept(element.getKey(), Optional.of(element.getValue())));
        allKnownFiles.forEach(l -> {
            fileCache.remove(l);
            consumer.accept(l, Optional.<String>empty());
        });
    }

    public List<FileChange> changedFilesList() {
        ArrayList<FileChange> result = new ArrayList<>();
        changedFiles((filepath, content) -> {
            FileChange change = content.<FileChange>map(c -> new FileChange.CreateFile(filepath, c.getBytes()))
                    .orElseGet(() -> new FileChange.DeleteFile(filepath));
            result.add(change);
        });
        return result;
    }

    private String md5(String content) {
        MessageDigest m = messageDigest;
        m.reset();
        m.update(content.getBytes());
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
}
