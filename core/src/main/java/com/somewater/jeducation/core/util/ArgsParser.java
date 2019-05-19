package com.somewater.jeducation.core.util;

import com.somewater.jeducation.core.conf.SharedConf;

import java.util.*;

abstract public class ArgsParser {
    public static final Set<String> DefaultExts = Set.of("java", "xml", "iml");

    public final Map<String, String> opts;

    public ArgsParser(String[] args) {
        var opts = new HashMap<String, String>();
        String key = null;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                key = arg;
                while (key.startsWith("-")) key = key.substring(1);
                opts.put(key, null);
            } else if (key != null) {
                opts.put(key, arg);
            }
        }
        this.opts = Collections.unmodifiableMap(opts);
    }

    public Integer broadcastPort() {
        return Optional.ofNullable(opts.get("b")).map(s -> Integer.parseInt(s)).orElse(SharedConf.BROADCAST_PORT);
    }

    public Set<String> fileExtensions() {
        return Optional.ofNullable(opts.get("e"))
                .map(l -> Set.of(l.split(",")))
                .orElse(DefaultExts);
    }

    public boolean isHelpParam() {
        return opts.containsKey("help");
    }

    abstract public void printHelp();
}
