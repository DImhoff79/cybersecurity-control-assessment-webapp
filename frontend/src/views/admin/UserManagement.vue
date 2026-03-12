<template>
  <div>
    <h1 class="h3 mb-1">User Management</h1>
    <p class="text-muted mb-3">
      Approve access and assign roles. User identity fields are read-only after account creation.
    </p>

    <section class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Pending Social Access Requests</h2>
        <p v-if="!pendingRequests.length" class="text-muted mb-0">No pending requests.</p>
        <div v-else class="table-responsive">
          <table class="table table-striped mb-0">
            <thead>
              <tr>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="togglePendingSort('email')">Email {{ pendingSortIndicator('email') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="togglePendingSort('displayName')">Name {{ pendingSortIndicator('displayName') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="togglePendingSort('provider')">Provider {{ pendingSortIndicator('provider') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="togglePendingSort('requestedAt')">Requested {{ pendingSortIndicator('requestedAt') }}</button></th>
                <th class="w-role">Grant Role</th>
                <th class="w-action"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in sortedPendingRequests" :key="r.id">
                <td>{{ r.email }}</td>
                <td>{{ r.displayName || '-' }}</td>
                <td><span class="badge text-bg-light border">{{ r.provider }}</span></td>
                <td>{{ formatDate(r.requestedAt) }}</td>
                <td>
                  <select v-model="decisionRole[r.id]" class="form-select form-select-sm">
                    <option v-for="role in roles" :key="role.value" :value="role.value">{{ role.label }}</option>
                  </select>
                </td>
                <td class="text-nowrap">
                  <button class="btn btn-success btn-sm me-2" @click="approve(r.id)">Approve</button>
                  <button class="btn btn-outline-danger btn-sm" @click="reject(r.id)">Reject</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <section class="card shadow-sm">
      <div class="card-body">
        <h2 class="h5 mb-2">Create User</h2>
        <p class="small text-muted mb-3">
          Permissions are automatically assigned from the selected role.
        </p>
        <form class="row g-3 mb-4 border rounded p-3 bg-light-subtle" @submit.prevent="createUser">
          <div class="col-md-4">
            <label class="form-label small text-muted mb-1">Email</label>
            <input v-model.trim="createForm.email" type="email" class="form-control" placeholder="name@company.com" required>
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">Display Name</label>
            <input v-model.trim="createForm.displayName" type="text" class="form-control" placeholder="Display name">
          </div>
          <div class="col-md-2">
            <label class="form-label small text-muted mb-1">Password</label>
            <input v-model="createForm.password" type="password" class="form-control" placeholder="Password" required minlength="8">
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">Role</label>
            <select v-model="createForm.role" class="form-select">
              <option v-for="role in roles" :key="role.value" :value="role.value">{{ role.label }}</option>
            </select>
          </div>
          <div class="col-12 d-flex justify-content-end align-items-center">
            <button class="btn btn-primary" type="submit">Create User</button>
          </div>
        </form>

        <h2 class="h5 mb-3">Users</h2>
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleUserSort('user')">User {{ userSortIndicator('user') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleUserSort('role')">Role {{ userSortIndicator('role') }}</button></th>
                <th class="w-action"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in sortedUsers" :key="u.id">
                <td>
                  <div class="fw-semibold">{{ userForm(u.id).displayName || 'No display name' }}</div>
                  <div class="small text-muted">{{ userForm(u.id).email }}</div>
                </td>
                <td>
                  <select
                    v-model="userForm(u.id).role"
                    class="form-select form-select-sm"
                  >
                    <option v-for="role in roles" :key="role.value" :value="role.value">{{ role.label }}</option>
                  </select>
                </td>
                <td class="text-nowrap">
                  <button class="btn btn-primary btn-sm me-2" @click="updateUser(u.id)">Save</button>
                  <button class="btn btn-outline-secondary btn-sm me-2" @click="resetUserForm(u.id)">Reset</button>
                  <button class="btn btn-outline-danger btn-sm" @click="deleteUser(u.id)">Delete</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'
import { useTableSort } from '../../composables/useTableSort'

const users = ref([])
const pendingRequests = ref([])
const decisionRole = reactive({})
const editForms = reactive({})

const roles = [
  { value: 'ADMIN', label: 'Admin' },
  { value: 'AUDIT_MANAGER', label: 'Audit Manager' },
  { value: 'AUDITOR', label: 'Auditor' },
  { value: 'APPLICATION_OWNER', label: 'Application Owner' }
]

const { sortedRows: sortedPendingRequests, toggleSort: togglePendingSort, sortIndicator: pendingSortIndicator } = useTableSort(pendingRequests, {
  initialKey: 'requestedAt'
})

const { sortedRows: sortedUsers, toggleSort: toggleUserSort, sortIndicator: userSortIndicator } = useTableSort(users, {
  initialKey: 'user',
  valueGetters: {
    user: (row) => row.displayName || row.email || ''
  }
})

const createForm = reactive({
  email: '',
  displayName: '',
  password: '',
  role: 'APPLICATION_OWNER'
})

onMounted(load)

async function load() {
  const [usersRes, requestsRes] = await Promise.all([
    api.get('/api/users'),
    api.get('/api/admin/access-requests')
  ])
  users.value = usersRes.data || []
  pendingRequests.value = requestsRes.data || []
  for (const request of pendingRequests.value) {
    if (!decisionRole[request.id]) decisionRole[request.id] = createForm.role
  }

  for (const user of users.value) {
    editForms[user.id] = {
      email: user.email || '',
      displayName: user.displayName || '',
      role: user.role || 'APPLICATION_OWNER',
      originalRole: user.role || 'APPLICATION_OWNER'
    }
  }
}

async function approve(id) {
  try {
    await api.post(`/api/admin/access-requests/${id}/approve`, { role: decisionRole[id] || createForm.role })
    toastSuccess('Access request approved.')
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to approve access request')
  }
}

async function reject(id) {
  try {
    await api.post(`/api/admin/access-requests/${id}/reject`, {})
    toastSuccess('Access request rejected.')
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to reject access request')
  }
}

async function createUser() {
  try {
    await api.post('/api/users', {
      email: createForm.email,
      displayName: createForm.displayName || null,
      password: createForm.password,
      role: createForm.role
    })
    toastSuccess('User created.')
    createForm.email = ''
    createForm.displayName = ''
    createForm.password = ''
    createForm.role = 'APPLICATION_OWNER'
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create user')
  }
}

async function updateUser(userId) {
  const form = editForms[userId]
  try {
    await api.put(`/api/users/${userId}`, {
      role: form.role
    })
    toastSuccess('User updated.')
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update user')
  }
}

async function deleteUser(userId) {
  if (!window.confirm('Delete this user?')) return
  try {
    await api.delete(`/api/users/${userId}`)
    toastSuccess('User deleted.')
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to delete user')
  }
}

function userForm(userId) {
  if (!editForms[userId]) {
    editForms[userId] = {
      email: '',
      displayName: '',
      role: 'APPLICATION_OWNER',
      originalRole: 'APPLICATION_OWNER'
    }
  }
  return editForms[userId]
}

function resetUserForm(userId) {
  const form = userForm(userId)
  form.role = form.originalRole
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}
</script>

<style scoped>
.w-action {
  width: 190px;
}

.w-role {
  width: 190px;
}
</style>
