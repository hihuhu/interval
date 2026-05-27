import { ref } from 'vue';
import { defineStore } from 'pinia';
import * as statsService from '@/services/statsService';
import type { CategoryDurationSummary, StatsRangeKey } from '@/types/stats';

export const useStatsStore = defineStore('stats', () => {
  const summary = ref<CategoryDurationSummary | null>(null);
  const loading = ref(false);
  const errorMessage = ref<string | null>(null);
  const selectedRange = ref<StatsRangeKey>('week');

  async function fetchCategoryDurations(startDate: string, endDate: string, range: StatsRangeKey = selectedRange.value) {
    loading.value = true;
    errorMessage.value = null;
    selectedRange.value = range;
    try {
      summary.value = await statsService.getCategoryDurations(startDate, endDate);
      return summary.value;
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load statistics';
      throw error;
    } finally {
      loading.value = false;
    }
  }

  return { summary, loading, errorMessage, selectedRange, fetchCategoryDurations };
});
