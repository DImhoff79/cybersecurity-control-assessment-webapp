<template>
  <div>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Issue Program Hub</h1>
        <p class="text-muted mb-0">
          One audit context for findings, control exceptions, application-scoped risks, and remediation plans.
        </p>
      </div>
      <div class="d-flex gap-2 flex-wrap">
        <router-link class="btn btn-outline-secondary btn-sm" to="/admin/findings">Findings</router-link>
        <router-link class="btn btn-outline-secondary btn-sm" to="/admin/control-exceptions">Exceptions</router-link>
      </div>
    </div>

    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <div class="row g-2 align-items-end">
          <div class="col-md-6">
            <label class="form-label small mb-1">Audit</label>
            <select v-model.number="selectedAuditId" class="form-select" @change="persistAuditQuery">
              <option :value="null">Select an audit…</option>
              <option v-for="a in audits" :key="a.id" :value="a.id">
                {{ a.applicationName }} ({{ a.year }})
              </option>
            </select>
          </div>
          <div class="col-md-6" v-if="selectedAudit">
            <div class="small text-muted">
              Application context for risk &amp; remediation deep links:
              <strong>{{ selectedAudit.applicationName }}</strong>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!selectedAuditId" class="alert alert-info py-2">
      Choose an audit to load findings, exceptions, and (if permitted) risks and plans for that application.
    </div>

    <template v-else>
      <div v-if="loading" class="text-muted small mb-2">Loading program data…</div>
      <div v-if="loadError" class="alert alert-danger py-2 d-flex justify-content-between align-items-center gap-2">
        <span>{{ loadError }}</span>
        <button type="button" class="btn btn-outline-danger btn-sm" @click="reload">Retry</button>
      </div>

      <div class="alert alert-light border mb-3 d-flex flex-wrap justify-content-between align-items-center gap-2">
        <div>
          <span class="fw-semibold">{{ selectedAudit?.applicationName }}</span>
          <span class="text-muted"> — year {{ selectedAudit?.year }}, audit #{{ selectedAuditId }}</span>
        </div>
        <router-link class="btn btn-sm btn-outline-primary" :to="`/admin/audits/${selectedAuditId}`">
          Open audit workspace
        </router-link>
      </div>

      <div class="row g-3 mb-3">
        <div v-for="card in summaryCards" :key="card.key" class="col-md-3">
          <div class="card shadow-sm h-100">
            <div class="card-body">
              <div class="text-muted small">{{ card.label }}</div>
              <div class="h4 mb-1">{{ card.value }}</div>
              <router-link v-if="card.to" :to="card.to" class="small">{{ card.linkLabel }}</router-link>
            </div>
          </div>
        </div>
      </div>

      <div class="row g-3">
        <div class="col-lg-6">
          <div class="card shadow-sm h-100">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <h2 class="h6 mb-0">Findings (active)</h2>
                <router-link class="btn btn-sm btn-outline-primary" :to="deepLinkFindings">Manage all</router-link>
              </div>
              <p v-if="!activeFindings.length" class="text-muted small mb-0">No open or in-progress findings for this audit.</p>
              <ul v-else class="list-group list-group-flush small">
                <li
                  v-for="f in activeFindingsPreview"
                  :key="f.id"
                  class="list-group-item px-0 d-flex justify-content-between align-items-start gap-2"
                >
                  <div>
                    <div class="fw-semibold">{{ f.title }}</div>
                    <div class="text-muted">{{ f.severity }} · {{ formatFindingStatus(f.status) }}</div>
                  </div>
                  <div class="text-nowrap">
                    <router-link class="btn btn-sm btn-outline-secondary py-0" :to="riskLink(f)">Risk</router-link>
                    <router-link class="btn btn-sm btn-outline-secondary py-0 ms-1" :to="remediationLink(f)">Plan</router-link>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>
        <div class="col-lg-6">
          <div class="card shadow-sm h-100">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <h2 class="h6 mb-0">Control exceptions</h2>
                <router-link class="btn btn-sm btn-outline-primary" :to="deepLinkExceptions">Manage all</router-link>
              </div>
              <p v-if="!exceptions.length" class="text-muted small mb-0">No exceptions for this audit.</p>
              <ul v-else class="list-group list-group-flush small">
                <li
                  v-for="ex in exceptionsPreview"
                  :key="ex.id"
                  class="list-group-item px-0 d-flex justify-content-between align-items-start gap-2"
                >
                  <div>
                    <div class="fw-semibold">{{ ex.controlId ? `${ex.controlId} — ${ex.controlName}` : 'General exception' }}</div>
                    <div class="text-muted">{{ ex.status }} · SLA {{ ex.slaState || '—' }}</div>
                  </div>
                  <span class="badge" :class="exceptionStatusClass(ex.status)">{{ ex.status }}</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <div v-if="authStore.hasPermission('RISK_MANAGEMENT') || authStore.hasPermission('REMEDIATION_MANAGEMENT')" class="row g-3 mt-1">
        <div v-if="authStore.hasPermission('RISK_MANAGEMENT')" class="col-md-6">
          <div class="card shadow-sm">
            <div class="card-body">
              <h2 class="h6 mb-2">Risks for this application</h2>
              <p class="small text-muted mb-2">
                {{ appRisks.length }} register item(s) attributed to
                <strong>{{ selectedAudit?.applicationName }}</strong>.
              </p>
              <router-link
                class="btn btn-sm btn-outline-primary"
                :to="riskRegisterDeepLink"
              >
                Open risk register (filtered)
              </router-link>
            </div>
          </div>
        </div>
        <div v-if="authStore.hasPermission('REMEDIATION_MANAGEMENT')" class="col-md-6">
          <div class="card shadow-sm">
            <div class="card-body">
              <h2 class="h6 mb-2">Remediation plans</h2>
              <p class="small text-muted mb-2">
                {{ appRemediationPlans.length }} plan(s) linked to risks for this application.
              </p>
              <router-link
                class="btn btn-sm btn-outline-primary"
                :to="remediationDeepLink"
              >
                Open remediation (filtered)
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../services/api'
import { toastError } from '../../services/toast'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const audits = ref([])
const findings = ref([])
const exceptions = ref([])
const risks = ref([])
const plans = ref([])
const loading = ref(false)
const loadError = ref('')
const selectedAuditId = ref(null)

