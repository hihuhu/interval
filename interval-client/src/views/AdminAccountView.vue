<template>
  <AppLayout :username="auth.username ?? 'admin'" active-route="admin" account-type="ADMIN" @logout="logout">
    <section class="admin-page">
      <header class="topline">
        <div class="page-title">
          <h1>账号管理</h1>
          <p>查看用户、识别活跃度、重置密码、禁用或启用账号。</p>
        </div>
      </header>

      <section class="metrics" aria-label="账号仪表盘">
        <article class="metric">
          <div class="metric-top">
            <span>总用户</span>
            <Users :size="17" aria-hidden="true" />
          </div>
          <div class="metric-value">{{ admin.dashboard?.totalUsers ?? 0 }}</div>
          <p class="metric-note">不含管理员账号</p>
        </article>
        <article class="metric">
          <div class="metric-top">
            <span>启用账号</span>
            <CircleCheck :size="17" aria-hidden="true" />
          </div>
          <div class="metric-value">{{ admin.dashboard?.activeUsers ?? 0 }}</div>
          <p class="metric-note">可正常登录</p>
        </article>
        <article class="metric">
          <div class="metric-top">
            <span>禁用账号</span>
            <Ban :size="17" aria-hidden="true" />
          </div>
          <div class="metric-value">{{ admin.dashboard?.disabledUsers ?? 0 }}</div>
          <p class="metric-note">保留数据，不可登录</p>
        </article>
        <article class="metric">
          <div class="metric-top">
            <span>今日活跃</span>
            <Activity :size="17" aria-hidden="true" />
          </div>
          <div class="metric-value">{{ admin.dashboard?.activeToday ?? 0 }}</div>
          <p class="metric-note">今天登录或记录时间</p>
        </article>
        <article class="metric">
          <div class="metric-top">
            <span>近 7 日活跃</span>
            <Activity :size="17" aria-hidden="true" />
          </div>
          <div class="metric-value">{{ admin.dashboard?.activeLast7Days ?? 0 }}</div>
          <p class="metric-note">过去 7 天有行为</p>
        </article>
      </section>

      <section class="workbench">
        <article class="panel">
          <div class="panel-head">
            <div class="panel-title">
              <h2>用户列表</h2>
              <p>管理员账号不显示在普通用户列表中。</p>
            </div>
            <div class="tools">
              <input
                v-model="keyword"
                data-test="user-keyword"
                class="search"
                placeholder="搜索用户名"
                @input="applyFilters"
              />
              <select v-model="statusFilter" data-test="user-status" aria-label="状态筛选" @change="applyFilters">
                <option value="">全部状态</option>
                <option value="ACTIVE">启用</option>
                <option value="DISABLED">禁用</option>
              </select>
            </div>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>用户</th>
                  <th>状态</th>
                  <th>注册时间</th>
                  <th>最近登录</th>
                  <th>最近活跃</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="user in admin.users"
                  :key="user.userId"
                  :data-test="`user-row-${user.userId}`"
                  :class="{ 'disabled-row': user.status === 'DISABLED', selected: admin.selectedUserId === user.userId }"
                  @click="admin.selectedUserId = user.userId"
                >
                  <td>
                    <div class="user-cell">
                      <div class="mini-avatar">{{ user.username.slice(0, 1).toUpperCase() }}</div>
                      <strong>{{ user.username }}</strong>
                    </div>
                  </td>
                  <td>
                    <span class="badge" :class="user.status === 'ACTIVE' ? 'active' : 'disabled'">
                      {{ user.status === 'ACTIVE' ? '启用' : '禁用' }}
                    </span>
                    <span v-if="user.mustChangePassword" class="badge must">需改密</span>
                  </td>
                  <td>{{ formatDate(user.createdAt) }}</td>
                  <td>{{ formatDate(user.lastLoginAt) }}</td>
                  <td>{{ formatDate(user.lastActiveAt) }}</td>
                  <td>
                    <div class="row-actions">
                      <button :data-test="`reset-${user.userId}`" type="button" class="table-action" @click.stop="openReset(user)">
                        <KeyRound :size="16" aria-hidden="true" />
                        重置
                      </button>
                      <button
                        type="button"
                        class="table-action"
                        :class="user.status === 'ACTIVE' ? 'disable' : 'enable'"
                        @click.stop="toggleStatus(user)"
                      >
                        <component :is="user.status === 'ACTIVE' ? Ban : CircleCheck" :size="16" aria-hidden="true" />
                        {{ user.status === 'ACTIVE' ? '禁用' : '启用' }}
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <aside
          v-if="admin.selectedUser"
          data-test="user-detail"
          class="panel detail"
          :class="{ 'disabled-detail': admin.selectedUser.status === 'DISABLED' }"
        >
          <div class="panel-head">
            <div class="panel-title">
              <h2>账号详情</h2>
              <p>选择用户后执行管理操作。</p>
            </div>
          </div>
          <div class="detail-body">
            <div class="detail-header" :class="{ disabled: admin.selectedUser.status !== 'ACTIVE' }">
              <div class="detail-avatar">{{ admin.selectedUser.username.slice(0, 1).toUpperCase() }}</div>
              <div class="detail-name">
                <h3>{{ admin.selectedUser.username }}</h3>
                <p>{{ admin.selectedUser.status === 'ACTIVE' ? '账号可登录' : '账号已禁用' }}</p>
              </div>
            </div>

            <div class="kv">
              <div class="kv-row">
                <span>状态</span>
                <span class="status-pill" :class="admin.selectedUser.status === 'ACTIVE' ? 'active' : 'disabled'">
                  <component :is="admin.selectedUser.status === 'ACTIVE' ? CircleCheck : Ban" :size="14" aria-hidden="true" />
                  {{ admin.selectedUser.status === 'ACTIVE' ? '启用' : '禁用' }}
                </span>
              </div>
              <div class="kv-row"><span>注册时间</span><span>{{ formatDate(admin.selectedUser.createdAt) }}</span></div>
              <div class="kv-row"><span>最近登录</span><span>{{ formatDate(admin.selectedUser.lastLoginAt) }}</span></div>
              <div class="kv-row"><span>最近活跃</span><span>{{ formatDate(admin.selectedUser.lastActiveAt) }}</span></div>
              <div class="kv-row"><span>下次登录改密</span><span>{{ admin.selectedUser.mustChangePassword ? '需要' : '不需要' }}</span></div>
            </div>

            <div class="action-stack">
              <button type="button" class="btn primary full" @click="openReset(admin.selectedUser)">
                <KeyRound :size="17" aria-hidden="true" />
                重置密码
              </button>
              <button
                type="button"
                class="btn secondary full status-action"
                :class="{ enable: admin.selectedUser.status !== 'ACTIVE' }"
                @click="toggleStatus(admin.selectedUser)"
              >
                <component :is="admin.selectedUser.status === 'ACTIVE' ? Ban : CircleCheck" :size="17" aria-hidden="true" />
                {{ admin.selectedUser.status === 'ACTIVE' ? '禁用账号' : '启用账号' }}
              </button>
            </div>

            <div>
              <p class="helper">账号活跃概览</p>
              <div class="activity-bars">
                <div v-for="item in activityBars(admin.selectedUser)" :key="item.label" class="bar-row">
                  <span>{{ item.label }}</span>
                  <div class="bar"><span :style="{ width: item.value + '%' }" /></div>
                  <strong>{{ item.text }}</strong>
                </div>
              </div>
            </div>
          </div>
        </aside>
      </section>

      <div v-if="resetTarget" class="modal-backdrop" @click.self="closeReset">
        <article class="modal" role="dialog" aria-modal="true" aria-labelledby="reset-title">
          <div class="modal-head">
            <div>
              <h2 id="reset-title">重置 {{ resetTarget.username }} 的密码</h2>
              <p class="subtitle">生成临时密码后，该用户下次登录必须修改密码。</p>
            </div>
            <button class="icon-btn" type="button" @click="closeReset" aria-label="关闭弹窗">
              <X :size="17" aria-hidden="true" />
            </button>
          </div>

          <div class="modal-body">
            <div class="segmented-field">
              <span class="segmented-label">临时密码生成方式</span>
              <div class="segmented-control" role="radiogroup" aria-label="临时密码生成方式">
                <button
                  type="button"
                  class="segment-option"
                  :class="{ active: resetMode === 'AUTO' }"
                  role="radio"
                  :aria-checked="resetMode === 'AUTO'"
                  @click="resetMode = 'AUTO'"
                >
                  <Sparkles :size="18" aria-hidden="true" />
                  <span>
                    <strong>自动生成</strong>
                    <span>推荐，减少弱密码</span>
                  </span>
                </button>
                <button
                  type="button"
                  class="segment-option"
                  :class="{ active: resetMode === 'MANUAL' }"
                  role="radio"
                  :aria-checked="resetMode === 'MANUAL'"
                  @click="resetMode = 'MANUAL'"
                >
                  <Keyboard :size="18" aria-hidden="true" />
                  <span>
                    <strong>手动设置</strong>
                    <span>用于线下约定密码</span>
                  </span>
                </button>
              </div>
            </div>

            <label v-if="resetMode === 'MANUAL'">
              临时密码
              <input v-model="manualPassword" name="temporaryPassword" />
            </label>
            <label v-if="resetMode === 'MANUAL'">
              确认临时密码
              <input v-model="confirmManualPassword" name="confirmTemporaryPassword" />
            </label>
            <p v-if="resetMode === 'MANUAL'" class="password-rule">{{ PASSWORD_RULE_TEXT }}</p>
            <p v-if="resetErrorMessage" class="reset-error" role="alert">{{ resetErrorMessage }}</p>

            <div v-if="generatedPassword" class="temp-password">
              <div>
                <p class="helper">请复制并通过线下渠道发给用户</p>
                <strong>{{ generatedPassword }}</strong>
              </div>
              <button class="btn secondary" type="button">
                <Copy :size="16" aria-hidden="true" />
                复制
              </button>
            </div>

            <div class="alert warn">
              <ShieldAlert :size="17" aria-hidden="true" />
              <span>系统只保存密码哈希。管理员只能看到本次生成的临时密码，关闭弹窗后不再展示。</span>
            </div>
          </div>

          <footer class="modal-foot">
            <button type="button" class="btn secondary" @click="closeReset">取消</button>
            <button type="button" class="btn primary" @click="confirmReset">
              <RotateCcwKey :size="17" aria-hidden="true" />
              确认重置
            </button>
          </footer>
        </article>
      </div>
    </section>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  Activity,
  Ban,
  CircleCheck,
  Copy,
  KeyRound,
  Keyboard,
  RotateCcwKey,
  ShieldAlert,
  Sparkles,
  Users,
  X,
} from '@lucide/vue';
import AppLayout from '@/components/AppLayout.vue';
import { useAdminStore } from '@/stores/useAdminStore';
import { useAuthStore } from '@/stores/useAuthStore';
import type { AdminUserListItemDto, ResetPasswordMode, UserStatus } from '@/types/admin';
import {
  PASSWORD_RULE_TEXT,
  getPasswordConfirmationMessage,
  getPasswordValidationMessage,
} from '@/utils/passwordRules';

