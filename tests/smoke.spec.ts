
import { test, expect } from '@playwright/test';

test('1. Entry page loads', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle(/CivicConnect/i);
  await expect(page.getByText(/Citizen/i).first()).toBeVisible();
  await expect(page.getByText(/Officer/i).first()).toBeVisible();
});

test('2. Citizen can navigate between main pages', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /Citizen/i }).click();

  await expect(page.getByRole('heading', {
    name: /Make Our Neighbourhood Better/i,
  })).toBeVisible();

  await page.getByRole('button', { name: /Report a Complaint/i }).click();
  await expect(page.getByRole('heading', {
    name: 'Report a Local Issue',
  })).toBeVisible();

  await page.getByRole('button', {
    name: 'Track Complaint', exact: true,
  }).click();
  await expect(page.getByRole('heading', {
    name: 'Track Your Complaint',
  })).toBeVisible();

  await page.getByRole('button', { name: /My Complaints/i }).click();
  await expect(page.getByRole('heading', {
    name: 'My Complaints',
  })).toBeVisible();
});

test('3. Empty complaint form shows all required-field errors', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /Citizen/i }).click();
  await page.getByRole('button', { name: /Report a Complaint/i }).click();
  await page.getByRole('button', { name: /Submit Complaint/i }).click();

  await expect(page.getByText('This field is required.'))
    .toHaveCount(4);
});

test('4. Officer workspace and filters load', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /Officer/i }).click();

  await expect(page.getByRole('heading', {
    name: 'Complaint operations',
  })).toBeVisible();

  await expect(page.getByRole('heading', {
    name: 'Complaint management',
  })).toBeVisible();

  await expect(page.getByRole('combobox', {
    name: 'Filter by status',
  })).toBeVisible();

  await expect(page.getByRole('combobox', {
    name: 'Filter by category',
  })).toBeVisible();

  await expect(page.getByRole('combobox', {
    name: 'Filter by locality',
  })).toBeVisible();
});

test('5. Unsupported image type is rejected', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /Citizen/i }).click();
  await page.getByRole('button', { name: /Report a Complaint/i }).click();

  await page.locator('input[type="file"]').setInputFiles({
    name: 'invalid.txt',
    mimeType: 'text/plain',
    buffer: Buffer.from('not an image'),
  });

  await expect(page.getByText(
    'Choose a JPEG, PNG, or WebP image.',
  )).toBeVisible();
});

test('6. Image larger than 5 MB is rejected', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /Citizen/i }).click();
  await page.getByRole('button', { name: /Report a Complaint/i }).click();

  await page.locator('input[type="file"]').setInputFiles({
    name: 'large.png',
    mimeType: 'image/png',
    buffer: Buffer.alloc(5 * 1024 * 1024 + 1),
  });

  await expect(page.getByText(
    'Each image must be 5 MB or smaller.',
  )).toBeVisible();
});

test('7. Homepage category opens a preselected complaint form', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /Citizen/i }).click();

  await page.getByRole('button', {
    name: /Roads & Potholes/i,
  }).click();

  await expect(page.getByRole('heading', {
    name: 'Report a Local Issue',
  })).toBeVisible();

  await expect(page.locator('select[name="category"]'))
    .toHaveValue('Roads & Potholes');
});

test('8. Citizen can switch workspaces', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /Citizen/i }).click();

  await page.getByRole('button', {
    name: 'Switch workspace',
  }).click();

  await expect(page.getByRole('heading', {
    name: 'Choose your workspace',
  })).toBeVisible();
});



test('9. Tracking a nonexistent complaint shows a helpful message', async ({ page }) => {
  await page.goto('/');

  await page.getByRole('button', { name: /Citizen/i }).click();

  await page.getByRole('button', {
    name: 'Track a Complaint',
    exact: true,
  }).click();

  await page.getByPlaceholder('e.g. CC-2026-0001').fill(
    'CC-TEST-NONEXISTENT-987654'
  );

  await page.getByRole('main').getByRole('button', {
    name: 'Track Complaint',
    exact: true,
  }).click();

  await expect(page.getByRole('heading', {
    name: "We couldn't find that reference",
  })).toBeVisible();
});

test('10. Officer can filter complaints by status', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /Officer/i }).click();

  const statusFilter = page.getByRole('combobox', {
    name: 'Filter by status',
  });

  await expect(statusFilter).toBeVisible();
  await statusFilter.selectOption('Resolved');
  await expect(statusFilter).toHaveValue('Resolved');

  await statusFilter.selectOption('All');
  await expect(statusFilter).toHaveValue('All');
});

test('11. Officer can search and clear complaint filters', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /Officer/i }).click();

  const search = page.getByPlaceholder('Search reference or title');

  await search.fill('NO-MATCH-TEST-987654');
  await expect(page.getByRole('heading', {
    name: 'No matching records',
  })).toBeVisible();

  await page.getByRole('button', { name: 'Clear filters' }).click();

  await expect(search).toHaveValue('');
  await expect(page.getByRole('combobox', {
    name: 'Filter by status',
  })).toHaveValue('All');
});