const selectedAudit = computed(() => audits.value.find((a) => a.id === selectedAuditId.value) || null)

const applicationId = computed(() => selectedAudit.value?.applicationId ?? null)

const activeFindings = computed(() =>
  (findings.value || []).filter((f) => f.status === 'OPEN' || f.status === 'IN_PROGRESS')
)

const activeFindingsPreview = computed(() => {
  const rank = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 }
  return [...activeFindings.value]
    .sort((a, b) => (rank[a.severity] ?? 9) - (rank[b.severity] ?? 9))
    .slice(0, 8)
})

const exceptionsPreview = computed(() => (exceptions.value || []).slice(0, 8))

const appRisks = computed(() => {
  if (!applicationId.value) return []
  return (risks.value || []).filter((r) => r.applicationId === applicationId.value)
})

const appRiskIds = computed(() => new Set(appRisks.value.map((r) => r.id)))

const appRemediationPlans = computed(() =>
  (plans.value || []).filter((p) => p.riskId && appRiskIds.value.has(p.riskId))
)

const deepLinkFindings = computed(() => ({
  path: '/admin/findings',
  query: { auditId: String(selectedAuditId.value) }
}))

const deepLinkExceptions = computed(() => ({
  path: '/admin/control-exceptions',
  query: { auditId: String(selectedAuditId.value) }
}))

const riskRegisterDeepLink = computed(() => {
  const q = { applicationId: String(applicationId.value) }
  if (selectedAudit.value?.applicationName) q.applicationName = selectedAudit.value.applicationName
  return { path: '/admin/risk-register', query: q }
})

const remediationDeepLink = computed(() => {
  const q = { applicationId: String(applicationId.value) }
  if (selectedAudit.value?.applicationName) q.applicationName = selectedAudit.value.applicationName
  return { path: '/admin/remediation-plans', query: q }
})

