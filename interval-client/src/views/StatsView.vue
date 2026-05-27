<template>
  <AppLayout :username="auth.username ?? 'User'" active-route="stats" @logout="logout">
    <section class="stats-hero">
      <div class="surface-card hero-main">
        <div class="hero-eyebrow"><span></span>CATEGORY DURATION STATS</div>
        <h1>分类耗时统计</h1>
        <p>
          按 15 分钟时间格聚合，快速看清每个分类花了多久、占比多少，以及哪些历史分类仍在贡献统计结果。
        </p>
      </div>

      <div class="surface-card hero-aside">
        <div class="range-tabs" aria-label="统计范围">
          <button
            v-for="option in rangeOptions"
            :key="option.key"
            type="button"
            :class="{ active: activeRange === option.key }"
            :data-testid="`range-${option.key}`"
            @click="selectQuickRange(option.key)"
          >
            {{ option.label }}
          </button>
        </div>

        <form class="custom-range" @submit.prevent="applyCustomRange">
          <label>
            <span>开始</span>
            <input v-model="customStartDate" type="date" />
          </label>
          <label>
            <span>结束</span>
            <input v-model="customEndDate" type="date" />
          </label>
          <button type="submit" class="ghost-action">应用</button>
        </form>

        <div class="insight">
          <strong>本次范围洞察</strong>
          <p>{{ insightText }}</p>
        </div>
      </div>
    </section>

    <p v-if="statsStore.errorMessage" class="error">{{ statsStore.errorMessage }}</p>

    <section class="surface-card summary-card">
      <div class="summary-top">
        <div class="range-copy">
          <small>当前统计范围</small>
          <strong>{{ rangeLabel }}</strong>
          <span>{{ summary?.startDate ?? startDate }} 至 {{ summary?.endDate ?? endDate }} · 共 {{ selectedDayCount }} 天</span>
        </div>
        <p>重点聚焦“记录占比 + 分类结构 + 选中分类详情”，方便快速验证时间投入是否符合预期。</p>
      </div>

      <div class="metric-grid">
        <div class="metric-card recorded">
          <small>已记录</small>
          <strong>{{ durationText(summary?.totalRecordedMinutes ?? 0) }}</strong>
          <span>真实投入的可追踪时长</span>
        </div>
        <div class="metric-card slots">
          <small>时间块</small>
          <strong>{{ summary?.totalSlotCount ?? 0 }} 格</strong>
          <span>所有已登记的 15 分钟块</span>
        </div>
        <div class="metric-card rate">
          <small>记录率</small>
          <strong>{{ recordedRate }}%</strong>
          <span>范围内总时长的覆盖情况</span>
        </div>
        <div class="metric-card blank">
          <small>未记录</small>
          <strong>{{ durationText(summary?.unrecordedMinutes ?? 0) }}</strong>
          <span>仍可继续补记的时间</span>
        </div>
      </div>
    </section>

    <section class="stats-layout">
      <div class="surface-card category-panel">
        <div class="panel-title">
          <div>
            <h2>分类占比</h2>
            <p>点击任一分类，右侧会同步展示细节信息与趋势占位。</p>
          </div>
          <button type="button" class="ghost-action" @click="refresh">刷新</button>
        </div>

        <div v-if="statsStore.loading && !summary" class="loading-list">
          <div v-for="item in 4" :key="item"></div>
        </div>

        <div v-else-if="!summary || summary.categories.length === 0" class="empty-state">
          <Clock3 :size="34" />
          <h3>这个时间范围还没有记录</h3>
          <p>回到时间格页面登记后，这里会自动生成分类耗时统计。</p>
        </div>

        <div v-else class="category-list">
          <button
            v-for="category in summary.categories"
            :key="category.categoryId"
            type="button"
            class="category-row"
            :class="{ selected: selectedCategoryId === category.categoryId }"
            :data-testid="`category-row-${category.categoryId}`"
            @click="selectedCategoryId = category.categoryId"
          >
            <div class="row-top">
              <div class="name-block">
                <span class="swatch" :style="{ background: category.categoryColor }"></span>
                <span>
                  <b>{{ category.categoryName }}</b>
                  <i v-if="category.categoryStatus === 'ARCHIVED'">已归档</i>
                  <small>{{ category.slotCount }} 个时间块 · {{ category.categoryStatus === 'ARCHIVED' ? '历史分类' : '当前分类' }}</small>
                </span>
              </div>
              <div class="amount">
                <b>{{ durationText(category.durationMinutes) }}</b>
                <small>{{ percentageText(category.percentage) }}%</small>
              </div>
            </div>
            <div class="progress">
              <span :style="{ width: `${category.percentage}%`, background: category.categoryColor }"></span>
            </div>
          </button>
        </div>
      </div>

      <aside class="side-stack">
        <div class="surface-card side-panel">
          <h2>结构概览</h2>
          <p>已记录时间在不同分类之间的分布情况。</p>
          <div class="donut-wrap">
            <div class="donut" data-testid="donut-chart" :style="donutStyle"></div>
            <div class="donut-center">
              <strong>{{ durationText(summary?.totalRecordedMinutes ?? 0) }}</strong>
              <span>总记录时长</span>
            </div>
          </div>
          <div class="legend">
            <span v-for="category in legendCategories" :key="category.categoryId">
              <i :style="{ background: category.categoryColor }"></i>
              {{ category.categoryName }}
            </span>
          </div>
        </div>

        <div class="surface-card side-panel">
          <h2>分类详情</h2>
          <p>当前选中分类的统计摘要。</p>
          <div v-if="selectedCategory" class="detail-card">
            <div class="detail-head">
              <span class="swatch" :style="{ background: selectedCategory.categoryColor }"></span>
              <div>
                <b>{{ selectedCategory.categoryName }}</b>
                <small>{{ selectedCategory.categoryStatus === 'ARCHIVED' ? '历史分类，仍参与统计' : '当前可用分类' }}</small>
              </div>
            </div>
            <div class="detail-grid">
              <div><small>耗时</small><b>{{ durationText(selectedCategory.durationMinutes) }}</b></div>
              <div><small>占比</small><b>{{ percentageText(selectedCategory.percentage) }}%</b></div>
              <div><small>时间块</small><b>{{ selectedCategory.slotCount }} 格</b></div>
              <div><small>平均每天</small><b>{{ durationText(averageMinutesPerDay) }}</b></div>
            </div>
          </div>
          <div v-else class="detail-card muted-detail">暂无可展示分类</div>
        </div>

        <div class="surface-card side-panel">
          <h2>时段趋势</h2>
          <p>第一版先保留趋势占位，后续接入 daily stats 后可替换为真实柱状图。</p>
          <div class="trend">
            <div v-for="item in trendItems" :key="item.label" class="trend-col">
              <span>{{ Math.round(item.minutes / 60) }}h</span>
              <i :style="{ height: `${trendHeight(item.minutes)}px` }"></i>
              <small>{{ item.label }}</small>
            </div>
          </div>
        </div>

        <div class="next-panel">
          <small>下一步可扩展</small>
          <h3>日报 / 周报 / 月报</h3>
          <p>当前版本先完成分类耗时闭环；后续可以继续扩展活动名称排行、导出和跨范围对比。</p>
        </div>
      </aside>
    </section>
  </AppLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Clock3 } from '@lucide/vue';
