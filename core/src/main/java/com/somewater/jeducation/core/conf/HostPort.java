package com.somewater.jeducation.core.conf;

public class HostPort {
    public final String host;
    public final int port;

    public HostPort(String payload) {
        String[] parts = payload.split(":");
        if (parts.length > 1) {
            host = parts[0];
            port = Integer.parseInt(parts[1]);
        } else {
            host = parts[0];
            port = SharedConf.DEFAULT_PORT;
        }
    }

    public HostPort(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getPayload() {
        return String.join(":", host, String.valueOf(port));
    }
}