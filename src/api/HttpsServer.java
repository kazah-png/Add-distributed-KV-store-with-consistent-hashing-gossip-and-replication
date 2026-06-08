package com.kv.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kv.gossip.GossipManager;
import com.kv.hash.ConsistentHashRing;
import com.kv.storage.KVStore;
import com.kv.storage.Replicator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpServer {
    private final Server server;
    private final KVStore store;
    private final ConsistentHashRing ring;
    private final Replicator replicator;
    private final String localNodeId;
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpServer(int port, KVStore store, ConsistentHashRing ring, Replicator replicator, String localNodeId) {
        this.store = store;
        this.ring = ring;
        this.replicator = replicator;
        this.localNodeId = localNodeId;
        server = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new KVHandler()), "/kv/*");
    }

    public void start() throws Exception {
        server.start();
    }

    public void join() throws Exception {
        server.join();
    }

    private class KVHandler extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) {
                resp.setStatus(400);
                return;
            }
            String key = path.substring(1);
            // Determinar si este nodo es el coordinador (opcional, se puede redirigir)
            String owner = ring.getNode(key);
            if (!owner.equals(localNodeId)) {
                resp.setStatus(307);
                resp.setHeader("Location", "http://" + owner + "/kv/" + key);
                return;
            }
            String value = store.get(key);
            if (value == null) {
                resp.setStatus(404);
            } else {
                resp.setStatus(200);
                resp.getWriter().write(value);
            }
        }

        @Override
        protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) {
                resp.setStatus(400);
                return;
            }
            String key = path.substring(1);
            String value = req.getReader().lines().reduce("", (a,b) -> a + b);
            // Escribir localmente
            store.put(key, value);
            // Replicar
            replicator.writeReplicas(key, value, localNodeId);
            resp.setStatus(200);
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) {
                resp.setStatus(400);
                return;
            }
            String key = path.substring(1);
            store.delete(key);
            replicator.deleteReplicas(key, localNodeId);
            resp.setStatus(200);
        }
    }
}