import AppLayout from '@/components/AppLayout.vue';
import { useAuthStore } from '@/stores/useAuthStore';
import { useStatsStore } from '@/stores/useStatsStore';
import type { CategoryDurationStat, StatsRangeKey } from '@/types/stats';

const router = useRouter();
const auth = useAuthStore();
const statsStore = useStatsStore();

const activeRange = ref<StatsRangeKey>('week');
const today = new Date();
const todayIso = toIsoDate(today);
const startDate = ref(toIsoDate(startOfWeek(today)));
const endDate = ref(toIsoDate(endOfWeek(today)));
const customStartDate = ref(startDate.value);
const customEndDate = ref(endDate.value);
const selectedCategoryId = ref<number | null>(null);

const rangeOptions: Array<{ key: StatsRangeKey; label: string }> = [
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'month', label: '本月' },
  { key: 'custom', label: '自定义' },
];

const rangeLabels: Record<StatsRangeKey, string> = {
  today: '今日统计',
  week: '本周统计',
  month: '本月统计',
  custom: '自定义范围',
};

const summary = computed(() => statsStore.summary);
const selectedDayCount = computed(() => inclusiveDayCount(summary.value?.startDate ?? startDate.value, summary.value?.endDate ?? endDate.value));
const rangeLabel = computed(() => rangeLabels[activeRange.value]);
const recordedRate = computed(() => {
  const data = summary.value;
  if (!data || data.totalAvailableMinutes === 0) return '0.0';
  return ((data.totalRecordedMinutes / data.totalAvailableMinutes) * 100).toFixed(1);
});
const selectedCategory = computed(() => {
  const categories = summary.value?.categories ?? [];
  return categories.find((category) => category.categoryId === selectedCategoryId.value) ?? categories[0] ?? null;
});
const averageMinutesPerDay = computed(() => {
  if (!selectedCategory.value) return 0;
  return Math.round(selectedCategory.value.durationMinutes / Math.max(selectedDayCount.value, 1));
});
const legendCategories = computed(() => (summary.value?.categories ?? []).slice(0, 6));
const donutStyle = computed(() => ({ '--segments': buildDonutSegments(summary.value?.categories ?? []) }));
const insightText = computed(() => {
  const topCategory = summary.value?.categories[0];
  if (!topCategory) return '当前范围暂无足够数据。完成时间格登记后，这里会显示投入最多的分类。';
  return `${topCategory.categoryName} 是当前投入最多的分类，占比 ${percentageText(topCategory.percentage)}%，总计 ${durationText(topCategory.durationMinutes)}。`;
});
const trendItems = computed(() => buildTrendItems(activeRange.value, summary.value?.totalRecordedMinutes ?? 0));
const trendMax = computed(() => Math.max(...trendItems.value.map((item) => item.minutes), 0));

