package com.somewater.jsync.server.multicast;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
public class NetworkUtil {
    static final String IPv4_PATTERN = "\\d+(\\.\\d+){3}";

    public static InetAddress getMyAddress( ) {
        List<InetAddress> addresses = getAllAddresses();
        for(InetAddress address : addresses) {
            if (address.isLoopbackAddress() || address.isLinkLocalAddress()) continue;
            if ( address.getHostAddress().matches(IPv4_PATTERN) ) return address;
        }
        try {
            return InetAddress.getLocalHost();
        } catch (Exception e) {
            return null;
        }
    }

    public static List<InetAddress> getAllAddresses() {
        List<InetAddress> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) continue;

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    result.add(addresses.nextElement());
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}