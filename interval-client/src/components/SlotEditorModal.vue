<template>
  <div v-if="open" class="backdrop" role="dialog" aria-modal="true">
    <form class="modal" @submit.prevent="submit">
      <header>
        <h2>{{ title }}</h2>
        <button type="button" @click="$emit('close')">关闭</button>
      </header>
      <p v-if="rangeText" class="range">{{ rangeText }}</p>
      <label>
        活动名称
        <input name="activityName" v-model="activityName" :disabled="saving" placeholder="例如：写代码" />
      </label>
      <label>
        分类
        <CategorySelect v-model="categoryId" :categories="categories" :disabled="saving" />
      </label>
      <button type="submit" class="primary" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import CategorySelect from '@/components/CategorySelect.vue';
import { slotIndexToRange } from '@/composables/useTimeSlots';
import type { CategoryDto } from '@/types/category';
import type { TimeSlotDto } from '@/types/timeSlot';

interface SlotEditorModalProps {
  open: boolean;
  slotIndex: number | null;
  slotIndexes?: number[];
  existingSlot?: TimeSlotDto;
  categories: CategoryDto[];
  saving?: boolean;
}

const props = withDefaults(defineProps<SlotEditorModalProps>(), { saving: false, slotIndexes: () => [] });
const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'submit', payload: { slotIndex: number; slotIndexes: number[]; activityName: string; categoryId: number }): void;
}>();

const activityName = ref('');
const categoryId = ref<number | null>(null);
const selectedSlotIndexes = computed(() => props.slotIndexes.length > 0 ? props.slotIndexes : props.slotIndex === null ? [] : [props.slotIndex]);
const title = computed(() => selectedSlotIndexes.value.length > 1 ? '批量记录时间格' : props.existingSlot ? '编辑时间格' : '记录时间格');
const rangeText = computed(() => {
  const indexes = selectedSlotIndexes.value;
  if (indexes.length === 0) return '';
  if (indexes.length === 1) return slotIndexToRange(indexes[0]);
  return `${slotIndexToRange(indexes[0])} 至 ${slotIndexToRange(indexes[indexes.length - 1])}（${indexes.length} 格）`;
});

watch(() => [props.open, props.existingSlot, props.slotIndex, props.slotIndexes] as const, () => {
  activityName.value = props.existingSlot?.activityName ?? '';
  categoryId.value = props.existingSlot?.categoryId ?? props.categories[0]?.id ?? null;
}, { immediate: true });

function submit() {
  const indexes = selectedSlotIndexes.value;
  if (indexes.length === 0) return;
  if (!activityName.value.trim() || categoryId.value === null) {
    window.alert('请填写活动名称并选择分类');
    return;
  }
  emit('submit', {
    slotIndex: indexes[0],
    slotIndexes: indexes,
    activityName: activityName.value.trim(),
    categoryId: categoryId.value,
  });
}
</script>

<style scoped>
.backdrop { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.38); display: grid; place-items: center; padding: 16px; }
.modal { width: min(420px, 100%); background: #fff; border-radius: 18px; padding: 22px; display: grid; gap: 16px; box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25); }
header { display: flex; justify-content: space-between; align-items: center; }
h2 { margin: 0; }
.range { margin: 0; color: #4f46e5; font-weight: 700; }
label { display: grid; gap: 6px; color: #374151; }
input { border: 1px solid #d1d5db; border-radius: 10px; padding: 10px 12px; }
button { cursor: pointer; border: 1px solid #d1d5db; border-radius: 10px; padding: 9px 12px; background: #fff; }
.primary { border: 0; background: #4f46e5; color: #fff; font-weight: 700; }
</style>
