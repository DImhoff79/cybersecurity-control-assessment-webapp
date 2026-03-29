import { describe, expect, it } from 'vitest'
import {
  controlDisplayName,
  controlIdentifierDisplay,
  responderAudienceBadgeClass,
  responderAudienceLabel
} from './controlDisplay'

describe('controlIdentifierDisplay', () => {
  it('strips KCF_ prefix', () => {
    expect(controlIdentifierDisplay({ controlId: 'KCF_AC-1' })).toBe('AC-1')
  })
  it('passes through non-Kroger ids', () => {
    expect(controlIdentifierDisplay({ controlId: 'AC-2' })).toBe('AC-2')
  })
})

describe('responderAudienceLabel', () => {
  it('maps API enum values', () => {
    expect(responderAudienceLabel('APPLICATION_OWNER')).toBe('App owner')
    expect(responderAudienceLabel('SECURITY_COMPLIANCE')).toBe('Security / compliance')
    expect(responderAudienceLabel('MIXED')).toBe('Mixed')
  })
  it('returns badge classes', () => {
    expect(responderAudienceBadgeClass('APPLICATION_OWNER')).toContain('primary')
    expect(responderAudienceBadgeClass('SECURITY_COMPLIANCE')).toContain('secondary')
  })
})

describe('controlDisplayName', () => {
  it('strips leading id from V42-style name', () => {
    expect(
      controlDisplayName({
        controlId: 'KCF_AC-1',
        name: 'AC-1 Access Control Policy and Procedures'
      })
    ).toBe('Access Control Policy and Procedures')
  })
  it('strips leading id when name already has plain title', () => {
    expect(
      controlDisplayName({
        controlId: 'KCF_AC-3',
        name: 'AC-3 Access Enforcement'
      })
    ).toBe('Access Enforcement')
  })
  it('handles V38 em-dash seed names', () => {
    expect(
      controlDisplayName({
        controlId: 'KCF_AC-2',
        name: 'AC-2a — AC-2 Account Management'
      })
    ).toBe('Account Management')
  })
  it('falls back to identifier when name is only the id', () => {
    expect(controlDisplayName({ controlId: 'KCF_AC-1', name: 'AC-1' })).toBe('AC-1')
  })
})
