<template>
  <section class="category-panel" :class="{ embedded }" aria-labelledby="category-manager-title">
    <header v-if="!embedded">
      <div>
        <p class="eyebrow">分类管理</p>
        <h2 id="category-manager-title">维护时间分类</h2>
      </div>
      <button type="button" class="ghost-action icon-button" aria-label="聚焦新建分类" @click="focusNewCategory">
        <Plus :size="16" />
      </button>
    </header>

    <form class="create-form" data-testid="create-category" @submit.prevent="createCategory">
      <input ref="newCategoryInput" name="newCategoryName" v-model="newCategoryName" placeholder="新分类名称" />
      <input name="newCategoryColor" v-model="newCategoryColor" type="color" aria-label="新分类颜色" />
      <button type="submit" class="primary-action">
        <Plus :size="15" />
        新建
      </button>
    </form>

    <div v-if="categories.length === 0" class="empty-state">暂无分类，请先新建一个分类。</div>
    <div v-else class="category-list">
      <form
        v-for="category in categories"
        :key="category.id"
        class="category-row"
        :data-testid="`update-category-${category.id}`"
        @submit.prevent="updateCategory(category)"
      >
        <span class="swatch" :style="{ backgroundColor: getDraft(category).colorCode }"></span>
        <input
          :name="`categoryName-${category.id}`"
          v-model="getDraft(category).name"
          :aria-label="`${category.name} 分类名称`"
        />
        <input
          :name="`categoryColor-${category.id}`"
          v-model="getDraft(category).colorCode"
          type="color"
          :aria-label="`${category.name} 分类颜色`"
        />
        <button type="submit" class="ghost-action save-button" aria-label="保存分类">
          <Save :size="15" />
        </button>
        <button
          type="button"
          class="danger-button"
          :data-testid="`delete-category-${category.id}`"
          aria-label="删除分类"
          @click="$emit('delete', category.id, category.name)"
        >
          <Trash2 :size="15" />
        </button>
      </form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { Plus, Save, Trash2 } from '@lucide/vue';
import type { CategoryDto, CreateCategoryRequest, UpdateCategoryRequest } from '@/types/category';

interface CategoryDraft {
  name: string;
  colorCode: string;
}

interface CategoryManagerPanelProps {
  categories: CategoryDto[];
  embedded?: boolean;
}

const props = withDefaults(defineProps<CategoryManagerPanelProps>(), { embedded: false });
const emit = defineEmits<{
  (e: 'create', payload: CreateCategoryRequest): void;
  (e: 'update', categoryId: number, payload: UpdateCategoryRequest): void;
  (e: 'delete', categoryId: number, categoryName: string): void;
}>();

const newCategoryName = ref('');
const newCategoryColor = ref('#a5b4fc');
const newCategoryInput = ref<HTMLInputElement | null>(null);
const drafts = reactive<Record<number, CategoryDraft>>({});

watch(
  () => props.categories,
  () => {
    props.categories.forEach((category) => {
      drafts[category.id] = {
        name: category.name,
        colorCode: category.colorCode,
      };
    });
  },
  { immediate: true },
);

function focusNewCategory() {
  newCategoryInput.value?.focus();
}

function createCategory() {
  const name = newCategoryName.value.trim();
  if (!name) return;
  emit('create', { name, colorCode: newCategoryColor.value });
  newCategoryName.value = '';
}

function updateCategory(category: CategoryDto) {
  const draft = getDraft(category);
  const name = draft.name.trim();
  if (!name) return;
  emit('update', category.id, {
    name,
    colorCode: draft.colorCode,
    displayOrder: category.displayOrder,
  });
}

function getDraft(category: CategoryDto) {
  if (!drafts[category.id]) {
    drafts[category.id] = {
      name: category.name,
      colorCode: category.colorCode,
    };
  }
  return drafts[category.id];
}
</script>

<style scoped>
.category-panel {
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 14px 38px rgba(15, 23, 42, 0.06);
  padding: 20px;
}

.category-panel.embedded {
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  padding: 0;
}

header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

h2 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}

.eyebrow {
  margin: 0 0 5px;
  color: #4338ca;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.icon-button,
.save-button,
.danger-button {
  width: 38px;
  height: 38px;
  padding: 0;
}

.create-form,
.category-row {
  display: grid;
  align-items: center;
  gap: 8px;
}

.create-form {
  grid-template-columns: minmax(0, 1fr) auto auto;
  margin-bottom: 12px;
}

.category-list {
  display: grid;
  gap: 8px;
}

.category-row {
  grid-template-columns: auto minmax(0, 1fr) auto auto auto;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 16px;
  padding: 10px;
  background: rgba(248, 250, 252, 0.75);
  transition: transform .16s ease, background .16s ease;
}

.category-row:hover {
  transform: translateX(2px);
  background: rgba(238, 242, 255, 0.56);
}

input {
  min-width: 0;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  padding: 9px 10px;
  background: rgba(255, 255, 255, 0.9);
  color: #0f172a;
}

input[type="color"] {
  width: 44px;
  height: 40px;
  padding: 4px;
}

.primary-action {
  min-height: 40px;
  padding: 0 12px;
  font-size: 12px;
}

.danger-button {
  display: inline-grid;
  place-items: center;
  border: 1px solid #fecaca;
  border-radius: 12px;
  background: #fef2f2;
  color: #dc2626;
  cursor: pointer;
}

.swatch {
  width: 16px;
  height: 16px;
  border-radius: 999px;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.12);
}

.empty-state {
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
  padding: 14px;
  color: #64748b;
}

@media (max-width: 720px) {
  .create-form,
  .category-row {
    grid-template-columns: 1fr;
  }

  .icon-button,
  .save-button,
  .danger-button {
    width: 100%;
  }
}
</style>
