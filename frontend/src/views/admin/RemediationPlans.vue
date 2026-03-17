<template>
  <div>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Remediation Plans</h1>
        <p class="text-muted mb-0">Plan and execute corrective actions with due-date tracking.</p>
      </div>
    </div>
    <div class="small text-muted mb-3">
      Attach remediation plans to specific risks and assign accountable owners for each action.
    </div>

    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Create Plan</h2>
        <div class="row g-2">
          <div class="col-md-3">
            <select v-model="planForm.riskId" class="form-select form-select-sm">
              <option :value="null">Risk (optional)</option>
              <option v-for="r in risks" :key="r.id" :value="r.id">{{ r.title }}</option>
            </select>
          </div>
          <div class="col-md-5">
            <input v-model="planForm.title" class="form-control form-control-sm" placeholder="Plan title" />
          </div>
          <div class="col-md-2">
            <input v-model="planForm.targetCompleteAt" type="date" class="form-control form-control-sm" />
          </div>
          <div class="col-md-2">
            <button class="btn btn-primary btn-sm w-100" @click="createPlan">Create</button>
          </div>
        </div>
      </div>
    </div>

    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Plans</h2>
        <p v-if="!plans.length" class="text-muted mb-0">No remediation plans yet.</p>
        <div v-else class="table-responsive">
          <table class="table table-striped align-middle mb-0">
            <thead>
              <tr>
                <th>Title</th>
                <th>Risk</th>
                <th>Status</th>
                <th>Target</th>
                <th style="min-width: 280px;">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="plan in plans" :key="plan.id">
                <td>{{ plan.title }}</td>
                <td>{{ plan.riskTitle || '-' }}</td>
                <td><span class="badge text-bg-secondary">{{ plan.status }}</span></td>
                <td>{{ formatDate(plan.targetCompleteAt) }}</td>
                <td>
                  <div class="input-group input-group-sm">
                    <select v-model="plan._nextStatus" class="form-select">
                      <option value="DRAFT">DRAFT</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="BLOCKED">BLOCKED</option>
                      <option value="COMPLETE">COMPLETE</option>
                    </select>
                    <button class="btn btn-outline-secondary" @click="updatePlan(plan)">Update</button>
                    <button class="btn btn-outline-primary" @click="selectPlan(plan)">Open</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div v-if="selectedPlan" class="card shadow-sm">
      <div class="card-body">
        <h2 class="h5 mb-3">Actions - {{ selectedPlan.title }}</h2>
        <div class="row g-2 mb-3">
          <div class="col-md-4">
            <input v-model="actionForm.actionTitle" class="form-control form-control-sm" placeholder="Action title" />
          </div>
          <div class="col-md-3">
            <select v-model="actionForm.ownerUserId" class="form-select form-select-sm">
              <option :value="null">Owner (optional)</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.email }}</option>
            </select>
          </div>
          <div class="col-md-3">
            <input v-model="actionForm.dueAt" type="date" class="form-control form-control-sm" />
          </div>
          <div class="col-md-2">
            <button class="btn btn-outline-primary btn-sm w-100" @click="createAction">Create</button>
          </div>
        </div>
        <p v-if="!actions.length" class="text-muted mb-0">No actions added.</p>
        <div v-else class="table-responsive">
          <table class="table table-sm table-striped mb-0">
            <thead>
              <tr>
                <th>Action</th>
                <th>Owner</th>
                <th>Due</th>
                <th>Status</th>
                <th style="min-width: 220px;">Update</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="action in actions" :key="action.id">
                <td>{{ action.actionTitle }}</td>
                <td>{{ action.ownerEmail || '-' }}</td>
                <td>{{ formatDate(action.dueAt) }}</td>
                <td>{{ action.status }}</td>
                <td>
                  <div class="input-group input-group-sm">
                    <select v-model="action._nextStatus" class="form-select">
                      <option value="TODO">TODO</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="BLOCKED">BLOCKED</option>
                      <option value="DONE">DONE</option>
                    </select>
                    <button class="btn btn-outline-secondary" @click="updateAction(action)">Update</button>
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

const risks = ref([])
const users = ref([])
const plans = ref([])
const selectedPlan = ref(null)
const actions = ref([])

const planForm = ref({
  riskId: null,
  title: '',
  targetCompleteAt: ''
})
const actionForm = ref({
  actionTitle: '',
  ownerUserId: null,
  dueAt: ''
})

onMounted(async () => {
  await loadAll()
})

async function loadAll() {
  const [plansRes, risksRes, usersRes] = await Promise.all([
    api.get('/api/remediation-plans'),
    api.get('/api/risks'),
    api.get('/api/users')
  ])
  plans.value = (plansRes.data || []).map((p) => ({ ...p, _nextStatus: p.status }))
  risks.value = risksRes.data || []
  users.value = usersRes.data || []
}

async function createPlan() {
  try {
    await api.post('/api/remediation-plans', {
      ...planForm.value,
      targetCompleteAt: toInstant(planForm.value.targetCompleteAt)
    })
    planForm.value.title = ''
    planForm.value.targetCompleteAt = ''
    await loadAll()
    toastSuccess('Remediation plan created.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create plan')
  }
}

async function updatePlan(plan) {
  try {
    await api.put(`/api/remediation-plans/${plan.id}`, { status: plan._nextStatus })
    await loadAll()
    toastSuccess('Remediation plan updated.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update plan')
  }
}

async function selectPlan(plan) {
  selectedPlan.value = plan
  await loadActions()
}

async function loadActions() {
  if (!selectedPlan.value) return
  const res = await api.get(`/api/remediation-plans/${selectedPlan.value.id}/actions`)
  actions.value = (res.data || []).map((a) => ({ ...a, _nextStatus: a.status }))
}

async function createAction() {
  if (!selectedPlan.value) return
  try {
    await api.post(`/api/remediation-plans/${selectedPlan.value.id}/actions`, {
      ...actionForm.value,
      dueAt: toInstant(actionForm.value.dueAt)
    })
    actionForm.value.actionTitle = ''
    actionForm.value.dueAt = ''
    await loadActions()
    toastSuccess('Remediation action created.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create action')
  }
}

async function updateAction(action) {
  try {
    await api.put(`/api/remediation-plans/actions/${action.id}`, {
      status: action._nextStatus,
      completedAt: action._nextStatus === 'DONE' ? new Date().toISOString() : null
    })
    await loadActions()
    toastSuccess('Action updated.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update action')
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString()
}

function toInstant(dateValue) {
  if (!dateValue) return null
  return new Date(dateValue).toISOString()
}
</script>
