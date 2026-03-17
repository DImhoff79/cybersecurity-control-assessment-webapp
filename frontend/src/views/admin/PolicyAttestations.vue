<template>
  <div>
    <div class="mb-3">
      <h1 class="h3 mb-1">Policy Attestations</h1>
      <p class="text-muted mb-0">
        Monitor acknowledgement progress and overdue policy attestations.
      </p>
    </div>
    <div class="small text-muted mb-3">
      Filter by policy to identify acknowledgement bottlenecks quickly.
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
        </div>
      </div>
    </div>

    <div class="card shadow-sm">
      <div class="card-body">
        <h2 class="h5 mb-3">Acknowledgements</h2>
        <p v-if="!rows.length" class="text-muted mb-0">No acknowledgement records found.</p>
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
              <tr v-for="row in rows" :key="row.id">
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
import { onMounted, ref } from 'vue'
import api from '../../services/api'
import { toastError } from '../../services/toast'

const policies = ref([])
const rows = ref([])
const selectedPolicyId = ref('')

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
