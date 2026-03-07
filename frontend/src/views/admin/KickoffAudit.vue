<template>
  <div>
    <h1 class="page-title">Audits</h1>

    <section class="card">
      <h2>Kick off new audit</h2>
      <form @submit.prevent="createAudit" class="inline-form">
        <div class="form-group">
          <label>Application</label>
          <select v-model="createForm.applicationId" required>
            <option :value="null">— Select —</option>
            <option v-for="a in applications" :key="a.id" :value="a.id">{{ a.name }}</option>
          </select>
        </div>
        <div class="form-group">
          <label>Year</label>
          <input v-model.number="createForm.year" type="number" min="2020" max="2030" required />
        </div>
        <button type="submit" class="btn btn-primary">Create audit</button>
      </form>
    </section>

    <section class="card">
      <h2>Audit history by application</h2>
      <div v-for="app in applications" :key="app.id" class="app-audits">
        <h3>{{ app.name }}</h3>
        <table>
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
              <td>{{ a.status }}</td>
              <td>{{ a.assignedToDisplayName || a.assignedToEmail || '—' }}</td>
              <td>
                <button class="btn btn-secondary btn-sm" @click="openAssignModal(a)">Assign / Send</button>
                <router-link :to="`/admin/audits/${a.id}`" class="btn btn-primary btn-sm">View / Edit</router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="assignModal" class="modal-overlay" @click.self="assignModal = null">
      <div class="modal card">
        <h2>Assign & send audit</h2>
        <p v-if="assignModal">{{ assignModal.applicationName }} – {{ assignModal.year }}</p>
        <form @submit.prevent>
          <div class="form-group">
            <label>Assign to</label>
            <select v-model="assignForm.userId">
              <option :value="null">— Select —</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.displayName || u.email }}</option>
            </select>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="assignModal = null">Cancel</button>
            <button type="button" class="btn btn-primary" @click="assignAndSend">Assign only</button>
            <button type="button" class="btn btn-primary" @click="sendToOwner">Assign &amp; send to owner</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import api from '../../services/api'

const applications = ref([])
const users = ref([])
const createForm = reactive({ applicationId: null, year: new Date().getFullYear() })
const assignModal = ref(null)
const assignForm = reactive({ userId: null })

const auditsByApp = ref({})

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
</script>

<style scoped>
.inline-form { display: flex; flex-wrap: wrap; gap: 1rem; align-items: flex-end; }
.inline-form .form-group { margin-bottom: 0; min-width: 160px; }
.app-audits { margin-bottom: 1.5rem; }
.app-audits h3 { margin: 0 0 0.5rem; font-size: 1.1rem; }
.btn-sm { padding: 0.35rem 0.75rem; font-size: 0.85rem; margin-right: 0.25rem; }
.form-actions { display: flex; gap: 0.75rem; margin-top: 1rem; flex-wrap: wrap; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { max-width: 480px; width: 90%; }
.modal h2 { margin-top: 0; }
</style>
