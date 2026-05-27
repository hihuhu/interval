<template>
  <section class="selection-panel" aria-labelledby="selection-editor-title">
    <div v-if="selectedSlotIndexes.length === 0" class="empty-panel">
      <div class="empty-icon">
        <MousePointer2 :size="18" />
      </div>
      <p class="eyebrow">选择时间块</p>
      <h2 id="selection-editor-title">先选一个时间块</h2>
      <p>右侧面板会展示摘要、分类和备注编辑。拖拽可快速选择连续时间，点击可补选离散时间块。</p>
    </div>

    <form v-else class="selection-form" @submit.prevent="submit">
      <header>
        <div>
          <p class="eyebrow">当前选择中</p>
          <h2 id="selection-editor-title">{{ selectedSlotIndexes.length }} 个时间块</h2>
        </div>
        <button type="button" class="ghost-action icon-button" aria-label="取消选择" @click="$emit('cancel')">
          <X :size="16" />
        </button>
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
            :disabled="saving"
            :aria-checked="selectedCategoryId === category.id"
            role="radio"
            :data-testid="`category-option-${category.id}`"
            @click="selectedCategoryId = category.id"
          >
            <span class="category-dot" :style="{ backgroundColor: category.colorCode }"></span>
            <span class="category-name">{{ category.name }}</span>
          </button>
        </div>
      </div>
      <p class="field-hint">
        <span v-if="editorState.categoryMode === 'mixed'">当前选择包含多个分类，请重新指定一个统一分类。</span>
        <span v-else-if="editorState.categoryMode === 'single'">当前选择已自动回显同一分类。</span>
        <span v-else>当前选择还没有分类，保存时会按你新选的分类写入。</span>
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
      <p class="field-hint">
        <span v-if="editorState.noteMode === 'mixed'">当前选择包含不同备注；不修改这里会保留原备注。</span>
        <span v-else-if="editorState.noteMode === 'single'">已自动回显统一备注；修改后会批量覆盖。</span>
        <span v-else>留空可不写备注；主动清空并保存会移除备注。</span>
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
import { AlertTriangle, MousePointer2, Save, Trash2, X } from '@lucide/vue';
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
  border: 1px solid rgba(199, 210, 254, 0.7);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(238, 242, 255, 0.80));
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
  padding: 20px;
}

.empty-panel {
  min-height: 250px;
  display: grid;
  align-content: center;
  gap: 10px;
  color: #64748b;
}

.empty-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 15px;
  background: rgba(99, 102, 241, 0.10);
  color: #4f46e5;
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
  color: #4338ca;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.selection-form {
  display: grid;
  gap: 14px;
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.summary-grid div {
  border: 1px solid rgba(226, 232, 240, 0.85);
  border-radius: 16px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.72);
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
  border-radius: 14px;
  padding: 11px 12px;
  background: rgba(255, 255, 255, 0.9);
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
  min-height: 42px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid transparent;
  border-radius: 14px;
  background: transparent;
  padding: 0 12px;
  color: #334155;
  cursor: pointer;
  text-align: left;
  transition: background .15s ease, border-color .15s ease, box-shadow .15s ease, transform .15s ease;
}

.category-option:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.72);
}

.category-option.active {
  border-color: rgba(129, 140, 248, 0.42);
  background: rgba(238, 242, 255, 0.72);
  box-shadow: 0 0 0 1px rgba(199, 210, 254, 0.72) inset, 0 6px 14px rgba(79, 70, 229, 0.06);
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
</style>
