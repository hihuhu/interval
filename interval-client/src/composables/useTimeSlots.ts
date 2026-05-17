export function slotIndexToRange(slotIndex: number): string {
  const startMinutes = slotIndex * 15;
  const endMinutes = startMinutes + 15;
  return `${formatMinutes(startMinutes)}-${formatMinutes(endMinutes)}`;
}

export function slotIndexRange(startSlotIndex: number, endSlotIndex: number): number[] {
  const start = Math.min(startSlotIndex, endSlotIndex);
  const end = Math.max(startSlotIndex, endSlotIndex);
  return Array.from({ length: end - start + 1 }, (_, index) => start + index);
}

export function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10);
}

function formatMinutes(totalMinutes: number): string {
  if (totalMinutes === 1440) {
    return '24:00';
  }
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`;
}
