import { describe, expect, it } from 'vitest'
import { findLongestNavMatch } from './adminNavMatch'

describe('findLongestNavMatch', () => {
  const sections = [
    {
      key: 'a',
      label: 'A',
      items: [
        { label: 'Audits', to: '/admin/audits' },
        { label: 'Overview', to: '/admin' }
      ]
    }
  ]

  it('returns exact match', () => {
    const m = findLongestNavMatch('/admin/audits', sections)
    expect(m?.item.label).toBe('Audits')
  })

  it('prefers longest prefix for nested paths', () => {
    const m = findLongestNavMatch('/admin/audits/12', sections)
    expect(m?.item.label).toBe('Audits')
    expect(m?.item.to).toBe('/admin/audits')
  })

  it('does not match sibling paths as children', () => {
    const m = findLongestNavMatch('/admin/audit-queue', sections)
    expect(m?.item.label).toBe('Overview')
  })

  it('returns null when no item matches', () => {
    const m = findLongestNavMatch('/other', sections)
    expect(m).toBeNull()
  })
})
