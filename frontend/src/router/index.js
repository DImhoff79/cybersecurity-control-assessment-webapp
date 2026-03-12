import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true } },
    { path: '/', redirect: '/my-audits' },
    { path: '/my-audits', name: 'MyAudits', component: () => import('../views/selfservice/MyAudits.vue') },
    { path: '/my-tasks', name: 'MyTasks', component: () => import('../views/selfservice/MyTasks.vue') },
    { path: '/audits/:auditId/respond', name: 'AuditRespond', component: () => import('../views/selfservice/AuditRespond.vue') },
    { path: '/admin', redirect: '/admin/reports' },
    { path: '/admin/applications', name: 'AdminApplications', component: () => import('../views/admin/Applications.vue'), meta: { permission: 'APPLICATION_MANAGEMENT' } },
    { path: '/admin/audits', name: 'AdminAudits', component: () => import('../views/admin/KickoffAudit.vue'), meta: { permission: 'AUDIT_MANAGEMENT' } },
    { path: '/admin/audit-projects', name: 'AdminAuditProjects', component: () => import('../views/admin/AuditProjects.vue'), meta: { permission: 'AUDIT_MANAGEMENT' } },
    { path: '/admin/findings', name: 'AdminFindings', component: () => import('../views/admin/Findings.vue'), meta: { permission: 'AUDIT_MANAGEMENT' } },
    { path: '/admin/control-exceptions', name: 'AdminControlExceptions', component: () => import('../views/admin/ControlExceptions.vue'), meta: { permission: 'AUDIT_MANAGEMENT' } },
    { path: '/admin/review-queue', name: 'AdminReviewQueue', component: () => import('../views/admin/ReviewQueue.vue'), meta: { permission: 'AUDIT_MANAGEMENT' } },
    { path: '/admin/reports', name: 'AdminReports', component: () => import('../views/admin/Reports.vue'), meta: { permission: 'REPORT_VIEW' } },
    { path: '/admin/auditor-workbench', name: 'AdminAuditorWorkbench', component: () => import('../views/admin/AuditorWorkbench.vue'), meta: { permission: 'REPORT_VIEW' } },
    { path: '/admin/users', name: 'AdminUsers', component: () => import('../views/admin/UserManagement.vue'), meta: { permission: 'USER_MANAGEMENT' } },
    {
      path: '/admin/questionnaire-templates',
      name: 'AdminQuestionnaireTemplates',
      component: () => import('../views/admin/QuestionnaireTemplates.vue'),
      meta: { permission: 'AUDIT_MANAGEMENT', roles: ['ADMIN', 'AUDIT_MANAGER'] }
    },
    { path: '/admin/questionnaire-builder', name: 'AdminQuestionnaireBuilder', component: () => import('../views/admin/QuestionnaireBuilder.vue'), meta: { permission: 'AUDIT_MANAGEMENT' } },
    {
      path: '/admin/controls',
      name: 'AdminControls',
      redirect: (to) => ({
        name: 'AdminQuestionnaireBuilder',
        query: { ...to.query, tab: 'controls' }
      })
    },
    {
      path: '/admin/questions',
      name: 'AdminQuestions',
      redirect: (to) => ({
        name: 'AdminQuestionnaireBuilder',
        query: { ...to.query, tab: 'questions' }
      })
    },
    { path: '/admin/audits/:auditId', name: 'AuditDetail', component: () => import('../views/audits/Assessment.vue'), meta: { permission: 'AUDIT_MANAGEMENT' } },
    { path: '/access-denied', name: 'AccessDenied', component: () => import('../views/AccessDenied.vue') },
    { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue') },
  ]
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (to.meta.public) return next()
  if (!authStore.hasCredentials) return next({ name: 'Login', query: { redirect: to.fullPath } })
  authStore.fetchUser().then(() => {
    if (to.path === '/admin') {
      if (authStore.hasPermission('APPLICATION_MANAGEMENT')) return next('/admin/applications')
      if (authStore.hasPermission('AUDIT_MANAGEMENT')) return next('/admin/audits')
      if (authStore.hasPermission('REPORT_VIEW')) return next('/admin/reports')
      if (authStore.hasPermission('USER_MANAGEMENT')) return next('/admin/users')
      return next('/my-audits')
    }
    if (to.path.startsWith('/admin') && !authStore.canAccessAdmin) {
      next({
        name: 'AccessDenied',
        query: {
          from: to.fullPath
        }
      })
    } else if (to.meta.roles?.length && !to.meta.roles.includes(authStore.user?.role)) {
      next({
        name: 'AccessDenied',
        query: {
          from: to.fullPath,
          required: to.meta.roles.join(' or ')
        }
      })
    } else if (to.meta.permission && !authStore.hasPermission(to.meta.permission)) {
      next({
        name: 'AccessDenied',
        query: {
          from: to.fullPath,
          required: to.meta.permission
        }
      })
    } else {
      next()
    }
  }).catch(() => next({ name: 'Login' }))
})

export default router
