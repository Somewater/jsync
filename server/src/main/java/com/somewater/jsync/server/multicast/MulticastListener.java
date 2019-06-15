package com.somewater.jsync.server.multicast;

import com.somewater.jsync.core.conf.HostPort;
import com.somewater.jsync.core.conf.SharedConf;
import com.somewater.jsync.server.conf.Args;

import java.io.IOException;

public class MulticastListener {
    private final Args args;

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
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
