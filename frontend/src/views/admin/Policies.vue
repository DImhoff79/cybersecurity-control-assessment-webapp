<template>
  <div>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Policy Management</h1>
        <p class="text-muted mb-0">
          Author, version, and publish policy baselines with tracked owner attestations.
        </p>
      </div>
    </div>
    <div class="small text-muted mb-3">
      Publishing a policy version creates acknowledgement tasks for application owners.
    </div>

    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Create Policy</h2>
        <form class="row g-2" @submit.prevent="createPolicy">
          <div class="col-md-2">
            <input v-model="createForm.code" class="form-control form-control-sm" placeholder="Code (POL-001)" required />
          </div>
          <div class="col-md-3">
            <input v-model="createForm.name" class="form-control form-control-sm" placeholder="Policy name" required />
          </div>
          <div class="col-md-4">
            <input v-model="createForm.description" class="form-control form-control-sm" placeholder="Description" />
          </div>
          <div class="col-md-2">
            <input v-model="createForm.initialVersionTitle" class="form-control form-control-sm" placeholder="Initial version title" />
          </div>
          <div class="col-md-1">
            <button type="submit" class="btn btn-primary btn-sm w-100">Create</button>
          </div>
        </form>
      </div>
    </div>

    <div class="card shadow-sm">
      <div class="card-body">
        <h2 class="h5 mb-3">Policies</h2>
        <p v-if="!policies.length" class="text-muted mb-0">No policies created yet.</p>
        <div v-else class="table-responsive">
          <table class="table table-striped align-middle mb-0">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Status</th>
                <th>Published Version</th>
                <th>Next Review</th>
                <th style="min-width: 360px;">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="policy in policies" :key="policy.id">
                <td>{{ policy.code }}</td>
                <td>{{ policy.name }}</td>
                <td><span class="badge text-bg-secondary">{{ policy.status }}</span></td>
                <td>{{ policy.publishedVersionId || '-' }}</td>
                <td>{{ formatDate(policy.nextReviewAt) }}</td>
                <td>
                  <div class="d-flex gap-2 flex-wrap">
                    <button class="btn btn-outline-secondary btn-sm" @click="openVersionModal(policy)">
                      New Version
                    </button>
                    <button
                      class="btn btn-outline-primary btn-sm"
                      :disabled="!latestDraftVersion(policy)"
                      @click="publishPolicy(policy)"
                    >
                      Publish Latest Draft
                    </button>
                  </div>
                  <div class="small text-muted mt-1">
                    Versions: {{ (policy.versions || []).map((v) => `v${v.versionNumber} ${v.status}`).join(', ') || 'None' }}
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div v-if="versionModal.open" class="modal-backdrop-custom">
      <div class="card shadow modal-card">
        <div class="card-body">
          <h2 class="h5 mb-3">New Version - {{ versionModal.policy?.code }}</h2>
          <form class="d-grid gap-2" @submit.prevent="submitVersion">
            <input
              v-model="versionModal.title"
              class="form-control form-control-sm"
              placeholder="Version title"
              required
            />
            <textarea
              v-model="versionModal.bodyMarkdown"
              rows="8"
              class="form-control form-control-sm"
              placeholder="Markdown content"
              required
            />
            <div class="d-flex justify-content-end gap-2">
              <button type="button" class="btn btn-outline-secondary btn-sm" @click="closeVersionModal">Cancel</button>
              <button type="submit" class="btn btn-primary btn-sm">Create Version</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const policies = ref([])
const createForm = ref({
  code: '',
  name: '',
  description: '',
  initialVersionTitle: ''
})
const versionModal = ref({
  open: false,
  policy: null,
  title: '',
  bodyMarkdown: ''
})

async function loadPolicies() {
  const res = await api.get('/api/policies')
  policies.value = res.data || []
}

onMounted(async () => {
  await loadPolicies()
})

function latestDraftVersion(policy) {
  return (policy.versions || []).find((v) => v.status === 'DRAFT')
}

async function createPolicy() {
  try {
    await api.post('/api/policies', createForm.value)
    createForm.value = { code: '', name: '', description: '', initialVersionTitle: '' }
    await loadPolicies()
    toastSuccess('Policy created.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create policy')
  }
}

function openVersionModal(policy) {
  versionModal.value = {
    open: true,
    policy,
    title: `${policy.name} - next revision`,
    bodyMarkdown: '## Scope\n\n## Controls\n\n## Exceptions'
  }
}

function closeVersionModal() {
  versionModal.value.open = false
}

async function submitVersion() {
  try {
    await api.post(`/api/policies/${versionModal.value.policy.id}/versions`, {
      title: versionModal.value.title,
      bodyMarkdown: versionModal.value.bodyMarkdown
    })
    closeVersionModal()
    await loadPolicies()
    toastSuccess('Policy version created.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create version')
  }
}

async function publishPolicy(policy) {
  const draft = latestDraftVersion(policy)
  if (!draft) return
  try {
    await api.post(`/api/policies/${policy.id}/publish`, {
      policyVersionId: draft.id
    })
    await loadPolicies()
    toastSuccess('Policy published and attestation tasks generated.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to publish policy')
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString()
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
  width: min(760px, 100%);
}
</style>
