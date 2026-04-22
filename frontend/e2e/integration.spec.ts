import { test, expect, registerUser, createProject, goToProject } from './fixtures';

test('full playbook assembly', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);

  // 1. Create host group
  await goToProject(page, projectName, 'host-groups');
  await page.getByRole('button', { name: '新建主机组' }).click();
  await page.getByLabel('名称').fill('web_servers');
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('web_servers')).toBeVisible();

  // 2. Create role with a task
  await goToProject(page, projectName);
  await page.getByRole('button', { name: '新建 Role' }).click();
  await page.getByLabel('名称').fill('nginx');
  await page.getByRole('button', { name: '确定' }).click();
  await page.getByText('nginx').click();
  await page.waitForURL(/.*\/roles\/\d+/);
  await page.getByRole('button', { name: '添加 Task' }).click();
  await page.getByLabel('名称').fill('Install nginx');
  await page.getByRole('textbox', { name: '顺序' }).fill('1');
  await page.getByText('选择或输入 Ansible 模块名').click();
  await page.getByText('在远程主机上执行 Shell 命令').click();
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('Install nginx')).toBeVisible();

  // 3. Create tag
  await goToProject(page, projectName, 'tags');
  await page.getByRole('button', { name: '新建标签' }).click();
  await page.getByLabel('名称').fill('deploy');
  await page.getByRole('button', { name: '确定' }).click();

  // 4. Create environment
  await goToProject(page, projectName, 'environments');
  await page.getByRole('button', { name: '新建环境' }).click();
  await page.getByLabel('名称').fill('production');
  await page.getByRole('button', { name: '确定' }).click();

  // 5. Create variable
  await goToProject(page, projectName, 'variables');
  await page.getByRole('button', { name: '新建变量' }).click();
  await page.getByLabel('Key').fill('app_name');
  await page.getByLabel('Value').fill('myapp');
  await page.getByRole('button', { name: '确定' }).click();

  // 6. Create playbook and assemble everything
  await goToProject(page, projectName, 'playbooks');
  await page.getByRole('button', { name: '新建剧本' }).click();
  await page.getByLabel('名称').fill('full-deploy.yml');
  await page.getByRole('button', { name: '确定' }).click();
  await page.getByText('full-deploy.yml').click();
  await page.waitForURL(/.*\/playbooks\/\d+/);

  // Add host group
  await page.getByText(/选择.*主机组/).click();
  await page.getByText('web_servers').click();

  // Add role
  await page.getByText(/选择.*Role/).click();
  await page.getByText('nginx').click();

  // Add tag
  await page.getByText(/选择.*标签/).click();
  await page.getByText('deploy').click();

  // Add environment
  await page.getByText(/选择.*环境/).click();
  await page.getByText('production').click();

  // Verify YAML preview contains key elements
  const yamlPreview = page.locator('.language-yaml, pre, code').last();
  await expect(yamlPreview).toContainText(/web_servers|nginx|deploy/);
});

test('YAML generation with extraVars', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);

  // Create role
  await goToProject(page, projectName);
  await page.getByRole('button', { name: '新建 Role' }).click();
  await page.getByLabel('名称').fill('app-role');
  await page.getByRole('button', { name: '确定' }).click();

  // Create playbook with extraVars
  await goToProject(page, projectName, 'playbooks');
  await page.getByRole('button', { name: '新建剧本' }).click();
  await page.getByLabel('名称').fill('with-vars.yml');
  await page.getByLabel('描述').fill('Test extraVars');
  await page.getByRole('button', { name: '确定' }).click();
  await page.getByText('with-vars.yml').click();
  await page.waitForURL(/.*\/playbooks\/\d+/);

  // Fill extraVars
  const textarea = page.getByPlaceholder(/key: value/);
  await textarea.fill('port: 8080\nhost: localhost');
  await page.getByRole('button', { name: '保存' }).click();

  // Add role
  await page.getByText(/选择.*Role/).click();
  await page.getByText('app-role').click();

  // Verify YAML
  await expect(page.locator('pre, code').last()).toContainText('8080');
});

test('owner can fully manage playbook composition', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);

  // Create role
  await goToProject(page, projectName);
  await page.getByRole('button', { name: '新建 Role' }).click();
  await page.getByLabel('名称').fill('owner-role');
  await page.getByRole('button', { name: '确定' }).click();

  // Create playbook and add role (owner should succeed)
  await goToProject(page, projectName, 'playbooks');
  await page.getByRole('button', { name: '新建剧本' }).click();
  await page.getByLabel('名称').fill('owner-pb.yml');
  await page.getByRole('button', { name: '确定' }).click();
  await page.getByText('owner-pb.yml').click();
  await page.waitForURL(/.*\/playbooks\/\d+/);

  // Add role (should work as owner)
  await page.getByText(/选择.*Role/).click();
  await page.getByText('owner-role').click();
  await expect(page.getByText('owner-role')).toBeVisible();
});
