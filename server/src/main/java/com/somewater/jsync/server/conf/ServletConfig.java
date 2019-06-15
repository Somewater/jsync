package com.somewater.jsync.server.conf;

import com.somewater.jsync.core.conf.SharedConf;
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

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        factory.setAddress(args.host().map(host -> {
            try {
                return Inet4Address.getByName(host);
            } catch (UnknownHostException e) {
                e.printStackTrace();
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
