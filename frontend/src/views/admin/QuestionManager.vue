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

const loading = ref(false)
const search = ref('')
const askOwnerFilter = ref('ALL')
const questions = ref([])

const editModal = ref(null)
const editForm = ref({ id: null, controlId: null, questionText: '', helpText: '', askOwner: true, controls: [] })
const toast = ref({ show: false, message: '', at: '' })
let toastTimer = null

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

load()

async function load() {
  loading.value = true
  try {
    const res = await api.get('/api/controls?includeQuestions=true')
    const controls = res.data || []

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
            controls: []
          })
        }
        byQuestion.get(q.id).controls.push({
          id: control.id,
          controlId: control.controlId,
          name: control.name
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
    alert(e.response?.data?.error || 'Failed to update question visibility.')
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
    controls: [...question.controls]
  }
}

async function saveEdit() {
  try {
    await api.put(`/api/controls/${editForm.value.controlId}/questions/${editForm.value.id}`, {
      questionText: editForm.value.questionText,
      helpText: editForm.value.helpText,
      askOwner: editForm.value.askOwner
    })
    isEditOpen.value = false
    await load()
    showUpdatedToast('Question details saved.')
  } catch (e) {
    alert(e.response?.data?.error || 'Failed to save question changes.')
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
</script>
