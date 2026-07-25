---
name: sla-benchmark-runner
description: Generates automated k6 and Gatling benchmark scripts to verify SLA performance thresholds from config.json.
---

# SLA Benchmark Generator Skill (`/gen-benchmark`)

When generating performance load test scripts:

## 1. SLA Threshold Lookup
Read targets directly from `.agents/config.json` (`qualityGoalsAndSLA` block):
* Off-heap JWT verification: `hotPathJwtVerifyMsP99` ($\le 1.0\text{ ms}$)
* Governance check: `governanceEvaluationMsP99` ($\le 0.1\text{ ms}$)
* Command palette search: `globalSearchPaletteMsP99` ($\le 100.0\text{ ms}$)
* Cross-repo mount: `crossRepoMountMsP99` ($\le 1.0\text{ ms}$)

## 2. k6 Benchmark Script (`/benchmarks/k6-hotpath.js`)
```javascript
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    layer_download_hotpath: {
      executor: 'constant-arrival-rate',
      rate: 1000,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 50,
    },
  },
  thresholds: {
    http_req_duration: ['p(99)<1.0'], // Enforces SLA from config.json
  },
};

export default function () {
  const params = {
    headers: { Authorization: `Bearer ${__ENV.JWT_TOKEN}` },
  };
  const res = http.get('http://localhost:8080/v2/test/blobs/sha256:e3b0c442...', params);
  check(res, { 'status is 200': (r) => r.status === 200 });
}
```

## 3. Gatling Benchmark Simulation (`/benchmarks/GatlingHotPathSimulation.java`)
```java
package io.omnidepot.benchmark;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class GatlingHotPathSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/octet-stream");

    ScenarioBuilder scn = scenario("Layer Stream Hot Path")
            .exec(http("Fetch Layer Blob")
                    .get("/v2/test/blobs/sha256:e3b0c442...")
                    .header("Authorization", "Bearer ${jwtToken}")
                    .check(status().is(200)));

    {
        setUp(
                scn.injectOpen(constantUsersPerSec(1000).during(30))
        ).protocols(httpProtocol)
         .assertions(global().responseTime().percentile(99.0).lt(1));
    }
}
```
