<template>
  <div>
    <h1 class="h3 mb-3">Applications</h1>
    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <button class="btn btn-primary mb-3" @click="openModal()">Add application</button>
        <div class="table-responsive">
          <table class="table table-striped table-hover align-middle mb-0">
            <thead>
              <tr>
                <th>Name</th>
                <th>Description</th>
                <th>Owner</th>
                <th>Criticality</th>
                <th>Lifecycle</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in applications" :key="a.id">
                <td>{{ a.name }}</td>
                <td>{{ a.description || '-' }}</td>
                <td>{{ a.ownerDisplayName || a.ownerEmail || '-' }}</td>
                <td>{{ a.criticality || '-' }}</td>
                <td>{{ a.lifecycleStatus || '-' }}</td>
                <td class="text-nowrap">
                  <button class="btn btn-secondary btn-sm me-2" @click="openModal(a)">Edit</button>
                  <button class="btn btn-danger btn-sm" @click="remove(a.id)">Delete</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <BsModal v-model="showModal" :title="`${editing?.id ? 'Edit' : 'Add'} Application`">
      <form id="application-form" @submit.prevent="save">
        <div class="mb-3">
          <label class="form-label">Name</label>
          <input v-model="form.name" required class="form-control" />
        </div>
        <div class="mb-3">
          <label class="form-label">Description</label>
          <textarea v-model="form.description" class="form-control" />
        </div>
        <div class="mb-3">
          <label class="form-label">Owner</label>
          <select v-model="form.ownerId" class="form-select">
            <option :value="null">- Select -</option>
            <option v-for="u in users" :key="u.id" :value="u.id">{{ u.displayName || u.email }}</option>
          </select>
        </div>
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label">Criticality</label>
            <select v-model="form.criticality" class="form-select">
              <option :value="null">- Select -</option>
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">Data Classification</label>
            <select v-model="form.dataClassification" class="form-select">
              <option :value="null">- Select -</option>
              <option value="PUBLIC">PUBLIC</option>
              <option value="INTERNAL">INTERNAL</option>
              <option value="CONFIDENTIAL">CONFIDENTIAL</option>
              <option value="RESTRICTED">RESTRICTED</option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">Lifecycle Status</label>
            <select v-model="form.lifecycleStatus" class="form-select">
              <option :value="null">- Select -</option>
              <option value="PLANNED">PLANNED</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="SUNSETTING">SUNSETTING</option>
              <option value="RETIRED">RETIRED</option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">Regulatory Scope</label>
            <input v-model="form.regulatoryScope" class="form-control" />
          </div>
          <div class="col-md-6">
            <label class="form-label">Business Owner Name</label>
            <input v-model="form.businessOwnerName" class="form-control" />
          </div>
          <div class="col-md-6">
            <label class="form-label">Technical Owner Name</label>
            <input v-model="form.technicalOwnerName" class="form-control" />
          </div>
        </div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="showModal = false">Cancel</button>
        <button type="submit" form="application-form" class="btn btn-primary">Save</button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import BsModal from '../../components/BsModal.vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const applications = ref([])
const users = ref([])
const showModal = ref(false)
const editing = ref(null)
const form = reactive({
  name: '',
  description: '',
  ownerId: null,
  criticality: null,
  dataClassification: null,
  regulatoryScope: '',
  businessOwnerName: '',
  technicalOwnerName: '',
  lifecycleStatus: null
})

onMounted(load)

async function load() {
  const [appsRes, usersRes] = await Promise.all([
    api.get('/api/applications'),
    api.get('/api/users')
  ])
  applications.value = appsRes.data || []
  users.value = usersRes.data || []
}

function openModal(app = null) {
  editing.value = app
  form.name = app?.name ?? ''
  form.description = app?.description ?? ''
  form.ownerId = app?.ownerId ?? null
  form.criticality = app?.criticality ?? null
  form.dataClassification = app?.dataClassification ?? null
  form.regulatoryScope = app?.regulatoryScope ?? ''
  form.businessOwnerName = app?.businessOwnerName ?? ''
  form.technicalOwnerName = app?.technicalOwnerName ?? ''
  form.lifecycleStatus = app?.lifecycleStatus ?? null
  showModal.value = true
}

async function save() {
  try {
    if (editing.value?.id) {
      await api.put(`/api/applications/${editing.value.id}`, form)
      toastSuccess('Application updated.')
    } else {
      await api.post('/api/applications', form)
      toastSuccess('Application created.')
    }
    showModal.value = false
    load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to save')
  }
}

async function remove(id) {
  if (!confirm('Delete this application?')) return
  try {
    await api.delete(`/api/applications/${id}`)
    toastSuccess('Application deleted.')
    load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to delete')
  }
}
</script>
