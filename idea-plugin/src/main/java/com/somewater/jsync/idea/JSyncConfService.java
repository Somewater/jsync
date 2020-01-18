package com.somewater.jsync.idea;

import com.somewater.jsync.core.conf.HostPort;
import com.somewater.jsync.core.conf.LocalConf;

import java.util.concurrent.atomic.AtomicReference;

public class JSyncConfService {
    private LocalConf conf = null;
    private HostPort hostPort = null;
    private boolean inited = false;

    public LocalConf getConf() {
        if (conf == null) {
            synchronized (this) {
                if (conf == null) {
                    conf = new LocalConf(null);
                }
            }
        }
        return conf;
    }

    public void setHostPort(HostPort hp) {
        conf.getServerHost();
        this.hostPort = hp;
    }

    public HostPort getHostPort() {
        return hostPort;
    }

    public boolean isInited() {
        return inited;
    }

    public void setInited() {
        this.inited = true;
    }
}
