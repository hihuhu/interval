import { describe, expect, it } from 'vitest';
import { mount, RouterLinkStub } from '@vue/test-utils';
import AppLayout from '@/components/AppLayout.vue';
import appLayoutSource from '@/components/AppLayout.vue?raw';

describe('AppLayout', () => {
  it('only shows primary page navigation in the header', () => {
    const wrapper = mount(AppLayout, {
      props: { username: 'admin', activeRoute: 'timeGrid' },
      slots: { default: '<section>content</section>' },
      global: {
        stubs: { RouterLink: RouterLinkStub },
      },
    });

    expect(wrapper.text()).toContain('时间格');
    expect(wrapper.text()).toContain('统计');
    expect(wrapper.find('nav').text()).not.toContain('分类');
  });

  it('does not create a main stacking context above full-screen overlays', () => {
    expect(appLayoutSource).toContain('main {');
    expect(appLayoutSource).not.toMatch(/main\s*\{[^}]*z-index:\s*1;/);
  });
});
