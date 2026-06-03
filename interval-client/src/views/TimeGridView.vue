<template>
  <AppLayout :username="auth.username ?? 'User'" active-route="timeGrid" @logout="logout">
    <section class="hero-grid">
      <div class="hero-panel surface-card">
        <div class="hero-content">
          <div class="hero-eyebrow">
            <span></span>
            DAILY TIME GRID
          </div>
          <h1>
            把一天拆成 96 格，
            <br />
            让记录变得更直观。
          </h1>
          <p>
            这个原型重点验证时间块选择、批量登记、备注回显和分类管理。视觉上我把它调整得更像正式产品：更轻、更清晰，也更有节奏感。
          </p>
          <div class="hero-chips">
            <span>拖拽连续选择</span>
            <span>点击补选跳选</span>
            <span>右侧实时编辑</span>
          </div>
        </div>
      </div>

      <div class="metric-grid" aria-label="时间记录概览">
        <div class="metric-card">
          <span>今日已记录</span>
          <strong>{{ recordedHoursText }}</strong>
          <small>{{ recordedDurationText }} · {{ timeSlotStore.slots.length }} / 96 格</small>
        </div>
        <div class="metric-card">
          <span>完成度</span>
          <strong>{{ completionRate }}%</strong>
          <small>按全天时间格粗略估算</small>
        </div>
        <div class="metric-card">
          <span>今日分类数</span>
          <strong>{{ usedCategoryCount }}</strong>
          <small>活动分类 {{ categoryStore.categories.length }} 个</small>
        </div>
        <div class="metric-card">
          <span>当前选择</span>
          <strong>{{ selectedSlotIndexes.length }}</strong>
          <small>{{ selectedSlotIndexes.length ? selectedRangeText : '还没有选择时间块' }}</small>
        </div>
      </div>
    </section>

    <section class="toolbar">
      <div class="date-strip surface-card-soft">
        <div class="date-summary">
          <CalendarDays :size="18" />
          <div>
            <span>当前日期</span>
            <strong>{{ formattedDate }}</strong>
            <small>{{ dayName }}</small>
          </div>
        </div>
        <div class="date-actions" aria-label="日期切换">
          <button type="button" class="date-icon-button" data-testid="previous-day" aria-label="前一天" @click="shiftDate(-1)">
            <ChevronLeft :size="17" />
          </button>
          <button type="button" class="today-button" data-testid="today-button" @click="setToday">今天</button>
          <button type="button" class="date-icon-button" data-testid="next-day" aria-label="后一天" @click="shiftDate(1)">
            <ChevronRight :size="17" />
          </button>
          <div class="date-picker-shell" ref="datePickerRef">
            <button
              type="button"
              class="date-trigger"
              data-testid="date-trigger"
              aria-haspopup="dialog"
              :aria-expanded="datePickerOpen"
              @click="toggleDatePicker"
            >
              <CalendarDays :size="15" />
              <span>{{ compactDateText }}</span>
            </button>
            <div v-if="datePickerOpen" class="date-popover" data-testid="date-popover" role="dialog" aria-label="选择日期">
              <div class="calendar-head">
                <button type="button" class="calendar-nav" aria-label="上个月" @click="shiftCalendarMonth(-1)">
                  <ChevronLeft :size="15" />
                </button>
                <strong>{{ calendarMonthLabel }}</strong>
                <button type="button" class="calendar-nav" aria-label="下个月" @click="shiftCalendarMonth(1)">
                  <ChevronRight :size="15" />
                </button>
              </div>
              <div class="calendar-weekdays" aria-hidden="true">
                <span v-for="weekday in calendarWeekdays" :key="weekday">{{ weekday }}</span>
              </div>
              <div class="calendar-grid">
                <button
                  v-for="day in calendarDays"
                  :key="day.key"
                  type="button"
                  class="calendar-day"
                  :class="{ muted: !day.inCurrentMonth, selected: day.isoDate === date, today: day.isoDate === todayDate }"
                  :aria-label="day.ariaLabel"
                  :aria-pressed="day.isoDate === date"
                  @click="selectCalendarDate(day.isoDate)"
                >
                  {{ day.dayNumber }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <p v-if="categoryNotice" class="notice">{{ categoryNotice }}</p>
    <p v-if="categoryStore.errorMessage || timeSlotStore.errorMessage" class="error">
      {{ categoryStore.errorMessage || timeSlotStore.errorMessage }}
    </p>

    <section class="workbench">
      <TimeGrid
        :slots="timeSlotStore.slots"
        :loading="timeSlotStore.loading"
        :selected-slot-indexes="selectedSlotIndexes"
        @selection-change="handleSelectionChange"
        @delete-slot="deleteSlot"
      />
      <aside class="side-stack">
        <SelectionEditorPanel
          :selected-slot-indexes="selectedSlotIndexes"
          :slots="timeSlotStore.slots"
          :categories="categoryStore.categories"
          :saving="timeSlotStore.saving"
          @save="saveSelection"
          @erase="eraseSelection"
          @cancel="clearSelection"
          @manage-categories="openCategoryManager"
        />
      </aside>
    </section>

    <div
      v-if="categoryManagerOpen"
      class="modal-backdrop"
      data-testid="category-manager-backdrop"
      @click.self="closeCategoryManager"
    >
      <section
        class="category-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="category-manager-title"
      >
        <div class="dialog-header">
          <div>
            <p class="dialog-eyebrow">分类管理</p>
            <h2 id="category-manager-title">维护时间分类</h2>
          </div>
          <button
            type="button"
            class="ghost-action dialog-close"
            data-testid="close-category-manager"
            aria-label="关闭分类管理"
            @click="closeCategoryManager"
          >
            <X :size="17" />
          </button>
        </div>
        <CategoryManagerPanel
          :categories="categoryStore.categories"
          embedded
          @create="createCategory"
          @update="updateCategory"
          @delete="deleteCategory"
        />
      </section>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { CalendarDays, ChevronLeft, ChevronRight, X } from '@lucide/vue';
import { useRouter } from 'vue-router';
import AppLayout from '@/components/AppLayout.vue';
import CategoryManagerPanel from '@/components/CategoryManagerPanel.vue';
import SelectionEditorPanel from '@/components/SelectionEditorPanel.vue';
import TimeGrid from '@/components/TimeGrid.vue';
import { slotIndexToRange, todayIsoDate } from '@/composables/useTimeSlots';
import { useAuthStore } from '@/stores/useAuthStore';
import { useCategoryStore } from '@/stores/useCategoryStore';
import { useTimeSlotStore } from '@/stores/useTimeSlotStore';
import type { CreateCategoryRequest, UpdateCategoryRequest } from '@/types/category';

const router = useRouter();
const auth = useAuthStore();
const categoryStore = useCategoryStore();
const timeSlotStore = useTimeSlotStore();
const date = ref(timeSlotStore.selectedDate);
const selectedSlotIndexes = ref<number[]>([]);
const categoryNotice = ref<string | null>(null);
const categoryManagerOpen = ref(false);
const datePickerOpen = ref(false);
const datePickerRef = ref<HTMLElement | null>(null);
const calendarCursor = ref(startOfMonth(new Date(`${date.value}T00:00:00`)));
const calendarWeekdays = ['一', '二', '三', '四', '五', '六', '日'];

const recordedMinutes = computed(() => timeSlotStore.slots.length * 15);
const recordedHoursText = computed(() => `${Math.floor(recordedMinutes.value / 60)}h`);
const recordedDurationText = computed(() => {
  const hours = Math.floor(recordedMinutes.value / 60);
  const restMinutes = recordedMinutes.value % 60;
  if (hours === 0) return `${restMinutes} 分钟`;
  if (restMinutes === 0) return `${hours} 小时`;
  return `${hours} 小时 ${restMinutes} 分钟`;
});
const completionRate = computed(() => Math.round((timeSlotStore.slots.length / 96) * 100));
const usedCategoryCount = computed(() => new Set(timeSlotStore.slots.map((slot) => slot.categoryId).filter(Boolean)).size);
const selectedRangeText = computed(() => {
  if (selectedSlotIndexes.value.length === 0) return '';
  const sorted = [...selectedSlotIndexes.value].sort((a, b) => a - b);
  return sorted.length === 1
    ? slotIndexToRange(sorted[0])
    : `${slotIndexToRange(sorted[0]).split('-')[0]}-${slotIndexToRange(sorted[sorted.length - 1]).split('-')[1]}`;
});
const selectedDate = computed(() => new Date(`${date.value}T00:00:00`));
const formattedDate = computed(() => selectedDate.value.toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
}));
const dayName = computed(() => selectedDate.value.toLocaleDateString('zh-CN', { weekday: 'long' }));
const todayDate = computed(() => todayIsoDate());
const compactDateText = computed(() => selectedDate.value.toLocaleDateString('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  weekday: 'short',
}));
const calendarMonthLabel = computed(() => calendarCursor.value.toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
}));
const calendarDays = computed(() => {
  const firstDay = startOfMonth(calendarCursor.value);
  const startOffset = (firstDay.getDay() + 6) % 7;
  const gridStart = new Date(firstDay);
  gridStart.setDate(firstDay.getDate() - startOffset);

  return Array.from({ length: 42 }, (_, index) => {
    const day = new Date(gridStart);
    day.setDate(gridStart.getDate() + index);
    const isoDate = formatLocalDate(day);
    return {
      key: isoDate,
      isoDate,
      dayNumber: day.getDate(),
      inCurrentMonth: day.getMonth() === calendarCursor.value.getMonth(),
      ariaLabel: day.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        weekday: 'long',
      }),
    };
  });
});

