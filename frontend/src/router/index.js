import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true } },
    { path: '/', redirect: '/my-audits' },
    { path: '/my-audits', name: 'MyAudits', component: () => import('../views/selfservice/MyAudits.vue') },
    { path: '/audits/:auditId/respond', name: 'AuditRespond', component: () => import('../views/selfservice/AuditRespond.vue') },
    { path: '/admin', redirect: '/admin/applications' },
    { path: '/admin/applications', name: 'AdminApplications', component: () => import('../views/admin/Applications.vue') },
    { path: '/admin/audits', name: 'AdminAudits', component: () => import('../views/admin/KickoffAudit.vue') },
    { path: '/admin/review-queue', name: 'AdminReviewQueue', component: () => import('../views/admin/ReviewQueue.vue') },
    { path: '/admin/controls', name: 'AdminControls', component: () => import('../views/admin/ControlCatalog.vue') },
    { path: '/admin/questions', name: 'AdminQuestions', component: () => import('../views/admin/QuestionManager.vue') },
    { path: '/admin/audits/:auditId', name: 'AuditDetail', component: () => import('../views/audits/Assessment.vue') },
    { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue') },
  ]
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (to.meta.public) return next()
  if (!authStore.hasCredentials) return next({ name: 'Login', query: { redirect: to.fullPath } })
  authStore.fetchUser().then(() => {
    if (to.path.startsWith('/admin') && !authStore.isAdmin) {
      next('/my-audits')
    } else {
      next()
    }
  }).catch(() => next({ name: 'Login' }))
})

export default router
