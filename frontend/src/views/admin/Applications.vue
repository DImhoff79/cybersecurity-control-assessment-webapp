<template>
  <div>
    <h1 class="page-title">Applications</h1>
    <div class="card">
      <button class="btn btn-primary" @click="openModal()">Add application</button>
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Description</th>
            <th>Owner</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in applications" :key="a.id">
            <td>{{ a.name }}</td>
            <td>{{ a.description || '—' }}</td>
            <td>{{ a.ownerDisplayName || a.ownerEmail || '—' }}</td>
            <td>
              <button class="btn btn-secondary btn-sm" @click="openModal(a)">Edit</button>
              <button class="btn btn-danger btn-sm" @click="remove(a.id)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal card">
        <h2>{{ editing?.id ? 'Edit' : 'Add' }} Application</h2>
        <form @submit.prevent="save">
          <div class="form-group">
            <label>Name</label>
            <input v-model="form.name" required />
          </div>
          <div class="form-group">
            <label>Description</label>
            <textarea v-model="form.description" />
          </div>
          <div class="form-group">
            <label>Owner</label>
            <select v-model="form.ownerId">
              <option :value="null">— Select —</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.displayName || u.email }}</option>
            </select>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="showModal = false">Cancel</button>
            <button type="submit" class="btn btn-primary">Save</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api from '../../services/api'

const applications = ref([])
const users = ref([])
const showModal = ref(false)
const editing = ref(null)
const form = reactive({ name: '', description: '', ownerId: null })

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
  showModal.value = true
}

async function save() {
  try {
    if (editing.value?.id) {
      await api.put(`/api/applications/${editing.value.id}`, form)
    } else {
      await api.post('/api/applications', form)
    }
    showModal.value = false
    load()
  } catch (e) {
    alert(e.response?.data?.error || 'Failed to save')
  }
}

async function remove(id) {
  if (!confirm('Delete this application?')) return
  try {
    await api.delete(`/api/applications/${id}`)
    load()
  } catch (e) {
    alert(e.response?.data?.error || 'Failed to delete')
  }
}
</script>

<style scoped>
.btn-sm { padding: 0.35rem 0.75rem; font-size: 0.85rem; margin-right: 0.25rem; }
.form-actions { display: flex; gap: 0.75rem; margin-top: 1rem; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { max-width: 500px; width: 90%; }
.modal h2 { margin-top: 0; }
</style>
