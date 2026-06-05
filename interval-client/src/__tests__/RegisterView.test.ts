import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import RegisterView from '@/views/RegisterView.vue';
import { useAuthStore } from '@/stores/useAuthStore';

const push = vi.fn();

vi.mock('vue-router', () => ({
  RouterLink: { template: '<a><slot /></a>' },
  useRouter: () => ({ push }),
}));

describe('RegisterView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.useFakeTimers();
    push.mockReset();
  });

  it('requires password confirmation and strength before registering', async () => {
    const wrapper = mount(RegisterView);
    const auth = useAuthStore();
    const register = vi.spyOn(auth, 'register').mockResolvedValue({ userId: 7, username: 'alex' });

    expect(wrapper.text()).toContain('至少 8 位，必须同时包含字母和数字');
    expect(wrapper.find('input[name="confirmPassword"]').exists()).toBe(true);

    await wrapper.find('input[name="username"]').setValue('alex');
    await wrapper.find('input[name="password"]').setValue('12345678');
    await wrapper.find('input[name="confirmPassword"]').setValue('12345678');
    await wrapper.find('form').trigger('submit.prevent');

    expect(wrapper.text()).toContain('密码必须同时包含字母和数字');
    expect(register).not.toHaveBeenCalled();

    await wrapper.find('input[name="password"]').setValue('Pass1234');
    await wrapper.find('input[name="confirmPassword"]').setValue('Pass4321');
    await wrapper.find('form').trigger('submit.prevent');

    expect(wrapper.text()).toContain('两次输入的密码不一致');
    expect(register).not.toHaveBeenCalled();
  });
});