const summaryCards = computed(() => {
  const pendingEx = (exceptions.value || []).filter((e) => e.status === 'REQUESTED').length
  const cards = [
    {
      key: 'findings',
      label: 'Active findings',
      value: activeFindings.value.length,
      to: deepLinkFindings.value,
      linkLabel: 'View findings →'
    },
    {
      key: 'exceptions',
      label: 'Exceptions (total)',
      value: (exceptions.value || []).length,
      to: deepLinkExceptions.value,
      linkLabel: 'View exceptions →'
    },
    {
      key: 'risks',
      label: 'App risks',
      value: authStore.hasPermission('RISK_MANAGEMENT') ? appRisks.value.length : '—',
      to: authStore.hasPermission('RISK_MANAGEMENT') ? riskRegisterDeepLink.value : null,
      linkLabel: authStore.hasPermission('RISK_MANAGEMENT') ? 'Risk register →' : ''
    },
    {
      key: 'plans',
      label: 'Remediation plans',
      value: authStore.hasPermission('REMEDIATION_MANAGEMENT') ? appRemediationPlans.value.length : '—',
      to: authStore.hasPermission('REMEDIATION_MANAGEMENT') ? remediationDeepLink.value : null,
      linkLabel: authStore.hasPermission('REMEDIATION_MANAGEMENT') ? 'Remediation →' : ''
    }
  ]
  return cards
})

function persistAuditQuery() {
  const q = { ...route.query }
  if (selectedAuditId.value) q.auditId = String(selectedAuditId.value)
  else delete q.auditId
  router.replace({ path: route.path, query: q })
}

function syncAuditFromRoute() {
  const raw = route.query.auditId
  if (raw != null && raw !== '') {
    const n = Number(raw)
    selectedAuditId.value = Number.isNaN(n) ? null : n
  } else {
    selectedAuditId.value = null
  }
}

async function loadAudits() {
  try {
    const res = await api.get('/api/my-audits')
    audits.value = res.data || []
  } catch (e) {
    loadError.value = e.response?.data?.error || 'Failed to load audits.'
    toastError(loadError.value)
  }
}

async function reload() {
  if (!selectedAuditId.value) return
  loading.value = true
  loadError.value = ''
  try {
    const auditId = selectedAuditId.value
    const [fRes, xRes] = await Promise.all([
      api.get('/api/findings', { params: { auditId } }),
      api.get('/api/admin/control-exceptions', { params: { auditId } })
    ])
    findings.value = fRes.data || []
    exceptions.value = xRes.data || []

    const extras = []
    if (authStore.hasPermission('RISK_MANAGEMENT')) {
      extras.push(
        api.get('/api/risks').then((r) => {
          risks.value = r.data || []
        })
      )
    } else {
      risks.value = []
    }
    if (authStore.hasPermission('REMEDIATION_MANAGEMENT')) {
      extras.push(
        api.get('/api/remediation-plans').then((r) => {
          plans.value = r.data || []
        })
      )
    } else {
      plans.value = []
    }
    await Promise.all(extras)
  } catch (e) {
    loadError.value = e.response?.data?.error || 'Failed to load program data.'
    toastError(loadError.value)
  } finally {
    loading.value = false
  }
}

watch(
  () => route.query.auditId,
  () => {
    syncAuditFromRoute()
    if (selectedAuditId.value) reload()
    else {
      findings.value = []
      exceptions.value = []
      risks.value = []
      plans.value = []
    }
  }
)

onMounted(async () => {
  await loadAudits()
  syncAuditFromRoute()
  if (selectedAuditId.value) await reload()
})

function formatFindingStatus(status) {
  if (status === 'IN_PROGRESS') return 'In progress'
  if (status === 'OPEN') return 'Open'
  return status || '—'
}

function riskLink(finding) {
  const params = new URLSearchParams()
  params.set('findingId', String(finding.id))
  if (finding.title) params.set('findingTitle', finding.title)
  if (finding.applicationName) params.set('applicationName', finding.applicationName)
  if (finding.auditId) params.set('auditId', String(finding.auditId))
  return `/admin/risk-register?${params.toString()}`
}

function remediationLink(finding) {
  const params = new URLSearchParams()
  params.set('findingId', String(finding.id))
  if (finding.title) params.set('findingTitle', finding.title)
  if (finding.applicationName) params.set('applicationName', finding.applicationName)
  if (finding.auditId) params.set('auditId', String(finding.auditId))
  return `/admin/remediation-plans?${params.toString()}`
}

function exceptionStatusClass(status) {
  switch (status) {
    case 'APPROVED': return 'text-bg-success'
    case 'REJECTED': return 'text-bg-danger'
    case 'EXPIRED': return 'text-bg-secondary'
    default: return 'text-bg-warning'
  }
}
</script>
