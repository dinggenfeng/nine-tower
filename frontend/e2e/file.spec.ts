import { test, expect, registerUser, createProject, goToProject } from './fixtures';

test('file and directory CRUD', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName);

  // Create role
  await page.getByRole('button', { name: '新建 Role' }).click();
  await page.getByLabel('名称').fill('file-role');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await page.getByText('file-role').click();
  await page.waitForURL(/.*\/roles\/\d+/);

  // Switch to Files tab
  await page.getByRole('tab', { name: 'Files' }).click();

  // Create directory
  await page.getByRole('button', { name: '新建目录' }).click();
  await page.getByLabel('名称').fill('scripts');
  await page.getByRole('button', { name: /确\s*定/ }).click();

  // Create file
  await page.getByRole('button', { name: '新建文件' }).click();
  await page.getByLabel('名称').fill('deploy.sh');
  await page.getByLabel('文件内容').fill('#!/bin/bash\necho deploy');
  await page.getByRole('button', { name: /确\s*定/ }).click();
  await expect(page.getByText('deploy.sh')).toBeVisible();
});
