<template>
  <div>
    <div class="mb-3">
      <h1 class="h3 mb-1">Approval delegates</h1>
      <p class="text-muted mb-0">
        Users listed here may approve or reject control exception requests in addition to users with audit management
        access.
      </p>
    </div>

    <div class="card workspace-card border-0 shadow-sm">
      <div class="card-body">
        <div class="row g-2 align-items-end mb-3">
          <div class="col-md-6">
            <label class="form-label small mb-1">Add user</label>
            <select v-model="selectedUserId" class="form-select">
              <option :value="null">Select a user…</option>
              <option v-for="u in selectableUsers" :key="u.id" :value="u.id">
                {{ u.displayName || u.email }} ({{ u.email }})
              </option>
            </select>
          </div>
          <div class="col-md-auto">
            <button class="btn btn-primary" :disabled="!selectedUserId || saving" @click="addDelegate">Add delegate</button>
          </div>
        </div>

        <div v-if="loading" class="text-muted">Loading…</div>
        <div v-else-if="!delegates.length" class="text-muted">No delegates yet.</div>
        <div v-else class="table-responsive">
          <table class="table workspace-table align-middle mb-0">
            <thead>
              <tr>
                <th>User</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in delegates" :key="d.id">
                <td>
                  <div class="fw-semibold">{{ d.displayName || d.email }}</div>
                  <div class="small text-muted">{{ d.email }}</div>
                </td>
                <td class="text-end">
                  <button type="button" class="btn btn-outline-danger btn-sm" @click="remove(d.id)">Remove</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const delegates = ref([])
const users = ref([])
const loading = ref(true)
const saving = ref(false)
const selectedUserId = ref(null)

const delegateUserIds = computed(() => new Set(delegates.value.map((d) => d.userId)))

const selectableUsers = computed(() =>
  (users.value || []).filter((u) => !delegateUserIds.value.has(u.id))
)

onMounted(async () => {
  await Promise.all([loadDelegates(), loadUsers()])
})

async function loadDelegates() {
  loading.value = true
  try {
    const res = await api.get('/api/admin/approval-delegates')
    delegates.value = res.data || []
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to load delegates.')
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  try {
    const res = await api.get('/api/users')
    users.value = res.data || []
  } catch {
    users.value = []
  }
}

async function addDelegate() {
  if (!selectedUserId.value) return
  saving.value = true
  try {
    await api.post('/api/admin/approval-delegates', { userId: selectedUserId.value })
    toastSuccess('Delegate added.')
    selectedUserId.value = null
    await loadDelegates()
  } catch (e) {
    toastError(e.response?.data?.error || 'Could not add delegate.')
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  if (!confirm('Remove this approval delegate?')) return
  try {
    await api.delete(`/api/admin/approval-delegates/${id}`)
    toastSuccess('Removed.')
    await loadDelegates()
  } catch (e) {
    toastError(e.response?.data?.error || 'Could not remove.')
  }
}
</script>
