package com.somewater.jeducation.client.conf;

import com.somewater.jeducation.core.util.ArgsParser;

import java.util.Optional;
import java.util.Set;

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

    public void printHelp() {
        System.out.println("Use as:\n" +
                "java -jar ./jeducation-client.jar\n\n" +
                "Optional params:\n" +
                "  -h IP - server ip (host)\n" +
                "  -p INT - server port\n" +
                "  -e STR - comma separated list of supported file extensions (default " + DefaultExts.toString() + ")\n" +
                "  -d FILEPATH - directory to watch (current dir for default)\n" +
                "  -n STR - project name (current dir name by default)\n" +
                "  -u STR - user name (random generated file ~/jeducation-config.txt by default)\n" +
                "  -r - work in readonly mode (ignore file updates from server)\n" +
                "  -b INT - broadcast port\n");
    }
}
