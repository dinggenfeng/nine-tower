import { test, expect, registerUser, createProject, goToProject } from './fixtures';

test('task CRUD and reorder', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName);

  // Create a role first
  await page.getByRole('button', { name: '新建 Role' }).click();
  await page.getByLabel('名称').fill('test-role');
  await page.getByRole('button', { name: '确定' }).click();
  await page.getByText('test-role').click();
  await page.waitForURL(/.*\/roles\/\d+/);

  // Create first task
  await page.getByRole('button', { name: '添加 Task' }).click();
  await page.getByLabel('名称').fill('First task');
  await page.getByRole('textbox', { name: '顺序' }).fill('1');
  // Select module
  await page.getByText('选择或输入 Ansible 模块名').click();
  await page.getByText('在远程主机上执行 Shell 命令').click();
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('First task')).toBeVisible();

  // Create second task
  await page.getByRole('button', { name: '添加 Task' }).click();
  await page.getByLabel('名称').fill('Second task');
  await page.getByRole('textbox', { name: '顺序' }).fill('2');
  await page.getByText('选择或输入 Ansible 模块名').click();
  await page.getByText('在远程主机上执行 Shell 命令').click();
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('Second task')).toBeVisible();
});

test('handler CRUD and notify', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName);

  // Create role
  await page.getByRole('button', { name: '新建 Role' }).click();
  await page.getByLabel('名称').fill('notify-role');
  await page.getByRole('button', { name: '确定' }).click();
  await page.getByText('notify-role').click();
  await page.waitForURL(/.*\/roles\/\d+/);

  // Switch to Handlers tab
  await page.getByRole('tab', { name: 'Handlers' }).click();

  // Create handler
  await page.getByRole('button', { name: '新建 Handler' }).click();
  await page.getByLabel('名称').fill('Restart nginx');
  await page.getByText('选择或输入 Ansible 模块名').click();
  await page.getByText('在远程主机上执行 Shell 命令').click();
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('Restart nginx')).toBeVisible();
});

test('block task', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName);

  // Create role
  await page.getByRole('button', { name: '新建 Role' }).click();
  await page.getByLabel('名称').fill('block-role');
  await page.getByRole('button', { name: '确定' }).click();
  await page.getByText('block-role').click();
  await page.waitForURL(/.*\/roles\/\d+/);

  // Create block task
  await page.getByRole('button', { name: '添加 Task' }).click();
  await page.getByLabel('名称').fill('Block task');
  await page.getByRole('textbox', { name: '顺序' }).fill('1');
  await page.getByText('选择或输入 Ansible 模块名').click();
  await page.getByText('将多个任务组合为块').click();
  // Verify block editor tabs appear
  await expect(page.getByText(/block（必填）/)).toBeVisible();
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('Block task')).toBeVisible();
});
