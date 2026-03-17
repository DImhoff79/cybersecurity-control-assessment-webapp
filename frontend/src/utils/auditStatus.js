export function auditStatusLabel(status) {
  switch (status) {
    case 'SUBMITTED':
      return 'Completed - pending admin review'
    case 'COMPLETE':
      return 'Validated complete'
    case 'ATTESTED':
      return 'Attested - pending final completion'
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
    case 'SUBMITTED':
      return 'text-bg-info'
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
    case 'SUBMITTED':
      return 'Manager review'
    case 'ATTESTED':
      return 'Final Validation'
    case 'COMPLETE':
      return 'Closed'
    default:
      return 'Unknown'
  }
}
