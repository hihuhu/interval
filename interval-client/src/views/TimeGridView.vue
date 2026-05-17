<template>
  <AppLayout :username="auth.username ?? 'User'" @logout="logout">
    <section class="toolbar">
      <label>日期<input v-model="date" type="date" @change="reload" /></label>
      <form class="category-form" @submit.prevent="createCategory">
        <input v-model="newCategoryName" placeholder="新分类名称" />
        <input v-model="newCategoryColor" type="color" aria-label="分类颜色" />
        <button type="submit">新建分类</button>
      </form>
    </section>

    <p v-if="categoryNotice" class="notice">{{ categoryNotice }}</p>
    <p v-if="categoryStore.errorMessage || timeSlotStore.errorMessage" class="error">
      {{ categoryStore.errorMessage || timeSlotStore.errorMessage }}
    </p>

    <section class="category-panel" aria-labelledby="category-panel-title">
      <div class="panel-header">
        <div>
          <h2 id="category-panel-title">分类管理</h2>
          <p>编辑分类名称和颜色，或删除不再使用的分类。</p>
        </div>
      </div>
      <div v-if="categoryStore.categories.length === 0" class="empty-state">暂无分类，请先新建一个分类。</div>
      <div v-else class="category-list">
        <form
          v-for="category in categoryStore.categories"
          :key="category.id"
          class="category-row"
          @submit.prevent="updateCategory(category.id)"
        >
          <span class="swatch" :style="{ backgroundColor: getCategoryDraft(category.id).colorCode }"></span>
          <input
            v-model="getCategoryDraft(category.id).name"
            :aria-label="`${category.name} 分类名称`"
            placeholder="分类名称"
          />
          <input
            v-model="getCategoryDraft(category.id).colorCode"
            type="color"
            :aria-label="`${category.name} 分类颜色`"
          />
          <button type="submit">保存</button>
          <button type="button" class="danger" @click="deleteCategory(category.id, category.name)">删除</button>
        </form>
      </div>
    </section>

    <TimeGrid
      :slots="timeSlotStore.slots"
      :loading="timeSlotStore.loading"
      @select-slot="openEditor"
      @select-slot-range="openRangeEditor"
      @delete-slot="deleteSlot"
    />

    <SlotEditorModal
      :open="editorOpen"
      :slot-index="selectedSlotIndex"
      :slot-indexes="selectedSlotIndexes"
      :existing-slot="existingSlot"
      :categories="categoryStore.categories"
      :saving="timeSlotStore.saving"
      @close="editorOpen = false"
      @submit="saveSlot"
    />
  </AppLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppLayout from '@/components/AppLayout.vue';
import SlotEditorModal from '@/components/SlotEditorModal.vue';
import TimeGrid from '@/components/TimeGrid.vue';
import { useAuthStore } from '@/stores/useAuthStore';
import { useCategoryStore } from '@/stores/useCategoryStore';
import { useTimeSlotStore } from '@/stores/useTimeSlotStore';
import type { CategoryDto } from '@/types/category';

interface CategoryDraft {
  name: string;
  colorCode: string;
}

const router = useRouter();
const auth = useAuthStore();
const categoryStore = useCategoryStore();
const timeSlotStore = useTimeSlotStore();
const date = ref(timeSlotStore.selectedDate);
const newCategoryName = ref('');
const newCategoryColor = ref('#3b82f6');
const editorOpen = ref(false);
const selectedSlotIndex = ref<number | null>(null);
const selectedSlotIndexes = ref<number[]>([]);
const categoryNotice = ref<string | null>(null);
const categoryDrafts = reactive<Record<number, CategoryDraft>>({});
const existingSlot = computed(() => selectedSlotIndex.value === null ? undefined : timeSlotStore.slotsByIndex.get(selectedSlotIndex.value));

onMounted(async () => {
  await Promise.all([categoryStore.fetchCategories(), timeSlotStore.fetchDailySlots(date.value)]);
  syncCategoryDrafts();
});

async function reload() {
  timeSlotStore.setDate(date.value);
  await timeSlotStore.fetchDailySlots(date.value);
}

async function createCategory() {
  const name = newCategoryName.value.trim();
  if (!name) return;
  const category = await categoryStore.createCategory({ name, colorCode: newCategoryColor.value });
  categoryDrafts[category.id] = toCategoryDraft(category);
  newCategoryName.value = '';
  categoryNotice.value = '分类已创建。';
}

function openEditor(slotIndex: number) {
  selectedSlotIndex.value = slotIndex;
  selectedSlotIndexes.value = [slotIndex];
  editorOpen.value = true;
}

