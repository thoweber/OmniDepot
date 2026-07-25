# ADR-018: Two-Phase Tombstone Garbage Collection & DAG Traversal

* **Status:** Accepted

## Context
Deleting container manifests or multi-arch indexes without checking underlying layer references can delete shared layers still needed by other images.

## Decision
Execute a two-phase GC algorithm. Soft-delete unreferenced artifacts by marking them with a 48-hour tombstone timestamp. Perform recursive Directed Acyclic Graph (DAG) traversal across multi-arch index manifests before purging orphaned CAS blobs.

## Consequences

### Positive
- Prevents catastrophic layer corruption across shared container images; provides a 48-hour safety net to recover accidentally deleted artifacts.

### Negative
- Physical storage space is not reclaimed immediately upon deletion (delayed by 48h grace period); requires executing recursive DAG queries.

## Non-Negotiable Invariants
- Physical blob deletion is forbidden without verifying zero active references across the entire manifest DAG.
