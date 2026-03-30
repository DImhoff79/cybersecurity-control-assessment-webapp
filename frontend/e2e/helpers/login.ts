import { expect, type Page } from '@playwright/test'

/** Basic email/password sign-in against the default login shell. */
export async function loginWithBasicAuth(page: Page, email: string, password: string) {
  await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: /sign in/i }).click()
  await expect(page).not.toHaveURL(/\/login/, { timeout: 20_000 })
}