watch(summary, (nextSummary) => {
  const firstCategory = nextSummary?.categories[0];
  selectedCategoryId.value = firstCategory?.categoryId ?? null;
});

onMounted(() => {
  void fetchStats();
});

async function selectQuickRange(range: StatsRangeKey) {
  activeRange.value = range;
  const selected = calculateRange(range);
  startDate.value = selected.startDate;
  endDate.value = selected.endDate;
  customStartDate.value = selected.startDate;
  customEndDate.value = selected.endDate;
  await fetchStats();
}

async function applyCustomRange() {
  activeRange.value = 'custom';
  startDate.value = customStartDate.value;
  endDate.value = customEndDate.value;
  await fetchStats();
}

async function refresh() {
  await fetchStats();
}

async function fetchStats() {
  try {
    await statsStore.fetchCategoryDurations(startDate.value, endDate.value, activeRange.value);
  } catch {
    // The store keeps the previous summary and exposes the error message for the page.
  }
}

function calculateRange(range: StatsRangeKey) {
  if (range === 'today') {
    return { startDate: todayIso, endDate: todayIso };
  }
  if (range === 'month') {
    return { startDate: toIsoDate(startOfMonth(today)), endDate: toIsoDate(endOfMonth(today)) };
  }
  if (range === 'custom') {
    return { startDate: customStartDate.value, endDate: customEndDate.value };
  }
  return { startDate: toIsoDate(startOfWeek(today)), endDate: toIsoDate(endOfWeek(today)) };
}

function durationText(minutes: number) {
  if (minutes < 60) return `${minutes} 分钟`;
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return rest ? `${hours} 小时 ${rest} 分钟` : `${hours} 小时`;
}

function percentageText(value: number) {
  return value.toFixed(2);
}

function buildDonutSegments(categories: CategoryDurationStat[]) {
  if (categories.length === 0) return '#f1f5f9 0deg 360deg';
  let offset = 0;
  const segments = categories.map((category) => {
    const start = offset;
    const end = offset + (category.percentage / 100) * 360;
    offset = end;
    return `${category.categoryColor} ${start}deg ${end}deg`;
  });
  if (offset < 360) segments.push(`#f1f5f9 ${offset}deg 360deg`);
  return segments.join(',');
}

function buildTrendItems(range: StatsRangeKey, totalMinutes: number) {
  const units = range === 'today'
    ? ['上午', '中午', '下午', '晚上']
    : range === 'month'
      ? ['W1', 'W2', 'W3', 'W4', 'W5']
      : ['一', '二', '三', '四', '五', '六', '日'];
  const fallback = Math.max(Math.round(totalMinutes / Math.max(units.length, 1)), 30);
  return units.map((label, index) => ({
    label,
    minutes: Math.max(Math.round(fallback * (0.72 + ((index % 3) * 0.16))), totalMinutes ? 15 : 0),
  }));
}

function trendHeight(minutes: number) {
  if (!trendMax.value) return 18;
  return Math.round((minutes / trendMax.value) * 110) + 18;
}

function inclusiveDayCount(start: string, end: string) {
  const startTime = new Date(`${start}T00:00:00`).getTime();
  const endTime = new Date(`${end}T00:00:00`).getTime();
  return Math.max(Math.round((endTime - startTime) / 86400000) + 1, 1);
}

function startOfWeek(value: Date) {
  const date = cloneDate(value);
  const day = date.getDay() || 7;
  date.setDate(date.getDate() - day + 1);
  return date;
}

