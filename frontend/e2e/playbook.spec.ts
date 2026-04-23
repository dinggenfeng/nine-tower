import { test, expect, registerUser, createProject, goToProject } from './fixtures';

test('playbook CRUD', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName, 'playbooks');

  // Create playbook
  await page.getByRole('button', { name: '新建 Playbook' }).click();
  await page.getByLabel('名称').fill('deploy.yml');
  await page.getByLabel('描述').fill('Main deployment playbook');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('deploy.yml')).toBeVisible();

  // Enter playbook builder and verify cards
  await page.getByText('deploy.yml').click();
  await page.waitForURL(/.*\/playbooks\/\d+/);
  await expect(page.getByText('Roles（按顺序）')).toBeVisible();
  await expect(page.getByText('主机组').first()).toBeVisible();
  await expect(page.getByText('标签').first()).toBeVisible();
  await expect(page.getByText('环境').first()).toBeVisible();
  await expect(page.getByText('YAML 预览')).toBeVisible();

  // Go back and delete
  await page.goBack();
  await page.getByRole('button', { name: /删除/ }).first().click();
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('deploy.yml')).not.toBeVisible();
});
