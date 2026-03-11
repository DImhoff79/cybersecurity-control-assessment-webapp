# Scalability and Reliability Playbook

## Service Level Objectives

These targets are also mirrored in config under `app.scalability.slos`:

- API latency (all authenticated API routes): p95 <= 350ms
- API error rate (5xx + timeouts): < 1.0%
- Report generation latency (`/api/reports/*` exports): p95 <= 4s
- Evidence upload latency (`/api/evidences/upload`): p95 <= 2.5s

## Load and capacity baselines

- Target steady load: 200 concurrent users
- Peak load target: 500 concurrent users for 15 minutes
- Data profile: 50k audits, 500k control records, 200k evidence uploads

## Observability endpoints

- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Scaling model

### Backend

- Run multiple API replicas behind a load balancer.
- Use PostgreSQL in non-local environments.
- Scheduler jobs use DB-backed locks (`shedlock`) so only one replica runs a given schedule.
- Evidence payloads are persisted in the database to avoid node-local disk coupling.

### Frontend

- Serve built assets through CDN or edge cache.
- API calls stay same-origin through reverse proxy.

## Autoscaling policy (recommended defaults)

- Scale out when any trigger is true for 5 minutes:
  - CPU > 65%
  - p95 latency > 350ms
  - HTTP 5xx rate > 1%
- Scale in when all are true for 10 minutes:
  - CPU < 35%
  - p95 latency < 250ms
  - HTTP 5xx rate < 0.2%
- Keep minimum 2 replicas for high availability.

## Rollout sequence

1. Enable metrics scraping and dashboards.
2. Validate SLO dashboards in staging.
3. Enable production alerts tied to SLO thresholds.
4. Turn on autoscaling with conservative min/max bounds.
5. Re-run synthetic and load tests after each major release.
