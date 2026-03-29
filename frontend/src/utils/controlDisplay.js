/**
 * Spreadsheet-style labels for controls: column B = identifier (e.g. AC-1), column C = title only
 * (e.g. "Access Control Policy and Procedures"), never the concatenated requirement text.
 */

const WS = /\s+/g

/** NIST-style control id at the start of a label (AC-1, AC-2a, SC-13(1), CP-9 (8), …). */
const LEADING_CONTROL_PREFIX =
  /^[A-Z]{2}-\d+[a-z]?(?:\s*\(\d+\))*(?:\([a-z]\))*\s+/i

function normalizeWs(s) {
  return String(s).replace(WS, ' ').trim()
}

/**
 * Column B: human identifier without internal KCF_ prefix (stored ids stay KCF_*).
 * @param {string|{ controlId?: string }} c
 */
export function controlIdentifierDisplay(c) {
  const id = typeof c === 'string' ? c : (c?.controlId || '')
  const t = id.trim()
  if (t.startsWith('KCF_')) return t.slice(4)
  return t || '—'
}

function stripLeadingControlIdTokens(str, control) {
  let s = normalizeWs(str)
  const idSuffix = (control?.controlId || '').replace(/^KCF_/i, '').trim()
  if (idSuffix) {
    const esc = idSuffix.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const re = new RegExp(`^${esc}\\s+`, 'i')
    const t = s.replace(re, '').trim()
    if (t) s = t
  }
  for (let i = 0; i < 3; i++) {
    const m = s.match(LEADING_CONTROL_PREFIX)
    if (!m) break
    const rest = s.slice(m[0].length).trim()
    if (!rest) break
    s = rest
  }
  return s
}

/**
 * Column C: plain title only (no leading control numbers).
 * @param {{ name?: string, controlId?: string }} c
 */
/** API field `responderAudience` on controls (derived from questions' ask_owner). */
export function responderAudienceLabel(value) {
  switch (value) {
    case 'APPLICATION_OWNER':
      return 'App owner'
    case 'SECURITY_COMPLIANCE':
      return 'Security / compliance'
    case 'MIXED':
      return 'Mixed'
    default:
      return '—'
  }
}

export function responderAudienceBadgeClass(value) {
  switch (value) {
    case 'APPLICATION_OWNER':
      return 'text-bg-primary'
    case 'SECURITY_COMPLIANCE':
      return 'text-bg-secondary'
    case 'MIXED':
      return 'text-bg-warning'
    default:
      return 'text-bg-light border'
  }
}

export function controlDisplayName(c) {
  const raw = normalizeWs(c?.name || '')
  const fallbackId = controlIdentifierDisplay(c)
  if (!raw) return fallbackId

  // V38 seed: "AC-2a — AC-2 Account Management" → "Account Management"
  if (/[—–]/.test(raw)) {
    const parts = raw.split(/\s*[—–]\s*/).map((p) => normalizeWs(p)).filter(Boolean)
    if (parts.length >= 2) {
      const tail = parts.slice(1).join(' ')
      const tailPlain = stripLeadingControlIdTokens(tail, c)
      if (tailPlain) return tailPlain
    }
  }

  const s = stripLeadingControlIdTokens(raw, c)
  if (!s || s === fallbackId) return fallbackId
  return s
}
