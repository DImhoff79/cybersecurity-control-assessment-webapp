<template>
  <div>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Audit Program Reports</h1>
        <p class="text-muted mb-0">
          Monitor outcomes, export reports, and manage scheduled distribution.
        </p>
      </div>
      <div class="d-flex gap-2 flex-wrap">
        <button class="btn btn-outline-primary btn-sm" @click="downloadAuditsCsv">Export Audits CSV</button>
        <button class="btn btn-primary btn-sm" @click="downloadBoardPackPdf">Export Board Pack PDF</button>
      </div>
    </div>
    <div class="small text-muted mb-3">
      Use Scheduled Exports for recurring distribution and one-time exports for ad hoc requests.
    </div>
    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-2">Manager Drilldowns</h2>
        <p class="small text-muted mb-3">
          Jump directly from KPI context into the operational workspace.
        </p>
        <div class="d-flex gap-2 flex-wrap">
          <router-link to="/admin/operations" class="btn btn-outline-primary btn-sm">Open Operations Queue</router-link>
          <router-link to="/admin/risk-register" class="btn btn-outline-primary btn-sm">Open Risk Register</router-link>
          <router-link to="/admin/remediation-plans" class="btn btn-outline-primary btn-sm">Open Remediation Plans</router-link>
          <router-link to="/admin/policy-attestations" class="btn btn-outline-primary btn-sm">Open Policy Attestations</router-link>
        </div>
      </div>
    </div>
    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Scheduled Exports</h2>
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
          <table class="table table-striped mb-0">
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
                <td><button class="btn btn-outline-danger btn-sm" @click="deleteSchedule(s.id)">Delete</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="text-muted small">No schedules configured.</div>
      </div>
    </div>

    <div v-if="loading" class="text-muted">Loading...</div>
    <div v-else>
      <div class="row g-3 mb-3">
        <div class="col-md-4" v-for="card in cards" :key="card.label">
          <div class="card shadow-sm h-100">
            <div class="card-body">
              <div class="text-muted small">{{ card.label }}</div>
              <div class="display-6 fw-semibold">{{ card.value }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="card shadow-sm mb-3">
        <div class="card-body">
          <h2 class="h5 mb-3">Manager Signals</h2>
          <div class="row g-2">
            <div v-for="signal in managerSignals" :key="signal.label" class="col-md-4">
              <div class="border rounded p-2 h-100">
                <div class="small text-muted">{{ signal.label }}</div>
                <div class="fw-semibold">{{ signal.value }}</div>
                <div class="small" :class="signal.toneClass">{{ signal.tone }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="card shadow-sm mb-3">
        <div class="card-body">
          <h2 class="h5 mb-3">Trend Charts</h2>
          <div class="small text-muted mb-2">Completion, open, and overdue trends by audit year.</div>
          <div class="trend-chart-wrap">
            <svg viewBox="0 0 700 220" class="trend-chart">
              <line x1="40" y1="180" x2="680" y2="180" stroke="#9ca3af" />
              <line x1="40" y1="20" x2="40" y2="180" stroke="#9ca3af" />
              <polyline :points="seriesPoints('complete')" fill="none" stroke="#198754" stroke-width="3" />
              <polyline :points="seriesPoints('open')" fill="none" stroke="#0d6efd" stroke-width="3" />
              <polyline :points="seriesPoints('overdue')" fill="none" stroke="#dc3545" stroke-width="3" />
              <g v-for="(p, idx) in trendPlotPoints" :key="`x-${p.year}`">
                <text :x="p.x - 10" y="198" font-size="11" fill="#6b7280">{{ p.year }}</text>
                <circle :cx="p.x" :cy="p.completeY" r="3" fill="#198754" />
                <circle :cx="p.x" :cy="p.openY" r="3" fill="#0d6efd" />
                <circle :cx="p.x" :cy="p.overdueY" r="3" fill="#dc3545" />
              </g>
            </svg>
          </div>
          <div class="d-flex gap-3 small mt-2">
            <span><span class="trend-dot trend-complete"></span>Completed</span>
            <span><span class="trend-dot trend-open"></span>Open</span>
            <span><span class="trend-dot trend-overdue"></span>Overdue</span>
          </div>
        </div>
      </div>
      <div class="card shadow-sm">
        <div class="card-body">
          <h2 class="h5 mb-3">By Year</h2>
          <div class="table-responsive">
            <table class="table table-striped mb-0">
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
        </div>
      </div>
      <div class="card shadow-sm mb-3">
        <div class="card-body">
          <h2 class="h5 mb-3">By Audit Project</h2>
          <div class="table-responsive">
            <table class="table table-striped mb-0">
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
      </div>
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

const cards = computed(() => {
  const s = summary.value || {}
  return [
    { label: 'Applications', value: s.totalApplications ?? 0 },
    { label: 'Audit Projects', value: s.totalAuditProjects ?? 0 },
    { label: 'Active Projects', value: s.activeAuditProjects ?? 0 },
    { label: 'Total Audits', value: s.totalAudits ?? 0 },
    { label: 'Open Audits', value: s.openAudits ?? 0 },
    { label: 'Overdue Audits', value: s.overdueAudits ?? 0 },
    { label: 'Submitted', value: s.submittedAudits ?? 0 },
    { label: 'Attested', value: s.attestedAudits ?? 0 },
    { label: 'Completed', value: s.completedAudits ?? 0 },
    { label: 'Policies', value: complianceKpis.value.totalPolicies ?? 0 },
    { label: 'Req Control Coverage %', value: `${complianceKpis.value.controlCoveragePct ?? 0}%` },
    { label: 'Req Policy Coverage %', value: `${complianceKpis.value.policyCoveragePct ?? 0}%` },
    { label: 'Pending Attestations', value: complianceKpis.value.pendingAttestations ?? 0 },
    { label: 'Open Risks', value: riskKpis.value.openRisks ?? 0 },
    { label: 'High Risks', value: riskKpis.value.highRisks ?? 0 },
    { label: 'Overdue Remediation Actions', value: riskKpis.value.overdueRemediationActions ?? 0 }
  ]
})

const managerSignals = computed(() => {
  const s = summary.value || {}
  const submitted = s.submittedAudits ?? 0
  const open = s.openAudits ?? 0
  const overdue = s.overdueAudits ?? 0
  const highRisks = riskKpis.value.highRisks ?? 0
  const overdueRemediation = riskKpis.value.overdueRemediationActions ?? 0
  const pendingAttestations = complianceKpis.value.pendingAttestations ?? 0
  return [
    {
      label: 'Audit Throughput',
      value: `${submitted} submitted / ${open} open`,
      tone: overdue > 0 ? `${overdue} overdue audits need escalation` : 'No overdue audits',
      toneClass: overdue > 0 ? 'text-danger' : 'text-success'
    },
    {
      label: 'Risk Exposure',
      value: `${highRisks} high residual risk items`,
      tone: highRisks > 0 ? 'Track risk treatment plans weekly' : 'No high residual risks',
      toneClass: highRisks > 0 ? 'text-danger' : 'text-success'
    },
    {
      label: 'Remediation Execution',
      value: `${overdueRemediation} overdue remediation actions`,
      tone: pendingAttestations > 0
        ? `${pendingAttestations} policy attestations pending`
        : 'No pending policy attestations',
      toneClass: overdueRemediation > 0 || pendingAttestations > 0 ? 'text-warning' : 'text-success'
    }
  ]
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
    schedules.value = schedulesRes.data || []
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
    schedules.value = schedulesRes.data || []
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
.trend-complete { background: #198754; }
.trend-open { background: #0d6efd; }
.trend-overdue { background: #dc3545; }
</style>
