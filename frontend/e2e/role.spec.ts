import { test, expect, registerUser, createProject, goToProject } from './fixtures';

test('role CRUD', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  // Default sub-page is roles
  await goToProject(page, projectName);

  // Create role
  await page.getByRole('button', { name: '新建 Role' }).click();
  await page.getByLabel('名称').fill('nginx');
  await page.getByLabel('描述').fill('Nginx web server role');
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('nginx')).toBeVisible();

  // Click into role detail and verify tabs
  await page.getByText('nginx').click();
  await page.waitForURL(/.*\/roles\/\d+/);
  await expect(page.getByRole('tab', { name: 'Tasks' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Handlers' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Templates' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Files' })).toBeVisible();
});
