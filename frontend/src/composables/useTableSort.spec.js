import { describe, expect, it } from 'vitest'
import { ref } from 'vue'
import { useTableSort } from './useTableSort'

describe('useTableSort', () => {
  it('sorts rows by default key and toggles direction', () => {
    const rows = ref([
      { id: 1, name: 'Charlie' },
      { id: 2, name: 'Alpha' },
      { id: 3, name: 'Bravo' }
    ])
    const { sortedRows, toggleSort, sortIndicator } = useTableSort(rows, { initialKey: 'name' })

    expect(sortedRows.value.map((r) => r.name)).toEqual(['Alpha', 'Bravo', 'Charlie'])
    expect(sortIndicator('name')).toBe('⬆')

    toggleSort('name')
    expect(sortedRows.value.map((r) => r.name)).toEqual(['Charlie', 'Bravo', 'Alpha'])
    expect(sortIndicator('name')).toBe('⬇')
  })

  it('supports custom getter and date sorting', () => {
    const rows = ref([
      { id: 1, user: { displayName: 'Beta' }, createdAt: '2026-03-01T10:00:00Z' },
      { id: 2, user: { displayName: 'Alpha' }, createdAt: '2026-01-01T10:00:00Z' },
      { id: 3, user: { displayName: 'Gamma' }, createdAt: '2026-02-01T10:00:00Z' }
    ])
    const { sortedRows, toggleSort } = useTableSort(rows, {
      initialKey: 'createdAt',
      valueGetters: {
        user: (row) => row.user.displayName
      }
    })

    expect(sortedRows.value.map((r) => r.id)).toEqual([2, 3, 1])

    toggleSort('user')
    expect(sortedRows.value.map((r) => r.id)).toEqual([2, 1, 3])
  })
})
