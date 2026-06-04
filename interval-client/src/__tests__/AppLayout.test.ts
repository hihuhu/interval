import { describe, expect, it } from 'vitest';
import { mount, RouterLinkStub } from '@vue/test-utils';
import AppLayout from '@/components/AppLayout.vue';
import appLayoutSource from '@/components/AppLayout.vue?raw';

describe('AppLayout', () => {
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
    expect(wrapper.text()).toContain('退出登录');

    await wrapper.get('button.logout-menu-item').trigger('click');
    expect(wrapper.emitted('logout')).toHaveLength(1);
  });

  it('does not create a main stacking context above full-screen overlays', () => {
    expect(appLayoutSource).toContain('main {');
    expect(appLayoutSource).not.toMatch(/main\s*\{[^}]*z-index:\s*1;/);
  });
});
