<template>
  <section class="selection-panel" aria-labelledby="selection-editor-title">
    <div v-if="selectedSlotIndexes.length === 0" class="empty-panel">
      <div class="panel-topline">
        <div class="empty-icon">
          <MousePointer2 :size="18" />
        </div>
        <button
          type="button"
          class="ghost-action manage-button"
          data-testid="open-category-manager"
          @click="$emit('manageCategories')"
        >
          <Settings2 :size="15" />
          管理分类
        </button>
      </div>
      <p class="eyebrow">选择时间块</p>
      <h2 id="selection-editor-title">先选一个时间块</h2>
      <p>选择后在这里设置分类和备注。</p>
    </div>

    <form v-else class="selection-form" @submit.prevent="submit">
      <header>
        <div>
          <p class="eyebrow">当前选择</p>
          <h2 id="selection-editor-title">{{ selectedSlotIndexes.length }} 个时间块</h2>
        </div>
        <div class="header-actions">
          <button
            type="button"
            class="ghost-action manage-button"
            data-testid="open-category-manager"
            @click="$emit('manageCategories')"
          >
            <Settings2 :size="15" />
            管理分类
          </button>
          <button type="button" class="ghost-action icon-button" aria-label="取消选择" @click="$emit('cancel')">
            <X :size="16" />
          </button>
        </div>
      </header>

      <div class="summary-grid">
        <div>
          <span>总时长</span>
          <strong>{{ durationText }}</strong>
        </div>
        <div>
          <span>时间范围</span>
          <strong>{{ rangeText }}</strong>
        </div>
      </div>

      <div v-if="editorState.hasRecordedSlot" class="overlap-warning">
        <AlertTriangle :size="15" />
        选中的 {{ editorState.overlapCount }} 个时间块已有记录，保存时会覆盖分类。
      </div>

      <button
        v-if="recordedSelectedSlotIndexes.length > 0"
        type="button"
        class="danger-button"
        data-testid="erase-selected"
        @click="$emit('erase', recordedSelectedSlotIndexes)"
      >
        <Trash2 :size="15" />
        擦除已登记数据
      </button>

      <div class="category-field">
        <span class="field-label">分类</span>
        <div class="category-list" role="radiogroup" aria-label="选择分类">
          <button
            v-for="category in categories"
            :key="category.id"
            type="button"
            class="category-option"
            :class="{ active: selectedCategoryId === category.id }"
            :style="{ '--category-color': category.colorCode }"
            :disabled="saving"
            :aria-checked="selectedCategoryId === category.id"
            role="radio"
            :data-testid="`category-option-${category.id}`"
            @click="selectedCategoryId = category.id"
          >
            <span class="category-dot" :style="{ backgroundColor: category.colorCode }"></span>
            <span class="category-name">{{ category.name }}</span>
            <span
              v-if="selectedCategoryId === category.id"
              class="selected-category-check"
              data-testid="selected-category-check"
              aria-hidden="true"
            >
              <Check :size="13" :stroke-width="3" />
            </span>
          </button>
        </div>
      </div>
      <p v-if="editorState.categoryMode === 'mixed'" class="field-hint">
        当前选择包含多个分类，请重新指定一个统一分类。
      </p>

      <label>
        <span>备注</span>
        <textarea
          name="note"
          v-model="note"
          :disabled="saving"
          rows="3"
          placeholder="这段时间做了什么..."
          @input="noteTouched = true"
        />
      </label>
      <p v-if="editorState.noteMode === 'mixed'" class="field-hint">
        当前选择包含不同备注；不修改这里会保留原备注。
      </p>

      <button type="submit" class="primary-action submit-button" :disabled="saving || selectedCategoryId === null">
        <Save :size="16" />
        {{ saving ? '保存中...' : editorState.hasRecordedSlot ? '确认覆盖' : '保存记录' }}
      </button>
    </form>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { AlertTriangle, Check, MousePointer2, Save, Settings2, Trash2, X } from '@lucide/vue';
import { deriveSelectionEditorState } from '@/composables/useSelectionEditorState';
import { slotIndexToRange } from '@/composables/useTimeSlots';
import type { CategoryDto } from '@/types/category';
import type { TimeSlotDto } from '@/types/timeSlot';

interface SelectionEditorPanelProps {
  selectedSlotIndexes: number[];
  slots: TimeSlotDto[];
  categories: CategoryDto[];
  saving?: boolean;
}

const props = withDefaults(defineProps<SelectionEditorPanelProps>(), { saving: false });
const emit = defineEmits<{
  (e: 'save', payload: { slotIndexes: number[]; categoryId: number; note: string | null; noteTouched: boolean }): void;
  (e: 'erase', slotIndexes: number[]): void;
  (e: 'cancel'): void;
  (e: 'manageCategories'): void;
}>();

