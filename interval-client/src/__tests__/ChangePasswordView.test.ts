import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import ChangePasswordView from '@/views/ChangePasswordView.vue';
import * as authService from '@/services/authService';
import { useAuthStore } from '@/stores/useAuthStore';

const push = vi.fn();

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
}));

vi.mock('@/services/authService');

describe('ChangePasswordView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.resetAllMocks();
  });

  it('renders readable password change copy', () => {
    const wrapper = mount(ChangePasswordView);

    expect(wrapper.text()).toContain('修改密码');
    expect(wrapper.text()).toContain('首次登录或密码重置后需要设置新密码');
    expect(wrapper.text()).toContain('当前密码');
    expect(wrapper.text()).toContain('新密码');
  });

  it('submits password change and routes admin to admin page', async () => {
    vi.mocked(authService.changePassword).mockResolvedValue(undefined);
    const auth = useAuthStore();
    auth.$patch({ token: 'token', username: 'admin', accountType: 'ADMIN', mustChangePassword: true });
    const wrapper = mount(ChangePasswordView);

    await wrapper.find('input[name="currentPassword"]').setValue('123456');
    await wrapper.find('input[name="newPassword"]').setValue('ChangedPass123');
    await wrapper.find('form').trigger('submit.prevent');

    expect(authService.changePassword).toHaveBeenCalledWith({
      currentPassword: '123456',
      newPassword: 'ChangedPass123',
    });
    expect(auth.mustChangePassword).toBe(false);
    expect(push).toHaveBeenCalledWith('/admin');
  });
});
