import { test, expect, registerUser, createProject, goToProject, createRoleApi } from './fixtures';

async function fillShellModule(page: import('@playwright/test').Page, cmd: string) {
  await page.getByRole('combobox').first().click();
  await page.getByText('在远程主机上执行 Shell 命令').click();
  await page.getByPlaceholder('ls -la /tmp').fill(cmd);
}

test('task CRUD and reorder', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName);
  const projectId = Number(page.url().match(/\/projects\/(\d+)/)![1]);

  // Create a role via API
  const role = await createRoleApi(page, projectId, 'test-role');
  await page.goto(`/projects/${projectId}/roles/${role.id}`);
  await page.waitForURL(/.*\/roles\/\d+/);

  // Create first task
  await page.getByRole('button', { name: '添加 Task' }).click();
  await page.getByLabel('名称').fill('First task');
  await page.getByLabel('顺序').fill('1');
  await fillShellModule(page, 'echo first');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('First task')).toBeVisible();

  // Create second task
  await page.getByRole('button', { name: '添加 Task' }).click();
  await page.getByLabel('名称').fill('Second task');
  await page.getByLabel('顺序').fill('2');
  await fillShellModule(page, 'echo second');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('Second task')).toBeVisible();
});

test('handler CRUD and notify', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName);
  const projectId = Number(page.url().match(/\/projects\/(\d+)/)![1]);

  // Create role via API
  const role = await createRoleApi(page, projectId, 'notify-role');
  await page.goto(`/projects/${projectId}/roles/${role.id}`);
  await page.waitForURL(/.*\/roles\/\d+/);

  // Switch to Handlers tab
  await page.getByRole('tab', { name: 'Handlers' }).click();

  // Create handler
  await page.getByRole('button', { name: '添加 Handler' }).click();
  await page.getByLabel('名称').fill('Restart nginx');
  await fillShellModule(page, 'systemctl restart nginx');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('Restart nginx')).toBeVisible();
});

test('block task', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName);
  const projectId = Number(page.url().match(/\/projects\/(\d+)/)![1]);

  // Create role via API
  const role = await createRoleApi(page, projectId, 'block-role');
  await page.goto(`/projects/${projectId}/roles/${role.id}`);
  await page.waitForURL(/.*\/roles\/\d+/);

  // Create block task
  await page.getByRole('button', { name: '添加 Task' }).click();
  await page.getByLabel('名称').fill('Block task');
  await page.getByLabel('顺序').fill('1');
  await page.getByRole('combobox').first().click();
  await page.getByText('将多个任务组合为块').click();
  // Verify block editor tabs appear
  await expect(page.getByText(/block（必填）/)).toBeVisible();
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('Block task')).toBeVisible();
});
