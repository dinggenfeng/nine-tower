import { test, expect, registerUser, createProject, goToProject } from './fixtures';

test('host group and host CRUD', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName, 'host-groups');

  // Create host group
  await page.getByRole('button', { name: '新建主机组' }).click();
  await page.locator('.ant-modal:visible #name').fill('web_servers');
  await page.locator('.ant-modal:visible .ant-btn-primary').click();
  await expect(page.getByText('web_servers')).toBeVisible();

  // Click into host group
  await page.getByText('web_servers').click();

  // Add host with SSH password
  await page.getByRole('button', { name: '添加主机' }).click();
  await page.locator('.ant-modal:visible #name').fill('web1');
  await page.locator('.ant-modal:visible #ip').fill('192.168.1.10');
  await page.locator('.ant-modal:visible #ansibleSshPass').fill('secret123');
  await page.locator('.ant-modal:visible .ant-btn-primary').click();

  // Verify host appears in table
  await expect(page.getByText('web1')).toBeVisible();
  await expect(page.getByText('192.168.1.10')).toBeVisible();
});
