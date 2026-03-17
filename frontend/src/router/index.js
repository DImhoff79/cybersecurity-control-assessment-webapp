import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true } },
    { path: '/', redirect: '/my-audits' },
    { path: '/my-audits', name: 'MyAudits', component: () => import('../views/selfservice/MyAudits.vue') },
    { path: '/my-tasks', name: 'MyTasks', component: () => import('../views/selfservice/MyTasks.vue') },
    { path: '/my-policies', name: 'MyPolicies', component: () => import('../views/selfservice/MyPolicies.vue') },
    { path: '/audits/:auditId/respond', name: 'AuditRespond', component: () => import('../views/selfservice/AuditRespond.vue') },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { appShell: 'admin' },
      children: [
        { path: '', redirect: '/admin/operations' },
        {
          path: 'applications',
          name: 'AdminApplications',
          component: () => import('../views/admin/Applications.vue'),
          meta: { permission: 'APPLICATION_MANAGEMENT', section: 'Admin Ops', pageTitle: 'Applications' }
        },
        {
          path: 'audit-projects',
          name: 'AdminAuditProjects',
          component: () => import('../views/admin/AuditProjects.vue'),
          meta: { permission: 'AUDIT_MANAGEMENT', section: 'Audit Program', pageTitle: 'Audit Projects' }
        },
        {
          path: 'audits',
          name: 'AdminAudits',
          component: () => import('../views/admin/KickoffAudit.vue'),
          meta: { permission: 'AUDIT_MANAGEMENT', section: 'Audit Program', pageTitle: 'Audits' }
        },
        {
          path: 'operations',
          name: 'AdminOperations',
          component: () => import('../views/admin/OperationsQueue.vue'),
          meta: { permission: 'REPORT_VIEW', section: 'Audit Program', pageTitle: 'Operations Queue' }
        },
        {
          path: 'findings',
          name: 'AdminFindings',
          component: () => import('../views/admin/Findings.vue'),
          meta: { permission: 'AUDIT_MANAGEMENT', section: 'Risk & Remediation', pageTitle: 'Findings' }
        },
        {
          path: 'control-exceptions',
          name: 'AdminControlExceptions',
          component: () => import('../views/admin/ControlExceptions.vue'),
          meta: { permission: 'AUDIT_MANAGEMENT', section: 'Risk & Remediation', pageTitle: 'Control Exceptions' }
        },
        {
          path: 'reports',
          name: 'AdminReports',
          component: () => import('../views/admin/Reports.vue'),
          meta: { permission: 'REPORT_VIEW', section: 'Reporting', pageTitle: 'Reports' }
        },
        {
          path: 'policies',
          name: 'AdminPolicies',
          component: () => import('../views/admin/Policies.vue'),
          meta: { permission: 'POLICY_MANAGEMENT', section: 'Governance & Compliance', pageTitle: 'Policies' }
        },
        {
          path: 'policy-attestations',
          name: 'AdminPolicyAttestations',
          component: () => import('../views/admin/PolicyAttestations.vue'),
          meta: { permission: 'REPORT_VIEW', section: 'Governance & Compliance', pageTitle: 'Policy Attestations' }
        },
        {
          path: 'compliance-obligations',
          name: 'AdminComplianceObligations',
          component: () => import('../views/admin/ComplianceObligations.vue'),
          meta: { permission: 'COMPLIANCE_MANAGEMENT', section: 'Governance & Compliance', pageTitle: 'Compliance Obligations' }
        },
        {
          path: 'risk-register',
          name: 'AdminRiskRegister',
          component: () => import('../views/admin/RiskRegister.vue'),
          meta: { permission: 'RISK_MANAGEMENT', section: 'Risk & Remediation', pageTitle: 'Risk Register' }
        },
        {
          path: 'remediation-plans',
          name: 'AdminRemediationPlans',
          component: () => import('../views/admin/RemediationPlans.vue'),
          meta: { permission: 'REMEDIATION_MANAGEMENT', section: 'Risk & Remediation', pageTitle: 'Remediation Plans' }
        },
        {
          path: 'users',
          name: 'AdminUsers',
          component: () => import('../views/admin/UserManagement.vue'),
          meta: { permission: 'USER_MANAGEMENT', section: 'Admin Ops', pageTitle: 'Users' }
        },
        {
          path: 'questionnaire',
          name: 'AdminQuestionnaireHub',
          component: () => import('../views/admin/QuestionnaireHub.vue'),
          meta: { permission: 'AUDIT_MANAGEMENT', section: 'Audit Program', pageTitle: 'Questionnaire' }
        },
        {
          path: 'questionnaire-templates',
          name: 'AdminQuestionnaireTemplates',
          component: () => import('../views/admin/QuestionnaireTemplates.vue'),
          meta: { permission: 'AUDIT_MANAGEMENT', roles: ['ADMIN', 'AUDIT_MANAGER'], section: 'Admin Ops', pageTitle: 'Questionnaire Governance' }
        },
        {
          path: 'questionnaire-builder',
          name: 'AdminQuestionnaireBuilder',
          component: () => import('../views/admin/QuestionnaireBuilder.vue'),
          meta: { permission: 'AUDIT_MANAGEMENT', section: 'Audit Program', pageTitle: 'Questionnaire Builder' }
        },
        {
          path: 'audits/:auditId',
          name: 'AuditDetail',
          component: () => import('../views/audits/Assessment.vue'),
          meta: { permission: 'REPORT_VIEW', section: 'Audit Program', pageTitle: 'Audit Workspace' }
        }
      ]
    },
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
    { path: '/admin/review-queue', redirect: '/admin/operations' },
    { path: '/admin/auditor-workbench', redirect: '/admin/operations' },
    { path: '/access-denied', name: 'AccessDenied', component: () => import('../views/AccessDenied.vue') },
    { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue') },
  ]
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  if (to.meta.public) return next()
  if (!authStore.hasCredentials) return next({ name: 'Login', query: { redirect: to.fullPath } })
  try {
    await authStore.fetchUser()
  } catch {
    return next({ name: 'Login' })
  }
  if (to.path === '/admin') {
    if (!authStore.canAccessAdmin) return next('/my-audits')
    if (authStore.hasPermission('AUDIT_MANAGEMENT')) return next('/admin/audit-projects')
    if (authStore.hasPermission('REPORT_VIEW')) return next('/admin/operations')
    if (authStore.hasPermission('POLICY_MANAGEMENT')) return next('/admin/policies')
    if (authStore.hasPermission('REMEDIATION_MANAGEMENT')) return next('/admin/remediation-plans')
    if (authStore.hasPermission('COMPLIANCE_MANAGEMENT')) return next('/admin/compliance-obligations')
    if (authStore.hasPermission('APPLICATION_MANAGEMENT')) return next('/admin/applications')
    if (authStore.hasPermission('USER_MANAGEMENT')) return next('/admin/users')
    return next('/my-audits')
  }
  if (to.path.startsWith('/admin') && !authStore.canAccessAdmin) {
    return next({
      name: 'AccessDenied',
      query: {
        from: to.fullPath
      }
    })
  }
  if (to.meta.roles?.length && !to.meta.roles.includes(authStore.user?.role)) {
    return next({
      name: 'AccessDenied',
      query: {
        from: to.fullPath,
        required: to.meta.roles.join(' or ')
      }
    })
  }
  if (to.meta.permission && !authStore.hasPermission(to.meta.permission)) {
    return next({
      name: 'AccessDenied',
      query: {
        from: to.fullPath,
        required: to.meta.permission
      }
    })
  }
  return next()
})

export default router
