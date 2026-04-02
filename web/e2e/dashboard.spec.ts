import { test, expect } from '@playwright/test'

test.use({ viewport: { width: 1280, height: 800 } })

test('dashboard renders trace table with rows, navigates to trace view', async ({ page }) => {
  await page.goto('/')

  // Select first agent pill (loads from API)
  const pill = page.locator('[data-testid="agent-pill"]').first()
  await expect(pill).toBeVisible({ timeout: 15_000 })
  await pill.click()

  // Trace table renders with data rows
  await expect(page.locator('table').first()).toBeVisible({ timeout: 15_000 })
  const firstRow = page.locator('tbody tr').first()
  await expect(firstRow).toBeVisible({ timeout: 15_000 })

  // Click first trace row — selects it in the store
  await firstRow.click()

  // Navigate to Traces page
  await page.getByRole('button', { name: 'Traces' }).click()

  // TraceView loads (either with selected trace detail or trace selector list)
  await expect(page.getByText(/Traces|Memory Lineage|Select a trace/).first()).toBeVisible({ timeout: 10_000 })
})
