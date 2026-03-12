<template>
  <div>
    <h1 class="h3 mb-3">Audit Program Reports</h1>
    <div class="d-flex gap-2 mb-3">
      <button class="btn btn-outline-primary btn-sm" @click="downloadAuditsCsv">Export Audits CSV</button>
      <button class="btn btn-primary btn-sm" @click="downloadBoardPackPdf">Export Board Pack PDF</button>
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
      <div class="card shadow-sm mt-3">
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
import { toastError } from '../../services/toast'
import { useTableSort } from '../../composables/useTableSort'

const loading = ref(true)
const summary = ref(null)
const byYear = ref([])
const trends = ref([])
const byProject = ref([])

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
    { label: 'Completed', value: s.completedAudits ?? 0 }
  ]
})

onMounted(async () => {
  try {
    const [res, byYearRes, trendsRes, byProjectRes] = await Promise.all([
      api.get('/api/reports/summary'),
      api.get('/api/reports/by-year'),
      api.get('/api/reports/trends'),
      api.get('/api/reports/by-project')
    ])
    summary.value = res.data || {}
    byYear.value = byYearRes.data || []
    trends.value = trendsRes.data || []
    byProject.value = byProjectRes.data || []
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
