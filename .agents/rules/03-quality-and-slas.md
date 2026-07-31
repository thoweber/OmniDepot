# omnidepot Quality Goals & SLA Enforcement

> All numerical SLA thresholds are configured strictly in `.agents/config.json` under `qualityGoalsAndSLA`.

## Verification Protocols

1. **Memory & Boot Verification:**
   * Validate idle RAM ($\le 80\text{ MB}$) and native cold boot ($\le 0.8\text{s}$) metrics in container audits.
2. **Hot-Path Read Isolation:**
   * Ensure zero synchronous database queries execute during active layer streaming.
   * Verify off-heap JWT signature validation latency ($P_{99} \le 1.0\text{ ms}$) using `/gen-benchmark`.
3. **Storage Deduplication & Buffering:**
   * Verify that identical binary uploads across OCI, Maven, and NPM yield a $1.0\times$ storage redundancy ratio in CAS.
   * Confirm that active S3 chunk buffers accumulate $\ge 5,242,880\text{ bytes}$ before dispatching `uploadPart()`.
4. **Clustered Outbox & Resilience:**
   * Verify multi-node outbox polling using `FOR UPDATE SKIP LOCKED` guarantees zero duplicate event dispatches and zero lock wait timeouts under load.
   * Verify proxy fallback returns HTTP `200 OK` with header `Warning: 110` during simulated upstream network outages.

## 5. Global CI & SonarCloud Polling / Waiting Strategy

* **Initial Wait Threshold:** When waiting for SonarCloud or GitHub Actions workflow results after a push or trigger, assume an initial wait time of 2 minutes (120 seconds) before checking status for the first time.
* **Status Polling Interval (`gh run view`):** For any commands checking workflow status or job execution (including calls starting with `gh run view`), only recheck status after waiting at least 45 seconds between subsequent polls.
