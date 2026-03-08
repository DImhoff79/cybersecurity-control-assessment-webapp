<template>
  <div>
    <h1 class="h3 mb-3">Question Manager</h1>

    <div class="card shadow-sm mb-3">
      <div class="card-body row g-3 align-items-end">
        <div class="col-md-5">
          <label class="form-label">Search</label>
          <input v-model="search" class="form-control" placeholder="Search question text or control id" />
        </div>
        <div class="col-md-3">
          <label class="form-label">Owner visibility</label>
          <select v-model="askOwnerFilter" class="form-select">
            <option value="ALL">All questions</option>
            <option value="ASKED">Asked to owners</option>
            <option value="HIDDEN">Hidden from owners</option>
          </select>
        </div>
        <div class="col-md-4 text-md-end">
          <button class="btn btn-primary" @click="load" :disabled="loading">Refresh</button>
        </div>
      </div>
    </div>

    <div class="card shadow-sm">
      <div class="card-body">
        <div v-if="loading" class="text-muted">Loading questions...</div>
        <div v-else class="table-responsive">
          <table class="table table-striped table-hover align-middle mb-0">
            <thead>
              <tr>
                <th style="min-width: 420px;">Question</th>
                <th>Mapped controls</th>
                <th>Ask owners</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="q in filteredQuestions" :key="q.id">
                <td>
                  <div class="fw-semibold">{{ q.questionText }}</div>
                  <div class="small text-muted" v-if="q.helpText">{{ q.helpText }}</div>
                </td>
                <td>
                  <div class="d-flex flex-wrap gap-1">
                    <span v-for="c in q.controls.slice(0, 4)" :key="`${q.id}-${c.id}`" class="badge text-bg-light border">
                      {{ c.controlId }}
                    </span>
                    <span v-if="q.controls.length > 4" class="badge text-bg-secondary">+{{ q.controls.length - 4 }} more</span>
                  </div>
                </td>
                <td>
                  <div class="form-check form-switch m-0">
                    <input
                      class="form-check-input"
                      type="checkbox"
                      :checked="q.askOwner"
                      @change="toggleAskOwner(q, $event.target.checked)"
                    />
                  </div>
                </td>
                <td class="text-nowrap">
                  <button class="btn btn-secondary btn-sm" @click="openEdit(q)">Edit</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div v-if="toast.show" class="position-fixed bottom-0 end-0 p-3" style="z-index: 1080;">
      <div class="toast show align-items-center text-bg-success border-0" role="status" aria-live="polite" aria-atomic="true">
        <div class="d-flex">
          <div class="toast-body">
            <strong class="me-2">Last updated</strong>
            <span>{{ toast.message }}</span>
            <div class="small text-white-50 mt-1">{{ toast.at }}</div>
          </div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" aria-label="Close" @click="toast.show = false" />
        </div>
      </div>
    </div>

    <BsModal v-model="isEditOpen" title="Edit question" size="lg">
      <form id="question-edit-form" @submit.prevent="saveEdit">
        <div class="mb-3">
          <label class="form-label">Question text</label>
          <textarea v-model="editForm.questionText" class="form-control" rows="3" required />
        </div>
        <div class="mb-3">
          <label class="form-label">Help text</label>
          <textarea v-model="editForm.helpText" class="form-control" rows="3" />
        </div>
        <div class="mb-3">
          <label class="form-label">Mapped controls</label>
          <div class="d-flex flex-wrap gap-1">
            <span v-for="c in editForm.controls" :key="`edit-${c.id}`" class="badge text-bg-light border">{{ c.controlId }} - {{ c.name }}</span>
          </div>
        </div>
        <div class="form-check form-switch">
          <input id="askOwnerSwitch" class="form-check-input" type="checkbox" v-model="editForm.askOwner" />
          <label for="askOwnerSwitch" class="form-check-label">Ask this question to application owners</label>
        </div>
        <hr />
        <h3 class="h6">Primary Mapping Metadata</h3>
        <p class="small text-muted">Edits apply to the primary mapped control for this question.</p>
        <div class="mb-3">
          <label class="form-label">Mapping rationale</label>
          <textarea v-model="editForm.mappingRationale" class="form-control" rows="2" />
        </div>
        <div class="row g-3">
          <div class="col-md-4">
            <label class="form-label">Weight</label>
            <input v-model.number="editForm.mappingWeight" type="number" step="0.01" min="0" max="100" class="form-control" />
          </div>
          <div class="col-md-4">
            <label class="form-label">Effective from</label>
            <input v-model="editForm.effectiveFrom" type="datetime-local" class="form-control" />
          </div>
          <div class="col-md-4">
            <label class="form-label">Effective to</label>
            <input v-model="editForm.effectiveTo" type="datetime-local" class="form-control" />
          </div>
        </div>
        <hr />
        <h3 class="h6">Manage control mappings</h3>
        <p class="small text-muted">Add or remove mapped controls and update metadata per mapping.</p>
        <div class="d-flex gap-2 align-items-end mb-3">
          <div class="flex-grow-1">
            <label class="form-label">Add mapping to control</label>
            <select v-model="newMappingControlId" class="form-select">
              <option :value="null">Select control</option>
              <option v-for="c in availableMappingControls" :key="`map-${c.id}`" :value="c.id">
                {{ c.controlId }} - {{ c.name }}
              </option>
            </select>
          </div>
          <button type="button" class="btn btn-outline-primary" @click="addMapping">Add mapping</button>
        </div>
        <div class="table-responsive">
          <table class="table table-sm align-middle">
            <thead>
              <tr>
                <th>Control</th>
                <th>Rationale</th>
                <th>Weight</th>
                <th>Effective from</th>
                <th>Effective to</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in editForm.controls" :key="`mapping-row-${m.id}`">
                <td>{{ m.controlId }}</td>
                <td><input v-model="m.mappingRationale" class="form-control form-control-sm" /></td>
                <td><input v-model.number="m.mappingWeight" type="number" min="0" max="100" step="0.01" class="form-control form-control-sm" /></td>
                <td><input v-model="m.effectiveFrom" type="datetime-local" class="form-control form-control-sm" /></td>
                <td><input v-model="m.effectiveTo" type="datetime-local" class="form-control form-control-sm" /></td>
                <td class="text-nowrap">
                  <button type="button" class="btn btn-outline-success btn-sm me-2" @click="saveMapping(m)">Save</button>
                  <button type="button" class="btn btn-outline-danger btn-sm" @click="removeMapping(m)">Remove</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="isEditOpen = false">Cancel</button>
        <button type="submit" form="question-edit-form" class="btn btn-primary">Save changes</button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import BsModal from '../../components/BsModal.vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const loading = ref(false)
