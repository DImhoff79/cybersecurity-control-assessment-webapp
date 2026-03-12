<template>
  <div>
    <div class="mb-3">
      <h1 class="h3 mb-1">My Audits</h1>
      <p class="text-muted mb-0">
        Track assigned assessments, completion progress, and next actions.
      </p>
    </div>
    <div class="small text-muted mb-3">
      Start or resume any in-progress audit, then submit when all sections are complete.
    </div>
    <p v-if="!items.length && !loading" class="text-muted">No audits assigned to you.</p>
    <div v-else class="card shadow-sm">
      <div class="card-body">
        <div class="table-responsive">
          <table class="table table-striped table-hover align-middle mb-0 my-audits-table">
            <thead>
              <tr>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('applicationName')">Application {{ sortIndicator('applicationName') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('year')">Year {{ sortIndicator('year') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('projectName')">Project {{ sortIndicator('projectName') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('status')">Status {{ sortIndicator('status') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('completionPct')">Completion {{ sortIndicator('completionPct') }}</button></th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in sortedRows" :key="a.id">
                <td>{{ a.applicationName }}</td>
                <td>{{ a.year }}</td>
                <td>{{ a.projectName || '-' }}</td>
                <td>
                  <span class="badge status-badge" :class="statusBadgeClass(a.status)">
                    {{ statusLabel(a.status) }}
                  </span>
                </td>
                <td>
                  <div class="d-flex align-items-center gap-2">
                    <div class="progress flex-grow-1" style="height: 8px; min-width: 120px;">
                      <div
                        class="progress-bar"
                        role="progressbar"
                        :style="{ width: `${a.completionPct || 0}%` }"
                        :aria-valuenow="a.completionPct || 0"
                        aria-valuemin="0"
                        aria-valuemax="100"
                      />
                    </div>
                    <span class="small text-muted">{{ a.completionPct || 0 }}%</span>
                  </div>
                </td>
                <td class="audit-action-cell">
                  <router-link
                    :to="`/audits/${a.id}/respond`"
                    class="btn btn-primary btn-sm audit-action-btn"
                    :class="{ disabled: a.status === 'SUBMITTED' || a.status === 'ATTESTED' || a.status === 'COMPLETE' }"
                  >
                    {{ actionLabel(a) }}
                  </router-link>
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
import { ref, onMounted } from 'vue'
import api from '../../services/api'
import { useTableSort } from '../../composables/useTableSort'

const items = ref([])
const loading = ref(true)
const { sortedRows, toggleSort, sortIndicator } = useTableSort(items, {
  initialKey: 'applicationName'
})

onMounted(async () => {
  try {
    const res = await api.get('/api/my-audits')
    items.value = res.data || []
  } finally {
    loading.value = false
  }
})

function statusLabel(status) {
  switch (status) {
    case 'SUBMITTED':
      return 'Completed - pending admin review'
    case 'COMPLETE':
      return 'Validated complete'
    case 'ATTESTED':
      return 'Attested by audit team'
    case 'IN_PROGRESS':
      return 'In progress'
    case 'DRAFT':
      return 'Draft'
    default:
      return status || '-'
  }
}

function actionLabel(audit) {
  if (audit.status === 'SUBMITTED' || audit.status === 'ATTESTED' || audit.status === 'COMPLETE') return 'View submission'
  return (audit.completionPct || 0) > 0 ? 'Resume audit' : 'Start audit'
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

<style scoped>
.my-audits-table th:nth-child(1),
.my-audits-table td:nth-child(1) {
  min-width: 220px;
}

.my-audits-table th:nth-child(5),
.my-audits-table td:nth-child(5) {
  min-width: 220px;
}

.audit-action-cell {
  min-width: 140px;
}

.audit-action-btn {
  width: 100%;
}

@media (min-width: 768px) {
  .audit-action-btn {
    width: auto;
  }
}
</style>
