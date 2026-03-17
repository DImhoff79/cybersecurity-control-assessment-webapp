<template>
  <div>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Remediation Plans</h1>
        <p class="text-muted mb-0">Plan and execute corrective actions with due-date tracking.</p>
      </div>
    </div>
    <div class="small text-muted mb-3">
      Attach remediation plans to specific risks and assign accountable owners for each action. Approval decisions are made by Admins or Audit Managers.
    </div>

    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Create Plan</h2>
        <div class="row g-2">
          <div class="col-md-3">
            <div class="small fw-semibold text-secondary mb-1">Risk</div>
            <select v-model="planForm.riskId" class="form-select form-select-sm">
              <option :value="null">Risk (optional)</option>
              <option v-for="r in risks" :key="r.id" :value="r.id">{{ r.title }}</option>
            </select>
          </div>
          <div class="col-md-5">
            <div class="small fw-semibold text-secondary mb-1">Plan title</div>
            <input v-model="planForm.title" class="form-control form-control-sm" placeholder="Plan title" />
          </div>
          <div class="col-md-2">
            <div class="small fw-semibold text-secondary mb-1">Target complete</div>
            <input v-model="planForm.targetCompleteAt" type="date" class="form-control form-control-sm" />
          </div>
          <div class="col-md-2">
            <div class="small fw-semibold text-secondary mb-1">Create</div>
            <button class="btn btn-primary btn-sm w-100" @click="createPlan">Create</button>
          </div>
          <div class="col-md-12">
            <div class="small fw-semibold text-secondary mb-1">Proposed plan</div>
            <textarea v-model="planForm.proposedPlan" class="form-control form-control-sm" rows="3" placeholder="Document the proposed remediation plan"></textarea>
          </div>
          <div class="col-md-4">
            <div class="small fw-semibold text-secondary mb-1">Timeframe detail</div>
            <input v-model="planForm.timeframeText" class="form-control form-control-sm" placeholder="e.g. 30/60/90 day phased rollout" />
          </div>
          <div class="col-md-8">
            <div class="small fw-semibold text-secondary mb-1">Compensating controls</div>
            <input v-model="planForm.compensatingControls" class="form-control form-control-sm" placeholder="Interim controls in place while remediation is underway" />
          </div>
          <div class="col-md-12">
            <div class="small fw-semibold text-secondary mb-1">How this helps</div>
            <textarea v-model="planForm.planRationale" class="form-control form-control-sm" rows="3" placeholder="Free text explaining risk reduction and expected outcome"></textarea>
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
                <th>Approval</th>
                <th>Target</th>
                <th style="min-width: 280px;">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="plan in plans" :key="plan.id">
                <td>{{ plan.title }}</td>
                <td>{{ plan.riskTitle || '-' }}</td>
                <td><span class="badge text-bg-secondary">{{ plan.status }}</span></td>
                <td>
                  <span class="badge" :class="approvalBadgeClass(plan.approvalStatus)">{{ plan.approvalStatus || 'DRAFT' }}</span>
                </td>
                <td>{{ formatDate(plan.targetCompleteAt) }}</td>
                <td>
                  <div class="input-group input-group-sm mb-1">
                    <select v-model="plan._nextStatus" class="form-select">
                      <option value="DRAFT">DRAFT</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="BLOCKED">BLOCKED</option>
                      <option value="COMPLETE">COMPLETE</option>
                    </select>
                    <button type="button" class="btn btn-outline-secondary" @click="updatePlan(plan)">Update</button>
                    <button type="button" class="btn btn-outline-primary" @click="selectPlan(plan)">Open</button>
                  </div>
                  <div class="d-flex gap-1 flex-wrap">
                    <button
                      v-if="canSubmit(plan)"
                      type="button"
                      class="btn btn-outline-warning btn-sm"
                      @click="submitForApproval(plan)"
                    >
                      Submit for Approval
                    </button>
                    <button
                      v-if="canReview(plan)"
                      type="button"
                      class="btn btn-success btn-sm"
                      @click="decideApproval(plan, true)"
                    >
                      Approve
                    </button>
                    <button
                      v-if="canReview(plan)"
                      type="button"
                      class="btn btn-danger btn-sm"
                      @click="decideApproval(plan, false)"
                    >
                      Reject
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div v-if="selectedPlan" class="modal-backdrop-custom" @click.self="closePlanModal">
      <div class="card shadow-sm modal-card-lg">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-start mb-3">
            <div>
              <h2 class="h5 mb-1">Plan Workspace - {{ selectedPlan.title }}</h2>
              <div class="small text-muted">Edit documentation, track approval, and manage plan actions.</div>
            </div>
            <button type="button" class="btn btn-sm btn-outline-secondary" @click="closePlanModal">Close</button>
          </div>
        <h2 class="h5 mb-3">Actions - {{ selectedPlan.title }}</h2>
        <div class="small text-muted mb-3">
          Approval: <strong>{{ selectedPlan.approvalStatus || 'DRAFT' }}</strong>
          <span v-if="selectedPlan.approvedByEmail"> by {{ selectedPlan.approvedByEmail }}</span>
        </div>
        <div class="row g-2 mb-3">
          <div class="col-md-12">
            <div class="small fw-semibold text-secondary mb-1">Proposed plan</div>
            <textarea v-model="selectedPlan.proposedPlan" class="form-control form-control-sm" rows="3"></textarea>
          </div>
          <div class="col-md-4">
            <div class="small fw-semibold text-secondary mb-1">Timeframe detail</div>
            <input v-model="selectedPlan.timeframeText" class="form-control form-control-sm" />
          </div>
          <div class="col-md-8">
            <div class="small fw-semibold text-secondary mb-1">Compensating controls</div>
            <input v-model="selectedPlan.compensatingControls" class="form-control form-control-sm" />
          </div>
          <div class="col-md-12">
            <div class="small fw-semibold text-secondary mb-1">How this helps</div>
            <textarea v-model="selectedPlan.planRationale" class="form-control form-control-sm" rows="3"></textarea>
          </div>
          <div class="col-md-2">
            <button type="button" class="btn btn-outline-secondary btn-sm w-100" @click="savePlanDocumentation">Save Plan Notes</button>
          </div>
          <div class="col-md-10" v-if="selectedPlan.approvalNotes">
            <div class="small text-muted">
              Latest approval note: {{ selectedPlan.approvalNotes }}
            </div>
          </div>
        </div>
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
            <button type="button" class="btn btn-outline-primary btn-sm w-100" @click="createAction">Create</button>
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
                    <button type="button" class="btn btn-outline-secondary" @click="updateAction(action)">Update</button>
                  </div>
                </td>
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
import { useAuthStore } from '../../stores/auth'

