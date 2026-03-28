import { describe, expect, it } from 'vitest'
import { findLongestNavMatch } from './adminNavMatch'

describe('findLongestNavMatch', () => {
  const sections = [
    {
      key: 'a',
      label: 'A',
      items: [
        { to: '/admin', label: 'Home' },
        { to: '/admin/audits', label: 'Audits' }
      ]
    }
  ]

  it('returns null for empty input', () => {
    expect(findLongestNavMatch('', sections)).toBeNull()
    expect(findLongestNavMatch('/admin', null)).toBeNull()
  })

  it('prefers longest matching prefix', () => {
    const m = findLongestNavMatch('/admin/audits/42', sections)
    expect(m.item.to).toBe('/admin/audits')
    expect(m.section.key).toBe('a')
  })

  it('matches exact path', () => {
    const m = findLongestNavMatch('/admin', sections)
    expect(m.item.to).toBe('/admin')
  })
})
