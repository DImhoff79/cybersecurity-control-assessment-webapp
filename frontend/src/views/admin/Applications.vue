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
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in applications" :key="a.id">
                <td>{{ a.name }}</td>
                <td>{{ a.description || '-' }}</td>
                <td>{{ a.ownerDisplayName || a.ownerEmail || '-' }}</td>
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
