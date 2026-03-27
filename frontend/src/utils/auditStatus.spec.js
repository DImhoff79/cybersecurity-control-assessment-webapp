import { describe, expect, it } from 'vitest'
import { auditStageLabel, auditStatusLabel, isAuditCompleted } from './auditStatus'

describe('auditStatus helpers', () => {
  it('maps status into user-facing labels and stage', () => {
    expect(auditStatusLabel('IN_PROGRESS')).toBe('In progress')
    expect(auditStatusLabel('SUBMITTED')).toBe('Submitted — pending auditor approval')
    expect(auditStatusLabel('PENDING_APPROVAL')).toBe('Submitted — pending auditor approval')
    expect(auditStageLabel('IN_PROGRESS')).toBe('Fieldwork')
    expect(auditStageLabel('COMPLETE')).toBe('Closed')
  })

  it('detects completed (closed) audits', () => {
    expect(isAuditCompleted('COMPLETE')).toBe(true)
    expect(isAuditCompleted('IN_PROGRESS')).toBe(false)
    expect(isAuditCompleted('ATTESTED')).toBe(false)
  })
})
