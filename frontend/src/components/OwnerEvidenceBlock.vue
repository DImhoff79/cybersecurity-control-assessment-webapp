<template>
  <div class="owner-evidence-block" :class="{ 'owner-evidence-block--compact': compact }">
    <div v-if="showTitle" class="owner-evidence-block__title">{{ controlLabel || 'Supporting evidence' }}</div>
    <p v-if="!compact" class="owner-evidence-block__hint small text-muted mb-2">
      Upload files for auditors to review. Include a clear description (min 8 characters). Max 10MB.
    </p>
    <p v-else class="owner-evidence-block__hint-compact small text-muted mb-2">
      Optional files · PDF, images, Office, CSV · max 10MB · description ≥ 8 characters
    </p>

    <div v-if="!readOnly" class="mb-2">
      <label class="form-label small mb-1">Notes for this control</label>
      <textarea
        :value="notes"
        :rows="compact ? 2 : 3"
        class="form-control"
        placeholder="Optional context for the assessor"
        @blur="onNotesBlur"
      />
    </div>
    <div v-else-if="notes" class="small mb-2 text-muted">
      <span class="fw-semibold">Notes:</span> {{ notes }}
    </div>

    <div v-if="loading" class="small text-muted py-1">Loading files…</div>
    <div v-else>
      <ul v-if="evidences.length" class="list-unstyled mb-2 small owner-evidence-block__file-list">
        <li v-for="ev in evidences" :key="ev.id" class="d-flex flex-wrap align-items-start gap-2 py-1 border-bottom border-light">
          <div class="flex-grow-1 min-w-0">
            <span class="fw-medium text-break">{{ ev.fileName || ev.title }}</span>
            <span v-if="ev.reviewStatus" class="badge text-bg-light border ms-1">{{ ev.reviewStatus }}</span>
            <div v-if="ev.notes" class="text-muted text-break">{{ ev.notes }}</div>
          </div>
          <button
            type="button"
            class="btn btn-outline-secondary btn-sm flex-shrink-0"
            :disabled="!ev.uri || ev.lifecycleStatus === 'DISPOSED'"
            @click="downloadEvidence(ev)"
          >
            Download
          </button>
        </li>
      </ul>
      <p v-else class="small text-muted mb-2 mb-lg-0">No files uploaded yet.</p>

      <div v-if="!readOnly" class="owner-evidence-block__upload">
        <label class="form-label small mb-1">Describe the file, then choose it</label>
        <textarea
          v-model="draft.description"
          class="form-control mb-2"
          :rows="compact ? 2 : 2"
          placeholder="What does this file show?"
        />
        <div class="d-flex flex-column flex-sm-row gap-2 align-items-stretch align-items-sm-end">
          <div class="flex-grow-1">
            <input
              type="file"
              accept=".pdf,.txt,.csv,.png,.jpg,.jpeg,.doc,.docx,.xls,.xlsx"
              class="form-control"
              @change="onFile"
            />
          </div>
          <button type="button" class="btn btn-primary btn-sm" :disabled="uploading" @click="upload">Upload</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import api from '../services/api'
import { toastError, toastSuccess, toastWarning } from '../services/toast'

const props = defineProps({
  auditControlId: { type: Number, required: true },
  controlLabel: { type: String, default: '' },
  notes: { type: String, default: '' },
  readOnly: { type: Boolean, default: false },
  /** Tighter layout for nested / accordion use */
  compact: { type: Boolean, default: false },
  /** Show the control ID line; parent can hide when redundant */
  showTitle: { type: Boolean, default: true }
})

const emit = defineEmits(['control-updated'])

const loading = ref(true)
const uploading = ref(false)
const evidences = ref([])
const draft = ref({ description: '' })
const file = ref(null)

watch(
  () => props.auditControlId,
  () => {
    loadEvidences()
  }
)

onMounted(() => {
  loadEvidences()
})

async function loadEvidences() {
  loading.value = true
  try {
    const res = await api.get(`/api/audit-controls/${props.auditControlId}/evidences`)
    evidences.value = res.data || []
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to load evidence')
    evidences.value = []
  } finally {
    loading.value = false
  }
}

function onFile(e) {
  file.value = e.target?.files?.[0] || null
}

async function onNotesBlur(ev) {
  if (props.readOnly) return
  const value = ev.target?.value ?? ''
  try {
    await api.put(`/api/audit-controls/${props.auditControlId}`, { notes: value })
    emit('control-updated')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to save notes')
  }
}

async function upload() {
  if (!file.value) {
    toastWarning('Select a file to upload.')
    return
  }
  const description = draft.value.description?.trim()
  if (!description) {
    toastWarning('Enter a description for the evidence (min 8 characters).')
    return
  }
  if (description.length < 8) {
    toastWarning('Description must be at least 8 characters.')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file.value)
    formData.append('description', description)
    await api.post(`/api/audit-controls/${props.auditControlId}/evidences/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    draft.value.description = ''
    file.value = null
    await loadEvidences()
    emit('control-updated')
    toastSuccess('Evidence uploaded.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to upload evidence')
  } finally {
    uploading.value = false
  }
}

async function downloadEvidence(evidence) {
  if (!evidence?.uri || evidence.lifecycleStatus === 'DISPOSED') {
    toastWarning('This file is no longer available.')
    return
  }
  try {
    const cred = localStorage.getItem('auth_credentials')
    const response = await fetch(evidence.uri, {
      method: 'GET',
      credentials: 'include',
      headers: cred ? { Authorization: `Basic ${cred}` } : {}
    })
    if (!response.ok) {
      throw new Error(`Download failed (${response.status})`)
    }
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = evidence.fileName || evidence.title || 'evidence.bin'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    toastError(e?.response?.data?.error || e?.message || 'Failed to download')
  }
}
</script>

<style scoped>
.owner-evidence-block {
  border-radius: 0.375rem;
  padding: 0.75rem 0.75rem 0.75rem 1rem;
  border: 1px solid #e9ecef;
  background: #fafbfc;
  font-size: 1rem;
  line-height: 1.5;
  font-family: inherit;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.owner-evidence-block--compact {
  padding: 0.5rem 0.5rem 0.5rem 0.75rem;
  border-left: 3px solid #0d6efd33;
  border-top: none;
  border-right: none;
  border-bottom: none;
  background: #fff;
}

.owner-evidence-block__title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #495057;
  margin-bottom: 0.35rem;
}

.owner-evidence-block__file-list {
  margin-bottom: 0;
}

.owner-evidence-block__upload {
  padding-top: 0.25rem;
}

.owner-evidence-block .form-control {
  max-width: 100%;
}
</style>
