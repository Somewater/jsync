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
        return Optional.ofNullable(opts.get("p")).map(Integer::parseInt);
    }

    public Optional<String> projectsDir() {
        return Optional.ofNullable(opts.get("d"));
    }

    public boolean readonly() {
        return opts.containsKey("r");
    }

    public int maxFiles() {
        return Optional.ofNullable(opts.get("max-files")).map(Integer::parseInt).orElse(1000);
    }

    public int maxFileSize() {
        return Optional.ofNullable(opts.get("max-file-size")).map(Integer::parseInt).orElse(1024 * 1024 * 10);
    }

    public int sumFileSize() {
        return Optional.ofNullable(opts.get("sum-file-size")).map(Integer::parseInt).orElse(1024 * 1024 * 100);
    }

    public int maxProjects() {
        return Optional.ofNullable(opts.get("max-projects")).map(Integer::parseInt).orElse(1000);
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
                "  -b INT - broadcast port\n" +
                "  --max-files INT - maximum files per project (1000 by default)\n" +
                "  --max-file-size INT - maximum filesize in bytes (10Mb by default\n" +
                "  --sum-file-size INT - maximum sum of filesizes in bytes per project (100Mb by default\n" +
                "  --max-projects INT - maximum projects (1000 by default\n");
    }
}
