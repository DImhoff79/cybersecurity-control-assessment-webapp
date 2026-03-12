import { computed, ref } from 'vue'

function compareValues(left, right) {
  if (left == null && right == null) return 0
  if (left == null) return -1
  if (right == null) return 1

  if (typeof left === 'number' && typeof right === 'number') {
    return left - right
  }

  const leftDate = Date.parse(left)
  const rightDate = Date.parse(right)
  if (!Number.isNaN(leftDate) && !Number.isNaN(rightDate)) {
    return leftDate - rightDate
  }

  return String(left).localeCompare(String(right), undefined, { sensitivity: 'base' })
}

export function useTableSort(rows, options = {}) {
  const {
    initialKey = '',
    initialDirection = 'asc',
    valueGetters = {}
  } = options

  const sortKey = ref(initialKey)
  const sortDirection = ref(initialDirection)

  const sortedRows = computed(() => {
    const inputRows = rows.value || []
    if (!sortKey.value) return inputRows

    const getter = valueGetters[sortKey.value] || ((row) => row?.[sortKey.value])
    const ordered = [...inputRows].sort((a, b) => compareValues(getter(a), getter(b)))
    return sortDirection.value === 'asc' ? ordered : ordered.reverse()
  })

  function toggleSort(key) {
    if (sortKey.value === key) {
      sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
      return
    }
    sortKey.value = key
    sortDirection.value = 'asc'
  }

  function sortIndicator(key) {
    if (sortKey.value !== key) return '⇅'
    return sortDirection.value === 'asc' ? '⬆' : '⬇'
  }

  return {
    sortedRows,
    toggleSort,
    sortIndicator
  }
}