function openRangeEditor(slotIndexes: number[]) {
  selectedSlotIndexes.value = slotIndexes;
  selectedSlotIndex.value = slotIndexes[0] ?? null;
  editorOpen.value = slotIndexes.length > 0;
}

async function saveSlot(payload: { slotIndex: number; slotIndexes?: number[]; activityName: string; categoryId: number }) {
  const slotIndexes = payload.slotIndexes ?? [payload.slotIndex];
  if (slotIndexes.length > 1) {
    await timeSlotStore.upsertSlotRange(date.value, slotIndexes, payload.activityName, payload.categoryId);
  } else {
    await timeSlotStore.upsertSlot({ date: date.value, slotIndex: payload.slotIndex, activityName: payload.activityName, categoryId: payload.categoryId });
  }
  editorOpen.value = false;
}

async function deleteSlot(slotId: number) {
  await timeSlotStore.deleteSlot(slotId);
}

async function updateCategory(categoryId: number) {
  const category = categoryStore.categories.find((item) => item.id === categoryId);
  const draft = getCategoryDraft(categoryId);
  const name = draft.name.trim();
  if (!category || !name) {
    categoryNotice.value = '分类名称不能为空。';
    return;
  }

  const updated = await categoryStore.updateCategory(categoryId, {
    name,
    colorCode: draft.colorCode,
    displayOrder: category.displayOrder,
  });
  categoryDrafts[updated.id] = toCategoryDraft(updated);
  categoryNotice.value = '分类已更新。';
}

async function deleteCategory(categoryId: number, categoryName: string) {
  const confirmed = window.confirm(`确定删除分类“${categoryName}”吗？有历史记录的分类会被归档。`);
  if (!confirmed) return;

  const result = await categoryStore.deleteCategory(categoryId);
  delete categoryDrafts[categoryId];
  categoryNotice.value = result.action === 'DELETED'
    ? '分类已删除。'
    : `分类已归档，${result.affectedRecords} 条历史记录将继续显示该分类。`;
}

function getCategoryDraft(categoryId: number) {
  const category = categoryStore.categories.find((item) => item.id === categoryId);
  if (!categoryDrafts[categoryId] && category) {
    categoryDrafts[categoryId] = toCategoryDraft(category);
  }
  return categoryDrafts[categoryId] ?? { name: '', colorCode: '#3b82f6' };
}

function syncCategoryDrafts() {
  categoryStore.categories.forEach((category) => {
    categoryDrafts[category.id] = toCategoryDraft(category);
  });
}

function toCategoryDraft(category: CategoryDto): CategoryDraft {
  return {
    name: category.name,
    colorCode: category.colorCode,
  };
}

async function logout() {
  auth.logout();
  await router.push('/login');
}
</script>

<style scoped>
.toolbar { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 16px; margin-bottom: 18px; background: #fff; border-radius: 18px; padding: 18px; box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06); }
label, .category-form { display: flex; align-items: center; gap: 10px; color: #374151; }
input { border: 1px solid #d1d5db; border-radius: 10px; padding: 10px; }
button { border: 0; border-radius: 10px; background: #4f46e5; color: white; padding: 10px 14px; font-weight: 700; cursor: pointer; }
button.danger { background: #dc2626; }
.notice { color: #047857; background: #ecfdf5; border: 1px solid #a7f3d0; padding: 12px; border-radius: 12px; }
.error { color: #dc2626; background: #fef2f2; border: 1px solid #fecaca; padding: 12px; border-radius: 12px; }
.category-panel { margin-bottom: 18px; background: #fff; border-radius: 18px; padding: 18px; box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06); }
.panel-header { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 14px; }
h2 { margin: 0; font-size: 18px; color: #111827; }
.panel-header p { margin: 4px 0 0; color: #6b7280; }
.category-list { display: grid; gap: 10px; }
.category-row { display: grid; grid-template-columns: auto minmax(140px, 1fr) auto auto auto; align-items: center; gap: 10px; padding: 10px; border: 1px solid #e5e7eb; border-radius: 14px; }
.swatch { width: 18px; height: 18px; border-radius: 999px; box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.1); }
.empty-state { color: #6b7280; border: 1px dashed #d1d5db; border-radius: 14px; padding: 14px; }
@media (max-width: 720px) {
  label, .category-form { width: 100%; align-items: stretch; flex-direction: column; }
  .category-row { grid-template-columns: auto 1fr auto; }
  .category-row button { grid-column: span 3; }
}
</style>
