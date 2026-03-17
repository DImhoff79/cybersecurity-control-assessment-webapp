# Phase 2 Risk and Remediation Design

## Objective
Add a first-class risk register and structured remediation (CAPA) workflows linked to existing findings and control exceptions.

## Proposed Domain Model

- `risk_register_items`
  - `id`, `title`, `description`, `business_impact`, `likelihood_score`, `impact_score`, `inherent_risk_score`, `residual_risk_score`
  - `owner_user_id`, `application_id`, `status`, `target_close_at`, `created_at`, `updated_at`
- `risk_control_links`
  - map risk to `controls` with optional effectiveness notes
- `risk_finding_links`
  - map risk to existing `findings`
- `risk_exception_links`
  - map risk to existing `control_exceptions`
- `remediation_plans`
  - `id`, `risk_id`, `title`, `status`, `target_complete_at`, `created_by_user_id`, timestamps
- `remediation_actions`
  - `id`, `plan_id`, `action_title`, `action_detail`, `owner_user_id`, `due_at`, `status`, `sequence`, timestamps
- `remediation_dependencies`
  - predecessor/successor relationships for actions

## Workflow

1. Create risk item and score inherent risk.
2. Link relevant controls/findings/exceptions.
3. Create remediation plan and discrete actions.
4. Track action completion and roll-up risk residual score.
5. Escalate overdue remediation actions via notification engine.

## API Surface (Proposed)

- `/api/risks` (list/create/update)
- `/api/risks/{id}/links/controls`
- `/api/risks/{id}/links/findings`
- `/api/risks/{id}/links/exceptions`
- `/api/remediation-plans` (list/create/update)
- `/api/remediation-plans/{id}/actions` (CRUD)
- `/api/remediation-plans/{id}/timeline`

## UI Surface (Proposed)

- Admin routes:
  - `/admin/risk-register`
  - `/admin/remediation-plans`
- Dashboard additions:
  - open high risks, overdue remediation actions, residual risk trend

## RBAC

- `ADMIN`, `AUDIT_MANAGER`: full create/edit/close rights
- `AUDITOR`: update execution and evidence
- `APPLICATION_OWNER`: action completion on assigned remediation tasks
