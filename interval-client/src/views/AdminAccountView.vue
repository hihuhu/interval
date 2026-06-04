<template>
  <AppLayout :username="auth.username ?? 'admin'" active-route="admin" account-type="ADMIN" @logout="logout">
    <section class="admin-page">
      <header class="page-head">
        <div>
          <h1>账号管理</h1>
          <p>查看用户、活跃度、重置密码、禁用或启用账号。</p>
        </div>
      </header>

      <section class="metrics" aria-label="账号仪表盘">
        <article class="metric">
          <span>总用户</span>
          <strong>{{ admin.dashboard?.totalUsers ?? 0 }}</strong>
        </article>
        <article class="metric">
          <span>启用账号</span>
          <strong>{{ admin.dashboard?.activeUsers ?? 0 }}</strong>
        </article>
        <article class="metric">
          <span>禁用账号</span>
          <strong>{{ admin.dashboard?.disabledUsers ?? 0 }}</strong>
        </article>
        <article class="metric">
          <span>今日活跃</span>
          <strong>{{ admin.dashboard?.activeToday ?? 0 }}</strong>
        </article>
        <article class="metric">
          <span>近 7 日活跃</span>
          <strong>{{ admin.dashboard?.activeLast7Days ?? 0 }}</strong>
        </article>
      </section>

      <section class="workbench">
        <article class="panel">
          <div class="panel-head">
            <h2>用户列表</h2>
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
                  <td><strong>{{ user.username }}</strong></td>
                  <td>
                    <span class="status-badge" :class="user.status === 'ACTIVE' ? 'active' : 'disabled'">
                      {{ user.status === 'ACTIVE' ? '启用' : '禁用' }}
                    </span>
                    <span v-if="user.mustChangePassword" class="status-badge must">需改密</span>
                  </td>
                  <td>{{ formatDate(user.lastLoginAt) }}</td>
                  <td>{{ formatDate(user.lastActiveAt) }}</td>
                  <td>
                    <div class="row-actions">
                      <button :data-test="`reset-${user.userId}`" type="button" class="table-action" @click.stop="openReset(user)">
                        重置
                      </button>
                      <button
                        type="button"
                        class="table-action"
                        :class="user.status === 'ACTIVE' ? 'danger' : 'enable'"
                        @click.stop="toggleStatus(user)"
                      >
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
          <h2>{{ admin.selectedUser.username }}</h2>
          <p>{{ admin.selectedUser.status === 'ACTIVE' ? '账号可登录' : '账号已禁用' }}</p>
          <dl>
            <div>
              <dt>最近登录</dt>
              <dd>{{ formatDate(admin.selectedUser.lastLoginAt) }}</dd>
            </div>
            <div>
              <dt>最近活跃</dt>
              <dd>{{ formatDate(admin.selectedUser.lastActiveAt) }}</dd>
            </div>
            <div>
              <dt>下次登录改密</dt>
              <dd>{{ admin.selectedUser.mustChangePassword ? '需要' : '不需要' }}</dd>
            </div>
          </dl>
        </aside>
      </section>

      <div v-if="resetTarget" class="modal-backdrop" @click.self="closeReset">
        <article class="modal" role="dialog" aria-modal="true">
          <header>
            <h2>重置 {{ resetTarget.username }} 的密码</h2>
          </header>
          <div class="segmented-control" role="radiogroup">
            <button type="button" :class="{ active: resetMode === 'AUTO' }" @click="resetMode = 'AUTO'">自动生成</button>
            <button type="button" :class="{ active: resetMode === 'MANUAL' }" @click="resetMode = 'MANUAL'">手动设置</button>
          </div>
          <label v-if="resetMode === 'MANUAL'">
            临时密码
            <input v-model="manualPassword" name="temporaryPassword" />
          </label>
          <p v-if="generatedPassword" class="temp-password">{{ generatedPassword }}</p>
          <footer>
            <button type="button" class="table-action" @click="closeReset">取消</button>
            <button type="button" class="table-action primary" @click="confirmReset">确认重置</button>
          </footer>
        </article>
      </div>
    </section>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppLayout from '@/components/AppLayout.vue';
import { useAdminStore } from '@/stores/useAdminStore';
import { useAuthStore } from '@/stores/useAuthStore';
import type { AdminUserListItemDto, ResetPasswordMode, UserStatus } from '@/types/admin';

const admin = useAdminStore();
const auth = useAuthStore();
const router = useRouter();
const resetTarget = ref<AdminUserListItemDto | null>(null);
const resetMode = ref<ResetPasswordMode>('AUTO');
const manualPassword = ref('');
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

function openReset(user: AdminUserListItemDto) {
  resetTarget.value = user;
  resetMode.value = 'AUTO';
  manualPassword.value = '';
  generatedPassword.value = '';
}

function closeReset() {
  resetTarget.value = null;
}

async function confirmReset() {
  if (!resetTarget.value) return;
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

.page-head h1,
.page-head p {
  margin: 0;
}

.page-head h1 {
  color: #1e293b;
  font-size: 24px;
}

.page-head p {
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
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  padding: 16px;
}

.metric span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.metric strong {
  display: block;
  margin-top: 10px;
  font-size: 26px;
}

.workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.panel {
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  overflow: hidden;
}

.panel-head {
  padding: 14px 16px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-head h2,
.detail h2,
.detail p {
  margin: 0;
}

.tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search,
.tools select {
  min-height: 38px;
  border: 0;
  border-bottom: 1px solid #cbd5e1;
  background: transparent;
  color: #334155;
  padding: 0 8px;
  font-size: 13px;
}

.search {
  width: 220px;
}

.table-wrap {
  overflow: auto;
}

table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

th,
td {
  padding: 13px 16px;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
  font-size: 13px;
}

tr.selected {
  background: #eef2ff;
}

.disabled-row {
  background: #fff7f7;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  border-radius: 999px;
  padding: 0 9px;
  font-size: 12px;
  font-weight: 800;
}

.status-badge.active {
  background: #dcfce7;
  color: #15803d;
}

.status-badge.disabled {
  background: #fee2e2;
  color: #b91c1c;
}

.status-badge.must {
  margin-left: 6px;
  background: #fef3c7;
  color: #b45309;
}

.row-actions {
  display: flex;
  gap: 8px;
}

.table-action {
  min-height: 34px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  padding: 0 10px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.table-action.danger {
  border-color: #fecaca;
  background: #fff7f7;
  color: #b91c1c;
}

.table-action.enable {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.table-action.primary {
  border-color: #6366f1;
  background: #6366f1;
  color: #fff;
}

.detail {
  padding: 16px;
  display: grid;
  gap: 14px;
}

.disabled-detail {
  border-color: #fecaca;
  background: #fff7f7;
}

dl {
  display: grid;
  gap: 10px;
  margin: 0;
}

dt {
  color: #64748b;
  font-size: 12px;
}

dd {
  margin: 2px 0 0;
  font-size: 13px;
  font-weight: 800;
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
  border-radius: 8px;
  background: #fff;
  padding: 18px;
  display: grid;
  gap: 16px;
}

.segmented-control {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  padding: 6px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.segmented-control button {
  min-height: 44px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  font-weight: 800;
}

.segmented-control button.active {
  border-color: rgba(99, 102, 241, 0.38);
  background: #fff;
  color: #4338ca;
}

.temp-password {
  border: 1px dashed #a5b4fc;
  border-radius: 8px;
  background: #eef2ff;
  padding: 14px;
  font-weight: 900;
}

footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .metrics,
  .workbench {
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
