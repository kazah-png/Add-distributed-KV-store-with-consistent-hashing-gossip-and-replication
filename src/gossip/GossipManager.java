package com.kv.gossip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GossipManager {
    private static final Logger log = LoggerFactory.getLogger(GossipManager.class);
    private final String localNodeId;
    private final int gossipPort;
    private final MembershipList membershipList;
    private final List<String> seedNodes;
    private ScheduledExecutorService scheduler;
    private DatagramSocket udpSocket;

    public GossipManager(String localNodeId, int gossipPort, List<String> seedNodes) {
        this.localNodeId = localNodeId;
        this.gossipPort = gossipPort;
        this.seedNodes = seedNodes;
        this.membershipList = new MembershipList();
        this.membershipList.heartbeat(localNodeId);
    }

    public void start() throws SocketException {
        udpSocket = new DatagramSocket(gossipPort);
        scheduler = Executors.newScheduledThreadPool(2);
        // Enviar heartbeat cada 5 segundos
        scheduler.scheduleAtFixedRate(this::broadcastHeartbeat, 0, 5, TimeUnit.SECONDS);
        // Escuchar mensajes UDP
        scheduler.submit(this::listen);
        // Contactar seed nodes al inicio
        seedNodes.forEach(seed -> sendGossip(seed, gossipPort));
    }

    private void broadcastHeartbeat() {
        // Enviar a todos los nodos vivos (incluidos seeds)
        Set<String> alive = membershipList.getAliveNodes();
        for (String node : alive) {
            if (!node.equals(localNodeId)) {
                sendHeartbeatTo(node);
            }
        }
    }

    private void sendHeartbeatTo(String nodeId) {
        // Asumimos que la dirección se resuelve mediante un mapeo simple: nodeId = host:port
        // En producción se usaría un servicio de descubrimiento. Aquí simplificamos:
        String[] parts = nodeId.split(":");
        if (parts.length != 2) return;
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        sendUdp(host, port, "HEARTBEAT:" + localNodeId);
    }

    private void sendGossip(String targetHost, int targetPort) {
        sendUdp(targetHost, targetPort, "JOIN:" + localNodeId);
    }

    private void sendUdp(String host, int port, String msg) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] buf = msg.getBytes();
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            log.warn("Failed to send UDP to {}:{}", host, port, e);
        }
    }

    private void listen() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                udpSocket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                processMessage(msg, packet.getAddress().getHostAddress());
            }
        } catch (IOException e) {
            log.error("UDP listen error", e);
        }
    }

    private void processMessage(String msg, String senderHost) {
        if (msg.startsWith("HEARTBEAT:")) {
            String nodeId = msg.substring(10);
            membershipList.heartbeat(nodeId);
        } else if (msg.startsWith("JOIN:")) {
            String newNodeId = msg.substring(5);
            membershipList.heartbeat(newNodeId);
            // Responder con todos los nodos conocidos (gossip pull)
        }
    }

    public MembershipList getMembershipList() {
        return membershipList;
    }
}