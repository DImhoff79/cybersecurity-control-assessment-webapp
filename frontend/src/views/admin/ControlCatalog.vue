<template>
  <div>
    <h1 v-if="!embedded" class="h3 mb-3">Control catalog</h1>
    <div v-if="!embedded && cameFromGovernance" class="alert alert-info d-flex flex-wrap justify-content-between align-items-center gap-2">
      <div class="small">
        You are editing controls as part of the questionnaire governance workflow.
      </div>
      <div class="d-flex gap-2">
        <router-link :to="{ name: 'AdminQuestions', query: { from: 'governance' } }" class="btn btn-outline-primary btn-sm">
          Go to Questions
        </router-link>
        <router-link :to="{ name: 'AdminQuestionnaireTemplates' }" class="btn btn-primary btn-sm">
          Back to Governance
        </router-link>
      </div>
    </div>

    <div class="card shadow-sm mb-3">
      <div class="card-body row g-3 align-items-end">
        <div class="col-md-4">
          <label class="form-label">Framework</label>
          <select v-model="filterFramework" class="form-select">
            <option value="">All</option>
            <option value="NIST_800_53_LOW">NIST 800-53 Low</option>
            <option value="PCI_DSS_V4">PCI DSS v4</option>
            <option value="HIPAA">HIPAA</option>
            <option value="SOX">SOX</option>
          </select>
        </div>
        <div class="col-md-4">
          <div class="form-check">
            <input type="checkbox" v-model="filterEnabled" class="form-check-input" id="enabledOnly" />
            <label class="form-check-label" for="enabledOnly">Enabled only</label>
          </div>
        </div>
        <div class="col-md-4 text-md-end">
          <button class="btn btn-primary" @click="openCreate">Add control</button>
        </div>
      </div>
    </div>

    <div class="card shadow-sm">
      <div class="card-body">
        <div class="table-responsive">
          <table class="table table-striped table-hover align-middle mb-0">
            <thead>
              <tr>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleControlSort('controlId')">ID {{ controlSortIndicator('controlId') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleControlSort('name')">Name {{ controlSortIndicator('name') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleControlSort('framework')">Framework {{ controlSortIndicator('framework') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleControlSort('enabled')">Enabled {{ controlSortIndicator('enabled') }}</button></th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in sortedControls" :key="c.id">
                <td>{{ c.controlId }}</td>
                <td>
                  <button class="btn btn-link p-0 text-decoration-none" type="button" @click="openControlDetails(c.id)">
                    {{ c.name }}
                  </button>
                </td>
                <td>{{ c.framework }}</td>
                <td>
                  <input type="checkbox" :checked="c.enabled" @change="toggleEnabled(c)" class="form-check-input" />
                </td>
                <td class="text-nowrap">
                  <button class="btn btn-secondary btn-sm me-2" @click="openEdit(c)">Edit</button>
                  <button class="btn btn-secondary btn-sm" @click="openQuestions(c)">Questions</button>
                  <button class="btn btn-danger btn-sm ms-2" @click="deleteControl(c)">Delete</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <BsModal v-model="isEditOpen" title="Edit control">
      <form id="edit-control-form" @submit.prevent="saveControl">
        <div class="mb-3">
          <label class="form-label">Name</label>
          <input v-model="editForm.name" class="form-control" />
        </div>
        <div class="mb-3">
          <label class="form-label">Description</label>
          <textarea v-model="editForm.description" class="form-control" />
        </div>
        <div class="form-check mb-3">
          <input type="checkbox" v-model="editForm.enabled" class="form-check-input" id="enabledControl" />
          <label class="form-check-label" for="enabledControl">Enabled</label>
        </div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="isEditOpen = false">Cancel</button>
        <button type="submit" form="edit-control-form" class="btn btn-primary">Save</button>
      </template>
    </BsModal>

    <BsModal v-model="isCreateOpen" title="Add control">
      <form id="create-control-form" @submit.prevent="createControl">
        <div class="mb-3">
          <label class="form-label">Control ID</label>
          <input v-model="createForm.controlId" class="form-control" placeholder="AC-1" required />
        </div>
        <div class="mb-3">
          <label class="form-label">Name</label>
          <input v-model="createForm.name" class="form-control" required />
        </div>
        <div class="mb-3">
          <label class="form-label">Framework</label>
          <select v-model="createForm.framework" class="form-select" required>
            <option disabled value="">Select framework</option>
            <option value="NIST_800_53_LOW">NIST 800-53 Low</option>
            <option value="PCI_DSS_V4">PCI DSS v4</option>
            <option value="HIPAA">HIPAA</option>
            <option value="SOX">SOX</option>
          </select>
        </div>
        <div class="mb-3">
          <label class="form-label">Category</label>
          <input v-model="createForm.category" class="form-control" placeholder="Access Control" />
        </div>
        <div class="mb-3">
          <label class="form-label">Description</label>
          <textarea v-model="createForm.description" class="form-control" />
        </div>
        <div class="form-check mb-3">
          <input type="checkbox" v-model="createForm.enabled" class="form-check-input" id="enabledNewControl" />
          <label class="form-check-label" for="enabledNewControl">Enabled</label>
        </div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="isCreateOpen = false">Cancel</button>
        <button type="submit" form="create-control-form" class="btn btn-primary">Create</button>
      </template>
    </BsModal>

    <BsModal v-model="isQuestionsOpen" :title="questionsTitle" size="lg">
      <div class="table-responsive">
        <table class="table table-striped table-hover align-middle">
          <thead>
            <tr>
              <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleQuestionSort('questionText')">Question {{ questionSortIndicator('questionText') }}</button></th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="q in sortedQuestions" :key="q.id">
              <td>{{ q.questionText }}</td>
              <td class="text-nowrap">
                <button class="btn btn-secondary btn-sm me-2" @click="editQuestion(q)">Edit</button>
                <button class="btn btn-danger btn-sm" @click="deleteQuestion(q.id)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <form @submit.prevent="addQuestion" class="border-top pt-3 mt-3">
        <div class="mb-3">
          <label class="form-label">New question</label>
          <input v-model="newQuestionText" placeholder="Plain English question" class="form-control" />
        </div>
        <button type="submit" class="btn btn-primary">Add question</button>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="isQuestionsOpen = false">Close</button>
      </template>
    </BsModal>

    <BsModal v-model="isDetailsOpen" :title="detailsTitle" size="lg">
      <p><strong>Framework:</strong> {{ detailsModal?.framework }}</p>
      <p><strong>Category:</strong> {{ detailsModal?.category || 'General' }}</p>
      <p><strong>Description:</strong> {{ detailsModal?.description || 'No description available yet.' }}</p>

      <h3 class="h6">Examples / Guidance</h3>
      <ul v-if="detailExamples.length">
        <li v-for="(item, idx) in detailExamples" :key="idx">{{ item }}</li>
      </ul>
      <p v-else class="text-muted">No examples available yet. Add plain-English questions/help text for this control.</p>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="isDetailsOpen = false">Close</button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import BsModal from '../../components/BsModal.vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'
import { useTableSort } from '../../composables/useTableSort'

defineProps({
  embedded: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()
const controls = ref([])
const filterFramework = ref('')
const filterEnabled = ref(false)
const createModal = ref(false)
const createForm = ref({ controlId: '', name: '', framework: '', description: '', enabled: true, category: '' })
const editModal = ref(null)
const editForm = ref({ id: null, name: '', description: '', enabled: true })
const questionsModal = ref(null)
const questionsList = ref([])
const newQuestionText = ref('')
const detailsModal = ref(null)
const cameFromGovernance = computed(() => route.query.from === 'governance')

const filteredControls = computed(() => {
  let list = controls.value
  if (filterFramework.value) list = list.filter((c) => c.framework === filterFramework.value)
  if (filterEnabled.value) list = list.filter((c) => c.enabled)
  return list
})

const { sortedRows: sortedControls, toggleSort: toggleControlSort, sortIndicator: controlSortIndicator } = useTableSort(filteredControls, {
  initialKey: 'controlId'
})

const { sortedRows: sortedQuestions, toggleSort: toggleQuestionSort, sortIndicator: questionSortIndicator } = useTableSort(questionsList, {
  initialKey: 'questionText'
})

const isEditOpen = computed({
  get: () => !!editModal.value,
  set: (open) => {
    if (!open) editModal.value = null
  }
})

const isCreateOpen = computed({
  get: () => createModal.value,
  set: (open) => {
    createModal.value = open
  }
})

const isQuestionsOpen = computed({
  get: () => !!questionsModal.value,
  set: (open) => {
    if (!open) questionsModal.value = null
  }
})

const isDetailsOpen = computed({
  get: () => !!detailsModal.value,
  set: (open) => {
    if (!open) detailsModal.value = null
  }
})

const questionsTitle = computed(() => {
  if (!questionsModal.value) return 'Questions'
  return `Questions for ${questionsModal.value.controlId} - ${questionsModal.value.name}`
})

const detailsTitle = computed(() => {
  if (!detailsModal.value) return 'Control details'
  return `${detailsModal.value.controlId} - ${detailsModal.value.name}`
})

const detailExamples = computed(() => {
  if (!detailsModal.value?.questions?.length) return []
  return detailsModal.value.questions
    .map((q) => q.helpText || q.questionText)
    .filter(Boolean)
    .slice(0, 5)
})

onMounted(load)

async function load() {
  const res = await api.get('/api/controls?includeQuestions=false')
  controls.value = res.data || []
}

function openCreate() {
  createForm.value = { controlId: '', name: '', framework: '', description: '', enabled: true, category: '' }
  createModal.value = true
}

async function createControl() {
  try {
    await api.post('/api/controls', {
      controlId: createForm.value.controlId,
      name: createForm.value.name,
      framework: createForm.value.framework,
      description: createForm.value.description || null,
      enabled: createForm.value.enabled,
      category: createForm.value.category || null
    })
    createModal.value = false
    await load()
    toastSuccess('Control created.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create control')
  }
}

async function toggleEnabled(c) {
  try {
    await api.patch(`/api/controls/${c.id}`, { enabled: !c.enabled })
    await load()
    toastSuccess('Control updated.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update')
  }
}

function openEdit(c) {
  editModal.value = c
  editForm.value = { id: c.id, name: c.name, description: c.description || '', enabled: c.enabled }
}

async function saveControl() {
  try {
    await api.patch(`/api/controls/${editForm.value.id}`, {
      name: editForm.value.name,
      description: editForm.value.description,
      enabled: editForm.value.enabled
    })
    editModal.value = null
    await load()
    toastSuccess('Control saved.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to save')
  }
}

async function deleteControl(control) {
  if (!confirm(`Delete control ${control.controlId}? This removes its mappings.`)) return
  try {
    await api.delete(`/api/controls/${control.id}`)
    await load()
    toastSuccess('Control deleted.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to delete control')
  }
}

async function openQuestions(c) {
  questionsModal.value = c
  const res = await api.get(`/api/controls/${c.id}/questions`)
  questionsList.value = res.data || []
  newQuestionText.value = ''
}

async function addQuestion() {
  if (!newQuestionText.value.trim() || !questionsModal.value) return
  try {
    await api.post(`/api/controls/${questionsModal.value.id}/questions`, {
      questionText: newQuestionText.value.trim()
    })
    await openQuestions(questionsModal.value)
    toastSuccess('Question added.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to add question')
  }
}

async function deleteQuestion(id) {
  if (!confirm('Delete this question?')) return
  try {
    await api.delete(`/api/controls/${questionsModal.value.id}/questions/${id}`)
    await openQuestions(questionsModal.value)
    toastSuccess('Question deleted.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to delete')
  }
}

function editQuestion(q) {
  const text = prompt('Edit question text:', q.questionText)
  if (text == null) return
  api.put(`/api/controls/${questionsModal.value.id}/questions/${q.id}`, { questionText: text })
    .then(() => {
      toastSuccess('Question updated.')
      return openQuestions(questionsModal.value)
    })
    .catch((e) => toastError(e.response?.data?.error || 'Failed to update'))
}

async function openControlDetails(controlId) {
  try {
    const res = await api.get(`/api/controls/${controlId}?includeQuestions=true`)
    detailsModal.value = res.data
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to load control details')
  }
}
</script>
