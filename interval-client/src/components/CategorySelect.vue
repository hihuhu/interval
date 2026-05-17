<template>
  <select name="categoryId" :value="modelValue ?? ''" :disabled="disabled" @change="onChange">
    <option value="" disabled>请选择分类</option>
    <option v-for="category in categories" :key="category.id" :value="category.id">
      {{ category.name }}
    </option>
  </select>
</template>

<script setup lang="ts">
import type { CategoryDto } from '@/types/category';

interface CategorySelectProps {
  categories: CategoryDto[];
  modelValue: number | null;
  disabled?: boolean;
}

withDefaults(defineProps<CategorySelectProps>(), { disabled: false });
const emit = defineEmits<{ (e: 'update:modelValue', value: number | null): void }>();

function onChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value;
  emit('update:modelValue', value ? Number(value) : null);
}
</script>

<style scoped>
select { width: 100%; border: 1px solid #d1d5db; border-radius: 10px; padding: 10px 12px; background: #fff; }
</style>
