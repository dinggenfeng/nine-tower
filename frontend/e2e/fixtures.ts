import { test as base, expect, type Page } from '@playwright/test';

// Counter for unique usernames to avoid collisions between test runs
let userCounter = Date.now();

function uniqueUsername(prefix = 'user') {
  return `${prefix}_${++userCounter}`;
}

/**
 * Register a new user via UI and return token + userId.
 * Ends on /projects page.
 */
export async function registerUser(page: Page, username?: string) {
  const name = username ?? uniqueUsername('e2e');
  await page.goto('/register');
  await page.getByPlaceholder('用户名').fill(name);
  await page.getByPlaceholder('密码', { exact: false }).first().fill('Test1234!');
  await page.getByPlaceholder('邮箱').fill(`${name}@e2e.test`);
  await page.getByRole('button', { name: '注册' }).click();
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
  await page.getByRole('button', { name: '登录' }).click();
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
 * Navigate to a specific project's sub-page.
 * Assumes user is on /projects page.
 */
export async function goToProject(page: Page, projectName: string, subPage = 'roles') {
  await page.getByText(projectName).click();
  // Wait for project layout to load, then navigate to sub-page
  await page.waitForURL(`**/projects/*`);
  if (subPage !== 'roles') {
    // Default landing is roles, navigate to target
    await page.getByText(subPage === 'host-groups' ? '主机组' : subPage === 'variables' ? '变量' : subPage === 'environments' ? '环境' : subPage === 'tags' ? '标签' : subPage === 'playbooks' ? '剧本' : subPage === 'members' ? '成员' : subPage === 'settings' ? '设置' : subPage, { exact: false }).click();
  }
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