const search = ref('')
const askOwnerFilter = ref('ALL')
const questions = ref([])
const allControls = ref([])

const editModal = ref(null)
const editForm = ref({
  id: null,
  controlId: null,
  questionText: '',
  helpText: '',
  askOwner: true,
  mappingRationale: '',
  mappingWeight: null,
  effectiveFrom: '',
  effectiveTo: '',
  controls: []
})
const toast = ref({ show: false, message: '', at: '' })
let toastTimer = null
const newMappingControlId = ref(null)

const isEditOpen = computed({
  get: () => !!editModal.value,
  set: (open) => {
    if (!open) editModal.value = null
  }
})

const filteredQuestions = computed(() => {
  const term = search.value.trim().toLowerCase()
  return questions.value.filter((q) => {
    const matchFilter = askOwnerFilter.value === 'ALL'
      || (askOwnerFilter.value === 'ASKED' && q.askOwner)
      || (askOwnerFilter.value === 'HIDDEN' && !q.askOwner)

    if (!matchFilter) return false
    if (!term) return true

    const controlMatch = q.controls.some((c) => `${c.controlId} ${c.name}`.toLowerCase().includes(term))
    return q.questionText.toLowerCase().includes(term) || (q.helpText || '').toLowerCase().includes(term) || controlMatch
  })
})

const availableMappingControls = computed(() => {
  const mapped = new Set((editForm.value.controls || []).map((c) => c.id))
  return allControls.value.filter((c) => !mapped.has(c.id))
})

load()

async function load() {
  loading.value = true
  try {
    const res = await api.get('/api/controls?includeQuestions=true')
    const controls = res.data || []
    allControls.value = controls.map((c) => ({ id: c.id, controlId: c.controlId, name: c.name }))

    const byQuestion = new Map()
    controls.forEach((control) => {
      ;(control.questions || []).forEach((q) => {
        if (!byQuestion.has(q.id)) {
          byQuestion.set(q.id, {
            id: q.id,
            controlId: q.controlId,
            questionText: q.questionText,
            helpText: q.helpText || '',
            askOwner: q.askOwner !== false,
            mappingRationale: q.mappingRationale || '',
            mappingWeight: q.mappingWeight ?? null,
            effectiveFrom: q.effectiveFrom || '',
            effectiveTo: q.effectiveTo || '',
            controls: []
          })
        }
        byQuestion.get(q.id).controls.push({
          id: control.id,
          controlId: control.controlId,
          name: control.name,
          mappingRationale: q.mappingRationale || '',
          mappingWeight: q.mappingWeight ?? null,
          effectiveFrom: toDateTimeLocal(q.effectiveFrom),
          effectiveTo: toDateTimeLocal(q.effectiveTo)
        })
      })
    })

    questions.value = Array.from(byQuestion.values())
      .sort((a, b) => a.questionText.localeCompare(b.questionText))
  } finally {
    loading.value = false
  }
}

