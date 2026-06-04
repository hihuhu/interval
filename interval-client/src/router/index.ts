import { createRouter, createWebHistory, type Router, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/useAuthStore';

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'homeRedirect',
    redirect: () => {
      const auth = useAuthStore();
      if (!auth.isAuthenticated) return '/login';
      if (auth.mustChangePassword) return '/change-password';
      return auth.accountType === 'ADMIN' ? '/admin' : '/time-grid';
    },
  },
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { publicOnly: true } },
  { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { publicOnly: true } },
  { path: '/forgot-password', name: 'forgotPassword', component: () => import('@/views/ForgotPasswordView.vue'), meta: { publicOnly: true } },
  { path: '/change-password', name: 'changePassword', component: () => import('@/views/ChangePasswordView.vue'), meta: { requiresAuth: true } },
  { path: '/admin', name: 'admin', component: () => import('@/views/AdminAccountView.vue'), meta: { requiresAuth: true, adminOnly: true } },
  { path: '/time-grid', name: 'timeGrid', component: () => import('@/views/TimeGridView.vue'), meta: { requiresAuth: true } },
  { path: '/stats', name: 'stats', component: () => import('@/views/StatsView.vue'), meta: { requiresAuth: true } },
  { path: '/:pathMatch(.*)*', name: 'notFound', component: () => import('@/views/NotFoundView.vue') },
];

export function installAuthGuard(router: Router) {
  router.beforeEach((to) => {
    const auth = useAuthStore();
    auth.restoreSession();

    if (to.meta.requiresAuth && !auth.isAuthenticated) {
      return { name: 'login', query: { redirect: to.fullPath } };
    }

    if (auth.isAuthenticated && auth.mustChangePassword && to.name !== 'changePassword') {
      return { name: 'changePassword' };
    }

    if (to.meta.publicOnly && auth.isAuthenticated) {
      return { name: auth.accountType === 'ADMIN' ? 'admin' : 'timeGrid' };
    }

    if (to.meta.adminOnly && auth.accountType !== 'ADMIN') {
      return { name: 'timeGrid' };
    }

    if (auth.accountType === 'ADMIN' && (to.name === 'timeGrid' || to.name === 'stats')) {
      return { name: 'admin' };
    }

    return true;
  });
}

export const router = createRouter({
  history: createWebHistory(),
  routes,
});

installAuthGuard(router);
