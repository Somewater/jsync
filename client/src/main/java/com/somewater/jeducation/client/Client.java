package com.somewater.jeducation.client;

import com.somewater.jeducation.client.conf.Args;
import com.somewater.jeducation.client.conf.ProjectId;
import com.somewater.jeducation.client.conf.Uid;
import com.somewater.jeducation.client.controller.Controller;
import com.somewater.jeducation.client.network.FindServer;
import com.somewater.jeducation.client.network.ServerApi;
import com.somewater.jeducation.core.conf.SharedConf;
import com.somewater.jeducation.core.manager.FileTreeUpdater;
import com.somewater.jeducation.core.manager.FileTreeWatcher;
import com.somewater.jeducation.core.util.RetryException;

import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author pnaydenov
 */
public class Client {
    public static void main(String[] args0) {
        Args args = new Args(args0);
        while (true) {
            try {
                run(args);
            } catch (AssertionError e) {
                args.printHelp();
                System.out.println(e.getMessage());
                System.exit(-1);
            } catch (RetryException e){
                System.err.println(e.getMessage());
                continue;
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

    private static void run(Args args) {
        if (args.isHelpParam()) {
            args.printHelp();
            return;
        }

        String host;
        int port;
        if (args.serverHost().isPresent()) {
            host = args.serverHost().get();
            port = args.serverPort().orElse(SharedConf.DEFAULT_PORT);
        } else {
            var hostPort = new FindServer().find();
            host = hostPort.host;
            port = hostPort.port;
        }

        System.out.printf("Server host=%s, port=%d\n", host, port);
        ServerApi server = new ServerApi(host, port);
        Path wordDir = Paths.get(System.getProperty("user.dir"));
        Path projectDir = args.projectDir().map(Paths::get).orElse(wordDir);
        if (projectDir.toFile().isDirectory()) {
            FileTreeWatcher watcher = new FileTreeWatcher(projectDir, args.fileExtensions());
            FileTreeUpdater updater = new FileTreeUpdater(projectDir);
            Uid uid = new Uid(args);
            ProjectId projectId = new ProjectId(projectDir, args.projectName());
            Controller controller = new Controller(args, server, watcher, updater, uid, projectId);
            controller.start();
        } else {
            System.out.println("Project directory does not exist: " + projectDir);
        }
    }
}
