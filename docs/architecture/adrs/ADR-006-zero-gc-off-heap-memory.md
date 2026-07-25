# ADR-006: Zero-GC and Off-Heap Direct Memory Allocation Rules

* **Status:** Accepted

## Context
Streaming multi-gigabyte files through Java heap memory triggers frequent Stop-The-World (STW) Garbage Collection pauses under heavy load.

## Decision
Enforce zero-copy, off-heap streaming mechanics. Pass direct Netty `ByteBuf` instances directly from Vert.x network sockets to S3 or local file channels without transferring byte arrays into the Java heap.

## Consequences

### Positive
- Completely eliminates STW Garbage Collection pauses during large artifact downloads, maintaining predictable low-latency performance under load.

### Negative
- Requires manual, strict Netty reference counting (`ByteBuf.release()`), creating risks of off-heap memory leaks if buffers are mismanaged.

## Non-Negotiable Invariants
- Do not buffer entire file payloads in memory or convert binary streams into heap-allocated `byte[]` arrays.
