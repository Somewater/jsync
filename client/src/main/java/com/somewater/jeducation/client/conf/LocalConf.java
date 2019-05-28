package com.somewater.jeducation.client.conf;

import com.somewater.jeducation.core.conf.SharedConf;

import java.io.*;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

public class LocalConf {
    public static String UID = "name";

    private final Args args;
    private final Properties conf;
    private Logger logger = Logger.getLogger(getClass().getName());

    public LocalConf(Args args) {
        this.args = args;
        File configFilepath = args.configPath()
                .map(l -> new File(l))
                .orElseGet(() -> Paths.get(System.getProperty("user.home"), "jeducation-config.txt").toFile());
        this.conf = readConfig(configFilepath)
                .filter(LocalConf::isValid)
                .orElseGet(() -> generateConfig(configFilepath));
    }

    public String getUid() {
        return args.userName().filter(LocalConf::isCorrectUid).orElse(conf.getProperty(UID));
    }

    private Optional<Properties> readConfig(File filepath) {
        if (filepath.exists()) {
            try(var reader = new BufferedReader(new FileReader(filepath, SharedConf.CHARSET))) {
                var props = new Properties();
                props.load(reader);
                return Optional.of(props);
            } catch (IOException e) {
                logger.severe(e.getMessage());
            }
        }
        return Optional.empty();
    }

    private static boolean isValid(Properties props) {
        return isCorrectUid(props.getProperty(UID));
    }

    private Properties generateConfig(File filepath) {
        Properties props = randomConfig();
        try (var file = new FileOutputStream(filepath)) {
            props.store(file, "jEducation properties file");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.warning("Config file generated at " + filepath);
        return props;
    }

    private static boolean isCorrectUid(String uid) {
        if (uid == null || uid.isEmpty()) {
            return false;
        }
        for (var c : uid.toCharArray())
            if (!Character.isLetterOrDigit(c) && c != '_')
                return false;
        return true;
    }

    private static Properties randomConfig() {
        var props = new Properties();
        props.setProperty(UID, randomUid());
        return props;
    }

    private static String randomUid() {
        var r = new Random(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            char c = (char) (r.nextInt('Z' - 'A') + 'A');
            sb.append(c);
        }
        return sb.toString();
    }
}
