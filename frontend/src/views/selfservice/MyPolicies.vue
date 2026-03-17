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
                    class="btn btn-outline-secondary btn-sm me-2"
                    @click="openPolicyBody(row)"
                  >
                    View Policy
                  </button>
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

    <div v-if="policyModal.open" class="modal-backdrop-custom">
      <div class="card shadow modal-card">
        <div class="card-body">
          <h2 class="h5 mb-2">{{ policyModal.title }}</h2>
          <p class="small text-muted mb-3">Version {{ policyModal.version }}</p>
          <article class="policy-body policy-prose mb-3" v-html="policyModal.body"></article>
          <div class="d-flex justify-content-end">
            <button type="button" class="btn btn-outline-secondary btn-sm" @click="closePolicyBody">Close</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { marked } from 'marked'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const rows = ref([])
const policyModal = ref({
  open: false,
  title: '',
  version: '',
  body: ''
})

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

function openPolicyBody(row) {
  policyModal.value = {
    open: true,
    title: `${row.policyCode} - ${row.policyName}`,
    version: row.policyVersionTitle,
    body: renderPolicyHtml(row.policyVersionBodyMarkdown)
  }
}

function closePolicyBody() {
  policyModal.value.open = false
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function renderPolicyHtml(content) {
  if (!content || !content.trim()) {
    return '<p class="text-muted">No policy body is available for this version.</p>'
  }
  const raw = content.trim()
  const looksLikeHtml = /<\/?[a-z][\s\S]*>/i.test(raw)
  if (looksLikeHtml) return raw
  const normalized = raw.replace(/([^\n])\s(#{1,6}\s)/g, '$1\n\n$2')
  return marked.parse(normalized, { gfm: true, breaks: true })
}
</script>

<style scoped>
.modal-backdrop-custom {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 1200;
  display: grid;
  place-items: center;
  padding: 1rem;
}

.modal-card {
  width: min(860px, 100%);
}

.policy-body {
  max-height: 55vh;
  overflow: auto;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfd 100%);
  border: 1px solid #d9dee4;
  border-radius: 0.5rem;
  padding: 1.1rem 1.25rem;
}

.policy-prose {
  color: #1f2937;
  line-height: 1.65;
  font-size: 0.95rem;
}

.policy-prose :deep(h1),
.policy-prose :deep(h2),
.policy-prose :deep(h3) {
  color: #0f172a;
  margin-top: 1rem;
  margin-bottom: 0.55rem;
}

.policy-prose :deep(h1) {
  font-size: 1.3rem;
}

.policy-prose :deep(h2) {
  font-size: 1.1rem;
}

.policy-prose :deep(p) {
  margin: 0 0 0.8rem;
}

.policy-prose :deep(ul),
.policy-prose :deep(ol) {
  padding-left: 1.15rem;
  margin-bottom: 0.85rem;
}

.policy-prose :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0.8rem 0;
}

.policy-prose :deep(th),
.policy-prose :deep(td) {
  border: 1px solid #d1d5db;
  padding: 0.4rem 0.5rem;
}
</style>
