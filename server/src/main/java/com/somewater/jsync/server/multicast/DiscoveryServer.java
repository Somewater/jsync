package com.somewater.jsync.server.multicast;

import com.somewater.jsync.core.conf.SharedConf;

import java.io.IOException;
import java.net.*;

public class DiscoveryServer {
    private static final int MAX_PACKET_SIZE = 1024;
    private DatagramSocket socket;
    private final String payload;

    public DiscoveryServer(String payload) {
        this.payload = payload;
    }

    public void listen() throws IOException {
        InetAddress addr = InetAddress.getByName("0.0.0.0");
        socket = new DatagramSocket(SharedConf.BROADCAST_PORT, addr);
        socket.setBroadcast(true);

        while (true) {
            byte[] recvBuf = new byte[MAX_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(packet);

            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();

            String message = new String(packet.getData()).trim();
            if (message.startsWith(SharedConf.DISCOVERY_REQUEST)) {
                String reply =  SharedConf.DISCOVERY_REPLY + payload;
                byte[] sendData = reply.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                socket.send(sendPacket);
            }
        }
    }

}