onMounted(async () => {
  document.addEventListener('click', handleOutsideDatePickerClick);
  await Promise.all([categoryStore.fetchCategories(), timeSlotStore.fetchDailySlots(date.value)]);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleOutsideDatePickerClick);
});

watch(date, (value) => {
  calendarCursor.value = startOfMonth(new Date(`${value}T00:00:00`));
});

async function reload() {
  timeSlotStore.setDate(date.value);
  selectedSlotIndexes.value = [];
  await timeSlotStore.fetchDailySlots(date.value);
}

function toggleDatePicker() {
  datePickerOpen.value = !datePickerOpen.value;
}

function closeDatePicker() {
  datePickerOpen.value = false;
}

async function selectCalendarDate(isoDate: string) {
  date.value = isoDate;
  closeDatePicker();
  await reload();
}

function shiftCalendarMonth(offset: number) {
  const current = new Date(calendarCursor.value);
  current.setMonth(current.getMonth() + offset);
  calendarCursor.value = startOfMonth(current);
}

function handleOutsideDatePickerClick(event: MouseEvent) {
  if (!datePickerOpen.value) return;
  const target = event.target;
  if (!(target instanceof Node)) return;
  if (datePickerRef.value?.contains(target)) return;
  closeDatePicker();
}

async function createCategory(payload: CreateCategoryRequest) {
  await categoryStore.createCategory(payload);
  categoryNotice.value = '分类已创建。';
}

