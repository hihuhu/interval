import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount, RouterLinkStub } from '@vue/test-utils';
import AppLayout from '@/components/AppLayout.vue';
import appLayoutSource from '@/components/AppLayout.vue?raw';
import * as authService from '@/services/authService';

vi.mock('@/services/authService', () => ({
  changePassword: vi.fn(),
}));

describe('AppLayout', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(authService.changePassword).mockResolvedValue();
  });

  function mountLayout(props: Record<string, unknown>) {
    return mount(AppLayout, {
      props: { username: 'alex', ...props },
      slots: { default: '<section>content</section>' },
      global: {
        stubs: { RouterLink: RouterLinkStub },
      },
    });
  }

  it('shows time grid and stats navigation for regular users', () => {
    const wrapper = mountLayout({ accountType: 'USER', activeRoute: 'timeGrid' });

    expect(wrapper.text()).toContain('时间格');
    expect(wrapper.text()).toContain('统计');
    expect(wrapper.text()).not.toContain('管理');
  });

  it('shows only admin navigation for admins', () => {
    const wrapper = mountLayout({ username: 'admin', accountType: 'ADMIN', activeRoute: 'admin' });

    expect(wrapper.text()).toContain('管理');
    expect(wrapper.text()).not.toContain('时间格');
    expect(wrapper.text()).not.toContain('统计');
  });

  it('opens user menu and emits logout from dropdown', async () => {
    const wrapper = mountLayout({ accountType: 'USER', activeRoute: 'timeGrid' });

    await wrapper.get('button.user-trigger').trigger('click');
    expect(wrapper.text()).toContain('修改密码');
    expect(wrapper.text()).toContain('退出登录');

    await wrapper.get('button.logout-menu-item').trigger('click');
    expect(wrapper.emitted('logout')).toHaveLength(1);
  });

  it('opens styled change password modal and validates before submitting', async () => {
    const wrapper = mountLayout({ accountType: 'USER', activeRoute: 'timeGrid' });

    await wrapper.get('button.user-trigger').trigger('click');
    await wrapper.get('button.change-password-menu-item').trigger('click');

    expect(wrapper.find('.change-password-modal').exists()).toBe(true);
    expect(wrapper.find('.modal-head').exists()).toBe(true);
    expect(wrapper.find('.modal-body').exists()).toBe(true);
    expect(wrapper.find('.modal-foot').exists()).toBe(true);
    expect(wrapper.text()).toContain('修改密码');
    expect(wrapper.text()).toContain('至少 8 位，必须同时包含字母和数字');

    await wrapper.get('input[name="currentPassword"]').setValue('OldPass123');
    await wrapper.get('input[name="newPassword"]').setValue('abcdefgh');
    await wrapper.get('input[name="confirmPassword"]').setValue('abcdefgh');
    await wrapper.get('button.change-password-submit').trigger('click');

    expect(wrapper.text()).toContain('密码必须同时包含字母和数字');
    expect(authService.changePassword).not.toHaveBeenCalled();
  });

  it('submits self-service password change and shows success feedback', async () => {
    const wrapper = mountLayout({ accountType: 'ADMIN', activeRoute: 'admin' });

    await wrapper.get('button.user-trigger').trigger('click');
    await wrapper.get('button.change-password-menu-item').trigger('click');
    await wrapper.get('input[name="currentPassword"]').setValue('OldPass123');
    await wrapper.get('input[name="newPassword"]').setValue('NewPass123');
    await wrapper.get('input[name="confirmPassword"]').setValue('NewPass123');
    await wrapper.get('button.change-password-submit').trigger('click');
    await Promise.resolve();

    expect(authService.changePassword).toHaveBeenCalledWith({
      currentPassword: 'OldPass123',
      newPassword: 'NewPass123',
    });
    expect(wrapper.text()).toContain('密码已修改');
    expect((wrapper.get('input[name="currentPassword"]').element as HTMLInputElement).value).toBe('');
  });

  it('does not create a main stacking context above full-screen overlays', () => {
    expect(appLayoutSource).toContain('main {');
    expect(appLayoutSource).not.toMatch(/main\s*\{[^}]*z-index:\s*1;/);
  });
});
