package com.somewater.jeducation.core.conf;

import java.nio.charset.Charset;

public class SharedConf {
    public static Charset CHARSET = Charset.forName("UTF-8");
    public static final int DEFAULT_PORT = 16287;
    public static final int BROADCAST_PORT = 16288;
    public static final String DISCOVERY_REQUEST = "jeducation-server-request";
    public static final String DISCOVERY_REPLY = "jeducation-server-ip-port ";
}
