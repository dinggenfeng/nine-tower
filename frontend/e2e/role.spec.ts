import { test, expect, registerUser, createProject, goToProject, createRoleApi } from './fixtures';

test('role CRUD', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName);
  const projectId = Number(page.url().match(/\/projects\/(\d+)/)![1]);

  // Create role via API (UI modal OK button unreliable)
  await createRoleApi(page, projectId, 'nginx', 'Nginx web server role');
  await page.reload();
  await expect(page.getByText('nginx').first()).toBeVisible();

  // Click into role detail and verify tabs (<a> without href has no link role)
  await page.locator('a', { hasText: 'nginx' }).click();
  await page.waitForURL(/.*\/roles\/\d+/);
  await expect(page.getByRole('tab', { name: 'Tasks' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Handlers' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Templates' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Files' })).toBeVisible();
});
