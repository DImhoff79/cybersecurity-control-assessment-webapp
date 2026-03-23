/**
 * Find the sidebar nav item whose `to` is the longest prefix of `path`
 * (so /admin/audits/12 matches "Audits" at /admin/audits, not a shorter prefix).
 */
export function findLongestNavMatch(path, sections) {
  if (!path || !sections?.length) return null
  let best = null
  let bestLen = -1
  for (const section of sections) {
    for (const item of section.items) {
      const to = item.to
      if (path === to || path.startsWith(`${to}/`)) {
        if (to.length > bestLen) {
          bestLen = to.length
          best = { section, item }
        }
      }
    }
  }
  return best
}
