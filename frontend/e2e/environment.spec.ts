import { test, expect, registerUser, createProject, goToProject } from './fixtures';

test('environment and config CRUD', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName, 'environments');

  // Create environment
  await page.getByRole('button', { name: '新建环境' }).click();
  await page.getByLabel('名称').fill('staging');
  await page.getByLabel('描述').fill('Staging environment');
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('staging')).toBeVisible();

  // Click into environment to add config
  await page.getByText('staging').click();
  await page.getByRole('button', { name: '添加配置项' }).click();
  await page.getByLabel('Key').fill('DB_HOST');
  await page.getByLabel('Value').fill('staging.db.local');
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('DB_HOST')).toBeVisible();
  await expect(page.getByText('staging.db.local')).toBeVisible();
});
