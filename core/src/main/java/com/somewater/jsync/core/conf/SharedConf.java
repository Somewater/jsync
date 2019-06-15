package com.somewater.jsync.core.conf;

import java.nio.charset.Charset;

public class SharedConf {
    public static Charset CHARSET = Charset.forName("UTF-8");
    public static final int DEFAULT_PORT = 16287;
    public static final int BROADCAST_PORT = 16288;
    public static final String DISCOVERY_REQUEST = "jsync-server-request";
    public static final String DISCOVERY_REPLY = "jsync-server-ip-port ";
    public static final String ERROR_MSG_READONLY_SERVER = "Server works in readonly mode: can't fetch changes from server file tree";
}
