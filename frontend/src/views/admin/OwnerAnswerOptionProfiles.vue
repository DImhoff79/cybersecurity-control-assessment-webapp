<template>
  <div>
    <div class="mb-3">
      <h1 class="h3 mb-1">Owner answer options</h1>
      <p class="text-muted mb-0">
        Dropdown labels and field copy for application-owner audit questions. Questions reference a profile; edit text here without redeploying the app.
      </p>
    </div>

    <div class="card workspace-card border-0 shadow-sm">
      <div class="card-body">
        <div v-if="loading" class="text-muted">Loading profiles…</div>
        <div v-else class="table-responsive">
          <table class="table workspace-table align-middle mb-0">
            <thead>
              <tr>
                <th>Code</th>
                <th>Display name</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in profiles" :key="p.id">
                <td><code class="small">{{ p.code }}</code></td>
                <td>{{ p.displayName }}</td>
                <td class="text-end">
                  <button type="button" class="btn btn-sm btn-secondary" @click="openEdit(p)">Edit</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <BsModal v-model="editOpen" title="Edit owner answer profile" size="lg">
      <form id="profile-edit-form" @submit.prevent="saveEdit">
        <div class="mb-3">
          <label class="form-label">Display name</label>
          <input v-model="editForm.displayName" type="text" class="form-control" required maxlength="200" />
        </div>
        <div class="mb-3">
          <label class="form-label">Field label</label>
          <input v-model="editForm.fieldLabel" type="text" class="form-control" maxlength="500" />
        </div>
        <div class="mb-3">
          <label class="form-label">Field hint</label>
          <textarea v-model="editForm.fieldHint" class="form-control" rows="2" maxlength="2000" />
        </div>
        <div class="mb-2">
          <label class="form-label">Options (JSON array)</label>
          <p class="small text-muted">
            Array of <code>{ "value": "YES", "label": "…" }</code>. Must include values
            <code>UNANSWERED</code>, <code>YES</code>, <code>PARTIAL</code>, <code>NO</code>,
            <code>NOT_APPLICABLE</code>.
          </p>
          <textarea v-model="editForm.optionsJson" class="form-control font-monospace small" rows="12" spellcheck="false" />
        </div>
        <div v-if="saveError" class="alert alert-danger small mb-0">{{ saveError }}</div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="editOpen = false">Cancel</button>
        <button type="submit" form="profile-edit-form" class="btn btn-primary" :disabled="saving">Save</button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import BsModal from '../../components/BsModal.vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const loading = ref(true)
const saving = ref(false)
const profiles = ref([])
const editOpen = ref(false)
const saveError = ref('')
const editForm = ref({
  id: null,
  displayName: '',
  fieldLabel: '',
  fieldHint: '',
  optionsJson: ''
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await api.get('/api/owner-answer-option-profiles')
    profiles.value = res.data || []
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to load profiles.')
  } finally {
    loading.value = false
  }
}

function openEdit(p) {
  saveError.value = ''
  editForm.value = {
    id: p.id,
    displayName: p.displayName || '',
    fieldLabel: p.fieldLabel || '',
    fieldHint: p.fieldHint || '',
    optionsJson: p.optionsJson || '[]'
  }
  editOpen.value = true
}

async function saveEdit() {
  saving.value = true
  saveError.value = ''
  try {
    await api.put(`/api/owner-answer-option-profiles/${editForm.value.id}`, {
      displayName: editForm.value.displayName,
      fieldLabel: editForm.value.fieldLabel || null,
      fieldHint: editForm.value.fieldHint || null,
      optionsJson: editForm.value.optionsJson
    })
    editOpen.value = false
    await load()
    toastSuccess('Profile saved.')
  } catch (e) {
    const msg = e.response?.data?.message || e.response?.data?.error || e.message || 'Save failed.'
    saveError.value = typeof msg === 'string' ? msg : 'Invalid request. Check JSON and required option values.'
    toastError(saveError.value)
  } finally {
    saving.value = false
  }
}
</script>
