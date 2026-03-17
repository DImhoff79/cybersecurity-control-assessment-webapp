<template>
  <div>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Risk Register</h1>
        <p class="text-muted mb-0">Capture, score, and track operational and compliance risks.</p>
      </div>
    </div>
    <div class="small text-muted mb-3">
      Use inherent and residual scoring to prioritize remediation planning.
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
            <input v-model="form.title" class="form-control form-control-sm" placeholder="Risk title" required />
          </div>
          <div class="col-md-2">
            <input v-model.number="form.likelihoodScore" type="number" min="1" max="5" class="form-control form-control-sm" placeholder="Likelihood (1-5)" required />
          </div>
          <div class="col-md-2">
            <input v-model.number="form.impactScore" type="number" min="1" max="5" class="form-control form-control-sm" placeholder="Impact (1-5)" required />
          </div>
          <div class="col-md-2">
            <select v-model="form.ownerUserId" class="form-select form-select-sm">
              <option :value="null">Owner</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.email }}</option>
            </select>
          </div>
          <div class="col-md-2">
            <button type="submit" class="btn btn-primary btn-sm w-100">Create</button>
          </div>
        </form>
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
                <th>Residual</th>
                <th>Owner</th>
                <th style="min-width: 220px;">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in items" :key="row.id">
                <td>{{ row.title }}</td>
                <td><span class="badge text-bg-secondary">{{ row.status }}</span></td>
                <td>{{ row.inherentRiskScore }}</td>
                <td>{{ row.residualRiskScore }}</td>
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
const kpis = ref({})
const form = ref({
  title: '',
  likelihoodScore: 3,
  impactScore: 3,
  ownerUserId: null
})

onMounted(async () => {
  await load()
})

async function load() {
  const [riskRes, usersRes, kpiRes] = await Promise.all([
    api.get('/api/risks'),
    api.get('/api/users'),
    api.get('/api/reports/risk-kpis')
  ])
  items.value = (riskRes.data || []).map((row) => ({ ...row, _nextStatus: row.status }))
  users.value = usersRes.data || []
  kpis.value = kpiRes.data || {}
}

async function createRisk() {
  try {
    await api.post('/api/risks', form.value)
    form.value.title = ''
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