async function toggleAskOwner(question, enabled) {
  const previous = question.askOwner
  question.askOwner = enabled
  try {
    await api.put(`/api/controls/${question.controlId}/questions/${question.id}`, { askOwner: enabled })
    await load()
    showUpdatedToast(`Question visibility changed to ${enabled ? 'shown' : 'hidden'} for owners.`)
  } catch (e) {
    question.askOwner = previous
    toastError(e.response?.data?.error || 'Failed to update question visibility.')
  }
}

function openEdit(question) {
  editModal.value = question
  editForm.value = {
    id: question.id,
    controlId: question.controlId,
    questionText: question.questionText,
    helpText: question.helpText,
    askOwner: question.askOwner,
    mappingRationale: question.mappingRationale || '',
    mappingWeight: question.mappingWeight ?? null,
    effectiveFrom: toDateTimeLocal(question.effectiveFrom),
    effectiveTo: toDateTimeLocal(question.effectiveTo),
    controls: [...question.controls]
  }
  newMappingControlId.value = null
}

async function saveEdit() {
  try {
    await api.put(`/api/controls/${editForm.value.controlId}/questions/${editForm.value.id}`, {
      questionText: editForm.value.questionText,
      helpText: editForm.value.helpText,
      askOwner: editForm.value.askOwner
    })
    await api.put(`/api/controls/${editForm.value.controlId}/questions/${editForm.value.id}/mapping`, {
      mappingRationale: editForm.value.mappingRationale || null,
      mappingWeight: editForm.value.mappingWeight != null ? editForm.value.mappingWeight : null,
      effectiveFrom: editForm.value.effectiveFrom ? new Date(editForm.value.effectiveFrom).toISOString() : null,
      effectiveTo: editForm.value.effectiveTo ? new Date(editForm.value.effectiveTo).toISOString() : null
    })
    isEditOpen.value = false
    await load()
    showUpdatedToast('Question details and mapping metadata saved.')
    toastSuccess('Question updated.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to save question changes.')
  }
}

async function addMapping() {
  if (!editForm.value.id || !newMappingControlId.value) return
  try {
    await api.post(`/api/controls/${newMappingControlId.value}/questions`, {
      questionText: editForm.value.questionText,
      helpText: editForm.value.helpText,
      askOwner: editForm.value.askOwner
    })
    await load()
    const refreshed = questions.value.find((q) => q.id === editForm.value.id)
    if (refreshed) openEdit(refreshed)
    toastSuccess('Mapping added.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to add mapping.')
  }
}

async function saveMapping(mapping) {
  try {
    await api.put(`/api/controls/${mapping.id}/questions/${editForm.value.id}/mapping`, {
      mappingRationale: mapping.mappingRationale || null,
      mappingWeight: mapping.mappingWeight != null ? mapping.mappingWeight : null,
      effectiveFrom: mapping.effectiveFrom ? new Date(mapping.effectiveFrom).toISOString() : null,
      effectiveTo: mapping.effectiveTo ? new Date(mapping.effectiveTo).toISOString() : null
    })
    await load()
    const refreshed = questions.value.find((q) => q.id === editForm.value.id)
    if (refreshed) openEdit(refreshed)
    toastSuccess('Mapping saved.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to save mapping.')
  }
}

async function removeMapping(mapping) {
  if (!confirm(`Remove mapping to ${mapping.controlId}?`)) return
  try {
    await api.delete(`/api/controls/${mapping.id}/questions/${editForm.value.id}`)
    await load()
    const refreshed = questions.value.find((q) => q.id === editForm.value.id)
    if (refreshed) {
      openEdit(refreshed)
    } else {
      isEditOpen.value = false
    }
    toastSuccess('Mapping removed.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to remove mapping.')
  }
}

function showUpdatedToast(message) {
  const now = new Date()
  toast.value = {
    show: true,
    message,
    at: now.toLocaleTimeString()
  }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.value.show = false
  }, 3500)
}

function toDateTimeLocal(value) {
  if (!value) return ''
  const d = new Date(value)
  const pad = (n) => `${n}`.padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>
