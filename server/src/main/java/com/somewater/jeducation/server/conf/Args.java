package com.somewater.jeducation.server.conf;

import com.somewater.jeducation.core.util.ArgsParser;
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
}
