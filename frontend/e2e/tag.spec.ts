import { test, expect, registerUser, createProject, goToProject } from './fixtures';

test('tag CRUD', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName, 'tags');

  // Create tag
  await page.getByRole('button', { name: '新建标签' }).click();
  await page.getByLabel('名称').fill('deploy');
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('deploy')).toBeVisible();

  // Edit tag
  await page.getByRole('button', { name: /编辑/ }).first().click();
  await page.getByLabel('名称').clear();
  await page.getByLabel('名称').fill('deploy-prod');
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('deploy-prod')).toBeVisible();

  // Delete tag
  await page.getByRole('button', { name: /删除/ }).first().click();
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('deploy-prod')).not.toBeVisible();
});

test('tag associated with task', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);

  // Create tag first
  await goToProject(page, projectName, 'tags');
  await page.getByRole('button', { name: '新建标签' }).click();
  await page.getByLabel('名称').fill('critical');
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('critical')).toBeVisible();

  // Create role and task with the tag
  await goToProject(page, projectName);
  await page.getByRole('button', { name: '新建 Role' }).click();
  await page.getByLabel('名称').fill('tagged-role');
  await page.getByRole('button', { name: '确定' }).click();
  await page.getByText('tagged-role').click();
  await page.waitForURL(/.*\/roles\/\d+/);

  // Create task
  await page.getByRole('button', { name: '添加 Task' }).click();
  await page.getByLabel('名称').fill('Tagged task');
  await page.getByRole('textbox', { name: '顺序' }).fill('1');
  await page.getByText('选择或输入 Ansible 模块名').click();
  await page.getByText('在远程主机上执行 Shell 命令').click();
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('Tagged task')).toBeVisible();
});
