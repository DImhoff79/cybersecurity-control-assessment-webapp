import { describe, expect, it } from 'vitest'
import { auditStageLabel, auditStatusBadgeClass, auditStatusLabel } from './auditStatus'

describe('auditStatus helpers', () => {
  it('maps status into user-facing labels and stage', () => {
    expect(auditStatusLabel('IN_PROGRESS')).toBe('In progress')
    expect(auditStatusLabel('SUBMITTED')).toBe('Completed - pending admin review')
    expect(auditStageLabel('IN_PROGRESS')).toBe('Fieldwork')
    expect(auditStageLabel('COMPLETE')).toBe('Closed')
  })

  it('maps status into consistent badge classes', () => {
    expect(auditStatusBadgeClass('ATTESTED')).toBe('text-bg-primary')
    expect(auditStatusBadgeClass('UNKNOWN')).toBe('text-bg-secondary')
  })
})
