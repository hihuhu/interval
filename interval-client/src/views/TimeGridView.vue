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
      <div class="date-card surface-card-soft">
        <CalendarDays :size="20" />
        <div>
          <span>当前日期</span>
          <strong>{{ formattedDate }}</strong>
          <small>{{ dayName }}</small>
        </div>
        <input v-model="date" type="date" aria-label="选择日期" @change="reload" />
      </div>
      <div class="date-actions">
        <button type="button" class="ghost-action" @click="shiftDate(-1)">
          <ChevronLeft :size="16" />
          前一天
        </button>
        <button type="button" class="primary-action today-button" @click="setToday">今天</button>
        <button type="button" class="ghost-action" @click="shiftDate(1)">
          后一天
          <ChevronRight :size="16" />
        </button>
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
        />
        <CategoryManagerPanel
          :categories="categoryStore.categories"
          @create="createCategory"
          @update="updateCategory"
          @delete="deleteCategory"
        />
      </aside>
    </section>
  </AppLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { CalendarDays, ChevronLeft, ChevronRight } from '@lucide/vue';
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

onMounted(async () => {
  await Promise.all([categoryStore.fetchCategories(), timeSlotStore.fetchDailySlots(date.value)]);
});

async function reload() {
  timeSlotStore.setDate(date.value);
  selectedSlotIndexes.value = [];
  await timeSlotStore.fetchDailySlots(date.value);
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
  date.value = current.toISOString().slice(0, 10);
  await reload();
}

async function setToday() {
  date.value = todayIsoDate();
  await reload();
}

async function logout() {
  auth.logout();
  await router.push('/login');
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.date-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 13px;
  padding: 13px 14px;
  color: #475569;
}

.date-card span {
  display: block;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.14em;
}

.date-card strong,
.date-card small {
  display: block;
}

.date-card strong {
  color: #0f172a;
  font-size: 15px;
}

.date-card small {
  color: #64748b;
  font-size: 11px;
}

.date-card input {
  max-width: 144px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.86);
  padding: 9px 10px;
  color: #0f172a;
}

.date-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.ghost-action,
.today-button {
  min-height: 42px;
  padding: 0 14px;
  font-size: 12px;
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

  .toolbar,
  .date-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .date-card {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .date-card input {
    grid-column: 1 / -1;
    max-width: none;
  }
}
</style>
