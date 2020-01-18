package com.somewater.jsync.core.conf;

import java.io.*;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class LocalConf {
    private static String UID_FIELD = "name";
    private static String HOST_FIELD = "host";
    private static String PORT_FIELD = "port";
    private static String LOCAL_SLEEP_MS_FIELD = "local_sleep_ms";
    private static String REMOTE_SLEEP_MS_FIELD = "remote_sleep_ms";
    private static String READONLY_FIELD = "readonly";

    private final Properties conf;
    private Logger logger = Logger.getLogger(getClass().getName());
    public final File configFilepath;
    private static final Pattern UidPatter = Pattern.compile("[A-Za-z][A-Za-z0-9_]+");

    public LocalConf(String filepath) {
        configFilepath = Optional.ofNullable(filepath)
                .map(f -> Paths.get(f).toFile())
                .orElse(Paths.get(System.getProperty("user.home"), "jsync-config.txt").toFile());
        this.conf = loadConf();
    }

    private Properties loadConf() {
        return readConfig(configFilepath)
                .filter(LocalConf::isValid)
                .orElseGet(() -> writeConfig(configFilepath, randomConfig()));
    }

    public String getUid() {
        return conf.getProperty(UID_FIELD);
    }

    public Optional<String> getServerHost() {
        return Optional.ofNullable(conf.getProperty(HOST_FIELD));
    }

    public Integer getServerPort() {
        return Integer.parseInt(conf.getProperty(PORT_FIELD, Integer.toString(SharedConf.DEFAULT_PORT)));
    }

    public Integer getLocalSleepMs() {
        return Integer.parseInt(conf.getProperty(LOCAL_SLEEP_MS_FIELD, Integer.toString(SharedConf.LOCAL_SLEEP_MS)));
    }

    public Integer getRemoteSleepMs() {
        return Integer.parseInt(conf.getProperty(REMOTE_SLEEP_MS_FIELD, Integer.toString(SharedConf.REMOTE_SLEEP_MS)));
    }

    public boolean getReadonly() {
        return conf.getProperty(READONLY_FIELD, "0").equals("1");
    }

    public void writeUid(String uid) {
        conf.setProperty(UID_FIELD, uid);
        conf.setProperty(LOCAL_SLEEP_MS_FIELD, Integer.toString(SharedConf.LOCAL_SLEEP_MS));
        conf.setProperty(REMOTE_SLEEP_MS_FIELD, Integer.toString(SharedConf.REMOTE_SLEEP_MS));
        conf.setProperty(READONLY_FIELD, "0");
        if (isValid(conf)) {
            writeConfig(configFilepath, conf);
        } else {
            StringBuilder sb = new StringBuilder();
            conf.forEach((k, v) -> {
                sb.append(k);
                sb.append('=');
                sb.append(v);
                sb.append('\n');
            });
            throw new AssertionError("Invalid properties:\n" + sb.toString());
        }
    }

    private Optional<Properties> readConfig(File filepath) {
        if (filepath.exists()) {
            try(BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
                Properties props = new Properties();
                props.load(reader);
                return Optional.of(props);
            } catch (IOException e) {
                logger.severe(e.getMessage());
            }
        }
        return Optional.empty();
    }

    private static boolean isValid(Properties props) {
        return isCorrectUid(props.getProperty(UID_FIELD));
    }

    private Properties writeConfig(File filepath, Properties props) {
        try (FileOutputStream file = new FileOutputStream(filepath)) {
            props.store(file, "jsync properties file");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.warning("Config file generated at " + filepath);
        return props;
    }

    public static boolean isCorrectUid(String uid) {
        if (uid == null || uid.isEmpty())
            return false;
        return UidPatter.matcher(uid).matches();
    }

    private static Properties randomConfig() {
        Properties props = new Properties();
        props.setProperty("uid", UUID.randomUUID().toString());
        return props;
    }
}
