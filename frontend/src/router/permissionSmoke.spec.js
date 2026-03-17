import { describe, expect, it } from 'vitest'
import router from './index'
import { adminSections } from '../config/adminNavigation'

function routeByPath(path) {
  return router.getRoutes().find((route) => route.path === path)
}

function canAccessAdmin(role) {
  return role === 'ADMIN' || role === 'AUDIT_MANAGER' || role === 'AUDITOR'
}

function canAccessRoute(path, persona) {
  const route = routeByPath(path)
  if (!route) return false
  if (path.startsWith('/admin') && !canAccessAdmin(persona.role)) return false
  if (route.meta?.roles?.length && !route.meta.roles.includes(persona.role)) return false
  if (route.meta?.permission && !persona.permissions.includes(route.meta.permission)) return false
  return true
}

describe('permission smoke checks', () => {
  it('auditor persona can access report-view program routes but not manager-only pages', () => {
    const auditor = {
      role: 'AUDITOR',
      permissions: ['REPORT_VIEW', 'AUDIT_EXECUTION', 'COMPLIANCE_MANAGEMENT', 'REMEDIATION_MANAGEMENT']
    }
    expect(canAccessRoute('/admin/audit-projects', auditor)).toBe(true)
    expect(canAccessRoute('/admin/operations', auditor)).toBe(true)
    expect(canAccessRoute('/admin/policy-attestations', auditor)).toBe(true)
    expect(canAccessRoute('/admin/policies', auditor)).toBe(false)
    expect(canAccessRoute('/admin/findings', auditor)).toBe(false)
    expect(canAccessRoute('/admin/risk-register', auditor)).toBe(false)
  })

  it('application owner cannot access admin routes', () => {
    const owner = {
      role: 'APPLICATION_OWNER',
      permissions: ['APPLICATION_MANAGEMENT', 'AUDIT_EXECUTION', 'REPORT_VIEW']
    }
    expect(canAccessRoute('/admin/operations', owner)).toBe(false)
    expect(canAccessRoute('/admin/audit-projects', owner)).toBe(false)
  })

  it('admin navigation entries map to real routes with matching permission metadata', () => {
    const routeMap = new Map(router.getRoutes().map((route) => [route.path, route]))
    adminSections.forEach((section) => {
      section.items.forEach((item) => {
        const route = routeMap.get(item.to)
        expect(route, `missing route for ${item.to}`).toBeTruthy()
        if (item.permission) {
          expect(route.meta.permission, `permission mismatch for ${item.to}`).toBe(item.permission)
        }
      })
    })
  })
})
