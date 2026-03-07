<template>
  <div>
    <h1 class="h3 mb-2">{{ audit?.applicationName }} - {{ audit?.year }} (Assessment view)</h1>
    <p v-if="audit" class="text-muted">
      Status: {{ audit.status }} |
      Assigned to: {{ audit.assignedToDisplayName || audit.assignedToEmail || '-' }}
    </p>

    <div v-if="loading" class="text-muted">Loading...</div>
    <div v-else class="card shadow-sm">
      <div class="card-body">
        <div class="table-responsive">
          <table class="table table-striped table-hover align-middle mb-0">
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
                  <button class="btn btn-link p-0 text-decoration-none" type="button" @click="openControlDetails(ac.controlId)">
                    {{ ac.controlName }}
                  </button>
                </td>
                <td>
                  <select :value="ac.status" @change="updateStatus(ac, $event.target.value)" class="form-select form-select-sm">
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
                    class="form-control form-control-sm"
                  />
                </td>
                <td>
                  <div v-for="a in ac.answers" :key="a.id" class="small mb-2">
                    <strong>Q:</strong> {{ a.questionText }}<br />
                    <strong>A:</strong> {{ a.answerText || '-' }}
                  </div>
                  <span v-if="!ac.answers?.length">-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

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

const route = useRoute()
const auditId = Number(route.params.auditId)
const audit = ref(null)
const auditControls = ref([])
const loading = ref(true)
const detailsModal = ref(null)

const isDetailsOpen = computed({
  get: () => !!detailsModal.value,
  set: (open) => {
    if (!open) detailsModal.value = null
  }
})

const detailsTitle = computed(() => {
  if (!detailsModal.value) return 'Control details'
  return `${detailsModal.value.controlId} - ${detailsModal.value.name}`
})

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
