package com.somewater.jsync.client.conf;

import com.somewater.jsync.core.conf.LocalConf;
import com.somewater.jsync.core.conf.SharedConf;

import java.io.*;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Conf extends LocalConf {
    public static String UID_FIELD = "name";
    public static String HOST_FIELD = "host";
    public static String PORT_FIELD = "port";
    public static String LOCAL_SLEEP_MS_FIELD = "local_sleep_ms";
    public static String REMOTE_SLEEP_MS_FIELD = "remote_sleep_ms";
    public static String READONLY_FIELD = "readonly";

    private final Args args;
    private Logger logger = Logger.getLogger(getClass().getName());

    public Conf(Args args) {
        super(args.configPath().orElse(null));
        this.args = args;
    }

    @Override
    public String getUid() {
        return args.userName().filter(Conf::isCorrectUid).orElse(super.getUid());
    }

    @Override
    public Optional<String> getServerHost() {
        Optional<String> a = args.serverHost();
        return (a.isPresent() ? a : super.getServerHost());
    }

    @Override
    public Integer getServerPort() {
        return args.serverPort().orElse(super.getServerPort());
    }

    @Override
    public Integer getLocalSleepMs() {
        return args.localSleepMs().orElse(super.getLocalSleepMs());
    }

    @Override
    public Integer getRemoteSleepMs() {
        return args.remoteSleepMs().orElse(super.getRemoteSleepMs());
    }

    @Override
    public boolean getReadonly() {
        return args.readonly() || super.getReadonly();
    }
}
