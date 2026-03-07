<template>
  <div>
    <h1 class="h3 mb-3">Audits</h1>

    <section class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Kick off new audit</h2>
        <form @submit.prevent="createAudit" class="row g-3 align-items-end">
          <div class="col-md-5">
            <label class="form-label">Application</label>
            <select v-model="createForm.applicationId" required class="form-select">
              <option :value="null">- Select -</option>
              <option v-for="a in applications" :key="a.id" :value="a.id">{{ a.name }}</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label">Year</label>
            <input v-model.number="createForm.year" type="number" min="2020" max="2030" required class="form-control" />
          </div>
          <div class="col-md-4">
            <button type="submit" class="btn btn-primary">Create audit</button>
          </div>
        </form>
      </div>
    </section>

    <section class="card shadow-sm">
      <div class="card-body">
        <h2 class="h5 mb-3">Audit history by application</h2>
        <div v-for="app in applications" :key="app.id" class="mb-4">
          <h3 class="h6 mb-2">{{ app.name }}</h3>
          <div class="table-responsive">
            <table class="table table-striped table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>Year</th>
                  <th>Status</th>
                  <th>Assigned to</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="a in auditsByApp[app.id] || []" :key="a.id">
                  <td>{{ a.year }}</td>
                  <td>{{ formatAuditStatus(a.status) }}</td>
                  <td>{{ a.assignedToDisplayName || a.assignedToEmail || '-' }}</td>
                  <td class="text-nowrap">
                    <button class="btn btn-secondary btn-sm me-2" @click="openAssignModal(a)">Assign / Send</button>
                    <router-link :to="`/admin/audits/${a.id}`" class="btn btn-primary btn-sm me-2">View / Edit</router-link>
                    <button class="btn btn-danger btn-sm" @click="deleteAudit(a.id)">Delete</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>

    <BsModal v-model="isAssignModalOpen" title="Assign and send audit">
      <p v-if="assignModal" class="text-muted">{{ assignModal.applicationName }} - {{ assignModal.year }}</p>
      <form @submit.prevent>
        <div class="mb-3">
          <label class="form-label">Assign to</label>
          <select v-model="assignForm.userId" class="form-select">
            <option :value="null">- Select -</option>
            <option v-for="u in users" :key="u.id" :value="u.id">{{ u.displayName || u.email }}</option>
          </select>
        </div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="isAssignModalOpen = false">Cancel</button>
        <button type="button" class="btn btn-primary" @click="assignAndSend">Assign only</button>
        <button type="button" class="btn btn-primary" @click="sendToOwner">Assign and send to owner</button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import BsModal from '../../components/BsModal.vue'
import api from '../../services/api'

const applications = ref([])
const users = ref([])
const createForm = reactive({ applicationId: null, year: new Date().getFullYear() })
const assignModal = ref(null)
const assignForm = reactive({ userId: null })
const auditsByApp = ref({})

const isAssignModalOpen = computed({
  get: () => !!assignModal.value,
  set: (open) => {
    if (!open) assignModal.value = null
  }
})

onMounted(load)

async function load() {
  const [appsRes, usersRes] = await Promise.all([
    api.get('/api/applications'),
    api.get('/api/users')
  ])
  applications.value = appsRes.data || []
  users.value = usersRes.data || []
  for (const app of applications.value) {
    const res = await api.get(`/api/applications/${app.id}/audits`)
    auditsByApp.value[app.id] = res.data || []
  }
}

async function createAudit() {
  try {
    await api.post(`/api/applications/${createForm.applicationId}/audits`, { year: createForm.year })
    load()
  } catch (e) {
    alert(e.response?.data?.error || 'Failed to create audit')
  }
}

function openAssignModal(audit) {
  assignModal.value = audit
  assignForm.userId = audit.assignedToUserId ?? null
}

async function assignAndSend() {
  if (!assignModal.value) return
  try {
    await api.put(`/api/audits/${assignModal.value.id}/assign`, { userId: assignForm.userId })
    load()
    assignModal.value = null
  } catch (e) {
    alert(e.response?.data?.error || 'Failed to assign')
  }
}

async function sendToOwner() {
  if (!assignModal.value) return
  try {
    await api.put(`/api/audits/${assignModal.value.id}/assign`, { userId: assignForm.userId })
    await api.post(`/api/audits/${assignModal.value.id}/send`)
    load()
    assignModal.value = null
  } catch (e) {
    alert(e.response?.data?.error || 'Failed to send')
  }
}

async function deleteAudit(auditId) {
  if (!confirm('Delete this audit and all of its responses? This cannot be undone.')) return
  try {
    await api.delete(`/api/audits/${auditId}`)
    await load()
  } catch (e) {
    alert(e.response?.data?.error || 'Failed to delete audit')
  }
}

function formatAuditStatus(status) {
  switch (status) {
    case 'SUBMITTED':
      return 'Completed - pending admin review'
    case 'COMPLETE':
      return 'Validated complete'
    case 'IN_PROGRESS':
      return 'In progress'
    case 'DRAFT':
      return 'Draft'
    default:
      return status || '-'
  }
}
</script>