const selectedCategoryId = ref<number | null>(null);
const note = ref('');
const noteTouched = ref(false);
const slotsByIndex = computed(() => new Map(props.slots.map((slot) => [slot.slotIndex, slot])));
const editorState = computed(() => deriveSelectionEditorState(props.selectedSlotIndexes, props.slots));
const recordedSelectedSlotIndexes = computed(() => props.selectedSlotIndexes.filter((slotIndex) => slotsByIndex.value.has(slotIndex)));
const durationText = computed(() => `${props.selectedSlotIndexes.length * 15} 分钟`);
const rangeText = computed(() => {
  if (props.selectedSlotIndexes.length === 0) return '';
  const sorted = [...props.selectedSlotIndexes].sort((a, b) => a - b);
  return sorted.length === 1
    ? slotIndexToRange(sorted[0])
    : `${slotIndexToRange(sorted[0])} 至 ${slotIndexToRange(sorted[sorted.length - 1])}`;
});

watch(
  () => [props.selectedSlotIndexes.join(','), props.slots] as const,
  () => {
    selectedCategoryId.value = editorState.value.selectedCategoryId;
    note.value = editorState.value.displayedNote;
    noteTouched.value = false;
  },
  { immediate: true },
);

function submit() {
  if (selectedCategoryId.value === null) return;
  emit('save', {
    slotIndexes: props.selectedSlotIndexes,
    categoryId: selectedCategoryId.value,
    note: noteTouched.value ? note.value.trim() || null : null,
    noteTouched: noteTouched.value,
  });
}
</script>

<style scoped>
.selection-panel {
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 34px rgba(15, 23, 42, 0.06);
  padding: 18px;
}

.empty-panel {
  min-height: 210px;
  display: grid;
  align-content: center;
  gap: 10px;
  color: #64748b;
}

.panel-topline,
.header-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.empty-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: rgba(241, 245, 249, 0.9);
  color: #64748b;
}

.empty-panel h2,
.selection-form h2 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
}

.empty-panel p {
  margin: 0;
  line-height: 1.7;
}

.eyebrow {
  margin: 0;
  color: #64748b;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.selection-form {
  display: grid;
  gap: 13px;
}

header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.icon-button {
  width: 38px;
  height: 38px;
  padding: 0;
}

.manage-button {
  min-height: 38px;
  padding: 0 11px;
  font-size: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.summary-grid div {
  border: 1px solid rgba(226, 232, 240, 0.85);
  border-radius: 14px;
  padding: 12px;
  background: #f8fafc;
}

.summary-grid span,
label span,
.field-label {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  margin-bottom: 6px;
}

.summary-grid strong {
  color: #0f172a;
  font-size: 14px;
}

textarea {
  width: 100%;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  padding: 11px 12px;
  background: #ffffff;
  color: #0f172a;
}

.category-field {
  display: grid;
  gap: 8px;
}

.category-list {
  display: grid;
  gap: 6px;
  max-height: 168px;
  overflow-y: auto;
  padding-right: 2px;
}

.category-option {
  --category-color: #6366f1;
  min-height: 42px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(226, 232, 240, 0.68);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.54);
  padding: 0 12px;
  color: #334155;
  cursor: pointer;
  text-align: left;
  transition: background .15s ease, border-color .15s ease, box-shadow .15s ease, color .15s ease;
}

.category-option:hover:not(:disabled) {
  border-color: rgba(148, 163, 184, 0.48);
  background: #f8fafc;
}

.category-option.active {
  border-color: var(--category-color);
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--category-color) 16%, white) 0 4px, transparent 4px),
    #eef2ff;
  box-shadow:
    0 0 0 2px color-mix(in srgb, var(--category-color) 18%, transparent),
    0 0 0 1px rgba(255, 255, 255, 0.78) inset;
  color: #111827;
}

.category-option.active:hover:not(:disabled) {
  border-color: var(--category-color);
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--category-color) 20%, white) 0 4px, transparent 4px),
    #e0e7ff;
}

.category-option:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.category-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  flex: 0 0 auto;
}

.category-name {
  min-width: 0;
  flex: 1;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-option.active .category-name {
  color: #111827;
  font-weight: 900;
}

.selected-category-check {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 999px;
  background: var(--category-color);
  color: #ffffff;
}

textarea {
  resize: vertical;
}

.field-hint {
  margin: -8px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.overlap-warning,
.danger-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 14px;
  padding: 10px 12px;
  font-size: 12px;
}

.overlap-warning {
  border: 1px solid #fed7aa;
  background: #fff7ed;
  color: #c2410c;
}

.danger-button {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #dc2626;
  cursor: pointer;
  font-weight: 800;
}

.submit-button {
  min-height: 46px;
}

@media (max-width: 720px) {
  header,
  .panel-topline {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions,
  .manage-button {
    width: 100%;
  }
}
</style>
