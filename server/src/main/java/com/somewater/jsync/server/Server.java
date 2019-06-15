package com.somewater.jsync.server;

import com.somewater.jsync.server.conf.Args;
import com.somewater.jsync.server.manager.ProjectsFileTreeUpdater;
import com.somewater.jsync.server.multicast.MulticastListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Server {
    public static void main(String[] args) {
        var context = SpringApplication.run(Server.class, args);
    }

    @Bean(initMethod = "listen")
    public MulticastListener multicastListener(Args args) {
        return new MulticastListener(args);
    }

    @Bean
    public ProjectsFileTreeUpdater fileTreeUpdater(Args args) {
        return new ProjectsFileTreeUpdater(args.projectsDir(), args.fileExtensions(), args.readonly());
    }
}