const risks = ref([])
const users = ref([])
const plans = ref([])
const selectedPlan = ref(null)
const actions = ref([])
const authStore = useAuthStore()
const canApprovePlans = computed(() => authStore.user?.role === 'ADMIN' || authStore.user?.role === 'AUDIT_MANAGER')

const planForm = ref({
  riskId: null,
  title: '',
  targetCompleteAt: '',
  proposedPlan: '',
  timeframeText: '',
  compensatingControls: '',
  planRationale: ''
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
  const [plansRes, risksRes, usersRes] = await Promise.allSettled([
    api.get('/api/remediation-plans'),
    api.get('/api/risks'),
    api.get('/api/users')
  ])
  plans.value = plansRes.status === 'fulfilled'
    ? (plansRes.value.data || []).map((p) => ({ ...p, _nextStatus: p.status }))
    : []
  risks.value = risksRes.status === 'fulfilled' ? (risksRes.value.data || []) : []
  users.value = usersRes.status === 'fulfilled' ? (usersRes.value.data || []) : []
}

async function createPlan() {
  try {
    await api.post('/api/remediation-plans', {
      ...planForm.value,
      targetCompleteAt: toInstant(planForm.value.targetCompleteAt)
    })
    planForm.value.title = ''
    planForm.value.targetCompleteAt = ''
    planForm.value.proposedPlan = ''
    planForm.value.timeframeText = ''
    planForm.value.compensatingControls = ''
    planForm.value.planRationale = ''
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

async function savePlanDocumentation() {
  if (!selectedPlan.value) return
  try {
    await api.put(`/api/remediation-plans/${selectedPlan.value.id}`, {
      proposedPlan: selectedPlan.value.proposedPlan || null,
      timeframeText: selectedPlan.value.timeframeText || null,
      compensatingControls: selectedPlan.value.compensatingControls || null,
      planRationale: selectedPlan.value.planRationale || null
    })
    await loadAll()
    selectedPlan.value = plans.value.find((p) => p.id === selectedPlan.value.id) || selectedPlan.value
    toastSuccess('Plan documentation updated.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to save plan documentation')
  }
}

async function selectPlan(plan) {
  selectedPlan.value = { ...plan }
  await loadActions()
}

function closePlanModal() {
  selectedPlan.value = null
  actions.value = []
}

async function loadActions() {
  if (!selectedPlan.value) return
  try {
    const res = await api.get(`/api/remediation-plans/${selectedPlan.value.id}/actions`)
    actions.value = (res.data || []).map((a) => ({ ...a, _nextStatus: a.status }))
  } catch (e) {
    actions.value = []
    toastError(e.response?.data?.error || 'Failed to load plan actions')
  }
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

async function submitForApproval(plan) {
  try {
    await api.post(`/api/remediation-plans/${plan.id}/submit-approval`, {})
    await loadAll()
    toastSuccess('Plan submitted for approval.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to submit for approval')
  }
}

async function decideApproval(plan, approved) {
  const notes = window.prompt(approved ? 'Optional approval note' : 'Reason for rejection')
  if (notes === null) return
  if (!approved && !notes.trim()) {
    toastError('A rejection reason is required.')
    return
  }
  try {
    await api.post(`/api/remediation-plans/${plan.id}/approval-decision`, {
      approved,
      notes
    })
    await loadAll()
    toastSuccess(approved ? 'Plan approved.' : 'Plan rejected.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to record approval decision')
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

function canSubmit(plan) {
  return !plan.approvalStatus || plan.approvalStatus === 'DRAFT' || plan.approvalStatus === 'REJECTED'
}

function canReview(plan) {
  return canApprovePlans.value && plan.approvalStatus === 'SUBMITTED'
}

function approvalBadgeClass(status) {
  if (status === 'APPROVED') return 'text-bg-success'
  if (status === 'SUBMITTED') return 'text-bg-warning'
  if (status === 'REJECTED') return 'text-bg-danger'
  return 'text-bg-secondary'
}
</script>

<style scoped>
.modal-backdrop-custom {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 1300;
  display: grid;
  place-items: center;
  padding: 1rem;
}

.modal-card-lg {
  width: min(1100px, 100%);
  max-height: calc(100vh - 2rem);
  overflow: auto;
}
</style>
