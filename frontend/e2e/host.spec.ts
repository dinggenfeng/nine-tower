import { test, expect, registerUser, createProject, goToProject } from './fixtures';

test('host group and host CRUD', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName, 'host-groups');

  // Create host group
  await page.getByRole('button', { name: '新建主机组' }).click();
  await page.getByLabel('名称').fill('web_servers');
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('web_servers')).toBeVisible();

  // Click into host group
  await page.getByText('web_servers').click();

  // Add host with SSH password
  await page.getByRole('button', { name: '新建主机' }).click();
  await page.getByLabel('名称').fill('web1');
  await page.getByLabel('IP 地址').fill('192.168.1.10');
  await page.getByLabel('SSH 密码').fill('secret123');
  await page.getByRole('button', { name: '确定' }).click();

  // Verify host appears and password is masked
  await expect(page.getByText('web1')).toBeVisible();
  await expect(page.getByText('****')).toBeVisible();
});