function handleSelectionChange(slotIndexes: number[]) {
  selectedSlotIndexes.value = slotIndexes;
}

async function saveSelection(payload: { slotIndexes: number[]; categoryId: number; note: string | null; noteTouched: boolean }) {
  const category = categoryStore.categories.find((item) => item.id === payload.categoryId);
  await timeSlotStore.upsertSlotRange(date.value, payload.slotIndexes, category?.name ?? '', payload.categoryId, {
    note: payload.note,
    noteTouched: payload.noteTouched,
  });
  clearSelection();
}

async function deleteSlot(slotId: number) {
  await timeSlotStore.deleteSlot(slotId);
}

async function updateCategory(categoryId: number, payload: UpdateCategoryRequest) {
  await categoryStore.updateCategory(categoryId, payload);
  categoryNotice.value = '分类已更新。';
}

async function deleteCategory(categoryId: number, categoryName: string) {
  const confirmed = window.confirm(`确定删除分类“${categoryName}”吗？有历史记录的分类会被归档。`);
  if (!confirmed) return;

  const result = await categoryStore.deleteCategory(categoryId);
  categoryNotice.value = result.action === 'DELETED'
    ? '分类已删除。'
    : `分类已归档，${result.affectedRecords} 条历史记录将继续显示该分类。`;
}

function openCategoryManager() {
  categoryManagerOpen.value = true;
}

function closeCategoryManager() {
  categoryManagerOpen.value = false;
}

async function eraseSelection(slotIndexes: number[]) {
  const ids = slotIndexes
    .map((slotIndex) => timeSlotStore.slotsByIndex.get(slotIndex)?.id)
    .filter((slotId): slotId is number => Boolean(slotId));
  await timeSlotStore.deleteSlotBatch(ids);
  clearSelection();
}

function clearSelection() {
  selectedSlotIndexes.value = [];
}

async function shiftDate(offset: number) {
  const current = new Date(`${date.value}T00:00:00`);
  current.setDate(current.getDate() + offset);
  date.value = formatLocalDate(current);
  closeDatePicker();
  await reload();
}

async function setToday() {
  date.value = todayIsoDate();
  closeDatePicker();
  await reload();
}

async function logout() {
  auth.logout();
  await router.push('/login');
}

