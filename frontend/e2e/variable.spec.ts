import { test, expect, registerUser, createProject, goToProject, createVariableApi } from './fixtures';

test('variable CRUD', async ({ page }) => {
  await registerUser(page);
  const projectName = await createProject(page);
  await goToProject(page, projectName, 'variables');
  const projectId = Number(page.url().match(/\/projects\/(\d+)/)![1]);

  // Create project-level variable via API
  const varName = `var_${Date.now()}`;
  await createVariableApi(page, projectId, varName, 'myapp');
  await page.reload();
  await expect(page.getByText(varName).first()).toBeVisible();
  await expect(page.getByText('myapp').first()).toBeVisible();
});
