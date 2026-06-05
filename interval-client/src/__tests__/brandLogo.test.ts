import { describe, expect, it } from 'vitest';
import { mount, RouterLinkStub } from '@vue/test-utils';
import AppLayout from '@/components/AppLayout.vue';
import appLayoutSource from '@/components/AppLayout.vue?raw';
import { FAVICON_PATH } from '@/constants/brand';

describe('brand logo', () => {
  it('uses the favicon asset for the app header brand mark', () => {
    const wrapper = mount(AppLayout, {
      props: { username: 'alex' },
      global: {
        stubs: { RouterLink: RouterLinkStub },
      },
    });
    const logo = wrapper.get('img.logo-icon');

    expect(logo.attributes('src')).toBe(FAVICON_PATH);
    expect(logo.attributes('aria-hidden')).toBe('true');
    expect(appLayoutSource).not.toContain('.brand-mark .logo-icon');
  });
});