const admin = useAdminStore();
const auth = useAuthStore();
const router = useRouter();
const resetTarget = ref<AdminUserListItemDto | null>(null);
const resetMode = ref<ResetPasswordMode>('AUTO');
const manualPassword = ref('');
const confirmManualPassword = ref('');
const resetErrorMessage = ref('');
const generatedPassword = ref('');
const keyword = ref('');
const statusFilter = ref<UserStatus | ''>('');

onMounted(() => {
  void admin.loadAll();
});

function formatDate(value: string | null) {
  if (!value) return '-';
  return value.slice(0, 10);
}

function activityBars(user: AdminUserListItemDto) {
  return [
    { label: '注册', value: 100, text: formatDate(user.createdAt) },
    { label: '登录', value: user.lastLoginAt ? 72 : 8, text: formatDate(user.lastLoginAt) },
    { label: '活跃', value: user.lastActiveAt ? 56 : 8, text: formatDate(user.lastActiveAt) },
  ];
}

function openReset(user: AdminUserListItemDto) {
  resetTarget.value = user;
  resetMode.value = 'AUTO';
  manualPassword.value = '';
  confirmManualPassword.value = '';
  resetErrorMessage.value = '';
  generatedPassword.value = '';
}

function closeReset() {
  resetTarget.value = null;
}

