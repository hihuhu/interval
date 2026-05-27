import { http } from '@/services/http';
import type { CategoryDurationSummary } from '@/types/stats';

export async function getCategoryDurations(startDate: string, endDate: string): Promise<CategoryDurationSummary> {
  return http.get<never, CategoryDurationSummary>('/api/stats/category-durations', {
    params: { startDate, endDate },
  });
}
