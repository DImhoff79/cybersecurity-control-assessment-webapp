<template>
  <div>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Audit program reports</h1>
        <p class="text-muted mb-0">How the program is doing and what deserves attention next.</p>
      </div>
      <div class="d-flex gap-2 flex-wrap">
        <button type="button" class="btn btn-outline-primary btn-sm" data-testid="export-audits-csv" @click="downloadAuditsCsv">
          Export Audits CSV
        </button>
        <button type="button" class="btn btn-primary btn-sm" data-testid="export-board-pack-pdf" @click="downloadBoardPackPdf">
          Export Board Pack PDF
        </button>
      </div>
    </div>

    <div v-if="loading" class="text-muted">Loading...</div>
    <div v-else>
      <h2 class="h6 text-uppercase text-muted mb-2">How we're doing</h2>
      <div class="row g-2 mb-2">
        <div class="col-6 col-lg-3" v-for="card in healthCards" :key="card.label">
          <div
            class="card shadow-sm h-100"
            :class="{ 'border-danger': card.variant === 'danger', 'border-warning': card.variant === 'warning' }"
          >
            <div class="card-body py-3">
              <div class="text-muted small">{{ card.label }}</div>
              <div class="h3 mb-0">{{ card.value }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="row g-2 mb-3">
        <div class="col-md-4" v-for="card in riskComplianceCards" :key="card.label">
          <div
            class="card shadow-sm h-100"
            :class="{ 'border-danger': card.variant === 'danger', 'border-warning': card.variant === 'warning' }"
          >
            <div class="card-body py-3">
              <div class="text-muted small">{{ card.label }}</div>
              <div class="h3 mb-0">{{ card.value }}</div>
            </div>
          </div>
        </div>
      </div>
      <p class="small text-muted mb-4 border-start border-3 ps-2" :class="healthBorderClass">{{ healthNarrative }}</p>

      <div class="card shadow-sm mb-3">
        <div class="card-body">
          <h2 class="h5 mb-3">What to watch</h2>
          <ul v-if="actionItems.length" class="mb-0 ps-3">
            <li v-for="(item, idx) in actionItems" :key="idx" class="mb-2">
              {{ item.text }}
              <router-link v-if="item.to" :to="item.to" class="ms-1">{{ item.linkLabel }}</router-link>
            </li>
          </ul>
          <p v-else class="text-muted mb-0">No urgent items on this snapshot — keep monitoring scheduled work.</p>
        </div>
      </div>

      <div class="mb-3">
        <h2 class="h6 text-uppercase text-muted mb-2">Quick links</h2>
        <div class="d-flex gap-2 flex-wrap">
          <router-link to="/admin/operations" class="btn btn-outline-primary btn-sm">Audit Queue</router-link>
          <router-link to="/admin/risk-register" class="btn btn-outline-primary btn-sm">Risk Register</router-link>
          <router-link to="/admin/remediation-plans" class="btn btn-outline-primary btn-sm">Remediation Plans</router-link>
          <router-link to="/admin/policy-attestations" class="btn btn-outline-primary btn-sm">Policy Attestations</router-link>
        </div>
      </div>

      <details class="card shadow-sm mb-3">
        <summary class="card-header user-select-none">Email schedules</summary>
        <div class="card-body border-top">
          <p class="small text-muted mb-3">Recurring exports to stakeholders. One-time downloads use the buttons above.</p>
          <form class="row g-2 mb-3" @submit.prevent="createSchedule">
            <div class="col-md-3">
              <input v-model="scheduleForm.name" class="form-control form-control-sm" placeholder="Schedule name" required />
            </div>
            <div class="col-md-2">
              <select v-model="scheduleForm.reportType" class="form-select form-select-sm">
                <option value="AUDITS_CSV">Audits CSV</option>
                <option value="RECENT_ACTIVITY_CSV">Recent Activity CSV</option>
              </select>
            </div>
            <div class="col-md-2">
              <select v-model="scheduleForm.frequency" class="form-select form-select-sm">
                <option value="DAILY">Daily</option>
                <option value="WEEKLY">Weekly</option>
                <option value="MONTHLY">Monthly</option>
              </select>
            </div>
            <div class="col-md-3">
              <input v-model="scheduleForm.recipientEmails" class="form-control form-control-sm" placeholder="Recipients (comma-separated)" required />
            </div>
            <div class="col-md-2">
              <button type="submit" class="btn btn-outline-primary btn-sm w-100">Create</button>
            </div>
          </form>
          <div v-if="schedules.length" class="table-responsive">
            <table class="table table-striped mb-0 table-sm">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Frequency</th>
                  <th>Recipients</th>
                  <th>Next run</th>
                  <th>Last run</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in schedules" :key="s.id">
                  <td>{{ s.name }}</td>
                  <td>{{ s.reportType }}</td>
                  <td>{{ s.frequency }}</td>
                  <td>{{ s.recipientEmails }}</td>
                  <td>{{ formatDateTime(s.nextRunAt) }}</td>
                  <td>{{ formatDateTime(s.lastRunAt) }}</td>
                  <td>{{ s.lastRunStatus || '-' }}</td>
                  <td><button type="button" class="btn btn-outline-danger btn-sm" @click="deleteSchedule(s.id)">Delete</button></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="text-muted small">No schedules configured.</div>
        </div>
      </details>

      <details class="card shadow-sm mb-3">
        <summary class="card-header user-select-none">Trends &amp; breakdowns</summary>
        <div class="card-body border-top">
          <h3 class="h6 mb-2">Trend by audit year</h3>
          <div class="small text-muted mb-2">Completion, open, and overdue trends.</div>
          <div class="trend-chart-wrap mb-4">
            <svg viewBox="0 0 700 220" class="trend-chart">
              <line x1="40" y1="180" x2="680" y2="180" stroke="#9ca3af" />
              <line x1="40" y1="20" x2="40" y2="180" stroke="#9ca3af" />
              <polyline :points="seriesPoints('complete')" fill="none" stroke="#198754" stroke-width="3" />
              <polyline :points="seriesPoints('open')" fill="none" stroke="#0d6efd" stroke-width="3" />
              <polyline :points="seriesPoints('overdue')" fill="none" stroke="#dc3545" stroke-width="3" />
              <g v-for="p in trendPlotPoints" :key="`x-${p.year}`">
                <text :x="p.x - 10" y="198" font-size="11" fill="#6b7280">{{ p.year }}</text>
                <circle :cx="p.x" :cy="p.completeY" r="3" fill="#198754" />
                <circle :cx="p.x" :cy="p.openY" r="3" fill="#0d6efd" />
                <circle :cx="p.x" :cy="p.overdueY" r="3" fill="#dc3545" />
              </g>
            </svg>
          </div>
          <div class="d-flex gap-3 small mb-4">
            <span><span class="trend-dot trend-complete"></span>Completed</span>
            <span><span class="trend-dot trend-open"></span>Open</span>
            <span><span class="trend-dot trend-overdue"></span>Overdue</span>
          </div>

          <div v-if="projectHotspots.length" class="mb-4">
            <h3 class="h6 mb-2">Busy projects</h3>
            <div v-for="hotspot in projectHotspots" :key="hotspot.projectId" class="border rounded p-2 mb-2">
              <div class="fw-semibold">{{ hotspot.projectName }}</div>
              <div class="small text-muted">
                Open audits: {{ hotspot.openAudits }} · Overdue load signal: {{ hotspot.overdueAudits }}
              </div>
            </div>
          </div>

          <h3 class="h6 mb-2">By year</h3>
          <div class="table-responsive mb-4">
            <table class="table table-striped mb-0 table-sm">
              <thead>
                <tr>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByYearSort('year')">Year {{ byYearSortIndicator('year') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByYearSort('total')">Total {{ byYearSortIndicator('total') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByYearSort('draft')">Draft {{ byYearSortIndicator('draft') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByYearSort('inProgress')">In Progress {{ byYearSortIndicator('inProgress') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByYearSort('submitted')">Submitted {{ byYearSortIndicator('submitted') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByYearSort('attested')">Attested {{ byYearSortIndicator('attested') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByYearSort('complete')">Complete {{ byYearSortIndicator('complete') }}</button></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in sortedByYear" :key="row.year">
                  <td>{{ row.year }}</td>
                  <td>{{ row.total }}</td>
                  <td>{{ row.draft }}</td>
                  <td>{{ row.inProgress }}</td>
                  <td>{{ row.submitted }}</td>
                  <td>{{ row.attested }}</td>
                  <td>{{ row.complete }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <h3 class="h6 mb-2">By audit project</h3>
          <div class="table-responsive">
            <table class="table table-striped mb-0 table-sm">
              <thead>
                <tr>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByProjectSort('project')">Project {{ byProjectSortIndicator('project') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByProjectSort('framework')">Framework {{ byProjectSortIndicator('framework') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByProjectSort('year')">Year {{ byProjectSortIndicator('year') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByProjectSort('scopedApplications')">Apps {{ byProjectSortIndicator('scopedApplications') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByProjectSort('totalAudits')">Total Audits {{ byProjectSortIndicator('totalAudits') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByProjectSort('openAudits')">Open {{ byProjectSortIndicator('openAudits') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByProjectSort('submittedAudits')">Submitted {{ byProjectSortIndicator('submittedAudits') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByProjectSort('attestedAudits')">Attested {{ byProjectSortIndicator('attestedAudits') }}</button></th>
                  <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleByProjectSort('completeAudits')">Complete {{ byProjectSortIndicator('completeAudits') }}</button></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in sortedByProject" :key="row.projectId">
                  <td>{{ row.projectName }}</td>
                  <td>{{ row.frameworkTag || '-' }}</td>
                  <td>{{ row.year }}</td>
                  <td>{{ row.scopedApplications }}</td>
                  <td>{{ row.totalAudits }}</td>
                  <td>{{ row.openAudits }}</td>
                  <td>{{ row.submittedAudits }}</td>
                  <td>{{ row.attestedAudits }}</td>
                  <td>{{ row.completeAudits }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </details>

      <details class="card shadow-sm">
        <summary class="card-header user-select-none">Full program inventory</summary>
        <div class="card-body border-top">
          <p class="small text-muted mb-3">Coverage and volume metrics for deep dives.</p>
          <div class="table-responsive">
            <table class="table table-sm mb-0">
              <tbody>
                <tr v-for="row in inventoryRows" :key="row.label">
                  <td class="text-muted">{{ row.label }}</td>
                  <td class="fw-semibold text-end">{{ row.value }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </details>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'
import { useTableSort } from '../../composables/useTableSort'

const loading = ref(true)
const summary = ref(null)
const byYear = ref([])
const trends = ref([])
const byProject = ref([])
const complianceKpis = ref({})
const riskKpis = ref({})
const schedules = ref([])
const scheduleForm = ref({
  name: '',
  reportType: 'AUDITS_CSV',
  frequency: 'WEEKLY',
  recipientEmails: ''
})

const { sortedRows: sortedByYear, toggleSort: toggleByYearSort, sortIndicator: byYearSortIndicator } = useTableSort(byYear, {
  initialKey: 'year'
})

const { sortedRows: sortedByProject, toggleSort: toggleByProjectSort, sortIndicator: byProjectSortIndicator } = useTableSort(byProject, {
  initialKey: 'project',
  valueGetters: {
    project: (row) => row.projectName || '',
    framework: (row) => row.frameworkTag || ''
  }
})

const healthCards = computed(() => {
  const s = summary.value || {}
  return [
    { label: 'Open audits', value: s.openAudits ?? 0, variant: 'default' },
    { label: 'Overdue', value: s.overdueAudits ?? 0, variant: (s.overdueAudits ?? 0) > 0 ? 'danger' : 'default' },
    { label: 'Awaiting review', value: s.submittedAudits ?? 0, variant: 'default' },
    { label: 'Completed', value: s.completedAudits ?? 0, variant: 'default' }
  ]
})

const riskComplianceCards = computed(() => {
  const rk = riskKpis.value || {}
  const ck = complianceKpis.value || {}
  return [
    { label: 'High residual risks', value: rk.highRisks ?? 0, variant: (rk.highRisks ?? 0) > 0 ? 'danger' : 'default' },
    { label: 'Overdue remediation', value: rk.overdueRemediationActions ?? 0, variant: (rk.overdueRemediationActions ?? 0) > 0 ? 'warning' : 'default' },
    { label: 'Pending attestations', value: ck.pendingAttestations ?? 0, variant: (ck.pendingAttestations ?? 0) > 0 ? 'warning' : 'default' }
  ]
})

const inventoryRows = computed(() => {
  const s = summary.value || {}
  const ck = complianceKpis.value || {}
  const rk = riskKpis.value || {}
  return [
    { label: 'Applications', value: s.totalApplications ?? 0 },
    { label: 'Audit projects', value: s.totalAuditProjects ?? 0 },
    { label: 'Active projects', value: s.activeAuditProjects ?? 0 },
    { label: 'Total audits', value: s.totalAudits ?? 0 },
    { label: 'Attested', value: s.attestedAudits ?? 0 },
    { label: 'Policies', value: ck.totalPolicies ?? 0 },
    { label: 'Req control coverage', value: `${ck.controlCoveragePct ?? 0}%` },
    { label: 'Req policy coverage', value: `${ck.policyCoveragePct ?? 0}%` },
    { label: 'Open risks', value: rk.openRisks ?? 0 }
  ]
})

const healthNarrative = computed(() => {
  const s = summary.value || {}
  const overdue = s.overdueAudits ?? 0
  const submitted = s.submittedAudits ?? 0
  const rk = riskKpis.value || {}
  const ck = complianceKpis.value || {}
  if (overdue > 0) {
    return `Attention: ${overdue} audit(s) are past due — prioritize dates and ownership before backlog grows.`
  }
  if (submitted > 0) {
    return `${submitted} audit(s) are submitted and waiting for review or attestation.`
  }
  if ((rk.overdueRemediationActions ?? 0) > 0 || (ck.pendingAttestations ?? 0) > 0 || (rk.highRisks ?? 0) > 0) {
    return 'Risk and compliance follow-ups below need steady attention even when audits look healthy.'
  }
  return 'Program posture looks steady on this snapshot — keep exports and schedules aligned with leadership asks.'
})

const healthBorderClass = computed(() => {
  const s = summary.value || {}
  if ((s.overdueAudits ?? 0) > 0) return 'border-danger'
  const rk = riskKpis.value || {}
  const ck = complianceKpis.value || {}
  if ((rk.highRisks ?? 0) > 0 || (rk.overdueRemediationActions ?? 0) > 0 || (ck.pendingAttestations ?? 0) > 0) return 'border-warning'
  return 'border-success'
})

const projectHotspots = computed(() => {
  return (byProject.value || [])
    .map((row) => ({
      projectId: row.projectId,
      projectName: row.projectName,
      openAudits: row.openAudits || 0,
      overdueAudits: Math.max(0, (row.openAudits || 0) - (row.submittedAudits || 0) - (row.attestedAudits || 0))
    }))
    .sort((a, b) => b.openAudits + b.overdueAudits - (a.openAudits + a.overdueAudits))
    .slice(0, 3)
})

const actionItems = computed(() => {
  const items = []
  const s = summary.value || {}
  const rk = riskKpis.value || {}
  const ck = complianceKpis.value || {}
  if ((s.overdueAudits ?? 0) > 0) {
    items.push({
      text: `${s.overdueAudits} audit(s) are overdue.`,
      to: '/admin/operations',
      linkLabel: 'Triage in Audit Queue'
    })
  }
  if ((s.submittedAudits ?? 0) > 0) {
    items.push({
      text: `${s.submittedAudits} audit(s) are submitted and need review or attestation.`,
      to: '/admin/operations',
      linkLabel: 'Review in Audit Queue'
    })
  }
  if ((rk.highRisks ?? 0) > 0) {
    items.push({
      text: `${rk.highRisks} high residual risk item(s) on the register.`,
      to: '/admin/risk-register',
      linkLabel: 'Open risk register'
    })
  }
  if ((rk.overdueRemediationActions ?? 0) > 0) {
    items.push({
      text: `${rk.overdueRemediationActions} remediation action(s) are overdue.`,
      to: '/admin/remediation-plans',
      linkLabel: 'Remediation plans'
    })
  }
  if ((ck.pendingAttestations ?? 0) > 0) {
    items.push({
      text: `${ck.pendingAttestations} policy attestation(s) are still pending.`,
      to: '/admin/policy-attestations',
      linkLabel: 'Policy attestations'
    })
  }
  const ordered = (trends.value || []).slice().sort((a, b) => a.year - b.year)
  if (ordered.length >= 2) {
    const current = ordered[ordered.length - 1] || {}
    const previous = ordered[ordered.length - 2] || {}
    const overdueDelta = (current.overdue || 0) - (previous.overdue || 0)
    if (overdueDelta > 0) {
      items.push({
        text: `Overdue audits vs prior year: +${overdueDelta} (year-over-year).`,
        to: null,
        linkLabel: null
      })
    }
  }
  const hotspots = projectHotspots.value || []
  if (hotspots.length && hotspots[0].openAudits > 0) {
    const h = hotspots[0]
    items.push({
      text: `Busiest project right now: ${h.projectName} (${h.openAudits} open audits).`,
      to: '/admin/operations',
      linkLabel: 'Go to Audit Queue'
    })
  }
  return items
})

onMounted(async () => {
  try {
    const [res, byYearRes, trendsRes, byProjectRes, schedulesRes, complianceRes, riskRes] = await Promise.all([
      api.get('/api/reports/summary'),
      api.get('/api/reports/by-year'),
      api.get('/api/reports/trends'),
      api.get('/api/reports/by-project'),
      api.get('/api/reports/schedules'),
      api.get('/api/reports/compliance-kpis'),
      api.get('/api/reports/risk-kpis')
    ])
    summary.value = res.data || {}
    byYear.value = byYearRes.data || []
    trends.value = trendsRes.data || []
    byProject.value = byProjectRes.data || []
    schedules.value = Array.isArray(schedulesRes.data) ? schedulesRes.data : []
    complianceKpis.value = complianceRes.data || {}
    riskKpis.value = riskRes.data || {}
  } finally {
    loading.value = false
  }
})

const trendPlotPoints = computed(() => {
  const rows = (trends.value || []).slice().sort((a, b) => a.year - b.year)
  if (!rows.length) return []
  const minX = 60
  const maxX = 660
  const minY = 30
  const maxY = 170
  const span = Math.max(1, rows.length - 1)
  const maxValue = Math.max(
    1,
    ...rows.map((r) => Math.max(r.complete || 0, r.open || 0, r.overdue || 0, r.total || 0))
  )
  return rows.map((r, i) => {
    const x = minX + (i * (maxX - minX)) / span
    const yFor = (v) => maxY - ((v || 0) / maxValue) * (maxY - minY)
    return {
      year: r.year,
      x,
      completeY: yFor(r.complete),
      openY: yFor(r.open),
      overdueY: yFor(r.overdue)
    }
  })
})

function seriesPoints(kind) {
  const points = trendPlotPoints.value
  if (!points.length) return ''
  if (kind === 'complete') return points.map((p) => `${p.x},${p.completeY}`).join(' ')
  if (kind === 'open') return points.map((p) => `${p.x},${p.openY}`).join(' ')
  return points.map((p) => `${p.x},${p.overdueY}`).join(' ')
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

async function createSchedule() {
  try {
    await api.post('/api/reports/schedules', {
      ...scheduleForm.value
    })
    scheduleForm.value.name = ''
    scheduleForm.value.recipientEmails = ''
    const schedulesRes = await api.get('/api/reports/schedules')
    schedules.value = Array.isArray(schedulesRes.data) ? schedulesRes.data : []
    toastSuccess('Report schedule created.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create schedule')
  }
}

async function deleteSchedule(id) {
  try {
    await api.delete(`/api/reports/schedules/${id}`)
    schedules.value = schedules.value.filter((s) => s.id !== id)
    toastSuccess('Report schedule deleted.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to delete schedule')
  }
}

async function downloadAuditsCsv() {
  try {
    const res = await api.get('/api/reports/audits.csv', { responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = 'audits-report.csv'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to export report')
  }
}

async function downloadBoardPackPdf() {
  try {
    const res = await api.get('/api/reports/board-pack.pdf', { responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const a = document.createElement('a')
    a.href = url
    a.download = 'audit-board-pack.pdf'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to export board pack')
  }
}
</script>

<style scoped>
.trend-chart-wrap {
  overflow-x: auto;
}
.trend-chart {
  width: 100%;
  min-width: 700px;
  height: 220px;
}
.trend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  margin-right: 4px;
}
.trend-complete {
  background: #198754;
}
.trend-open {
  background: #0d6efd;
}
.trend-overdue {
  background: #dc3545;
}
</style>
