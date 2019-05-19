package com.somewater.jeducation.client.conf;

import com.somewater.jeducation.core.util.StringUtil;
import com.somewater.jeducation.core.conf.SharedConf;

import java.io.*;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Random;

public class Uid {
    private final Args args;

    public Uid(Args args) {
        this.args = args;
    }

    public String get() {
        return tryGet()
                .or(() -> args.userName())
                .map(StringUtil::removeUnsupportedPathSymbols)
                .filter(s -> !s.isEmpty())
                .orElseGet(this::generate);
    }

    private Optional<String> tryGet() {
        var file = getConfigFile();
        if (file.exists()) {
            try(var reader = new BufferedReader(new FileReader(file, SharedConf.CHARSET))) {
                return Optional.ofNullable(reader.readLine()).map(String::strip).filter(this::isCorrect);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return Optional.empty();
    }

    private String generate() {
        String uid = randomUid();
        try (var file = new FileWriter(getConfigFile(), SharedConf.CHARSET)) {
            file.write(uid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return uid;
    }

    public boolean isCorrect(String uid) {
        for (var c : uid.toCharArray())
            if (!Character.isLetterOrDigit(c) && c != '_')
                return false;
        return true;
    }

    private File getConfigFile() {
        return Paths.get(System.getProperty("user.home"), "jeducation-config.txt").toFile();
    }

    private String randomUid() {
        var r = new Random(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            char c = (char) (r.nextInt('Z' - 'A') + 'A');
            sb.append(c);
        }
        return sb.toString();
    }
}
