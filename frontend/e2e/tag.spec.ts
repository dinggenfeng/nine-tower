import { test, expect, registerUser, createProject, goToProject, createRoleApi } from './fixtures';

test('tag CRUD', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName, 'tags');

  // Create tag
  await page.getByRole('button', { name: '新建标签' }).click();
  await page.getByLabel('名称').fill('deploy');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('deploy')).toBeVisible();

  // Edit tag
  await page.getByRole('button', { name: /编辑/ }).first().click();
  await page.getByLabel('名称').clear();
  await page.getByLabel('名称').fill('deploy-prod');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('deploy-prod')).toBeVisible();

  // Delete tag
  await page.getByRole('button', { name: /删除/ }).first().click();
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('deploy-prod')).not.toBeVisible();
});

test('tag associated with task', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);

  // Navigate to project first to get projectId
  await goToProject(page, projectName, 'tags');
  const projectId = Number(page.url().match(/\/projects\/(\d+)/)![1]);

  // Create tag
  await page.getByRole('button', { name: '新建标签' }).click();
  await page.getByLabel('名称').fill('critical');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('critical')).toBeVisible();

  // Create role via API and navigate to it
  const role = await createRoleApi(page, projectId, 'tagged-role');
  await page.goto(`/projects/${projectId}/roles/${role.id}`);
  await page.waitForURL(/.*\/roles\/\d+/);

  // Create task with shell module
  await page.getByRole('button', { name: '添加 Task' }).click();
  await page.getByLabel('名称').fill('Tagged task');
  await page.getByLabel('顺序').fill('1');
  await page.getByRole('combobox').first().click();
  await page.getByText('在远程主机上执行 Shell 命令').click();
  await page.getByPlaceholder('ls -la /tmp').fill('echo tagged');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('Tagged task')).toBeVisible();
});
