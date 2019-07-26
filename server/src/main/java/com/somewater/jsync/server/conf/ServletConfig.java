package com.somewater.jsync.server.conf;

import com.somewater.jsync.core.conf.SharedConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Configuration
public class ServletConfig implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    @Autowired
    public Args args;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        factory.setAddress(args.host().map(host -> {
            try {
                return Inet4Address.getByName(host);
            } catch (UnknownHostException e) {
                log.error("Inet4Address.getByName invocation exception", e);
                try {
                    return Inet4Address.getLocalHost();
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(e);
                }
            }
        }).orElseGet(() -> new InetSocketAddress(0).getAddress()));
        factory.setPort(args.port().orElse(SharedConf.DEFAULT_PORT));
    }
}
