package com.kv.hash;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class ConsistentHashRing {
    private final TreeMap<Integer, String> ring = new TreeMap<>();
    private final int virtualNodes;
    private final Set<String> nodes = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ConsistentHashRing(int virtualNodes) {
        this.virtualNodes = virtualNodes;
    }

    public void addNode(String nodeId) {
        if (nodes.contains(nodeId)) return;
        nodes.add(nodeId);
        for (int i = 0; i < virtualNodes; i++) {
            String virtualKey = nodeId + "#" + i;
            int hash = Murmur3Hash.hash32(virtualKey.getBytes());
            ring.put(hash, nodeId);
        }
    }

    public void removeNode(String nodeId) {
        if (!nodes.contains(nodeId)) return;
        nodes.remove(nodeId);
        ring.entrySet().removeIf(entry -> entry.getValue().equals(nodeId));
    }

    public String getNode(String key) {
        if (ring.isEmpty()) return null;
        int hash = Murmur3Hash.hash32(key.getBytes());
        Map.Entry<Integer, String> entry = ring.ceilingEntry(hash);
        if (entry == null) {
            entry = ring.firstEntry();
        }
        return entry.getValue();
    }

    public List<String> getReplicaNodes(String key, int replicationFactor) {
        Set<String> replicas = new LinkedHashSet<>();
        String primary = getNode(key);
        if (primary == null) return new ArrayList<>();
        replicas.add(primary);
        // Recorrer el anillo en orden para encontrar los siguientes nodos distintos
        List<String> allNodesSorted = new ArrayList<>(nodes);
        Collections.sort(allNodesSorted);
        int index = allNodesSorted.indexOf(primary);
        for (int i = 1; i < replicationFactor && replicas.size() < replicationFactor; i++) {
            String next = allNodesSorted.get((index + i) % allNodesSorted.size());
            replicas.add(next);
        }
        return new ArrayList<>(replicas);
    }
}