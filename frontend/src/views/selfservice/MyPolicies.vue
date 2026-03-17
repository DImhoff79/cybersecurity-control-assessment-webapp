<template>
  <div>
    <div class="mb-3">
      <h1 class="h3 mb-1">My Policies</h1>
      <p class="text-muted mb-0">
        Review published policies and submit acknowledgements assigned to you.
      </p>
    </div>
    <div class="small text-muted mb-3">
      Overdue acknowledgements remain visible until completion.
    </div>

    <div class="card shadow-sm">
      <div class="card-body">
        <p v-if="!rows.length" class="text-muted mb-0">No policy acknowledgements assigned.</p>
        <div v-else class="table-responsive">
          <table class="table table-striped align-middle mb-0">
            <thead>
              <tr>
                <th>Policy</th>
                <th>Version</th>
                <th>Status</th>
                <th>Due</th>
                <th>Acknowledged</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.id">
                <td>{{ row.policyCode }} - {{ row.policyName }}</td>
                <td>v{{ row.policyVersionNumber }} - {{ row.policyVersionTitle }}</td>
                <td>
                  <span class="badge" :class="badgeClass(row.status)">{{ row.status }}</span>
                </td>
                <td>{{ formatDate(row.dueAt) }}</td>
                <td>{{ formatDate(row.acknowledgedAt) }}</td>
                <td>
                  <button
                    class="btn btn-primary btn-sm"
                    :disabled="row.status === 'ACKNOWLEDGED'"
                    @click="acknowledge(row.id)"
                  >
                    {{ row.status === 'ACKNOWLEDGED' ? 'Completed' : 'Acknowledge' }}
                  </button>
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

const rows = ref([])

onMounted(async () => {
  await loadRows()
})

async function loadRows() {
  try {
    const res = await api.get('/api/policies/my-acknowledgements')
    rows.value = res.data || []
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to load policy assignments')
  }
}

async function acknowledge(id) {
  try {
    await api.post(`/api/policies/acknowledgements/${id}/acknowledge`)
    await loadRows()
    toastSuccess('Policy acknowledgement submitted.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to submit acknowledgement')
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
