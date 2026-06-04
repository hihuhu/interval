import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import LoginView from '@/views/LoginView.vue';
import { useAuthStore } from '@/stores/useAuthStore';

const push = vi.fn();

vi.mock('vue-router', () => ({
  RouterLink: { template: '<a><slot /></a>' },
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push }),
}));

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    push.mockReset();
  });

  function mountLoginView() {
    return mount(LoginView, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' },
        },
      },
    });
  }

  it('does not expose or prefill the development account', () => {
    const wrapper = mountLoginView();

    expect(wrapper.text()).not.toContain('开发账号');
    expect(wrapper.text()).not.toContain('123ABCdef');
    expect((wrapper.find('input[name="username"]').element as HTMLInputElement).value).toBe('');
    expect((wrapper.find('input[name="password"]').element as HTMLInputElement).value).toBe('');
  });

  it('shows readable login and forgot password copy', () => {
    const wrapper = mountLoginView();

    expect(wrapper.text()).toContain('欢迎回来');
    expect(wrapper.text()).toContain('忘记密码');
    expect(wrapper.text()).toContain('还没有账号？注册');
  });

  it('redirects to change password when login requires password change', async () => {
    const wrapper = mountLoginView();
    const auth = useAuthStore();
    vi.spyOn(auth, 'login').mockImplementation(async () => {
      auth.$patch({ token: 'token', username: 'admin', accountType: 'ADMIN', mustChangePassword: true });
    });

    await wrapper.find('input[name="username"]').setValue('admin');
    await wrapper.find('input[name="password"]').setValue('123456');
    await wrapper.find('form').trigger('submit.prevent');

    expect(push).toHaveBeenCalledWith('/change-password');
  });

  it('redirects admin login to admin page', async () => {
    const wrapper = mountLoginView();
    const auth = useAuthStore();
    vi.spyOn(auth, 'login').mockImplementation(async () => {
      auth.$patch({ token: 'token', username: 'admin', accountType: 'ADMIN', mustChangePassword: false });
    });

    await wrapper.find('input[name="username"]').setValue('admin');
    await wrapper.find('input[name="password"]').setValue('ChangedPass123');
    await wrapper.find('form').trigger('submit.prevent');

    expect(push).toHaveBeenCalledWith('/admin');
  });
});
