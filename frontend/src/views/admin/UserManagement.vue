<template>
  <div>
    <h1 class="h3 mb-3">User Management</h1>

    <section class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Pending Social Access Requests</h2>
        <p v-if="!pendingRequests.length" class="text-muted mb-0">No pending requests.</p>
        <div v-else class="table-responsive">
          <table class="table table-striped mb-0">
            <thead>
              <tr>
                <th>Email</th>
                <th>Name</th>
                <th>Provider</th>
                <th>Requested</th>
                <th>Role</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in pendingRequests" :key="r.id">
                <td>{{ r.email }}</td>
                <td>{{ r.displayName || '-' }}</td>
                <td>{{ r.provider }}</td>
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
        <h2 class="h5 mb-3">Create User</h2>
        <form class="row g-2 mb-4" @submit.prevent="createUser">
          <div class="col-md-3">
            <input v-model.trim="createForm.email" type="email" class="form-control" placeholder="Email" required>
          </div>
          <div class="col-md-2">
            <input v-model.trim="createForm.displayName" type="text" class="form-control" placeholder="Display name">
          </div>
          <div class="col-md-2">
            <input v-model="createForm.password" type="password" class="form-control" placeholder="Password" required minlength="8">
          </div>
          <div class="col-md-2">
            <select v-model="createForm.role" class="form-select">
              <option v-for="role in roles" :key="role.value" :value="role.value">{{ role.label }}</option>
            </select>
          </div>
          <div class="col-md-3 d-flex align-items-center justify-content-end">
            <button class="btn btn-primary" type="submit">Create User</button>
          </div>
          <div class="col-12">
            <div class="small text-muted mb-1">Permissions</div>
            <div class="d-flex flex-wrap gap-3">
              <label v-for="permission in permissions" :key="permission.value" class="form-check">
                <input
                  class="form-check-input me-1"
                  type="checkbox"
                  :value="permission.value"
                  :checked="createForm.permissions.includes(permission.value)"
                  @change="togglePermission(createForm.permissions, permission.value, $event.target.checked)"
                >
                <span class="form-check-label">{{ permission.label }}</span>
              </label>
            </div>
          </div>
        </form>

        <h2 class="h5 mb-3">Users</h2>
        <div class="table-responsive">
          <table class="table table-striped mb-0">
            <thead>
              <tr>
                <th>Email</th>
                <th>Name</th>
                <th>Role</th>
                <th>Permissions</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in users" :key="u.id">
                <td><input v-model.trim="userForm(u.id).email" type="email" class="form-control form-control-sm"></td>
                <td><input v-model.trim="userForm(u.id).displayName" type="text" class="form-control form-control-sm"></td>
                <td>
                  <select v-model="userForm(u.id).role" class="form-select form-select-sm">
                    <option v-for="role in roles" :key="role.value" :value="role.value">{{ role.label }}</option>
                  </select>
                </td>
                <td>
                  <div class="d-flex flex-wrap gap-2">
                    <label v-for="permission in permissions" :key="permission.value + u.id" class="form-check">
                      <input
                        class="form-check-input me-1"
                        type="checkbox"
                        :value="permission.value"
                        :checked="userForm(u.id).permissions.includes(permission.value)"
                        @change="togglePermission(userForm(u.id).permissions, permission.value, $event.target.checked)"
                      >
                      <span class="form-check-label small">{{ permission.label }}</span>
                    </label>
                  </div>
                </td>
                <td class="text-nowrap">
                  <button class="btn btn-primary btn-sm me-2" @click="updateUser(u.id)">Save</button>
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

const permissions = [
  { value: 'USER_MANAGEMENT', label: 'User Management' },
  { value: 'APPLICATION_MANAGEMENT', label: 'Application Management' },
  { value: 'AUDIT_MANAGEMENT', label: 'Audit Management' },
  { value: 'AUDIT_EXECUTION', label: 'Audit Execution' },
  { value: 'REPORT_VIEW', label: 'Report View' }
]

const createForm = reactive({
  email: '',
  displayName: '',
  password: '',
  role: 'APPLICATION_OWNER',
  permissions: []
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
      permissions: [...(user.permissions || [])]
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
      role: createForm.role,
      permissions: createForm.permissions
    })
    toastSuccess('User created.')
    createForm.email = ''
    createForm.displayName = ''
    createForm.password = ''
    createForm.role = 'APPLICATION_OWNER'
    createForm.permissions = []
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create user')
  }
}

async function updateUser(userId) {
  const form = editForms[userId]
  try {
    await api.put(`/api/users/${userId}`, {
      email: form.email,
      displayName: form.displayName || null,
      role: form.role,
      permissions: form.permissions
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

function togglePermission(target, permission, checked) {
  if (checked) {
    if (!target.includes(permission)) target.push(permission)
    return
  }
  const idx = target.indexOf(permission)
  if (idx >= 0) target.splice(idx, 1)
}

function userForm(userId) {
  if (!editForms[userId]) {
    editForms[userId] = {
      email: '',
      displayName: '',
      role: 'APPLICATION_OWNER',
      permissions: []
    }
  }
  return editForms[userId]
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}
</script>
