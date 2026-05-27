import { beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import StatsView from '@/views/StatsView.vue';
import { useAuthStore } from '@/stores/useAuthStore';
import * as statsService from '@/services/statsService';

vi.mock('@/services/statsService');

const mockedStatsService = vi.mocked(statsService);

function mountView() {
  return mount(StatsView, {
    global: {
      plugins: [createPinia()],
      stubs: {
        RouterLink: true,
      },
      mocks: {
        $router: { push: vi.fn() },
      },
    },
  });
}

describe('StatsView', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-05-20T10:00:00+08:00'));
    setActivePinia(createPinia());
    const auth = useAuthStore();
    auth.$patch({ username: 'alex', token: 'token' });
    vi.resetAllMocks();
  });

  it('renders prototype-style summary and category duration rows', async () => {
    mockedStatsService.getCategoryDurations.mockResolvedValue({
      startDate: '2026-05-18',
      endDate: '2026-05-24',
      totalSlotCount: 6,
      totalRecordedMinutes: 90,
      totalAvailableMinutes: 10080,
      unrecordedMinutes: 9990,
      categories: [
        {
          categoryId: 1,
          categoryName: '工作',
          categoryColor: '#6366f1',
          categoryStatus: 'ACTIVE',
          slotCount: 4,
          durationMinutes: 60,
          percentage: 66.67,
        },
        {
          categoryId: 2,
          categoryName: '项目复盘',
          categoryColor: '#8b5cf6',
          categoryStatus: 'ARCHIVED',
          slotCount: 2,
          durationMinutes: 30,
          percentage: 33.33,
        },
      ],
    });

    const wrapper = mountView();
    await flushPromises();

    expect(mockedStatsService.getCategoryDurations).toHaveBeenCalledWith('2026-05-18', '2026-05-24');
    expect(wrapper.text()).toContain('分类耗时统计');
    expect(wrapper.text()).toContain('1 小时 30 分钟');
    expect(wrapper.text()).toContain('项目复盘');
    expect(wrapper.text()).toContain('已归档');
    expect(wrapper.find('[data-testid="category-row-1"]').text()).toContain('66.67%');
    expect(wrapper.find('[data-testid="donut-chart"]').attributes('style')).toContain('#6366f1');
  });

  it('switches to month range through the quick range control', async () => {
    mockedStatsService.getCategoryDurations.mockResolvedValue({
      startDate: '2026-05-01',
      endDate: '2026-05-31',
      totalSlotCount: 0,
      totalRecordedMinutes: 0,
      totalAvailableMinutes: 44640,
      unrecordedMinutes: 44640,
      categories: [],
    });

    const wrapper = mountView();
    await flushPromises();
    await wrapper.find('[data-testid="range-month"]').trigger('click');
    await flushPromises();

    expect(mockedStatsService.getCategoryDurations).toHaveBeenLastCalledWith('2026-05-01', '2026-05-31');
    expect(wrapper.text()).toContain('这个时间范围还没有记录');
  });
});
