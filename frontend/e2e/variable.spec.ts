import { test, expect, registerUser, createProject, goToProject } from './fixtures';

test('variable CRUD', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName, 'variables');

  // Create project-level variable
  await page.getByRole('button', { name: '新建变量' }).click();
  await page.getByLabel('Key').fill('app_name');
  await page.getByLabel('Value').fill('myapp');
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('app_name')).toBeVisible();
  await expect(page.getByText('myapp')).toBeVisible();
});