function endOfWeek(value: Date) {
  const date = startOfWeek(value);
  date.setDate(date.getDate() + 6);
  return date;
}

function startOfMonth(value: Date) {
  return new Date(value.getFullYear(), value.getMonth(), 1);
}

function endOfMonth(value: Date) {
  return new Date(value.getFullYear(), value.getMonth() + 1, 0);
}

function cloneDate(value: Date) {
  return new Date(value.getFullYear(), value.getMonth(), value.getDate());
}

function toIsoDate(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

async function logout() {
  auth.logout();
  await router.push('/login');
}
</script>

<style scoped>
.stats-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 18px;
  margin-bottom: 22px;
}

.hero-main {
  position: relative;
  overflow: hidden;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(165, 180, 252, 0.24), transparent 28%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(238, 242, 255, 0.88));
}

.hero-main::after {
  content: '';
  position: absolute;
  right: -70px;
  bottom: -110px;
  width: 220px;
  height: 220px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.18), transparent 68%);
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid rgba(129, 140, 248, 0.18);
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.72);
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
  margin: 18px 0 12px;
  color: #0f172a;
  font-size: clamp(34px, 5vw, 44px);
  line-height: 1.04;
  letter-spacing: 0;
}

.hero-main p,
.side-panel p,
.panel-title p,
.summary-top p,
.empty-state p,
.next-panel p,
.insight p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.75;
}

.hero-main p {
  max-width: 650px;
}

.hero-aside {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 22px;
}

.range-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.range-tabs button {
  min-height: 42px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  color: #64748b;
  padding: 0 16px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.18s ease;
}

.range-tabs button:hover {
  border-color: rgba(129, 140, 248, 0.35);
  color: #4338ca;
}

.range-tabs .active {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: white;
  border-color: transparent;
  box-shadow: 0 16px 28px rgba(79, 70, 229, 0.22);
}

.custom-range {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 10px;
  align-items: end;
}

.custom-range label span {
  display: block;
  margin-bottom: 6px;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 900;
}

.custom-range input {
  width: 100%;
  min-height: 40px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.86);
  color: #0f172a;
  padding: 8px 10px;
}

.custom-range button {
  min-height: 40px;
  padding: 0 14px;
}

.insight {
  padding: 16px 18px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.10), rgba(56, 189, 248, 0.08));
  border: 1px solid rgba(199, 210, 254, 0.75);
}

.insight strong {
  display: block;
  margin-bottom: 6px;
  color: #4338ca;
  font-size: 13px;
}

.summary-card {
  margin-bottom: 22px;
  padding: 20px;
}

.summary-top {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.summary-top p {
  max-width: 420px;
}

.range-copy small,
.metric-card small {
  display: block;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.06em;
  margin-bottom: 8px;
}

.range-copy strong {
  display: block;
  color: #0f172a;
  font-size: 20px;
}

.range-copy span {
  display: block;
  margin-top: 5px;
  color: #94a3b8;
  font-size: 13px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.76);
  padding: 18px;
}

.metric-card strong {
  display: block;
  color: currentColor;
  font-size: 26px;
  line-height: 1.1;
}

.metric-card span {
  display: block;
  margin-top: 6px;
  color: rgba(15, 23, 42, 0.56);
  font-size: 12px;
  line-height: 1.5;
}