async function confirmReset() {
  if (!resetTarget.value) return;
  resetErrorMessage.value = '';
  if (resetMode.value === 'MANUAL') {
    resetErrorMessage.value = getPasswordValidationMessage(manualPassword.value)
      || getPasswordConfirmationMessage(manualPassword.value, confirmManualPassword.value);
    if (resetErrorMessage.value) return;
  }

  const response = await admin.resetUserPassword(resetTarget.value.userId, {
    mode: resetMode.value,
    temporaryPassword: resetMode.value === 'MANUAL' ? manualPassword.value : null,
  });
  generatedPassword.value = response.temporaryPassword;
  await admin.loadAll();
}

async function toggleStatus(user: AdminUserListItemDto) {
  if (user.status === 'ACTIVE') {
    await admin.disableUser(user.userId);
    return;
  }
  await admin.enableUser(user.userId);
}

async function logout() {
  auth.logout();
  await router.push('/login');
}

async function applyFilters() {
  await admin.loadUsers({ keyword: keyword.value, status: statusFilter.value });
}
</script>

<style scoped>
.admin-page {
  display: grid;
  gap: 18px;
}

.topline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.page-title h1,
.page-title p,
.panel-title h2,
.panel-title p,
.detail-name h3,
.detail-name p,
.helper {
  margin: 0;
}

