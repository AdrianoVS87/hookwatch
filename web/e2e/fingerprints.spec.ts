import { test, expect } from '@playwright/test'

test.use({ viewport: { width: 1280, height: 800 } })

test('fingerprints page shows table', async ({ page }) => {
  await page.goto('/')

  // Select first agent so stores are primed
  const pill = page.locator('[data-testid="agent-pill"]').first()
  await expect(pill).toBeVisible({ timeout: 15_000 })
  await pill.click()

  // Navigate to Fingerprints
  await page.getByRole('button', { name: 'Fingerprints' }).click()
  await expect(page.getByText('Failure Fingerprints')).toBeVisible()

  // Table is always rendered (either data rows or empty state)
  await expect(page.locator('table').first()).toBeVisible()

  // Wait for API calls
  await page.waitForTimeout(3_000)

  const rowCount = await page.locator('tbody tr').count()
  if (rowCount > 0) {
    await expect(page.locator('[data-testid="sparkline"]').first()).toBeVisible()
  } else {
    await expect(page.getByText('No recurring failures yet.')).toBeVisible()
  }
})
