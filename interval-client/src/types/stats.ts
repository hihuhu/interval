export type StatsRangeKey = 'today' | 'week' | 'month' | 'custom';

export interface CategoryDurationStat {
  categoryId: number;
  categoryName: string;
  categoryColor: string;
  categoryStatus: 'ACTIVE' | 'ARCHIVED';
  slotCount: number;
  durationMinutes: number;
  percentage: number;
}

export interface CategoryDurationSummary {
  startDate: string;
  endDate: string;
  totalSlotCount: number;
  totalRecordedMinutes: number;
  totalAvailableMinutes: number;
  unrecordedMinutes: number;
  categories: CategoryDurationStat[];
}