.recorded {
  background: linear-gradient(180deg, #eef2ff, #e0e7ff);
  color: #4338ca;
}

.slots {
  background: linear-gradient(180deg, #ecfdf5, #d1fae5);
  color: #047857;
}

.rate {
  background: linear-gradient(180deg, #f0f9ff, #dbeafe);
  color: #0369a1;
}

.blank {
  background: linear-gradient(180deg, #f8fafc, #f1f5f9);
  color: #334155;
}

.stats-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  align-items: start;
  gap: 22px;
}

.category-panel,
.side-panel {
  padding: 22px;
}

.panel-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

h2 {
  margin: 0 0 6px;
  color: #0f172a;
  font-size: 18px;
}

.panel-title button {
  min-height: 40px;
  padding: 0 14px;
  font-size: 12px;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-row {
  width: 100%;
  border: 1px solid rgba(241, 245, 249, 0.96);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.84);
  padding: 16px;
  cursor: pointer;
  text-align: left;
  transition: all 0.18s ease;
}

.category-row:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.05);
  transform: translateY(-1px);
}

.category-row.selected {
  border-color: #c7d2fe;
  background: linear-gradient(180deg, #eef2ff, #f8faff);
  box-shadow: 0 18px 32px rgba(79, 70, 229, 0.10);
}

.row-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.name-block {
  display: flex;
  align-items: center;
  gap: 12px;
}

.swatch {
  width: 13px;
  height: 13px;
  border-radius: 999px;
  flex: 0 0 auto;
}

.name-block b,
.detail-head b,
.amount b {
  display: inline-block;
  color: #0f172a;
  font-size: 14px;
}

.name-block i {
  display: inline-block;
  margin-left: 7px;
  padding: 3px 7px;
  border: 1px solid #fde68a;
  border-radius: 999px;
  background: #fffbeb;
  color: #b45309;
  font-size: 10px;
  font-style: normal;
  font-weight: 800;
}

.name-block small,
.amount small,
.detail-head small {
  display: block;
  margin-top: 4px;
  color: #94a3b8;
  font-size: 11px;
}

.amount {
  text-align: right;
}

.progress {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #f1f5f9;
}

.progress span {
  display: block;
  height: 100%;
  border-radius: 999px;
}

.empty-state {
  min-height: 300px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  border: 1px dashed #cbd5e1;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(248, 250, 252, 0.94));
  color: #94a3b8;
  text-align: center;
  padding: 56px 20px;
}

.empty-state h3 {
  margin: 0;
  color: #0f172a;
}

.loading-list {
  display: grid;
  gap: 12px;
}

.loading-list div {
  height: 86px;
  border-radius: 22px;
  background: linear-gradient(90deg, #f8fafc, #eef2ff, #f8fafc);
  background-size: 200% 100%;
  animation: shimmer 1.1s linear infinite;
}

.side-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.donut-wrap {
  width: 210px;
  height: 210px;
  position: relative;
  display: grid;
  place-items: center;
  margin: 18px auto 16px;
}

.donut {
  width: 210px;
  height: 210px;
  border-radius: 50%;
  background: conic-gradient(var(--segments));
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.2);
}

.donut::after {
  content: '';
  position: absolute;
  inset: 26px;
  border-radius: 50%;
  background: white;
  box-shadow: inset 0 0 0 1px #f1f5f9;
}

.donut-center {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}

.donut-center strong {
  color: #0f172a;
  font-size: 22px;
}

.donut-center span {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 11px;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  background: #f8fafc;
  color: #64748b;
  padding: 8px 10px;
  font-size: 11px;
}

.legend i {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.detail-card {
  border: 1px solid #f1f5f9;
  border-radius: 22px;
  padding: 16px;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-head .swatch {
  width: 16px;
  height: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.detail-grid div {
  border-radius: 16px;
  background: #f8fafc;
  padding: 12px;
}

.detail-grid small {
  display: block;
  color: #94a3b8;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.04em;
}

.detail-grid b {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 13px;
}

.muted-detail {
  color: #94a3b8;
}

.trend {
  height: 170px;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding-top: 10px;
}

.trend-col {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 8px;
}

.trend-col span {
  color: #64748b;
  font-size: 10px;
}

.trend-col i {
  width: 100%;
  max-width: 30px;
  border-radius: 999px 999px 12px 12px;
  background: linear-gradient(180deg, #818cf8 0%, #4f46e5 100%);
  box-shadow: 0 10px 22px rgba(79, 70, 229, 0.18);
}

.trend-col small {
  color: #94a3b8;
  font-size: 11px;
}

.next-panel {
  border-radius: 24px;
  background: linear-gradient(135deg, #4f46e5, #4338ca);
  color: white;
  box-shadow: 0 20px 34px rgba(79, 70, 229, 0.22);
  padding: 20px;
}

.next-panel small {
  color: #c7d2fe;
  font-weight: 900;
  letter-spacing: 0.05em;
}

.next-panel h3 {
  margin: 10px 0 8px;
}

.next-panel p {
  color: #e0e7ff;
  font-size: 12px;
}

.error {
  border: 1px solid #fecaca;
  border-radius: 14px;
  background: #fef2f2;
  color: #b91c1c;
  padding: 12px 14px;
  font-size: 13px;
  margin: 0 0 16px;
}

@keyframes shimmer {
  from {
    background-position: 200% 0;
  }
  to {
    background-position: -200% 0;
  }
}

@media (max-width: 1080px) {
  .stats-hero,
  .stats-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 780px) {
  .summary-top {
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: 1fr 1fr;
  }

  .custom-range {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .hero-main,
  .hero-aside,
  .summary-card,
  .category-panel,
  .side-panel {
    padding: 18px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .row-top,
  .panel-title {
    flex-direction: column;
  }

  .amount {
    text-align: left;
  }
}
</style>
