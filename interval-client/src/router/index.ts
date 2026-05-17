import { createRouter, createWebHistory, type Router, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/useAuthStore';

export const routes: RouteRecordRaw[] = [
  { path: '/', name: 'homeRedirect', redirect: () => (useAuthStore().isAuthenticated ? '/time-grid' : '/login') },
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { publicOnly: true } },
  { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { publicOnly: true } },
  { path: '/time-grid', name: 'timeGrid', component: () => import('@/views/TimeGridView.vue'), meta: { requiresAuth: true } },
  { path: '/:pathMatch(.*)*', name: 'notFound', component: () => import('@/views/NotFoundView.vue') },
];

export function installAuthGuard(router: Router) {
  router.beforeEach((to) => {
    const auth = useAuthStore();
    auth.restoreSession();

    if (to.meta.requiresAuth && !auth.isAuthenticated) {
      return { name: 'login', query: { redirect: to.fullPath } };
    }

    if (to.meta.publicOnly && auth.isAuthenticated) {
      return { name: 'timeGrid' };
    }

    return true;
  });
}

export const router = createRouter({
  history: createWebHistory(),
  routes,
});

installAuthGuard(router);
