<template>
  <div>
    <div class="mb-3">
      <h1 class="h3 mb-1">Completed Assessments Pending Review</h1>
      <p class="text-muted mb-0">
        These assessments were submitted by application owners and are ready for admin validation.
      </p>
    </div>
    <div class="small text-muted mb-3">
      Attest submitted audits before marking them reviewed to preserve the audit trail.
    </div>

    <div v-if="loading" class="text-muted">Loading...</div>
    <div v-else-if="!submittedAudits.length" class="card shadow-sm">
      <div class="card-body text-muted mb-0">No submitted assessments are currently waiting for review.</div>
    </div>
    <div v-else class="card shadow-sm">
      <div class="card-body">
        <div class="table-responsive">
          <table class="table table-striped table-hover align-middle mb-0">
            <thead>
              <tr>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('applicationName')">Application {{ sortIndicator('applicationName') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('year')">Year {{ sortIndicator('year') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('status')">Status {{ sortIndicator('status') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('submittedBy')">Submitted by {{ sortIndicator('submittedBy') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('completedAt')">Submitted at {{ sortIndicator('completedAt') }}</button></th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="audit in sortedRows" :key="audit.id">
                <td>{{ audit.applicationName }}</td>
                <td>{{ audit.year }}</td>
                <td>
                  <span class="badge status-badge" :class="statusBadgeClass(audit.status)">
                    {{ audit.status }}
                  </span>
                </td>
                <td>{{ audit.assignedToDisplayName || audit.assignedToEmail || '-' }}</td>
                <td>{{ formatDate(audit.completedAt) }}</td>
                <td class="text-nowrap">
                  <router-link :to="`/admin/audits/${audit.id}`" class="btn btn-primary btn-sm me-2">
                    Review assessment
                  </router-link>
                  <button
                    type="button"
                    class="btn btn-outline-success btn-sm me-2"
                    :disabled="audit.status !== 'SUBMITTED' && audit.status !== 'ATTESTED'"
                    @click="attest(audit.id)"
                  >
                    Attest
                  </button>
                  <button type="button" class="btn btn-success btn-sm" @click="markReviewed(audit.id)">
                    Mark reviewed
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
import { computed, onMounted, ref } from 'vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'
import { useTableSort } from '../../composables/useTableSort'

const audits = ref([])
const loading = ref(true)

const submittedAudits = computed(() => {
  return audits.value
    .filter((a) => a.status === 'SUBMITTED' || a.status === 'ATTESTED')
    .sort((a, b) => new Date(b.completedAt || 0) - new Date(a.completedAt || 0))
})
const { sortedRows, toggleSort, sortIndicator } = useTableSort(submittedAudits, {
  initialKey: 'completedAt',
  initialDirection: 'desc',
  valueGetters: {
    submittedBy: (row) => row.assignedToDisplayName || row.assignedToEmail || ''
  }
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await api.get('/api/my-audits')
    audits.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function markReviewed(auditId) {
  try {
    await api.put(`/api/audits/${auditId}`, { status: 'COMPLETE' })
    await load()
    toastSuccess('Assessment marked reviewed.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to mark assessment as reviewed.')
  }
}

async function attest(auditId) {
  try {
    await api.post(`/api/audits/${auditId}/attest`, { statement: 'Attested during review queue processing.' })
    await load()
    toastSuccess('Assessment attested.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to attest assessment.')
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function statusBadgeClass(status) {
  switch (status) {
    case 'COMPLETE':
      return 'text-bg-success'
    case 'ATTESTED':
      return 'text-bg-primary'
    case 'SUBMITTED':
      return 'text-bg-info'
    case 'IN_PROGRESS':
      return 'text-bg-warning'
    case 'DRAFT':
      return 'text-bg-secondary'
    default:
      return 'text-bg-secondary'
  }
}
</script>
