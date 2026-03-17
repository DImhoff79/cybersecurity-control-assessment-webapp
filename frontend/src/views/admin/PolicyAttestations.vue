<template>
  <div>
    <div class="mb-3">
      <h1 class="h3 mb-1">Policy Attestations</h1>
      <p class="text-muted mb-0">
        Monitor acknowledgement progress, overdue attestations, and upcoming policy review decisions.
      </p>
    </div>
    <div class="small text-muted mb-3">
      Filter by policy to identify acknowledgement bottlenecks quickly.
    </div>
    <div class="row g-2 mb-3">
      <div class="col-md-3" v-for="card in queueCards" :key="card.label">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <div class="small text-muted">{{ card.label }}</div>
            <div class="h4 mb-0">{{ card.value }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <div class="row g-2 align-items-end">
          <div class="col-md-5">
            <label class="form-label small mb-1">Policy</label>
            <select v-model="selectedPolicyId" class="form-select form-select-sm" @change="loadAcknowledgements">
              <option value="">All policies</option>
              <option v-for="p in policies" :key="p.id" :value="String(p.id)">
                {{ p.code }} - {{ p.name }}
              </option>
            </select>
          </div>
          <div class="col-md-2">
            <button class="btn btn-outline-secondary btn-sm w-100" @click="loadAcknowledgements">Refresh</button>
          </div>
          <div class="col-md-3" v-if="hasPolicyManagement">
            <router-link class="btn btn-primary btn-sm w-100" to="/admin/policies">Open Policy Workspace</router-link>
          </div>
        </div>
      </div>
    </div>

    <div class="card shadow-sm">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center gap-2 flex-wrap mb-3">
          <h2 class="h5 mb-0">Approver Queue</h2>
          <select v-model="queueFilter" class="form-select form-select-sm queue-filter">
            <option value="all">All acknowledgement states</option>
            <option value="pending">Pending only</option>
            <option value="overdue">Overdue only</option>
          </select>
        </div>
        <p class="small text-muted mb-3">
          Use this queue for approver triage, then jump into Policy Workspace for publish/revision actions.
        </p>
        <h2 class="h5 mb-3">Acknowledgements</h2>
        <p v-if="!filteredRows.length" class="text-muted mb-0">No acknowledgement records found.</p>
        <div v-else class="table-responsive">
          <table class="table table-striped mb-0">
            <thead>
              <tr>
                <th>Policy</th>
                <th>User</th>
                <th>Status</th>
                <th>Assigned</th>
                <th>Due</th>
                <th>Acknowledged</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in filteredRows" :key="row.id">
                <td>
                  {{ row.policyCode }}<br>
                  <span class="text-muted small">v{{ row.policyVersionNumber }} - {{ row.policyVersionTitle }}</span>
                </td>
                <td>{{ row.userEmail }}</td>
                <td>
                  <span class="badge" :class="badgeClass(row.status)">{{ row.status }}</span>
                </td>
                <td>{{ formatDate(row.assignedAt) }}</td>
                <td>{{ formatDate(row.dueAt) }}</td>
                <td>{{ formatDate(row.acknowledgedAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../../services/api'
import { toastError } from '../../services/toast'
import { useAuthStore } from '../../stores/auth'

const policies = ref([])
const rows = ref([])
const selectedPolicyId = ref('')
const queueFilter = ref('all')
const authStore = useAuthStore()
const hasPolicyManagement = computed(() => authStore.hasPermission('POLICY_MANAGEMENT'))

const filteredRows = computed(() => {
  if (queueFilter.value === 'pending') {
    return rows.value.filter((row) => row.status === 'PENDING')
  }
  if (queueFilter.value === 'overdue') {
    return rows.value.filter((row) => row.status === 'OVERDUE')
  }
  return rows.value
})

const queueCards = computed(() => {
  const pending = rows.value.filter((row) => row.status === 'PENDING').length
  const overdue = rows.value.filter((row) => row.status === 'OVERDUE').length
  const now = Date.now()
  const reviewOverdue = policies.value.filter((policy) => policy.nextReviewAt && new Date(policy.nextReviewAt).getTime() < now).length
  const dueSoon = policies.value.filter((policy) => {
    if (!policy.nextReviewAt) return false
    const ms = new Date(policy.nextReviewAt).getTime() - now
    return ms >= 0 && ms <= 30 * 24 * 60 * 60 * 1000
  }).length
  return [
    { label: 'Pending Acknowledgements', value: pending },
    { label: 'Overdue Attestations', value: overdue },
    { label: 'Review Overdue Policies', value: reviewOverdue },
    { label: 'Reviews Due in 30 Days', value: dueSoon }
  ]
})

onMounted(async () => {
  try {
    const policiesRes = await api.get('/api/policies')
    policies.value = policiesRes.data || []
    await loadAcknowledgements()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to load policy attestations')
  }
})

async function loadAcknowledgements() {
  try {
    const params = {}
    if (selectedPolicyId.value) params.policyId = Number(selectedPolicyId.value)
    const res = await api.get('/api/policies/acknowledgements', { params })
    rows.value = res.data || []
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to load acknowledgements')
  }
}

function badgeClass(status) {
  if (status === 'ACKNOWLEDGED') return 'text-bg-success'
  if (status === 'OVERDUE') return 'text-bg-danger'
  return 'text-bg-warning'
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}
</script>

<style scoped>
.queue-filter {
  width: min(320px, 100%);
}
</style>
