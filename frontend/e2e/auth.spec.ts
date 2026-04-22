import { test, expect, registerUser, loginUser } from './fixtures';

test('register, login, logout', async ({ page }) => {
  const { username } = await registerUser(page);
  await expect(page).toHaveURL(/.*projects/);

  // Logout
  await page.getByText(username).click();
  await page.getByText('退出登录').click();
  await expect(page).toHaveURL(/.*login/);

  // Login again
  await loginUser(page, username);
  await expect(page).toHaveURL(/.*projects/);
});
