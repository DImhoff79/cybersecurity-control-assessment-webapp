<template>
  <div>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Risk Register</h1>
        <p class="text-muted mb-0">Capture, score, and track operational and compliance risks.</p>
      </div>
    </div>
    <div class="small text-muted mb-3">
      Log a risk, score how likely and severe it is, assign an owner, then track it from OPEN to CLOSED.
    </div>

    <div class="row g-3 mb-3">
      <div class="col-md-3">
        <div class="card shadow-sm h-100"><div class="card-body"><div class="text-muted small">Open Risks</div><div class="h4 mb-0">{{ kpis.openRisks || 0 }}</div></div></div>
      </div>
      <div class="col-md-3">
        <div class="card shadow-sm h-100"><div class="card-body"><div class="text-muted small">High Risks</div><div class="h4 mb-0">{{ kpis.highRisks || 0 }}</div></div></div>
      </div>
      <div class="col-md-3">
        <div class="card shadow-sm h-100"><div class="card-body"><div class="text-muted small">Overdue Actions</div><div class="h4 mb-0">{{ kpis.overdueRemediationActions || 0 }}</div></div></div>
      </div>
      <div class="col-md-3">
        <div class="card shadow-sm h-100"><div class="card-body"><div class="text-muted small">Plans In Progress</div><div class="h4 mb-0">{{ kpis.plansInProgress || 0 }}</div></div></div>
      </div>
    </div>

    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Create Risk</h2>
        <form class="row g-2" @submit.prevent="createRisk">
          <div class="col-md-4">
            <div class="small fw-semibold text-secondary mb-1">Risk title</div>
            <input v-model="form.title" class="form-control form-control-sm" placeholder="Risk title" required />
          </div>
          <div class="col-md-2">
            <div class="small fw-semibold text-secondary mb-1">Likelihood (1-5)</div>
            <input v-model.number="form.likelihoodScore" type="number" min="1" max="5" class="form-control form-control-sm" placeholder="1 low - 5 very likely" required />
          </div>
          <div class="col-md-2">
            <div class="small fw-semibold text-secondary mb-1">Impact (1-5)</div>
            <input v-model.number="form.impactScore" type="number" min="1" max="5" class="form-control form-control-sm" placeholder="1 low - 5 severe" required />
          </div>
          <div class="col-md-2">
            <div class="small fw-semibold text-secondary mb-1">Owner</div>
            <select v-model="form.ownerUserId" class="form-select form-select-sm">
              <option :value="null">Unassigned</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.email }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <div class="small fw-semibold text-secondary mb-1">Application attribution</div>
            <select v-model="form.attributionSelection" class="form-select form-select-sm">
              <option value="">Select application</option>
              <option v-for="app in applications" :key="app.id" :value="String(app.id)">{{ app.name }}</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
          <div v-if="form.attributionSelection === 'OTHER'" class="col-md-4">
            <div class="small fw-semibold text-secondary mb-1">Other attribution details</div>
            <input
              v-model="form.otherApplicationText"
              class="form-control form-control-sm"
              placeholder="Describe the system, shared service, or business area"
              required
            />
          </div>
          <div class="col-md-2 d-flex align-items-end">
            <button type="submit" class="btn btn-primary btn-sm w-100">Create</button>
          </div>
        </form>
        <div class="small text-muted mt-2">
          Inherent score = Likelihood x Impact before controls. Residual is manual after mitigation work and may be blank initially.
        </div>
      </div>
    </div>

    <div class="card shadow-sm">
      <div class="card-body">
        <h2 class="h5 mb-3">Risk Items</h2>
        <p v-if="!items.length" class="text-muted mb-0">No risks tracked yet.</p>
        <div v-else class="table-responsive">
          <table class="table table-striped align-middle mb-0">
            <thead>
              <tr>
                <th>Title</th>
                <th>Status</th>
                <th>Inherent</th>
                <th>Residual (Manual)</th>
                <th>Application</th>
                <th>Owner</th>
                <th style="min-width: 220px;">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in items" :key="row.id">
                <td>{{ row.title }}</td>
                <td><span class="badge text-bg-secondary">{{ row.status }}</span></td>
                <td>{{ row.inherentRiskScore }}</td>
                <td>{{ row.residualRiskScore ?? '-' }}</td>
                <td>{{ row.applicationName || row.otherApplicationText || '-' }}</td>
                <td>{{ row.ownerEmail || '-' }}</td>
                <td>
                  <div class="input-group input-group-sm">
                    <select v-model="row._nextStatus" class="form-select form-select-sm">
                      <option value="OPEN">OPEN</option>
                      <option value="MONITORING">MONITORING</option>
                      <option value="MITIGATED">MITIGATED</option>
                      <option value="CLOSED">CLOSED</option>
                    </select>
                    <button class="btn btn-outline-secondary" @click="updateStatus(row)">Update</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const items = ref([])
const users = ref([])
const applications = ref([])
const kpis = ref({})
const form = ref({
  title: '',
  likelihoodScore: 3,
  impactScore: 3,
  ownerUserId: null,
  attributionSelection: '',
  otherApplicationText: ''
})

onMounted(async () => {
  await load()
})

async function load() {
  const [riskRes, usersRes, appsRes, kpiRes] = await Promise.all([
    api.get('/api/risks'),
    api.get('/api/users'),
    api.get('/api/applications'),
    api.get('/api/reports/risk-kpis')
  ])
  items.value = (riskRes.data || []).map((row) => ({ ...row, _nextStatus: row.status }))
  users.value = usersRes.data || []
  applications.value = appsRes.data || []
  kpis.value = kpiRes.data || {}
}

async function createRisk() {
  const selected = form.value.attributionSelection
  const useOther = selected === 'OTHER'
  const otherText = useOther ? (form.value.otherApplicationText || '').trim() : ''
  if (!selected) {
    toastError('Select an application or choose Other')
    return
  }
  if (useOther && !otherText) {
    toastError('Provide details when selecting Other')
    return
  }
  try {
    const applicationId = !useOther ? Number(selected) : null
    await api.post('/api/risks', {
      title: form.value.title,
      likelihoodScore: form.value.likelihoodScore,
      impactScore: form.value.impactScore,
      ownerUserId: form.value.ownerUserId,
      applicationId,
      otherApplicationText: useOther ? otherText : null
    })
    form.value.title = ''
    form.value.attributionSelection = ''
    form.value.otherApplicationText = ''
    await load()
    toastSuccess('Risk item created.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create risk')
  }
}

async function updateStatus(row) {
  try {
    await api.put(`/api/risks/${row.id}`, { status: row._nextStatus })
    await load()
    toastSuccess('Risk status updated.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update risk')
  }
}
</script>
