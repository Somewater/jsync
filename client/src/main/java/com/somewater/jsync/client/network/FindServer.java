package com.somewater.jsync.client.network;

import com.somewater.jsync.core.conf.HostPort;
import com.somewater.jsync.core.conf.SharedConf;

import java.io.IOException;
import java.net.*;
import java.util.logging.Logger;

public class FindServer {

    private Logger logger = Logger.getLogger(getClass().getName());

    public HostPort find() {
        try {
            try(DatagramSocket socket = new DatagramSocket()) {
                return new HostPort(broadcastRequestResponse(socket));
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    private String broadcastRequestResponse(DatagramSocket socket) throws SocketException {
        socket.setBroadcast(true);
        socket.setSoTimeout(10000);
        byte[] receiveBuffer = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        byte[] packetData = SharedConf.DISCOVERY_REQUEST.getBytes();
        InetAddress broadcastAddress;
        try {
            broadcastAddress = InetAddress.getByName("255.255.255.255");
        } catch (UnknownHostException e) {
            throw new RuntimeException("Can't create broadcast address", e);
        }
        int servicePort = SharedConf.BROADCAST_PORT;
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, broadcastAddress, servicePort);

        String result = null;
        while(true) {
            try {
                logger.info("Try send broadcast message");
                socket.send(packet);

                socket.receive(receivePacket);
                String reply = new String(receivePacket.getData());
                int k = reply.indexOf(SharedConf.DISCOVERY_REPLY);
                if (k < 0) {
                    break;
                }
                k += SharedConf.DISCOVERY_REPLY.length();
                result = reply.substring(k).trim();
                logger.info("Response from server received");
                break;
            } catch(SocketTimeoutException ignored) {
            } catch (IOException e) {
                logger.severe(e.getMessage());
            }
        }
        return result;
    }
}
