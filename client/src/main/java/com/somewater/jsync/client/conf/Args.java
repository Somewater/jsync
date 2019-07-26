package com.somewater.jsync.client.conf;

import com.somewater.jsync.core.util.ArgsParser;

import java.util.Optional;

public class Args extends ArgsParser {
    public Args(String[] args) {
        super(args);
    }

    public Optional<String> serverHost() {
        return Optional.ofNullable(opts.get("h"));
    }

    public Optional<Integer> serverPort() {
        return Optional.ofNullable(opts.get("p")).map(s -> Integer.parseInt(s));
    }

    public Optional<String> projectDir() {
        return Optional.ofNullable(opts.get("d"));
    }

    public Optional<String> projectName() {
        return Optional.ofNullable(opts.get("n"));
    }

    public boolean isHelpParam() {
        return opts.containsKey("help");
    }

    public Optional<String> userName() {
        return Optional.ofNullable(opts.get("u"));
    }

    public boolean readonly() {
        return opts.containsKey("r");
    }

    public Optional<String> configPath() {
        return Optional.ofNullable(opts.get("c"));
    }

    public Optional<Integer> localSleepMs() {
        return Optional.ofNullable(opts.get("ls")).map(s -> Integer.parseInt(s));
    }

    public Optional<Integer> remoteSleepMs() {
        return Optional.ofNullable(opts.get("rs")).map(s -> Integer.parseInt(s));
    }

    public void printHelp() {
        System.out.println("Use as:\n" +
                "java -jar jsync-client.jar\n\n" +
                "Optional params:\n" +
                "  -h IP - server ip (host)\n" +
                "  -p INT - server port\n" +
                "  -e STR - comma separated list of supported file extensions (default " + DefaultExts.toString() + ")\n" +
                "  -d FILEPATH - directory to watch (current dir for default)\n" +
                "  -n STR - project name (current dir name by default)\n" +
                "  -u STR - user name (property 'name' from ~/jsync-config.txt by default or random generated string value)\n" +
                "  -r - work in readonly mode (ignore file updates from server)\n" +
                "  -b INT - broadcast port (required for automatic server discovery)\n" +
                "  -c FILEPATH - path to config (~/jsync-config.txt by default)\n" +
                "  -ls MILLIS - sleep between local file system checks\n" +
                "  -rs MILLIS - sleep between remote file system checks\n");
    }
}
