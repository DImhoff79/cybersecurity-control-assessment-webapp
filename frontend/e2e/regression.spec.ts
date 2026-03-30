import { test, expect } from '@playwright/test'
import { loginWithBasicAuth } from './helpers/login'

/**
 * Browser regression: authenticated flows against local Vite + proxied API.
 * Requires backend on 8080 with default seeded users and demo dataset (DataLoader + DemoDatasetSeeder).
 */
test.describe('authenticated regression', () => {
  test('admin can sign in and open admin applications workspace', async ({ page }) => {
    await loginWithBasicAuth(page, 'admin@example.com', 'admin123')
    await page.goto('/admin/applications')
    await expect(page.getByRole('heading', { name: /applications/i })).toBeVisible()
  })

  test('owner can sign in and open my audits', async ({ page }) => {
    await loginWithBasicAuth(page, 'owner@example.com', 'owner123')
    await page.goto('/my-audits')
    await expect(page.getByRole('heading', { name: /my audits/i })).toBeVisible()
  })

  test('audit manager can open questionnaire library builder (controls + questions)', async ({ page }) => {
    await loginWithBasicAuth(page, 'audit.manager@example.com', 'manager123')
    await page.goto('/admin/questionnaire-builder')
    await expect(page.getByRole('heading', { name: /questionnaire.*library builder/i })).toBeVisible()
    const mainTabs = page.locator('ul.nav.nav-tabs').first()
    await expect(mainTabs.getByRole('button', { name: /^controls$/i })).toBeVisible()
    await expect(mainTabs.getByRole('button', { name: /^questions$/i })).toBeVisible()
    await expect(page.getByText(/kroger ccf/i).first()).toBeVisible({ timeout: 30_000 })
  })

  test('audit manager can load branching workflow demo map and save bar', async ({ page }) => {
    test.setTimeout(120_000)
    await loginWithBasicAuth(page, 'audit.manager@example.com', 'manager123')
    await page.goto('/admin/branching-workflow-demo')
    await expect(page.getByRole('heading', { name: /branching workflow/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /save workflow/i })).toBeVisible({ timeout: 90_000 })
    await expect(page.getByText(/flow map/i).first()).toBeVisible()
  })

  test('owner can open audit respond from my audits', async ({ page }) => {
    test.setTimeout(120_000)
    await loginWithBasicAuth(page, 'owner@example.com', 'owner123')
    await page.goto('/my-audits')
    await expect(page.getByRole('heading', { name: /my audits/i })).toBeVisible()
    const auditLink = page.locator('a.my-audits-app-link').first()
    await expect(auditLink).toBeVisible({ timeout: 30_000 })
    await auditLink.click()
    await expect(page).toHaveURL(/\/audits\/\d+\/respond/)
    await expect(page.locator('h1.respond-page-title')).toBeVisible({ timeout: 45_000 })
    await expect(page.locator('h1.respond-page-title')).toContainText(/assessment/i)
    await expect(page.getByText(/progress/i).first()).toBeVisible()
  })
})
