package com.kv.gossip;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MembershipList {
    // nodeId -> heartbeat timestamp (última vez que se supo del nodo)
    private final ConcurrentHashMap<String, Long> aliveNodes = new ConcurrentHashMap<>();
    private final long timeoutMillis = 30_000; // 30 segundos sin heartbeat = muerto

    public void heartbeat(String nodeId) {
        aliveNodes.put(nodeId, System.currentTimeMillis());
    }

    public boolean isAlive(String nodeId) {
        Long last = aliveNodes.get(nodeId);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < timeoutMillis;
    }

    public Set<String> getAliveNodes() {
        Set<String> alive = new HashSet<>();
        for (Map.Entry<String, Long> entry : aliveNodes.entrySet()) {
            if (System.currentTimeMillis() - entry.getValue() < timeoutMillis) {
                alive.add(entry.getKey());
            }
        }
        return alive;
    }

    public void removeDeadNodes() {
        aliveNodes.entrySet().removeIf(entry ->
            System.currentTimeMillis() - entry.getValue() >= timeoutMillis);
    }

    public Map<String, Long> getView() {
        return new HashMap<>(aliveNodes);
    }
}