.page-title h1 {
  color: #0f172a;
  font-size: 24px;
  line-height: 1.18;
}

.page-title p {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.metric {
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
  padding: 16px;
}

.metric-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.metric-value {
  margin-top: 12px;
  color: #0f172a;
  font-size: 27px;
  font-weight: 800;
}

.metric-note {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
}

.panel {
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.75);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.08), 0 1px 2px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(22px);
}

.panel-head {
  min-height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.panel-title h2 {
  color: #0f172a;
  font-size: 16px;
}

.panel-title p {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search,
.tools select {
  width: 100%;
  min-height: 40px;
  border: 0;
  border-bottom: 1.5px solid #e2e8f0;
  border-radius: 0;
  outline: none;
  background: transparent;
  color: #1e293b;
  padding: 8px 0;
  font-size: 13px;
}

.search:focus,
.tools select:focus {
  border-bottom-color: #6366f1;
  box-shadow: 0 1px 0 0 #6366f1;
}

.search {
  width: 220px;
}

.table-wrap {
  overflow: auto;
}

table {
  width: 100%;
  min-width: 820px;
  border-collapse: collapse;
}

th,
td {
  padding: 13px 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  text-align: left;
  font-size: 13px;
}

th {
  color: #64748b;
  background: #f8fafc;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

tr.selected {
  background: #eef2ff;
}

tr:hover {
  background: #f8fafc;
}

.disabled-row {
  background: #fff7f7;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mini-avatar {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-weight: 800;
}

.badge,
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.badge {
  min-height: 24px;
  padding: 0 9px;
}

.badge.active,
.status-pill.active {
  background: #dcfce7;
  color: #15803d;
}

.badge.disabled,
.status-pill.disabled {
  background: #fee2e2;
  color: #b91c1c;
}

.badge.must {
  margin-left: 6px;
  background: #fef3c7;
  color: #b45309;
}

.row-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.table-action,
.btn,
.icon-btn {
  cursor: pointer;
}

.table-action {
  min-height: 34px;
  border: 1px solid rgba(203, 213, 225, 0.86);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.78);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 10px;
  color: #475569;
  font-size: 12px;
  font-weight: 800;
}

.table-action:hover {
  border-color: rgba(129, 140, 248, 0.45);
  background: #eef2ff;
  color: #4338ca;
}

.table-action.disable {
  border-color: #fecaca;
  background: #fff7f7;
  color: #b91c1c;
}

.table-action.enable {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.detail {
  display: block;
}

.detail.disabled-detail {
  border-color: #fecaca;
}

.detail-body {
  padding: 16px;
  display: grid;
  gap: 14px;
}

.detail-header {
  display: flex;
  gap: 12px;
  align-items: center;
  border: 1px solid transparent;
  border-radius: 16px;
  padding: 10px;
  margin: -2px -2px 0;
}

.detail-header.disabled {
  border-color: #fecaca;
  background: #fff7f7;
}

.detail-avatar {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: #f1f5f9;
  color: #334155;
  font-weight: 800;
}

.detail-header.disabled .detail-avatar {
  background: #fee2e2;
  color: #991b1b;
}

.detail-name h3 {
  color: #0f172a;
  font-size: 16px;
}

.detail-name p {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.detail-header.disabled .detail-name p {
  color: #b91c1c;
  font-weight: 800;
}

.status-pill {
  min-height: 26px;
  padding: 0 10px;
}

.kv {
  display: grid;
  gap: 8px;
}

.kv-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  font-size: 13px;
}

.kv-row span:first-child {
  color: #64748b;
}

.kv-row span:last-child {
  text-align: right;
  font-weight: 700;
}

.action-stack {
  display: grid;
  gap: 10px;
}

.btn {
  min-height: 40px;
  border: 1px solid transparent;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 800;
}

.btn.primary {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  box-shadow: 0 16px 30px rgba(79, 70, 229, 0.25);
}

.btn.primary:hover {
  background: linear-gradient(135deg, #6366f1, #4338ca);
}

.btn.secondary {
  border-color: #e2e8f0;
  background: #fff;
  color: #334155;
}

.btn.secondary:hover {
  background: #f8fafc;
}

.btn.full {
  width: 100%;
  min-height: 46px;
}

.status-action {
  border-color: #fecaca;
  color: #b91c1c;
}

.status-action.enable {
  border-color: #bbf7d0;
  color: #15803d;
  background: #f0fdf4;
}

.helper {
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.activity-bars {
  display: grid;
  gap: 10px;
  margin-top: 10px;
}

.bar-row {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) 76px;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 12px;
}

.bar {
  height: 8px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: #6366f1;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.36);
}

.modal {
  width: min(460px, 100%);
  overflow: hidden;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.24);
}

.modal-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.modal-head h2,
.subtitle {
  margin: 0;
}

.modal-head h2 {
  color: #0f172a;
  font-size: 18px;
}

.subtitle {
  margin-top: 10px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  display: grid;
  place-items: center;
}

.icon-btn:hover {
  color: #4338ca;
  border-color: rgba(129, 140, 248, 0.45);
  background: #eef2ff;
}

.modal-body {
  padding: 18px;
  display: grid;
  gap: 16px;
}

.modal-body label {
  display: grid;
  gap: 8px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

.modal-body input {
  width: 100%;
  min-height: 40px;
  border: 0;
  border-bottom: 1.5px solid #e2e8f0;
  border-radius: 0;
  outline: none;
  background: transparent;
  color: #1e293b;
  padding: 8px 0;
}

.modal-body input:focus {
  border-bottom-color: #6366f1;
  box-shadow: 0 1px 0 0 #6366f1;
}

.segmented-field {
  display: grid;
  gap: 9px;
}

.segmented-label {
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

.segmented-control {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  padding: 6px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 16px;
  background: #f8fafc;
}

.segment-option {
  min-height: 54px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  color: #64748b;
  text-align: left;
}

.segment-option strong {
  display: block;
  color: #334155;
  font-size: 13px;
}

.segment-option span span {
  display: block;
  margin-top: 2px;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.35;
}

.segment-option.active {
  border-color: rgba(129, 140, 248, 0.38);
  background: #fff;
  color: #4338ca;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.segment-option.active strong {
  color: #4338ca;
}

.password-rule {
  margin: -6px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.reset-error {
  margin: -6px 0 0;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fef2f2;
  color: #b91c1c;
  padding: 10px 12px;
  font-size: 12px;
}

.temp-password {
  border: 1px dashed #a5b4fc;
  border-radius: 8px;
  background: #eef2ff;
  padding: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.temp-password strong {
  font-size: 18px;
  letter-spacing: 0;
}

.alert {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  border-radius: 8px;
  padding: 12px;
  font-size: 12px;
  line-height: 1.6;
}

.alert.warn {
  background: #fef3c7;
  color: #92400e;
}

.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 18px 18px;
}

@media (max-width: 1080px) {
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workbench {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .topline {
    flex-direction: column;
  }

  .metrics {
    grid-template-columns: 1fr;
  }

  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .tools {
    width: 100%;
    flex-wrap: wrap;
  }

  .search {
    width: 100%;
  }
}
</style>
