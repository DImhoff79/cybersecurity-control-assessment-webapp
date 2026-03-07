<template>
  <div>
    <h1 class="page-title">{{ audit?.applicationName }} – {{ audit?.year }} (Assessment view)</h1>
    <p v-if="audit">
      Status: {{ audit.status }} |
      Assigned to: {{ audit.assignedToDisplayName || audit.assignedToEmail || '—' }}
    </p>
    <div v-if="loading">Loading...</div>
    <div v-else class="card">
      <table>
        <thead>
          <tr>
            <th>Control</th>
            <th>Name</th>
            <th>Status</th>
            <th>Evidence / Notes</th>
            <th>Owner answers</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ac in auditControls" :key="ac.id">
            <td>{{ ac.controlControlId }}</td>
            <td>
              <button class="link-btn" type="button" @click="openControlDetails(ac.controlId)">
                {{ ac.controlName }}
              </button>
            </td>
            <td>
              <select :value="ac.status" @change="updateStatus(ac, $event.target.value)">
                <option value="NOT_STARTED">Not started</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="PASS">Pass</option>
                <option value="FAIL">Fail</option>
                <option value="NA">N/A</option>
              </select>
            </td>
            <td>
              <textarea
                :value="ac.notes"
                @blur="updateNotes(ac, $event.target.value)"
                rows="2"
                placeholder="Notes / evidence"
                class="notes-input"
              />
            </td>
            <td>
              <div v-for="a in ac.answers" :key="a.id" class="answer-row">
                <strong>Q:</strong> {{ a.questionText }}<br />
                <strong>A:</strong> {{ a.answerText || '—' }}
              </div>
              <span v-if="!ac.answers?.length">—</span>
            </td>
          </tr>
        </tbody>
      </table>
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
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../services/api'

const route = useRoute()
const auditId = Number(route.params.auditId)
const audit = ref(null)
const auditControls = ref([])
const loading = ref(true)
const detailsModal = ref(null)

onMounted(async () => {
  try {
    const [auditRes, controlsRes] = await Promise.all([
      api.get(`/api/audits/${auditId}`),
      api.get(`/api/audits/${auditId}/controls`)
    ])
    audit.value = auditRes.data
    auditControls.value = controlsRes.data || []
  } finally {
    loading.value = false
  }
})

async function updateStatus(ac, status) {
  try {
    await api.put(`/api/audit-controls/${ac.id}`, { status })
    const idx = auditControls.value.findIndex((x) => x.id === ac.id)
    if (idx >= 0) auditControls.value[idx].status = status
  } catch (e) {
    alert(e.response?.data?.error || 'Failed to update')
  }
}

async function updateNotes(ac, notes) {
  if (notes === ac.notes) return
  try {
    await api.put(`/api/audit-controls/${ac.id}`, { notes })
    const idx = auditControls.value.findIndex((x) => x.id === ac.id)
    if (idx >= 0) auditControls.value[idx].notes = notes
  } catch (e) {
    alert(e.response?.data?.error || 'Failed to update')
  }
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
    alert(e.response?.data?.error || 'Failed to load control details')
  }
}
</script>

<style scoped>
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
.notes-input { width: 100%; padding: 0.35rem; border: 1px solid #cbd5e0; border-radius: 4px; font-size: 0.9rem; resize: vertical; }
.answer-row { font-size: 0.9rem; margin-bottom: 0.5rem; }
.answer-row strong { color: #4a5568; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { max-width: 700px; width: 92%; }
.modal-wide { max-width: 760px; }
</style>
