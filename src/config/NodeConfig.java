package com.kv.config;

public class NodeConfig {
    private String nodeId;
    private String host;
    private int port;          // API HTTP
    private int gossipPort;    // UDP para gossip
    private int virtualNodes;   // Nodos virtuales para anillo hash

    public NodeConfig(String nodeId, String host, int port, int gossipPort, int virtualNodes) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
        this.gossipPort = gossipPort;
        this.virtualNodes = virtualNodes;
    }

    // getters y setters
    public String getNodeId() { return nodeId; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public int getGossipPort() { return gossipPort; }
    public int getVirtualNodes() { return virtualNodes; }
}