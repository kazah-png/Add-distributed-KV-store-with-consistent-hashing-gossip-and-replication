<div align="center">

# Distributed KV Store — Consistent Hashing + Gossip + Replication

**A Dynamo‑style distributed key‑value store built from scratch in Java**  
HTTP API · Replication factor 2 · Node membership via gossip

[![Java](https://img.shields.io/badge/Java-17-red?style=flat-square&logo=java)](https://java.com)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Senior%20Portfolio-green?style=flat-square)]()

</div>

---

## Overview

This project is a **distributed, fault‑tolerant key‑value store** inspired by Amazon Dynamo. It partitions data using **consistent hashing**, replicates writes to `N` nodes (default 2), and maintains a cluster membership list via a **gossip protocol**. All operations are exposed through a simple HTTP API. The system handles node joins, failures, and provides eventual consistency (no quorum to keep it simple but extensible).

---

## Features

- **Consistent hash ring** – partition data across nodes; supports virtual nodes for load balancing.
- **Gossip membership** – nodes discover each other using UDP heartbeats; failure detection.
- **Replication** – each key is stored on the primary node + one replica (factor configurable).
- **HTTP REST API** – `GET /kv/{key}`, `PUT /kv/{key}`, `DELETE /kv/{key}`.
- **Redirect on miss** – if a request arrives at the wrong node, it redirects to the coordinator.
- **Disk persistence** – each node stores its partition locally.
- **Docker Compose** – spin up a 3‑node cluster in seconds.

---

## Quick Start

### Build
```bash
mvn clean package
Run a single node (for testing)
bash
java -jar target/distributed-kv-1.0-SNAPSHOT.jar node1 localhost 8080 9090 node2:8080,node3:8080
Run with Docker Compose (3‑node cluster)
bash
docker-compose up --build
Interact with the cluster
bash
# Put a value (goes to primary + replica)
curl -X PUT http://localhost:8081/kv/foo -d "bar"

# Get the value (may redirect if wrong node)
curl http://localhost:8081/kv/foo

# Delete
curl -X DELETE http://localhost:8081/kv/foo
Architecture
text
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Node 1    │    │   Node 2    │    │   Node 3    │
│ HTTP :8081  │◄──►│ HTTP :8082  │◄──►│ HTTP :8083  │
│ Gossip:9091 │    │ Gossip:9092 │    │ Gossip:9093 │
└─────────────┘    └─────────────┘    └─────────────┘
      │                   │                   │
      └───────────────────┼───────────────────┘
                    Consistent Hash Ring
Each node runs:

Jetty for HTTP API

Gossip (UDP) for membership

Local store (ConcurrentHashMap + disk persistence)

Hash ring (updated via gossip alive nodes)

Configuration
Edit NodeConfig or pass arguments:

nodeId – unique identifier (e.g. node1)

host – IP or hostname

httpPort – API port

gossipPort – UDP port for gossip

seedNodes – comma‑separated list of initial nodes (format host:port)

Replication factor is hardcoded to 2 (can be changed in Replicator).

Limitations & Future Work
No quorum writes – writes always succeed even if replicas are down (simplified).

No conflict resolution – last write wins (LWW) for simplicity.

No data rebalancing on node join/leave (you would need to move keys). Left as an exercise.

Single‑threaded per node – Jetty handles concurrency, but store uses ConcurrentHashMap which is fine.

Why This Is a Senior Portfolio Project
Implements two distributed algorithms (consistent hashing + gossip)

Uses raw sockets, concurrency, HTTP server programming

Demonstrates understanding of eventual consistency, replication, failure detection

Entirely from scratch – no external dependency for distribution logic (only Jetty, Jackson, Quartz)

Ready to scale horizontally

License
MIT

</div> ```