function formatLocalDate(value: Date) {
  const year = value.getFullYear();
  const month = `${value.getMonth() + 1}`.padStart(2, '0');
  const day = `${value.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function startOfMonth(value: Date) {
  return new Date(value.getFullYear(), value.getMonth(), 1);
}
</script>

<style scoped>
.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(360px, 0.82fr);
  gap: 18px;
  margin-bottom: 24px;
}

.hero-panel {
  position: relative;
  overflow: hidden;
  padding: 30px;
  background:
    radial-gradient(circle at top right, rgba(165, 180, 252, 0.24), transparent 28%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(238, 242, 255, 0.88));
}

.hero-panel::after {
  content: '';
  position: absolute;
  inset: auto -80px -120px auto;
  width: 220px;
  height: 220px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.18), transparent 68%);
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 660px;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(129, 140, 248, 0.18);
  background: rgba(255, 255, 255, 0.70);
  color: #4338ca;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.hero-eyebrow span {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: linear-gradient(135deg, #818cf8, #4f46e5);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.12);
}

h1 {
  margin: 18px 0 0;
  color: #0f172a;
  font-size: clamp(32px, 5vw, 44px);
  line-height: 1.08;
  letter-spacing: 0;
}

.hero-content p {
  max-width: 620px;
  margin: 16px 0 0;
  color: #64748b;
  font-size: 15px;
  line-height: 1.8;
}

.hero-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.hero-chips span {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 13px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.85);
  background: rgba(255, 255, 255, 0.78);
  color: #475569;
  font-size: 12px;
  font-weight: 800;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
  padding: 18px;
}

.metric-card span {
  color: #64748b;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.06em;
}

.metric-card strong {
  display: block;
  margin-top: 10px;
  color: #0f172a;
  font-size: 30px;
  line-height: 1;
}

.metric-card small {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.toolbar {
  display: block;
  margin-bottom: 18px;
}

.date-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 13px;
  padding: 10px 12px;
  color: #475569;
}

.date-summary {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.date-summary span {
  display: block;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.date-summary strong,
.date-summary small {
  display: block;
}

.date-summary strong {
  color: #0f172a;
  font-size: 15px;
}

.date-summary small {
  color: #64748b;
  font-size: 11px;
}

.date-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.date-icon-button,
.today-button,
.date-trigger,
.calendar-nav,
.calendar-day {
  min-width: 38px;
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(203, 213, 225, 0.75);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  color: #475569;
  cursor: pointer;
  transition: background .16s ease, border-color .16s ease, color .16s ease, transform .16s ease;
}

.date-icon-button:hover,
.today-button:hover,
.date-trigger:hover,
.calendar-nav:hover,
.calendar-day:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.35);
  background: #ffffff;
  color: #3730a3;
}

.today-button {
  border-color: rgba(129, 140, 248, 0.32);
  background: rgba(238, 242, 255, 0.84);
  padding: 0 13px;
  color: #4338ca;
  font-size: 12px;
  font-weight: 900;
}

.date-picker-shell {
  position: relative;
}

.date-trigger {
  min-width: 124px;
  gap: 8px;
  padding: 0 12px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
}

.date-popover {
  position: absolute;
  right: 0;
  top: calc(100% + 10px);
  z-index: 120;
  width: 294px;
  border: 1px solid rgba(203, 213, 225, 0.9);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 22px 60px rgba(15, 23, 42, 0.18);
  padding: 14px;
}

.calendar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.calendar-head strong {
  color: #0f172a;
  font-size: 14px;
}

.calendar-nav {
  min-width: 34px;
  min-height: 34px;
  border-radius: 11px;
  color: #475569;
}

.calendar-weekdays,
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 5px;
}

.calendar-weekdays {
  margin-bottom: 7px;
}

.calendar-weekdays span {
  color: #64748b;
  font-size: 11px;
  font-weight: 900;
  text-align: center;
}

.calendar-day {
  min-width: 0;
  min-height: 34px;
  border-radius: 11px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.calendar-day.muted {
  color: #94a3b8;
  background: rgba(248, 250, 252, 0.62);
}

.calendar-day.today {
  border-color: rgba(14, 165, 233, 0.34);
  color: #0369a1;
  background: #f0f9ff;
}

.calendar-day.selected {
  border-color: rgba(79, 70, 229, 0.52);
  background: #4f46e5;
  color: #ffffff;
  box-shadow: 0 12px 22px rgba(79, 70, 229, 0.22);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.notice,
.error {
  border-radius: 14px;
  padding: 12px 14px;
  font-size: 13px;
  margin: 0 0 16px;
}

.notice {
  color: #047857;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
}

.error {
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  align-items: start;
  gap: 18px;
}

.side-stack {
  display: grid;
  gap: 18px;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.38);
  backdrop-filter: blur(6px);
}

.category-dialog {
  width: min(620px, 100%);
  max-height: min(720px, calc(100vh - 48px));
  overflow: auto;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.22);
  padding: 20px;
}

.dialog-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.dialog-eyebrow {
  margin: 0 0 5px;
  color: #64748b;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.dialog-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}

.dialog-close {
  width: 38px;
  height: 38px;
  padding: 0;
}

@media (max-width: 1100px) {
  .hero-grid,
  .workbench {
    grid-template-columns: 1fr;
  }

  .metric-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .date-actions {
    align-items: stretch;
  }

  .date-strip {
    align-items: stretch;
    flex-direction: column;
  }

  .date-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .date-picker-shell,
  .date-trigger {
    width: 100%;
  }

  .date-popover {
    left: 0;
    right: auto;
    width: min(294px, calc(100vw - 28px));
  }
}
</style>
