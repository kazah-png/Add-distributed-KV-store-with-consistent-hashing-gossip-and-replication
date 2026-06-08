package com.kv;

import com.kv.api.HttpServer;
import com.kv.config.NodeConfig;
import com.kv.gossip.GossipManager;
import com.kv.hash.ConsistentHashRing;
import com.kv.storage.KVStore;
import com.kv.storage.Replicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DistributedKV {
    private static final Logger log = LoggerFactory.getLogger(DistributedKV.class);

    public static void main(String[] args) throws Exception {
        // Parámetros: nodeId host port gossipPort seedNodes (separados por coma)
        // Ejemplo: node1 localhost 8080 9090 node2:8081,node3:8082
        if (args.length < 5) {
            System.err.println("Usage: java -jar ... <nodeId> <host> <httpPort> <gossipPort> <seedNodes>");
            System.exit(1);
        }
        String nodeId = args[0];
        String host = args[1];
        int httpPort = Integer.parseInt(args[2]);
        int gossipPort = Integer.parseInt(args[3]);
        List<String> seeds = Arrays.asList(args[4].split(","));

        NodeConfig config = new NodeConfig(nodeId, host, httpPort, gossipPort, 150);
        log.info("Starting node {} at {}:{} (gossip {})", nodeId, host, httpPort, gossipPort);

        // Inicializar componentes
        GossipManager gossip = new GossipManager(nodeId, gossipPort, seeds);
        gossip.start();

        ConsistentHashRing ring = new ConsistentHashRing(config.getVirtualNodes());
        // Añadir este nodo y periódicamente actualizar con nodos vivos del gossip
        ring.addNode(nodeId);

        KVStore store = new KVStore("./data/" + nodeId);
        Replicator replicator = new Replicator(ring, 2); // replicación factor 2
        HttpServer httpServer = new HttpServer(httpPort, store, ring, replicator, nodeId);
        httpServer.start();

        // Hilo para actualizar el anillo con miembros vivos del gossip
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10_000);
                    var alive = gossip.getMembershipList().getAliveNodes();
                    // Reconstruir anillo con nodos vivos
                    // Simplificado: eliminar los que ya no están y añadir nuevos
                    for (String node : alive) {
                        if (!ring.getReplicaNodes(node, 1).contains(node)) {
                            ring.addNode(node);
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();

        httpServer.join();
    }
}