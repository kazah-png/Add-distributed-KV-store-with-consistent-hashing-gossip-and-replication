<div align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:0d1117,50:0a1f0a,100:0d2b0d&height=130&section=header&text=distributed-kv-store&fontSize=36&fontColor=e6edf3&animation=fadeIn&fontAlignY=55" />
</div>

<div align="center">

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://java.com)
[![License](https://img.shields.io/badge/License-MIT-3fb950?style=flat)](LICENSE)
[![Docker](https://img.shields.io/badge/Docker-3--node%20cluster-2496ED?style=flat&logo=docker&logoColor=white)]()

**Dynamo-style distributed key-value store in Java.**  
Consistent hashing · Gossip membership · Replication factor 2 · HTTP API · Disk persistence

</div>

---

## Overview

A distributed, fault-tolerant key-value store inspired by Amazon Dynamo. Data is partitioned across nodes using a **consistent hash ring** with virtual nodes. Writes are replicated to the primary node and one replica. Cluster membership is maintained via a **gossip protocol** over UDP — nodes discover each other, detect failures, and update the ring without a central coordinator.

All operations are exposed through a simple HTTP API. If a request arrives at the wrong node, it is transparently redirected to the correct coordinator.

---

## Architecture

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Node 1    │◄──►│   Node 2    │◄──►│   Node 3    │
│ HTTP :8081  │    │ HTTP :8082  │    │ HTTP :8083  │
│ Gossip:9091 │    │ Gossip:9092 │    │ Gossip:9093 │
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘
       └──────────────────┴──────────────────┘
                   Consistent Hash Ring
```

Each node runs three components independently:

- **Jetty** — HTTP API server
- **Gossip** — UDP heartbeat listener and sender
- **Local store** — `ConcurrentHashMap` backed by disk persistence

---

## Features

| Feature | Details |
|---|---|
| **Consistent hash ring** | Virtual nodes for even load distribution; ring updated on gossip membership changes |
| **Gossip membership** | UDP heartbeats; nodes mark peers as failed after missed intervals |
| **Replication** | Each key stored on primary + one replica (factor configurable in `Replicator`) |
| **Transparent redirect** | Request at wrong node returns HTTP 307 to the correct coordinator |
| **Disk persistence** | Each partition serialized to local disk; survives node restart |
| **Docker Compose** | 3-node cluster with a single command |

---

## Quick Start

### Docker Compose (3-node cluster)

```bash
docker-compose up --build
```

### From source

```bash
mvn clean package

# Node 1
java -jar target/distributed-kv-1.0-SNAPSHOT.jar \
  node1 localhost 8081 9091 node2:8082,node3:8083

# Node 2 (separate terminal)
java -jar target/distributed-kv-1.0-SNAPSHOT.jar \
  node2 localhost 8082 9092 node1:8081,node3:8083
```

---

## API

### Write a value

```bash
curl -X PUT http://localhost:8081/kv/my-key -d "my-value"
```

The request routes to the coordinator for `my-key` on the hash ring. The coordinator writes locally and replicates to one additional node.

### Read a value

```bash
curl http://localhost:8081/kv/my-key
```

If `my-key` does not belong to this node, the response is an HTTP 307 redirect to the correct node.

### Delete a key

```bash
curl -X DELETE http://localhost:8081/kv/my-key
```

---

## Configuration

Arguments at startup:

| Argument | Description |
|---|---|
| `nodeId` | Unique identifier (e.g. `node1`) |
| `host` | IP or hostname for this node |
| `httpPort` | HTTP API port |
| `gossipPort` | UDP gossip port |
| `seedNodes` | Comma-separated `host:port` list of initial peers |

Replication factor is set in `Replicator.java` (default: 2).

---

## Consistency model

This implementation uses **eventual consistency** — no quorum. Writes always succeed even if replicas are temporarily unreachable. Conflicts use last-write-wins (wall clock). This matches the Dynamo paper's simplified availability-first approach.

For stronger guarantees, replace the replication path with a quorum write (W + R > N).

---

## Limitations

- **No key rebalancing on join/leave** — adding or removing a node does not migrate existing keys. Keys on the old ring segments are unreachable until the node returns.
- **No conflict resolution beyond LWW** — vector clocks or a merge function would be needed for richer semantics.
- **No authentication** — suitable for trusted internal networks only.

---

<div align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:0d2b0d,50:0a1f0a,100:0d1117&height=80&section=footer" />
</div>
