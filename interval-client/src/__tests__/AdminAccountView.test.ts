import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import AdminAccountView from '@/views/AdminAccountView.vue';
import * as adminService from '@/services/adminService';
import type { AdminDashboardDto, AdminUserListItemDto } from '@/types/admin';

vi.mock('@/services/adminService');

vi.mock('@/components/AppLayout.vue', () => ({
  default: {
    props: ['username', 'activeRoute', 'accountType'],
    emits: ['logout'],
    template: '<div><nav>管理</nav><slot /></div>',
  },
}));

const dashboard: AdminDashboardDto = {
  totalUsers: 2,
  activeUsers: 1,
  disabledUsers: 1,
  activeToday: 1,
  activeLast7Days: 2,
};

const users: AdminUserListItemDto[] = [
  {
    userId: 2,
    username: 'alex',
    status: 'ACTIVE',
    createdAt: '2026-05-01T00:00:00Z',
    lastLoginAt: '2026-06-03T01:00:00Z',
    lastActiveAt: '2026-06-03T02:00:00Z',
    mustChangePassword: false,
  },
  {
    userId: 3,
    username: 'morgan',
    status: 'DISABLED',
    createdAt: '2026-05-02T00:00:00Z',
    lastLoginAt: '2026-05-25T01:00:00Z',
    lastActiveAt: '2026-05-25T02:00:00Z',
    mustChangePassword: true,
  },
];

describe('AdminAccountView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.resetAllMocks();
    vi.mocked(adminService.fetchDashboard).mockResolvedValue(dashboard);
    vi.mocked(adminService.fetchUsers).mockResolvedValue(users);
  });

  async function mountView() {
    const wrapper = mount(AdminAccountView);
    await Promise.resolve();
    await Promise.resolve();
    return wrapper;
  }

  it('renders dashboard metrics and users with readable copy', async () => {
    const wrapper = await mountView();

    expect(wrapper.text()).toContain('账号管理');
    expect(wrapper.text()).toContain('总用户');
    expect(wrapper.text()).toContain('2');
    expect(wrapper.text()).toContain('alex');
    expect(wrapper.text()).toContain('morgan');
  });

  it('shows selected user detail when clicking a row', async () => {
    const wrapper = await mountView();

    await wrapper.get('[data-test="user-row-3"]').trigger('click');

    expect(wrapper.get('[data-test="user-detail"]').text()).toContain('morgan');
  });

  it('opens reset password modal with segmented controls', async () => {
    const wrapper = await mountView();

    await wrapper.get('[data-test="reset-2"]').trigger('click');

    expect(wrapper.text()).toContain('自动生成');
    expect(wrapper.text()).toContain('手动设置');
  });

  it('requires valid and matching manual reset password before submitting', async () => {
    const wrapper = await mountView();

    await wrapper.get('[data-test="reset-2"]').trigger('click');
    await wrapper.findAll('.segmented-control button')[1].trigger('click');

    expect(wrapper.text()).toContain('至少 8 位，必须同时包含字母和数字');
    expect(wrapper.find('input[name="confirmTemporaryPassword"]').exists()).toBe(true);

    await wrapper.get('input[name="temporaryPassword"]').setValue('abcdefgh');
    await wrapper.get('input[name="confirmTemporaryPassword"]').setValue('abcdefgh');
    await wrapper.find('.modal-foot .btn.primary').trigger('click');

    expect(wrapper.text()).toContain('密码必须同时包含字母和数字');
    expect(adminService.resetPassword).not.toHaveBeenCalled();

    await wrapper.get('input[name="temporaryPassword"]').setValue('TempPass123');
    await wrapper.get('input[name="confirmTemporaryPassword"]').setValue('TempPass456');
    await wrapper.find('.modal-foot .btn.primary').trigger('click');

    expect(wrapper.text()).toContain('两次输入的密码不一致');
    expect(adminService.resetPassword).not.toHaveBeenCalled();
  });

  it('filters users by keyword and status', async () => {
    const wrapper = await mountView();

    await wrapper.get('[data-test="user-keyword"]').setValue('alex');
    expect(adminService.fetchUsers).toHaveBeenLastCalledWith({ keyword: 'alex', status: '' });

    await wrapper.get('[data-test="user-status"]').setValue('DISABLED');
    expect(adminService.fetchUsers).toHaveBeenLastCalledWith({ keyword: 'alex', status: 'DISABLED' });
  });

  it('uses red UI for disabled users in table and detail', async () => {
    const wrapper = await mountView();

    expect(wrapper.get('[data-test="user-row-3"]').classes()).toContain('disabled-row');
    await wrapper.get('[data-test="user-row-3"]').trigger('click');
    expect(wrapper.get('[data-test="user-detail"]').classes()).toContain('disabled-detail');
  });

  it('keeps prototype visual structure for metrics and selected user detail', async () => {
    const wrapper = await mountView();

    expect(wrapper.findAll('.metric .metric-top')).toHaveLength(5);
    expect(wrapper.findAll('.metric .metric-note')).toHaveLength(5);
    expect(wrapper.find('[data-test="user-detail"] .detail-header').exists()).toBe(true);
    expect(wrapper.find('[data-test="user-detail"] .status-pill').exists()).toBe(true);
    expect(wrapper.find('[data-test="user-detail"] .action-stack').exists()).toBe(true);
  });
});
