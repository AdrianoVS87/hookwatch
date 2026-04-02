import { test, expect } from '@playwright/test'

test.use({ viewport: { width: 1280, height: 800 } })

test('trace detail shows compliance badge in header', async ({ page }) => {
  await page.goto('/')

  // Select first agent
  const pill = page.locator('[data-testid="agent-pill"]').first()
  await expect(pill).toBeVisible({ timeout: 15_000 })
  await pill.click()

  // Wait for trace table
  await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 15_000 })

  // Click first trace
  await page.locator('tbody tr').first().click()

  // Go to Traces detail
  await page.getByRole('button', { name: 'Traces' }).click()

  // Compliance stat label in TraceView header
  await expect(page.getByText('Compliance')).toBeVisible({ timeout: 15_000 })
})
