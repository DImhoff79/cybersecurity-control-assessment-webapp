import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true } },
    {
      path: '/',
      component: () => import('../layouts/WorkspaceShell.vue'),
      children: [
        { path: '', redirect: '/start' },
        { path: 'start', name: 'RoleHub', component: () => import('../views/RoleHub.vue') },
        { path: 'my-audits', name: 'MyAudits', component: () => import('../views/selfservice/MyAudits.vue') },
        {
          path: 'my-exceptions',
          name: 'MyExceptions',
          component: () => import('../views/selfservice/MyExceptions.vue')
        },
        { path: 'audits/:auditId/respond', name: 'AuditRespond', component: () => import('../views/selfservice/AuditRespond.vue') },
        { path: 'profile', name: 'Profile', component: () => import('../views/Profile.vue') },
        { path: 'access-denied', name: 'AccessDenied', component: () => import('../views/AccessDenied.vue') }
      ]
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { appShell: 'admin' },
      children: [
        { path: '', redirect: '/admin/operations' },
        {
          path: 'my-audits',
          name: 'AdminWorkspaceMyAudits',
          component: () => import('../views/selfservice/MyAudits.vue'),
          meta: { section: 'Workspace', pageTitle: 'My Audits' }
        },
        {
          path: 'my-exceptions',
          name: 'AdminWorkspaceMyExceptions',
          component: () => import('../views/selfservice/MyExceptions.vue'),
          meta: { section: 'Workspace', pageTitle: 'My Exceptions' }
        },
        {
          path: 'profile',
          name: 'AdminWorkspaceProfile',
          component: () => import('../views/Profile.vue'),
          meta: { section: 'Workspace', pageTitle: 'Profile' }
        },
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
          meta: { permission: 'REPORT_VIEW', section: 'Audit Program', pageTitle: 'Audit Projects' }
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
          meta: { permission: 'REPORT_VIEW', section: 'Audit Program', pageTitle: 'Audit Queue' }
        },
        {
          path: 'issue-program',
          name: 'AdminIssueProgram',
          component: () => import('../views/admin/IssueProgramHub.vue'),
          meta: { permission: 'AUDIT_MANAGEMENT', section: 'Risk & Remediation', pageTitle: 'Issue Program Hub' }
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
          path: 'workspace-exceptions',
          name: 'AdminWorkspaceExceptions',
          component: () => import('../views/admin/AdminWorkspaceExceptions.vue'),
          meta: { roles: ['AUDITOR'], section: 'Risk & Remediation', pageTitle: 'My control exceptions' }
        },
        {
          path: 'approval-delegates',
          name: 'AdminApprovalDelegates',
          component: () => import('../views/admin/ApprovalDelegates.vue'),
          meta: {
            roles: ['ADMIN', 'AUDIT_MANAGER'],
            permission: 'USER_MANAGEMENT',
            section: 'Admin Ops',
            pageTitle: 'Approval delegates'
          }
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
          path: 'question-control-mapping-studio',
          name: 'AdminQuestionControlMappingStudio',
          component: () => import('../views/admin/QuestionControlMappingStudio.vue'),
          meta: { permission: 'AUDIT_MANAGEMENT', section: 'Audit Program', pageTitle: 'Mapping studio' }
        },
        {
          path: 'branching-workflow-demo',
          name: 'AdminBranchingWorkflowDemo',
          component: () => import('../views/admin/BranchingWorkflowDemo.vue'),
          meta: { permission: 'AUDIT_MANAGEMENT', section: 'Audit Program', pageTitle: 'Branching workflow (demo)' }
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
    { path: '/admin/auditor-workbench', redirect: '/admin/operations' }
  ]
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  if (to.meta.public) return next()
  if (!authStore.hasCredentials) return next({ name: 'Login', query: { redirect: to.fullPath } })
  // Load session user once per page load (Pinia resets on refresh). Refetching /me on every route
  // change caused slow navigation and flaky clicks when the request lagged or failed.
  if (!authStore.user) {
    try {
      await authStore.fetchUser()
    } catch {
      return next({ name: 'Login' })
    }
    if (!authStore.user) {
      return next({ name: 'Login', query: { redirect: to.fullPath } })
    }
  }
  // Keep admins in the Admin shell for self-service pages (same sidebar as other admin routes)
  const adminWorkspacePaths = ['/my-audits', '/my-exceptions', '/profile']
  if (authStore.canAccessAdmin && adminWorkspacePaths.includes(to.path)) {
    return next({ path: `/admin${to.path}`, query: to.query, hash: to.hash, replace: true })
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
