import { test as base, expect, type Page } from '@playwright/test';

// Counter for unique usernames to avoid collisions between test runs
let userCounter = Date.now();

function uniqueUsername(prefix = 'user') {
  return `${prefix}_${++userCounter}`;
}

// Get auth token from localStorage
async function getToken(page: Page): Promise<string> {
  return page.evaluate(() => localStorage.getItem('token') || '');
}

/**
 * Register a new user via UI. Ends on /projects page.
 */
export async function registerUser(page: Page, username?: string) {
  const name = username ?? uniqueUsername('e2e');
  await page.goto('/register');
  await page.getByPlaceholder('用户名').fill(name);
  await page.getByPlaceholder('密码', { exact: false }).first().fill('Test1234!');
  await page.getByPlaceholder('邮箱').fill(`${name}@e2e.test`);
  await page.getByRole('button', { name: /注\s*册/ }).click();
  await page.waitForURL('**/projects');
  return { username: name };
}

/**
 * Login via UI. Ends on /projects page.
 */
export async function loginUser(page: Page, username: string, password = 'Test1234!') {
  await page.goto('/login');
  await page.getByPlaceholder('用户名').fill(username);
  await page.getByPlaceholder('密码').fill(password);
  await page.getByRole('button', { name: /登\s*录/ }).click();
  await page.waitForURL('**/projects');
}

/**
 * Create a project via UI. Returns the project name.
 * Assumes user is logged in and on /projects page.
 */
export async function createProject(page: Page, name?: string) {
  const projectName = name ?? `project_${++userCounter}`;
  await page.getByRole('button', { name: '新建项目' }).click();
  await page.getByLabel('项目名称').fill(projectName);
  await page.getByRole('button', { name: '确定' }).click();
  await expect(page.getByText(projectName)).toBeVisible();
  return projectName;
}

/**
 * Create a project via API. Returns project ID.
 */
export async function createProjectApi(page: Page, name?: string) {
  const projectName = name ?? `project_${++userCounter}`;
  const token = await getToken(page);
  const result = await page.evaluate(async ({ token, projectName }) => {
    const r = await fetch('/api/projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ name: projectName }),
    });
    return r.json();
  }, { token, projectName });
  return { projectId: result.data.id, projectName };
}

/**
 * Create a role via API. Returns role data.
 */
export async function createRoleApi(page: Page, projectId: number, name: string, description?: string) {
  const token = await getToken(page);
  const result = await page.evaluate(async ({ token, projectId, name, description }) => {
    const r = await fetch(`/api/projects/${projectId}/roles`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ name, description }),
    });
    return r.json();
  }, { token, projectId, name, description: description || null });
  return result.data;
}

/**
 * Create a variable via API. Returns variable data.
 */
export async function createVariableApi(
  page: Page,
  projectId: number,
  key: string,
  value: string,
  scope: string = 'PROJECT',
  scopeId?: number,
) {
  const token = await getToken(page);
  const result = await page.evaluate(
    async ({ token, projectId, key, value, scope, scopeId }) => {
      const r = await fetch(`/api/projects/${projectId}/variables`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
        body: JSON.stringify({ scope, scopeId: scopeId ?? projectId, key, value }),
      });
      return r.json();
    },
    { token, projectId, key, value, scope, scopeId: scopeId ?? undefined },
  );
  return result.data;
}

/**
 * Navigate to a project sub-page by URL.
 */
export async function goToProject(page: Page, project?: string, subPage = 'roles') {
  const urlMatch = page.url().match(/\/projects\/(\d+)/);
  let projectId: string;

  if (urlMatch) {
    projectId = urlMatch[1];
  } else {
    // On project list - click the project card matching the project name
    const card = page.locator('.ant-list-item').filter({ hasText: project! }).first();
    await card.click();
    await page.waitForURL(/.*\/projects\/\d+/);
    projectId = page.url().match(/\/projects\/(\d+)/)![1];
  }

  await page.goto(`/projects/${projectId}/${subPage}`);
  await page.waitForURL(/.*\/projects\/\d+/);
}

// Extended test fixture with authenticated user
export const test = base.extend<{
  authenticatedPage: Page;
}>({
  authenticatedPage: async ({ page }, use) => {
    await registerUser(page);
    await use(page);
  },
});

export { expect };
