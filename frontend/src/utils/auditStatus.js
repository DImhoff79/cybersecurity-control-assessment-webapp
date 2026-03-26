/** Owner has submitted; audit is in the approval pipeline (not yet attested). */
export const AUDIT_SUBMITTED_PIPELINE = ['PENDING_APPROVAL', 'REVISIONS_REQUESTED', 'AUDITOR_APPROVED']

/** Legacy API / cached rows may still use SUBMITTED — treat like pending approval. */
const LEGACY_SUBMITTED = 'SUBMITTED'

/** Owner may not edit questionnaire answers (revisions requested is editable). */
export function isAuditOwnerAnswerLocked(status) {
  if (!status) return false
  if (status === LEGACY_SUBMITTED) return true
  return ['PENDING_APPROVAL', 'AUDITOR_APPROVED', 'ATTESTED', 'COMPLETE'].includes(status)
}

export function auditStatusLabel(status) {
  switch (status) {
    case LEGACY_SUBMITTED:
    case 'PENDING_APPROVAL':
      return 'Submitted — pending auditor approval'
    case 'REVISIONS_REQUESTED':
      return 'Revisions requested'
    case 'AUDITOR_APPROVED':
      return 'Auditor approved — pending attestation'
    case 'COMPLETE':
      return 'Validated complete'
    case 'ATTESTED':
      return 'Attested — pending final completion'
    case 'IN_PROGRESS':
      return 'In progress'
    case 'DRAFT':
      return 'Draft'
    default:
      return status || '-'
  }
}

export function auditStatusBadgeClass(status) {
  switch (status) {
    case 'COMPLETE':
      return 'text-bg-success'
    case 'ATTESTED':
      return 'text-bg-primary'
    case LEGACY_SUBMITTED:
    case 'PENDING_APPROVAL':
      return 'text-bg-info'
    case 'REVISIONS_REQUESTED':
      return 'text-bg-warning'
    case 'AUDITOR_APPROVED':
      return 'text-bg-success'
    case 'IN_PROGRESS':
      return 'text-bg-warning'
    case 'DRAFT':
      return 'text-bg-secondary'
    default:
      return 'text-bg-secondary'
  }
}

export function auditStageLabel(status) {
  switch (status) {
    case 'DRAFT':
      return 'Planning'
    case 'IN_PROGRESS':
      return 'Fieldwork'
    case LEGACY_SUBMITTED:
    case 'PENDING_APPROVAL':
      return 'Auditor approval'
    case 'REVISIONS_REQUESTED':
      return 'Owner revision'
    case 'AUDITOR_APPROVED':
      return 'Manager attestation'
    case 'ATTESTED':
      return 'Final validation'
    case 'COMPLETE':
      return 'Closed'
    default:
      return 'Unknown'
  }
}
