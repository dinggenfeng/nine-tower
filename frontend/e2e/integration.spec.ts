import { test, expect, registerUser, createProject, goToProject, createRoleApi, createVariableApi } from './fixtures';

async function clickSelectOption(page: import('@playwright/test').Page, placeholder: string, option: string) {
  const select = page.locator('.ant-select').filter({ hasText: placeholder }).first();
  await select.locator('input[role="combobox"]').click();
  await page.locator('.ant-select-item-option-content', { hasText: new RegExp(`^${option}$`) }).click();
}

test('full playbook assembly', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);

  // Navigate to project to get projectId
  await goToProject(page, projectName);
  const projectId = Number(page.url().match(/\/projects\/(\d+)/)![1]);

  // 1. Create host group
  await goToProject(page, projectName, 'host-groups');
  await page.getByRole('button', { name: '新建主机组' }).click();
  await page.getByLabel('名称').fill('web_servers');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('web_servers')).toBeVisible();

  // 2. Create role with a task via API
  const role = await createRoleApi(page, projectId, 'nginx');
  await page.goto(`/projects/${projectId}/roles/${role.id}`);
  await page.waitForURL(/.*\/roles\/\d+/);
  await page.getByRole('button', { name: '添加 Task' }).click();
  await page.getByLabel('名称').fill('Install nginx');
  await page.getByLabel('顺序').fill('1');
  await page.getByRole('combobox').first().click();
  await page.getByText('在远程主机上执行 Shell 命令').click();
  await page.getByPlaceholder('ls -la /tmp').fill('apt install nginx');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('Install nginx')).toBeVisible();

  // 3. Create tag
  await goToProject(page, projectName, 'tags');
  await page.getByRole('button', { name: '新建标签' }).click();
  await page.getByLabel('名称').fill('deploy');
  await page.getByRole('button', { name: /确\s*定/ }).click();

  // 4. Create environment
  await goToProject(page, projectName, 'environments');
  await page.getByRole('button', { name: '新建环境' }).click();
  await page.getByLabel('名称').fill('production');
  await page.getByRole('button', { name: /确\s*定/ }).click();

  // 5. Create variable via API
  await createVariableApi(page, projectId, 'app_name', 'myapp');

  // 6. Create playbook and assemble everything
  await goToProject(page, projectName, 'playbooks');
  await page.getByRole('button', { name: '新建 Playbook' }).click();
  await page.getByLabel('名称').fill('full-deploy.yml');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await page.getByText('full-deploy.yml').click();
  await page.waitForURL(/.*\/playbooks\/\d+/);

  // Wait for PlaybookBuilder data to load
  await expect(page.getByText('添加主机组')).toBeVisible();

  // Add host group
  await clickSelectOption(page, '添加主机组', 'web_servers');

  // Add role
  await clickSelectOption(page, '添加 Role', 'nginx');

  // Add tag
  await clickSelectOption(page, '添加标签', 'deploy');

  // Add environment
  await clickSelectOption(page, '添加环境', 'production');

  // Verify YAML preview contains key elements
  const yamlPreview = page.locator('.language-yaml, pre, code').last();
  await expect(yamlPreview).toContainText(/web_servers|nginx|deploy/);
});

test('YAML generation with extraVars', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);

  // Navigate to project to get projectId
  await goToProject(page, projectName);
  const projectId = Number(page.url().match(/\/projects\/(\d+)/)![1]);

  // Create role via API
  await createRoleApi(page, projectId, 'app-role');

  // Create playbook with extraVars
  await goToProject(page, projectName, 'playbooks');
  await page.getByRole('button', { name: '新建 Playbook' }).click();
  await page.getByLabel('名称').fill('with-vars.yml');
  await page.getByLabel('描述').fill('Test extraVars');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await page.getByText('with-vars.yml').click();
  await page.waitForURL(/.*\/playbooks\/\d+/);

  // Wait for PlaybookBuilder data to load
  await expect(page.getByText('添加 Role')).toBeVisible();

  // Fill extraVars
  const textarea = page.getByPlaceholder(/key: value/);
  await textarea.fill('port: 8080\nhost: localhost');
  await page.getByRole('button', { name: '保存' }).click();

  // Add role
  await clickSelectOption(page, '添加 Role', 'app-role');

  // Verify YAML
  await expect(page.locator('pre, code').last()).toContainText('8080');
});

test('owner can fully manage playbook composition', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);

  // Navigate to project to get projectId
  await goToProject(page, projectName);
  const projectId = Number(page.url().match(/\/projects\/(\d+)/)![1]);

  // Create role via API
  await createRoleApi(page, projectId, 'owner-role');

  // Create playbook and add role (owner should succeed)
  await goToProject(page, projectName, 'playbooks');
  await page.getByRole('button', { name: '新建 Playbook' }).click();
  await page.getByLabel('名称').fill('owner-pb.yml');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await page.getByText('owner-pb.yml').click();
  await page.waitForURL(/.*\/playbooks\/\d+/);

  // Wait for PlaybookBuilder data to load
  await expect(page.getByText('添加 Role')).toBeVisible();

  // Add role (should work as owner)
  await clickSelectOption(page, '添加 Role', 'owner-role');
  await expect(page.getByText('owner-role').first()).toBeVisible();
});
