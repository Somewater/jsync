package com.somewater.jeducation.client;

import com.somewater.jeducation.client.conf.Args;
import com.somewater.jeducation.client.conf.ProjectId;
import com.somewater.jeducation.client.conf.LocalConf;
import com.somewater.jeducation.client.controller.Controller;
import com.somewater.jeducation.client.network.FindServer;
import com.somewater.jeducation.client.network.ServerApi;
import com.somewater.jeducation.core.conf.SharedConf;
import com.somewater.jeducation.core.manager.FileTreeUpdater;
import com.somewater.jeducation.core.manager.FileTreeWatcher;
import com.somewater.jeducation.core.util.RetryException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * @author pnaydenov
 */
public class Client {
    private static Logger logger = Logger.getLogger(Client.class.getName());

    public static void main(String[] args0) {
        Args args = new Args(args0);
        setupLogger();
        while (true) {
            try {
                run(args);
            } catch (AssertionError e) {
                args.printHelp();
                System.err.println(e.getMessage());
                System.exit(-1);
            } catch (RetryException e){
                System.err.println(e.getMessage());
            } catch (Throwable e) {
                if (containsCauseClass(e, ConnectException.class)) {
                    continue;
                }
                throw e;
            }
        }
    }

    private static <E extends Throwable> boolean containsCauseClass(Throwable ex, Class<E> clazz) {
        while (ex != null) {
            if (clazz.isInstance(ex))
                return true;
            ex = ex.getCause();
        }
        return false;
    }

    private static void setupLogger() {
        try {
            LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(
                   ("java.util.logging.ConsoleHandler.level = INFO\n" +
                    "handlers = java.util.logging.ConsoleHandler\n" +
                    ".level = INFO\n" +
                    "java.util.logging.SimpleFormatter.format = [%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] %4$-6s %5$s%6$s%n\n").getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void run(Args args) {
        if (args.isHelpParam()) {
            args.printHelp();
            return;
        }
        LocalConf localConf = new LocalConf(args);
        Path wordDir = Paths.get(System.getProperty("user.dir"));
        Path projectDir = args.projectDir().map(Paths::get).orElse(wordDir);
        ProjectId projectId = new ProjectId(projectDir, args.projectName());

        String host;
        int port;
        if (args.serverHost().isPresent()) {
            host = args.serverHost().get();
            port = args.serverPort().orElse(SharedConf.DEFAULT_PORT);
        } else {
            logger.info("Server host/port unknown, start network discovery");
            var hostPort = new FindServer().find();
            host = hostPort.host;
            port = hostPort.port;
            logger.info("Discovery completed");
        }

        logger.info("App started at " + projectDir);
        logger.info(String.format("Server: %s:%d, project: %s, user: %s",
                host, port, projectId.getName(), localConf.getUid()));
        ServerApi server = new ServerApi(host, port);
        if (projectDir.toFile().isDirectory()) {
            FileTreeWatcher watcher = new FileTreeWatcher(projectDir, args.fileExtensions());
            FileTreeUpdater updater = new FileTreeUpdater(projectDir);
            Controller controller = new Controller(args, server, watcher, updater, localConf, projectId);
            controller.start();
        } else {
            throw new AssertionError("Project directory does not exist: " + projectDir);
        }
    }
}
