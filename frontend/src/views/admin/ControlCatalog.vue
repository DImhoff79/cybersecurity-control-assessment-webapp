<template>
  <div>
    <h1 class="page-title">Control catalog</h1>
    <div class="filters card">
      <label>Framework</label>
      <select v-model="filterFramework">
        <option value="">All</option>
        <option value="NIST_800_53_LOW">NIST 800-53 Low</option>
        <option value="PCI_DSS_V4">PCI DSS v4</option>
        <option value="HIPAA">HIPAA</option>
        <option value="SOX">SOX</option>
      </select>
      <label><input type="checkbox" v-model="filterEnabled" /> Enabled only</label>
    </div>
    <div class="card">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Framework</th>
            <th>Enabled</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in filteredControls" :key="c.id">
            <td>{{ c.controlId }}</td>
            <td>
              <button class="link-btn" type="button" @click="openControlDetails(c.id)">
                {{ c.name }}
              </button>
            </td>
            <td>{{ c.framework }}</td>
            <td>
              <input type="checkbox" :checked="c.enabled" @change="toggleEnabled(c)" />
            </td>
            <td>
              <button class="btn btn-secondary btn-sm" @click="openEdit(c)">Edit</button>
              <button class="btn btn-secondary btn-sm" @click="openQuestions(c)">Questions</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="editModal" class="modal-overlay" @click.self="editModal = null">
      <div class="modal card">
        <h2>Edit control</h2>
        <form @submit.prevent="saveControl">
          <div class="form-group">
            <label>Name</label>
            <input v-model="editForm.name" />
          </div>
          <div class="form-group">
            <label>Description</label>
            <textarea v-model="editForm.description" />
          </div>
          <div class="form-group">
            <label><input type="checkbox" v-model="editForm.enabled" /> Enabled</label>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="editModal = null">Cancel</button>
            <button type="submit" class="btn btn-primary">Save</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="questionsModal" class="modal-overlay" @click.self="questionsModal = null">
      <div class="modal card modal-wide">
        <h2>Questions for {{ questionsModal.controlId }} – {{ questionsModal.name }}</h2>
        <table>
          <thead>
            <tr>
              <th>Question</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="q in questionsList" :key="q.id">
              <td>{{ q.questionText }}</td>
              <td>
                <button class="btn btn-secondary btn-sm" @click="editQuestion(q)">Edit</button>
                <button class="btn btn-danger btn-sm" @click="deleteQuestion(q.id)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
        <form @submit.prevent="addQuestion" class="add-question">
          <div class="form-group">
            <label>New question</label>
            <input v-model="newQuestionText" placeholder="Plain English question" />
          </div>
          <button type="submit" class="btn btn-primary">Add question</button>
        </form>
        <button class="btn btn-secondary" @click="questionsModal = null">Close</button>
      </div>
    </div>

    <div v-if="detailsModal" class="modal-overlay" @click.self="detailsModal = null">
      <div class="modal card modal-wide">
        <h2>{{ detailsModal.controlId }} - {{ detailsModal.name }}</h2>
        <p><strong>Framework:</strong> {{ detailsModal.framework }}</p>
        <p><strong>Category:</strong> {{ detailsModal.category || 'General' }}</p>
        <p><strong>Description:</strong> {{ detailsModal.description || 'No description available yet.' }}</p>

        <h3>Examples / Guidance</h3>
        <ul v-if="detailExamples.length">
          <li v-for="(item, idx) in detailExamples" :key="idx">{{ item }}</li>
        </ul>
        <p v-else>No examples available yet. Add plain-English questions/help text for this control.</p>

        <button class="btn btn-secondary" @click="detailsModal = null">Close</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../../services/api'

const controls = ref([])
const filterFramework = ref('')
const filterEnabled = ref(false)
const editModal = ref(null)
const editForm = ref({ id: null, name: '', description: '', enabled: true })
const questionsModal = ref(null)
const questionsList = ref([])
const newQuestionText = ref('')
const detailsModal = ref(null)

const filteredControls = computed(() => {
  let list = controls.value
  if (filterFramework.value) list = list.filter((c) => c.framework === filterFramework.value)
  if (filterEnabled.value) list = list.filter((c) => c.enabled)
  return list
})

onMounted(load)

async function load() {
  const res = await api.get('/api/controls?includeQuestions=false')
  controls.value = res.data || []
}

async function toggleEnabled(c) {
  try {
    await api.patch(`/api/controls/${c.id}`, { enabled: !c.enabled })
    load()
  } catch (e) {
    alert('Failed to update')
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
    load()
  } catch (e) {
    alert('Failed to save')
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
    openQuestions(questionsModal.value)
  } catch (e) {
    alert('Failed to add question')
  }
}

async function deleteQuestion(id) {
  if (!confirm('Delete this question?')) return
  try {
    await api.delete(`/api/controls/${questionsModal.value.id}/questions/${id}`)
    openQuestions(questionsModal.value)
  } catch (e) {
    alert('Failed to delete')
  }
}

function editQuestion(q) {
  const text = prompt('Edit question text:', q.questionText)
  if (text == null) return
  api.put(`/api/controls/${questionsModal.value.id}/questions/${q.id}`, { questionText: text }).then(() => {
    openQuestions(questionsModal.value)
  }).catch(() => alert('Failed to update'))
}

const detailExamples = computed(() => {
  if (!detailsModal.value?.questions?.length) return []
  return detailsModal.value.questions
    .map((q) => q.helpText || q.questionText)
    .filter(Boolean)
    .slice(0, 5)
})

async function openControlDetails(controlId) {
  try {
    const res = await api.get(`/api/controls/${controlId}?includeQuestions=true`)
    detailsModal.value = res.data
  } catch (e) {
    alert('Failed to load control details')
  }
}
</script>

<style scoped>
.filters { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; }
.filters label { margin: 0; }
.link-btn {
  border: none;
  background: none;
  color: #2b6cb0;
  cursor: pointer;
  padding: 0;
  text-align: left;
  font: inherit;
}
.link-btn:hover { text-decoration: underline; }
.btn-sm { padding: 0.35rem 0.75rem; font-size: 0.85rem; margin-right: 0.25rem; }
.form-actions { display: flex; gap: 0.75rem; margin-top: 1rem; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { max-width: 520px; width: 90%; }
.modal-wide { max-width: 640px; }
.modal h2 { margin-top: 0; }
.add-question { margin: 1rem 0; padding-top: 1rem; border-top: 1px solid #e2e8f0; }
</style>
