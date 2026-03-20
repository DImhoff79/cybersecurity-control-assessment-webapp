# Phase 3 Automation and Integration Readiness

## In-product program depth (delivered incrementally)

- **Issue Program Hub** (`/admin/issue-program`, `AUDIT_MANAGEMENT`): audit-scoped roll-up of findings, control exceptions, and (when permitted) application-filtered risks and remediation plans, with shareable `?auditId=` URLs and deep links to Findings, Exceptions, Risk Register (`?applicationId=` / `applicationName`), and Remediation Plans.
- **Finding ↔ control exception linkage**: optional `finding_id` on `control_exceptions` (Flyway `V30`); API accepts `findingId` on create; list supports `findingId`; `FindingDto.linkedExceptionCount` drives Findings UI links to filtered Exceptions (`?auditId=` + `findingId`).

## Objective
Prepare the platform for enterprise-scale automation with external evidence connectors, continuous control monitoring, and configurable workflow orchestration.

## Integration Backbone

- Introduce `integration_connectors` table:
  - connector type, auth mode, encrypted secrets reference, sync cadence, last sync metadata
- Introduce `integration_jobs` and `integration_job_runs`:
  - queued/running/succeeded/failed states, retry metadata, structured error payload
- Introduce `external_evidence_links`:
  - map external source records to internal `audit_evidence` and policy/compliance objects

## Continuous Control Monitoring

- `control_monitor_rules`
  - detection query/condition, scope filters, severity, schedule
- `control_monitor_events`
  - event payload, status, ack state, linked finding/risk ids
- Optional first connectors:
  - ticketing systems (Jira/ServiceNow)
  - cloud posture systems
  - identity systems

## Workflow Engine Enhancements

- Rule-driven approvals:
  - approval chains by policy type, control framework, risk tier
- SLA policy templates:
  - response, remediation, escalation timers
- Segregation-of-duties checks:
  - prevent requester/approver identity collisions for key events

## Backlog Prioritization

1. Integration job framework and secrets management interface
2. One connector (ticketing) plus evidence-linking
3. Control monitoring rules with event-to-finding conversion
4. Approval matrix configuration UI
5. SLA template designer and alert tuning

## NFR and Operations

- Immutable audit logging for automation actions
- Idempotent connector ingest
- Dead-letter handling for failed integration runs
- Performance targets:
  - connector ingest throughput
  - dashboard refresh SLA
  - scheduled job completion windows
