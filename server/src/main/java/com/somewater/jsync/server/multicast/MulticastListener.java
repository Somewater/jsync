package com.somewater.jsync.server.multicast;

import com.somewater.jsync.core.conf.HostPort;
import com.somewater.jsync.core.conf.SharedConf;
import com.somewater.jsync.server.conf.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MulticastListener {
    private final Args args;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public MulticastListener(Args args) {
        this.args = args;
    }

    public void listen() {
        String ip = NetworkUtil.getMyAddress().getHostAddress();
        int port = args.port().orElse(SharedConf.DEFAULT_PORT);
        String payload = new HostPort(ip, port).getPayload();
        new Thread(() -> {
            while (true) {
                try {
                    new DiscoveryServer(payload).listen();
                } catch (IOException e) {
                    log.error("Multicast listener error, restart", e);
                }
            }
        }).start();
    }
}
