package com.somewater.jsync.client.conf;

import com.somewater.jsync.core.conf.SharedConf;

import java.io.*;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class LocalConf {
    public static String UID_FIELD = "name";
    public static String HOST_FIELD = "host";
    public static String PORT_FIELD = "port";
    public static String LOCAL_SLEEP_MS_FIELD = "local_sleep_ms";
    public static String REMOTE_SLEEP_MS_FIELD = "remote_sleep_ms";

    private final Args args;
    private final Properties conf;
    private Logger logger = Logger.getLogger(getClass().getName());
    public final File configFilepath;
    private static final Pattern UidPatter = Pattern.compile("[A-Za-z][A-Za-z0-9_]+");

    public LocalConf(Args args) {
        this.args = args;
        configFilepath = args.configPath()
                .map(l -> new File(l))
                .orElseGet(() -> Paths.get(System.getProperty("user.home"), "jsync-config.txt").toFile());
        this.conf = readConfig(configFilepath)
                .filter(LocalConf::isValid)
                .orElseGet(() -> writeConfig(configFilepath, randomConfig()));
    }

    public String getUid() {
        return args.userName().filter(LocalConf::isCorrectUid).orElse(conf.getProperty(UID_FIELD));
    }

    public Optional<String> getServerHost() {
        return args.serverHost().or(() -> Optional.ofNullable(conf.getProperty(HOST_FIELD)));
    }

    public Integer getServerPort() {
        return args.serverPort()
                .orElse(Integer.parseInt(conf.getProperty(PORT_FIELD, Integer.toString(SharedConf.DEFAULT_PORT))));
    }

    public Integer getLocalSleepMs() {
        return args.localSleepMs()
                .orElse(Integer.parseInt(conf.getProperty(LOCAL_SLEEP_MS_FIELD,
                        Integer.toString(SharedConf.LOCAL_SLEEP_MS))));
    }

    public Integer getRemoteSleepMs() {
        return args.remoteSleepMs()
                .orElse(Integer.parseInt(conf.getProperty(REMOTE_SLEEP_MS_FIELD,
                        Integer.toString(SharedConf.REMOTE_SLEEP_MS))));
    }

    public void writeUid(String uid) {
        conf.setProperty(UID_FIELD, uid);
        conf.setProperty(LOCAL_SLEEP_MS_FIELD, Integer.toString(SharedConf.LOCAL_SLEEP_MS));
        conf.setProperty(REMOTE_SLEEP_MS_FIELD, Integer.toString(SharedConf.REMOTE_SLEEP_MS));
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
        return isCorrectUid(props.getProperty(UID_FIELD));
    }

    private Properties writeConfig(File filepath, Properties props) {
        try (var file = new FileOutputStream(filepath)) {
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
        var props = new Properties();
        return props;
    }
}
