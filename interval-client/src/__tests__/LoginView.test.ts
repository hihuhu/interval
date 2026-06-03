import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import LoginView from '@/views/LoginView.vue';

vi.mock('vue-router', () => ({
  RouterLink: { template: '<a><slot /></a>' },
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push: vi.fn() }),
}));

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('does not expose or prefill the development account', () => {
    const wrapper = mount(LoginView);

    expect(wrapper.text()).not.toContain('开发账号');
    expect(wrapper.text()).not.toContain('123ABCdef');
    expect((wrapper.find('input[name="username"]').element as HTMLInputElement).value).toBe('');
    expect((wrapper.find('input[name="password"]').element as HTMLInputElement).value).toBe('');
  });
});
