<template>
  <div class="program-home">
    <div class="mb-4">
      <h1 class="h3 mb-1">Audit program home</h1>
      <p class="text-muted mb-0">
        One snapshot across applications, audits, findings, and risks—each link opens the same registers your team uses everywhere else.
      </p>
    </div>

    <div v-if="loading" class="text-muted">Loading program snapshot…</div>
    <template v-else>
      <h2 class="h6 text-uppercase text-muted mb-2">Program health</h2>
      <div class="row g-2 mb-3">
        <div v-for="c in summaryCards" :key="c.key" class="col-6 col-md-4 col-lg-3">
          <div class="card border-0 shadow-sm h-100">
            <div class="card-body py-3">
              <div class="text-muted small">{{ c.label }}</div>
              <div class="h3 mb-0">{{ c.value }}</div>
            </div>
          </div>
        </div>
        <div v-if="openFindingsCount != null" class="col-6 col-md-4 col-lg-3">
          <div class="card border-0 shadow-sm h-100">
            <div class="card-body py-3">
              <div class="text-muted small">Open findings</div>
              <div class="h3 mb-0">{{ openFindingsCount }}</div>
            </div>
          </div>
        </div>
        <div v-if="activeRisksCount != null" class="col-6 col-md-4 col-lg-3">
          <div class="card border-0 shadow-sm h-100">
            <div class="card-body py-3">
              <div class="text-muted small">Active risks</div>
              <div class="h3 mb-0">{{ activeRisksCount }}</div>
            </div>
          </div>
        </div>
      </div>

      <h2 class="h6 text-uppercase text-muted mb-2">Where to go next</h2>
      <div class="row g-2">
        <div v-for="link in quickLinks" :key="link.to" class="col-12 col-md-6 col-xl-4">
          <router-link :to="link.to" class="card border-0 shadow-sm text-decoration-none h-100 program-home-link">
            <div class="card-body">
              <div class="fw-semibold text-body">{{ link.title }}</div>
              <p class="small text-muted mb-0">{{ link.description }}</p>
            </div>
          </router-link>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../../services/api'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const loading = ref(true)
const summary = ref(null)
const openFindingsCount = ref(null)
const activeRisksCount = ref(null)

const canAuditManagement = computed(() => authStore.hasPermission('AUDIT_MANAGEMENT'))
const hasReportView = computed(() => authStore.hasPermission('REPORT_VIEW'))

onMounted(async () => {
  try {
    const summaryRes = await api.get('/api/reports/summary')
    summary.value = summaryRes.data
    const extra = []
    if (hasReportView.value) {
      extra.push(
        api.get('/api/findings').then((r) => {
          const findings = r.data || []
          openFindingsCount.value = findings.filter((f) => f.status === 'OPEN' || f.status === 'IN_PROGRESS').length
        })
      )
    }
    if (hasReportView.value) {
      extra.push(
        api.get('/api/risks').then((r) => {
          const risks = r.data || []
          activeRisksCount.value = risks.filter((x) => x.status === 'OPEN' || x.status === 'MONITORING').length
        })
      )
    }
    await Promise.all(extra)
  } catch {
    summary.value = null
  } finally {
    loading.value = false
  }
})

const summaryCards = computed(() => {
  const s = summary.value
  if (!s) return []
  return [
    { key: 'apps', label: 'Applications', value: s.totalApplications },
    { key: 'projects', label: 'Audit projects (active)', value: s.activeAuditProjects },
    { key: 'audits', label: 'Total audits', value: s.totalAudits },
    { key: 'open', label: 'Open audits', value: s.openAudits },
    { key: 'overdue', label: 'Overdue audits', value: s.overdueAudits },
    { key: 'submitted', label: 'Submitted for review', value: s.submittedAudits }
  ]
})

const quickLinks = computed(() => {
  const links = []
  if (authStore.hasPermission('APPLICATION_MANAGEMENT')) {
    links.push({
      to: '/admin/applications',
      title: 'Applications',
      description: 'Inventory, owners, and intake context.'
    })
  }
  if (hasReportView.value) {
    links.push(
      {
        to: '/admin/audit-projects',
        title: 'Audit projects',
        description: 'Programs and scoped applications.'
      },
      {
        to: '/admin/operations',
        title: 'Audit queue',
        description: 'Triage, assignments, and reviewer workload.'
      },
      {
        to: '/admin/reports',
        title: 'Reports & metrics',
        description: 'Trends, exports, and board pack.'
      },
      {
        to: '/admin/findings',
        title: 'Findings',
        description: 'Open and in-progress findings (scoped to audits you can access).'
      },
      {
        to: '/admin/issue-program',
        title: 'Issue program hub',
        description: 'Findings and exceptions in one place for an audit.'
      },
      {
        to: '/admin/control-exceptions',
        title: 'Control exceptions',
        description: 'Exception requests; managers approve or reject program-wide.'
      }
    )
  }
  if (canAuditManagement.value) {
    links.push(
      {
        to: '/admin/audits',
        title: 'Kick off audits',
        description: 'Create and assign audits.'
      },
      {
        to: '/admin/questionnaire',
        title: 'Questionnaire & controls',
        description: 'CCF content and mappings.'
      }
    )
  }
  if (hasReportView.value) {
    links.push({
      to: '/admin/risk-register',
      title: 'Risk register',
      description: 'Enterprise and application-scoped risks (read-only without Risk Management).'
    })
  }
  return links
})
</script>

<style scoped>
.program-home-link:hover {
  background: var(--bs-light, #f8f9fa);
}
</style>
