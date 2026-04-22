import { test, expect, registerUser, createProject } from './fixtures';

test('project CRUD', async ({ page }) => {
  await registerUser(page);

  // Create
  const projectName = `E2E Project ${Date.now()}`;
  await createProject(page, projectName);

  // Navigate to settings and edit
  await page.getByText(projectName).click();
  await page.waitForURL(/.*\/projects\/\d+/);
  await page.getByText('设置').click();
  await page.getByLabel('项目名称').clear();
  await page.getByLabel('项目名称').fill(`${projectName} Updated`);
  await page.getByRole('button', { name: '保存' }).click();

  // Go back to projects list and verify updated name
  await page.goto('/projects');
  await expect(page.getByText(`${projectName} Updated`)).toBeVisible();

  // Delete
  await page.getByText(`${projectName} Updated`).click();
  await page.waitForURL(/.*\/projects\/\d+/);
  await page.getByText('设置').click();
  await page.getByRole('button', { name: '删除项目' }).click();
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page).toHaveURL(/.*\/projects$/);
  await expect(page.getByText(`${projectName} Updated`)).not.toBeVisible();
});

test('member management', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);

  // Go to members page
  await page.getByText(projectName).click();
  await page.waitForURL(/.*\/projects\/\d+/);
  await page.getByText('成员').click();

  // Verify the creator is listed as admin
  await expect(page.getByText('PROJECT_ADMIN')).toBeVisible();
});
