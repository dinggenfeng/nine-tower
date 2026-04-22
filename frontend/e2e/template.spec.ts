import { test, expect, registerUser, createProject, goToProject } from './fixtures';

test('template CRUD', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName);

  // Create role
  await page.getByRole('button', { name: '新建 Role' }).click();
  await page.getByLabel('名称').fill('tpl-role');
  await page.getByRole('button', { name: '确定' }).click();
  await page.getByText('tpl-role').click();
  await page.waitForURL(/.*\/roles\/\d+/);

  // Switch to Templates tab
  await page.getByRole('tab', { name: 'Templates' }).click();

  // Create directory
  await page.getByRole('button', { name: '新建目录' }).click();
  await page.getByLabel('目录名').fill('conf.d');
  await page.getByRole('button', { name: '确定' }).click();

  // Create template
  await page.getByRole('button', { name: '新建模板' }).click();
  await page.getByLabel('文件名').fill('nginx.conf.j2');
  await page.getByLabel('目标路径').fill('/etc/nginx/nginx.conf');
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('nginx.conf.j2')).toBeVisible();
});
