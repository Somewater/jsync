package com.somewater.jsync.server;

import com.somewater.jsync.server.conf.Args;
import com.somewater.jsync.server.manager.ProjectsFileTreeUpdater;
import com.somewater.jsync.server.multicast.MulticastListener;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Server implements ApplicationRunner {
    public static void main(String[] args) {
        var context = SpringApplication.run(Server.class, args);
    }

    @Bean(initMethod = "listen")
    public MulticastListener multicastListener(Args args) {
        return new MulticastListener(args);
    }

    @Bean
    public ProjectsFileTreeUpdater fileTreeUpdater(Args args) {
        return new ProjectsFileTreeUpdater(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var args2 = new Args(args);
        if (args2.isHelpParam()) {
            args2.printHelp();
            System.exit(-1);
        }
    }
}
