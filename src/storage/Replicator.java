package com.kv.storage;

import com.kv.hash.ConsistentHashRing;
import com.kv.client.KvClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class Replicator {
    private static final Logger log = LoggerFactory.getLogger(Replicator.class);
    private final ConsistentHashRing ring;
    private final int replicationFactor;

    public Replicator(ConsistentHashRing ring, int replicationFactor) {
        this.ring = ring;
        this.replicationFactor = replicationFactor;
    }

    public void writeReplicas(String key, String value, String localNodeId) {
        List<String> nodes = ring.getReplicaNodes(key, replicationFactor);
        for (String node : nodes) {
            if (node.equals(localNodeId)) {
                // Ya se escribió localmente
                continue;
            }
            // Replicar vía HTTP
            try {
                KvClient client = new KvClient(node);
                client.put(key, value);
            } catch (Exception e) {
                log.warn("Failed to replicate to {}", node, e);
            }
        }
    }

    public void deleteReplicas(String key, String localNodeId) {
        List<String> nodes = ring.getReplicaNodes(key, replicationFactor);
        for (String node : nodes) {
            if (node.equals(localNodeId)) continue;
            try {
                KvClient client = new KvClient(node);
                client.delete(key);
            } catch (Exception e) {
                log.warn("Failed to delete replica on {}", node, e);
            }
        }
    }
}