<template>
  <div>
    <h1 class="h3 mb-2">Completed Assessments Pending Review</h1>
    <p class="text-muted mb-3">
      These assessments were submitted by application owners and are ready for admin validation.
    </p>

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
                <th>Application</th>
                <th>Year</th>
                <th>Submitted by</th>
                <th>Submitted at</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="audit in submittedAudits" :key="audit.id">
                <td>{{ audit.applicationName }}</td>
                <td>{{ audit.year }}</td>
                <td>{{ audit.assignedToDisplayName || audit.assignedToEmail || '-' }}</td>
                <td>{{ formatDate(audit.completedAt) }}</td>
                <td class="text-nowrap">
                  <router-link :to="`/admin/audits/${audit.id}`" class="btn btn-primary btn-sm me-2">
                    Review assessment
                  </router-link>
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

const audits = ref([])
const loading = ref(true)

const submittedAudits = computed(() => {
  return audits.value
    .filter((a) => a.status === 'SUBMITTED')
    .sort((a, b) => new Date(b.completedAt || 0) - new Date(a.completedAt || 0))
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
  } catch (e) {
    alert(e.response?.data?.error || 'Failed to mark assessment as reviewed.')
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}
</script>
