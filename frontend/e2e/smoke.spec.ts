import { test, expect } from '@playwright/test'

test.describe('public shell', () => {
  test('login page renders the product title and sign-in prompt', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Cybersecurity Control Assessment')
    await expect(page.getByText('Sign in to continue')).toBeVisible()
  })

  test('login page exposes email and password fields when basic auth is enabled', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByLabel('Email')).toBeVisible()
    await expect(page.getByLabel('Password')).toBeVisible()
    await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible()
  })
})

test.describe('routing', () => {
  test('unauthenticated visit to a protected route redirects to login', async ({ page }) => {
    await page.goto('/access-denied')
    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Cybersecurity Control Assessment')
  })
})
