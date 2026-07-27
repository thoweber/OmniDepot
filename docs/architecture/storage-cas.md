# Storage Context & Content-Addressable Storage (CAS)

OmniDepot utilizes a pure **Content-Addressable Storage (CAS)** model for all binary artifact payloads across all supported formats (OCI, Maven, NPM).

---

## Storage Architecture

```mermaid
graph TD
    Client["Protocol Adapter / Ingestion"] --> |Sha256Digest + InputStream| BlobStore["BlobStore SPI"]

    subgraph Storage Providers
        FS["repo-storage-fs: FileSystemBlobStore"]
        S3["repo-storage-s3: S3BlobStore"]
    end

    BlobStore -->|@LookupIfProperty filesystem| FS
    BlobStore -->|@LookupIfProperty s3| S3

    FS --> |Write to Path| Disk["/blobs/sha256/e3/b0/c4..."]
    S3 --> |5MB Buffer Aggregation| RustFS["s3://bucket/blobs/sha256/e3/b0/c4..."]
```

---

## Key Invariants & Features

1. **SHA-256 Deduplication (ADR-015):**
   - All physical binary storage paths are determined strictly by the payload's SHA-256 digest: `/blobs/sha256/<first2>/<next2>/<hash>`.
   - Uploading identical files across multiple repositories or protocols yields a $1.0\times$ physical storage footprint (70%+ S3 storage cost reduction).

2. **Off-Heap Netty Streaming (ADR-006):**
   - Binary streams are transferred off-heap directly between Netty network buffers and S3/file channels, avoiding STW Garbage Collection pauses.

3. **S3 5 MB Part Aggregation (ADR-025):**
   - AWS S3 requires non-final parts in Multipart Uploads to be $\ge 5\text{ MB}$.
   - `S3BlobStore` buffers incoming chunks in off-heap Netty memory until $\ge 5,242,880\text{ bytes}$ before calling `s3Client.uploadPart()`.

4. **Two-Phase Tombstone GC (ADR-018):**
   - Deleted manifests undergo a 48-hour tombstone soft-deletion grace period.
   - Physical CAS blob removal occurs only after recursive DAG traversal confirms zero active manifest references.
