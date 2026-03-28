import { describe, expect, it } from 'vitest'
import { formatLoadError } from './loadErrorFormat'

describe('formatLoadError', () => {
  it('joins object message fields', () => {
    const msg = formatLoadError({
      response: { status: 404, data: { message: 'No demo', detail: 'Run Flyway' } }
    })
    expect(msg).toContain('No demo')
    expect(msg).toContain('Run Flyway')
  })

  it('handles 403', () => {
    expect(formatLoadError({ response: { status: 403 } })).toContain('403')
  })

  it('handles network error', () => {
    expect(formatLoadError({ code: 'ERR_NETWORK', message: 'Network Error' })).toContain('8080')
  })

  it('falls back to message', () => {
    expect(formatLoadError({ message: 'oops' })).toBe('oops')
  })
})
