package com.somewater.jsync.server.conf;

import com.somewater.jsync.core.util.ArgsParser;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Args extends ArgsParser {
    public Args(ApplicationArguments args) {
        super(args.getSourceArgs());
    }

    public Optional<String> host() {
        return Optional.ofNullable(opts.get("h"));
    }

    public Optional<Integer> port() {
        return Optional.ofNullable(opts.get("p")).map(s -> Integer.parseInt(s));
    }

    public Optional<String> projectsDir() {
        return Optional.ofNullable(opts.get("d"));
    }

    public boolean readonly() {
        return opts.containsKey("r");
    }

    public void printHelp() {
        System.out.println("Use as:\n" +
                "java -jar jsync-server.jar\n\n" +
                "Optional params:\n" +
                "  -h IP - server ip (host)\n" +
                "  -p INT - server port\n" +
                "  -e STR - comma separated list of supported file extensions (default " + DefaultExts.toString() + ")\n" +
                "  -d FILEPATH - directory to store watched projects ('.\\PROJECTS' by default)\n" +
                "  -r - work in readonly mode (ignore file updates from server)\n" +
                "  -b INT - broadcast port\n");
    }
}