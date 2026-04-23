import { test, expect, registerUser, createProject, goToProject } from './fixtures';

test('environment and config CRUD', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName, 'environments');

  // Create environment
  await page.getByRole('button', { name: '新建环境' }).click();
  await page.getByRole('dialog').getByLabel('名称').fill('staging');
  await page.getByRole('dialog').getByLabel('描述').fill('Staging env');
  await page.getByRole('dialog').getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('staging').first()).toBeVisible();

  // Add config to environment
  await page.getByRole('button', { name: '添加配置' }).first().click();
  await page.getByRole('dialog').getByLabel('Key').fill('DB_HOST');
  await page.getByRole('dialog').getByLabel('Value').fill('staging.db.local');
  await page.getByRole('dialog').getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.locator('td', { hasText: 'DB_HOST' }).first()).toBeVisible();
  await expect(page.locator('td', { hasText: 'staging.db.local' }).first()).toBeVisible();
});
